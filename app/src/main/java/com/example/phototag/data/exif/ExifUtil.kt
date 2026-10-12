package com.example.phototag.data.exif

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExtractedExifData(
    val yearTag: String?,        // 如 "#2026年"
    val monthTag: String?,       // 如 "#8月"
    val orientationTag: String?, // 如 "#横屏壁纸" 或 "#长图截图"
    val typeTag: String?         // 如 "#GIF动图"
)

/**
 * EXIF 与图片元数据自动解析工具类。
 */
object ExifUtil {

    fun extractMetadata(
        context: Context,
        uriString: String,
        mimeType: String,
        dateAddedSec: Long,
        width: Int,
        height: Int
    ): ExtractedExifData {
        var yearTag: String? = null
        var monthTag: String? = null

        // 1. 优先通过 ExifInterface 读取拍摄日期
        try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                if (!dateTimeStr.isNullOrBlank()) {
                    val sdf = SimpleDateFormat("yyyy:MM:dd", Locale.getDefault())
                    val date = sdf.parse(dateTimeStr)
                    if (date != null) {
                        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
                        val month = SimpleDateFormat("M", Locale.getDefault()).format(date)
                        yearTag = "#${year}年"
                        monthTag = "#${month}月"
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 回退方案：使用 MediaStore dateAdded 时间戳
        if (yearTag == null && dateAddedSec > 0) {
            val date = Date(dateAddedSec * 1000L)
            val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
            val month = SimpleDateFormat("M", Locale.getDefault()).format(date)
            yearTag = "#${year}年"
            monthTag = "#${month}月"
        }

        // 2. 宽高比与类型推断
        var orientationTag: String? = null
        if (width > 0 && height > 0) {
            val ratio = width.toFloat() / height.toFloat()
            if (ratio >= 1.35f) {
                orientationTag = "#横屏壁纸"
            } else if (ratio <= 0.52f) {
                orientationTag = "#长图截图"
            }
        }

        var typeTag: String? = null
        if (mimeType.contains("gif", ignoreCase = true)) {
            typeTag = "#GIF动图"
        }

        return ExtractedExifData(
            yearTag = yearTag,
            monthTag = monthTag,
            orientationTag = orientationTag,
            typeTag = typeTag
        )
    }
}
