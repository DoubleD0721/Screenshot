package com.android.screenshot.functions

import android.content.Context
import android.view.View
import com.android.screenshot.utils.Logger

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/22
 * @author kongdebin.0721@bytedance.com
 */
abstract class AbsFunction(
    protected val context: Context
): IFunction, Logger {
    override fun view(): View? = null
}