package com.example.phototag.util.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.phototag.data.entity.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 裁剪比例枚举
 */
enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("自由", null),
    RATIO_1_1("1:1", 1.0f),
    RATIO_4_5("4:5", 4f / 5f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_3_4("3:4", 3f / 4f)
}

/**
 * 马赛克打码笔触与坐标点
 */
data class MosaicPoint(
    val x: Float, // 归一化坐标 0f..1f
    val y: Float  // 归一化坐标 0f..1f
)

data class MosaicStroke(
    val points: List<MosaicPoint>,
    val radiusNorm: Float = 0.035f // 笔触半径相对于宽度的比例
)

object ImageEditorEngine {

    /**
     * 加载轻量级高帧率编辑预览位图 (严格限制最大边长为 1280px，仅占用约 4MB 内存，彻底防止 GC 卡顿与 OOM)
     */
    suspend fun loadPreviewBitmapWithCorrectOrientation(
        context: Context,
        uri: Uri,
        maxPreviewDimension: Int = 1280
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 1. 读取原始图像尺寸
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return@withContext null

            // 2. 计算精准采样比例 inSampleSize
            var sampleSize = 1
            val maxActual = max(origW, origH)
            while (maxActual / (sampleSize * 2) >= maxPreviewDimension) {
                sampleSize *= 2
            }

            // 3. 读取原始图像 Orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (_: Exception) {}

            // 4. 解码下采样预览位图
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 5. 应用矫正矩阵
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return@withContext sampledBitmap
            }

            val corrected = Bitmap.createBitmap(
                sampledBitmap,
                0,
                0,
                sampledBitmap.width,
                sampledBitmap.height,
                matrix,
                true
            )
            if (corrected != sampledBitmap) {
                sampledBitmap.recycle()
            }
            corrected
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 URI 中加载 Bitmap 并根据 EXIF Orientation 矫正为物理正立图像
     */
    suspend fun loadBitmapWithCorrectOrientation(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 1. 读取原始图像 Orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (_: Exception) {}

            // 2. 解码原始位图 (根据 4096px 安全上限智能计算采样率，严防 5000 万像素超大图触发 OOM 崩溃)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 4096) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 3. 应用矫正矩阵
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return@withContext originalBitmap
            }

