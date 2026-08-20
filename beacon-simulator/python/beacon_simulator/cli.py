"""
Command-line interface for the Beacon simulator.
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Optional

import click
import numpy as np
from rich.console import Console
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn

from .models import (NodeConfig, Position, PowerMode, SimulationConfig, Scenario, TransportType)
from .simulator import Simulator, create_simulator
from .routing import create_routing_protocol

console = Console()


@click.group()
@click.version_option(version="0.1.0-alpha")
def main():
    """Beacon Network Simulator - Mesh protocol validation for disaster-resilient communications."""
    pass


@main.command()
@click.option("--nodes", "-n", default=50, help="Number of nodes")
@click.option("--duration", "-d", default=3600, help="Simulation duration (seconds)")
@click.option("--area-width", default=1000, help="Area width (meters)")
@click.option("--area-height", default=1000, help="Area height (meters)")
@click.option("--mobility", default="random_waypoint", 
              type=click.Choice(["static", "random_waypoint", "random_walk", "gauss_markov", "disaster"]))
@click.option("--routing", default="hybrid",
              type=click.Choice(["flooding", "probabilistic", "epidemic", "geographic", "hybrid", "priority_aware"]))
@click.option("--propagation", default="log_distance",
              type=click.Choice(["free_space", "log_distance", "two_ray", "indoor"]))
@click.option("--path-loss", default=3.5, help="Path loss exponent")
@click.option("--seed", default=None, type=int, help="Random seed")
@click.option("--output", "-o", default="output", help="Output directory")
@click.option("--scenario", "-s", default=None, help="Scenario file (YAML)")
def run(nodes: int, duration: int, area_width: int, area_height: int,
        mobility: str, routing: str, propagation: str, path_loss: float,
        seed: Optional[int], output: str, scenario: Optional[str]):
    """Run a simulation."""
    
    console.print("[bold blue]Beacon Network Simulator[/bold blue]")
    console.print(f"Nodes: {nodes}, Duration: {duration}s, Area: {area_width}x{area_height}m")
    console.print(f"Mobility: {mobility}, Routing: {routing}, Propagation: {propagation}")
    
    # Create config
    config = SimulationConfig(
        duration_seconds=duration,
        area_width=area_width,
        area_height=area_height,
        num_nodes=nodes,
        mobility_enabled=(mobility != "static"),
        propagation_model=propagation,
        path_loss_exponent=path_loss,
        random_seed=seed,
        output_dir=output
    )
    
    # Load scenario if provided
    if scenario:
        config = _load_scenario(scenario, config)
    
    # Create and run simulator
    simulator = create_simulator(config)
    
    # Override routing if specified
    if routing:
        simulator.routing = create_routing_protocol(routing)
    
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        console=console
    ) as progress:
        task = progress.add_task("Running simulation...", total=None)
        simulator.run()
        progress.update(task, completed=True)
    
    # Save results
    simulator.save_results(output)
    
    # Print summary
    _print_summary(simulator)


@main.command()
@click.option("--output", "-o", default="scenario.yaml", help="Output scenario file")
def create_scenario(output: str):
    """Create a sample scenario file."""
    scenario = Scenario(
        name="urban_disaster",
        description="Urban disaster scenario with shelters and rescue teams",
        config=SimulationConfig(
            duration_seconds=7200,
            area_width=2000,
            area_height=2000,
            num_nodes=100,
            mobility_enabled=True,
            propagation_model="log_distance",
            path_loss_exponent=3.5
        ),
        mobility_patterns={
            "civilian": {"model": "disaster", "shelter_positions": [[500, 500], [1500, 500], [1000, 1500]]},
            "rescue": {"model": "gauss_markov", "alpha": 0.7, "mean_speed": 3.0}
        },
        traffic_patterns={
            "telemetry": {"interval": 30, "priority": "low"},
            "sos": {"rate": 0.001, "priority": "critical"},
            "resource_report": {"rate": 0.01, "priority": "high"}
        }
    )
    
    # Convert to dict for YAML
    scenario_dict = {
        "name": scenario.name,
        "description": scenario.description,
        "config": {
            "duration_seconds": scenario.config.duration_seconds,
            "area_width": scenario.config.area_width,
            "area_height": scenario.config.area_height,
            "num_nodes": scenario.config.num_nodes,
            "mobility_enabled": scenario.config.mobility_enabled,
            "propagation_model": scenario.config.propagation_model,
            "path_loss_exponent": scenario.config.path_loss_exponent
        },
        "mobility_patterns": scenario.mobility_patterns,
        "traffic_patterns": scenario.traffic_patterns
    }
    
    import yaml
    with open(output, "w") as f:
        yaml.dump(scenario_dict, f, default_flow_style=False)
    
    console.print(f"[green]Scenario saved to {output}[/green]")


@main.command()
@click.argument("results_dir", type=click.Path(exists=True))
def analyze(results_dir: str):
    """Analyze simulation results."""
    import pandas as pd
    
    path = Path(results_dir)
    
    # Load metrics
    metrics_file = path / "metrics.csv"
    if not metrics_file.exists():
        console.print("[red]No metrics.csv found[/red]")
        return
    
    df = pd.read_csv(metrics_file)
    
    # Print summary table
    table = Table(title="Simulation Metrics Summary")
    table.add_column("Metric", style="cyan")
    table.add_column("Final Value", style="green")
    table.add_column("Peak", style="yellow")
    table.add_column("Average", style="blue")
    
    key_metrics = [
        ("Delivery Rate", "delivery_rate"),
        ("Avg Latency (ms)", "avg_latency_ms"),
        ("Avg Hop Count", "avg_hop_count"),
        ("Avg Battery %", "avg_battery_pct"),
        ("Connected Components", "connected_components"),
        ("Largest Component", "largest_component_size"),
        ("Throughput (bps)", "throughput_bps"),
    ]
    
    for name, col in key_metrics:
        if col in df.columns:
            table.add_row(
                name,
                f"{df[col].iloc[-1]:.4f}",
                f"{df[col].max():.4f}",
                f"{df[col].mean():.4f}"
            )
    
    console.print(table)
    
    # Priority delivery rates
    if "priority_delivery_rates" in df.columns:
        # This would need parsing - simplified
        pass


@main.command()
@click.option("--nodes", "-n", default=10, help="Number of nodes")
@click.option("--output", "-o", default="topology.png", help="Output image")
def visualize(nodes: int, output: str):
    """Generate a network topology visualization."""
    import matplotlib.pyplot as plt
    import networkx as nx
    
    # Create a simple simulation for visualization
    config = SimulationConfig(
        duration_seconds=100,
        num_nodes=nodes,
        area_width=500,
        area_height=500,
        mobility_enabled=False
    )
    
    simulator = create_simulator(config)
    simulator.run()
    
    # Create graph
    G = nx.Graph()
    for node_id, state in simulator.nodes.items():
        G.add_node(node_id, pos=state.position.to_tuple()[:2], battery=state.battery_pct)
    
    for (src, dst), link in simulator._create_topology_snapshot().links.items():
        G.add_edge(src, dst, weight=link.quality_score())
    
    # Plot
    fig, ax = plt.subplots(figsize=(10, 10))
    pos = nx.get_node_attributes(G, 'pos')
    batteries = nx.get_node_attributes(G, 'battery')
    
    # Color by battery
    colors = [batteries[n] for n in G.nodes()]
    nx.draw(G, pos, node_color=colors, node_size=100, 
            cmap=plt.cm.RdYlGn, vmin=0, vmax=100,
            with_labels=True, font_size=8, ax=ax)
    
    plt.colorbar(plt.cm.ScalarMappable(cmap=plt.cm.RdYlGn), ax=ax, label="Battery %")
    ax.set_title(f"Beacon Mesh Topology ({nodes} nodes)")
    plt.savefig(output, dpi=150, bbox_inches='tight')
    console.print(f"[green]Visualization saved to {output}[/green]")


def _load_scenario(scenario_path: str, config: SimulationConfig) -> SimulationConfig:
    """Load scenario from YAML file."""
    import yaml
    
    with open(scenario_path) as f:
        data = yaml.safe_load(f)
    
    if "config" in data:
        for key, value in data["config"].items():
            if hasattr(config, key):
                setattr(config, key, value)
    
    return config


def _print_summary(simulator: Simulator):
    """Print simulation summary."""
    if not simulator.metrics_history:
        return
    
    final = simulator.metrics_history[-1]
    
    table = Table(title="Simulation Complete")
    table.add_column("Metric", style="cyan")
    table.add_column("Value", style="green")
    
    table.add_row("Duration", f"{simulator.current_time:.1f}s")
    table.add_row("Nodes Alive", str(final.num_nodes_alive))
    table.add_row("Nodes Dead", str(final.num_nodes_dead))
    table.add_row("Messages Created", str(final.total_messages_created))
    table.add_row("Messages Delivered", str(final.total_messages_delivered))
    table.add_row("Messages Dropped", str(final.total_messages_dropped))
    table.add_row("Delivery Rate", f"{final.delivery_rate:.2%}")
    table.add_row("Avg Latency", f"{final.avg_latency_ms:.1f}ms")
    table.add_row("Avg Hops", f"{final.avg_hop_count:.1f}")
    table.add_row("Avg Battery", f"{final.avg_battery_pct:.1f}%")
    table.add_row("Components", str(final.connected_components))
    table.add_row("Largest Component", str(final.largest_component_size))
    table.add_row("Avg Degree", f"{final.avg_node_degree:.1f}")
    table.add_row("Throughput", f"{final.throughput_bps:.0f} bps")
    table.add_row("Energy Used", f"{final.energy_consumed_mah:.1f} mAh")
    
    console.print(table)
    
    # Priority breakdown
    if final.priority_delivery_rates:
        prio_table = Table(title="Delivery Rate by Priority")
        prio_table.add_column("Priority", style="cyan")
        prio_table.add_column("Delivery Rate", style="green")
        for priority, rate in final.priority_delivery_rates.items():
            prio_table.add_row(priority, f"{rate:.2%}")
        console.print(prio_table)


if __name__ == "__main__":
    main()