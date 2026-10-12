package com.example.phototag.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

/**
 * 🗺️ 地图应用一键联动与精准定位导航助手 (MapNavigationHelper)。
 *
 * 优先级：
 * 1. 国内优先调用【高德地图】（以 GCJ-02 火星坐标精准标注建筑与 POI 场所）。
 * 2. 次选【百度地图】、【腾讯地图】、【谷歌地图】。
 * 3. 若手机未安装任何独立地图 App，自动调起高德 Web 浏览器高精卫星/矢量地图标注页。
 */
object MapNavigationHelper {

    /**
     * 一键在地图应用或高德 Web 中精准查看照片拍摄地点。
     *
     * @param context 上下文
     * @param wgsLat WGS-84 原始拍摄纬度
     * @param wgsLng WGS-84 原始拍摄经度
     * @param locationTitle 地点或建筑场所名称（如 "广西大学 (汇学堂)"）
     */
    fun openLocationInMap(
        context: Context,
        wgsLat: Double,
        wgsLng: Double,
        locationTitle: String? = null
    ) {
        val title = locationTitle?.trim()?.takeIf { it.isNotBlank() } ?: "照片拍摄地"

        // 1. 坐标转换：WGS-84 -> GCJ-02 (高德/腾讯) & BD-09 (百度)
        val (gcjLat, gcjLng) = CoordinateTransformUtil.wgs84ToGcj02(wgsLat, wgsLng)
        val (bdLat, bdLng) = CoordinateTransformUtil.gcj02ToBd09(gcjLat, gcjLng)

        val pm = context.packageManager

        // ==================== 1. 尝试唤起【高德地图 App】 ====================
        if (isPackageInstalled(pm, "com.autonavi.minimap")) {
            try {
                val amapUri = Uri.parse(
                    "androidamap://viewMap?sourceApplication=FutureAlbum&poiname=${Uri.encode(title)}&lat=$gcjLat&lon=$gcjLng&dev=0"
                )
                val amapIntent = Intent(Intent.ACTION_VIEW, amapUri).apply {
                    setPackage("com.autonavi.minimap")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(amapIntent)
                return
            } catch (_: Exception) {}
        }

        // ==================== 2. 尝试唤起【百度地图 App】 ====================
        if (isPackageInstalled(pm, "com.baidu.BaiduMap")) {
            try {
                val baiduUri = Uri.parse(
                    "baidumap://map/marker?location=$bdLat,$bdLng&title=${Uri.encode(title)}&content=${Uri.encode(title)}&coord_type=bd09ll&src=andr.futurealbum"
                )
                val baiduIntent = Intent(Intent.ACTION_VIEW, baiduUri).apply {
                    setPackage("com.baidu.BaiduMap")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(baiduIntent)
                return
            } catch (_: Exception) {}
        }

        // ==================== 3. 尝试唤起【腾讯地图 App】 ====================
        if (isPackageInstalled(pm, "com.tencent.map")) {
            try {
                val tencentUri = Uri.parse(
                    "qqmap://map/routeplan?type=drive&to=${Uri.encode(title)}&tocoord=$gcjLat,$gcjLng&referer=FutureAlbum"
                )
                val tencentIntent = Intent(Intent.ACTION_VIEW, tencentUri).apply {
                    setPackage("com.tencent.map")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(tencentIntent)
                return
            } catch (_: Exception) {}
        }

        // ==================== 4. 尝试唤起【谷歌地图 App】 ====================
        if (isPackageInstalled(pm, "com.google.android.apps.maps")) {
            try {
                val googleUri = Uri.parse("geo:$wgsLat,$wgsLng?q=$wgsLat,$wgsLng(${Uri.encode(title)})")
                val googleIntent = Intent(Intent.ACTION_VIEW, googleUri).apply {
                    setPackage("com.google.android.apps.maps")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(googleIntent)
                return
            } catch (_: Exception) {}
        }

        // ==================== 5. 系统通用 Geo Intent 广播选择器 ====================
        try {
            val geoUri = Uri.parse("geo:$gcjLat,$gcjLng?q=$gcjLat,$gcjLng(${Uri.encode(title)})")
            val generalIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (generalIntent.resolveActivity(pm) != null) {
                context.startActivity(Intent.createChooser(generalIntent, "选择地图查看位置"))
                return
            }
        } catch (_: Exception) {}

        // ==================== 6. 兜底：唤起高德地图 Web 浏览器高精标注页 ====================
        try {
            val webAmapUrl = "https://uri.amap.com/marker?position=$gcjLng,$gcjLat&name=${Uri.encode(title)}&coordinate=gaode&callnative=1"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webAmapUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开地图：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
