# ADR-0002: Offline GIS Database Engine Selection

## Document Metadata

* **Document ID:** `ADR-0002`
* **Version:** `1.0.0`
* **Status:** Accepted
* **Author:** Project Beacon Core Team
* **Reviewers:** Project Beacon Maintainers
* **Last Updated:** 2026-08-20

---

## Status

**Accepted** — **SQLite with SpatiaLite extension** is the primary offline GIS database engine. **DuckDB** is retained as an analytical query engine for dashboard aggregation. **Custom flat files (MBTiles/GeoPackage)** are used for vector tile storage.

---

## Context

Project Beacon requires offline geospatial capabilities including:
- **Vector tile rendering** (MapLibre) — MBTiles/GeoPackage format
- **POI storage and search** — Hospitals, shelters, water points, hazards
- **Route calculation** — Offline routing graph (Valhalla/OSRM)
- **Community marker sync** — Distributed CRDT-style sync via mesh
- **Spatial analytics** — Heatmaps, coverage analysis, resource density

### Requirements

| Requirement | Priority | Detail |
|-------------|----------|--------|
| **REQ-GIS-001** | Must | Embedded, zero-config, no server process |
| **REQ-GIS-002** | Must | Spatial indexing (R-tree) for fast bbox/knn queries |
| **REQ-GIS-003** | Must | OGC Simple Features compliance (Point, LineString, Polygon) |
| **REQ-GIS-004** | Must | GeoPackage / MBTiles read/write support |
| **REQ-GIS-005** | Must | Cross-platform (Android, Linux, desktop) |
| **REQ-GIS-006** | Should | SQL interface for complex queries |
| **REQ-GIS-007** | Should | Vector tile decoding performance > 60fps |
| **REQ-GIS-008** | Could | Analytical OLAP queries (dashboard) |
| **REQ-GIS-009** | Could | CRDT-friendly schema for mesh sync |
| **REQ-GIS-010** | Must | Permissive license (MIT/BSD/Apache-2.0) |

### Candidate Technologies Evaluated

| Engine | Type | Spatial Index | Vector Tiles | License | Android Support |
|--------|------|---------------|--------------|---------|-----------------|
| **SQLite + SpatiaLite** | Embedded RDBMS | R-tree (virtual table) | Via GeoPackage | MPL-1.1 / GPL | Excellent (native) |
| **DuckDB** | Embedded OLAP | R-tree (extension) | Via extension | MIT | Good (via JNI/WASM) |
| **GeoPackage (SQLite)** | File format | R-tree | Native | OGC / MIT | Excellent |
| **MBTiles (SQLite)** | File format | Quadkey index | Native | BSD | Excellent |
| **PostGIS** | Server | GiST/SP-GiST | Via functions | GPL | No (server required) |
| **H2GIS** | Embedded Java | R-tree | Partial | LGPL | Good (JVM only) |
| **Custom flat files** | Custom | Custom | Custom | Any | Any |

---

## Decision

### Primary: SQLite + SpatiaLite (for operational data)
- **Use cases**: POI storage, community markers, user locations, routing graph nodes/edges, sync metadata
- **Why**: 
  - Native Android support via `android.database.sqlite` + SpatiaLite loadable extension
  - Mature, battle-tested, OGC compliant
  - Single file, zero-config, transactional
  - R-tree virtual tables for spatial indexes
  - GeoPackage read/write via GDAL or native SQL
  - CRDT-friendly: each feature = row with version vector

### Secondary: DuckDB (for analytical queries)
- **Use cases**: Dashboard aggregations, heatmap generation, coverage statistics, historical analysis
- **Why**:
  - Columnar OLAP engine — 10-100x faster for analytical queries
  - Can query SQLite databases directly via `sqlite` extension
  - Runs in-process, no server
  - MIT license
  - Python/Rust/Node bindings for dashboard/backend

