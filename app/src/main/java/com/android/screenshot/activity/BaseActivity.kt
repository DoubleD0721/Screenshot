package com.android.screenshot.activity

import androidx.appcompat.app.AppCompatActivity
import com.android.screenshot.utils.AppMonitor
import com.android.screenshot.utils.Logger

/**
 * Created by Debin Kong on 2024/3/22
 * @author Debin Kong
 */
abstract class BaseActivity : AppCompatActivity(), Logger {
    override fun onResume() {
        super.onResume()
        AppMonitor.setCurrentActivity(this)
    }

    override fun onPause() {
        super.onPause()
        AppMonitor.setCurrentActivity(null)
    }
}