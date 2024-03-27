package com.android.screenshot.screenshot

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/25
 * @author kongdebin.0721@bytedance.com
 */
abstract class ScreenShotListener {
    /**
     * ### Screen shot
     * @param imagePath screenshot image file path
     */
    abstract fun onShot(imagePath: String?)
}