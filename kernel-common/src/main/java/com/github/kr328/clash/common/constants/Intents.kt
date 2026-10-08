package com.github.kr328.clash.common.constants

import com.github.kr328.clash.common.util.packageName

object Intents {
    // 应用内部广播动作（内核服务与应用间通信）
    val ACTION_CLASH_STARTED = "$packageName.intent.action.CLASH_STARTED"
    val ACTION_CLASH_STOPPED = "$packageName.intent.action.CLASH_STOPPED"
    val ACTION_CLASH_REQUEST_STOP = "$packageName.intent.action.CLASH_REQUEST_STOP"
    val ACTION_TUN_RESTART = "$packageName.intent.action.TUN_RESTART"
    val ACTION_PROFILE_CHANGED = "$packageName.intent.action.PROFILE_CHANGED"
    val ACTION_PROFILE_LOADED = "$packageName.intent.action.PROFILE_LOADED"
    val ACTION_OVERRIDE_CHANGED = "$packageName.intent.action.OVERRIDE_CHANGED"

    const val EXTRA_STOP_REASON = "stop_reason"
    const val EXTRA_UUID = "uuid"
}
