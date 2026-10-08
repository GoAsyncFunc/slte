package com.github.kr328.clash.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

class StatusProvider : ContentProvider() {
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            METHOD_CURRENT_PROFILE -> {
                return if (serviceRunning) {
                    Bundle().apply {
                        putString("name", currentProfile)
                    }
                } else {
                    null
                }
            }
            else -> super.call(method, arg, extras)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw IllegalArgumentException("Stub!")

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = throw IllegalArgumentException("Stub!")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw IllegalArgumentException("Stub!")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw IllegalArgumentException("Stub!")

    override fun getType(uri: Uri): String? = throw IllegalArgumentException("Stub!")

    override fun onCreate(): Boolean = true

    companion object {
        const val METHOD_CURRENT_PROFILE = "currentProfile"

        var serviceRunning: Boolean = false
        var currentProfile: String? = null
    }
}
