package com.android.screenshot.functions

import android.content.Context
import android.view.View
import com.android.screenshot.R
import com.android.screenshot.screenshot.ScreenShotEventDispatcher
import com.android.screenshot.screenshot.ScreenShotListener
import com.android.screenshot.screenshot.ScreenShotMonitor
import com.android.screenshot.screenshot.ScreenShotPreviewView
import com.android.screenshot.utils.AppMonitor
import com.android.screenshot.utils.Weak
import com.android.screenshot.utils.activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
class ScreenShotStartFunction(context: Context): AbsFunction(context) {
    val previewView by Weak { ScreenShotPreviewView(context) }

    override fun label(): Int = R.string.function_start_monitor_screenshot

    override fun clickAction() {
        CoroutineScope(Dispatchers.IO).launch {
            ScreenShotMonitor.instance().startListen(WeakReference(context.activity ?: AppMonitor.getCurrentActivity()))
            ScreenShotEventDispatcher.setScreenShotConfig(object : ScreenShotListener() {
                override fun onShot(imagePath: String?) {
                    previewView?.updateSetting(imagePath)
                    info { "screenshot path: $imagePath" }
                }
            })
        }
    }

    override fun view(): View? = previewView
}