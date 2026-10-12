package com.example.phototag.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.PhotoTagCrossRef
import com.example.phototag.data.relation.PhotoWithTags
import kotlinx.coroutines.flow.Flow

data class PhotoIdAndUri(
    val photoId: Long,
    val uri: String
)

@Dao
interface PhotoDao {

    // ==================== 照片 CRUD ====================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotos(photos: List<PhotoEntity>): List<Long>

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("UPDATE photos SET customLocation = :customLocation WHERE photoId = :photoId")
    suspend fun updateCustomLocation(photoId: Long, customLocation: String?)

    @Query("UPDATE photos SET customLocation = :customLocation WHERE uri = :uri")
    suspend fun updateCustomLocationByUri(uri: String, customLocation: String?)

    @Query("UPDATE photos SET mimeType = :mimeType, durationMs = :durationMs, width = :width, height = :height WHERE uri = :uri")
    suspend fun updatePhotoMediaMeta(uri: String, mimeType: String, durationMs: Long?, width: Int, height: Int)

    @Query("SELECT * FROM photos WHERE isDeleted = 0 AND (mimeType LIKE 'video/%' OR mimeType = 'image/gif' OR displayName LIKE '%.gif' OR displayName LIKE '%.mp4') AND (width = 0 OR height = 0) LIMIT 100")
    suspend fun getPhotosNeedingMediaMeta(): List<PhotoEntity>

    @Query("UPDATE photos SET address = :address WHERE photoId = :photoId")
    suspend fun updatePhotoAddress(photoId: Long, address: String)

    @Query("UPDATE photos SET address = :address WHERE uri = :uri")
    suspend fun updatePhotoAddressByUri(uri: String, address: String)

    @Query("UPDATE photos SET latitude = :latitude, longitude = :longitude, address = :address, altitude = :altitude WHERE photoId = :photoId")
    suspend fun updatePhotoLocationFull(photoId: Long, latitude: Double?, longitude: Double?, address: String?, altitude: Double?)

    @Query("UPDATE photos SET latitude = :latitude, longitude = :longitude, address = :address WHERE photoId = :photoId")
    suspend fun updatePhotoLocationInfo(photoId: Long, latitude: Double?, longitude: Double?, address: String?)

    @Query("UPDATE photos SET latitude = :latitude, longitude = :longitude, address = :address WHERE uri = :uri")
    suspend fun updatePhotoLocationInfoByUri(uri: String, latitude: Double?, longitude: Double?, address: String?)

    @Query("UPDATE photos SET latitude = :latitude, longitude = :longitude, altitude = :altitude WHERE photoId = :photoId")
    suspend fun updatePhotoCoordinatesAndAltitude(photoId: Long, latitude: Double, longitude: Double, altitude: Double?)

    @Query("UPDATE photos SET latitude = :latitude, longitude = :longitude WHERE photoId = :photoId")
    suspend fun updatePhotoCoordinates(photoId: Long, latitude: Double?, longitude: Double?)

    @Query("UPDATE photos SET altitude = :altitude WHERE photoId = :photoId")
    suspend fun updatePhotoAltitude(photoId: Long, altitude: Double?)

