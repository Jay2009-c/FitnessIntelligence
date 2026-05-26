package com.example.fitnessanalyzer.domain.model

data class UserProfile(
    val age: Int? = null,
    val weightKg: Float? = null,
    val heightCm: Float? = null,
    val isMale: Boolean? = null
)
