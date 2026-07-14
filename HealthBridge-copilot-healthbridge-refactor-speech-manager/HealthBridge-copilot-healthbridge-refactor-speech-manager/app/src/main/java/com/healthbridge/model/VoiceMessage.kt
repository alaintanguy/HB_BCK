package com.healthbridge.model

data class VoiceMessage(
    val from: String = "",
    val to: String = "",
    val text: String = "",
    val status: String = "",
    val timestamp: Long = 0L
)
