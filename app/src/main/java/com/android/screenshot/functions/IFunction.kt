package com.android.screenshot.functions

import android.view.View
import androidx.annotation.StringRes

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
interface IFunction {

    @StringRes
    fun label(): Int

    fun clickAction()

    fun view(): View?
}