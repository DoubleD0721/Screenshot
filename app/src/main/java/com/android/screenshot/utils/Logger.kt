package com.android.screenshot.utils

import android.util.Log

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
interface Logger {
    fun tag(): String = this.javaClass.simpleName

    fun debug(func: () -> String) = Log.d("Screenshot-" + tag(), func.invoke())

    fun info(func: () -> String) = Log.i("Screenshot-" + tag(), func.invoke())

    fun warn(func: () -> String) = Log.w("Screenshot-" + tag(), func.invoke())

    fun error(func: () -> String) = Log.e("Screenshot-" + tag(), func.invoke())
}