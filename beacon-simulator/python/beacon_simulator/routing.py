"""
Routing protocols for the mesh network simulation.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Set, Tuple
import heapq
import random

from .models import Message, MessagePriority, NetworkTopology, NodeState, Position, TransportType


class RoutingProtocol(ABC):
    """Base class for routing protocols."""
    
    @abstractmethod
    def find_route(self, source_id: str, destination_id: Optional[str], 
                   topology: NetworkTopology, node_states: Dict[str, NodeState],
                   message: Message) -> List[str]:
        """Find route from source to destination. Returns list of node IDs (path)."""
        pass
    
    @abstractmethod
    def should_forward(self, node_id: str, message: Message, 
                       topology: NetworkTopology, node_states: Dict[str, NodeState]) -> bool:
        """Decide if node should forward this message."""
        pass


class FloodingProtocol(RoutingProtocol):
    """Simple flooding with TTL and duplicate detection."""
    
    def __init__(self, forwarding_probability: float = 1.0):
        self.forwarding_probability = forwarding_probability
        self.seen_messages: Dict[str, Set[str]] = {}  # node_id -> set of message_ids
    
    def find_route(self, source_id: str, destination_id: Optional[str], 
                   topology: NetworkTopology, node_states: Dict[str, NodeState],
                   message: Message) -> List[str]:
        # Flooding doesn't compute routes; returns empty (handled by should_forward)
        return []
    
    def should_forward(self, node_id: str, message: Message, 
                       topology: NetworkTopology, node_states: Dict[str, NodeState]) -> bool:
        # Check TTL
        if message.is_expired:
            return False
        
        # Check if already seen
        if node_id not in self.seen_messages:
            self.seen_messages[node_id] = set()
        
        if message.message_id in self.seen_messages[node_id]:
            return False
        
        self.seen_messages[node_id].add(message.message_id)
        
        # Probabilistic forwarding
        return random.random() < self.forwarding_probability


class ProbabilisticFloodingProtocol(RoutingProtocol):
    """Probabilistic flooding with distance-based probability."""
    
    def __init__(self, base_probability: float = 0.5, distance_factor: float = 0.1):
        self.base_probability = base_probability
        self.distance_factor = distance_factor
        self.seen_messages: Dict[str, Set[str]] = {}
    
    def find_route(self, source_id: str, destination_id: Optional[str], 
                   topology: NetworkTopology, node_states: Dict[str, NodeState],
                   message: Message) -> List[str]:
        return []
    
    def should_forward(self, node_id: str, message: Message, 
                       topology: NetworkTopology, node_states: Dict[str, NodeState]) -> bool:
        if message.is_expired:
            return False
        
        if node_id not in self.seen_messages:
            self.seen_messages[node_id] = set()
        
        if message.message_id in self.seen_messages[node_id]:
            return False
        
        self.seen_messages[node_id].add(message.message_id)
        
        # Higher probability for nodes closer to destination (if known)
        prob = self.base_probability
        
        if message.recipient_id and message.recipient_id in node_states:
            source_pos = node_states[node_id].position
            dest_pos = node_states[message.recipient_id].position
            dist = source_pos.distance_to(dest_pos)
            prob += self.distance_factor * max(0, 100 - dist) / 100
        
        # Priority boost
        if message.priority == MessagePriority.CRITICAL:
            prob = min(1.0, prob * 2.0)
        elif message.priority == MessagePriority.HIGH:
            prob = min(1.0, prob * 1.5)
        
        return random.random() < prob


class EpidemicRoutingProtocol(RoutingProtocol):
    """Epidemic routing (store-carry-forward) for DTN."""
    
    def __init__(self, buffer_size: int = 1000, replication_limit: int = 3):
        self.buffer_size = buffer_size
        self.replication_limit = replication_limit
        self.message_copies: Dict[str, int] = {}  # message_id -> copy count
        self.seen_messages: Dict[str, Set[str]] = {}
    
    def find_route(self, source_id: str, destination_id: Optional[str], 
                   topology: NetworkTopology, node_states: Dict[str, NodeState],
                   message: Message) -> List[str]:
        return []
    
    def should_forward(self, node_id: str, message: Message, 
                       topology: NetworkTopology, node_states: Dict[str, NodeState]) -> bool:
        if message.is_expired:
            return False
        
        # Check replication limit
        copies = self.message_copies.get(message.message_id, 0)
        if copies >= self.replication_limit:
            return False
        
        # Check if destination is direct neighbor
        if message.recipient_id and message.recipient_id in topology.get_neighbors(node_id):
            return True
        
        # Check if already seen
        if node_id not in self.seen_messages:
            self.seen_messages[node_id] = set()
        
        if message.message_id in self.seen_messages[node_id]:
            return False
        
        self.seen_messages[node_id].add(message.message_id)
        self.message_copies[message.message_id] = copies + 1
        
        # Forward to all neighbors (epidemic)
        return True


class GeographicRoutingProtocol(RoutingProtocol):
    """Geographic routing using position information (GPSR-like)."""
    
    def __init__(self, perimeter_mode: bool = True):
        self.perimeter_mode = perimeter_mode
        self.seen_messages: Dict[str, Set[str]] = {}
    
    def find_route(self, source_id: str, destination_id: Optional[str], 
                   topology: NetworkTopology, node_states: Dict[str, NodeState],
                   message: Message) -> List[str]:
        if not destination_id or destination_id not in node_states:
            return []
        
        if destination_id not in topology.get_neighbors(source_id):
            return []
        
        return [source_id, destination_id]
    
    def should_forward(self, node_id: str, message: Message, 
                       topology: NetworkTopology, node_states: Dict[str, NodeState]) -> bool:
        if message.is_expired:
            return False
        
        if not message.recipient_id or message.recipient_id not in node_states:
            # No destination info, flood probabilistically
            return random.random() < 0.3
        
        # Check if already seen
        if node_id not in self.seen_messages:
            self.seen_messages[node_id] = set()
        
        if message.message_id in self.seen_messages[node_id]:
            return False
        
        self.seen_messages[node_id].add(message.message_id)
        
        # Greedy forwarding: forward to neighbor closer to destination
        current_pos = node_states[node_id].position
        dest_pos = node_states[message.recipient_id].position
        current_dist = current_pos.distance_to(dest_pos)
        
        neighbors = topology.get_neighbors(node_id)
        better_neighbors = []
        
        for neighbor_id in neighbors:
            if neighbor_id not in node_states:
                continue
            neighbor_pos = node_states[neighbor_id].position
            neighbor_dist = neighbor_pos.distance_to(dest_pos)
            if neighbor_dist < current_dist:
                better_neighbors.append(neighbor_id)
        
        if better_neighbors:
            # Forward to best neighbor (closest to destination)
            best = min(better_neighbors, key=lambda n: node_states[n].position.distance_to(dest_pos))
            # In real implementation, we'd select this specific neighbor
            return True
        
        # No better neighbor - perimeter mode or drop
        if self.perimeter_mode:
            # Simplified: forward with low probability
            return random.random() < 0.1
        
        return False


class HybridRoutingProtocol(RoutingProtocol):
    """Hybrid: Geographic for known destinations, Epidemic for unknown."""
    
    def __init__(self):
        self.geographic = GeographicRoutingProtocol()
        self.epidemic = EpidemicRoutingProtocol(replication_limit=2)
        self.destination_known: Set[str] = set()
    
    def find_route(self, source_id: str, destination_id: Optional[str], 
                   topology: NetworkTopology, node_states: Dict[str, NodeState],
                   message: Message) -> List[str]:
        if destination_id and destination_id in node_states:
            self.destination_known.add(message.message_id)
            return self.geographic.find_route(source_id, destination_id, topology, node_states, message)
        return self.epidemic.find_route(source_id, destination_id, topology, node_states, message)
    
    def should_forward(self, node_id: str, message: Message, 
                       topology: NetworkTopology, node_states: Dict[str, NodeState]) -> bool:
        if message.message_id in self.destination_known:
            return self.geographic.should_forward(node_id, message, topology, node_states)
        return self.epidemic.should_forward(node_id, message, topology, node_states)


class PriorityAwareRoutingProtocol(RoutingProtocol):
    """Priority-aware routing: CRITICAL messages get preferential treatment."""
    
    def __init__(self, base_protocol: RoutingProtocol):
        self.base_protocol = base_protocol
        self.priority_queues: Dict[str, Dict[MessagePriority, List[Message]]] = {}
    
    def find_route(self, source_id: str, destination_id: Optional[str], 
                   topology: NetworkTopology, node_states: Dict[str, NodeState],
                   message: Message) -> List[str]:
        return self.base_protocol.find_route(source_id, destination_id, topology, node_states, message)
    
    def should_forward(self, node_id: str, message: Message, 
                       topology: NetworkTopology, node_states: Dict[str, NodeState]) -> bool:
        # CRITICAL messages always forwarded if not expired
        if message.priority == MessagePriority.CRITICAL and not message.is_expired:
            if node_id not in self.priority_queues:
                self.priority_queues[node_id] = {p: [] for p in MessagePriority}
            self.priority_queues[node_id][message.priority].append(message)
            return True
        
        # For other priorities, use base protocol with priority weighting
        return self.base_protocol.should_forward(node_id, message, topology, node_states)
    
    def get_next_message(self, node_id: str) -> Optional[Message]:
        """Get highest priority message from queue."""
        if node_id not in self.priority_queues:
            return None
        
        for priority in [MessagePriority.CRITICAL, MessagePriority.HIGH, 
                         MessagePriority.NORMAL, MessagePriority.LOW]:
            queue = self.priority_queues[node_id][priority]
            if queue:
                return queue.pop(0)
        return None


def create_routing_protocol(protocol_type: str, **kwargs) -> RoutingProtocol:
    """Factory function to create routing protocols."""
    if protocol_type == "flooding":
        return FloodingProtocol(kwargs.get("forwarding_probability", 1.0))
    elif protocol_type == "probabilistic":
        return ProbabilisticFloodingProtocol(
            kwargs.get("base_probability", 0.5),
            kwargs.get("distance_factor", 0.1)
        )
    elif protocol_type == "epidemic":
        return EpidemicRoutingProtocol(
            kwargs.get("buffer_size", 1000),
            kwargs.get("replication_limit", 3)
        )
    elif protocol_type == "geographic":
        return GeographicRoutingProtocol(kwargs.get("perimeter_mode", True))
    elif protocol_type == "hybrid":
        return HybridRoutingProtocol()
    elif protocol_type == "priority_aware":
        base = create_routing_protocol(kwargs.get("base", "probabilistic"), **kwargs)
        return PriorityAwareRoutingProtocol(base)
    else:
        raise ValueError(f"Unknown routing protocol: {protocol_type}")