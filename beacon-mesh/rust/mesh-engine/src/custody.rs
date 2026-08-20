//! Custody transfer management (DTN)

use crate::bundle::{BundleId, PeerId};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use chrono::{DateTime, Utc};
use tokio::sync::RwLock;

/// Custody state for a bundle
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CustodyState {
    pub bundle_id: BundleId,
    pub custodian: PeerId,
    pub accepted_at: DateTime<Utc>,
    pub expires_at: DateTime<Utc>,
    pub retransmission_count: u8,
    pub next_retry: DateTime<Utc>,
}

/// Custody manager for DTN-style store-and-forward
pub struct CustodyManager {
    custody_map: RwLock<HashMap<BundleId, CustodyState>>,
    max_retransmissions: u8,
    custody_timeout_seconds: u64,
}

impl Default for CustodyManager {
    fn default() -> Self {
        Self::new()
    }
}

impl CustodyManager {
    pub fn new() -> Self {
        Self {
            custody_map: RwLock::new(HashMap::new()),
            max_retransmissions: 3,
            custody_timeout_seconds: 3600,
        }
    }

    pub async fn accept_custody(&self, bundle_id: BundleId, node_id: PeerId) -> Result<(), CustodyError> {
        let mut map = self.custody_map.write().await;
        let now = Utc::now();
        
        map.insert(bundle_id.clone(), CustodyState {
            bundle_id: bundle_id.clone(),
            custodian: node_id,
            accepted_at: now,
            expires_at: now + chrono::Duration::seconds(self.custody_timeout_seconds as i64),
            retransmission_count: 0,
            next_retry: now + chrono::Duration::seconds(30),
        });
        
        Ok(())
    }

    pub async fn release_custody(&self, bundle_id: &BundleId) -> Result<(), CustodyError> {
        let mut map = self.custody_map.write().await;
        map.remove(bundle_id);
        Ok(())
    }

    pub async fn get_custody(&self, bundle_id: &BundleId) -> Option<CustodyState> {
        let map = self.custody_map.read().await;
        map.get(bundle_id).cloned()
    }

    pub async fn has_custody(&self, bundle_id: &BundleId, node_id: &PeerId) -> bool {
        let map = self.custody_map.read().await;
        map.get(bundle_id)
            .map(|s| s.custodian == *node_id && s.expires_at > Utc::now())
            .unwrap_or(false)
    }

    pub async fn get_due_for_retry(&self) -> Vec<BundleId> {
        let map = self.custody_map.read().await;
        let now = Utc::now();
        
        map.iter()
            .filter(|(_, state)| state.next_retry <= now && state.retransmission_count < self.max_retransmissions)
            .map(|(id, _)| id.clone())
            .collect()
    }

    pub async fn record_retransmission(&self, bundle_id: &BundleId) -> Result<(), CustodyError> {
        let mut map = self.custody_map.write().await;
        
        if let Some(state) = map.get_mut(bundle_id) {
            state.retransmission_count += 1;
            state.next_retry = Utc::now() + chrono::Duration::seconds(
                (30 * 2_u64.pow(state.retransmission_count as u32)) as i64
            );
            Ok(())
        } else {
            Err(CustodyError::NotFound(bundle_id.clone()))
        }
    }

    pub async fn cleanup_expired(&self) {
        let mut map = self.custody_map.write().await;
        let now = Utc::now();
        
        map.retain(|_, state| {
            state.expires_at > now && state.retransmission_count < self.max_retransmissions
        });
    }

    pub async fn get_custody_bundles(&self, node_id: &PeerId) -> Vec<BundleId> {
        let map = self.custody_map.read().await;
        map.iter()
            .filter(|(_, state)| state.custodian == *node_id)
            .map(|(id, _)| id.clone())
            .collect()
    }
}

#[derive(Debug, thiserror::Error)]
pub enum CustodyError {
    #[error("Bundle not in custody: {0}")]
    NotFound(BundleId),
    
    #[error("Max retransmissions exceeded for bundle: {0}")]
    MaxRetransmissions(BundleId),
    
    #[error("Custody already held by another node")]
    AlreadyHeld,
}