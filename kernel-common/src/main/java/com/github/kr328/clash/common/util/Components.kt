package com.github.kr328.clash.common.util

import android.content.ComponentName
import com.github.kr328.clash.common.Global
import kotlin.reflect.KClass

val KClass<*>.componentName: ComponentName
    get() = ComponentName(Global.application.packageName, this.java.name)
