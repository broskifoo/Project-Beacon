"""
Radio propagation models for the simulator.
"""
from __future__ import annotations

import numpy as np
from dataclasses import dataclass
from typing import Optional

from .models import Position, TransportType


@dataclass
class PropagationParams:
    """Parameters for propagation models."""
    frequency_mhz: float = 2400.0  # 2.4 GHz for BLE/WiFi
    tx_power_dbm: float = 10.0
    rx_sensitivity_dbm: float = -90.0
    path_loss_exponent: float = 3.5
    shadowing_std_db: float = 4.0
    reference_distance_m: float = 1.0
    reference_loss_db: float = 40.0  # Path loss at reference distance


class PropagationModel:
    """Base class for propagation models."""
    
    def __init__(self, params: PropagationParams):
        self.params = params
    
    def path_loss(self, distance_m: float) -> float:
        """Calculate path loss in dB. Override in subclasses."""
        raise NotImplementedError
    
    def received_power(self, distance_m: float, tx_power_dbm: Optional[float] = None) -> float:
        """Calculate received power in dBm."""
        tx = tx_power_dbm if tx_power_dbm is not None else self.params.tx_power_dbm
        loss = self.path_loss(distance_m)
        shadowing = np.random.normal(0, self.params.shadowing_std_db)
        return tx - loss + shadowing
    
    def snr(self, distance_m: float, noise_floor_dbm: float = -100.0) -> float:
        """Calculate Signal-to-Noise Ratio in dB."""
        rx_power = self.received_power(distance_m)
        return rx_power - noise_floor_dbm
    
    def packet_loss_rate(self, distance_m: float, transport: TransportType) -> float:
        """Estimate packet loss rate based on SNR and transport."""
        snr = self.snr(distance_m)
        # Simplified model: loss increases exponentially below threshold
        if transport == TransportType.BLE:
            threshold = 10.0  # dB
        elif transport == TransportType.WIFI_DIRECT:
            threshold = 15.0
        elif transport == TransportType.LORA:
            threshold = -5.0  # LoRa can work below noise floor
        else:
            threshold = 10.0
        
        if snr >= threshold:
            return 0.0
        else:
            # Exponential increase in loss
            return min(1.0, np.exp(-(snr - threshold) / 5.0))
    
    def max_range(self, transport: TransportType, tx_power_dbm: Optional[float] = None) -> float:
        """Calculate maximum communication range."""
        tx = tx_power_dbm if tx_power_dbm is not None else self.params.tx_power_dbm
        sensitivity = self.params.rx_sensitivity_dbm
        max_loss = tx - sensitivity
        
        # Binary search for distance
        low, high = 0.1, 10000.0
        for _ in range(50):
            mid = (low + high) / 2
            if self.path_loss(mid) <= max_loss:
                low = mid
            else:
                high = mid
        return low


class FreeSpaceModel(PropagationModel):
    """Free space propagation model (Friis equation)."""
    
    def path_loss(self, distance_m: float) -> float:
        if distance_m <= 0:
            return 0.0
        wavelength = 3e8 / (self.params.frequency_mhz * 1e6)
        loss_db = 20 * np.log10(4 * np.pi * distance_m / wavelength)
        return loss_db


class LogDistanceModel(PropagationModel):
    """Log-distance path loss model with shadowing."""
    
    def path_loss(self, distance_m: float) -> float:
        if distance_m <= self.params.reference_distance_m:
            return self.params.reference_loss_db
        
        loss = (self.params.reference_loss_db + 
                10 * self.params.path_loss_exponent * 
                np.log10(distance_m / self.params.reference_distance_m))
        return loss


class TwoRayGroundModel(PropagationModel):
    """Two-ray ground reflection model."""
    
    def __init__(self, params: PropagationParams, tx_height_m: float = 1.5, rx_height_m: float = 1.5):
        super().__init__(params)
        self.tx_height = tx_height_m
        self.rx_height = rx_height_m
    
    def path_loss(self, distance_m: float) -> float:
        if distance_m <= 0:
            return 0.0
        
        # Critical distance
        wavelength = 3e8 / (self.params.frequency_mhz * 1e6)
        critical_dist = (4 * self.tx_height * self.rx_height) / wavelength
        
        if distance_m < critical_dist:
            # Free space region
            return FreeSpaceModel(self.params).path_loss(distance_m)
        else:
            # Two-ray region: 40 dB/decade
            loss = (40 * np.log10(distance_m) - 
                    20 * np.log10(self.tx_height * self.rx_height) -
                    20 * np.log10(wavelength))
            return loss


class IndoorModel(PropagationModel):
    """Indoor propagation model (ITU-R P.1238 simplified)."""
    
    def __init__(self, params: PropagationParams, floors: int = 0, walls: int = 0):
        super().__init__(params)
        self.floors = floors
        self.walls = walls
    
    def path_loss(self, distance_m: float) -> float:
        if distance_m <= 0:
            return 0.0
        
        # Base log-distance
        base_loss = LogDistanceModel(self.params).path_loss(distance_m)
        
        # Floor penetration loss
        floor_loss = self.floors * 15.0  # ~15 dB per floor
        
        # Wall penetration loss
        wall_loss = self.walls * 5.0  # ~5 dB per wall
        
        return base_loss + floor_loss + wall_loss


def create_propagation_model(model_type: str, **kwargs) -> PropagationModel:
    """Factory function to create propagation models."""
    params = PropagationParams(**{k: v for k, v in kwargs.items() if k in PropagationParams.__dataclass_fields__})
    
    if model_type == "free_space":
        return FreeSpaceModel(params)
    elif model_type == "log_distance":
        return LogDistanceModel(params)
    elif model_type == "two_ray":
        return TwoRayGroundModel(params, 
                                 kwargs.get('tx_height_m', 1.5),
                                 kwargs.get('rx_height_m', 1.5))
    elif model_type == "indoor":
        return IndoorModel(params,
                          kwargs.get('floors', 0),
                          kwargs.get('walls', 0))
    else:
        raise ValueError(f"Unknown propagation model: {model_type}")