"""
Mobility models for node movement in the simulation.
"""
from __future__ import annotations

import numpy as np
from dataclasses import dataclass
from typing import Dict, Optional, Tuple
from abc import ABC, abstractmethod

from .models import Position, NodeConfig


class MobilityModel(ABC):
    """Base class for mobility models."""
    
    @abstractmethod
    def update_position(self, node_id: str, current_pos: Position, dt: float, area_bounds: Tuple[float, float]) -> Position:
        """Update node position based on mobility model."""
        pass
    
    @abstractmethod
    def initialize_node(self, node_id: str, config: NodeConfig, area_bounds: Tuple[float, float]) -> Dict:
        """Initialize node-specific mobility state."""
        pass


class RandomWaypointModel(MobilityModel):
    """Random Waypoint mobility model."""
    
    def __init__(self, min_speed: float = 0.5, max_speed: float = 2.0, pause_time: float = 10.0):
        self.min_speed = min_speed
        self.max_speed = max_speed
        self.pause_time = pause_time
        self.node_states: Dict[str, Dict] = {}
    
    def initialize_node(self, node_id: str, config: NodeConfig, area_bounds: Tuple[float, float]) -> Dict:
        width, height = area_bounds
        return {
            "target": Position(np.random.uniform(0, width), np.random.uniform(0, height)),
            "speed": np.random.uniform(self.min_speed, self.max_speed),
            "paused": False,
            "pause_timer": 0.0,
            "area_bounds": area_bounds
        }
    
    def update_position(self, node_id: str, current_pos: Position, dt: float, area_bounds: Tuple[float, float]) -> Position:
        state = self.node_states.get(node_id)
        if state is None:
            state = self.initialize_node(node_id, NodeConfig(), area_bounds)
            self.node_states[node_id] = state
        
        width, height = area_bounds
        
        if state["paused"]:
            state["pause_timer"] -= dt
            if state["pause_timer"] <= 0:
                state["paused"] = False
                state["target"] = Position(np.random.uniform(0, width), np.random.uniform(0, height))
                state["speed"] = np.random.uniform(self.min_speed, self.max_speed)
            return current_pos
        
        # Move toward target
        dx = state["target"].x - current_pos.x
        dy = state["target"].y - current_pos.y
        dist = np.sqrt(dx*dx + dy*dy)
        
        if dist < state["speed"] * dt:
            # Reached target
            new_pos = state["target"]
            state["paused"] = True
            state["pause_timer"] = self.pause_time
        else:
            # Move toward target
            move_dist = state["speed"] * dt
            new_pos = Position(
                current_pos.x + (dx / dist) * move_dist,
                current_pos.y + (dy / dist) * move_dist
            )
        
        # Clamp to bounds
        new_pos = Position(
            max(0, min(width, new_pos.x)),
            max(0, min(height, new_pos.y))
        )
        
        return new_pos


class RandomWalkModel(MobilityModel):
    """Random Walk (Brownian) mobility model."""
    
    def __init__(self, speed: float = 1.0, direction_change_prob: float = 0.1):
        self.speed = speed
        self.direction_change_prob = direction_change_prob
        self.node_states: Dict[str, Dict] = {}
    
    def initialize_node(self, node_id: str, config: NodeConfig, area_bounds: Tuple[float, float]) -> Dict:
        return {
            "angle": np.random.uniform(0, 2*np.pi),
            "speed": self.speed,
            "area_bounds": area_bounds
        }
    
    def update_position(self, node_id: str, current_pos: Position, dt: float, area_bounds: Tuple[float, float]) -> Position:
        state = self.node_states.get(node_id)
        if state is None:
            state = self.initialize_node(node_id, NodeConfig(), area_bounds)
            self.node_states[node_id] = state
        
        width, height = area_bounds
        
        # Randomly change direction
        if np.random.random() < self.direction_change_prob:
            state["angle"] = np.random.uniform(0, 2*np.pi)
        
        move_dist = state["speed"] * dt
        new_x = current_pos.x + move_dist * np.cos(state["angle"])
        new_y = current_pos.y + move_dist * np.sin(state["angle"])
        
        # Bounce off walls
        if new_x <= 0 or new_x >= width:
            state["angle"] = np.pi - state["angle"]
            new_x = max(0, min(width, new_x))
        if new_y <= 0 or new_y >= height:
            state["angle"] = -state["angle"]
            new_y = max(0, min(height, new_y))
        
        return Position(new_x, new_y)


