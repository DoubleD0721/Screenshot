package com.android.screenshot.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.screenshot.activity.PermissionsActivity

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/25
 * @author kongdebin.0721@bytedance.com
 */
object PermissionsUtils: Logger {

    fun request(context: Context, permission: Array<String>, listener: PermissionListener) {
        // 注册接收权限结果的 BroadcastReceiver
        val permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val permissions = intent.getStringArrayExtra(PermissionsActivity.EXTRA_PERMISSIONS) ?: arrayOf()
                    val grantResults = intent.getIntArrayExtra(PermissionsActivity.EXTRA_PERMISSIONS_RESULT)
                    grantResults?.forEach { result ->
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            listener.onPermissionDenied(permissions)
                            return
                        }
                    }
                    listener.onPermissionGrant(permissions)
                } catch (e: Exception) { error { e.toString() } } finally {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
                }
            }
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(permissionReceiver, IntentFilter(
            PermissionsActivity.ACTION_PERMISSIONS_RESPONSE))
        PermissionsActivity.startForResult(context, permission)
    }
}

interface PermissionListener {
    fun onPermissionGrant(permissions: Array<String>)
    fun onPermissionDenied(permissions: Array<String>)
}