//! Network topology management

use crate::bundle::PeerId;
use crate::neighbor::NeighborInfo;
use crate::model::{Location, TransportType};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use chrono::{DateTime, Utc};

/// Network topology snapshot
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkTopology {
    pub nodes: HashMap<PeerId, NodeInfo>,
    pub links: Vec<Link>,
    pub timestamp: DateTime<Utc>,
}

impl Default for NetworkTopology {
    fn default() -> Self {
        Self::new()
    }
}

impl NetworkTopology {
    pub fn new() -> Self {
        Self {
            nodes: HashMap::new(),
            links: Vec::new(),
            timestamp: Utc::now(),
        }
    }

    /// Update node information
    pub fn update_node(&mut self, peer_id: PeerId, neighbor: NeighborInfo) {
        let node = NodeInfo {
            peer_id: peer_id.clone(),
            display_name: neighbor.display_name,
            location: neighbor.location,
            battery_level: neighbor.battery_level,
            power_mode: neighbor.power_mode,
            transports: neighbor.transports,
            is_online: neighbor.is_online,
            trust_score: neighbor.trust_score,
            last_seen: neighbor.last_seen,
        };
        self.nodes.insert(peer_id.clone(), node);
    }

    /// Update link between two nodes
    pub fn update_link(&mut self, source: PeerId, target: PeerId, transport: TransportType, quality: f32, signal: i8) {
        // Remove existing link if present
        self.links.retain(|l| !(l.source == source && l.target == target && l.transport == transport));
        
        if quality > 0.0 {
            self.links.push(Link {
                source,
                target,
                transport,
                quality,
                signal_strength: signal,
                is_active: true,
                last_update: Utc::now(),
            });
        }
    }

    /// Check if there's a better path to destination via neighbors
    pub fn has_better_path(&self, from: &PeerId, destination: &PeerId) -> bool {
        // Check if destination is direct neighbor
        if self.links.iter().any(|l| l.source == *from && l.target == *destination && l.is_active) {
            return true;
        }

        // Check if any neighbor has route to destination
        // Simplified: check if destination node exists and is online
        self.nodes.get(destination).map(|n| n.is_online).unwrap_or(false)
    }

    /// Get neighbors of a node
    pub fn get_neighbors(&self, peer_id: &PeerId) -> Vec<&PeerId> {
        self.links
            .iter()
            .filter(|l| l.source == *peer_id && l.is_active)
            .map(|l| &l.target)
            .collect()
    }

    /// Get topology stats
    pub fn stats(&self) -> TopologyStats {
        let online_nodes = self.nodes.values().filter(|n| n.is_online).count();
        let active_links = self.links.iter().filter(|l| l.is_active).count();
        
        TopologyStats {
            total_nodes: self.nodes.len(),
            online_nodes,
            active_links,
            timestamp: self.timestamp,
        }
    }
}

/// Node information in topology
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NodeInfo {
    pub peer_id: PeerId,
    pub display_name: Option<String>,
    pub location: Option<crate::model::Location>,
    pub battery_level: Option<u8>,
    pub power_mode: Option<crate::model::PowerMode>,
    pub transports: Vec<TransportType>,
    pub is_online: bool,
    pub trust_score: f32,
    pub last_seen: DateTime<Utc>,
}

/// Link between two nodes
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Link {
    pub source: PeerId,
    pub target: PeerId,
    pub transport: TransportType,
    pub quality: f32,
    pub signal_strength: i8,
    pub is_active: bool,
    pub last_update: DateTime<Utc>,
}

/// Topology statistics
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TopologyStats {
    pub total_nodes: usize,
    pub online_nodes: usize,
    pub active_links: usize,
    pub timestamp: DateTime<Utc>,
}

/// Topology events for dissemination
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum TopologyEvent {
    NodeJoined(PeerId, NodeInfo),
    NodeLeft(PeerId),
    NodeUpdated(PeerId, NodeInfo),
    LinkAdded(Link),
    LinkRemoved(PeerId, PeerId, TransportType),
    LinkUpdated(Link),
}