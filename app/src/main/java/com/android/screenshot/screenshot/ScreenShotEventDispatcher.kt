package com.android.screenshot.screenshot

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Display
import android.view.WindowManager
import com.android.screenshot.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Created by kongdebin.0721@bytedance.com on 2024/3/25
 * @author kongdebin.0721@bytedance.com
 */
object ScreenShotEventDispatcher: Logger {

    private var screenRealSize: Point? = null

    /**
     * Listener brought with Config configuration, only holds one and will be replaced by a new page config at any time
     */
    private var screenShotConfigListener: ScreenShotListener? = null

    /**
     * ### Set screenshot config
     * @param enterFrom String
     * @param screenShotListener [ScreenShotListener]?
     */
    fun setScreenShotConfig(screenShotListener: ScreenShotListener?) {
        screenShotConfigListener = screenShotListener
    }

    /**
     * ### Refresh screenshot config
     */
    fun refreshScreenShotConfig() {
        screenShotConfigListener = null
    }

    /**
     * ### Handle media content change (get 1st data and determine whether it is a screenshot file)
     * @param contentUri Uri
     * @param context Context?
     * @param startListenTime Long
     */
    fun handleMediaContentChange(
        contentUri: Uri,
        context: Context?,
        startListenTime: Long?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (context == null) {
                error { "context is null" }
                return@launch
            }
            if (screenRealSize == null) screenRealSize = getRealScreenSize(context)

            var cursor: Cursor? = null

            try {
                cursor = getContentResolverCursor(contentUri, context)
            } catch (_: Exception) { }

            if (cursor == null || !cursor.moveToFirst()) {
                error { "cannot move to first" }
                return@launch
            }

            // Get all of colum index
            with(cursor) {
                val dataIndex = getColumnIndex(MediaStore.Images.ImageColumns.DATA) ?: -1
                val dateAddedIndex = getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED) ?: -1
                val widthIndex = getColumnIndex(MediaStore.Images.ImageColumns.WIDTH) ?: -1
                val heightIndex = getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT) ?: -1

