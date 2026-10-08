package com.slte.app.domain.repository

import com.slte.app.domain.model.AppUpdate
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    val updates: Flow<AppUpdate>

    fun current(): AppUpdate

    suspend fun refresh(): Boolean
}
