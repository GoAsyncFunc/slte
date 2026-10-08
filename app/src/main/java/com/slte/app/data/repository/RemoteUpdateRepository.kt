package com.slte.app.data.repository

import com.slte.app.data.remote.config.RemoteConfig
import com.slte.app.data.remote.config.RemoteConfigData
import com.slte.app.di.IoDispatcher
import com.slte.app.domain.model.AppUpdate
import com.slte.app.domain.repository.UpdateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RemoteUpdateRepository
@Inject
constructor(
    private val remoteConfig: RemoteConfig,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRepository {
    override val updates: Flow<AppUpdate> = remoteConfig.dataFlow.map { it.toAppUpdate() }

    override fun current(): AppUpdate = remoteConfig.data.toAppUpdate()

    override suspend fun refresh(): Boolean = withContext(ioDispatcher) {
        remoteConfig.refresh(force = true)
    }

    private fun RemoteConfigData.toAppUpdate() = AppUpdate(
        version = updateVersion,
        changelogTitle = updateChangelogTitle,
        changelog = updateChangelog,
        force = updateForce,
        downloadUrl = updateApkUrl,
    )
}
