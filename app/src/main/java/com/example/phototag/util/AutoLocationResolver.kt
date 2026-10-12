package com.example.phototag.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 📍 高精度建筑场所与街道级自动逆地理编码引擎 (AutoLocationResolver)。
 *
 * 核心架构设计：
 * 1. 【高精四核多通道调度】：
 *    - 通道一：高德地图 Web 高精逆地理编码服务（国内 WGS-84 转 GCJ-02，毫秒级响应，建筑/街道/POI 场所门牌级解析）。
 *    - 通道二：BigDataCloud 全球免费免 Key 逆地理客户端（海内外全通，快速兜底）。
 *    - 通道三：OpenStreetMap Nominatim / Photon 高精街道引擎（zoom=18 建筑门牌级）。
 *    - 通道四：Android 原生系统级 Geocoder（离线与系统级服务）。
 * 2. 【智能街道深度校验 (isDetailedAddress)】：
 *    - 优先保留包含“具体路名/街名/门牌/建筑地标”的高精详细地址。
 * 3. 【500 条高精 LRU 缓存池与简繁自愈】：
 *    - 本地内存极速命中，自动规范化为纯净简体中文，自动移除多余的“中华人民共和国”前缀。
 */
object AutoLocationResolver {

    private const val TAG = "AutoLocationResolver"

    // 内存 LRU 缓存，容量 500 条地址记录
    private val memoryCache = LruCache<String, String>(500)

    /**
     * 判断一个解析出的地址是否已经包含具体的街道/道路/门牌或地标 POI（而非泛泛的省市区）。
     */
    fun isDetailedAddress(addr: String?): Boolean {
        if (addr.isNullOrBlank()) return false
        if (addr.contains(" · ")) return true
        val clean = addr.removePrefix("中华人民共和国").removePrefix("中国").trim()
        return clean.contains("路") || clean.contains("街") || clean.contains("道") ||
                clean.contains("巷") || clean.contains("弄") || clean.contains("号") ||
                clean.contains("院") || clean.contains("楼") || clean.contains("大厦") ||
                clean.contains("广场") || clean.contains("公园") || clean.contains("开发区") ||
                clean.contains("村") || clean.contains("庄") || clean.contains("大学") ||
                clean.contains("校区") || clean.contains("商场") || clean.contains("中心")
    }

    /**
     * 对经纬度发起全自动高精度建筑、街道与场所级逆地理编码解析。
     *
     * @param context Android 上下文
     * @param latitude 原始 WGS-84 纬度
     * @param longitude 原始 WGS-84 经度
     * @return 格式化后的高精度建筑/街道/场所地名（如："广东省深圳市南山区科苑南路 · 腾讯大厦"），失败时返回 null
     */
    suspend fun resolveAddress(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        // 1. 检查经纬度合法性
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return@withContext null
        }

        // 2. 检查内存缓存 (约 11 米精度)
        val cacheKey = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
        memoryCache.get(cacheKey)?.let {
            if (isDetailedAddress(it)) {
                Log.d(TAG, "从内存缓存命中高精度详细地址: $cacheKey -> $it")
                return@withContext it
            }
        }

        var detailedAddress: String? = null
        var fallbackCoarseAddress: String? = null

        val isInsideChina = latitude in 3.0..54.0 && longitude in 73.0..136.0

        // ==================== 通道一：高德高精逆地理编码服务 (国内优先) ====================
        if (isInsideChina) {
            try {
                val amapResult = resolveViaAmap(latitude, longitude)
                if (isDetailedAddress(amapResult)) {
                    detailedAddress = amapResult
                    Log.d(TAG, "通道一 (高德 Web) 街道建筑级解析成功: $detailedAddress")
                } else if (!amapResult.isNullOrBlank()) {
                    fallbackCoarseAddress = amapResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "高德 Web 解析异常: ${e.message}")
            }
        }

        // ==================== 通道二：BigDataCloud 免费客户端逆编码 ====================
        if (detailedAddress == null) {
            try {
                val bdcResult = resolveViaBigDataCloud(latitude, longitude)
                if (isDetailedAddress(bdcResult)) {
                    detailedAddress = bdcResult
                    Log.d(TAG, "通道二 (BigDataCloud) 街道级解析成功: $detailedAddress")
                } else if (fallbackCoarseAddress == null && !bdcResult.isNullOrBlank()) {
                    fallbackCoarseAddress = bdcResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "BigDataCloud 解析异常: ${e.message}")
            }
        }

