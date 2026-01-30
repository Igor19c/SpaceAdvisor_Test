package com.example.spaceadvisor.data

import java.io.Serializable

data class User(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long? = null,
    val bio: String = "",
    val favoriteDestinations: List<Int> = listOf(),
    val badges: List<String> = listOf() // רשימת שמות הקבצים של התגים (למשל ["badge1.png", "badge2.png"])
) : Serializable
