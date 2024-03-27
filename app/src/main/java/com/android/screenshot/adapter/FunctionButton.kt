package com.android.screenshot.adapter

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.android.screenshot.databinding.ViewFunctionItemBinding

/**
 * Created by Debin Kong on 2024/3/22
 * @author Debin Kong
 */
class FunctionButton(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewFunctionItemBinding

    init {
        binding = ViewFunctionItemBinding.inflate(LayoutInflater.from(context), this, true)
    }
}