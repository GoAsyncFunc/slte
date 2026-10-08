package com.github.kr328.clash.service.remote

import com.github.kr328.clash.service.model.Profile
import com.github.kr328.kaidl.BinderInterface
import java.util.*

@BinderInterface
interface IProfileManager {
    suspend fun create(type: Profile.Type, name: String, source: String = "", ageSecretKey: String? = null): UUID
    suspend fun commit(uuid: UUID)
    suspend fun delete(uuid: UUID)
    suspend fun queryByUUID(uuid: UUID): Profile?
    suspend fun queryAll(): List<Profile>
    suspend fun queryActive(): Profile?
    suspend fun setActive(profile: Profile)
}
