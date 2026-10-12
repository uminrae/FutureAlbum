package com.example.phototag.util.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.phototag.PhotoTagApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * 🏷️ 模板水印种类枚举（涵盖经典地标/影调框/相框与 DAZZ 官方 6 款机背时间戳）
 */
enum class WatermarkType(val label: String, val subtitle: String) {
    NONE("无水印", "不添加水印"),

    // 🎞️ DAZZ 官方机背时间戳系列（严格 1:1 对齐移植包 timestamp/ 规范）
    DAZZ_VCR_TILTED("Dazz 招牌倾斜橙", "VCR 点阵 · +3.9° 经典倾角 · 暖橙机背 (原版默认)"),
    DAZZ_VCR_ORANGE("VCR 录像机橙", "VCR 点阵 · 水平正放 · 经典复古橙"),
    DAZZ_LCD_RED("LCD 液晶红点阵", "LCD 点阵 · 数码机背 · 鲜艳亮红"),
    DAZZ_LCD_GREEN("LCD 液晶绿点阵", "LCD 点阵 · 一次性相机 · 经典荧光绿"),
    DAZZ_VHS("VHS 录像机荧光", "VHS 专用字体 · 荧光黄绿 · 怀旧录像带"),
    DAZZ_DEFAULT_YELLOW("经典胶片金黄", "石英钟机背 · 暖金色调 · 经典通用"),

    // 🎞️ 经典胶片石英机背时间戳
    DAZZ_CPM35_DATE("CPM35 石英点阵", "35mm胶片机背 · 暖金点阵 · 日系年月日"),

    // 🎨 经典模板水印系列（相框与胶囊）
    INSTA_GEOTAG_PILL("Instagram地标", "精致红色地标点 · 磨砂黑胶囊 · 潮流打卡"),
    LEICA_CAMERA_FRAME("经典影调框", "德味经典影调底栏 · 相机机型 · 曝光参数"),
    POLAROID_FRAME("拍立得经典相框", "经典显影白边相纸 · 宽边复古留白"),
    FILM_METADATA_STRIP("胶片参数条", "柯达/富士胶卷边缘暗码 · 电影底片参数")
}

/**
 * 水印锚点位置 (原版 Dazz 统一在右下角，支持手动切换至居中或左侧)
 */
enum class WatermarkPosition(val label: String) {
    BOTTOM_RIGHT("右下角 (原版推荐)"),
    BOTTOM_LEFT("左下角"),
    BOTTOM_CENTER("居中下"),
    LEFT_VERTICAL("左侧纵排")
}

/**
 * 绘制水印所需的上下文元数据
 */
data class WatermarkMetadata(
    val dateTaken: Date = Date(),
    val locationName: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val customText: String? = null,
    val pattern: String = "''yy MM dd",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT
)

/**
 * 内部时间戳定义规范（严格源自 DAZZ_移植包/timestamp/时间戳实现指南.md 实测参数）
 */
internal enum class DazzStyleSpec(
    val fontAsset: String,
    val textColor: Int,
    val bgColor: Int,
    val tiltDeg: Float,
    val defaultPattern: String
) {
    VCR_TILTED   ("VCR_OSD_MONO.ttf", 0xFFFF8C1E.toInt(), 0, 3.9f, "''yy MM dd"),
    VCR_ORANGE   ("VCR_OSD_MONO.ttf", 0xFFFF9128.toInt(), 0, 0f,   "''yy MM dd"),
    LCD_RED      ("lcddot_tr.ttf",    0xFFFF5A1E.toInt(), 0, 0f,   "''yy MM dd"),
    LCD_GREEN    ("lcddot_tr.ttf",    0xFF78FF50.toInt(), 0, 0f,   "''yy MM dd"),
    VHS          ("VHS.ttf",          0xFFC8FF3C.toInt(), 0, 0f,   "yyyy.MM.dd HH:mm"),
    DIGITAL      ("timestamp.ttf",    0xFFFFEB50.toInt(), 0, 0f,   "''yy MM dd")
}

/**
 * 🎞️ 模板水印与复古时间戳渲染引擎 (100% 零崩溃防护与所见即所得)
 */