    @Query("SELECT * FROM photos WHERE isDeleted = 0 AND latitude IS NULL LIMIT :limit")
    suspend fun getPhotosNeedingGpsExtraction(limit: Int = 500): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE isDeleted = 0 AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY dateAdded DESC")
    fun getAllPhotosWithCoordinatesFlow(): kotlinx.coroutines.flow.Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isDeleted = 0 AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY dateAdded DESC")
    suspend fun getAllPhotosWithCoordinates(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE isDeleted = 0 AND latitude IS NOT NULL AND longitude IS NOT NULL AND (address IS NULL OR address = '') LIMIT :limit")
    suspend fun getPhotosNeedingReverseGeocode(limit: Int = 200): List<PhotoEntity>

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE photoId IN (:photoIds)")
    suspend fun deletePhotosByIds(photoIds: List<Long>)

    @Query("DELETE FROM photos WHERE uri = :uri")
    suspend fun deletePhotoByUri(uri: String)

    @Query("DELETE FROM photo_tag_cross_ref WHERE photoId IN (:photoIds)")
    suspend fun deleteCrossRefsByPhotoIds(photoIds: List<Long>)

    /**
     * 事务性删除照片及其所有关联关系。
     */
    @Transaction
    suspend fun deletePhotosAndRelations(photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        deleteCrossRefsByPhotoIds(photoIds)
        deletePhotosByIds(photoIds)
    }

    /**
     * 根据死链 URI 事务性清理照片与关联。
     */
    @Transaction
    suspend fun deleteDeadPhotoByUri(uri: String) {
        val photo = getPhotoByUri(uri) ?: return
        deletePhotosAndRelations(listOf(photo.photoId))
    }

    // ==================== 死链与差量比对轻量查询 ====================

    @Query("SELECT photoId, uri FROM photos WHERE isDeleted = 0")
    suspend fun getActivePhotoIdAndUriList(): List<PhotoIdAndUri>

    // ==================== OCR 离线文字内容管理 ====================

    @Query("UPDATE photos SET ocrText = :ocrText WHERE photoId = :photoId")
    suspend fun updatePhotoOcrText(photoId: Long, ocrText: String)

    /** 查询尚未进行 OCR 识别的照片（ocrText 为 NULL 且未移入回收站） */
    @Query("SELECT * FROM photos WHERE ocrText IS NULL AND isDeleted = 0 ORDER BY dateAdded DESC")
    suspend fun getPhotosWithoutOcr(): List<PhotoEntity>

    /** 分批查询未 OCR 的照片 */
    @Query("SELECT * FROM photos WHERE ocrText IS NULL AND isDeleted = 0 ORDER BY dateAdded DESC LIMIT :limit")
    suspend fun getPendingOcrPhotosBatch(limit: Int): List<PhotoEntity>

    // ==================== 照片查询 ====================

    @Query("SELECT * FROM photos WHERE isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    /** 查询未打标照片数量（排除已移入回收站的照片） */
    @Query("""
        SELECT COUNT(p.photoId) FROM photos p
        LEFT JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.photoId IS NULL AND p.isDeleted = 0
    """)
    fun getUntaggedCount(): Flow<Int>

    @Query("""
        SELECT COUNT(p.photoId) FROM photos p
        LEFT JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.photoId IS NULL AND p.isDeleted = 0
    """)
    fun getUntaggedPhotosCountFlow(): Flow<Int>

    /** 查询所有未绑定任何标签的照片（排除回收站） */
    @Query("""
        SELECT p.* FROM photos p
        LEFT JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.photoId IS NULL AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
    """)
    fun getUntaggedPhotos(): Flow<List<PhotoEntity>>

    @Query("""
        SELECT p.* FROM photos p
        LEFT JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.photoId IS NULL AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
    """)
    fun getUntaggedPhotosFlow(): Flow<List<PhotoEntity>>

    /**
     * 高性能轻量分页查询未打标照片（极速整理模式专用，避免 2900+ 数据一次性装载卡死）。
     */
    @Query("""
        SELECT p.* FROM photos p
        LEFT JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.photoId IS NULL AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUntaggedPhotosPaged(limit: Int, offset: Int = 0): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE photoId = :photoId")
    fun getPhotoById(photoId: Long): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE uri = :uri LIMIT 1")
    suspend fun getPhotoByUri(uri: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE uri IN (:uris)")
    suspend fun getPhotosByUris(uris: List<String>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE isDeleted = 0")
    suspend fun getAllPhotosSync(): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos WHERE isDeleted = 0")
    fun getPhotoCount(): Flow<Int>

    // ==================== 照片 + 标签关联查询 ====================

    @Transaction
    @Query("SELECT * FROM photos WHERE isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAllPhotosWithTags(): Flow<List<PhotoWithTags>>

    @Transaction
    @Query("SELECT * FROM photos WHERE photoId = :photoId")
    fun getPhotoWithTags(photoId: Long): Flow<PhotoWithTags?>

    /**
     * 多标签 AND 精确联合筛选（必须同时包含所有指定 tagId 的照片）。
     */
    @Transaction
    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.tagId IN (:tagIds) AND p.isDeleted = 0
        GROUP BY p.photoId
        HAVING COUNT(DISTINCT pt.tagId) = :tagCount
        ORDER BY p.dateAdded DESC
    """)
    fun getPhotosByAllTagIds(tagIds: List<Long>, tagCount: Int): Flow<List<PhotoEntity>>

    /**
     * 多标签 OR 联合筛选（包含任意一个指定 tagId 的照片，去重）。
     */
    @Transaction
    @Query("""
        SELECT DISTINCT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.tagId IN (:tagIds) AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
    """)
    fun getPhotosByAnyTagIds(tagIds: List<Long>): Flow<List<PhotoEntity>>

    /**
     * 单独查询父标签名下的照片（排除已打上该父标签任何子标签的照片）。
     */
    @Transaction
    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.tagId = :parentTagId
          AND p.isDeleted = 0
          AND p.photoId NOT IN (
              SELECT pt2.photoId FROM photo_tag_cross_ref pt2
              WHERE pt2.tagId IN (:childTagIds)
          )
        ORDER BY p.dateAdded DESC
    """)
    fun getPhotosByParentTagExcludingChildren(parentTagId: Long, childTagIds: List<Long>): Flow<List<PhotoEntity>>

    /**
     * 多标签 AND 精确筛选（按标签名）。
     */
    @Transaction
    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        INNER JOIN tags t ON pt.tagId = t.tagId
        WHERE t.name IN (:tagNames) AND p.isDeleted = 0
        GROUP BY p.photoId
        HAVING COUNT(DISTINCT t.tagId) = :tagCount
        ORDER BY p.dateAdded DESC
    """)
    fun getPhotosByAllTags(tagNames: List<String>, tagCount: Int): Flow<List<PhotoEntity>>

    /**
     * 全局复合检索（标签名称匹配 OR 图片内 OCR 提取的文字 OR 文件名）。
     */
    @Transaction
    @Query("""
        SELECT DISTINCT p.* FROM photos p
        LEFT JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        LEFT JOIN tags t ON pt.tagId = t.tagId
        WHERE (t.name LIKE '%' || :query || '%'
           OR p.ocrText LIKE '%' || :query || '%'
           OR p.displayName LIKE '%' || :query || '%'
           OR p.address LIKE '%' || :query || '%'
           OR p.customLocation LIKE '%' || :query || '%')
          AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
    """)
    fun searchPhotosByTextAndTags(query: String): Flow<List<PhotoEntity>>

    /** 按标签名模糊搜索 */
    @Transaction
    @Query("""
        SELECT DISTINCT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        INNER JOIN tags t ON pt.tagId = t.tagId
        WHERE t.name LIKE '%' || :query || '%' AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
    """)
    fun searchPhotosByTagName(query: String): Flow<List<PhotoEntity>>

    /** 获取某标签下的所有照片（含标签详情） */
    @Transaction
    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.tagId = :tagId AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
    """)
    fun getPhotosWithTagsByTagId(tagId: Long): Flow<List<PhotoWithTags>>

    /** 动态根据复杂组合规则查询照片（支持 Room 实时响应式 Flow 观察） */
    @RawQuery(observedEntities = [PhotoEntity::class, PhotoTagCrossRef::class])
    fun getPhotosByRawQuery(query: SupportSQLiteQuery): Flow<List<PhotoEntity>>

    /** 获取某标签下的所有照片（仅 PhotoEntity） */
    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref pt ON p.photoId = pt.photoId
        WHERE pt.tagId = :tagId AND p.isDeleted = 0
        ORDER BY p.dateAdded DESC
    """)
    fun getPhotosByTagId(tagId: Long): Flow<List<PhotoEntity>>

