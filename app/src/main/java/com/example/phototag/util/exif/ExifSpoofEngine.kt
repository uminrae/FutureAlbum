package com.example.phototag.util.exif

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * 🎭 EXIF 深度伪装与元数据伪造核心引擎 (工业级高兼容全平台识别)
 */
object ExifSpoofEngine {

    private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
    private val exifDateStampFormat = SimpleDateFormat("yyyy:MM:dd", Locale.US)
    private val exifTimeStampFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    /**
     * 将十进制经纬度转换为 EXIF 标准度分秒有理数格式 (例如: "37/1,32/1,12345/1000")
     */
    private fun decimalToDmsRational(coordinate: Double): String {
        val absCoord = abs(coordinate)
        val degrees = absCoord.toInt()
        val minutes = ((absCoord - degrees) * 60.0).toInt()
        val seconds = (absCoord - degrees - minutes / 60.0) * 3600.0
        val secNum = (seconds * 10000.0).roundToLong().coerceAtLeast(0L)
        val secDen = 10000L
        return "$degrees/1,$minutes/1,$secNum/$secDen"
    }

    /**
     * 生成带有虚假/伪装 EXIF 信息的临时图片文件（用于直接分享到微信/QQ/系统分享）
     */
    suspend fun createSpoofedTempFile(
        context: Context,
        originalUri: Uri,
        config: SpoofConfig
    ): Uri = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "spoofed_shares")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val targetDate = calculateTargetDate(config)
        val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(targetDate)
        val randomSuffix = (100..999).random()
        val tempFile = File(cacheDir, "IMG_${dateTag}_$randomSuffix.jpg")

        // 统一转码/保存为高质量 JPEG 格式，保证 100% 支持所有 EXIF APP1 GPS 数据块
        writeAsJpegFile(context, originalUri, tempFile)

        // 注入全套工业级标准 GPS 与元数据
        applySpoofToExif(tempFile.absolutePath, config)

        // 返回供系统安全分享的 FileProvider content:// URI
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    /**
     * 将带有虚假/伪装 EXIF 信息的照片副本永久保存至系统相册 (Pictures/FutureAlbum)
     */
    suspend fun saveSpoofedPhotoToMediaStore(
        context: Context,
        originalUri: Uri,
        config: SpoofConfig
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "temp_saves")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // 彻底去除文件名中的地址与 SPOOF 字样，采用真实相机照片标准命名格式 (如 IMG_20260902_233846_821.jpg)
            val targetDate = calculateTargetDate(config)
            val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(targetDate)
            val randomSuffix = (100..999).random()
            val newFileName = "IMG_${dateTag}_$randomSuffix.jpg"
            val tempFile = File(cacheDir, newFileName)

            // 1. 转为标准 JPEG 文件
            writeAsJpegFile(context, originalUri, tempFile)

            // 2. 注入伪装 EXIF 属性
            applySpoofToExif(tempFile.absolutePath, config)

            // 3. 插入系统 MediaStore (明确写入 LATITUDE, LONGITUDE, DATE_TAKEN 字段，确保系统相册与第三方 App 直接读取地理位置！)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newFileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, targetDate.time)
                put(MediaStore.Images.Media.DATE_ADDED, targetDate.time / 1000L)
                put(MediaStore.Images.Media.DATE_MODIFIED, targetDate.time / 1000L)
                put(MediaStore.Images.Media.DESCRIPTION, config.locationName)
                if (config.enableLocation) {
                    put(MediaStore.Images.Media.LATITUDE, config.latitude)
                    put(MediaStore.Images.Media.LONGITUDE, config.longitude)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FutureAlbum")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val mediaStoreUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext null

            // 4. 写入二进制数据
            context.contentResolver.openOutputStream(mediaStoreUri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            // 5. 额外通过 openFileDescriptor("rw") 直接向 MediaStore 注入一次 EXIF，确保物理文件与 MediaStore 索引完全对齐
            try {
                context.contentResolver.openFileDescriptor(mediaStoreUri, "rw")?.use { pfd ->
                    val destExif = ExifInterface(pfd.fileDescriptor)
                    applySpoofToExif(destExif, config)
                    destExif.saveAttributes()
                }
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(mediaStoreUri, contentValues, null, null)
            }

            // 6. 触发 MediaScanner 刷新，确保系统即时建立真实文件的全局索引
            try {
                var realSavedPath: String? = null
                context.contentResolver.query(mediaStoreUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (idx != -1) {
                            realSavedPath = cursor.getString(idx)
                        }
                    }
                }
                if (!realSavedPath.isNullOrBlank()) {
                    try {
                        applySpoofToExif(realSavedPath!!, config)
                    } catch (_: Exception) {}

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(realSavedPath),
                        arrayOf("image/jpeg"),
                        null
                    )
                }
            } catch (_: Exception) {}

            tempFile.delete()
            mediaStoreUri
        } catch (e: Exception) {
            android.util.Log.e("ExifSpoofEngine", "保存伪装副本失败", e)
            null
        }
    }

    /**
     * 将输入 URI 写入为标准高质量 JPEG 文件（若原图为 PNG/WEBP 等则无损转码为 98% 质量 JPEG，确保 EXIF 标签 100% 写入成功）
     */
    private fun writeAsJpegFile(context: Context, uri: Uri, destFile: File) {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val isPngOrWebp = mimeType.contains("png", true) || mimeType.contains("webp", true)

        if (isPngOrWebp) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                if (bitmap != null) {
                    FileOutputStream(destFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
                    }
                    bitmap.recycle()
                    return
                }
            }
        }

        // 直接复制原始文件
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * 底层使用 ExifInterface 注入全套工业级 GPS、机型与时间元数据
     */
    private fun applySpoofToExif(filePath: String, config: SpoofConfig) {
        try {
            val exif = ExifInterface(filePath)
            applySpoofToExif(exif, config)
            exif.saveAttributes()
        } catch (e: Exception) {
            android.util.Log.e("ExifSpoofEngine", "写入伪装 EXIF 失败: $filePath", e)
        }
    }

    private fun applySpoofToExif(exif: ExifInterface, config: SpoofConfig) {
        val targetDate = calculateTargetDate(config)

        // ==================== 1. 注入全套工业级假地理位置 (GPS) ====================
        if (config.enableLocation) {
            // A. 官方标准 API (设置标准 TAG_GPS_LATITUDE, TAG_GPS_LATITUDE_REF, TAG_GPS_LONGITUDE, TAG_GPS_LONGITUDE_REF)
            exif.setLatLong(config.latitude, config.longitude)

            // B. 海拔高度与基准
            val altAbs = abs(config.altitude)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "${(altAbs * 1000).toInt()}/1000")
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (config.altitude >= 0) "0" else "1")

            // C. GPS 时间戳与标准坐标系协议
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exifDateStampFormat.format(targetDate))
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, exifTimeStampFormat.format(targetDate))
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "GPS")
            exif.setAttribute(ExifInterface.TAG_GPS_MAP_DATUM, "WGS-84")
            exif.setAttribute(ExifInterface.TAG_GPS_VERSION_ID, "2.3.0.0")
        }

        // ==================== 2. 注入假拍摄机型 (Make / Model / Software) ====================
        if (config.enableDevice) {
            exif.setAttribute(ExifInterface.TAG_MAKE, config.make)
            exif.setAttribute(ExifInterface.TAG_MODEL, config.model)
            if (config.software.isNotBlank()) {
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, config.software)
            }
        }

        // ==================== 3. 注入假拍摄时间 (DateTimeOriginal / DateTime / DateTimeDigitized) ====================
        if (config.enableTime) {
            val dateStr = exifDateFormat.format(targetDate)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME, dateStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateStr)
        }
    }

    private fun calculateTargetDate(config: SpoofConfig): Date {
        if (config.customDateMs != null && config.customDateMs > 0) {
            return Date(config.customDateMs)
        }
        val cal = Calendar.getInstance()
        when (config.timeMode) {
            SpoofTimeMode.JUST_NOW -> { /* 当前时间不变 */ }
            SpoofTimeMode.YESTERDAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            SpoofTimeMode.THREE_DAYS_AGO -> cal.add(Calendar.DAY_OF_YEAR, -3)
            SpoofTimeMode.ONE_WEEK_AGO -> cal.add(Calendar.DAY_OF_YEAR, -7)
            SpoofTimeMode.ONE_MONTH_AGO -> cal.add(Calendar.MONTH, -1)
            else -> {}
        }
        return cal.time
    }
}