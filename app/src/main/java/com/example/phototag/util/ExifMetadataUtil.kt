package com.example.phototag.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.util.exif.ExifSpoofPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * 照片 EXIF 元数据与地理位置、时间生命周期详情模型。
 */
data class PhotoMetadata(
    val photoId: Long = 0,
    val fileName: String = "",
    val fileSizeFormatted: String = "",
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val resolutionFormatted: String = "",
    val megapixels: String = "",
    val aspectRatioFormatted: String = "",
    // 📅 时间与生命周期
    val dateTakenFormatted: String = "",
    val dateAddedFormatted: String = "",
    val timeOfDayBadge: String? = null,           // 如 ☀️ 正午阳光 / 🌇 傍晚黄昏 / 🌙 宁静深夜
    val relativeTimeFormatted: String? = null,     // 如 星期六 · 6天前 / 2年前的今天
    val timezoneFormatted: String? = null,         // 如 GMT+8 (中国标准时间)
    // 📍 GPS 经纬度与人类可读真实地址
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsCoordinatesFormatted: String? = null,
    val altitude: Double? = null,
    val isHighAltitude: Boolean = false,           // 是否为高原/飞行高海拔（>= 1000m）
    val highAltitudeBadge: String? = null,         // 🏔️ 高原海拔 3,820 m / ✈️ 飞行高空 9,800 m
    val altitudeShortFormatted: String? = null,    // 海拔 0 m
    val compassDirectionFormatted: String? = null, // 🧭 朝向 142° (东南)
    val address: String? = null,
    val customLocation: String? = null,
    val hasGps: Boolean = false,
    // 📷 相机与镜头参数
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val lensModel: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val focalLengthIn35mm: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val exposureBias: String? = null,
    val meteringMode: String? = null,
    val colorSpace: String? = null
)

/**
 * EXIF 解析与地理位置逆编码工具类（全通道穿透引擎）。
 */
object ExifMetadataUtil {

    private val dateTimeDisplayFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
    private val exifInputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
    private val decimalFormat = DecimalFormat("0.0")

    // 经纬度逆编码地址内存缓存 (Key: "lat_4,lng_4", Value: "详细中文地址")
    private val addressCache = ConcurrentHashMap<String, String>()