    // ==================== 照片—标签关联管理 ====================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotoTagCrossRef(crossRef: PhotoTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotoTagCrossRefs(crossRefs: List<PhotoTagCrossRef>)

    @Query("""
        INSERT OR IGNORE INTO photo_tag_cross_ref (photoId, tagId)
        SELECT photoId, :tagId FROM photos
        WHERE (mimeType LIKE 'video/%' OR (durationMs IS NOT NULL AND durationMs > 0) OR uri LIKE '%/video/%' OR displayName LIKE '%.mp4' OR displayName LIKE '%.mov' OR displayName LIKE '%.mkv')
          AND isDeleted = 0
    """)
    suspend fun autoTagAllVideos(tagId: Long)

    @Query("""
        INSERT OR IGNORE INTO photo_tag_cross_ref (photoId, tagId)
        SELECT photoId, :tagId FROM photos
        WHERE (mimeType = 'image/gif' OR displayName LIKE '%.gif' OR uri LIKE '%.gif')
          AND isDeleted = 0
    """)
    suspend fun autoTagAllGifs(tagId: Long)

    @Query("""
        INSERT OR IGNORE INTO photo_tag_cross_ref (photoId, tagId)
        SELECT photoId, :tagId FROM photos
        WHERE ((latitude IS NOT NULL AND longitude IS NOT NULL) OR (address IS NOT NULL AND address != '') OR (customLocation IS NOT NULL AND customLocation != ''))
          AND isDeleted = 0
    """)
    suspend fun autoTagAllLocations(tagId: Long)

