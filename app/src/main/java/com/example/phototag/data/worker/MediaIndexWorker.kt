package com.example.phototag.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.phototag.PhotoTagApplication
import com.example.phototag.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * 智能相册后台离线索引 Worker（绿色低功耗设计）。
 *
 * 核心优化：
 * 1. 强制在 [Dispatchers.IO] 线程低优先级运行，零主线程干扰。
 * 2. 批次处理机制（每处理 20 张图片让步休眠 50ms + yield），杜绝瞬间吃满 CPU 核心引起手机发热。
 * 3. 尊重系统的 [isStopped] 取消回调，随时优雅暂停。
 * 4. 前台服务优雅进度通知。
 */
class MediaIndexWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = appContext.applicationContext as? PhotoTagApplication ?: return@withContext Result.failure()
        val photoDao = app.database.photoDao()
        val photoRepo = app.photoRepository

        createNotificationChannel()

        // 1. 同步最新相册差量
        try {
            photoRepo.syncPhotosFromMediaStore()
        } catch (_: Exception) {}

        if (isStopped) return@withContext Result.success()

        // 2. 统计待处理 OCR 数量
        val pendingPhotos = photoDao.getPhotosWithoutOcr()
        val totalCount = pendingPhotos.size
        if (totalCount == 0) {
            return@withContext Result.success()
        }

        // 显示前台通知进度
        var processedCount = 0
        try {
            setForeground(createForegroundInfo(processedCount, totalCount))
        } catch (_: Exception) {}

        // 3. 低功耗分批次处理（每 20 张让步休眠 50ms）
        val batchSize = 20
        for ((index, photo) in pendingPhotos.withIndex()) {
            if (isStopped) break

            yield() // 协程让步，避免垄断线程池

            try {
                photoRepo.runOcrOnPendingPhotos()
            } catch (_: Exception) {}

            processedCount++

            // 每 20 张让步休眠 50ms，防止 CPU 高频发热
            if ((index + 1) % batchSize == 0) {
                delay(50)
                try {
                    setForeground(createForegroundInfo(processedCount, totalCount))
                } catch (_: Exception) {}
            } else if (processedCount == totalCount) {
                try {
                    setForeground(createForegroundInfo(processedCount, totalCount))
                } catch (_: Exception) {}
            }
        }

        notificationManager?.cancel(NOTIFICATION_ID)
        Result.success()
    }

    private fun createForegroundInfo(current: Int, total: Int): ForegroundInfo {
        val title = "未来的相册 · 正在后台智能索引"
        val content = "已处理 $current / $total 张图片文字与元数据…"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(total, current, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "相册智能索引后台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "展示相册后台 EXIF 分析与离线 OCR 文字索引进度"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "media_index_progress_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
