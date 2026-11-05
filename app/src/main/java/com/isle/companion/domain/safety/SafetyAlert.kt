package com.isle.companion.domain.safety

/** 给 UI 的非阻塞/阻塞安全提示。 */
data class SafetyAlert(
    val title: String,
    val message: String,
    val isCrisis: Boolean = false
)
