package com.example.phototag.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * 照片/视频媒体实体。
 *
 * 新增：
 * 1. [durationMs]：视频时长（毫秒），支持识别视频并格式化时长角标。
 * 2. [isGif]、[isVideo]、[formattedDuration]：便捷的媒体类型判定与格式化属性。
 */
@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["ocrText"]),
        Index(value = ["isDeleted"]),
        Index(value = ["deletedAt"]),
        Index(value = ["address"])
    ]
)
@androidx.compose.runtime.Immutable
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val photoId: Long = 0,

    /** MediaStore content URI 字符串（唯一索引，防止重复） */
    val uri: String,

    val displayName: String,
    val dateAdded: Long,
    val size: Long = 0,
    val mimeType: String = "",
    val width: Int = 0,
    val height: Int = 0,

    /** Google ML Kit 离线 OCR 提取的文字内容（用于搜图搜字） */
    val ocrText: String? = null,

    /** 视频媒体时长（毫秒），图片类型为 null */
    val durationMs: Long? = null,

    /** 🗑️ 是否已移入回收站（软删除标记） */
    val isDeleted: Boolean = false,

    /** 🗑️ 移入回收站的时间戳（毫秒），用于 7 天倒计时展示与超期自愈清理 */
    val deletedAt: Long? = null,

    /** 🌐 经纬度 (纬度)，从 EXIF 提取并持久化 */
    val latitude: Double? = null,

    /** 🌐 经纬度 (经度)，从 EXIF 提取并持久化 */
    val longitude: Double? = null,

    /** 📍 全自动双通道逆地理编码解析的真实中文地址 (Room 持久化缓存) */
    val address: String? = null,

    /** ⛰️ 真实海拔垂直高度 (米)，从 EXIF 提取并持久化 */
    val altitude: Double? = null,

    /** 📍 用户手动标注/自定义修改的地理位置名称 (如 "首尔圣水洞") */
    val customLocation: String? = null
) {
    /** 是否为 GIF 动图 */
    val isGif: Boolean
        get() = mimeType.equals("image/gif", ignoreCase = true) || 
                displayName.endsWith(".gif", ignoreCase = true) ||
                uri.endsWith(".gif", ignoreCase = true)

    /** 是否为视频媒体 */
    val isVideo: Boolean
        get() = mimeType.startsWith("video/", ignoreCase = true) || 
                (durationMs != null && durationMs > 0) ||
                uri.contains("/video/", ignoreCase = true) ||
                displayName.endsWith(".mp4", ignoreCase = true) ||
                displayName.endsWith(".mov", ignoreCase = true) ||
                displayName.endsWith(".mkv", ignoreCase = true) ||
                displayName.endsWith(".3gp", ignoreCase = true) ||
                displayName.endsWith(".avi", ignoreCase = true) ||
                displayName.endsWith(".webm", ignoreCase = true)

    /** 格式化视频时长（如 01:25、00:15） */
    val formattedDuration: String
        get() {
            val totalSeconds = ((durationMs ?: 0L) / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val remMinutes = minutes % 60
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, remMinutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }

    /** 📸 是否为设备屏幕截图 */
    val isScreenshot: Boolean
        get() = displayName.contains("screenshot", ignoreCase = true) ||
                displayName.contains("截屏", ignoreCase = true) ||
                displayName.contains("截图", ignoreCase = true) ||
                uri.contains("screenshot", ignoreCase = true)

    /** 🎞️ 是否为专业 RAW / DNG 格式照片 */
    val isRaw: Boolean
        get() = mimeType.contains("raw", ignoreCase = true) ||
                mimeType.contains("dng", ignoreCase = true) ||
                displayName.endsWith(".dng", ignoreCase = true) ||
                displayName.endsWith(".cr2", ignoreCase = true) ||
                displayName.endsWith(".nef", ignoreCase = true) ||
                displayName.endsWith(".arw", ignoreCase = true) ||
                displayName.endsWith(".rw2", ignoreCase = true) ||
                displayName.endsWith(".orf", ignoreCase = true)
}
