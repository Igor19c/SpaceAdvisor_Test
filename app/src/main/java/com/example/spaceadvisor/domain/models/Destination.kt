package com.example.spaceadvisor.domain.models

import java.io.Serializable

data class Destination(
    val id: String = "",
    val parentId: String = "root",
    val title: String = "",
    val subtitle: String = "",
    val shortDescription: String = "",
    val type: String = "",
    val level: Int = 0,
    val priceTier: Int = 0,
    val imageUrl: String = "",
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val childCount: Int = 0,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val details: DestinationDetails? = null,
    val difficulty: Int = 0,
    val travelTimeFromEarth: TravelTime? = null,
    val safetyLevel: Int = 0
) : Serializable

data class DestinationDetails(
    val longDescription: String = "",
    val bestTimeToVisit: String = "",
    val highlights: List<String> = emptyList(),
    val safety: Safety? = null,
    val environment: Environment? = null,
) : Serializable

data class Safety(
    val notes: List<String> = emptyList(),
    val hazards: List<Hazard> = emptyList()
) : Serializable

data class Hazard(
    val name: String = "",
    val level: Int = 0,
    val tip: String = ""
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

data class TravelTime(
    val value: Int = 0,
    val unit: String = ""
) : Serializable
