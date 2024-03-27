package com.android.screenshot.activity

import androidx.appcompat.app.AppCompatActivity
import com.android.screenshot.utils.AppMonitor
import com.android.screenshot.utils.Logger

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/22
 * @author kongdebin.0721@bytedance.com
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