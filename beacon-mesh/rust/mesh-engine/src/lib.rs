//! Beacon Mesh Routing Engine
//! 
//! Core routing logic for delay-tolerant mesh networking.

pub mod bundle;
pub mod routing;
pub mod neighbor;
pub mod topology;
pub mod custody;
pub mod scheduler;

pub use bundle::{Bundle, BundleId, RoutingFlags, CustodyTransfer, BundleStatus};
pub use routing::{RoutingTable, RoutingEntry, RouteType, RoutingProtocol};
pub use neighbor::{NeighborTable, NeighborInfo, LinkQuality};
pub use topology::{NetworkTopology, TopologyEvent, TopologySnapshot};
pub use custody::{CustodyManager, CustodyState};
pub use scheduler::{TaskScheduler, ScheduledTask, TaskHandle};

use crate::bundle::Bundle;
use crate::neighbor::NeighborInfo;
use crate::routing::RoutingEntry;
use crate::topology::NetworkTopology;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

/// Main mesh engine coordinating all mesh operations
pub struct MeshEngine {
    routing_table: Arc<RwLock<RoutingTable>>,
    neighbor_table: Arc<RwLock<NeighborTable>>,
    topology: Arc<RwLock<NetworkTopology>>,
    custody_manager: Arc<CustodyManager>,
    scheduler: Arc<TaskScheduler>,
    config: MeshConfig,
}

/// Mesh engine configuration
#[derive(Debug, Clone)]
pub struct MeshConfig {
    pub max_hops: u8,
    pub default_ttl_seconds: u64,
    pub bundle_buffer_size: usize,
    pub neighbor_timeout_seconds: u64,
    pub topology_broadcast_interval_seconds: u64,
    pub enable_geographic_routing: bool,
    pub enable_epidemic_routing: bool,
    pub forwarding_probability: f32,
}

impl Default for MeshConfig {
    fn default() -> Self {
        Self {
            max_hops: 5,
            default_ttl_seconds: 3600,
            bundle_buffer_size: 1000,
            neighbor_timeout_seconds: 300,
            topology_broadcast_interval_seconds: 30,
            enable_geographic_routing: true,
            enable_epidemic_routing: true,
            forwarding_probability: 0.5,
        }
    }
}

impl MeshEngine {
    /// Create a new mesh engine
    pub fn new(config: MeshConfig) -> Self {
        Self {
            routing_table: Arc::new(RwLock::new(RoutingTable::new())),
            neighbor_table: Arc::new(RwLock::new(NeighborTable::new(config.neighbor_timeout_seconds))),
            topology: Arc::new(RwLock::new(NetworkTopology::new())),
            custody_manager: Arc::new(CustodyManager::new()),
            scheduler: Arc::new(TaskScheduler::new()),
            config,
        }
    }

    /// Process an incoming bundle from a neighbor
    pub async fn receive_bundle(
        &self,
        bundle: Bundle,
        from_peer: &crate::bundle::PeerId,
        link_quality: f32,
    ) -> Result<ReceiveResult, MeshError> {
        // Check duplicate
        if self.routing_table.read().await.has_seen(&bundle.id) {
            return Ok(ReceiveResult::Duplicate);
        }

        // Mark as seen
        self.routing_table.write().await.mark_seen(bundle.id.clone());

        // Check TTL
        if bundle.hop_count >= bundle.max_hops {
            return Ok(ReceiveResult::TtlExpired);
        }

        // Check if for us
        if bundle.destination.as_ref() == Some(&self.local_peer_id()) {
            // Deliver locally
            self.custody_manager.accept_custody(bundle.id.clone(), self.local_peer_id()).await?;
            return Ok(ReceiveResult::Delivered);
        }

        // Forwarding decision
        let should_forward = self.should_forward(&bundle, from_peer, link_quality).await?;
        
        if should_forward {
            // Select next hops
            let next_hops = self.select_next_hops(&bundle, from_peer).await?;
            
            // Update bundle
            let mut forwarded_bundle = bundle;
            forwarded_bundle.hop_count += 1;
            forwarded_bundle.routing_flags.is_fragment = false;
            
            return Ok(ReceiveResult::Forward { 
                bundle: forwarded_bundle, 
                next_hops 
            });
        }

        Ok(ReceiveResult::Dropped)
    }

