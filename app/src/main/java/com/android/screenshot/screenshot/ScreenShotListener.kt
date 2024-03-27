package com.android.screenshot.screenshot

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
abstract class ScreenShotListener {
    /**
     * ### Screen shot
     * @param imagePath screenshot image file path
     */
    abstract fun onShot(imagePath: String?)
}