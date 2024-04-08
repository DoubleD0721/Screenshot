# 前瞻
目前Android针对截屏的监控主要有三种方式：

1. 利用FileObserver监听某个目录中资源的变化
2. 利用ContentObserver监听全部资源的变化
3. 直接监听截屏快捷键(由于不同的厂商自定义的原因，使用这种方法进行监听比较困难)

本文主要使用ContentObserver的方式来实现对截屏的监控。

# Android 各版本适配
主要针对Android 13及Android 14更新的存储权限进行适配。

在Android 13中，存储权限从原来的`READ_EXTERNAL_STORAGE`细化成为`READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`/`READ_MEDIA_AUDIO`三种权限，在进行权限判断的时候需要进行版本区分。

在Android 14中，存储权限从Android 13的细化权限中更新成为允许用户选择部分图片资源给应用访问。但是针对截屏增加了一个新的截屏监控权限`DETECT_SCREEN_CAPTURE`，该权限默认为开且用户无感知，针对用户只给部分权限的情况，我们可以通过该权限来获取用户的截屏动作，尝试一些不依赖截屏文件的操作。

|权限状态|Android 13及以下机型|Android 14及以上机型|
|----|----|---|
|有全部相册权限|使用媒体库监控实现监控|使用媒体库监控实现监控
|有部分相册权限|无法进行监控|使用系统API进行监控(但无法拿到截屏文件)
|没有相册权限|无法进行监控|使用系统API进行监控(但无法拿到截屏文件)

## Android 13及以下机型监控
> 针对Android 13及以下用户，使用监听媒体库方式进行截屏的监控
### 1. 建立相关截屏媒体库，分别监控内部存储及外部存储
```kotlin
private inner class MediaContentObserver(private val contentUri: Uri, handler: Handler?) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        // handle screenshot file
    }
}
```
在合适时机，通过`registerActivityLifecycleCallbacks`的方法将截屏的开始监控及取消监控注入到每个activity的生命周期中。将开始监控媒体库方法注入每个activity的`onResume`中，将停止监控注入每个activity的`onPause`中，保证activity在展示的时候开始监控截屏，在消失的时候结束对截屏的监控。
```kotlin
application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            startListen(WeakReference(activity))
        }
    }

    override fun onActivityPaused(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            stopListen(WeakReference(activity))
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityDestroyed(activity: Activity) {}

})
```
如果不希望这样实现，也可以直接将相关能力注入到需要被监控activity的生命周期中，而不是所有的activity。

