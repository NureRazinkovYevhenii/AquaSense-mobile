package com.example.aquasense.models

data class Aquarium(
    val id: Long,
    val name: String,
    val description: String?,
    val temperature: Float,
    val isLightOn: Boolean,
    val lastFeedTime: String
)