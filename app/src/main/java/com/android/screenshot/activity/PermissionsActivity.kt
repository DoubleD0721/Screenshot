package com.android.screenshot.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.screenshot.R

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
class PermissionsActivity : BaseActivity() {

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1
        const val EXTRA_PERMISSIONS = "extra_permissions"
        const val ACTION_PERMISSIONS_RESPONSE = "action_permissions_response"
        const val EXTRA_PERMISSIONS_RESULT = "extra_permissions_result"

        fun startForResult(context: Context, permissions: Array<String>) {
            val intent = Intent(context, PermissionsActivity::class.java).apply {
                putExtra(EXTRA_PERMISSIONS, permissions)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Transparent)
        val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)
        if (permissions.isNullOrEmpty()) {
            finish()
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
        }
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val intent = Intent(ACTION_PERMISSIONS_RESPONSE).apply {
                putExtra(EXTRA_PERMISSIONS, permissions)
                putExtra(EXTRA_PERMISSIONS_RESULT, grantResults)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            finish()
        }
    }
}