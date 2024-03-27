package com.android.screenshot.utils

import android.util.Log

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/25
 * @author kongdebin.0721@bytedance.com
 */
interface Logger {
    fun tag(): String = this.javaClass.simpleName

    fun debug(func: () -> String) = Log.d("Screenshot-" + tag(), func.invoke())

    fun info(func: () -> String) = Log.i("Screenshot-" + tag(), func.invoke())

    fun warn(func: () -> String) = Log.w("Screenshot-" + tag(), func.invoke())

    fun error(func: () -> String) = Log.e("Screenshot-" + tag(), func.invoke())
}