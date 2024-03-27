package com.android.screenshot.functions

import android.view.View
import androidx.annotation.StringRes

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/25
 * @author kongdebin.0721@bytedance.com
 */
interface IFunction {

    @StringRes
    fun label(): Int

    fun clickAction()

    fun view(): View?
}