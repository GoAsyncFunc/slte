package com.slte.app.domain.model

data class AppUpdate(
    val version: String = "",
    val changelogTitle: String = "",
    val changelog: String = "",
    val force: Boolean = false,
    val downloadUrl: String = "",
)