object WatermarkRenderer {

    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun loadTypeface(context: Context?, fontAsset: String): Typeface {
        if (fontAsset.isBlank()) return Typeface.MONOSPACE
        typefaceCache[fontAsset]?.let { return it }

        val assets = context?.assets ?: try {
            PhotoTagApplication.instance.assets
        } catch (_: Throwable) {
            null
        } ?: return Typeface.MONOSPACE

        val candidatePaths = listOf(
            "timestamp/fonts/$fontAsset",
            "dazz/timestamp/fonts/$fontAsset",
            "dazz/fonts/$fontAsset",
            "timestamp/$fontAsset",
            fontAsset
        )

        for (path in candidatePaths) {
            try {
                val tf = Typeface.createFromAsset(assets, path)
                if (tf != null) {
                    typefaceCache[fontAsset] = tf
                    return tf
                }
            } catch (_: Throwable) {}
        }

        return Typeface.MONOSPACE
    }

    /**
     * 将指定模板的水印绘制在目标 Bitmap 上，返回渲染后的全新 Bitmap (零崩溃保证)
     */
    fun applyWatermark(
        source: Bitmap,
        type: WatermarkType,
        metadata: WatermarkMetadata,
        context: Context? = null
    ): Bitmap {
        if (type == WatermarkType.NONE) return source

        return try {
            val width = source.width
            val height = source.height
            if (width <= 0 || height <= 0) return source

            val resultBitmap = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
            val canvas = Canvas(resultBitmap)

            drawWatermarkOverlay(canvas, width, height, type, metadata, context)
            resultBitmap
        } catch (t: Throwable) {
            android.util.Log.e("WatermarkRenderer", "applyWatermark safely caught exception", t)
            source
        }
    }

