package com.slte.app.data.remote

import com.slte.app.data.remote.adapter.xboard.XboardAuthApi
import com.slte.app.data.remote.adapter.xboard.XboardAuthRetrofit
import com.slte.app.data.remote.adapter.xboard.XboardUserPlanRetrofit
import com.slte.app.data.remote.adapter.xboard.XboardUserRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bAuthApi
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bAuthRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserPlanRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserRetrofit
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.utils.ApiErrors
import javax.inject.Inject
import javax.inject.Singleton

/** Selects a panel adapter after the shared transport has been built. */
@Singleton
class BackendAdapterFactory
@Inject
constructor(
    private val retrofitFactory: ApiRetrofitFactory,
) {
    fun createAuthApi(backend: ApiBackend): AuthApi {
        val backendType = ApiBackendType.fromConfig(backend.type)
            ?: throw ApiException("不支持的后端类型: ${backend.type}", ApiErrors.UNSUPPORTED_BACKEND)
        val retrofit = retrofitFactory.create(backend)
        return when (backendType) {
            ApiBackendType.V2BOARD -> XiaoV2bAuthApi(
                authApi = retrofit.create(XiaoV2bAuthRetrofit::class.java),
                userApi = retrofit.create(XiaoV2bUserRetrofit::class.java),
                userPlanApi = retrofit.create(XiaoV2bUserPlanRetrofit::class.java),
            )
            ApiBackendType.XBOARD -> XboardAuthApi(
                authApi = retrofit.create(XboardAuthRetrofit::class.java),
                userApi = retrofit.create(XboardUserRetrofit::class.java),
                userPlanApi = retrofit.create(XboardUserPlanRetrofit::class.java),
            )
        }
    }
}