    /// Determine if bundle should be forwarded
    async fn should_forward(
        &self,
        bundle: &Bundle,
        from_peer: &crate::bundle::PeerId,
        link_quality: f32,
    ) -> Result<bool, MeshError> {
        // CRITICAL always forwarded
        if bundle.priority == crate::model::MessagePriority::CRITICAL {
            return Ok(true);
        }

        // Don't forward back to sender
        if bundle.source == *from_peer {
            return Ok(false);
        }

        // Probabilistic forwarding
        let mut probability = self.config.forwarding_probability;
        
        // Boost for HIGH priority
        if bundle.priority == crate::model::MessagePriority::HIGH {
            probability = (probability * 1.5).min(1.0);
        }

        // Reduce for LOW priority
        if bundle.priority == crate::model::MessagePriority::LOW {
            probability *= 0.1;
        }

        // Geographic routing boost if destination known
        if bundle.destination.is_some() && self.config.enable_geographic_routing {
            if self.has_better_neighbor(bundle.destination.as_ref().unwrap()).await? {
                probability = (probability * 2.0).min(1.0);
            }
        }

        // Random decision
        use rand::Rng;
        Ok(rand::rng().random::<f32>() < probability)
    }

    /// Select next hop peers for forwarding
    async fn select_next_hops(
        &self,
        bundle: &Bundle,
        from_peer: &crate::bundle::PeerId,
    ) -> Result<Vec<crate::bundle::PeerId>, MeshError> {
        let neighbors = self.neighbor_table.read().await.get_active_neighbors();
        
        // Filter out sender
        let mut candidates: Vec<_> = neighbors
            .into_iter()
            .filter(|n| n.peer_id != *from_peer)
            .collect();

        if candidates.is_empty() {
            return Ok(vec![]);
        }

        // Sort by link quality (descending)
        candidates.sort_by(|a, b| b.link_quality.partial_cmp(&a.link_quality).unwrap());

        // Select top N (probabilistic flooding)
        let max_forwards = (candidates.len() as f32 * self.config.forwarding_probability).ceil() as usize;
        let max_forwards = max_forwards.max(1).min(3); // At least 1, at most 3

        Ok(candidates.into_iter()
            .take(max_forwards)
            .map(|n| n.peer_id)
            .collect())
    }

    /// Check if we have a neighbor closer to destination
    async fn has_better_neighbor(&self, destination: &crate::bundle::PeerId) -> Result<bool, MeshError> {
        let topology = self.topology.read().await;
        Ok(topology.has_better_path(self.local_peer_id(), destination))
    }

    /// Get local peer ID
    fn local_peer_id(&self) -> crate::bundle::PeerId {
        // TODO: Get from identity service
        crate::bundle::PeerId::from("local")
    }

    /// Update neighbor information
    pub async fn update_neighbor(&self, neighbor: NeighborInfo) -> Result<(), MeshError> {
        self.neighbor_table.write().await.update(neighbor.clone());
        self.update_topology_from_neighbor(&neighbor).await;
        Ok(())
    }

    /// Update topology from neighbor info
    async fn update_topology_from_neighbor(&self, neighbor: &NeighborInfo) {
        let mut topology = self.topology.write().await;
        topology.update_node(neighbor.peer_id.clone(), neighbor.clone());
    }

    /// Get current topology snapshot
    pub async fn get_topology(&self) -> NetworkTopology {
        self.topology.read().await.clone()
    }

    /// Get routing table
    pub async fn get_routing_table(&self) -> RoutingTable {
        self.routing_table.read().await.clone()
    }

    /// Start background tasks
    pub async fn start(&self) -> Result<(), MeshError> {
        // Periodic topology broadcast
        self.scheduler.schedule_periodic(
            "topology_broadcast",
            std::time::Duration::from_secs(self.config.topology_broadcast_interval_seconds),
            || async {
                // Broadcast topology to neighbors
            }
        ).await?;

        // Neighbor cleanup
        self.scheduler.schedule_periodic(
            "neighbor_cleanup",
            std::time::Duration::from_secs(60),
            || async {
                // Remove stale neighbors
            }
        ).await?;

        Ok(())
    }

    /// Stop the engine
    pub async fn stop(&self) -> Result<(), MeshError> {
        self.scheduler.shutdown().await?;
        Ok(())
    }
}

/// Result of receiving a bundle
#[derive(Debug, Clone, PartialEq)]
pub enum ReceiveResult {
    Delivered,
    Forward { bundle: Bundle, next_hops: Vec<crate::bundle::PeerId> },
    Duplicate,
    TtlExpired,
    Dropped,
}

/// Mesh engine errors
#[derive(Debug, thiserror::Error)]
pub enum MeshError {
    #[error("Bundle not found: {0}")]
    BundleNotFound(String),
    
    #[error("Neighbor not found: {0}")]
    NeighborNotFound(String),
    
    #[error("Routing error: {0}")]
    RoutingError(String),
    
    #[error("Storage error: {0}")]
    StorageError(String),
    
    #[error("Crypto error: {0}")]
    CryptoError(String),
    
    #[error("Scheduler error: {0}")]
    SchedulerError(String),
    
    #[error("Serialization error: {0}")]
    SerializationError(String),
}