    @Delete
    suspend fun deletePhotoTagCrossRef(crossRef: PhotoTagCrossRef)

    @Query("SELECT COUNT(*) FROM photo_tag_cross_ref WHERE photoId = :photoId AND tagId = :tagId")
    suspend fun isPhotoTagged(photoId: Long, tagId: Long): Int

    @Query("DELETE FROM photo_tag_cross_ref WHERE photoId = :photoId")
    suspend fun deleteAllTagsForPhoto(photoId: Long)

    @Query("DELETE FROM photo_tag_cross_ref WHERE photoId = :photoId AND tagId IN (:tagIds)")
    suspend fun removeTagsFromPhoto(photoId: Long, tagIds: List<Long>)

    @Transaction
    suspend fun batchTagPhotos(photoIds: List<Long>, tagIds: List<Long>) {
        val crossRefs = photoIds.flatMap { photoId ->
            tagIds.map { tagId -> PhotoTagCrossRef(photoId = photoId, tagId = tagId) }
        }
        insertPhotoTagCrossRefs(crossRefs)
    }

    @Transaction
    suspend fun batchRemoveTagsFromPhotos(photoIds: List<Long>, tagIds: List<Long>) {
        photoIds.forEach { photoId -> removeTagsFromPhoto(photoId, tagIds) }
    }

    // ==================== 原子级标签转移与替换 (Move & Replace) ====================

    /**
     * 原子级标签转移事务方法：
     * 1. 批量向 `photo_tag_cross_ref` 插入关联记录 `PhotoTagCrossRef(photoId, toTagId)` (OnConflictStrategy.IGNORE)。
     * 2. 批量删除 `photo_tag_cross_ref` 中这些 photoUris 与 `fromTagId` 的旧关联记录。
     * 3. 在单个 SQLite 事务中完成。
     * @return 成功转移的照片数量
     */
    @Transaction
    suspend fun transferPhotosToTag(
        photoUris: List<String>,
        fromTagId: Long,
        toTagId: Long
    ): Int {
        if (photoUris.isEmpty() || fromTagId == toTagId) return 0
        val photoIds = getPhotoIdsByUris(photoUris)
        if (photoIds.isEmpty()) return 0

        // 1. 批量向 photo_tag_cross_ref 插入关联记录
        val newCrossRefs = photoIds.map { PhotoTagCrossRef(photoId = it, tagId = toTagId) }
        insertPhotoTagCrossRefs(newCrossRefs)

        // 2. 批量删除旧标签关联
        deleteCrossRefsForPhotosAndTag(photoIds, fromTagId)

        return photoIds.size
    }

    /**
     * 根据 Photo ID 列表进行原子转移与替换
     */
    @Transaction
    suspend fun transferPhotoIdsToTag(photoIds: List<Long>, fromTagId: Long?, toTagId: Long): Int {
        if (photoIds.isEmpty() || fromTagId == toTagId) return 0
        if (fromTagId != null) {
            deleteCrossRefsForPhotosAndTag(photoIds, fromTagId)
        } else {
            deleteCrossRefsForPhotos(photoIds)
        }
        val crossRefs = photoIds.map { PhotoTagCrossRef(photoId = it, tagId = toTagId) }
        insertPhotoTagCrossRefs(crossRefs)
        return photoIds.size
    }

