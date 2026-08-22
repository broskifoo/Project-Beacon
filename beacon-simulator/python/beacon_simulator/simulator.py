"""
Core simulation engine for Beacon mesh network.
"""
from __future__ import annotations

import json
import random
import time
import uuid
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Callable, Dict, List, Optional, Set, Tuple

import numpy as np
from tqdm import tqdm

from .models import (Link, Message, MessagePriority, MessageType, NetworkTopology, 
                     NodeConfig, NodeState, Position, SimulationConfig, SimulationMetrics,
                     TransportType, Scenario)
from .propagation import PropagationModel, create_propagation_model
from .mobility import MobilityModel, create_mobility_model
from .routing import RoutingProtocol, create_routing_protocol


@dataclass
class Simulator:
    """Main simulation engine."""
    
    config: SimulationConfig
    propagation: PropagationModel
    mobility: MobilityModel
    routing: RoutingProtocol
    
    # Runtime state
    nodes: Dict[str, NodeState] = field(default_factory=dict)
    messages: Dict[str, Message] = field(default_factory=dict)
    pending_messages: Dict[str, List[Message]] = field(default_factory=dict)  # node_id -> queue
    current_time: float = 0.0
    metrics_history: List[SimulationMetrics] = field(default_factory=list)
    topology_snapshots: List[NetworkTopology] = field(default_factory=list)
    event_log: List[Dict] = field(default_factory=list)
    
    # Callbacks
    on_message_created: Optional[Callable[[Message], None]] = None
    on_message_delivered: Optional[Callable[[Message], None]] = None
    on_message_dropped: Optional[Callable[[Message, str], None]] = None
    on_node_death: Optional[Callable[[str], None]] = None
    on_topology_change: Optional[Callable[[NetworkTopology], None]] = None
    
    def __post_init__(self):
        if self.config.random_seed is not None:
            random.seed(self.config.random_seed)
            np.random.seed(self.config.random_seed)
        
        self._initialize_nodes()
        self._initialize_pending_queues()
    
    def _initialize_nodes(self):
        """Create initial node states from config."""
        if self.config.node_configs:
            for node_config in self.config.node_configs:
                self._create_node(node_config)
        else:
            # Generate random nodes
            for i in range(self.config.num_nodes):
                config = NodeConfig(
                    node_id=f"node_{i:04d}",
                    position=Position(
                        np.random.uniform(0, self.config.area_width),
                        np.random.uniform(0, self.config.area_height)
                    )
                )
                self._create_node(config)
        
        # Initialize mobility for each node
        for node_id in self.nodes:
            self.mobility.initialize_node(node_id, self.nodes[node_id].config, 
                                         (self.config.area_width, self.config.area_height))
    
    def _create_node(self, config: NodeConfig):
        """Create a node state from config."""
        state = NodeState(
            config=config,
            position=config.position,
            battery_pct=config.initial_battery_pct,
            power_mode=config.power_mode
        )
        self.nodes[config.node_id] = state
        self.pending_messages[config.node_id] = []
    
    def _initialize_pending_queues(self):
        """Initialize message queues for each node."""
        for node_id in self.nodes:
            self.pending_messages[node_id] = []
    
    def run(self) -> List[SimulationMetrics]:
        """Run the simulation for the configured duration."""
        num_steps = int(self.config.duration_seconds * 1000 / self.config.time_step_ms)
        
        pbar = tqdm(total=num_steps, desc="Simulating", unit="step")
        
        last_metrics_time = 0.0
        last_snapshot_time = 0.0
        
        for step in range(num_steps):
            self.current_time = step * self.config.time_step_ms / 1000.0
            
            # Simulation step
            self._step()
            
            # Collect metrics
            if self.current_time - last_metrics_time >= self.config.metrics_interval_seconds:
                metrics = self._collect_metrics()
                self.metrics_history.append(metrics)
                last_metrics_time = self.current_time
            
            # Save topology snapshot
            if self.config.save_topology_snapshots and \
               self.current_time - last_snapshot_time >= self.config.snapshot_interval_seconds:
                snapshot = self._create_topology_snapshot()
                self.topology_snapshots.append(snapshot)
                last_snapshot_time = self.current_time
            
            pbar.update(1)
            pbar.set_postfix({
                "nodes": len([n for n in self.nodes.values() if n.is_alive]),
                "msgs": len(self.messages),
                "delivered": sum(1 for m in self.messages.values() if m.delivered_at)
            })
        
        pbar.close()
        
        # Final metrics
        self.metrics_history.append(self._collect_metrics())
        
        return self.metrics_history
    
    def _step(self):
        """Single simulation step."""
        dt = self.config.time_step_ms / 1000.0
        
        # Update mobility
        if self.config.mobility_enabled:
            self._update_mobility(dt)
        
        # Update radio links
        self._update_links()
        
        # Process message queues
        self._process_message_queues(dt)
        
        # Update node states (battery, power mode)
        self._update_node_states(dt)
        
        # Generate new messages (traffic patterns)
        self._generate_traffic(dt)
        
        # Handle scheduled events
        self._process_events()
    
    def _update_mobility(self, dt: float):
        """Update node positions based on mobility model."""
        for node_id, state in self.nodes.items():
            if not state.is_alive:
                continue
            
            new_pos = self.mobility.update_position(
                node_id, state.position, dt,
                (self.config.area_width, self.config.area_height)
            )
            state.position = new_pos
    
    def _update_links(self):
        """Update radio links between nodes based on positions."""
        node_ids = list(self.nodes.keys())
        new_links = {}
        
        for i, src_id in enumerate(node_ids):
            src_state = self.nodes[src_id]
            if not src_state.is_alive:
                continue
            
            for dst_id in node_ids[i+1:]:
                dst_state = self.nodes[dst_id]
                if not dst_state.is_alive:
                    continue
                
                # Check each transport type
                for transport in src_state.config.transports & dst_state.config.transports:
                    link = self._create_link(src_id, dst_id, transport)
                    if link and link.is_active:
                        key = (src_id, dst_id) if src_id < dst_id else (dst_id, src_id)
                        if key not in new_links or link.quality_score() > new_links[key].quality_score():
                            new_links[key] = link
                        
                        # Update node neighbor tables
                        src_state.neighbors[dst_id] = link
                        dst_state.neighbors[src_id] = link
        
        # Clean up old neighbors
        for state in self.nodes.values():
            active_neighbors = set()
            for key, link in new_links.items():
                if key[0] == state.config.node_id:
                    active_neighbors.add(key[1])
                elif key[1] == state.config.node_id:
                    active_neighbors.add(key[0])
            
            # Remove stale neighbors
            stale = set(state.neighbors.keys()) - active_neighbors
            for n in stale:
                del state.neighbors[n]
    
    def _create_link(self, src_id: str, dst_id: str, transport: TransportType) -> Optional[Link]:
        """Create a link between two nodes if in range."""
        src = self.nodes[src_id]
        dst = self.nodes[dst_id]
        
        distance = src.position.distance_to(dst.position)
        
        # Get range for transport
        if transport == TransportType.BLE:
            max_range = src.config.radio_range_ble
        elif transport == TransportType.WIFI_DIRECT:
            max_range = src.config.radio_range_wifi
        elif transport == TransportType.LORA:
            max_range = src.config.radio_range_lora
        else:
            max_range = 50.0
        
        if distance > max_range:
            return None
        
        # Calculate signal strength
        rx_power = self.propagation.received_power(distance)
        snr = self.propagation.snr(distance)
        packet_loss = self.propagation.packet_loss_rate(distance, transport)
        
        # Estimate bandwidth
        if transport == TransportType.BLE:
            bandwidth = 1e6 * (1 - packet_loss)  # ~1 Mbps max
            latency = 10 + distance * 0.1  # ms
        elif transport == TransportType.WIFI_DIRECT:
            bandwidth = 50e6 * (1 - packet_loss)  # ~50 Mbps
            latency = 5 + distance * 0.05
        elif transport == TransportType.LORA:
            bandwidth = 5000 * (1 - packet_loss)  # ~5 kbps
            latency = 100 + distance * 0.5
        else:
            bandwidth = 1e6
            latency = 10
        
        return Link(
            source_id=src_id,
            target_id=dst_id,
            transport=transport,
            signal_strength_dbm=rx_power,
            snr_db=snr,
            bandwidth_bps=bandwidth,
            latency_ms=latency,
            packet_loss_rate=packet_loss,
            established_at=self.current_time,
            last_active=self.current_time
        )
    
    def _process_message_queues(self, dt: float):
        """Process pending message queues for each node."""
        for node_id, state in self.nodes.items():
            if not state.is_alive:
                continue
            
            queue = self.pending_messages.get(node_id, [])
            if not queue:
                continue
            
            # Sort by priority (CRITICAL first)
            priority_order = {
                MessagePriority.CRITICAL: 0,
                MessagePriority.HIGH: 1,
                MessagePriority.NORMAL: 2,
                MessagePriority.LOW: 3
            }
            queue.sort(key=lambda m: (priority_order[m.priority], m.created_at))
            
            # Process messages up to queue capacity
            processed = []
            remaining = []
            
            for msg in queue:
                if len(processed) >= 10:  # Max messages per tick
                    remaining.append(msg)
                    continue
                
                if self._transmit_message(node_id, msg):
                    processed.append(msg)
                else:
                    remaining.append(msg)
            
            self.pending_messages[node_id] = remaining
    
    def _transmit_message(self, node_id: str, message: Message) -> bool:
        """Attempt to transmit a message from a node."""
        state = self.nodes[node_id]
        
        # Check if destination is local
        if message.recipient_id == node_id:
            self._deliver_message(message, node_id)
            return True
        
        # Check if destination is direct neighbor
        if message.recipient_id and message.recipient_id in state.neighbors:
            link = state.neighbors[message.recipient_id]
            if self._attempt_transmission(message, link):
                self._deliver_message(message, message.recipient_id)
                return True
        
        # Determine forwarding neighbors
        if self.routing.should_forward(node_id, message, 
                                       self._create_topology_snapshot(), self.nodes):
            # Forward to neighbors
            neighbors = list(state.neighbors.keys())
            if not neighbors:
                return False
            
            # Select subset of neighbors (probabilistic)
            forward_count = max(1, int(len(neighbors) * 0.5))
            selected = random.sample(neighbors, min(forward_count, len(neighbors)))
            
            for next_hop in selected:
                link = state.neighbors[next_hop]
                if self._attempt_transmission(message, link):
                    # Queue at next hop
                    next_msg = Message(
                        message_id=message.message_id,
                        sender_id=message.sender_id,
                        recipient_id=message.recipient_id,
                        timestamp=self.current_time,
                        priority=message.priority,
                        ttl=message.ttl,
                        hop_count=message.hop_count + 1,
                        payload_size=message.payload_size,
                        message_type=message.message_type,
                        path=message.path + [node_id],
                        created_at=message.created_at
                    )
                    self.pending_messages[next_hop].append(next_msg)
                    state.forwarded_messages.append(message)
            
            return True
        
        return False
    
    def _attempt_transmission(self, message: Message, link: Link) -> bool:
        """Simulate transmission over a link."""
        # Check packet loss
        if random.random() < link.packet_loss_rate:
            return False
        
        # Energy cost
        tx_energy = self._calculate_tx_energy(message.payload_size, link.transport)
        self.nodes[link.source_id].battery_pct -= tx_energy
        self.nodes[link.source_id].total_tx_bytes += message.payload_size
        self.nodes[link.source_id].total_tx_time_ms += link.latency_ms
        
        # RX energy at destination
        rx_energy = self._calculate_rx_energy(message.payload_size, link.transport)
        self.nodes[link.target_id].battery_pct -= rx_energy
        self.nodes[link.target_id].total_rx_bytes += message.payload_size
        self.nodes[link.target_id].total_rx_time_ms += link.latency_ms
        
        return True
    
    def _calculate_tx_energy(self, payload_size: int, transport: TransportType) -> float:
        """Calculate TX energy cost in battery percentage."""
        # Simplified model
        if transport == TransportType.BLE:
            return payload_size * 1e-6  # ~1 uJ/byte
        elif transport == TransportType.WIFI_DIRECT:
            return payload_size * 5e-6
        elif transport == TransportType.LORA:
            return payload_size * 10e-6
        return payload_size * 1e-6
    
    def _calculate_rx_energy(self, payload_size: int, transport: TransportType) -> float:
        """Calculate RX energy cost in battery percentage."""
        if transport == TransportType.BLE:
            return payload_size * 0.5e-6
        elif transport == TransportType.WIFI_DIRECT:
            return payload_size * 2e-6
        elif transport == TransportType.LORA:
            return payload_size * 5e-6
        return payload_size * 0.5e-6
    
    def _deliver_message(self, message: Message, recipient_id: str):
        """Mark message as delivered."""
        message.delivered_at = self.current_time
        message.hop_count += 1
        
        if recipient_id in self.nodes:
            self.nodes[recipient_id].received_messages.append(message)
            self.nodes[recipient_id].total_rx_bytes += message.payload_size
        
        if self.on_message_delivered:
            self.on_message_delivered(message)
        
        self._log_event("message_delivered", {
            "message_id": message.message_id,
            "recipient": recipient_id,
            "hop_count": message.hop_count,
            "latency_ms": message.latency_ms
        })
    
    def _generate_traffic(self, dt: float):
        """Generate new messages based on traffic patterns."""
        # Background traffic: each node sends periodic telemetry
        for node_id, state in self.nodes.items():
            if not state.is_alive:
                continue
            
            # Periodic telemetry (every 30-60 seconds)
            if random.random() < dt / 45.0:
                msg = Message(
                    message_id=str(uuid.uuid4()),
                    sender_id=node_id,
                    recipient_id=None,  # Broadcast
                    timestamp=self.current_time,
                    priority=MessagePriority.LOW,
                    ttl=3,
                    payload_size=50,
                    message_type=MessageType.TELEMETRY,
                    created_at=self.current_time
                )
                self.messages[msg.message_id] = msg
                self.pending_messages[node_id].append(msg)
                
                if self.on_message_created:
                    self.on_message_created(msg)
            
            # Random user messages (low rate)
            if random.random() < dt / 300.0:
                # Pick random recipient
                alive_nodes = [n for n in self.nodes if self.nodes[n].is_alive and n != node_id]
                if alive_nodes:
                    recipient = random.choice(alive_nodes)
                    msg = Message(
                        message_id=str(uuid.uuid4()),
                        sender_id=node_id,
                        recipient_id=recipient,
                        timestamp=self.current_time,
                        priority=MessagePriority.NORMAL,
                        ttl=5,
                        payload_size=200,
                        message_type=MessageType.TEXT,
                        created_at=self.current_time
                    )
                    self.messages[msg.message_id] = msg
                    self.pending_messages[node_id].append(msg)
                    
                    if self.on_message_created:
                        self.on_message_created(msg)
    
    def _update_node_states(self, dt: float):
        """Update battery, power modes, check for node death."""
        for node_id, state in self.nodes.items():
            if not state.is_alive:
                continue
            
            # Base consumption (idle)
            base_consumption = 0.001 * dt  # 0.1%/hour base
            state.battery_pct = max(0, state.battery_pct - base_consumption)
            
            # Update power mode based on battery
            if state.battery_pct <= 10:
                state.power_mode = PowerMode.CRITICAL
            elif state.battery_pct <= 20:
                state.power_mode = PowerMode.SURVIVAL
            elif state.battery_pct <= 50:
                state.power_mode = PowerMode.CONSERVATION
            else:
                state.power_mode = PowerMode.NORMAL
            
            # Check for death
            if state.battery_pct <= 0:
                state.is_alive = False
                if self.on_node_death:
                    self.on_node_death(node_id)
                self._log_event("node_death", {"node_id": node_id, "time": self.current_time})
    
    def _process_events(self):
        """Process scheduled scenario events."""
        # TODO: Implement scenario event processing
        pass
    
    def _create_topology_snapshot(self) -> NetworkTopology:
        """Create a snapshot of current topology."""
        links = {}
        for state in self.nodes.values():
            if not state.is_alive:
                continue
            for neighbor_id, link in state.neighbors.items():
                key = (state.config.node_id, neighbor_id)
                if state.config.node_id < neighbor_id:
                    links[key] = link
        
        return NetworkTopology(
            nodes={k: v for k, v in self.nodes.items() if v.is_alive},
            links=links,
            timestamp=self.current_time
        )
    
    def _collect_metrics(self) -> SimulationMetrics:
        """Collect current simulation metrics."""
        alive_nodes = [n for n in self.nodes.values() if n.is_alive]
        dead_nodes = [n for n in self.nodes.values() if not n.is_alive]
        
        all_messages = list(self.messages.values())
        delivered = [m for m in all_messages if m.delivered_at]
        dropped = [m for m in all_messages if m.dropped]
        pending = [m for m in all_messages if not m.delivered_at and not m.dropped]
        
        # Delivery rate by priority
        priority_rates = {}
        for priority in MessagePriority:
            priority_msgs = [m for m in all_messages if m.priority == priority]
            if priority_msgs:
                priority_delivered = [m for m in priority_msgs if m.delivered_at]
                priority_rates[priority.value] = len(priority_delivered) / len(priority_msgs)
            else:
                priority_rates[priority.value] = 0.0
        
        # Network topology metrics
        topology = self._create_topology_snapshot()
        connected_components = self._count_connected_components(topology)
        largest_component = self._largest_component_size(topology)
        avg_degree = self._average_node_degree(topology)
        
        # Energy
        total_energy = sum(n.config.battery_capacity_mah * (1 - n.battery_pct/100) 
                          for n in self.nodes.values())
        
        return SimulationMetrics(
            timestamp=self.current_time,
            num_nodes_alive=len(alive_nodes),
            num_nodes_dead=len(dead_nodes),
            total_messages_created=len(all_messages),
            total_messages_delivered=len(delivered),
            total_messages_dropped=len(dropped),
            total_messages_pending=len(pending),
            delivery_rate=len(delivered) / len(all_messages) if all_messages else 0.0,
            avg_latency_ms=np.mean([m.latency_ms for m in delivered if m.latency_ms]) if delivered else 0.0,
            avg_hop_count=np.mean([m.hop_count for m in delivered]) if delivered else 0.0,
            avg_battery_pct=np.mean([n.battery_pct for n in alive_nodes]) if alive_nodes else 0.0,
            network_density=len(alive_nodes) / (self.config.area_width * self.config.area_height / 1e6),
            connected_components=connected_components,
            largest_component_size=largest_component,
            avg_node_degree=avg_degree,
            throughput_bps=sum(m.payload_size for m in delivered) / self.current_time if self.current_time > 0 else 0.0,
            energy_consumed_mah=total_energy,
            priority_delivery_rates=priority_rates
        )
    
    def _count_connected_components(self, topology: NetworkTopology) -> int:
        """Count connected components in the network."""
        visited = set()
        components = 0
        
        for node_id in topology.nodes:
            if node_id not in visited:
                self._dfs_component(node_id, topology, visited)
                components += 1
        
        return components
    
    def _dfs_component(self, node_id: str, topology: NetworkTopology, visited: Set[str]):
        """DFS to find connected component."""
        stack = [node_id]
        while stack:
            current = stack.pop()
            if current in visited:
                continue
            visited.add(current)
            for neighbor in topology.get_neighbors(current):
                if neighbor not in visited:
                    stack.append(neighbor)
    
    def _largest_component_size(self, topology: NetworkTopology) -> int:
        """Find size of largest connected component."""
        visited = set()
        max_size = 0
        
        for node_id in topology.nodes:
            if node_id not in visited:
                component = []
                self._dfs_collect(node_id, topology, visited, component)
                max_size = max(max_size, len(component))
        
        return max_size
    
    def _dfs_collect(self, node_id: str, topology: NetworkTopology, visited: Set[str], component: List[str]):
        """DFS to collect component nodes."""
        stack = [node_id]
        while stack:
            current = stack.pop()
            if current in visited:
                continue
            visited.add(current)
            component.append(current)
            for neighbor in topology.get_neighbors(current):
                if neighbor not in visited:
                    stack.append(neighbor)
    
    def _average_node_degree(self, topology: NetworkTopology) -> float:
        """Calculate average node degree."""
        if not topology.nodes:
            return 0.0
        total_degree = sum(len(topology.get_neighbors(n)) for n in topology.nodes)
        return total_degree / len(topology.nodes)
    
    def _log_event(self, event_type: str, data: Dict):
        """Log a simulation event."""
        self.event_log.append({
            "timestamp": self.current_time,
            "type": event_type,
            "data": data
        })
    
    def save_results(self, output_dir: str):
        """Save simulation results to files."""
        out_path = Path(output_dir)
        out_path.mkdir(parents=True, exist_ok=True)
        
        # Metrics CSV
        import pandas as pd
        df = pd.DataFrame([m.to_dict() for m in self.metrics_history])
        df.to_csv(out_path / "metrics.csv", index=False)
        
        # Events JSON
        with open(out_path / "events.json", "w") as f:
            json.dump(self.event_log, f, indent=2)
        
        # Final topology
        final_topo = self._create_topology_snapshot()
        topo_data = {
            "nodes": {k: {"position": v.position.to_tuple(), "battery": v.battery_pct, 
                          "alive": v.is_alive} for k, v in final_topo.nodes.items()},
            "links": {f"{k[0]}-{k[1]}": {"transport": v.transport.value, "signal": v.signal_strength_dbm,
                                          "quality": v.quality_score()} for k, v in final_topo.links.items()},
            "timestamp": final_topo.timestamp
        }
        with open(out_path / "final_topology.json", "w") as f:
            json.dump(topo_data, f, indent=2)
        
        # Message summary
        msg_data = []
        for msg in self.messages.values():
            msg_data.append({
                "id": msg.message_id,
                "sender": msg.sender_id,
                "recipient": msg.recipient_id,
                "priority": msg.priority.value,
                "type": msg.message_type.value,
                "hops": msg.hop_count,
                "delivered": msg.delivered_at is not None,
                "latency_ms": msg.latency_ms,
                "created": msg.created_at,
                "delivered_at": msg.delivered_at
            })
        pd.DataFrame(msg_data).to_csv(out_path / "messages.csv", index=False)
        
        print(f"Results saved to {out_path}")


def create_simulator(config: SimulationConfig) -> Simulator:
    """Factory function to create a simulator with all components."""
    propagation = create_propagation_model(
        config.propagation_model,
        frequency_mhz=2400,
        tx_power_dbm=10,
        rx_sensitivity_dbm=-90,
        path_loss_exponent=config.path_loss_exponent,
        shadowing_std_db=config.shadowing_std_db
    )
    
    mobility = create_mobility_model(
        config.node_configs[0].mobility_model if config.node_configs else "random_waypoint",
        min_speed=0.5, max_speed=2.0, pause_time=10.0,
        shelter_positions=[], rescue_team_ratio=0.1
    )
    
    routing = create_routing_protocol("hybrid")
    
    return Simulator(
        config=config,
        propagation=propagation,
        mobility=mobility,
        routing=routing
    )