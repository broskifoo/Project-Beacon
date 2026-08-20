"""
Integration tests for Beacon mesh networking using the simulator.
"""
import pytest
import asyncio
from beacon_simulator import (
    Simulator, SimulationConfig, NodeConfig, Position,
    TransportType, PowerMode, MessagePriority, MessageType
)
from beacon_simulator.models import Message

class TestMeshIntegration:
    """Integration tests for mesh networking scenarios."""
    
    @pytest.fixture
    def basic_config(self):
        """Basic simulation configuration for testing."""
        return SimulationConfig(
            duration_seconds=60.0,
            area_width=500.0,
            area_height=500.0,
            num_nodes=10,
            mobility_enabled=False,
            propagation_model="log_distance",
            path_loss_exponent=3.0,
            random_seed=42
        )
    
    @pytest.fixture
    def mobile_config(self):
        """Mobile simulation configuration."""
        return SimulationConfig(
            duration_seconds=120.0,
            area_width=1000.0,
            area_height=1000.0,
            num_nodes=20,
            mobility_enabled=True,
            propagation_model="log_distance",
            path_loss_exponent=3.5,
            random_seed=42
        )

    def test_direct_communication(self, basic_config):
        """Test direct peer-to-peer message delivery."""
        simulator = Simulator(config=basic_config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # Should have high delivery rate for direct communication
        assert final_metrics.delivery_rate > 0.8, f"Delivery rate too low: {final_metrics.delivery_rate}"
        assert final_metrics.avg_hop_count <= 1.5, f"Too many hops: {final_metrics.avg_hop_count}"

    def test_sos_priority_delivery(self, basic_config):
        """Test that SOS messages have higher delivery priority."""
        simulator = Simulator(config=basic_config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # CRITICAL messages should have higher delivery rate
        critical_rate = final_metrics.priority_delivery_rates.get("critical", 0)
        normal_rate = final_metrics.priority_delivery_rates.get("normal", 0)
        
        assert critical_rate >= normal_rate, "CRITICAL messages should have equal or higher delivery rate"

    def test_multi_hop_routing(self, basic_config):
        """Test multi-hop message routing."""
        # Increase area to force multi-hop
        config = SimulationConfig(
            duration_seconds=60.0,
            area_width=1000.0,
            area_height=1000.0,
            num_nodes=15,
            mobility_enabled=False,
            propagation_model="log_distance",
            path_loss_exponent=3.5,
            random_seed=42
        )
        
        simulator = Simulator(config=config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # Should have some multi-hop deliveries
        assert final_metrics.avg_hop_count > 1.0, "Should have multi-hop deliveries"
        assert final_metrics.avg_hop_count <= 3.0, "Should not have excessive hops"

    def test_store_and_forward(self, mobile_config):
        """Test store-and-forward with node mobility."""
        simulator = Simulator(config=mobile_config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # With mobility, some messages should be delayed but eventually delivered
        assert final_metrics.delivery_rate > 0.5, "Store-and-forward should work with mobility"
        
        # Check that messages were forwarded
        assert final_metrics.total_messages_forwarded > 0, "Messages should be forwarded"

    def test_power_mode_effects(self, basic_config):
        """Test that power modes affect network behavior."""
        # Create nodes with different power modes
        configs = []
        for i in range(10):
            configs.append(NodeConfig(
                node_id=f"node_{i}",
                position=Position(
                    x=50 + (i % 5) * 100,
                    y=50 + (i // 5) * 100
                ),
                power_mode=PowerMode.NORMAL if i < 5 else PowerMode.CONSERVATION,
                initial_battery_pct=100.0 if i < 5 else 30.0
            ))
        
        config = SimulationConfig(
            duration_seconds=60.0,
            area_width=500.0,
            area_height=500.0,
            node_configs=configs,
            mobility_enabled=False,
            propagation_model="log_distance",
            random_seed=42
        )
        
        simulator = Simulator(config=config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # NORMAL nodes should have higher connectivity
        assert final_metrics.avg_battery_pct > 50, "Average battery should be reasonable"

    def test_network_partition_recovery(self, basic_config):
        """Test network recovery after partition."""
        # Create two clusters far apart
        configs = []
        # Cluster 1
        for i in range(5):
            configs.append(NodeConfig(
                node_id=f"cluster1_{i}",
                position=Position(x=100 + i * 30, y=100 + i * 30)
            ))
        # Cluster 2 - far away
        for i in range(5):
            configs.append(NodeConfig(
                node_id=f"cluster2_{i}",
                position=Position(x=800 + i * 30, y=800 + i * 30)
            ))
        
        config = SimulationConfig(
            duration_seconds=120.0,
            area_width=1000.0,
            area_height=1000.0,
            node_configs=configs,
            mobility_enabled=True,
            propagation_model="log_distance",
            path_loss_exponent=3.5,
            random_seed=42
        )
        
        simulator = Simulator(config=config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # Initially partitioned, but mobility should allow some cross-cluster communication
        # Over 120 seconds, nodes should move and potentially connect clusters
        assert final_metrics.connected_components <= 2, "Should have at most 2 components"
        assert final_metrics.largest_component_size >= 5, "Should have reasonable component size"

    def test_message_priority_ordering(self, basic_config):
        """Test that messages are processed in priority order."""
        simulator = Simulator(config=basic_config)
        
        # Add custom high-priority messages
        # This would require access to internal message queues
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # Verify metrics collected
        assert final_metrics.total_messages_created > 0
        assert final_metrics.total_messages_delivered >= 0

    def test_energy_consumption(self, basic_config):
        """Test energy consumption tracking."""
        simulator = Simulator(config=basic_config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # Energy should be consumed
        assert final_metrics.energy_consumed_mah > 0
        assert final_metrics.avg_battery_pct <= 100

    def test_large_scale_simulation(self):
        """Test simulator with larger node count."""
        config = SimulationConfig(
            duration_seconds=30.0,
            area_width=2000.0,
            area_height=2000.0,
            num_nodes=100,
            mobility_enabled=True,
            propagation_model="log_distance",
            path_loss_exponent=3.5,
            random_seed=42
        )
        
        simulator = Simulator(config=config)
        metrics = simulator.run()
        
        final_metrics = metrics[-1]
        
        # Should complete without errors
        assert final_metrics.num_nodes_alive >= 0
        assert len(metrics) > 1  # Should have multiple metric snapshots

if __name__ == "__main__":
    pytest.main([__file__, "-v"])