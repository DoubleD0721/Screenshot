package com.android.screenshot.utils

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
object AppMonitor {

    private var currentActivity: WeakReference<Activity>? = null

    fun setCurrentActivity(activity: Activity?) {
        currentActivity = if (activity == null) {
            null
        } else {
            WeakReference(activity)
        }
    }

    fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }
}