    @Query("SELECT photoId FROM photos WHERE uri IN (:uris)")
    suspend fun getPhotoIdsByUris(uris: List<String>): List<Long>

    @Query("DELETE FROM photo_tag_cross_ref WHERE photoId IN (:photoIds) AND tagId = :tagId")
    suspend fun deleteCrossRefsForPhotosAndTag(photoIds: List<Long>, tagId: Long)

    @Query("DELETE FROM photo_tag_cross_ref WHERE photoId IN (:photoIds)")
    suspend fun deleteCrossRefsForPhotos(photoIds: List<Long>)

    // ==================== 🗑️ 7天安全回收站 (Soft Delete & Trash Bin) ====================

    /** 查询回收站中的所有照片（按删除时间倒序） */
    @Query("SELECT * FROM photos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashPhotos(): Flow<List<PhotoEntity>>

    /** 查询回收站中的照片数量 */
    @Query("SELECT COUNT(*) FROM photos WHERE isDeleted = 1")
    fun getTrashPhotoCount(): Flow<Int>

    /** 查询超期（如超过 7 天）待清理的回收站照片 */
    @Query("SELECT * FROM photos WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt <= :expiryTimestamp")
    suspend fun getExpiredTrashPhotos(expiryTimestamp: Long): List<PhotoEntity>

    /**
     * 软删除：移入回收站（标记 isDeleted = 1，记录 deletedAt 时间戳，完全不触碰底层物理文件）
     */
    @Query("UPDATE photos SET isDeleted = 1, deletedAt = :timestamp WHERE uri IN (:photoUris)")
    suspend fun softDeletePhotosByUris(photoUris: List<String>, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE photos SET isDeleted = 1, deletedAt = :timestamp WHERE photoId IN (:photoIds)")
    suspend fun softDeletePhotosByIds(photoIds: List<Long>, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * 一键恢复：从回收站恢复至正常相册（标记 isDeleted = 0，deletedAt = NULL，保留原标签与时间轴）
     */
    @Query("UPDATE photos SET isDeleted = 0, deletedAt = NULL WHERE uri IN (:photoUris)")
    suspend fun restorePhotosByUris(photoUris: List<String>): Int

    @Query("UPDATE photos SET isDeleted = 0, deletedAt = NULL WHERE photoId IN (:photoIds)")
    suspend fun restorePhotosByIds(photoIds: List<Long>): Int

    /**
     * 物理硬删除：彻底从数据库中抹除记录，并级联清理标签关联
     */
    @Transaction
    suspend fun hardDeletePhotosByUris(photoUris: List<String>): Int {
        val photoIds = getPhotoIdsByUris(photoUris)
        if (photoIds.isEmpty()) return 0
        deleteCrossRefsByPhotoIds(photoIds)
        deletePhotosByIds(photoIds)
        return photoIds.size
    }

    /**
     * 物理级彻底删除：通过 URI 列表永久硬删除记录与标签关联
     */
    @Transaction
    suspend fun deletePermanentlyByUris(photoUris: List<String>): Int {
        return hardDeletePhotosByUris(photoUris)
    }

    /**
     * 物理级彻底删除：通过 ID 列表永久硬删除记录与标签关联
     */
    @Transaction
    suspend fun deletePermanentlyByIds(photoIds: List<Long>): Int {
        if (photoIds.isEmpty()) return 0
        deleteCrossRefsByPhotoIds(photoIds)
        deletePhotosByIds(photoIds)
        return photoIds.size
    }

    @Transaction
    suspend fun clearAllTrash(): Int {
        val trashPhotos = getTrashPhotosDirect()
        if (trashPhotos.isEmpty()) return 0
        val photoIds = trashPhotos.map { it.photoId }
        deleteCrossRefsByPhotoIds(photoIds)
        deletePhotosByIds(photoIds)
        return photoIds.size
    }

    @Query("SELECT * FROM photos WHERE isDeleted = 1")
    suspend fun getTrashPhotosDirect(): List<PhotoEntity>

    @Query("UPDATE photos SET uri = :newUri WHERE uri = :oldUri")
    suspend fun updatePhotoUri(oldUri: String, newUri: String): Int

    @Query("SELECT tagId FROM photo_tag_cross_ref WHERE photoId = :photoId")
    suspend fun getTagIdsForPhoto(photoId: Long): List<Long>

    /**
     * 安全更新或合并照片 URI：
     * 若新 URI 已经被 MediaStore 扫描插入到了数据库（导致两张相同物理文件的重复记录）：
     * 自动将旧记录上的所有标签转移继承给新记录，并删除旧记录，彻底消除重复图片！
     * 若新 URI 尚未存在，则直接更新 URI。
     */
    @Transaction
    suspend fun safeUpdateOrMergePhotoUri(oldUri: String, newUri: String) {
        if (oldUri == newUri) return
        val existingNew = getPhotoByUri(newUri)
        val oldPhoto = getPhotoByUri(oldUri) ?: return

        if (existingNew != null) {
            val oldTagIds = getTagIdsForPhoto(oldPhoto.photoId)
            if (oldTagIds.isNotEmpty()) {
                val refs = oldTagIds.map { PhotoTagCrossRef(photoId = existingNew.photoId, tagId = it) }
                insertPhotoTagCrossRefs(refs)
            }
            if (!oldPhoto.customLocation.isNullOrBlank() && existingNew.customLocation.isNullOrBlank()) {
                updateCustomLocation(existingNew.photoId, oldPhoto.customLocation)
            }
            if (!oldPhoto.address.isNullOrBlank() && existingNew.address.isNullOrBlank()) {
                updatePhotoAddress(existingNew.photoId, oldPhoto.address)
            }
            deletePhotosAndRelations(listOf(oldPhoto.photoId))
        } else {
            updatePhotoUri(oldUri, newUri)
        }
    }

    /**
     * 智能清洗库中相同物理文件导致的重复记录（同 displayName + 同 size + isDeleted = 0）。
     */
    @Transaction
    suspend fun deduplicateDuplicatePhotos() {
        val allActive = getAllPhotosList()
        if (allActive.size <= 1) return
        val duplicates = allActive.groupBy { "${it.displayName}_${it.size}" }.filter { it.value.size > 1 }
        for ((_, group) in duplicates) {
            val primary = group.maxByOrNull { it.photoId } ?: continue
            val secondaryList = group.filter { it.photoId != primary.photoId }
            for (secondary in secondaryList) {
                val tagIds = getTagIdsForPhoto(secondary.photoId)
                if (tagIds.isNotEmpty()) {
                    val refs = tagIds.map { PhotoTagCrossRef(photoId = primary.photoId, tagId = it) }
                    insertPhotoTagCrossRefs(refs)
                }
                if (!secondary.customLocation.isNullOrBlank() && primary.customLocation.isNullOrBlank()) {
                    updateCustomLocation(primary.photoId, secondary.customLocation)
                }
                if (!secondary.address.isNullOrBlank() && primary.address.isNullOrBlank()) {
                    updatePhotoAddress(primary.photoId, secondary.address)
                }
            }
            deletePhotosAndRelations(secondaryList.map { it.photoId })
        }
    }

    // ==================== 🧹 相册健康度巡检与自愈 ====================

    /** 获取所有活跃照片的完整列表（非 Flow，供后台巡检） */
    @Query("SELECT * FROM photos WHERE isDeleted = 0")
    suspend fun getAllPhotosList(): List<PhotoEntity>

    /** 检查悬空/孤儿无效关联记录数量 */
    @Query("""
        SELECT COUNT(*) FROM photo_tag_cross_ref 
        WHERE photoId NOT IN (SELECT photoId FROM photos) 
           OR tagId NOT IN (SELECT tagId FROM tags)
    """)
    suspend fun getDeadCrossRefCount(): Int

    /** 清理所有悬空/孤儿无效关联记录 */
    @Query("""
        DELETE FROM photo_tag_cross_ref 
        WHERE photoId NOT IN (SELECT photoId FROM photos) 
           OR tagId NOT IN (SELECT tagId FROM tags)
    """)
    suspend fun deleteDeadCrossRefs(): Int
}

