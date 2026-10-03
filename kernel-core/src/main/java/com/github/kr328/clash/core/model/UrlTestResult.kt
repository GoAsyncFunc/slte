package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.util.Parcelizer
import kotlinx.serialization.Serializable

/**
 * 单节点测速结论（内核 UrlTest 桥接返回值）。
 *
 * delay 为实测毫秒；kind 为失败分类：空 = 存活，[KIND_TIMEOUT] = 在但不回包，
 * [KIND_OFFLINE] = 后端已不存在（应用侧只把 offline 标成离线）。
 */
@Serializable
data class UrlTestResult(
    val delay: Int,
    val kind: String,
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UrlTestResult> {
        /** 节点存活（delay 为实测毫秒） */
        const val KIND_ALIVE = ""

        /** 服务器在，但测速窗口内没回包 */
        const val KIND_TIMEOUT = "timeout"

        /** 后端已不存在（域名解析失败/连接被拒/路由不可达） */
        const val KIND_OFFLINE = "offline"

        override fun createFromParcel(parcel: Parcel): UrlTestResult {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<UrlTestResult?> {
            return arrayOfNulls(size)
        }
    }
}
