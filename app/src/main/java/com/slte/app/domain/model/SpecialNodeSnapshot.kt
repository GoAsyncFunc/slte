package com.slte.app.domain.model

import kotlinx.serialization.Serializable

/** Last selected automatic or fallback node, used while the kernel is offline. */
@Serializable
data class SpecialNodeSnapshot(
    val kernelName: String,
    val displayName: String,
    val countryCode: String,
    val delay: Int? = null,
)
