package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.util.Parcelizer
import kotlinx.serialization.Serializable

@Serializable
data class Proxy(
    val name: String,
    val title: String,
    val subtitle: String,
    val type: String,
    val delay: Int,
    var isGroup: Boolean,
    /** 内核在该节点的当前测速 URL 上已有历史（含失败）；false = 还没测到，delay 不可当结果 */
    val measured: Boolean = false,
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Proxy> {
        override fun createFromParcel(parcel: Parcel): Proxy = Parcelizer.decodeFromParcel(serializer(), parcel)

        override fun newArray(size: Int): Array<Proxy?> = arrayOfNulls(size)
    }
}