在对应的生命周期中实现对媒体库的绑定与解绑。
```kotlin
private fun registerObserver(activity: Activity) {
    internalObserver = MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, uiHandler)
    externalObserver = MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, uiHandler)
    startListenTime = System.currentTimeMillis()
    internalObserver?.let {
        activity.applicationContext.contentResolver.registerContentObserver(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            Build.VERSION.SDK_INT > Build.VERSION_CODES.P, it,
        )
    }
    externalObserver?.let {
        activity.applicationContext.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            Build.VERSION.SDK_INT > Build.VERSION_CODES.P, it,
        )
    }
}

private fun unregisterObserver(activity: Activity) {
    try {
        internalObserver?.let {
            activity.contentResolver.unregisterContentObserver(it)
        }
    } catch (_: Exception) {}
    internalObserver = null

    try {
        externalObserver?.let {
            activity.contentResolver.unregisterContentObserver(it)
        }
    } catch (_: Exception) {}
    externalObserver = null
    startListenTime = 0
}
```
### 2. 监听到媒体库变化后，获取最新的文件并判断是否是截屏文件
#### 2.1 获取最新媒体库文件
获取最新文件主要通过contentResolver通过DATE_MODIFIED来倒序获取第一个
```kotlin
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
```
其中，针对不同版本的Android机型，获取的字段也做了相应的处理
- Android 10及以上
``` kotlin
val MEDIA_PROJECTIONS_API_16 = arrayOf(
    MediaStore.Images.ImageColumns.DATA,
    MediaStore.Images.ImageColumns.DATE_ADDED,
    MediaStore.Images.ImageColumns.WIDTH,
    MediaStore.Images.ImageColumns.HEIGHT,
)
```
- Android 10以下
``` kotlin 
val MEDIA_PROJECTIONS = arrayOf(
    MediaStore.Images.ImageColumns.DATA,
    MediaStore.Images.ImageColumns.DATE_ADDED,
)
```
#### 2.2 判断是否为截屏文件
判断是否为截屏文件主要通过以下三个维度来进行判断
- 路径维度
- 时间维度
- 尺寸维度
##### 路径维度
判断获取到的文件路径是否包含screenshot相关字段
``` kotlin
private fun isFilePathLegal(filePath: String?): Boolean {
    // File path is not empty
    if (filePath == null || TextUtils.isEmpty(filePath)) {
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
```
其中的关键字包括：
```kotlin
private val KEYWORDS = arrayOf(
    "screenshot", "screen_shot", "screen-shot", "screen shot",
    "screencapture", "screen_capture", "screen-capture", "screen capture",
    "screencap", "screen_cap", "screen-cap", "screen cap",
)
```
##### 时间维度
> 判断文件创建的时间是否晚于开始监听截屏的时间同时文件创建的时间和当前时间相差小于10s
```kotlin
private fun isFileCreationTimeLegal(dateAdded: Long?, startListenTime: Long?) =
    if (dateAdded == null
        || startListenTime == null
        || dateAdded / 1000 < startListenTime / 1000
        || (System.currentTimeMillis() - dateAdded) > MAX_COST_TIME
    ) false else true
```
##### 尺寸维度
> 判断获取图片的大小和手机尺寸的大小是否一致
```kotlin
private fun isFileSizeLegal(width: Int?, height: Int?) =
    screenRealSize?.let {
        if (width == null || height == null) {
            false
        } else if (!((width <= it.x && height <= it.y) || (height <= it.x && width <= it.y))) {
            false
        } else {
            true
        }
    } ?: false
```
下面是获取屏幕尺寸的方法
```kotlin
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
```
### 3. 处理截屏文件
当判断为是截屏文件后，对截屏文件进行处理，这里通过一个全局变量的listener来控制监听到截屏后的动作，针对不同的场景对listener做动态的更新。
完整的截屏文件判断流程：
```kotlin
fun handleMediaContentChange(
    contentUri: Uri,
    context: Context?,
    startListenTime: Long?
) {
    CoroutineScope(Dispatchers.IO).launch {
        if (context == null) return@launch
        if (screenRealSize == null) screenRealSize = getRealScreenSize(context)

        var cursor: Cursor? = null

        try {
            cursor = getContentResolverCursor(contentUri, context)
        } catch (_: Exception) { }

        if (cursor == null || !cursor.moveToFirst()) return@launch

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
```
## Android 14及以上机型
使用系统API`Activity.ScreenCaptureCallback`进行监控，但是由于没有全部相册权限获取不到截屏文件的具体路径，所以只能实现一些不依赖路径的动作(如埋点上报等)
### 1. 声明相关callback
```kotlin
private var screenShotCaptureCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    Activity.ScreenCaptureCallback { 
        // handle screenshot
    }
} else null
```
### 2. 注册监听
在activity启动的时候开始对截屏进行监听，在activity消失的时候结束对截屏的监听，时机与使用媒体库监听时机一样
```kotlin
private fun registerCallback(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        screenShotCaptureCallback?.let { callback ->
            activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
        }
    }
}

private fun unregisterCallback(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        screenShotCaptureCallback?.let { callback ->
            try {
                activity.unregisterScreenCaptureCallback(callback)
                startListenTime = 0
            } catch (_: IllegalStateException) {}
        }
    }
}
```
