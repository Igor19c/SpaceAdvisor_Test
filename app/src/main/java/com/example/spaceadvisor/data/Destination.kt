package com.example.spaceadvisor.data

import java.io.Serializable

data class Destination(
    /*  General Info */
    val id: String = "",             // (Document ID in Firestore)
    val parentId: String = "root",    // Galaxies -> Bodies -> Locations
    val title: String = "",           // Name (Milky Way, Mars, Olympus Mons)
    val subtitle: String = "",
    val shortDescription: String = "",     // Short description
    val type: String = "",            // (GALAXY, PLANET, MOON, STATION, SPOT)
    val level: Int = 0,
    val priceTier: Int = 0,
    val imageUrl: String = "",
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val popularityScore: Double = 0.0,
    val childCount: Int = 0,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val details: DestinationDetails? = null,
    val bodyType: String = "",        // (PLANET, MOON, STATION)
    val difficulty: Int = 0,
    val durationMinutes: Int = 0,
    val locationCategory: String = "",
) : Serializable

data class DestinationDetails(
    val longDescription: String = "",
    val highlights: List<String> = emptyList(),
    val bestTimeToVisit: BestTimeToVisit? = null,
    val safety: Safety? = null,
    val environment: Environment? = null,
    val logistics: Logistics? = null,
    val gearChecklist: List<String> = emptyList()
) : Serializable

data class BestTimeToVisit(
    val summary: String = ""
) : Serializable

data class Safety(
    val overallLevel: Int = 0,
    val notes: List<String> = emptyList(),
    val requirements: List<String> = emptyList()
) : Serializable

data class Environment(
    val gravityG: Double = 0.0,
    val atmosphere: String = "",
    val temperatureC: Temperature? = null,
    val radiation: String = "",
    val dayLengthHours: Double = 0.0,
    val visibility: String = ""
) : Serializable

data class Temperature(
    val min: Int = 0,
    val max: Int = 0
) : Serializable

data class Logistics(
    val travelTimeFromEarth: TravelTime? = null,
    val typicalRoute: List<String> = emptyList(),
    val localTransport: List<String> = emptyList(),
    val priceNotes: String = "",
    val access: String = "",
    val fromMainHubMinutes: Int = 0,
    val routeHints: List<String> = emptyList()
) : Serializable

data class TravelTime(
    val value: Int = 0,
    val unit: String = ""
) : Serializable
