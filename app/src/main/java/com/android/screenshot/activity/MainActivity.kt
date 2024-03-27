package com.android.screenshot.activity

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.screenshot.databinding.ActivityMainBinding
import com.android.screenshot.functions.ScreenShotStartFunction
import com.android.screenshot.adapter.FunctionAdapter
import com.android.screenshot.functions.ScreenShotEndFunction

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        val functionsRecyclerView = binding.functionsContainer
        functionsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false) // 设置布局管理器
        functionsRecyclerView.adapter = FunctionAdapter(initFunctions(), binding.functionsView)
    }

    private fun initFunctions() = listOf(
        ScreenShotStartFunction(this),
        ScreenShotEndFunction(this),
    )
}