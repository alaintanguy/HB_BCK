package com.healthbridge.models

data class HealthData(
    val name: String = "",
    val heartRate: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = ""
)