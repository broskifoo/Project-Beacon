//! Routing table and algorithms

use crate::bundle::{Bundle, PeerId};
use crate::model::MessagePriority;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use chrono::{DateTime, Utc};

/// Route types
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum RouteType {
    Direct,
    Geographic,
    Epidemic,
    Manual,
}

/// Routing table entry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RoutingEntry {
    pub destination: PeerId,
    pub next_hop: PeerId,
    pub hop_count: u8,
    pub quality: f32,
    pub route_type: RouteType,
    pub last_update: DateTime<Utc>,
    pub expires_at: DateTime<Utc>,
    pub metrics: RouteMetrics,
}

/// Route quality metrics
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct RouteMetrics {
    pub success_count: u64,
    pub failure_count: u64,
    pub avg_latency_ms: f64,
    pub avg_hop_count: f32,
    pub last_used: Option<DateTime<Utc>>,
}

/// Routing table
#[derive(Debug, Default, Serialize, Deserialize)]
pub struct RoutingTable {
    routes: HashMap<PeerId, RoutingEntry>,
    seen_bundles: HashMap<BundleId, DateTime<Utc>>,
    max_seen_bundles: usize,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct BundleId {
    pub value: String,
}

impl RoutingTable {
    pub fn new() -> Self {
        Self {
            routes: HashMap::new(),
            seen_bundles: HashMap::new(),
            max_seen_bundles: 10000,
        }
    }

    /// Add or update a route
    pub fn add_route(&mut self, entry: RoutingEntry) {
        self.routes.insert(entry.destination.clone(), entry);
    }

    /// Get route to destination
    pub fn get_route(&self, destination: &PeerId) -> Option<&RoutingEntry> {
        self.routes.get(destination)
    }

    /// Remove a route
    pub fn remove_route(&mut self, destination: &PeerId) -> Option<RoutingEntry> {
        self.routes.remove(destination)
    }

    /// Get all routes
    pub fn all_routes(&self) -> Vec<&RoutingEntry> {
        self.routes.values().collect()
    }

    /// Check if bundle has been seen (duplicate detection)
    pub fn has_seen(&self, bundle_id: &BundleId) -> bool {
        self.seen_bundles.contains_key(bundle_id)
    }

    /// Mark bundle as seen
    pub fn mark_seen(&mut self, bundle_id: BundleId) {
        if self.seen_bundles.len() >= self.max_seen_bundles {
            // Remove oldest entries
            let keys: Vec<_> = self.seen_bundles.keys().cloned().take(1000).collect();
            for key in keys {
                self.seen_bundles.remove(&key);
            }
        }
        self.seen_bundles.insert(bundle_id, Utc::now());
    }

    /// Clean up expired routes and seen bundles
    pub fn cleanup_expired(&mut self, now: DateTime<Utc>) {
        self.routes.retain(|_, entry| entry.expires_at > now);
        self.seen_bundles.retain(|_, ts| *ts > now - chrono::Duration::hours(24));
    }

    /// Update route metrics on success
    pub fn record_success(&mut self, destination: &PeerId, latency_ms: f64, hops: u8) {
        if let Some(entry) = self.routes.get_mut(destination) {
            entry.metrics.success_count += 1;
            entry.metrics.avg_latency_ms = 
                (entry.metrics.avg_latency_ms * entry.metrics.success_count as f64 + latency_ms) 
                / (entry.metrics.success_count + 1) as f64;
            entry.metrics.avg_hop_count = 
                (entry.metrics.avg_hop_count * entry.metrics.success_count as f32 + hops as f32) 
                / (entry.metrics.success_count + 1) as f32;
            entry.metrics.last_used = Some(Utc::now());
        }
    }

    /// Update route metrics on failure
    pub fn record_failure(&mut self, destination: &PeerId) {
        if let Some(entry) = self.routes.get_mut(destination) {
            entry.metrics.failure_count += 1;
        }
    }
}

/// Routing protocol trait
pub trait RoutingProtocol {
    /// Find next hops for a bundle
    fn find_next_hops(
        &self,
        bundle: &Bundle,
        neighbors: &[crate::neighbor::NeighborInfo],
        from_peer: &PeerId,
    ) -> Vec<PeerId>;

    /// Should forward this bundle?
    fn should_forward(
        &self,
        bundle: &Bundle,
        neighbors: &[crate::neighbor::NeighborInfo],
        from_peer: &PeerId,
    ) -> bool;
}