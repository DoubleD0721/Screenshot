package com.android.screenshot.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
val Context.activity: Activity?
    get() {
        var c: Context? = this
        while (c != null) {
            c = when (c) {
                is Activity -> return c
                is ContextWrapper -> c.baseContext
                else -> return null
            }
        }
        return null
    }

val Number.dpFloat
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        toFloat(),
        Resources.getSystem().displayMetrics
    )


inline val Number.dp
    get() = dpFloat.roundToInt()


val Number.spFloat
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

inline val Number.sp
    get() = spFloat.roundToInt()


inline val Number.px
    get() = this.toInt()


fun Context.getScreenWidth(): Int {
    val dm = resources.displayMetrics
    return dm?.widthPixels ?: 0
}

fun Context.getScreenHeight(): Int {
    val dm = resources.displayMetrics
    return dm?.heightPixels ?: 0
}