### Vector Tiles: MBTiles / GeoPackage (SQLite-based)
- **Use cases**: MapLibre rendering, offline basemaps
- **Why**:
  - Industry standard (Mapbox, OpenMapTiles)
  - Single SQLite file with standardized schema
  - Native support in MapLibre, Mapbox GL, Tangram
  - Efficient quadkey/zxy indexing
  - Can be generated from OSM via `tippecanoe`/`planetiler`

---

## Alternatives Considered

### Alternative A: DuckDB Only
- **Rejection Rationale**: Spatial extension less mature than SpatiaLite; no native Android JNI build yet (WASM only); GeoPackage write support limited; transactional guarantees weaker for operational data

### Alternative B: H2GIS (Pure Java)
- **Rejection Rationale**: LGPL license (copyleft for static linking); JVM-only (no Rust/Python/Go); slower than native SQLite; less active maintenance

### Alternative C: Custom Flat Files (Protocol Buffers + Custom Index)
- **Rejection Rationale**: Reinventing GIS database; no standard tooling; interoperability loss; high maintenance burden; MapLibre expects MBTiles/GeoPackage

### Alternative D: PostGIS (Embedded via pg_embedded)
- **Rejection Rationale**: Server process required; heavy (~100MB+); GPL license; overkill for mobile

---

## Consequences

### Positive Impact
- **Unified storage**: Single SQLite file for operational + vector data (or few files)
- **Tooling ecosystem**: GDAL, QGIS, `ogr2ogr`, `tippecanoe`, `planetiler` all work natively
- **Android native**: `SQLiteDatabase` + `loadExtension("mod_spatialite")` works out of box
- **Transactional sync**: ACID for local writes; CRDT merge at application layer
- **Analytical power**: DuckDB reads same SQLite files for dashboard without ETL
- **License compatibility**: All components MIT/BSD/MPL compatible with Apache-2.0 project

### Negative/Trade-offs
- **SpatiaLite on Android**: Requires bundling `libspatialite.so` (architecture-specific); adds ~2-3MB per ABI
- **Dual-engine complexity**: Two query engines (SQLite for OLTP, DuckDB for OLAP) — must keep schemas compatible
- **Vector tile writes**: MBTiles write path requires careful transaction handling to avoid corruption
- **Routing graph**: Valhalla/OSRM expect custom formats; conversion step needed from SQLite
- **CRDT implementation**: Not built-in; application-level version vectors required

### Dependencies
- **SpatiaLite 5.x** compiled for `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`
- **DuckDB 1.0+** with `spatial` and `sqlite` extensions
- **GDAL 3.8+** for GeoPackage/MBTiles I/O (build-time tile generation)
- **MapLibre Native** or **MapLibre GL JS** for rendering
- **Valhalla** or **OSRM** for routing (separate binary, reads from SQLite export)

---

## Schema Design (Operational Database)

