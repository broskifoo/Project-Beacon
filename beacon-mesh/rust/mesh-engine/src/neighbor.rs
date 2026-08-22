//! Neighbor management

use crate::bundle::PeerId;
use crate::model::{Location, PowerMode, TransportType};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use chrono::{DateTime, Utc, Duration};

/// Neighbor information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NeighborInfo {
    pub peer_id: PeerId,
    pub display_name: Option<String>,
    pub last_seen: DateTime<Utc>,
    pub location: Option<Location>,
    pub battery_level: Option<u8>,
    pub power_mode: Option<PowerMode>,
    pub transports: Vec<TransportType>,
    pub link_quality: f32,
    pub signal_strength: Option<i8>, // dBm
    pub is_online: bool,
    pub trust_score: f32,
}

/// Neighbor table with timeout-based cleanup
#[derive(Debug, Serialize, Deserialize)]
pub struct NeighborTable {
    neighbors: HashMap<PeerId, NeighborInfo>,
    timeout_seconds: u64,
}

impl NeighborTable {
    pub fn new(timeout_seconds: u64) -> Self {
        Self {
            neighbors: HashMap::new(),
            timeout_seconds,
        }
    }

    /// Update or add neighbor
    pub fn update(&mut self, neighbor: NeighborInfo) {
        self.neighbors.insert(neighbor.peer_id.clone(), neighbor);
    }

    /// Get neighbor by ID
    pub fn get(&self, peer_id: &PeerId) -> Option<&NeighborInfo> {
        self.neighbors.get(peer_id)
    }

    /// Get mutable neighbor
    pub fn get_mut(&mut self, peer_id: &PeerId) -> Option<&mut NeighborInfo> {
        self.neighbors.get_mut(peer_id)
    }

    /// Remove neighbor
    pub fn remove(&mut self, peer_id: &PeerId) -> Option<NeighborInfo> {
        self.neighbors.remove(peer_id)
    }

    /// Get all active neighbors
    pub fn get_active_neighbors(&self) -> Vec<NeighborInfo> {
        let now = Utc::now();
        let timeout = Duration::seconds(self.timeout_seconds as i64);
        
        self.neighbors
            .values()
            .filter(|n| n.is_online && (now - n.last_seen) < timeout)
            .cloned()
            .collect()
    }

    /// Get all neighbors (including stale)
    pub fn all_neighbors(&self) -> Vec<&NeighborInfo> {
        self.neighbors.values().collect()
    }

    /// Get neighbor count
    pub fn count(&self) -> usize {
        self.neighbors.len()
    }

    /// Mark neighbor as offline
    pub fn mark_offline(&mut self, peer_id: &PeerId) {
        if let Some(n) = self.neighbors.get_mut(peer_id) {
            n.is_online = false;
        }
    }

    /// Clean up stale neighbors
    pub fn cleanup_stale(&mut self) {
        let now = Utc::now();
        let timeout = Duration::seconds(self.timeout_seconds as i64);
        
        self.neighbors.retain(|_, n| {
            n.is_online && (now - n.last_seen) < timeout
        });
    }

    /// Update link quality for neighbor
    pub fn update_link_quality(&mut self, peer_id: &PeerId, quality: f32, signal: Option<i8>) {
        if let Some(n) = self.neighbors.get_mut(peer_id) {
            n.link_quality = quality.clamp(0.0, 1.0);
            n.signal_strength = signal;
            n.last_seen = Utc::now();
            n.is_online = true;
        }
    }

    /// Get neighbors by transport type
    pub fn by_transport(&self, transport: TransportType) -> Vec<&NeighborInfo> {
        self.neighbors
            .values()
            .filter(|n| n.transports.contains(&transport))
            .collect()
    }

    /// Get best neighbor for destination (geographic routing)
    pub fn best_toward(&self, destination: &Location) -> Option<&NeighborInfo> {
        self.get_active_neighbors()
            .into_iter()
            .filter(|n| n.location.is_some())
            .min_by(|a, b| {
                let a_loc = a.location.as_ref().unwrap();
                let b_loc = b.location.as_ref().unwrap();
                let a_dist = haversine_distance(a_loc, destination);
                let b_dist = haversine_distance(b_loc, destination);
                a_dist.partial_cmp(&b_dist).unwrap()
            })
    }
}

/// Haversine distance between two locations (meters)
fn haversine_distance(a: &Location, b: &Location) -> f64 {
    const R: f64 = 6371000.0; // Earth radius in meters
    
    let lat1 = a.latitude.to_radians();
    let lat2 = b.latitude.to_radians();
    let dlat = (b.latitude - a.latitude).to_radians();
    let dlon = (b.longitude - a.longitude).to_radians();
    
    let a = (dlat / 2.0).sin().powi(2) 
        + lat1.cos() * lat2.cos() * (dlon / 2.0).sin().powi(2);
    let c = 2.0 * a.sqrt().atan2((1.0 - a).sqrt());
    
    R * c
}