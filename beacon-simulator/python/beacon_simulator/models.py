"""
Core data models for the Beacon network simulator.
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional, Set, Tuple
import numpy as np


class TransportType(str, Enum):
    """Supported transport types in the simulation."""
    BLE = "ble"
    WIFI_DIRECT = "wifi_direct"
    LORA = "lora"
    BLUETOOTH_CLASSIC = "bluetooth_classic"


class PowerMode(str, Enum):
    """Device power management modes."""
    NORMAL = "normal"
    CONSERVATION = "conservation"
    SURVIVAL = "survival"
    CRITICAL = "critical"


class MessagePriority(str, Enum):
    """Message priority levels."""
    CRITICAL = "critical"
    HIGH = "high"
    NORMAL = "normal"
    LOW = "low"


class MessageType(str, Enum):
    """Types of messages in the network."""
    TEXT = "text"
    LOCATION = "location"
    TELEMETRY = "telemetry"
    SOS = "sos"
    ACKNOWLEDGMENT = "ack"
    RESOURCE_REPORT = "resource_report"
    ALERT = "alert"
    MAP_TILE = "map_tile"
    VOICE_NOTE = "voice_note"
    IMAGE = "image"


@dataclass(frozen=True)
class Position:
    """2D position with optional altitude."""
    x: float
    y: float
    z: float = 0.0

    def distance_to(self, other: Position) -> float:
        return np.sqrt((self.x - other.x)**2 + (self.y - other.y)**2 + (self.z - other.z)**2)

    def to_tuple(self) -> Tuple[float, float, float]:
        return (self.x, self.y, self.z)


@dataclass
class NodeConfig:
    """Configuration for a simulated node."""
    node_id: str = field(default_factory=lambda: str(uuid.uuid4())[:8])
    position: Position = field(default_factory=lambda: Position(0.0, 0.0))
    battery_capacity_mah: float = 5000.0  # mAh
    initial_battery_pct: float = 100.0
    power_mode: PowerMode = PowerMode.NORMAL
    transports: Set[TransportType] = field(default_factory=lambda: {TransportType.BLE, TransportType.WIFI_DIRECT})
    radio_range_ble: float = 50.0  # meters
    radio_range_wifi: float = 100.0
    radio_range_lora: float = 2000.0
    tx_power_dbm: float = 10.0
    rx_sensitivity_dbm: float = -90.0
    scan_interval_ms: int = 2000
    scan_window_ms: int = 200
    advertise_interval_ms: int = 500
    message_queue_size: int = 1000
    mobility_model: Optional[str] = None
    mobility_params: Dict = field(default_factory=dict)


@dataclass
class Message:
    """A message in the simulation."""
    message_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    sender_id: str
    recipient_id: Optional[str] = None  # None = broadcast
    timestamp: float = 0.0
    priority: MessagePriority = MessagePriority.NORMAL
    ttl: int = 5
    hop_count: int = 0
    payload_size: int = 100  # bytes
    message_type: MessageType = MessageType.TEXT
    path: List[str] = field(default_factory=list)  # Node IDs traversed
    created_at: float = 0.0
    delivered_at: Optional[float] = None
    acknowledged_at: Optional[float] = None
    dropped: bool = False
    drop_reason: Optional[str] = None

    @property
    def is_broadcast(self) -> bool:
        return self.recipient_id is None

    @property
    def is_expired(self) -> bool:
        return self.hop_count >= self.ttl

    @property
    def latency_ms(self) -> Optional[float]:
        if self.delivered_at and self.created_at:
            return (self.delivered_at - self.created_at) * 1000
        return None


@dataclass
class Link:
    """A communication link between two nodes."""
    source_id: str
    target_id: str
    transport: TransportType
    signal_strength_dbm: float
    snr_db: float
    bandwidth_bps: float
    latency_ms: float
    packet_loss_rate: float
    established_at: float
    last_active: float
    is_active: bool = True

    def quality_score(self) -> float:
        """Compute link quality 0-1 based on signal and loss."""
        # Normalize signal strength (-100 to -30 dBm -> 0 to 1)
        signal_norm = max(0.0, min(1.0, (self.signal_strength_dbm + 100) / 70))
        # Invert packet loss
        loss_score = 1.0 - self.packet_loss_rate
        return (signal_norm + loss_score) / 2


@dataclass
class NetworkTopology:
    """Current network topology snapshot."""
    nodes: Dict[str, 'NodeState']
    links: Dict[Tuple[str, str], Link]
    timestamp: float

    def get_neighbors(self, node_id: str) -> List[str]:
        """Get direct neighbors of a node."""
        neighbors = []
        for (src, dst), link in self.links.items():
            if link.is_active:
                if src == node_id:
                    neighbors.append(dst)
                elif dst == node_id:
                    neighbors.append(src)
        return neighbors


@dataclass
class NodeState:
    """Runtime state of a simulated node."""
    config: NodeConfig
    position: Position
    battery_pct: float
    power_mode: PowerMode
    is_alive: bool = True
    message_queue: List[Message] = field(default_factory=list)
    sent_messages: List[Message] = field(default_factory=list)
    received_messages: List[Message] = field(default_factory=list)
    forwarded_messages: List[Message] = field(default_factory=list)
    dropped_messages: List[Message] = field(default_factory=list)
    neighbors: Dict[str, Link] = field(default_factory=dict)
    last_scan: float = 0.0
    last_advertise: float = 0.0
    total_tx_bytes: int = 0
    total_rx_bytes: int = 0
    total_tx_time_ms: float = 0.0
    total_rx_time_ms: float = 0.0
    sleep_time_ms: float = 0.0
    active_time_ms: float = 0.0


@dataclass
class SimulationConfig:
    """Global simulation configuration."""
    duration_seconds: float = 3600.0  # 1 hour default
    time_step_ms: float = 100.0  # Simulation tick rate
    area_width: float = 1000.0  # meters
    area_height: float = 1000.0
    num_nodes: int = 50
    node_configs: List[NodeConfig] = field(default_factory=list)
    mobility_enabled: bool = True
    propagation_model: str = "log_distance"  # "free_space", "log_distance", "two_ray"
    path_loss_exponent: float = 3.5
    shadowing_std_db: float = 4.0
    enable_collisions: bool = True
    enable_interference: bool = True
    random_seed: Optional[int] = None
    output_dir: str = "output"
    metrics_interval_seconds: float = 60.0
    save_topology_snapshots: bool = True
    snapshot_interval_seconds: float = 300.0


@dataclass
class SimulationMetrics:
    """Aggregated simulation metrics."""
    timestamp: float
    num_nodes_alive: int
    num_nodes_dead: int
    total_messages_created: int
    total_messages_delivered: int
    total_messages_dropped: int
    total_messages_pending: int
    delivery_rate: float
    avg_latency_ms: float
    avg_hop_count: float
    avg_battery_pct: float
    network_density: float  # nodes per km^2
    connected_components: int
    largest_component_size: int
    avg_node_degree: float
    throughput_bps: float
    energy_consumed_mah: float
    priority_delivery_rates: Dict[str, float] = field(default_factory=dict)

    def to_dict(self) -> Dict:
        return {
            "timestamp": self.timestamp,
            "nodes_alive": self.num_nodes_alive,
            "nodes_dead": self.num_nodes_dead,
            "messages_created": self.total_messages_created,
            "messages_delivered": self.total_messages_delivered,
            "messages_dropped": self.total_messages_dropped,
            "messages_pending": self.total_messages_pending,
            "delivery_rate": self.delivery_rate,
            "avg_latency_ms": self.avg_latency_ms,
            "avg_hop_count": self.avg_hop_count,
            "avg_battery_pct": self.avg_battery_pct,
            "network_density": self.network_density,
            "connected_components": self.connected_components,
            "largest_component_size": self.largest_component_size,
            "avg_node_degree": self.avg_node_degree,
            "throughput_bps": self.throughput_bps,
            "energy_consumed_mah": self.energy_consumed_mah,
            "priority_delivery_rates": self.priority_delivery_rates,
        }


@dataclass
class Scenario:
    """A simulation scenario definition."""
    name: str
    description: str
    config: SimulationConfig
    events: List[Dict] = field(default_factory=list)  # Scheduled events
    mobility_patterns: Dict[str, Dict] = field(default_factory=dict)
    traffic_patterns: Dict[str, Dict] = field(default_factory=dict)