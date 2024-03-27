package com.android.screenshot.functions

import android.content.Context
import com.android.screenshot.R
import com.android.screenshot.screenshot.ScreenShotEventDispatcher
import com.android.screenshot.screenshot.ScreenShotMonitor
import com.android.screenshot.utils.AppMonitor
import com.android.screenshot.utils.activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
class ScreenShotEndFunction(context: Context): AbsFunction(context) {
    override fun label(): Int = R.string.function_end_monitor_screenshot

    override fun clickAction() {
        CoroutineScope(Dispatchers.IO).launch {
            ScreenShotMonitor.instance().stopListen(WeakReference(context.activity ?: AppMonitor.getCurrentActivity()))
            ScreenShotEventDispatcher.refreshScreenShotConfig()
        }
    }
}