package com.example.phototag.data.preference

import android.content.Context
import android.content.SharedPreferences

/**
 * 用户本地偏好设置管理（持久化网格列数等配置）。
 */
class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("photo_tag_user_preferences", Context.MODE_PRIVATE)

    /**
     * 相册网格列数（2 ~ 5 列，默认 3 列）。
     */
    var gridColumns: Int
        get() = prefs.getInt(KEY_GRID_COLUMNS, 3).coerceIn(2, 5)
        set(value) = prefs.edit().putInt(KEY_GRID_COLUMNS, value.coerceIn(2, 5)).apply()

    /**
     * 获取指定标签相册的自定义封面 URI
     */
    fun getCustomAlbumCover(tagId: Long): String? {
        return prefs.getString(PREFIX_CUSTOM_COVER + tagId, null)
    }

    /**
     * 设置或清除指定标签相册的自定义封面 URI
     */
    fun setCustomAlbumCover(tagId: Long, uri: String?) {
        if (uri == null) {
            prefs.edit().remove(PREFIX_CUSTOM_COVER + tagId).apply()
        } else {
            prefs.edit().putString(PREFIX_CUSTOM_COVER + tagId, uri).apply()
        }
    }

    /**
     * 获取所有已设置自定义封面的映射 (tagId -> uri)
     */
    fun getAllCustomAlbumCovers(): Map<Long, String> {
        val all = prefs.all
        val map = mutableMapOf<Long, String>()
        for ((key, value) in all) {
            if (key.startsWith(PREFIX_CUSTOM_COVER) && value is String) {
                val tagIdStr = key.removePrefix(PREFIX_CUSTOM_COVER)
                tagIdStr.toLongOrNull()?.let { tagId ->
                    map[tagId] = value
                }
            }
        }
        return map
    }

    companion object {
        private const val KEY_GRID_COLUMNS = "key_grid_columns"
        private const val PREFIX_CUSTOM_COVER = "custom_cover_tag_"
    }
}

