//! Bundle types and management

use crate::model::{MessagePriority, PeerId, Location};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use uuid::Uuid;
use chrono::{DateTime, Utc};

/// Unique bundle identifier
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct BundleId {
    pub value: String,
}

impl BundleId {
    pub fn new() -> Self {
        Self {
            value: Uuid::now_v7().to_string(),
        }
    }

    pub fn from_string(s: String) -> Self {
        Self { value: s }
    }
}

impl Default for BundleId {
    fn default() -> Self {
        Self::new()
    }
}

/// Bundle routing flags
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct RoutingFlags {
    pub is_fragment: bool,
    pub fragment_index: u16,
    pub fragment_count: u16,
    pub request_ack: bool,
    pub report_delivery: bool,
}

/// Custody transfer record
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CustodyTransfer {
    pub node_id: PeerId,
    pub timestamp: DateTime<Utc>,
    pub accepted: bool,
}

/// Bundle status
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum BundleStatus {
    Outbox,
    InTransit,
    Inbox,
    Delivered,
    Acknowledged,
    Expired,
    Failed,
}

/// Main bundle structure (DTN bundle)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Bundle {
    pub id: BundleId,
    pub source: PeerId,
    pub destination: Option<PeerId>,
    pub creation_timestamp: DateTime<Utc>,
    pub expiration_timestamp: DateTime<Utc>,
    pub priority: MessagePriority,
    pub payload: Vec<u8>,
    pub routing_flags: RoutingFlags,
    pub hop_count: u8,
    pub max_hops: u8,
    pub custody_transfers: Vec<CustodyTransfer>,
    pub status: BundleStatus,
    pub route_path: Vec<PeerId>,
    pub metadata: HashMap<String, String>,
}

impl Bundle {
    /// Create a new bundle
    pub fn new(
        source: PeerId,
        destination: Option<PeerId>,
        payload: Vec<u8>,
        priority: MessagePriority,
        max_hops: u8,
        ttl_seconds: u64,
    ) -> Self {
        let now = Utc::now();
        Self {
            id: BundleId::new(),
            source,
            destination,
            creation_timestamp: now,
            expiration_timestamp: now + chrono::Duration::seconds(ttl_seconds as i64),
            priority,
            payload,
            routing_flags: RoutingFlags::default(),
            hop_count: 0,
            max_hops,
            custody_transfers: Vec::new(),
            status: BundleStatus::Outbox,
            route_path: Vec::new(),
            metadata: HashMap::new(),
        }
    }

    /// Check if bundle is expired
    pub fn is_expired(&self) -> bool {
        Utc::now() >= self.expiration_timestamp
    }

    /// Check if bundle is for local delivery
    pub fn is_for_local(&self, local_id: &PeerId) -> bool {
        self.destination.as_ref() == Some(local_id) || self.destination.is_none()
    }

    /// Get remaining TTL in seconds
    pub fn remaining_ttl(&self) -> i64 {
        (self.expiration_timestamp - Utc::now()).num_seconds().max(0)
    }

    /// Add hop to route path
    pub fn add_hop(&mut self, peer_id: PeerId) {
        self.hop_count += 1;
        self.route_path.push(peer_id);
    }

    /// Mark as delivered
    pub fn mark_delivered(&mut self) {
        self.status = BundleStatus::Delivered;
    }

    /// Mark as acknowledged
    pub fn mark_acknowledged(&mut self) {
        self.status = BundleStatus::Acknowledged;
    }
}