```sql
-- Enable SpatiaLite
SELECT load_extension('mod_spatialite');

-- Spatial metadata (auto-created by SpatiaLite)
-- geometry_columns, spatial_ref_sys

-- POIs (official + community)
CREATE TABLE pois (
    id TEXT PRIMARY KEY,              -- UUIDv7 (time-ordered)
    source TEXT NOT NULL,             -- 'osm' | 'community' | 'official'
    category TEXT NOT NULL,           -- 'water', 'medical', 'shelter', 'food', 'hazard', 'charging'
    name TEXT,
    description TEXT,
    geometry POINT NOT NULL,          -- SpatiaLite Point
    properties JSON,                  -- Flexible attributes
    confidence REAL DEFAULT 1.0,      -- 0.0-1.0 (community verified)
    version_vector JSON NOT NULL,     -- CRDT version vector {peer_id: counter}
    created_at INTEGER NOT NULL,      -- Unix ms
    updated_at INTEGER NOT NULL,
    expires_at INTEGER,               -- Auto-expiry for community reports
    reporter_pubkey TEXT,             -- Ed25519 public key (hex)
    signature TEXT                    -- Ed25519 signature of canonical JSON
);

-- Spatial index (R-tree virtual table)
CREATE VIRTUAL TABLE pois_rtree USING rtree(
    id, minx, maxx, miny, maxy
);

-- Trigger to maintain R-tree
CREATE TRIGGER pois_rtree_insert AFTER INSERT ON pois BEGIN
    INSERT INTO pois_rtree (id, minx, maxx, miny, maxy)
    VALUES (NEW.id, 
        ST_X(NEW.geometry), ST_X(NEW.geometry),
        ST_Y(NEW.geometry), ST_Y(NEW.geometry));
END;

-- Community markers (lightweight, high churn)
CREATE TABLE markers (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,               -- 'water', 'hazard', 'road_closed', etc.
    geometry POINT NOT NULL,
    message TEXT,
    severity INTEGER DEFAULT 1,       -- 1=info, 2=warning, 3=critical
    version_vector JSON NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    reporter_pubkey TEXT,
    signature TEXT
);

CREATE VIRTUAL TABLE markers_rtree USING rtree(
    id, minx, maxx, miny, maxy
);

-- Mesh peers (for network topology)
CREATE TABLE peers (
    pubkey TEXT PRIMARY KEY,          -- Ed25519 (hex)
    last_seen INTEGER NOT NULL,
    location POINT,                   -- Last known location
    battery INTEGER,                  -- 0-100
    power_mode TEXT,                  -- 'normal', 'conservation', 'survival', 'critical'
    transports TEXT,                  -- JSON array: ['ble', 'wifi', 'lora']
    version_vector JSON               -- For sync state
);

-- Routing graph (simplified for Valhalla export)
CREATE TABLE routing_nodes (
    id INTEGER PRIMARY KEY,
    geometry POINT NOT NULL,
    elevation REAL,
    tags JSON                         -- OSM tags
);

CREATE TABLE routing_edges (
    source INTEGER NOT NULL,
    target INTEGER NOT NULL,
    cost REAL NOT NULL,               -- Travel time (seconds)
    geometry LINESTRING NOT NULL,
    tags JSON,                        -- highway, surface, etc.
    PRIMARY KEY (source, target)
);

CREATE VIRTUAL TABLE routing_nodes_rtree USING rtree(
    id, minx, maxx, miny, maxy
);
```

---

## Sync Strategy (Mesh CRDT)

Each table with `version_vector` uses a **Last-Writer-Wins Register (LWW-Register)** per row with **causal context**:

1. **Local write**: Increment local counter in version vector, sign row
2. **Mesh broadcast**: Send `(table, id, row, version_vector, signature)`
3. **Receive**: 
   - Verify signature against `reporter_pubkey`
   - Compare version vectors (causal precedence)
   - If concurrent: LWW by timestamp (Unix ms in `updated_at`)
   - Apply if newer; merge version vectors
4. **Garbage collection**: Tombstones retained for `max_sync_window` (7 days)

---

## Performance Targets

| Operation | Target (p95) | Measurement |
|-----------|--------------|-------------|
| POI bbox query (1km²) | < 10ms | 10k POIs, R-tree |
| KNN search (10 nearest) | < 15ms | 10k POIs |
| Marker insert + index | < 5ms | Single row |
| Vector tile decode (256x256) | < 16ms | MapLibre + MBTiles |
| Dashboard aggregation (100k rows) | < 100ms | DuckDB columnar |
| Full DB vacuum | < 2s | 50MB database |

---

## References

* [SpatiaLite Documentation](https://www.gaia-gis.it/fossil/libspatialite/index)
* [DuckDB Spatial Extension](https://duckdb.org/docs/extensions/spatial)
* [GeoPackage Encoding Standard](https://www.ogc.org/standard/geopackage/)
* [MBTiles Specification](https://github.com/mapbox/mbtiles-spec)
* [MapLibre Native](https://github.com/maplibre/maplibre-native)
* [Valhalla Routing Engine](https://github.com/valhalla/valhalla)

---

## Revision History

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-08-20 | 1.0.0 | Initial decision | Project Beacon Core Team |

---

## Approval

**Status: ACCEPTED** ✅