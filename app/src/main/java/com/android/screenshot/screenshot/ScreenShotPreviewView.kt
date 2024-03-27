package com.android.screenshot.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.Px
import com.android.screenshot.R
import com.android.screenshot.databinding.ViewScreenshotPreviewBinding
import com.android.screenshot.utils.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Debin Kong on 2024/3/26
 * @author Debin Kong
 */
class ScreenShotPreviewView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewScreenshotPreviewBinding

    init {
        binding = ViewScreenshotPreviewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun updateSetting(path: String?) {
        binding.screenshotPath.text = path ?: context.getString(R.string.empty_path)
        CoroutineScope(Dispatchers.Main).launch {
            path?.let {
                val bitmap = getScreenShotBitmap(it)
                binding.screenshotPreview.setImageBitmap(bitmap)
            }
        }
    }

    private suspend fun getScreenShotBitmap(
        path: String,
        @Px borderWidth: Int = 2.dp,
        @ColorRes borderColor: Int = R.color.black
    ): Bitmap = withContext(Dispatchers.IO) {
        val originalBitmap = BitmapFactory.decodeFile(path)
        val newBitmapWidth = originalBitmap.width + borderWidth * 2
        val newBitmapHeight = originalBitmap.height + borderWidth * 2
        val newBitmap = Bitmap.createBitmap(newBitmapWidth, newBitmapHeight, originalBitmap.config)
        val canvas = Canvas(newBitmap)
        val paint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth.toFloat()
        }
        val borderRect = Rect(
            borderWidth / 2,
            borderWidth / 2,
            newBitmapWidth - borderWidth / 2,
            newBitmapHeight - borderWidth / 2
        )
        canvas.drawRect(borderRect, paint)
        val originalBitmapRect = Rect(0, 0, originalBitmap.width, originalBitmap.height)
        val newBitmapRect = Rect(borderWidth, borderWidth, newBitmapWidth - borderWidth, newBitmapHeight - borderWidth)
        canvas.drawBitmap(originalBitmap, originalBitmapRect, newBitmapRect, null)
        return@withContext newBitmap
    }
}