    /**
     * 尝试获取公共 MediaStore URI 对应的物理文件绝对路径（直通 POSIX 文件系统，彻底绕过 ContentResolver 的 GPS 隐私过滤脱敏）。
     */
    fun getPhysicalFilePath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme == "content") {
            try {
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (idx != -1) {
                            val path = cursor.getString(idx)
                            if (!path.isNullOrBlank()) {
                                return path
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * 智能解析 EXIF GPS 经纬度（支持标准 Double 数组与原生 Rational 分数格式兜底）。
     */
    fun extractLatLongFromExif(exif: ExifInterface): Pair<Double, Double>? {
        // 通道 1：ExifInterface 自带 latLong 解析
        val latLong = exif.latLong
        if (latLong != null && latLong.size >= 2) {
            val lat = latLong[0]
            val lng = latLong[1]
            if (lat != 0.0 || lng != 0.0) {
                return Pair(lat, lng)
            }
        }

        // 通道 2：手动提取 TAG_GPS_LATITUDE 与 TAG_GPS_LONGITUDE 原始属性进行分数转换
        try {
            val latStr = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            val latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) ?: "N"
            val lngStr = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            val lngRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) ?: "E"

            if (!latStr.isNullOrBlank() && !lngStr.isNullOrBlank()) {
                val lat = convertRationalLatLonToDouble(latStr, latRef)
                val lng = convertRationalLatLonToDouble(lngStr, lngRef)
                if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                    return Pair(lat, lng)
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * 将 EXIF 中的 Rational 经纬度度分秒（如 "30/1,15/1,2345/100" 或 "30,15,23.45"）转换为标准十进制 Double。
     */
    private fun convertRationalLatLonToDouble(rationalString: String?, ref: String?): Double? {
        if (rationalString.isNullOrBlank()) return null
        try {
            val parts = rationalString.split(",")
            if (parts.size == 3) {
                val degrees = parseRational(parts[0].trim())
                val minutes = parseRational(parts[1].trim())
                val seconds = parseRational(parts[2].trim())
                var result = degrees + (minutes / 60.0) + (seconds / 3600.0)
                if (ref.equals("S", ignoreCase = true) || ref.equals("W", ignoreCase = true)) {
                    result = -result
                }
                return result
            } else if (parts.size == 1) {
                val single = parseRational(parts[0].trim())
                var result = single
                if (ref.equals("S", ignoreCase = true) || ref.equals("W", ignoreCase = true)) {
                    result = -result
                }
                return result
            }
        } catch (_: Exception) {}
        return null
    }

    private fun parseRational(token: String): Double {
        return if (token.contains("/")) {
            val sub = token.split("/")
            val num = sub[0].toDoubleOrNull() ?: 0.0
            val den = sub.getOrNull(1)?.toDoubleOrNull() ?: 1.0
            if (den != 0.0) num / den else 0.0
        } else {
            token.toDoubleOrNull() ?: 0.0
        }
    }

    /**
     * 异步解析单张照片的完整 EXIF 摄影参数与地理地址（物理文件直读 + ContentResolver 多通道穿透）。
     */
    suspend fun parseMetadata(context: Context, photo: PhotoEntity): PhotoMetadata = withContext(Dispatchers.IO) {
        val uri = Uri.parse(photo.uri)
        var latitude: Double? = photo.latitude
        var longitude: Double? = photo.longitude
        var altitude: Double? = photo.altitude
        var dateTakenStr: String? = null
        var dateTakenParsedDate: Date? = null
        var deviceMake: String? = null
        var deviceModel: String? = null
        var lensModel: String? = null
        var aperture: String? = null
        var shutterSpeed: String? = null
        var iso: String? = null
        var focalLength: String? = null
        var focalLength35mm: String? = null
        var flashStr: String? = null
        var whiteBalanceStr: String? = null
        var exposureBiasStr: String? = null
        var meteringModeStr: String? = null
        var colorSpaceStr: String? = null
        var compassDirectionStr: String? = null
        var exifWidth: Int = photo.width
        var exifHeight: Int = photo.height

        var parsedExif: ExifInterface? = null

        // 1. 【优先通道】：尝试从真实物理文件路径直读（彻底规避 Android 10+ MediaStore 对 GPS 的隐私脱敏）
        val physicalPath = getPhysicalFilePath(context, uri)
        if (!physicalPath.isNullOrBlank()) {
            val file = File(physicalPath)
            if (file.exists() && file.canRead()) {
                try {
                    parsedExif = ExifInterface(physicalPath)
                } catch (_: Exception) {
                    try {
                        FileInputStream(file).use { fis ->
                            parsedExif = ExifInterface(fis)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 2. 【备选通道 A】：使用 openFileDescriptor
        if (parsedExif == null) {
            val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try { MediaStore.setRequireOriginal(uri) } catch (_: Exception) { uri }
            } else uri

            try {
                context.contentResolver.openFileDescriptor(targetUri, "r")?.use { pfd ->
                    parsedExif = ExifInterface(pfd.fileDescriptor)
                }
            } catch (_: Exception) {}
        }

        // 3. 【备选通道 B】：使用 openInputStream
        if (parsedExif == null) {
            val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try { MediaStore.setRequireOriginal(uri) } catch (_: Exception) { uri }
            } else uri

            try {
                context.contentResolver.openInputStream(targetUri)?.use { inputStream ->
                    parsedExif = ExifInterface(inputStream)
                }
            } catch (_: Exception) {}
        }

        // 解析 EXIF 属性
        parsedExif?.let { exif ->
            // A. 经纬度提取
            val latLngPair = extractLatLongFromExif(exif)
            if (latLngPair != null) {
                latitude = latLngPair.first
                longitude = latLngPair.second
            }

            // B. 海拔高程
            val alt = exif.getAltitude(Double.NaN)
            if (!alt.isNaN() && alt in -500.0..15000.0) {
                altitude = alt
            }

            // C. 拍摄朝向/指南针
            val imgDir = exif.getAttributeDouble(ExifInterface.TAG_GPS_IMG_DIRECTION, Double.NaN)
            if (!imgDir.isNaN()) {
                val deg = (imgDir.roundToInt() % 360 + 360) % 360
                val dirName = when (deg) {
                    in 23..67 -> "东北"
                    in 68..112 -> "正东"
                    in 113..157 -> "东南"
                    in 158..202 -> "正南"
                    in 203..247 -> "西南"
                    in 248..292 -> "正西"
                    in 293..337 -> "西北"
                    else -> "正北"
                }
                compassDirectionStr = "朝向 $deg° ($dirName)"
            }

            // D. 拍摄精确时间
            val origDate = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (!origDate.isNullOrBlank()) {
                try {
                    val parsed = exifInputFormat.parse(origDate)
                    if (parsed != null) {
                        dateTakenParsedDate = parsed
                        dateTakenStr = dateTimeDisplayFormat.format(parsed)
                    }
                } catch (_: Exception) {
                    dateTakenStr = origDate
                }
            }

            // E. 设备与相机
            deviceMake = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
            deviceModel = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
            lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()

            if (deviceMake != null && deviceModel != null && deviceModel.startsWith(deviceMake, ignoreCase = true)) {
                deviceMake = null
            }

            // F. 光圈
            val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
            if (fNumber > 0) {
                aperture = "f/${if (fNumber % 1.0 == 0.0) fNumber.toInt().toString() else decimalFormat.format(fNumber)}"
            } else {
                val apValue = exif.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.0)
                if (apValue > 0) aperture = "f/${decimalFormat.format(apValue)}"
            }

            // G. 快门速度
            val expTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            if (expTime > 0) {
                shutterSpeed = if (expTime < 1.0) {
                    val denominator = (1.0 / expTime).roundToInt()
                    "1/$denominator s"
                } else {
                    "${decimalFormat.format(expTime)} s"
                }
            }

            // H. ISO 感光度
            val isoVal = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                .takeIf { it > 0 }
                ?: exif.getAttributeInt("ISOSpeedRatings", 0)
            if (isoVal > 0) {
                iso = "ISO $isoVal"
            }

            // I. 焦距
            val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            if (focal > 0) {
                focalLength = "${if (focal % 1.0 == 0.0) focal.toInt().toString() else decimalFormat.format(focal)} mm"
            }
            val focal35 = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
            if (focal35 > 0) {
                focalLength35mm = "${focal35} mm (等效 35mm)"
            }

            // J. 闪光灯
            val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
            if (flash >= 0) {
                flashStr = if (flash and 1 != 0) "闪光灯已开启" else "闪光灯未开启"
            }

            // K. 白平衡
            val wb = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
            if (wb >= 0) {
                whiteBalanceStr = if (wb == 1) "手动白平衡" else "自动白平衡"
            }

            // L. 曝光补偿
            val expBias = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, Double.NaN)
            if (!expBias.isNaN()) {
                exposureBiasStr = "${if (expBias > 0) "+" else ""}${decimalFormat.format(expBias)} EV"
            }

            // M. 测光模式
            val metering = exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1)
            meteringModeStr = when (metering) {
                1 -> "平均测光"
                2 -> "中央重点平均测光"
                3 -> "点测光"
                4 -> "多点测光"
                5 -> "矩阵测光 (多区域)"
                6 -> "局部测光"
                else -> null
            }

            // N. 色彩空间
            val cs = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)
            colorSpaceStr = when (cs) {
                1 -> "sRGB"
                2 -> "Adobe RGB"
                else -> null
            }

            // O. 分辨率
            val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            if (w > 0 && h > 0) {
                exifWidth = w
                exifHeight = h
            }
        }

        val finalLat = latitude
        val finalLng = longitude
        var finalDateTaken = dateTakenStr

        val targetDate = dateTakenParsedDate ?: Date(if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L)

        // 兜底时间格式化
        if (finalDateTaken.isNullOrBlank()) {
            finalDateTaken = dateTimeDisplayFormat.format(targetDate)
        }
        val dateAddedStr = dateTimeDisplayFormat.format(Date(if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L))

        // 智能时段推断 (清晨/上午/正午/午后/傍晚/夜间/深夜)
        val cal = Calendar.getInstance().apply { time = targetDate }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val timeBadge = when (hour) {
            in 5..8 -> "清晨"
            in 9..11 -> "上午"
            in 12..13 -> "正午"
            in 14..17 -> "午后"
            in 18..19 -> "傍晚"
            in 20..22 -> "夜间"
            else -> "深夜"
        }

        // 相对时间与星期
        val now = System.currentTimeMillis()
        val diffMs = now - targetDate.time
        val days = (diffMs / (24 * 3600 * 1000L)).toInt()
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.CHINESE).format(targetDate)
        val relativeTime = when {
            days <= 0 -> "$dayOfWeek · 今天"
            days == 1 -> "$dayOfWeek · 昨天"
            days == 2 -> "$dayOfWeek · 前天"
            days in 3..30 -> "$dayOfWeek · ${days}天前"
            days in 31..365 -> "$dayOfWeek · ${days / 30}个月前"
            else -> "$dayOfWeek · ${days / 365}年前"
        }

        // 计算像素与分辨率
        val mp = if (exifWidth > 0 && exifHeight > 0) {
            val totalPx = exifWidth.toLong() * exifHeight.toLong()
            val mpVal = totalPx / 1_000_000.0
            "${decimalFormat.format(mpVal)} MP"
        } else ""
        val resolution = if (exifWidth > 0 && exifHeight > 0) "$exifWidth × $exifHeight" else ""

        // 画幅比例
        val aspectRatioStr = if (exifWidth > 0 && exifHeight > 0) {
            val ratio = exifWidth.toDouble() / exifHeight.toDouble()
            when {
                Math.abs(ratio - 1.0) < 0.04 -> "1:1 (正方形)"
                Math.abs(ratio - 4.0/3.0) < 0.04 -> "4:3 (横向标准)"
                Math.abs(ratio - 3.0/4.0) < 0.04 -> "3:4 (纵向标准)"
                Math.abs(ratio - 16.0/9.0) < 0.04 -> "16:9 (横向宽屏)"
                Math.abs(ratio - 9.0/16.0) < 0.04 -> "9:16 (纵向全屏)"
                Math.abs(ratio - 3.0/2.0) < 0.04 -> "3:2 (单反横画幅)"
                Math.abs(ratio - 2.0/3.0) < 0.04 -> "2:3 (单反竖画幅)"
                else -> "${exifWidth}:${exifHeight}"
            }
        } else ""

        // 计算文件大小格式化
        val fileSizeStr = formatFileSize(photo.size)

        // GPS 格式化字符串
        val gpsFormatted = if (finalLat != null && finalLng != null) {
            val latDir = if (finalLat >= 0) "N" else "S"
            val lngDir = if (finalLng >= 0) "E" else "W"
            String.format(Locale.US, "%.4f°%s, %.4f°%s", Math.abs(finalLat), latDir, Math.abs(finalLng), lngDir)
        } else null

        // 方案 B：海拔智能判断 (>= 1000m 为高原/登山，>= 3000m 为飞行高空)
        val currentAlt = altitude
        val isHighAlt = currentAlt != null && currentAlt >= 1000.0
        val highAltBadge = if (currentAlt != null && currentAlt >= 3000.0) {
            "高空 ${DecimalFormat("#,###").format(currentAlt.roundToInt())} m"
        } else if (currentAlt != null && currentAlt >= 1000.0) {
            "高海拔 ${DecimalFormat("#,###").format(currentAlt.roundToInt())} m"
        } else null

        val altShort = if (currentAlt != null) {
            "海拔 ${currentAlt.roundToInt()} m"
        } else null

        // 逆编码真实中文地址：优先使用 PhotoEntity 已经持久化在本地数据库中的 address
        var resolvedAddress: String? = photo.address
        if (resolvedAddress.isNullOrBlank() || isGarbageAddress(resolvedAddress)) {
            // A. 优先从知名地标预设库中高精匹配 (0ms 秒级呈现精准商圈地标)
            if (finalLat != null && finalLng != null) {
                val matched = ExifSpoofPresets.findMatchingPreset(finalLat, finalLng)
                if (matched != null) {
                    resolvedAddress = matched.displayAddress.ifBlank { matched.name }
                }
            }
            // B. 尝试从有效 EXIF 描述中读取（过滤掉 ???? 乱码）
            if (resolvedAddress.isNullOrBlank() || isGarbageAddress(resolvedAddress)) {
                val exifDesc = parsedExif?.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.trim()
                    ?: parsedExif?.getAttribute(ExifInterface.TAG_USER_COMMENT)?.trim()
                if (!exifDesc.isNullOrBlank() && !isGarbageAddress(exifDesc)) {
                    resolvedAddress = exifDesc
                }
            }
            // C. 若仍无有效地址，调用高精多通道逆地理编码引擎
            if ((resolvedAddress.isNullOrBlank() || isGarbageAddress(resolvedAddress)) && finalLat != null && finalLng != null) {
                resolvedAddress = AutoLocationResolver.resolveAddress(context, finalLat, finalLng)
            }
        }

        PhotoMetadata(
            photoId = photo.photoId,
            fileName = photo.displayName,
            fileSizeFormatted = fileSizeStr,
            fileSizeBytes = photo.size,
            mimeType = photo.mimeType,
            width = exifWidth,
            height = exifHeight,
            resolutionFormatted = resolution,
            megapixels = mp,
            aspectRatioFormatted = aspectRatioStr,
            dateTakenFormatted = finalDateTaken,
            dateAddedFormatted = dateAddedStr,
            timeOfDayBadge = timeBadge,
            relativeTimeFormatted = relativeTime,
            timezoneFormatted = "GMT+8 (中国标准时间)",
            latitude = finalLat,
            longitude = finalLng,
            gpsCoordinatesFormatted = gpsFormatted,
            altitude = altitude,
            isHighAltitude = isHighAlt,
            highAltitudeBadge = highAltBadge,
            altitudeShortFormatted = altShort,
            compassDirectionFormatted = compassDirectionStr,
            address = resolvedAddress,
            customLocation = photo.customLocation,
            hasGps = finalLat != null && finalLng != null,
            deviceMake = deviceMake,
            deviceModel = deviceModel,
            lensModel = lensModel,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            iso = iso,
            focalLength = focalLength,
            focalLengthIn35mm = focalLength35mm,
            flash = flashStr,
            whiteBalance = whiteBalanceStr,
            exposureBias = exposureBiasStr,
            meteringMode = meteringModeStr,
            colorSpace = colorSpaceStr
        )
    }

    /**
     * 判断地址或文本是否由 ASCII 强制转换导致的问号乱码 (如 "???? ? ????")
     */
    fun isGarbageAddress(addr: String?): Boolean {
        if (addr.isNullOrBlank()) return true
        val stripped = addr.replace("?", "").replace("？", "").replace(" ", "").trim()
        return stripped.isEmpty()
    }

    data class GpsLocationInfo(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double? = null,
        val description: String? = null
    )

    /**
     * 快速提取图片的 GPS 经纬度坐标与海拔高程（物理文件 + 多通道穿透提取）。
     */
    fun extractGpsCoordinatesAndAltitude(context: Context, uriString: String): GpsLocationInfo? {
        return try {
            val uri = Uri.parse(uriString)

            // 1. 【优先通道】：真实物理文件直读
            val physicalPath = getPhysicalFilePath(context, uri)
            if (!physicalPath.isNullOrBlank()) {
                val file = File(physicalPath)
                if (file.exists() && file.canRead()) {
                    try {
                        val exif = ExifInterface(physicalPath)
                        val latLng = extractLatLongFromExif(exif)
                        if (latLng != null) {
                            val alt = exif.getAltitude(Double.NaN)
                            val validAlt = if (!alt.isNaN() && alt in -500.0..15000.0) alt else null
                            val rawDesc = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.trim()
                                ?: exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.trim()
                            val desc = if (!isGarbageAddress(rawDesc)) rawDesc else null
                            return GpsLocationInfo(latLng.first, latLng.second, validAlt, desc)
                        }
                    } catch (_: Exception) {}
                }
            }

            val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try { MediaStore.setRequireOriginal(uri) } catch (_: Exception) { uri }
            } else uri

            // 2. 【通道二】：FileDescriptor
            try {
                context.contentResolver.openFileDescriptor(targetUri, "r")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    val latLng = extractLatLongFromExif(exif)
                    if (latLng != null) {
                        val alt = exif.getAltitude(Double.NaN)
                        val validAlt = if (!alt.isNaN() && alt in -500.0..15000.0) alt else null
                        val rawDesc = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.trim()
                            ?: exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.trim()
                        val desc = if (!isGarbageAddress(rawDesc)) rawDesc else null
                        return GpsLocationInfo(latLng.first, latLng.second, validAlt, desc)
                    }
                }
            } catch (_: Exception) {}

            // 3. 【通道三】：InputStream
            try {
                context.contentResolver.openInputStream(targetUri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val latLng = extractLatLongFromExif(exif)
                    if (latLng != null) {
                        val alt = exif.getAltitude(Double.NaN)
                        val validAlt = if (!alt.isNaN() && alt in -500.0..15000.0) alt else null
                        val rawDesc = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.trim()
                            ?: exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.trim()
                        val desc = if (!isGarbageAddress(rawDesc)) rawDesc else null
                        return GpsLocationInfo(latLng.first, latLng.second, validAlt, desc)
                    }
                }
            } catch (_: Exception) {}

            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 快速提取图片的 GPS 经纬度坐标。
     */
    fun extractGpsCoordinates(context: Context, uriString: String): Pair<Double, Double>? {
        val info = extractGpsCoordinatesAndAltitude(context, uriString) ?: return null
        return Pair(info.latitude, info.longitude)
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        if (name.isNullOrBlank()) {
            val last = uri.lastPathSegment ?: "image"
            val mimeType = try { context.contentResolver.getType(uri) ?: "image/jpeg" } catch (_: Exception) { "image/jpeg" }
            val ext = when {
                mimeType.contains("png", ignoreCase = true) -> ".png"
                mimeType.contains("gif", ignoreCase = true) -> ".gif"
                mimeType.contains("webp", ignoreCase = true) -> ".webp"
                mimeType.contains("mp4", ignoreCase = true) -> ".mp4"
                mimeType.contains("heic", ignoreCase = true) -> ".heic"
                else -> ".jpg"
            }
            name = if (last.contains(".")) last else "$last$ext"
        }
        return name
    }

    /**
     * 临时脱敏照片的 EXIF GPS 经纬度数据供安全分享。
     */
    suspend fun stripGpsMetadataForShare(context: Context, uris: List<Uri>): List<Uri> = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "shared_stripped_photos")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        uris.map { originalUri ->
            try {
                val originalFileName = getFileNameFromUri(context, originalUri)
                val cleanFileName = if (originalFileName.contains(".")) originalFileName else "$originalFileName.jpg"
                val tempFile = File(cacheDir, "share_${System.currentTimeMillis()}_$cleanFileName")

                context.contentResolver.openInputStream(originalUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                try {
                    val exif = ExifInterface(tempFile.absolutePath)
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_MAP_DATUM, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_SPEED, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_SPEED_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_STATUS, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_TRACK, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_TRACK_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_VERSION_ID, null)
                    exif.saveAttributes()
                } catch (_: Exception) {}

                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } catch (_: Exception) {
                originalUri
            }
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "${decimalFormat.format(gb)} GB"
            mb >= 1.0 -> "${decimalFormat.format(mb)} MB"
            kb >= 1.0 -> "${decimalFormat.format(kb)} KB"
            else -> "$bytes B"
        }
    }
}
