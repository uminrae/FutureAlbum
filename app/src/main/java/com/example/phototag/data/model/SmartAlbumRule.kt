package com.example.phototag.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * 智能动态虚拟相册规则配置模型。
 */
data class SmartAlbumRule(
    /** 必须包含的标签 ID 列表 */
    val includeTagIds: List<Long> = emptyList(),
    /** 标签间关系：true 为“且 (AND，同时包含)”，false 为“或 (OR，包含其一)” */
    val isAndLogic: Boolean = true,
    /** 必须排除的标签 ID 列表（包含任意一个即排除） */
    val excludeTagIds: List<Long> = emptyList(),
    /** 年份过滤（如 2026），null 表示不限年份 */
    val yearFilter: Int? = null,
    /** 媒体类型过滤：null 全部，"IMAGE" 仅照片，"GIF" 仅动图，"VIDEO" 仅视频 */
    val mediaTypeFilter: String? = null,
    /** 是否必须包含 GPS 经纬度/地理信息 */
    val mustHaveLocation: Boolean = false
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("includeTagIds", JSONArray(includeTagIds))
        json.put("isAndLogic", isAndLogic)
        json.put("excludeTagIds", JSONArray(excludeTagIds))
        if (yearFilter != null) json.put("yearFilter", yearFilter)
        if (mediaTypeFilter != null) json.put("mediaTypeFilter", mediaTypeFilter)
        json.put("mustHaveLocation", mustHaveLocation)
        return json.toString()
    }

    /** 格式化生成规则摘要描述，供卡片展示 */
    fun getSummaryDescription(
        tagNamesMap: Map<Long, String> = emptyMap()
    ): String {
        val tokens = mutableListOf<String>()

        if (includeTagIds.isNotEmpty()) {
            val names = includeTagIds.map { tagNamesMap[it] ?: "#$it" }
            val joinOp = if (isAndLogic) " 且 " else " 或 "
            tokens.add("包含 " + names.joinToString(joinOp))
        }

        if (excludeTagIds.isNotEmpty()) {
            val names = excludeTagIds.map { tagNamesMap[it] ?: "#$it" }
            tokens.add("排除 " + names.joinToString("、"))
        }

        if (yearFilter != null) {
            tokens.add("${yearFilter}年")
        }

        when (mediaTypeFilter) {
            "GIF" -> tokens.add("仅动图 GIF")
            "VIDEO" -> tokens.add("仅视频")
            "IMAGE" -> tokens.add("仅静态照片")
        }

        if (mustHaveLocation) {
            tokens.add("含 GPS 地点")
        }

        return if (tokens.isEmpty()) "全部照片 (无限制条件)" else tokens.joinToString(" · ")
    }

    companion object {
        fun fromJson(jsonStr: String?): SmartAlbumRule {
            if (jsonStr.isNullOrBlank()) return SmartAlbumRule()
            return try {
                val json = JSONObject(jsonStr)
                val includeArray = json.optJSONArray("includeTagIds")
                val includeList = mutableListOf<Long>()
                if (includeArray != null) {
                    for (i in 0 until includeArray.length()) {
                        includeList.add(includeArray.getLong(i))
                    }
                }
                val excludeArray = json.optJSONArray("excludeTagIds")
                val excludeList = mutableListOf<Long>()
                if (excludeArray != null) {
                    for (i in 0 until excludeArray.length()) {
                        excludeList.add(excludeArray.getLong(i))
                    }
                }
                SmartAlbumRule(
                    includeTagIds = includeList,
                    isAndLogic = json.optBoolean("isAndLogic", true),
                    excludeTagIds = excludeList,
                    yearFilter = if (json.has("yearFilter")) json.getInt("yearFilter") else null,
                    mediaTypeFilter = json.optString("mediaTypeFilter", "").takeIf { it.isNotBlank() },
                    mustHaveLocation = json.optBoolean("mustHaveLocation", false)
                )
            } catch (_: Exception) {
                SmartAlbumRule()
            }
        }
    }
}

/**
 * 带有实时动态匹配照片统计与封面的智能相册 UI 展示模型。
 */
data class SmartAlbumWithPhotos(
    val album: com.example.phototag.data.entity.SmartAlbumEntity,
    val matchedPhotos: List<com.example.phototag.data.entity.PhotoEntity> = emptyList()
) {
    val photoCount: Int
        get() = matchedPhotos.size

    val coverUri: String?
        get() = matchedPhotos.firstOrNull()?.uri
}