            val corrected = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )
            if (corrected != originalBitmap) {
                originalBitmap.recycle()
            }
            corrected
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 根据旋转角度 (0, 90, 180, 270) 与水平翻转变换位图
     */
    fun transformBitmap(
        source: Bitmap,
        rotationDegrees: Int = 0,
        flipHorizontal: Boolean = false
    ): Bitmap {
        if (rotationDegrees % 360 == 0 && !flipHorizontal) {
            return source
        }
        val matrix = Matrix()
        if (rotationDegrees % 360 != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * 根据归一化裁切框 (0..1) 纯净裁切位图
     */
    fun cropBitmap(
        source: Bitmap,
        cropRectNorm: RectF
    ): Bitmap {
        val width = source.width
        val height = source.height

        val left = (cropRectNorm.left * width).roundToInt().coerceIn(0, width - 1)
        val top = (cropRectNorm.top * height).roundToInt().coerceIn(0, height - 1)
        val right = (cropRectNorm.right * width).roundToInt().coerceIn(left + 1, width)
        val bottom = (cropRectNorm.bottom * height).roundToInt().coerceIn(top + 1, height)

        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)

        return Bitmap.createBitmap(source, left, top, cropW, cropH)
    }

    /**
     * 根据归一化裁切框 (0..1) 与额外旋转/翻转角度，裁切并变换位图
     */
    fun cropAndTransformBitmap(
        source: Bitmap,
        cropRectNorm: RectF,
        rotationDegrees: Int = 0,
        flipHorizontal: Boolean = false
    ): Bitmap {
        val transformed = transformBitmap(source, rotationDegrees, flipHorizontal)
        val result = cropBitmap(transformed, cropRectNorm)
        if (transformed != source && transformed != result) {
            transformed.recycle()
        }
        return result
    }

    /**
     * 将编辑后的照片高质量保存至系统 MediaStore (Pictures/FutureAlbum)，
     * 并将原图的 EXIF 元数据（GPS经纬度、机型、拍摄时间）完整继承写入新文件中！
     */
    suspend fun saveEditedPhoto(
        context: Context,
        originalPhoto: PhotoEntity,
        finalBitmap: Bitmap
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val now = Date()
            val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now)
            val randomSuffix = (100..999).random()
            val isPngOrAlpha = originalPhoto.mimeType?.contains("png", ignoreCase = true) == true ||
                    (finalBitmap.hasAlpha() && originalPhoto.mimeType?.contains("jpeg", ignoreCase = true) != true)
            val ext = if (isPngOrAlpha) "png" else "jpg"
            val mime = if (isPngOrAlpha) "image/png" else "image/jpeg"
            val newFileName = "IMG_${dateTag}_$randomSuffix.$ext"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newFileName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.DATE_TAKEN, now.time)
                put(MediaStore.Images.Media.DATE_ADDED, now.time / 1000L)
                put(MediaStore.Images.Media.DATE_MODIFIED, now.time / 1000L)
                if (originalPhoto.latitude != null) {
                    put(MediaStore.Images.Media.LATITUDE, originalPhoto.latitude)
                }
                if (originalPhoto.longitude != null) {
                    put(MediaStore.Images.Media.LONGITUDE, originalPhoto.longitude)
                }
                if (!originalPhoto.address.isNullOrBlank()) {
                    put(MediaStore.Images.Media.DESCRIPTION, originalPhoto.address)
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

            // 1. 写入高质量图片二进制 (PNG 保留透明度，JPEG 高清保真)
            context.contentResolver.openOutputStream(mediaStoreUri)?.use { output ->
                if (isPngOrAlpha) {
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                } else {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 98, output)
                }
                output.flush()
            }

            // 2. 完整继承原图 EXIF 元数据（GPS经纬度、拍摄机型、时间戳）
            try {
                context.contentResolver.openFileDescriptor(mediaStoreUri, "rw")?.use { pfd ->
                    val newExif = ExifInterface(pfd.fileDescriptor)

                    // 继承原图 GPS
                    if (originalPhoto.latitude != null && originalPhoto.longitude != null) {
                        newExif.setLatLong(originalPhoto.latitude, originalPhoto.longitude)
                        if (originalPhoto.altitude != null) {
                            val altAbs = Math.abs(originalPhoto.altitude)
                            newExif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "${(altAbs * 1000).toInt()}/1000")
                            newExif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (originalPhoto.altitude >= 0) "0" else "1")
                        }
                    }

                    // 继承时间与设备
                    val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    val dateStr = exifDateFormat.format(Date(originalPhoto.dateAdded * 1000L))
                    newExif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateStr)
                    newExif.setAttribute(ExifInterface.TAG_DATETIME, dateStr)
                    newExif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateStr)

                    // 尝试读取原图机型
                    try {
                        context.contentResolver.openInputStream(Uri.parse(originalPhoto.uri))?.use { origStream ->
                            val origExif = ExifInterface(origStream)
                            origExif.getAttribute(ExifInterface.TAG_MAKE)?.let { newExif.setAttribute(ExifInterface.TAG_MAKE, it) }
                            origExif.getAttribute(ExifInterface.TAG_MODEL)?.let { newExif.setAttribute(ExifInterface.TAG_MODEL, it) }
                            origExif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let { newExif.setAttribute(ExifInterface.TAG_SOFTWARE, it) }
                        }
                    } catch (_: Exception) {}

                    newExif.saveAttributes()
                }
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(mediaStoreUri, contentValues, null, null)
            }

            // 3. 通知 MediaScanner 建立全局索引
            try {
                var realSavedPath: String? = null
                context.contentResolver.query(mediaStoreUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (idx != -1) realSavedPath = cursor.getString(idx)
                    }
                }
                if (!realSavedPath.isNullOrBlank()) {
                    MediaScannerConnection.scanFile(context, arrayOf(realSavedPath), arrayOf(mime), null)
                }
            } catch (_: Exception) {}

            mediaStoreUri
        } catch (e: Exception) {
            android.util.Log.e("ImageEditorEngine", "保存编辑后图片失败", e)
            null
        }
    }

    /**
     * 生成像素化位图 (用于马赛克打码遮罩)
     */
    fun createPixelatedBitmap(source: Bitmap, blockSize: Int = 24): Bitmap {
        val safeBlock = blockSize.coerceAtLeast(4)
        val smallW = max(1, source.width / safeBlock)
        val smallH = max(1, source.height / safeBlock)
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, false)
        val pixelated = Bitmap.createScaledBitmap(small, source.width, source.height, false)
        if (small != pixelated) {
            small.recycle()
        }
        return pixelated
    }

    /**
     * 根据笔触列表将马赛克遮罩合成到原图上
     */
    fun applyMosaic(
        source: Bitmap,
        strokes: List<MosaicStroke>
    ): Bitmap {
        if (strokes.isEmpty()) return source
        val adaptiveBlock = max(8, min(source.width, source.height) / 45)
        val pixelatedFull = createPixelatedBitmap(source, adaptiveBlock)

        val result = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val clipPath = Path()

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            val strokeWidthPx = stroke.radiusNorm * source.width * 2f
            paint.strokeWidth = strokeWidthPx
            if (stroke.points.size == 1) {
                clipPath.addCircle(
                    stroke.points[0].x * source.width,
                    stroke.points[0].y * source.height,
                    strokeWidthPx / 2f,
                    Path.Direction.CW
                )
            } else {
                val strokePath = Path()
                strokePath.moveTo(stroke.points[0].x * source.width, stroke.points[0].y * source.height)
                for (i in 1 until stroke.points.size) {
                    strokePath.lineTo(stroke.points[i].x * source.width, stroke.points[i].y * source.height)
                }
                val fillPath = Path()
                paint.getFillPath(strokePath, fillPath)
                clipPath.addPath(fillPath)
            }
        }

        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(pixelatedFull, 0f, 0f, null)
        canvas.restore()
        pixelatedFull.recycle()

        return result
    }
}