    /**
     * 将水印直接绘制在已有 Canvas 上（用于 Compose 实时所见即所得极速预览与原图导出）
     */
    fun drawWatermarkOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        type: WatermarkType,
        metadata: WatermarkMetadata,
        context: Context? = null
    ) {
        if (type == WatermarkType.NONE || width <= 0 || height <= 0) return

        try {
            when (type) {
                WatermarkType.DAZZ_CPM35_DATE -> {
                    RetroDotMatrixRenderer.drawCpm35DotMatrixDate(
                        canvas = canvas,
                        imgWidth = width,
                        imgHeight = height,
                        date = metadata.dateTaken,
                        position = metadata.position,
                        pattern = metadata.pattern
                    )
                }
                WatermarkType.INSTA_GEOTAG_PILL -> {
                    drawInstaGeotagPill(canvas, width, height, metadata)
                }
                WatermarkType.LEICA_CAMERA_FRAME -> {
                    drawLeicaCameraFrame(canvas, width, height, metadata)
                }
                WatermarkType.POLAROID_FRAME -> {
                    drawPolaroidFrame(canvas, width, height, metadata)
                }
                WatermarkType.FILM_METADATA_STRIP -> {
                    drawFilmMetadataStrip(canvas, width, height, metadata, context)
                }
                WatermarkType.DAZZ_VCR_TILTED -> {
                    renderDazzTimestamp(canvas, width, height, DazzStyleSpec.VCR_TILTED, metadata, context)
                }
                WatermarkType.DAZZ_VCR_ORANGE -> {
                    renderDazzTimestamp(canvas, width, height, DazzStyleSpec.VCR_ORANGE, metadata, context)
                }
                WatermarkType.DAZZ_LCD_RED -> {
                    renderDazzTimestamp(canvas, width, height, DazzStyleSpec.LCD_RED, metadata, context)
                }
                WatermarkType.DAZZ_LCD_GREEN -> {
                    renderDazzTimestamp(canvas, width, height, DazzStyleSpec.LCD_GREEN, metadata, context)
                }
                WatermarkType.DAZZ_VHS -> {
                    renderDazzTimestamp(canvas, width, height, DazzStyleSpec.VHS, metadata, context)
                }
                WatermarkType.DAZZ_DEFAULT_YELLOW -> {
                    renderDazzTimestamp(canvas, width, height, DazzStyleSpec.DIGITAL, metadata, context)
                }
                WatermarkType.NONE -> {}
            }
        } catch (t: Throwable) {
            android.util.Log.e("WatermarkRenderer", "drawWatermarkOverlay safely caught exception", t)
        }
    }

    /**
     * 1. 📍 Instagram 风格潮流地标定位胶囊 (绝不闪退，支持任意地点/空值优雅降级)
     */
    private fun drawInstaGeotagPill(
        canvas: Canvas,
        width: Int,
        height: Int,
        metadata: WatermarkMetadata
    ) {
        val saveCount = canvas.save()
        try {
            val shortSide = minOf(width, height).toFloat()
            if (shortSide <= 0f) return

            // 文本内容：优先展示照片高精地标 POI，若无则展示自定义文本或默认潮流地标
            val dateStr = try {
                SimpleDateFormat("yyyy.MM.dd", Locale.US).format(metadata.dateTaken)
            } catch (_: Throwable) { "2026.10.06" }

            val rawLocation = metadata.locationName?.takeIf { it.isNotBlank() }
                ?: metadata.customText?.takeIf { it.isNotBlank() }
                ?: "$dateStr · Photo Spot"

            // 过滤换行符并限制最大长度，防止溢出屏幕
            val displayText = rawLocation.replace("\n", " ").trim().take(28)

            // 动态字号 (占短边约 3.2%)
            val fontSize = (shortSide * 0.032f).coerceIn(11f, 48f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = fontSize
                isSubpixelText = true
            }

            val dotRadius = fontSize * 0.28f
            val dotSpacing = fontSize * 0.40f
            val textWidth = textPaint.measureText(displayText)
            val fm = textPaint.fontMetrics
            val textHeight = fm.descent - fm.ascent

            val padH = fontSize * 0.75f
            val padV = fontSize * 0.45f
            val pillWidth = textWidth + dotRadius * 2f + dotSpacing + padH * 2f
            val pillHeight = textHeight + padV * 2f
            val margin = shortSide * 0.035f

            // 计算胶囊中心坐标
            val (cx, cy) = calculatePositionCenter(metadata.position, width, height, pillWidth, pillHeight, margin)

            val left = cx - pillWidth / 2f
            val top = cy - pillHeight / 2f
            val right = cx + pillWidth / 2f
            val bottom = cy + pillHeight / 2f

            // A. 绘制磨砂质感深黑半透明胶囊背景
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CC121212")
                style = Paint.Style.FILL
                try {
                    setShadowLayer(fontSize * 0.35f, 0f, fontSize * 0.12f, Color.parseColor("#55000000"))
                } catch (_: Throwable) {}
            }
            val radius = pillHeight / 2f
            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, radius, radius, pillPaint)

            // B. 绘制极细半透明高光边框 (增加现代 UI 精致感)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#28FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = (fontSize * 0.06f).coerceAtLeast(1f)
            }
            canvas.drawRoundRect(rect, radius, radius, strokePaint)

            // C. 绘制精致红色地标 Pin 针原点
            val dotCenterX = left + padH + dotRadius
            val dotCenterY = cy
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF3B30")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotPaint)

            // 绘制地标红色圆点中心的高光小白心
            val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(dotCenterX, dotCenterY, dotRadius * 0.4f, innerDotPaint)

            // D. 绘制白色地标文字（垂直居中对齐）
            val textStartX = dotCenterX + dotRadius + dotSpacing
            val baseline = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(displayText, textStartX, baseline, textPaint)
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * 2. 🔴 经典影调框 (解决没反应没效果：在画面底部内嵌极具德味的高级影调参数底栏)
     */
    private fun drawLeicaCameraFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        metadata: WatermarkMetadata
    ) {
        val saveCount = canvas.save()
        try {
            val shortSide = minOf(width, height).toFloat()
            if (shortSide <= 0f) return

            // 影调栏高度占短边的约 10%
            val barHeight = (shortSide * 0.10f).coerceIn(38f, 150f)
            val barTop = height.toFloat() - barHeight

            // 绘制深黑微光半透明影调底栏
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E60D0D0D")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, barTop, width.toFloat(), height.toFloat(), barPaint)

            // 绘制顶部精致微光分割线
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#33FFFFFF")
                strokeWidth = (barHeight * 0.02f).coerceAtLeast(1f)
            }
            canvas.drawLine(0f, barTop, width.toFloat(), barTop, linePaint)

            val centerY = barTop + barHeight / 2f
            val margin = (width * 0.045f).coerceIn(16f, 60f)

            // A. 左侧：经典徕卡红色小圆标 + 相机机型
            val redDotRadius = barHeight * 0.20f
            val redDotCenterX = margin + redDotRadius
            val redDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E50014")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(redDotCenterX, centerY, redDotRadius, redDotPaint)

            // 小红标内部优雅小白点
            val whiteDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(redDotCenterX, centerY, redDotRadius * 0.35f, whiteDotPaint)

            // 相机名称
            val cameraName = metadata.cameraModel?.takeIf { it.isNotBlank() }
                ?: metadata.cameraMake?.takeIf { it.isNotBlank() }
                ?: "LEICA M11-P"

            val titleFontSize = (barHeight * 0.28f).coerceIn(11f, 36f)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = titleFontSize
                isSubpixelText = true
            }
            val titleFm = titlePaint.fontMetrics
            val titleBaseline = centerY - (titleFm.ascent + titleFm.descent) / 2f
            canvas.drawText(cameraName, redDotCenterX + redDotRadius * 1.5f, titleBaseline, titlePaint)

            // B. 右侧：摄影参数与拍摄日期
            val dateStr = try {
                SimpleDateFormat("yyyy.MM.dd", Locale.US).format(metadata.dateTaken)
            } catch (_: Throwable) { "2026.10.06" }

            val paramText = "35mm f/1.4 1/250s ISO 100 · $dateStr"
            val paramFontSize = (barHeight * 0.22f).coerceIn(9.5f, 28f)
            val paramPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0B0B5")
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                textSize = paramFontSize
                isSubpixelText = true
            }
            val paramWidth = paramPaint.measureText(paramText)
            val paramFm = paramPaint.fontMetrics
            val paramBaseline = centerY - (paramFm.ascent + paramFm.descent) / 2f

            // 仅在宽度足够时绘制完整参数，否则自动紧凑化
            if (width - margin - paramWidth > redDotCenterX + redDotRadius * 1.5f + titlePaint.measureText(cameraName) + 20f) {
                canvas.drawText(paramText, width - margin - paramWidth, paramBaseline, paramPaint)
            } else {
                val shortParam = dateStr
                val shortW = paramPaint.measureText(shortParam)
                canvas.drawText(shortParam, width - margin - shortW, paramBaseline, paramPaint)
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * 3. 🖼️ 拍立得复古经典相框 (经典显影象牙白相纸 + 底部留白手写体日期)
     */
    private fun drawPolaroidFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        metadata: WatermarkMetadata
    ) {
        val saveCount = canvas.save()
        try {
            val shortSide = minOf(width, height).toFloat()
            val borderWidth = shortSide * 0.045f
            val bottomBorderWidth = height * 0.12f

            val frameColor = Color.parseColor("#FDFBF7")

            // 绘制四周显影相纸白框
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = frameColor
                style = Paint.Style.STROKE
                strokeWidth = borderWidth * 2f
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), strokePaint)

            // 绘制底部宽相纸留白
            val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = frameColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, height - bottomBorderWidth, width.toFloat(), height.toFloat(), bottomPaint)

            // 底部居中手写感复古日期与地点
            val dateStr = try {
                SimpleDateFormat("yyyy.MM.dd", Locale.US).format(metadata.dateTaken)
            } catch (_: Throwable) { "2026.10.06" }

            val locStr = metadata.locationName?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
            val caption = "'$dateStr$locStr"

            val fontSize = (bottomBorderWidth * 0.22f).coerceIn(11f, 36f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5A5A5C")
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textSize = fontSize
                isSubpixelText = true
            }
            val textW = textPaint.measureText(caption)
            val fm = textPaint.fontMetrics
            val baseline = (height - bottomBorderWidth / 2f) - (fm.ascent + fm.descent) / 2f
            canvas.drawText(caption, (width - textW) / 2f, baseline, textPaint)
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * 4. 🎞️ 胶片参数条 (柯达/富士电影底片暗房齿孔暗码风格)
     */
    private fun drawFilmMetadataStrip(
        canvas: Canvas,
        width: Int,
        height: Int,
        metadata: WatermarkMetadata,
        context: Context? = null
    ) {
        val saveCount = canvas.save()
        try {
            val shortSide = minOf(width, height).toFloat()
            if (shortSide <= 0f) return

            val dateStr = try {
                SimpleDateFormat("yy MM dd", Locale.US).format(metadata.dateTaken)
            } catch (_: Throwable) { "98 05 20" }

            val stripText = "KODAK PORTRA 400 · ▶ 24A ◀ · '$dateStr"
            val fontSize = (shortSide * 0.030f).coerceIn(10f, 40f)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFFFB340") // 胶卷暗码复古金黄色
                typeface = loadTypeface(context, "VCR_OSD_MONO.ttf")
                textSize = fontSize
                isSubpixelText = true
                try {
                    setShadowLayer(fontSize * 0.1f, 0f, fontSize * 0.05f, Color.parseColor("#CC000000"))
                } catch (_: Throwable) {}
            }

            val tw = textPaint.measureText(stripText)
            val fm = textPaint.fontMetrics
            val th = fm.descent - fm.ascent

            val padH = fontSize * 0.5f
            val padV = fontSize * 0.2f
            val boxW = tw + padH * 2f
            val boxH = th + padV * 2f
            val margin = shortSide * 0.035f

            val (cx, cy) = calculatePositionCenter(metadata.position, width, height, boxW, boxH, margin)

            // 绘制半透明胶片深色底条
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#99101010")
                style = Paint.Style.FILL
            }
            val rect = RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)
            val cornerRadius = fontSize * 0.15f
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

            // 绘制文字
            val baseline = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(stripText, cx - tw / 2f, baseline, textPaint)
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * 5. 🏷️ DAZZ 官方机背复古时间戳 (严格 1:1 还原移植包实测参数)
     */
    private fun renderDazzTimestamp(
        canvas: Canvas,
        width: Int,
        height: Int,
        spec: DazzStyleSpec,
        metadata: WatermarkMetadata,
        context: Context? = null
    ) {
        val shortSide = minOf(width, height).toFloat()
        if (shortSide <= 0f) return

        // 1. 字号：按短边的 4.0% 实测比例计算 (严格遵循《时间戳实现指南.md》规范)
        val textSize = shortSide * 0.040f
        val typeface = loadTypeface(context, spec.fontAsset)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
            this.color = spec.textColor
            this.isSubpixelText = true
            try {
                this.setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, 0xCC000000.toInt())
            } catch (_: Throwable) {}
        }

        // 2. 健壮日期字符串格式化（严格使用 Locale.US，支持带时间的完整格式）
        val pattern = if (metadata.pattern.isNotBlank() && !metadata.pattern.contains("年") && !metadata.pattern.contains("月") && !metadata.pattern.contains("日")) {
            metadata.pattern
        } else {
            spec.defaultPattern
        }
        val date = try {
            if (metadata.dateTaken.time <= 0L) Date() else metadata.dateTaken
        } catch (_: Throwable) {
            Date()
        }
        val text = try {
            SimpleDateFormat(pattern, Locale.US).format(date)
        } catch (_: Throwable) {
            try {
                SimpleDateFormat(spec.defaultPattern, Locale.US).format(date)
            } catch (_: Throwable) {
                SimpleDateFormat("yy MM dd", Locale.US).format(date)
            }
        }

        val tw = paint.measureText(text)
        val fm = paint.fontMetrics
        val th = fm.descent - fm.ascent

        // 3. 边距与边界盒尺寸 (短边 3.0% 边距，实测 padH=0.28, padV=0.16)
        val margin = shortSide * 0.030f
        val padH = textSize * 0.28f
        val padV = textSize * 0.16f
        val boxW = tw + padH * 2f
        val boxH = th + padV * 2f

        // 4. 计算旋转与中心坐标
        val (cx, cy) = calculatePositionCenter(metadata.position, width, height, boxW, boxH, margin)

        // 5. Canvas 栈安全屏障（使用 restoreToCount，彻底避免 Underflow in restore 崩溃）
        val saveCount = canvas.save()
        try {
            // 以时间戳框中心执行倾角旋转 (DAZZ 招牌 +3.9° 微倾斜，仅非左侧纵排时执行)
            if (metadata.position != WatermarkPosition.LEFT_VERTICAL) {
                canvas.rotate(spec.tiltDeg, cx, cy)
            } else {
                // 左侧纵排旋转 +90° 自上而下顺流（与胶片机竖拍完全一致）
                canvas.rotate(90f, cx, cy)
            }

            // 纯净绘制点阵文字，绝无任何丑陋灰色背景框（基线垂直对齐）
            val baseline = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(text, cx - tw / 2f, baseline, paint)
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * 辅助工具：根据 WatermarkPosition 计算中心坐标 (cx, cy)
     */
    private fun calculatePositionCenter(
        position: WatermarkPosition,
        width: Int,
        height: Int,
        boxW: Float,
        boxH: Float,
        margin: Float
    ): Pair<Float, Float> {
        return when (position) {
            WatermarkPosition.BOTTOM_RIGHT -> Pair(width - margin - boxW / 2f, height - margin - boxH / 2f)
            WatermarkPosition.BOTTOM_LEFT -> Pair(margin + boxW / 2f, height - margin - boxH / 2f)
            WatermarkPosition.BOTTOM_CENTER -> Pair(width / 2f, height - margin - boxH / 2f)
            WatermarkPosition.LEFT_VERTICAL -> Pair(margin + boxH / 2f, height - margin - boxW / 2f)
        }
    }
}


