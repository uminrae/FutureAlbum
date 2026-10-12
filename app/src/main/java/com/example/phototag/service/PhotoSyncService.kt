package com.example.phototag.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.phototag.PhotoTagApplication
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.PhotoTagCrossRef
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AutoOrganizeProgressState(
    val isOrganizing: Boolean = false,
    val isPaused: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val progress: Float = 0f
)

/**
 * ⚡ 相册后台处理与极速整理前台保活服务 (Foreground Service)。
 */
class PhotoSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null

    @Volatile
    private var isPaused = false

    @Volatile
    private var isCancelled = false

    private var currentProgress = 0
    private var totalProgress = 0

    private val app: PhotoTagApplication by lazy {
        application as PhotoTagApplication
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        startServiceForeground("相册后台服务已就绪", "正在准备数据处理流水线...")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_SYNC_EXIF_METADATA

        when (action) {
            ACTION_START_AUTO_ORGANIZE -> {
                val tagByTime = intent?.getBooleanExtra(EXTRA_TAG_BY_TIME, true) ?: true
                val tagBySource = intent?.getBooleanExtra(EXTRA_TAG_BY_SOURCE, true) ?: true
                val tagByCity = intent?.getBooleanExtra(EXTRA_TAG_BY_CITY, true) ?: true
                val tagByPerson = intent?.getBooleanExtra(EXTRA_TAG_BY_PERSON, false) ?: false
                handleAutoOrganize(tagByTime, tagBySource, tagByCity, tagByPerson)
            }
            ACTION_PAUSE_AUTO_ORGANIZE -> {
                isPaused = true
                val notification = NotificationHelper.buildProgressNotification(
                    context = this,
                    title = "自动归档已暂停",
                    contentText = "已暂停（已完成: $currentProgress / $totalProgress 张）",
                    current = currentProgress,
                    total = totalProgress,
                    indeterminate = false
                )
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID_SYNC, notification)
            }
            ACTION_RESUME_AUTO_ORGANIZE -> {
                isPaused = false
                val notification = NotificationHelper.buildProgressNotification(
                    context = this,
                    title = "正在极速整理相册...",
                    contentText = "正在继续处理: $currentProgress / $totalProgress 张",
                    current = currentProgress,
                    total = totalProgress,
                    indeterminate = false
                )
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID_SYNC, notification)
            }
            ACTION_CANCEL_AUTO_ORGANIZE -> {
                isCancelled = true
                syncJob?.cancel()
                _autoOrganizeProgress.value = AutoOrganizeProgressState()
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
            ACTION_SYNC_EXIF_METADATA -> {
                syncJob?.cancel()
                syncJob = serviceScope.launch {
                    try {
                        startServiceForeground("正在同步照片地理与Exif信息...", "正在解析中...")
                        app.photoRepository.syncMissingGpsMetadata()
                    } finally {
                        stopForeground(STOP_FOREGROUND_DETACH)
                        stopSelf()
                    }
                }
            }
            ACTION_STOP_SERVICE -> {
                syncJob?.cancel()
                _autoOrganizeProgress.value = AutoOrganizeProgressState()
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startServiceForeground(title: String, contentText: String) {
        val notification = NotificationHelper.buildProgressNotification(
            context = this,
            title = title,
            contentText = contentText,
            indeterminate = true
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.NOTIFICATION_ID_SYNC,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_SYNC, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(NotificationHelper.NOTIFICATION_ID_SYNC, notification)
            } catch (_: Exception) {}
        }
    }

    /**
     * 全自动整理相册（时间、地点标签）
     */
    private fun handleAutoOrganize(
        tagByTime: Boolean,
        tagBySource: Boolean,
        tagByCity: Boolean,
        tagByPerson: Boolean
    ) {
        syncJob?.cancel()
        isPaused = false
        isCancelled = false

        startServiceForeground("正在极速整理相册...", "正在准备分类流水线...")

        syncJob = serviceScope.launch {
            _autoOrganizeProgress.value = AutoOrganizeProgressState(isOrganizing = true, isPaused = false)

            try {
                val photoDao = app.database.photoDao()
                val tagDao = app.database.tagDao()
                val activePhotos = photoDao.getAllPhotosList()
                totalProgress = activePhotos.size
                currentProgress = 0

                val timeCategory = tagDao.getCategoryByName("时间")
                    ?: CategoryEntity(name = "时间").let {
                        val id = tagDao.insertCategory(it)
                        it.copy(categoryId = id)
                    }

                val cityCategory = tagDao.getCategoryByName("地点")
                    ?: CategoryEntity(name = "地点").let {
                        val id = tagDao.insertCategory(it)
                        it.copy(categoryId = id)
                    }

                val tagCache = mutableMapOf<String, TagEntity>()
                suspend fun getOrCreateTag(name: String, catId: Long): TagEntity {
                    val cached = tagCache[name]
                    if (cached != null) return cached
                    val found = tagDao.getTagByName(name)
                    if (found != null) {
                        tagCache[name] = found
                        return found
                    }
                    val newTag = TagEntity(name = name, categoryId = catId)
                    val id = tagDao.insertTag(newTag)
                    val saved = newTag.copy(tagId = id)
                    tagCache[name] = saved
                    return saved
                }

                val crossRefsToInsert = mutableListOf<PhotoTagCrossRef>()
                val timeFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())

                for (photo in activePhotos) {
                    if (isCancelled) break
                    while (isPaused) {
                        if (isCancelled) break
                        delay(300)
                    }
                    if (isCancelled) break

                    if (tagByTime) {
                        val date = Date(photo.dateAdded * 1000)
                        val timeTagName = timeFormat.format(date)
                        val tag = getOrCreateTag(timeTagName, timeCategory.categoryId)
                        crossRefsToInsert.add(PhotoTagCrossRef(photoId = photo.photoId, tagId = tag.tagId))
                    }

                    if (tagByCity && !photo.address.isNullOrBlank()) {
                        val cityTag = getOrCreateTag(photo.address, cityCategory.categoryId)
                        crossRefsToInsert.add(PhotoTagCrossRef(photoId = photo.photoId, tagId = cityTag.tagId))
                    }

                    currentProgress++

                    if (currentProgress % 20 == 0 || currentProgress == totalProgress) {
                        if (crossRefsToInsert.isNotEmpty()) {
                            photoDao.insertPhotoTagCrossRefs(crossRefsToInsert.toList())
                            crossRefsToInsert.clear()
                        }
                        val prog = if (totalProgress > 0) currentProgress.toFloat() / totalProgress else 0f
                        _autoOrganizeProgress.value = AutoOrganizeProgressState(
                            isOrganizing = true,
                            isPaused = isPaused,
                            current = currentProgress,
                            total = totalProgress,
                            progress = prog
                        )

                        val notif = NotificationHelper.buildProgressNotification(
                            context = this@PhotoSyncService,
                            title = if (isPaused) "自动归档已暂停" else "正在极速整理相册...",
                            contentText = "已整理: $currentProgress / $totalProgress 张",
                            current = currentProgress,
                            total = totalProgress,
                            indeterminate = false
                        )
                        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_SYNC, notif)
                    }
                }

                if (crossRefsToInsert.isNotEmpty()) {
                    photoDao.insertPhotoTagCrossRefs(crossRefsToInsert)
                }

            } catch (_: Exception) {
            } finally {
                _autoOrganizeProgress.value = AutoOrganizeProgressState()
                val finishNotif = NotificationHelper.buildCompletedNotification(
                    context = this@PhotoSyncService,
                    title = "相册整理完成",
                    contentText = "全部照片分类标签已同步完成！"
                )
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID_SYNC, finishNotif)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START_AUTO_ORGANIZE = "com.example.phototag.action.START_AUTO_ORGANIZE"
        const val ACTION_PAUSE_AUTO_ORGANIZE = "com.example.phototag.action.PAUSE_AUTO_ORGANIZE"
        const val ACTION_RESUME_AUTO_ORGANIZE = "com.example.phototag.action.RESUME_AUTO_ORGANIZE"
        const val ACTION_CANCEL_AUTO_ORGANIZE = "com.example.phototag.action.CANCEL_AUTO_ORGANIZE"
        const val ACTION_SYNC_EXIF_METADATA = "com.example.phototag.action.SYNC_EXIF_METADATA"
        const val ACTION_STOP_SERVICE = "com.example.phototag.action.STOP_SERVICE"

        const val EXTRA_TAG_BY_TIME = "extra_tag_by_time"
        const val EXTRA_TAG_BY_SOURCE = "extra_tag_by_source"
        const val EXTRA_TAG_BY_CITY = "extra_tag_by_city"
        const val EXTRA_TAG_BY_PERSON = "extra_tag_by_person"

        private val _autoOrganizeProgress = MutableStateFlow(AutoOrganizeProgressState())
        val autoOrganizeProgress: StateFlow<AutoOrganizeProgressState> = _autoOrganizeProgress.asStateFlow()

        fun startAutoOrganize(
            context: Context,
            tagByTime: Boolean = true,
            tagBySource: Boolean = true,
            tagByCity: Boolean = true,
            tagByPerson: Boolean = true
        ) {
            val intent = Intent(context, PhotoSyncService::class.java).apply {
                action = ACTION_START_AUTO_ORGANIZE
                putExtra(EXTRA_TAG_BY_TIME, tagByTime)
                putExtra(EXTRA_TAG_BY_SOURCE, tagBySource)
                putExtra(EXTRA_TAG_BY_CITY, tagByCity)
                putExtra(EXTRA_TAG_BY_PERSON, tagByPerson)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                try { context.startService(intent) } catch (_: Exception) {}
            }
        }

        fun pauseAutoOrganize(context: Context) {
            val intent = Intent(context, PhotoSyncService::class.java).apply {
                action = ACTION_PAUSE_AUTO_ORGANIZE
            }
            try { context.startService(intent) } catch (_: Exception) {}
        }

        fun resumeAutoOrganize(context: Context) {
            val intent = Intent(context, PhotoSyncService::class.java).apply {
                action = ACTION_RESUME_AUTO_ORGANIZE
            }
            try { context.startService(intent) } catch (_: Exception) {}
        }

        fun cancelAutoOrganize(context: Context) {
            val intent = Intent(context, PhotoSyncService::class.java).apply {
                action = ACTION_CANCEL_AUTO_ORGANIZE
            }
            try { context.startService(intent) } catch (_: Exception) {}
        }

        fun resume(context: Context) = resumeAutoOrganize(context)
        fun pause(context: Context) = pauseAutoOrganize(context)
        fun cancel(context: Context) = cancelAutoOrganize(context)
    }
}