class GaussMarkovModel(MobilityModel):
    """Gauss-Markov mobility model (smooth random motion)."""
    
    def __init__(self, alpha: float = 0.5, mean_speed: float = 1.0, speed_var: float = 0.5):
        self.alpha = alpha
        self.mean_speed = mean_speed
        self.speed_var = speed_var
        self.node_states: Dict[str, Dict] = {}
    
    def initialize_node(self, node_id: str, config: NodeConfig, area_bounds: Tuple[float, float]) -> Dict:
        return {
            "speed": mean_speed,
            "angle": np.random.uniform(0, 2*np.pi),
            "area_bounds": area_bounds
        }
    
    def update_position(self, node_id: str, current_pos: Position, dt: float, area_bounds: Tuple[float, float]) -> Position:
        state = self.node_states.get(node_id)
        if state is None:
            state = self.initialize_node(node_id, NodeConfig(), area_bounds)
            self.node_states[node_id] = state
        
        width, height = area_bounds
        
        # Gauss-Markov update
        n = np.random.normal(0, 1)
        state["speed"] = (self.alpha * state["speed"] + 
                          (1 - self.alpha) * self.mean_speed + 
                          np.sqrt(1 - self.alpha**2) * self.speed_var * n)
        state["speed"] = max(0.1, state["speed"])
        
        n = np.random.normal(0, 1)
        state["angle"] = (self.alpha * state["angle"] + 
                          np.sqrt(1 - self.alpha**2) * n)
        
        move_dist = state["speed"] * dt
        new_x = current_pos.x + move_dist * np.cos(state["angle"])
        new_y = current_pos.y + move_dist * np.sin(state["angle"])
        
        # Reflect at boundaries
        if new_x <= 0:
            new_x = -new_x
            state["angle"] = np.pi - state["angle"]
        elif new_x >= width:
            new_x = 2*width - new_x
            state["angle"] = np.pi - state["angle"]
        
        if new_y <= 0:
            new_y = -new_y
            state["angle"] = -state["angle"]
        elif new_y >= height:
            new_y = 2*height - new_y
            state["angle"] = -state["angle"]
        
        return Position(new_x, new_y)


