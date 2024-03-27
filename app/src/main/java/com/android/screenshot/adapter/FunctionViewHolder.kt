package com.android.screenshot.adapter

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.screenshot.R
import com.android.screenshot.functions.IFunction
import com.android.screenshot.utils.Logger

/**
 * Created by Debin Kong on 2024/3/22
 * @author Debin Kong
 */
class FunctionViewHolder(itemView: FunctionButton) : RecyclerView.ViewHolder(itemView), Logger {

    fun setUpView(function: IFunction, viewGroup: ViewGroup) {
        val labelView = itemView.findViewById<TextView>(R.id.function_label)
        val buttonContainer = itemView.findViewById<FrameLayout>(R.id.function_container)

        labelView?.setText(function.label())
        buttonContainer?.setOnClickListener {
            function.clickAction()
            viewGroup.removeAllViews()
            function.view()?.let {  view ->
                viewGroup.addView(view)
            }
        }
    }

}