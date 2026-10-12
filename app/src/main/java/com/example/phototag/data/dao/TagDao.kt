package com.example.phototag.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.PhotoTagCrossRef
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.TagWithCount
import com.example.phototag.data.relation.CategoryWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // ==================== 分类 CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE categoryId = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("SELECT DISTINCT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    fun getCategoryById(categoryId: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    fun getCategoryCount(): Flow<Int>

    // ==================== 标签 CRUD ====================

    /** 插入标签；唯一名称冲突则忽略 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<TagEntity>): List<Long>

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("UPDATE tags SET name = :name, colorHex = :colorHex WHERE tagId = :tagId")
    suspend fun updateTagNameAndColor(tagId: Long, name: String, colorHex: String)

    @Query("UPDATE tags SET name = :name, colorHex = :colorHex, categoryId = :categoryId WHERE tagId = :tagId")
    suspend fun updateTagInfo(tagId: Long, name: String, colorHex: String, categoryId: Long)

    @Query("UPDATE tags SET name = :name, colorHex = :colorHex, categoryId = :categoryId, parentId = :parentId WHERE tagId = :tagId")
    suspend fun updateTagFull(tagId: Long, name: String, colorHex: String, categoryId: Long, parentId: Long?)

    @Query("UPDATE tags SET parentId = :parentId WHERE tagId = :tagId")
    suspend fun updateTagParent(tagId: Long, parentId: Long?)

    /** 📌 置顶 / 取消置顶标签相册 */
    @Query("UPDATE tags SET isPinned = :isPinned WHERE tagId = :tagId")
    suspend fun setTagPinned(tagId: Long, isPinned: Boolean)

    /** 📦 归档 / 取消归档标签相册 */
    @Query("UPDATE tags SET isArchived = :isArchived WHERE tagId = :tagId")
    suspend fun setTagArchived(tagId: Long, isArchived: Boolean)

    /** 拖拽排序：更新单项 sortOrder */
    @Query("UPDATE tags SET sortOrder = :sortOrder WHERE tagId = :tagId")
    suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int)

    /** 批量更新标签自定义排序权重 */
    @Transaction
    suspend fun updateTagOrders(tagIdOrderList: List<Pair<Long, Int>>) {
        for ((tagId, order) in tagIdOrderList) {
            updateTagSortOrder(tagId, order)
        }
    }

    /** 打标计数累加与最后使用时间戳更新 */
    @Query("UPDATE tags SET useCount = useCount + 1, lastUsedTime = :timestamp WHERE tagId = :tagId")
    suspend fun incrementTagUsage(tagId: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE tagId = :tagId")
    suspend fun deleteTagById(tagId: Long)

    @Query("DELETE FROM photo_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteCrossRefsByTagId(tagId: Long)

    @Query("SELECT photoId FROM photo_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getPhotoIdsForTag(tagId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<PhotoTagCrossRef>)

    /**
     * 事务性合并两个标签：将 sourceTagId 绑定的所有图片迁移至 targetTagId，然后安全删除 sourceTagId。
     */
    @Transaction
    suspend fun mergeTags(sourceTagId: Long, targetTagId: Long) {
        if (sourceTagId == targetTagId) return
        val photoIds = getPhotoIdsForTag(sourceTagId)
        if (photoIds.isNotEmpty()) {
            val newCrossRefs = photoIds.map { PhotoTagCrossRef(photoId = it, tagId = targetTagId) }
            insertCrossRefs(newCrossRefs)
        }
        deleteCrossRefsByTagId(sourceTagId)
        deleteTagById(sourceTagId)
    }

    // ==================== 层级与置顶排序标签查询 ====================

    /** 查询所有活跃顶级父标签（parentId IS NULL 且 isArchived = 0，置顶优先，其次按自定义排序） */
    @Query("SELECT DISTINCT * FROM tags WHERE parentId IS NULL AND isArchived = 0 ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getRootTagsFlow(): Flow<List<TagEntity>>

    /** 查询所有已归档的顶级父标签（parentId IS NULL 且 isArchived = 1） */
    @Query("SELECT DISTINCT * FROM tags WHERE parentId IS NULL AND isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedRootTagsFlow(): Flow<List<TagEntity>>

    /** 查询所有已归档的标签 */
    @Query("SELECT DISTINCT * FROM tags WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedTagsFlow(): Flow<List<TagEntity>>

    /** 查询指定父标签下的所有子标签 */
    @Query("SELECT DISTINCT * FROM tags WHERE parentId = :parentId ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getChildTagsFlow(parentId: Long): Flow<List<TagEntity>>

    /** 查询所有子标签（parentId IS NOT NULL） */
    @Query("SELECT DISTINCT * FROM tags WHERE parentId IS NOT NULL ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getAllChildTagsFlow(): Flow<List<TagEntity>>

    /** 查询指定父标签下的子标签 ID 集合 */
    @Query("SELECT tagId FROM tags WHERE parentId = :parentId")
    suspend fun getChildTagIds(parentId: Long): List<Long>

    /** DISTINCT 查出所有活跃唯一标签（isArchived = 0，置顶优先） */
    @Query("SELECT DISTINCT * FROM tags WHERE isArchived = 0 ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getAllTags(): Flow<List<TagEntity>>

    /** 查询所有标签（含已归档标签） */
    @Query("SELECT DISTINCT * FROM tags ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getAllTagsIncludingArchived(): Flow<List<TagEntity>>

    /** 查询最近使用的标签（按 lastUsedTime 降序） */
    @Query("SELECT DISTINCT * FROM tags WHERE lastUsedTime > 0 AND isArchived = 0 ORDER BY lastUsedTime DESC LIMIT :limit")
    fun getRecentTags(limit: Int = 6): Flow<List<TagEntity>>

    /** 查询最高频常用标签（按 useCount 降序） */
    @Query("SELECT DISTINCT * FROM tags WHERE useCount > 0 AND isArchived = 0 ORDER BY useCount DESC, lastUsedTime DESC LIMIT :limit")
    fun getTopUsedTags(limit: Int = 8): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE tagId = :tagId")
    fun getTagById(tagId: Long): Flow<TagEntity?>

    @Query("SELECT * FROM tags WHERE tagId = :tagId LIMIT 1")
    suspend fun getTagByIdSync(tagId: Long): TagEntity?

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsList(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT DISTINCT * FROM tags WHERE categoryId = :categoryId AND isArchived = 0 ORDER BY isPinned DESC, sortOrder ASC, name ASC")
    fun getTagsByCategory(categoryId: Long): Flow<List<TagEntity>>

    @Query("SELECT DISTINCT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY isPinned DESC, sortOrder ASC, name ASC")
    fun searchTags(query: String): Flow<List<TagEntity>>

    @Query("SELECT COUNT(DISTINCT tagId) FROM tags WHERE isArchived = 0")
    fun getTagCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT tagId) FROM tags WHERE isArchived = 1")
    fun getArchivedTagCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT tagId) FROM tags WHERE categoryId = :categoryId AND isArchived = 0")
    fun getTagCountByCategory(categoryId: Long): Flow<Int>

    // ==================== 关联查询 ====================

    @Transaction
    @Query("SELECT DISTINCT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategoriesWithTags(): Flow<List<CategoryWithTags>>

    @Transaction
    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    fun getCategoryWithTags(categoryId: Long): Flow<CategoryWithTags?>

    /**
     * 标签相册：查询每个活跃标签关联的照片数量、自定义色彩、置顶状态与最新封面图 URI。
     */
    @Query("""
        SELECT 
            t.tagId,
            t.name,
            t.categoryId,
            t.sortOrder,
            t.colorHex,
            t.isSystem,
            (SELECT COUNT(DISTINCT r.photoId) 
             FROM photo_tag_cross_ref r 
             INNER JOIN photos p ON r.photoId = p.photoId 
             WHERE r.tagId = t.tagId AND p.isDeleted = 0
            ) AS photoCount,
            (SELECT p.uri 
             FROM photos p 
             INNER JOIN photo_tag_cross_ref r2 ON p.photoId = r2.photoId 
             WHERE r2.tagId = t.tagId AND p.isDeleted = 0
             ORDER BY p.dateAdded DESC 
             LIMIT 1
            ) AS coverUri
        FROM tags t
        WHERE t.isArchived = 0
        ORDER BY t.isPinned DESC, t.sortOrder ASC, t.createdAt DESC
    """)
    fun getTagsWithCount(): Flow<List<TagWithCount>>

    /**
     * 归档相册：查询所有已归档标签关联的照片数量与最新封面。
     */
    @Query("""
        SELECT 
            t.tagId,
            t.name,
            t.categoryId,
            t.sortOrder,
            t.colorHex,
            t.isSystem,
            (SELECT COUNT(DISTINCT r.photoId) 
             FROM photo_tag_cross_ref r 
             INNER JOIN photos p ON r.photoId = p.photoId 
             WHERE r.tagId = t.tagId AND p.isDeleted = 0
            ) AS photoCount,
            (SELECT p.uri 
             FROM photos p 
             INNER JOIN photo_tag_cross_ref r2 ON p.photoId = r2.photoId 
             WHERE r2.tagId = t.tagId AND p.isDeleted = 0
             ORDER BY p.dateAdded DESC 
             LIMIT 1
            ) AS coverUri
        FROM tags t
        WHERE t.isArchived = 1
        ORDER BY t.createdAt DESC
    """)
    fun getArchivedTagsWithCount(): Flow<List<TagWithCount>>

    // ==================== 🧹 相册健康度巡检与自愈 ====================

    /**
     * 查询所有 0 照片关联引用的幽灵标签（Orphan Tags）。
     */
    @Query("""
        SELECT * FROM tags 
        WHERE tagId NOT IN (SELECT DISTINCT tagId FROM photo_tag_cross_ref)
        ORDER BY isPinned DESC, createdAt DESC
    """)
    suspend fun getOrphanTags(): List<TagEntity>

    /**
     * 批量安全删除指定标签 ID 集合。
     */
    @Query("DELETE FROM tags WHERE tagId IN (:tagIds)")
    suspend fun deleteTagsByIds(tagIds: List<Long>): Int

    /**
     * 清理 tagId 不在 tags 表中的脏关联记录。
     */
    @Query("DELETE FROM photo_tag_cross_ref WHERE tagId NOT IN (SELECT tagId FROM tags)")
    suspend fun deleteOrphanTagCrossRefs(): Int

    // ==================== 辅助方法 ====================

    /**
     * 创建分类并同时批量插入属于该分类的标签（附带莫兰迪配色与系统标记）。
     */
    @Transaction
    suspend fun insertCategoryWithTags(
        category: CategoryEntity,
        tagData: List<Triple<String, String, Boolean>>
    ) {
        val categoryId = insertCategory(category)
        val tags = tagData.mapIndexed { index, (name, colorHex, isSystem) ->
            TagEntity(
                name = name,
                categoryId = categoryId,
                sortOrder = index,
                colorHex = colorHex,
                isSystem = isSystem
            )
        }
        insertTags(tags)
    }
}
