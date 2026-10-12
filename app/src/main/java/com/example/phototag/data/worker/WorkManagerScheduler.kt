package com.example.phototag.data.worker

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 智能省电 WorkManager 调度管理器。
 *
 * 核心特性：
 * 1. 【充电 + 息屏空闲 + 电量充足】：重度离线 OCR 与 EXIF 扫描仅在设备接入充电器且息屏空闲时静默执行，杜绝日常耗电与发热。
 * 2. 【防重复调度】：采用 [ExistingWorkPolicy.KEEP] 与 [ExistingPeriodicWorkPolicy.KEEP]，防止任务堆叠。
 * 3. 【周期性维护】：支持每 24 小时定期自动检查增量新照片。
 */
object WorkManagerScheduler {

    const val UNIQUE_IDLE_WORK_NAME = "media_index_idle_charging_work"
    const val UNIQUE_PERIODIC_WORK_NAME = "media_index_periodic_work"

    /**
     * 构建高标准省电约束（仅在充电、息屏空闲且电量充足时执行重度计算）。
     */
    fun createSmartPowerConstraints(): Constraints {
        val builder = Constraints.Builder()
            .setRequiresCharging(true)         // 必须处于充电状态
            .setRequiresBatteryNotLow(true)     // 电池电量不可过低
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setRequiresDeviceIdle(true) // 必须处于息屏空闲状态
        }

        return builder.build()
    }

    /**
     * 调度单次智能空闲充电任务（启动 App 时注册，待满足省电条件后自动静默执行）。
     */
    fun scheduleIdleChargingIndexing(context: Context) {
        try {
            val constraints = createSmartPowerConstraints()

            val workRequest = OneTimeWorkRequestBuilder<MediaIndexWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_IDLE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Throwable) {
            android.util.Log.w("WorkManagerScheduler", "Failed to schedule idle charging indexing", e)
        }
    }

    /**
     * 注册每日定期维护任务（每 24 小时在满足充电与空闲条件时增量索引）。
     */
    fun schedulePeriodicIndexing(context: Context, repeatIntervalHours: Long = 24) {
        try {
            val constraints = createSmartPowerConstraints()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<MediaIndexWorker>(
                repeatIntervalHours,
                TimeUnit.HOURS,
                1, // 弹性执行窗口 1 小时
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        } catch (e: Throwable) {
            android.util.Log.w("WorkManagerScheduler", "Failed to schedule periodic indexing", e)
        }
    }

    /**
     * 手动/即时低功耗触发（不强求充电，但仍强制后台低优先级调度）。
     */
    fun triggerImmediateLowPowerSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<MediaIndexWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "media_index_immediate_work",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Throwable) {
            android.util.Log.w("WorkManagerScheduler", "Failed to trigger immediate low power sync", e)
        }
    }

    /**
     * 取消所有后台调度任务。
     */
    fun cancelAllIndexing(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(UNIQUE_IDLE_WORK_NAME)
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        } catch (e: Throwable) {
            android.util.Log.w("WorkManagerScheduler", "Failed to cancel all indexing", e)
        }
    }
}