                // Handle media row data
                // File path
                val filePath = cursor.getScreenShotFilePath(dataIndex)
                if (!isFilePathLegal(filePath)) return@with
                // File Date Added
                val dateAdded = cursor.getScreenShotFileDateAdded(dateAddedIndex)
                if (!isFileCreationTimeLegal(dateAdded, startListenTime)) return@with
                // File Size
                val (width, height) = cursor.getScreenShotFileSize(filePath, widthIndex, heightIndex)
                if (!isFileSizeLegal(width, height)) return@with
                handleScreenShot(filePath)
            }
            if (!cursor.isClosed) cursor.close()
        }
    }

    /**
     * ### Handle screenshot
     * @param filePath String
     */
    fun handleScreenShot(filePath: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            screenShotConfigListener?.onShot(filePath)
        }
    }

    /**
     * ### Get screenshot file path
     * @receiver [Cursor]
     * @return [String]
     */
    private fun Cursor.getScreenShotFilePath(dataIndex: Int): String = this.getString(dataIndex) ?: ""

    /**
     * ### Get screenshot file date added
     * @receiver [Cursor]
     * @return [Long]
     */
    private fun Cursor.getScreenShotFileDateAdded(dateAddedIndex: Int): Long = (this.getLong(dateAddedIndex) * 1000) ?: 0L

    /**
     * ### Get screenshot file size
     * @receiver [Cursor]
     * @return [Pair]
     */
    private fun Cursor.getScreenShotFileSize(filePath: String, widthIndex: Int, heightIndex: Int): Pair<Int, Int> =
        if (widthIndex >= 0 && heightIndex >= 0) {
            Pair(this.getInt(widthIndex) ?: 0, this.getInt(heightIndex) ?: 0)
        } else {
            // Before API 16, the width and height need to be obtained manually.
            val size = getImageSize(filePath)
            Pair(size.x ?: 0, size.y ?: 0)
        }

    /**
     * ### Determine the file path
     * @param filePath String
     * @return Boolean
     */
    private fun isFilePathLegal(filePath: String?): Boolean {
        // File path is not empty
        if (filePath == null || TextUtils.isEmpty(filePath)) {
            warn { "error: path $filePath" }
            return false
        }

        // File path contains screenshot KEYWORDS
        var hasValidScreenShot = false
        val lowerPath = filePath.lowercase(Locale.getDefault())
        for (keyWork: String in KEYWORDS) {
            if (lowerPath.contains(keyWork)) {
                hasValidScreenShot = true
                break
            }
        }
        return hasValidScreenShot
    }

    /**
     * ### Determine the file creation time
     * If the time added to the database is before the start of listening or the difference is greater than 10 seconds from the current time, the screenshot file is considered not the current file.
     * @param dateAdded Long
     * @param startListenTime Long
     * @return Boolean
     */
    private fun isFileCreationTimeLegal(dateAdded: Long?, startListenTime: Long?) =
        if (dateAdded == null
            || startListenTime == null
            || dateAdded / 1000 < startListenTime / 1000
            || (System.currentTimeMillis() - dateAdded) > MAX_COST_TIME
        ) {
            warn {
                "error: time doesn't match\n" +
                    "  dateAdded: $dateAdded \n" +
                    "  startListenTime: $startListenTime \n"
            }
            false
        } else true

    /**
     * ### Determine the file size.
     * If the image size exceeds the screen, it is considered that the current screenshot file is not a local screenshot.
     * @param width Int
     * @param height Int
     * @return Boolean
     */
    private fun isFileSizeLegal(width: Int?, height: Int?) =
        screenRealSize?.let {
            if (width == null || height == null) {
                false
            } else if (!((width <= it.x && height <= it.y) || (height <= it.x && width <= it.y))) {
                warn { "error: size" }
                false
            } else {
                true
            }
        } ?: false


    /**
     * ### Get real screen size
     * @param context Context
     * @return [Point]?
     */
    private fun getRealScreenSize(context: Context): Point? {
        var screenSize: Point? = null
        try {
            screenSize = Point()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val defaultDisplay = windowManager.defaultDisplay
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                defaultDisplay.getRealSize(screenSize)
            } else {
                try {
                    val rawWidth = Display::class.java.getMethod("getRawWidth")
                    val rawHeight = Display::class.java.getMethod("getRawHeight")
                    screenSize.set(
                        (rawWidth.invoke(defaultDisplay) as Int),
                        (rawHeight.invoke(defaultDisplay) as Int),
                    )
                } catch (_: Exception) {
                    screenSize.set(defaultDisplay.width, defaultDisplay.height)
                }
            }
        } catch (_: Exception) { }
        return screenSize
    }

    /**
     * ### Get image size
     * @param imagePath String
     * @return [Point]
     */
    private fun getImageSize(imagePath: String): Point {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        return Point(options.outWidth, options.outHeight)
    }

    /**
     * ### Get content cursor
     * @param contentUri Uri
     * @param context Context
     * @param maxCount Int
     * @return [Cursor]
     */
    private fun getContentResolverCursor(
        contentUri: Uri,
        context: Context,
        maxCount: Int = 1
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val bundle = Bundle().apply {
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.ImageColumns.DATE_MODIFIED))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, maxCount)
        }
        context.contentResolver.query(
            contentUri,
            MEDIA_PROJECTIONS_API_16,
            bundle,
            null,
        )
    } else {
        context.contentResolver.query(
            contentUri,
            MEDIA_PROJECTIONS,
            null,
            null,
            "${MediaStore.Images.ImageColumns.DATE_MODIFIED} desc limit ${maxCount}",
        )
    }

    private class ScreenShotTrackerLRUMap :
        LinkedHashMap<String, Long>(MAX_CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > MAX_CAPACITY
        }

        companion object {
            private const val MAX_CAPACITY = 30
            private const val LOAD_FACTOR = 0.75f
        }
    }

    private const val MAX_COST_TIME = 10 * 1000
    private val MEDIA_PROJECTIONS = arrayOf(
        MediaStore.Images.ImageColumns.DATA,
        MediaStore.Images.ImageColumns.DATE_ADDED,
    )
    private val MEDIA_PROJECTIONS_API_16 = arrayOf(
        MediaStore.Images.ImageColumns.DATA,
        MediaStore.Images.ImageColumns.DATE_ADDED,
        MediaStore.Images.ImageColumns.WIDTH,
        MediaStore.Images.ImageColumns.HEIGHT,
    )
    private val KEYWORDS = arrayOf(
        "screenshot", "screen_shot", "screen-shot", "screen shot",
        "screencapture", "screen_capture", "screen-capture", "screen capture",
        "screencap", "screen_cap", "screen-cap", "screen cap",
    )
}