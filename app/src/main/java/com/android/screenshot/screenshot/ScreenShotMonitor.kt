package com.android.screenshot.screenshot

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.android.screenshot.screenshot.ScreenShotEventDispatcher.handleMediaContentChange
import com.android.screenshot.screenshot.ScreenShotEventDispatcher.handleScreenShot
import com.android.screenshot.utils.Logger
import com.android.screenshot.utils.PermissionListener
import com.android.screenshot.utils.PermissionsUtils
import com.android.screenshot.utils.Weak
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
class ScreenShotMonitor: Logger {

    private var isHasScreenShotListen = false

    private var isHasScreenShotCaptureCallback = false

    private var internalObserver: MediaContentObserver? = null

    private var externalObserver: MediaContentObserver? = null

    private val uiHandler = Handler(Looper.getMainLooper())

    private var startListenTime: Long = 0
        set(value) {
            field = value
            info { "startListenTime change to $value" }
        }

    private var screenShotCaptureCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Activity.ScreenCaptureCallback {
            handleScreenShot(null)
        }
    } else null

    private var activity: Activity? by Weak { null }

    companion object {
        private val monitor: ScreenShotMonitor by lazy { ScreenShotMonitor() }

        /**
         * ### INSTANCE
         * @return [ScreenShotMonitor]
         */
        fun instance() = monitor
    }

    suspend fun startListen(weakActivity: WeakReference<Activity>) = withContext(Dispatchers.IO) {
        weakActivity.get()?.let { activity ->
            this@ScreenShotMonitor.activity = activity
            info { "Activity[${activity}]: Try to start screen shot listener: isHasScreenShotListen: $isHasScreenShotListen isHasScreenShotCaptureCallback: $isHasScreenShotCaptureCallback" }
            if (!isHasScreenShotListen && !isHasScreenShotCaptureCallback) {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_IMAGES
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE

                try {
                    if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                        registerObserver(activity)
                    } else {
                        PermissionsUtils.request(activity, arrayOf(permission), object :
                            PermissionListener {
                            override fun onPermissionGrant(permissions: Array<String>) { registerObserver(activity) }
                            override fun onPermissionDenied(permissions: Array<String>) { registerCallback(activity) }
                        })
                    }
                } catch (exception: Exception) { error { exception.toString() } }
            }
        }
    }

    suspend fun stopListen(weakActivity: WeakReference<Activity>) = withContext(Dispatchers.IO) {
        weakActivity.get()?.let { activity ->
            this@ScreenShotMonitor.activity = null
            info { "Activity[${activity}]: Try to stop screen shot listener: isHasScreenShotListen: $isHasScreenShotListen isHasScreenShotCaptureCallback: $isHasScreenShotCaptureCallback" }
            if (isHasScreenShotListen) { unregisterObserver(activity) }
            if (isHasScreenShotCaptureCallback) { unregisterCallback(activity) }
        }
    }

    private fun registerObserver(activity: Activity) {
        internalObserver = MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, uiHandler)
        externalObserver = MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, uiHandler)
        startListenTime = System.currentTimeMillis()
        internalObserver?.let {
            activity.applicationContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                Build.VERSION.SDK_INT > Build.VERSION_CODES.P, it,
            )
        }
        externalObserver?.let {
            activity.applicationContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                Build.VERSION.SDK_INT > Build.VERSION_CODES.P, it,
            )
        }
        info {"[${activity}] start listen" }
        isHasScreenShotListen = true
    }

    private fun unregisterObserver(activity: Activity) {
        try {
            internalObserver?.let {
                activity.contentResolver.unregisterContentObserver(it)
            }
        } catch (e: Exception) { error { e.toString() } }
        internalObserver = null

        try {
            externalObserver?.let {
                activity.contentResolver.unregisterContentObserver(it)
            }
        } catch (e: Exception) { error { e.toString() } }
        externalObserver = null

        info { "[${activity}] Stop listen" }
        isHasScreenShotListen = false
        startListenTime = 0
    }

    private fun registerCallback(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenShotCaptureCallback?.let { callback ->
                activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
                isHasScreenShotCaptureCallback = true
                info {"[${activity}] start listen" }
            }
        }
    }

    private fun unregisterCallback(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenShotCaptureCallback?.let { callback ->
                try {
                    activity.unregisterScreenCaptureCallback(callback)
                    isHasScreenShotCaptureCallback = false
                    startListenTime = 0
                    info { "[${activity}] Stop listen" }
                } catch (e: IllegalStateException) {
                    isHasScreenShotCaptureCallback = false
                    error { e.toString() }
                }
            }
        }
    }

    private inner class MediaContentObserver(private val contentUri: Uri, handler: Handler?) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMediaContentChange(contentUri, activity, startListenTime)
        }
    }
}