        // ==================== 通道三：OpenStreetMap Nominatim (zoom=18) ====================
        if (detailedAddress == null) {
            try {
                val nominatimResult = resolveViaNominatim(latitude, longitude)
                if (isDetailedAddress(nominatimResult)) {
                    detailedAddress = nominatimResult
                    Log.d(TAG, "通道三 (OSM Nominatim) 街道建筑级解析成功: $detailedAddress")
                } else if (fallbackCoarseAddress == null && !nominatimResult.isNullOrBlank()) {
                    fallbackCoarseAddress = nominatimResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Nominatim 解析异常: ${e.message}")
            }
        }

        // ==================== 通道四：Photon Komoot Open Geocoder ====================
        if (detailedAddress == null) {
            try {
                val photonResult = resolveViaPhoton(latitude, longitude)
                if (isDetailedAddress(photonResult)) {
                    detailedAddress = photonResult
                    Log.d(TAG, "通道四 (Photon) 街道级解析成功: $detailedAddress")
                } else if (fallbackCoarseAddress == null && !photonResult.isNullOrBlank()) {
                    fallbackCoarseAddress = photonResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Photon 解析异常: ${e.message}")
            }
        }

        // ==================== 通道五：Android 原生 Geocoder ====================
        if (detailedAddress == null) {
            try {
                val nativeResult = resolveViaNativeGeocoder(context, latitude, longitude)
                if (isDetailedAddress(nativeResult)) {
                    detailedAddress = nativeResult
                    Log.d(TAG, "通道五 (原生 Geocoder) 详细地址解析成功: $detailedAddress")
                } else if (fallbackCoarseAddress == null && !nativeResult.isNullOrBlank()) {
                    fallbackCoarseAddress = nativeResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "原生 Geocoder 异常: ${e.message}")
            }
        }

        val finalResult = detailedAddress ?: fallbackCoarseAddress

        // 写入内存缓存
        if (!finalResult.isNullOrBlank()) {
            memoryCache.put(cacheKey, finalResult)
        }

        return@withContext finalResult
    }

    /**
     * 【通道一】：高德地图 Web 逆地理编码服务（带 GCJ-02 转换与 POI 地标精准解析）。
     */
    private fun resolveViaAmap(wgsLat: Double, wgsLng: Double): String? {
        var connection: HttpURLConnection? = null
        return try {
            val (gcjLat, gcjLng) = CoordinateTransformUtil.wgs84ToGcj02(wgsLat, wgsLng)
            val urlString = "https://restapi.amap.com/v3/geocode/regeo?key=8325164e247e15ced68e59c509b03e32&location=%.6f,%.6f&extensions=all&output=json".format(
                Locale.US, gcjLng, gcjLat
            )
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "FutureAlbumApp/7.9 (Android; Mobile)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
                parseAmapJson(responseText)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseAmapJson(jsonStr: String): String? {
        return try {
            val root = JSONObject(jsonStr)
            if (root.optString("status") != "1") return null
            val regeocode = root.optJSONObject("regeocode") ?: return null

            val formattedAddress = regeocode.optString("formatted_address", "")
            val addressComp = regeocode.optJSONObject("addressComponent")

            val province = addressComp?.optString("province", "") ?: ""
            val city = addressComp?.optString("city", "")?.takeIf { it != "[]" } ?: ""
            val district = addressComp?.optString("district", "")?.takeIf { it != "[]" } ?: ""
            val township = addressComp?.optString("township", "")?.takeIf { it != "[]" } ?: ""

            var street = ""
            val streetNumberObj = addressComp?.optJSONObject("streetNumber")
            if (streetNumberObj != null) {
                val s = streetNumberObj.optString("street", "")
                val n = streetNumberObj.optString("number", "")
                street = if (s.isNotBlank() && n.isNotBlank()) "$s$n" else s.ifBlank { n }
            }

            // 提取最精确的 POI 或建筑名称
            var landmark = ""
            val pois = regeocode.optJSONArray("pois")
            if (pois != null && pois.length() > 0) {
                val firstPoi = pois.optJSONObject(0)
                landmark = firstPoi?.optString("name", "") ?: ""
            }
            if (landmark.isBlank()) {
                val aois = regeocode.optJSONArray("aois")
                if (aois != null && aois.length() > 0) {
                    landmark = aois.optJSONObject(0)?.optString("name", "") ?: ""
                }
            }

            if (formattedAddress.isNotBlank()) {
                val clean = formattedAddress.removePrefix("中华人民共和国").removePrefix("中国").trim()
                if (landmark.isNotBlank() && !clean.contains(landmark)) {
                    "$clean · $landmark"
                } else {
                    clean
                }
            } else {
                buildChineseDetailedAddress(
                    country = "中国",
                    province = province,
                    city = city,
                    district = district,
                    street = if (street.isNotBlank()) street else township,
                    landmark = landmark
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 【通道二】：BigDataCloud 免费客户端逆编码。
     */
    private fun resolveViaBigDataCloud(lat: Double, lng: Double): String? {
        var connection: HttpURLConnection? = null
        return try {
            val urlString = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lng&localityLanguage=zh"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3200
                readTimeout = 3200
                setRequestProperty("User-Agent", "FutureAlbumApp/7.9 (Android; Mobile)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
                parseBigDataCloudJson(responseText)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseBigDataCloudJson(jsonStr: String): String? {
        return try {
            val root = JSONObject(jsonStr)
            val country = root.optString("countryName", "")
            val province = root.optString("principalSubdivision", "")
            val city = root.optString("city", "")
            val locality = root.optString("locality", "")

            var district = ""
            var street = ""

            val adminArray = root.optJSONObject("localityInfo")?.optJSONArray("administrative")
            if (adminArray != null) {
                for (i in 0 until adminArray.length()) {
                    val obj = adminArray.optJSONObject(i) ?: continue
                    val adminLevel = obj.optInt("adminLevel", -1)
                    val name = obj.optString("name", "")
                    if (adminLevel == 6 && district.isBlank()) {
                        district = name
                    } else if (adminLevel >= 7 && street.isBlank()) {
                        street = name
                    }
                }
            }

            if (district.isBlank()) district = locality

            buildChineseDetailedAddress(
                country = country,
                province = province,
                city = city,
                district = district,
                street = street,
                landmark = ""
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 【通道三】：OpenStreetMap Nominatim 逆编码 (zoom=18 高精度街道/门牌/地标)。
     */
    private fun resolveViaNominatim(lat: Double, lng: Double): String? {
        var connection: HttpURLConnection? = null
        return try {
            val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=18&addressdetails=1&accept-language=zh-CN,zh-Hans;q=1.0"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "FutureAlbumApp/7.9 (Android; Mobile)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
                parseNominatimJson(responseText)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseNominatimJson(jsonStr: String): String? {
        return try {
            val root = JSONObject(jsonStr)
            val addressObj = root.optJSONObject("address")

            if (addressObj != null) {
                val country = addressObj.optString("country", "")
                val state = addressObj.optString("state", addressObj.optString("province", ""))
                val city = addressObj.optString("city", addressObj.optString("municipality", addressObj.optString("prefecture", "")))
                val district = addressObj.optString("district", addressObj.optString("county", addressObj.optString("city_district", addressObj.optString("borough", ""))))
                val town = addressObj.optString("suburb", addressObj.optString("town", addressObj.optString("village", addressObj.optString("neighbourhood", addressObj.optString("residential", "")))))
                val road = addressObj.optString("road", addressObj.optString("pedestrian", addressObj.optString("street", addressObj.optString("footway", addressObj.optString("path", "")))))
                val houseNumber = addressObj.optString("house_number", addressObj.optString("street_number", ""))

                val landmark = addressObj.optString("amenity",
                    addressObj.optString("tourism",
                    addressObj.optString("leisure",
                    addressObj.optString("shop",
                    addressObj.optString("commercial",
                    addressObj.optString("historic",
                    addressObj.optString("building",
                    addressObj.optString("office",
                    addressObj.optString("craft", "")))))))))

                val bestPoi = when {
                    landmark.isNotBlank() -> landmark
                    town.isNotBlank() && town.contains("开发区") -> town
                    town.isNotBlank() && town.contains("园区") -> town
                    else -> ""
                }

                val combinedRoad = when {
                    road.isNotBlank() && houseNumber.isNotBlank() -> {
                        val numSuffix = if (houseNumber.all { it.isDigit() }) "${houseNumber}号" else houseNumber
                        "$road$numSuffix"
                    }
                    road.isNotBlank() -> road
                    town.isNotBlank() -> town
                    else -> ""
                }

                buildChineseDetailedAddress(
                    country = country,
                    province = state,
                    city = city,
                    district = district,
                    street = combinedRoad,
                    landmark = if (bestPoi != town) bestPoi else town
                )
            } else {
                if (root.has("display_name")) normalizeToSimplifiedChinese(root.optString("display_name")) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 【通道四】：Photon Komoot Open Geocoder (基于 OSM 极速街道引擎)。
     */
    private fun resolveViaPhoton(lat: Double, lng: Double): String? {
        var connection: HttpURLConnection? = null
        return try {
            val urlString = "https://photon.komoot.io/reverse?lat=$lat&lon=$lng"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3200
                readTimeout = 3200
                setRequestProperty("User-Agent", "FutureAlbumApp/7.9 (Android; Mobile)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
                parsePhotonJson(responseText)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parsePhotonJson(jsonStr: String): String? {
        return try {
            val root = JSONObject(jsonStr)
            val features = root.optJSONArray("features") ?: return null
            if (features.length() == 0) return null

            val firstFeature = features.optJSONObject(0) ?: return null
            val props = firstFeature.optJSONObject("properties") ?: return null

            val country = props.optString("country", "")
            val state = props.optString("state", "")
            val city = props.optString("city", "")
            val district = props.optString("district", props.optString("county", ""))
            val streetName = props.optString("name", props.optString("street", ""))
            val houseNumber = props.optString("housenumber", "")

            val combinedStreet = if (streetName.isNotBlank() && houseNumber.isNotBlank()) {
                val num = if (houseNumber.all { it.isDigit() }) "${houseNumber}号" else houseNumber
                "$streetName$num"
            } else {
                streetName
            }

            buildChineseDetailedAddress(
                country = country,
                province = state,
                city = city,
                district = district,
                street = combinedStreet,
                landmark = ""
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 【通道五】：Android 原生 Geocoder。
     */
    private suspend fun resolveViaNativeGeocoder(
        context: Context,
        lat: Double,
        lng: Double
    ): String? = withTimeoutOrNull(3000L) {
        if (!Geocoder.isPresent()) return@withTimeoutOrNull null

        val geocoder = Geocoder(context, Locale.SIMPLIFIED_CHINESE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val first = addresses.firstOrNull()
                        val formatted = formatNativeAddress(first)
                        continuation.resume(formatted)
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lng, 1)
            formatNativeAddress(list?.firstOrNull())
        }
    }

    private fun formatNativeAddress(addr: Address?): String? {
        if (addr == null) return null

        val country = addr.countryName ?: ""
        val adminArea = addr.adminArea ?: ""
        val locality = addr.locality ?: addr.subAdminArea ?: ""
        val subLocality = addr.subLocality ?: ""
        val thoroughfare = addr.thoroughfare ?: ""
        val subThoroughfare = addr.subThoroughfare ?: ""
        val featureName = addr.featureName ?: ""

        val streetWithNumber = when {
            thoroughfare.isNotBlank() && subThoroughfare.isNotBlank() -> {
                val num = if (subThoroughfare.all { it.isDigit() }) "${subThoroughfare}号" else subThoroughfare
                "$thoroughfare$num"
            }
            thoroughfare.isNotBlank() -> thoroughfare
            subThoroughfare.isNotBlank() -> if (subThoroughfare.all { it.isDigit() }) "${subThoroughfare}号" else subThoroughfare
            else -> ""
        }

        val rawLine = addr.getAddressLine(0)
        if (!rawLine.isNullOrBlank()) {
            val cleanedRaw = rawLine
                .removePrefix("中华人民共和国")
                .removePrefix("中国")
                .removePrefix("CN")
                .removePrefix("China")
                .trim()

            if (cleanedRaw.isNotBlank() && (cleanedRaw.contains("路") || cleanedRaw.contains("街") || cleanedRaw.contains("道") || cleanedRaw.contains("号"))) {
                return normalizeToSimplifiedChinese(cleanedRaw)
            }
        }

        return buildChineseDetailedAddress(
            country = country,
            province = adminArea,
            city = locality,
            district = subLocality,
            street = streetWithNumber,
            landmark = featureName
        )
    }

    fun buildChineseDetailedAddress(
        country: String,
        province: String,
        city: String,
        district: String,
        street: String,
        landmark: String
    ): String {
        val cleanCountry = normalizeToSimplifiedChinese(country.trim())
        val cleanProvince = normalizeToSimplifiedChinese(province.trim())
        val cleanCity = normalizeToSimplifiedChinese(city.trim())
        val cleanDistrict = normalizeToSimplifiedChinese(district.trim())
        val cleanStreet = normalizeToSimplifiedChinese(street.trim())
        val cleanLandmark = normalizeToSimplifiedChinese(landmark.trim())

        val baseParts = StringBuilder()

        if (cleanCountry.isNotBlank() && cleanCountry != "中国" && cleanCountry != "China" && cleanCountry != "CN" && cleanCountry != "中华人民共和国") {
            baseParts.append(cleanCountry).append(" ")
        }
        if (cleanProvince.isNotBlank()) {
            baseParts.append(cleanProvince)
        }
        if (cleanCity.isNotBlank() && cleanCity != cleanProvince && !cleanProvince.contains(cleanCity)) {
            baseParts.append(cleanCity)
        }
        if (cleanDistrict.isNotBlank() && cleanDistrict != cleanCity && cleanDistrict != cleanProvince) {
            baseParts.append(cleanDistrict)
        }
        if (cleanStreet.isNotBlank() && cleanStreet != cleanDistrict && cleanStreet != cleanCity) {
            baseParts.append(cleanStreet)
        }

        val baseAddrStr = baseParts.toString()
            .removePrefix("中华人民共和国")
            .removePrefix("中国")
            .trim()

        val isLandmarkValid = cleanLandmark.isNotBlank() &&
            cleanLandmark != cleanStreet &&
            cleanLandmark != cleanDistrict &&
            cleanLandmark != cleanCity &&
            !cleanStreet.contains(cleanLandmark) &&
            !baseAddrStr.contains(cleanLandmark) &&
            !cleanLandmark.matches(Regex("^[0-9]+$"))

        val finalAddress = if (isLandmarkValid && baseAddrStr.isNotBlank()) {
            "$baseAddrStr · $cleanLandmark"
        } else if (baseAddrStr.isNotBlank()) {
            baseAddrStr
        } else if (cleanLandmark.isNotBlank()) {
            cleanLandmark
        } else {
            listOfNotNull(cleanProvince.ifBlank { null }, cleanCity.ifBlank { null }, cleanDistrict.ifBlank { null }, cleanStreet.ifBlank { null }).joinToString("")
        }

        return normalizeToSimplifiedChinese(finalAddress)
    }

    fun normalizeToSimplifiedChinese(text: String): String {
        if (text.isBlank()) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val simplified = TRADITIONAL_TO_SIMPLIFIED_MAP[ch] ?: ch
            sb.append(simplified)
        }
        return sb.toString()
    }

    private val TRADITIONAL_TO_SIMPLIFIED_MAP = mapOf(
        '縣' to '县', '區' to '区', '號' to '号', '國' to '国', '省' to '省',
        '灣' to '湾', '廣' to '广', '東' to '东', '門' to '门', '樓' to '楼',
        '園' to '园', '橋' to '桥', '鎮' to '镇', '鄉' to '乡', '莊' to '庄',
        '鐵' to '铁', '線' to '线', '站' to '站', '華' to '华', '豐' to '丰',
        '陽' to '阳', '龍' to '龙', '興' to '兴', '嶺' to '岭', '濱' to '滨',
        '崗' to '岗', '嶼' to '屿', '壩' to '坝', '寶' to '宝', '臺' to '台',
        '廈' to '厦', '關' to '关', '貴' to '贵', '陝' to '陕', '雲' to '云',
        '蘇' to '苏', '遼' to '辽', '寧' to '宁', '閩' to '闽', '滬' to '沪',
        '魯' to '鲁', '贛' to '赣', '瓊' to '琼', '處' to '处', '館' to '馆',
        '學' to '学', '院' to '院', '場' to '场', '廳' to '厅', '點' to '点',
        '莊' to '庄', '村' to '村', '路' to '路', '街' to '街',
        '道' to '道', '巷' to '巷', '弄' to '弄', '里' to '里', '島' to '岛',
        '港' to '港', '澳' to '澳', '市' to '市'
    )
}
