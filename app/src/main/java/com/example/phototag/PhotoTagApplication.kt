package com.example.phototag

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.example.phototag.data.AppDatabase
import com.example.phototag.data.preference.UserPreferences
import com.example.phototag.data.repository.PhotoRepository
import com.example.phototag.data.repository.TagRepository
import com.example.phototag.util.CoilConfig

/**
 * 应用级 Application 类。
 */
class PhotoTagApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var instance: PhotoTagApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 全局崩溃异常兜底与日志记录（同时持久化写入 latest_crash.txt，杜绝黑盒闪退）
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("PhotoTagApp", "Uncaught exception in thread ${thread.name}", throwable)
            try {
                val baseDir = getExternalFilesDir(null) ?: filesDir
                val crashFile = java.io.File(baseDir, "latest_crash.txt")
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                crashFile.writeText(
                    "App Version: 8.10.3 (156)\n" +
                    "Time: ${java.util.Date()}\n" +
                    "Thread: ${thread.name}\n" +
                    "Exception: ${throwable.javaClass.name}: ${throwable.message}\n\n" +
                    "Stack Trace:\n$sw\n"
                )
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 创建全局通知渠道
        try {
            com.example.phototag.util.NotificationHelper.createNotificationChannels(this)
        } catch (e: Throwable) {
            android.util.Log.w("PhotoTagApp", "初始化通知渠道异常", e)
        }

        // 注册 7 天回收站超期自动巡检清理任务（安全容错，严防 WorkManager 启动异常导致冷启动闪退）
        try {
            com.example.phototag.worker.TrashCleanWorker.enqueuePeriodicWork(this)
        } catch (e: Throwable) {
            android.util.Log.w("PhotoTagApp", "注册后台巡检 Worker 异常", e)
        }

        // ⚡ 全局注入单例 ImageLoader（确保 Compose AsyncImage 与 context.imageLoader 100% 走定制硬件级缩略图引擎）
        try {
            SingletonImageLoader.setSafe { CoilConfig.createGlobalImageLoader(this) }
        } catch (e: Throwable) {
            android.util.Log.w("PhotoTagApp", "初始化 SingletonImageLoader 异常", e)
        }

        // ⚡ 启动时后台轻量巡检磁盘缓存容量
        Thread {
            try {
                kotlinx.coroutines.runBlocking {
                    com.example.phototag.util.ThumbnailDiskCache.trimCacheIfNeeded(this@PhotoTagApplication)
                }
            } catch (_: Throwable) {}
        }.apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val userPreferences: UserPreferences by lazy { UserPreferences(this) }

    val photoRepository: PhotoRepository by lazy {
        PhotoRepository(applicationContext, database.photoDao(), database.tagDao())
    }

    val tagRepository: TagRepository by lazy {
        TagRepository(database.tagDao())
    }

    val smartAlbumRepository: com.example.phototag.data.repository.SmartAlbumRepository by lazy {
        com.example.phototag.data.repository.SmartAlbumRepository(database.smartAlbumDao(), database.photoDao())
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return CoilConfig.createGlobalImageLoader(context)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                SingletonImageLoader.get(this).memoryCache?.clear()
                com.example.phototag.util.ThumbnailDiskCache.clearMemoryCache()
            } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                val memCache = SingletonImageLoader.get(this).memoryCache
                if (memCache != null) {
                    memCache.trimToSize(memCache.size / 2)
                }
                com.example.phototag.util.ThumbnailDiskCache.trimMemoryCache(0.5)
            }
        } catch (_: Throwable) {}
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            SingletonImageLoader.get(this).memoryCache?.clear()
            com.example.phototag.util.ThumbnailDiskCache.clearMemoryCache()
        } catch (_: Throwable) {}
    }
}