class DisasterMobilityModel(MobilityModel):
    """Disaster scenario mobility: groups moving to shelters, rescue teams patrolling."""
    
    def __init__(self, shelter_positions: list, rescue_team_ratio: float = 0.1):
        self.shelter_positions = shelter_positions
        self.rescue_team_ratio = rescue_team_ratio
        self.node_states: Dict[str, Dict] = {}
        self.node_types: Dict[str, str] = {}  # "civilian", "rescue", "static"
    
    def initialize_node(self, node_id: str, config: NodeConfig, area_bounds: Tuple[float, float]) -> Dict:
        # Assign node type
        rand = np.random.random()
        if rand < self.rescue_team_ratio:
            node_type = "rescue"
        elif rand < self.rescue_team_ratio + 0.1:
            node_type = "static"  # Fixed infrastructure
        else:
            node_type = "civilian"
        
        self.node_types[node_id] = node_type
        
        if node_type == "rescue":
            # Rescue teams start at random positions, patrol
            return {
                "type": "rescue",
                "patrol_points": [Position(np.random.uniform(0, area_bounds[0]), 
                                           np.random.uniform(0, area_bounds[1])) 
                                 for _ in range(5)],
                "current_patrol": 0,
                "speed": np.random.uniform(2.0, 5.0),
                "area_bounds": area_bounds
            }
        elif node_type == "static":
            return {
                "type": "static",
                "area_bounds": area_bounds
            }
        else:
            # Civilians move toward nearest shelter
            nearest_shelter = min(self.shelter_positions, 
                                key=lambda s: np.sqrt((s.x - config.position.x)**2 + 
                                                      (s.y - config.position.y)**2))
            return {
                "type": "civilian",
                "target_shelter": nearest_shelter,
                "speed": np.random.uniform(0.5, 1.5),
                "reached_shelter": False,
                "area_bounds": area_bounds
            }
    
    def update_position(self, node_id: str, current_pos: Position, dt: float, area_bounds: Tuple[float, float]) -> Position:
        state = self.node_states.get(node_id)
        if state is None:
            state = self.initialize_node(node_id, NodeConfig(), area_bounds)
            self.node_states[node_id] = state
        
        width, height = area_bounds
        node_type = self.node_types.get(node_id, "civilian")
        
        if node_type == "static":
            return current_pos
        
        elif node_type == "rescue":
            # Patrol between points
            target = state["patrol_points"][state["current_patrol"]]
            dx = target.x - current_pos.x
            dy = target.y - current_pos.y
            dist = np.sqrt(dx*dx + dy*dy)
            
            if dist < state["speed"] * dt:
                state["current_patrol"] = (state["current_patrol"] + 1) % len(state["patrol_points"])
                return target
            else:
                move_dist = state["speed"] * dt
                new_pos = Position(
                    current_pos.x + (dx / dist) * move_dist,
                    current_pos.y + (dy / dist) * move_dist
                )
                return Position(max(0, min(width, new_pos.x)), max(0, min(height, new_pos.y)))
        
        else:  # civilian
            if state["reached_shelter"]:
                # Small random movement around shelter
                return Position(
                    max(0, min(width, current_pos.x + np.random.uniform(-5, 5))),
                    max(0, min(height, current_pos.y + np.random.uniform(-5, 5)))
                )
            
            target = state["target_shelter"]
            dx = target.x - current_pos.x
            dy = target.y - current_pos.y
            dist = np.sqrt(dx*dx + dy*dy)
            
            if dist < 20:  # Reached shelter vicinity
                state["reached_shelter"] = True
                return current_pos
            
            move_dist = state["speed"] * dt
            if move_dist >= dist:
                return target
            else:
                new_pos = Position(
                    current_pos.x + (dx / dist) * move_dist,
                    current_pos.y + (dy / dist) * move_dist
                )
                return Position(max(0, min(width, new_pos.x)), max(0, min(height, new_pos.y)))


def create_mobility_model(model_type: str, **kwargs) -> MobilityModel:
    """Factory function to create mobility models."""
    if model_type == "random_waypoint":
        return RandomWaypointModel(
            kwargs.get("min_speed", 0.5),
            kwargs.get("max_speed", 2.0),
            kwargs.get("pause_time", 10.0)
        )
    elif model_type == "random_walk":
        return RandomWalkModel(
            kwargs.get("speed", 1.0),
            kwargs.get("direction_change_prob", 0.1)
        )
    elif model_type == "gauss_markov":
        return GaussMarkovModel(
            kwargs.get("alpha", 0.5),
            kwargs.get("mean_speed", 1.0),
            kwargs.get("speed_var", 0.5)
        )
    elif model_type == "disaster":
        return DisasterMobilityModel(
            kwargs.get("shelter_positions", []),
            kwargs.get("rescue_team_ratio", 0.1)
        )
    elif model_type == "static":
        return StaticMobilityModel()
    else:
        raise ValueError(f"Unknown mobility model: {model_type}")


class StaticMobilityModel(MobilityModel):
    """Static nodes (no movement)."""
    
    def initialize_node(self, node_id: str, config: NodeConfig, area_bounds: Tuple[float, float]) -> Dict:
        return {}
    
    def update_position(self, node_id: str, current_pos: Position, dt: float, area_bounds: Tuple[float, float]) -> Position:
        return current_pos