package com.example.phototag.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.phototag.data.dao.PhotoDao
import com.example.phototag.data.dao.SmartAlbumDao
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.SmartAlbumEntity
import com.example.phototag.data.model.SmartAlbumRule
import com.example.phototag.data.model.SmartAlbumWithPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 智能动态虚拟相册仓库。
 */
class SmartAlbumRepository(
    private val smartAlbumDao: SmartAlbumDao,
    private val photoDao: PhotoDao
) {

    /** 获取所有智能相册配置列表 Flow */
    val allSmartAlbums: Flow<List<SmartAlbumEntity>> = smartAlbumDao.getAllSmartAlbumsFlow()

    /**
     * 响应式获取所有智能相册及其当前动态匹配到的照片流。
     *
     * 只要底层 Room 数据库新增照片、删除照片或被打上标签，各个相册的内容与封面会自动实时更新。
     */
    fun getSmartAlbumsWithPhotosFlow(): Flow<List<SmartAlbumWithPhotos>> {
        return smartAlbumDao.getAllSmartAlbumsFlow().flatMapLatest { albums ->
            if (albums.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val flows = albums.map { album ->
                    getPhotosByRuleFlow(album.rule).map { photos ->
                        SmartAlbumWithPhotos(album = album, matchedPhotos = photos)
                    }
                }
                combine(flows) { it.toList() }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * 根据规则动态查询匹配照片的 Flow。
     */
    fun getPhotosByRuleFlow(rule: SmartAlbumRule): Flow<List<PhotoEntity>> {
        val query = buildDynamicQuery(rule)
        return photoDao.getPhotosByRawQuery(query)
    }

    /**
     * 构建动态 SupportSQLiteQuery。
     */
    private fun buildDynamicQuery(rule: SmartAlbumRule): SimpleSQLiteQuery {
        val whereClauses = mutableListOf<String>()
        val bindArgs = mutableListOf<Any>()

        // 默认排除软删除/回收站中的照片
        whereClauses.add("p.isDeleted = 0")

        // 1. 包含标签 (includeTagIds)
        if (rule.includeTagIds.isNotEmpty()) {
            if (rule.isAndLogic) {
                // AND 逻辑：必须同时拥有全部指定标签
                val placeholders = rule.includeTagIds.joinToString(",") { "?" }
                whereClauses.add("""
                    p.photoId IN (
                        SELECT photoId FROM photo_tag_cross_ref 
                        WHERE tagId IN ($placeholders) 
                        GROUP BY photoId 
                        HAVING COUNT(DISTINCT tagId) = ?
                    )
                """.trimIndent())
                bindArgs.addAll(rule.includeTagIds)
                bindArgs.add(rule.includeTagIds.size)
            } else {
                // OR 逻辑：包含任意一个指定标签
                val placeholders = rule.includeTagIds.joinToString(",") { "?" }
                whereClauses.add("""
                    p.photoId IN (
                        SELECT photoId FROM photo_tag_cross_ref 
                        WHERE tagId IN ($placeholders)
                    )
                """.trimIndent())
                bindArgs.addAll(rule.includeTagIds)
            }
        }

        // 2. 排除标签 (excludeTagIds)
        if (rule.excludeTagIds.isNotEmpty()) {
            val placeholders = rule.excludeTagIds.joinToString(",") { "?" }
            whereClauses.add("""
                p.photoId NOT IN (
                    SELECT photoId FROM photo_tag_cross_ref 
                    WHERE tagId IN ($placeholders)
                )
            """.trimIndent())
            bindArgs.addAll(rule.excludeTagIds)
        }

        // 3. 年份过滤 (yearFilter)
        if (rule.yearFilter != null) {
            val year = rule.yearFilter
            val cal = Calendar.getInstance()
            cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTsSec = cal.timeInMillis / 1000L
            val startTsMs = cal.timeInMillis

            cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endTsSec = cal.timeInMillis / 1000L
            val endTsMs = cal.timeInMillis

            whereClauses.add("""
                ((p.dateAdded >= ? AND p.dateAdded <= ?) OR (p.dateAdded >= ? AND p.dateAdded <= ?))
            """.trimIndent())
            bindArgs.add(startTsSec)
            bindArgs.add(endTsSec)
            bindArgs.add(startTsMs)
            bindArgs.add(endTsMs)
        }

        // 4. 媒体类型过滤 (mediaTypeFilter)
        when (rule.mediaTypeFilter) {
            "GIF" -> {
                whereClauses.add("(p.mimeType = 'image/gif' OR p.displayName LIKE '%.gif')")
            }
            "VIDEO" -> {
                whereClauses.add("(p.mimeType LIKE 'video/%' OR (p.durationMs IS NOT NULL AND p.durationMs > 0))")
            }
            "IMAGE" -> {
                whereClauses.add("(p.mimeType NOT LIKE 'video/%' AND (p.durationMs IS NULL OR p.durationMs = 0) AND p.mimeType != 'image/gif' AND p.displayName NOT LIKE '%.gif')")
            }
        }

        // 5. 地理位置/地址过滤 (mustHaveLocation)
        if (rule.mustHaveLocation) {
            whereClauses.add("((p.latitude IS NOT NULL AND p.longitude IS NOT NULL) OR (p.address IS NOT NULL AND p.address != '') OR (p.customLocation IS NOT NULL AND p.customLocation != ''))")
        }

        val sql = buildString {
            append("SELECT p.* FROM photos p")
            if (whereClauses.isNotEmpty()) {
                append(" WHERE ")
                append(whereClauses.joinToString(" AND "))
            }
            append(" ORDER BY p.dateAdded DESC")
        }

        return SimpleSQLiteQuery(sql, bindArgs.toTypedArray())
    }

    /**
     * 自动预装系统默认智能相册（如【📍 地点足迹】相册，自动收录全量包含地理位置/地址的照片）。
     */
    suspend fun ensurePreloadedSmartAlbums() = withContext(Dispatchers.IO) {
        try {
            val albums = smartAlbumDao.getAllSmartAlbumsList()
            val hasLocationAlbum = albums.any { it.name.contains("地点") || it.name.contains("足迹") || it.rule.mustHaveLocation }
            if (!hasLocationAlbum) {
                val locationRule = SmartAlbumRule(
                    mustHaveLocation = true
                )
                val entity = SmartAlbumEntity(
                    name = "地点足迹",
                    iconName = "LocationOn",
                    colorHex = "#10B981",
                    ruleJson = locationRule.toJson(),
                    createdAt = System.currentTimeMillis()
                )
                smartAlbumDao.insertSmartAlbum(entity)
            }
        } catch (_: Exception) {}
    }

    suspend fun createSmartAlbum(
        name: String,
        iconName: String,
        colorHex: String,
        rule: SmartAlbumRule
    ): Long = withContext(Dispatchers.IO) {
        val entity = SmartAlbumEntity(
            name = name.trim(),
            iconName = iconName.ifBlank { "Folder" },
            colorHex = colorHex,
            ruleJson = rule.toJson(),
            createdAt = System.currentTimeMillis()
        )
        smartAlbumDao.insertSmartAlbum(entity)
    }

    suspend fun updateSmartAlbum(
        id: Long,
        name: String,
        iconName: String,
        colorHex: String,
        rule: SmartAlbumRule
    ) = withContext(Dispatchers.IO) {
        val existing = smartAlbumDao.getSmartAlbumById(id) ?: return@withContext
        val updated = existing.copy(
            name = name.trim(),
            iconName = iconName.ifBlank { "Folder" },
            colorHex = colorHex,
            ruleJson = rule.toJson()
        )
        smartAlbumDao.updateSmartAlbum(updated)
    }

    suspend fun deleteSmartAlbum(albumId: Long) = withContext(Dispatchers.IO) {
        smartAlbumDao.deleteSmartAlbumById(albumId)
    }
}
