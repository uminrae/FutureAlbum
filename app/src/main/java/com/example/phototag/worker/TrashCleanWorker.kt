package com.example.phototag.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.phototag.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 🗑️ 7天安全回收站超期自动清理 Worker。
 *
 * 机制：
 * 1. 每日定时在后台执行轻量巡检。
 * 2. 检索 isDeleted = 1 且 (当前时间 - deletedAt) >= 7 天（7 * 24 * 3600 * 1000L）的照片。
 * 3. 自动对超期照片执行硬删除并清理脏关联索引。
 */
class TrashCleanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tag = "TrashCleanWorker"
        try {
            val database = AppDatabase.getInstance(applicationContext)
            val photoDao = database.photoDao()

            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - SEVEN_DAYS_MS

            val expiredPhotos = photoDao.getExpiredTrashPhotos(sevenDaysAgo)
            if (expiredPhotos.isNotEmpty()) {
                val expiredUris = expiredPhotos.map { it.uri }
                try {
                    com.example.phototag.util.MediaStoreDeleteHelper.prepareDeleteRequest(
                        applicationContext,
                        expiredUris.map { android.net.Uri.parse(it) }
                    )
                } catch (_: Exception) {}
                val deletedCount = photoDao.hardDeletePhotosByUris(expiredUris)
                Log.d(tag, "成功自动清理 ${expiredPhotos.size} 张超期（超过7天）的回收站照片，物理清理 $deletedCount 条数据库记录")
            } else {
                Log.d(tag, "回收站健康巡检完成，暂无超过 7 天的超期照片")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "回收站自动清理任务执行失败", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "TrashCleanPeriodicWorker"
        const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L

        /**
         * 注册每日定时自动清理任务。
         */
        fun enqueuePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanRequest = PeriodicWorkRequestBuilder<TrashCleanWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            try {
                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    cleanRequest
                )
            } catch (e: Exception) {
                Log.w("TrashCleanWorker", "无法注册回收站定时任务", e)
            }
        }
    }
}
