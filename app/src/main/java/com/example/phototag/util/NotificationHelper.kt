package com.example.phototag.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.phototag.MainActivity
import com.example.phototag.R

/**
 * 🔔 前台服务与常驻动态进度通知助手类。
 */
object NotificationHelper {

    const val CHANNEL_ID_SYNC = "photo_sync_channel"
    const val CHANNEL_NAME_SYNC = "相册后台处理与整理进度"
    const val NOTIFICATION_ID_SYNC = 1001

    /**
     * 初始化创建专用的通知渠道（低打扰度静音常驻）。
     */
    fun createNotificationChannels(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID_SYNC,
                    CHANNEL_NAME_SYNC,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "展示相册后台扫描、EXIF经纬度解析与一键极速整理的实时进度"
                    enableVibration(false)
                    setShowBadge(false)
                    setSound(null, null)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        } catch (_: Exception) {}
    }

    /**
     * 构建前台服务动态进度条通知。
     */
    fun buildProgressNotification(
        context: Context,
        title: String = "正在整理相册...",
        contentText: String = "正在处理中...",
        current: Int = 0,
        total: Int = 0,
        indeterminate: Boolean = false
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = R.drawable.ic_notification

        return NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setProgress(total.coerceAtLeast(1), current, indeterminate)
            .setOngoing(true) // 禁止用户左滑清除，保证后台存活
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }


    /**
     * 构建任务完成通知（可清除）。
     */
    fun buildCompletedNotification(
        context: Context,
        title: String = "相册整理完成",
        contentText: String = "所有照片已分类归档完毕"
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = R.drawable.ic_notification

        return NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setProgress(0, 0, false)
            .setOngoing(false) // 允许滑除
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * 检查是否已加入系统电池优化白名单。
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        }
        return true
    }

    /**
     * 引导用户加入电池优化白名单（防止系统深度杀后台）。
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (!isIgnoringBatteryOptimizations(context)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) {}
            }
        }
    }
}
