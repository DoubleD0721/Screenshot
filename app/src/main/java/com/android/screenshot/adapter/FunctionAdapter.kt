package com.android.screenshot.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.screenshot.R
import com.android.screenshot.functions.IFunction
import com.android.screenshot.utils.Logger

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/22
 * @author kongdebin.0721@bytedance.com
 */
class FunctionAdapter(
    private val functions: List<IFunction>,
    private val viewGroup: ViewGroup
) : RecyclerView.Adapter<FunctionViewHolder>(), Logger {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FunctionViewHolder {
        return FunctionViewHolder(FunctionButton(parent.context))
    }

    override fun getItemCount(): Int = functions.size

    override fun onBindViewHolder(holder: FunctionViewHolder, position: Int) {
        functions.getOrNull(position)?.let { function ->
            info { "set $position, label: ${function.label()}" }
            holder.setUpView(function, viewGroup)
        }
    }

    override fun getItemViewType(position: Int): Int = R.layout.view_function_item

}