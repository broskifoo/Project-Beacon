//! Core model types shared across mesh engine

use serde::{Deserialize, Serialize};
use uuid::Uuid;
use chrono::{DateTime, Utc};

/// Peer identifier (Ed25519 public key)
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct PeerId {
    pub value: String,
}

impl PeerId {
    pub fn new() -> Self {
        Self { value: Uuid::now_v7().to_string() }
    }

    pub fn from_string(s: String) -> Self {
        Self { value: s }
    }

    pub fn from_public_key(key: &[u8]) -> Self {
        Self { value: hex::encode(key) }
    }
}

impl Default for PeerId {
    fn default() -> Self {
        Self::new()
    }
}

impl std::fmt::Display for PeerId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", &self.value[..16.min(self.value.len())])
    }
}

/// Message priority levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize)]
pub enum MessagePriority {
    Low = 0,
    Normal = 1,
    High = 2,
    Critical = 3,
}

/// Power management modes
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum PowerMode {
    Normal,
    Conservation,
    Survival,
    Critical,
}

/// Transport types
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum TransportType {
    Ble,
    WifiDirect,
    Lora,
    BluetoothClassic,
}

/// GPS Location
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Location {
    pub latitude: f64,
    pub longitude: f64,
    pub altitude: Option<f64>,
    pub accuracy: Option<f64>,
    pub timestamp: DateTime<Utc>,
}

impl Location {
    pub fn new(lat: f64, lon: f64) -> Self {
        Self {
            latitude: lat,
            longitude: lon,
            altitude: None,
            accuracy: None,
            timestamp: Utc::now(),
        }
    }

    /// Distance to another location in meters (Haversine)
    pub fn distance_to(&self, other: &Location) -> f64 {
        const R: f64 = 6371000.0;
        
        let lat1 = self.latitude.to_radians();
        let lat2 = other.latitude.to_radians();
        let dlat = (other.latitude - self.latitude).to_radians();
        let dlon = (other.longitude - self.longitude).to_radians();
        
        let a = (dlat / 2.0).sin().powi(2) 
            + lat1.cos() * lat2.cos() * (dlon / 2.0).sin().powi(2);
        let c = 2.0 * a.sqrt().atan2((1.0 - a).sqrt());
        
        R * c
    }
}