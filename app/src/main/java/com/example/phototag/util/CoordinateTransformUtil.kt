package com.example.phototag.util

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 🌐 坐标系精准转换工具类 (CoordinateTransformUtil)。
 *
 * 支持：
 * 1. WGS-84 (国际地球坐标系，EXIF GPS 默认) -> GCJ-02 (中国火星坐标系，高德地图/腾讯地图/谷歌中国使用)。
 * 2. GCJ-02 -> BD-09 (百度坐标系)。
 * 3. 精确判定坐标是否在中国大陆境内。
 */
object CoordinateTransformUtil {

    private const val A = 6378245.0 // 克拉索夫斯基椭球长半轴
    private const val EE = 0.00669342162296594323 // 椭球的第一偏心率平方
    private const val X_PI = PI * 3000.0 / 180.0

    /**
     * 判断经纬度是否在中国境外（境外无需加偏）。
     */
    fun outOfChina(lat: Double, lng: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    /**
     * WGS-84 (GPS 原生坐标) -> GCJ-02 (高德/腾讯火星坐标)。
     */
    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) {
            return Pair(lat, lng)
        }
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * cos(radLat) * PI)
        val mgLat = lat + dLat
        val mgLng = lng + dLng
        return Pair(mgLat, mgLng)
    }

    /**
     * GCJ-02 (火星坐标) -> BD-09 (百度坐标)。
     */
    fun gcj02ToBd09(lat: Double, lng: Double): Pair<Double, Double> {
        val z = sqrt(lng * lng + lat * lat) + 0.00002 * sin(lat * X_PI)
        val theta = atan2(lat, lng) + 0.000003 * cos(lng * X_PI)
        val bdLng = z * cos(theta) + 0.0065
        val bdLat = z * sin(theta) + 0.006
        return Pair(bdLat, bdLng)
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(Math.abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(Math.abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
