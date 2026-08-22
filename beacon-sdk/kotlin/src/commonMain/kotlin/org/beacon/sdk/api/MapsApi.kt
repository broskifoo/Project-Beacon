package org.beacon.sdk.api

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import org.beacon.sdk.model.*

interface MapsApi {
    suspend fun getMapTile(z: Int, x: Int, y: Int): Result<ByteArray>

    suspend fun searchPoi(
        query: String,
        category: ResourceType? = null,
        bounds: GeoBounds? = null,
        limit: Int = 50
    ): Result<List<Poi>>

    suspend fun getNearbyPoi(
        location: Location,
        radiusMeters: Double,
        category: ResourceType? = null,
        limit: Int = 50
    ): Result<List<Poi>>

    suspend fun addPoi(poi: Poi): Result<Poi>

    suspend fun updatePoi(poi: Poi): Result<Poi>

    suspend fun deletePoi(poiId: String): Result<Unit>

    suspend fun calculateRoute(
        from: Location,
        to: Location,
        profile: RoutingProfile = RoutingProfile.FOOT
    ): Result<Route>

    fun observePoiUpdates(): Flow<PoiUpdate>

    suspend fun syncPoiFromMesh(): Result<Int>

    suspend fun exportMapRegion(bounds: GeoBounds, zoomMin: Int, zoomMax: Int): Result<ByteArray>

    suspend fun importMapRegion(data: ByteArray): Result<Int>
}

@Serializable
data class Poi(
    val id: String,
    val name: String,
    val category: ResourceType,
    val location: Location,
    val description: String? = null,
    val properties: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f,
    val source: String = "local",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val expiresAt: Instant? = null
)

@Serializable
data class PoiUpdate(
    val poi: Poi,
    val updateType: PoiUpdateType
)

enum class PoiUpdateType {
    ADDED, UPDATED, DELETED, EXPIRED
}

@Serializable
enum class RoutingProfile {
    FOOT, BICYCLE, CAR, WHEELCHAIR
}

@Serializable
data class Route(
    val geometry: List<Location>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val instructions: List<RouteInstruction>,
    val profile: RoutingProfile
)

@Serializable
data class RouteInstruction(
    val text: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuver: ManeuverType,
    val location: Location
)

enum class ManeuverType {
    START, END, TURN_LEFT, TURN_RIGHT, TURN_SLIGHT_LEFT, TURN_SLIGHT_RIGHT,
    CONTINUE, ROUNDABOUT, U_TURN, ARRIVE
}