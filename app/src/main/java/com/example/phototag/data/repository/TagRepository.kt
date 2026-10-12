package com.example.phototag.data.repository

import com.example.phototag.data.dao.TagDao
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.TagWithCount
import com.example.phototag.data.relation.CategoryWithTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TagRepository(private val tagDao: TagDao) {

    // ==================== 分类 ====================

    fun getAllCategories(): Flow<List<CategoryEntity>> = tagDao.getAllCategories()
    fun getCategoryById(categoryId: Long): Flow<CategoryEntity?> = tagDao.getCategoryById(categoryId)
    fun getAllCategoriesWithTags(): Flow<List<CategoryWithTags>> = tagDao.getAllCategoriesWithTags()
    fun getCategoryWithTags(categoryId: Long): Flow<CategoryWithTags?> = tagDao.getCategoryWithTags(categoryId)
    fun getCategoryCount(): Flow<Int> = tagDao.getCategoryCount()

    suspend fun getCategoryByName(name: String): CategoryEntity? = withContext(Dispatchers.IO) { tagDao.getCategoryByName(name) }
    suspend fun insertCategory(category: CategoryEntity): Long = tagDao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = tagDao.updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = tagDao.deleteCategory(category)
    suspend fun deleteCategoryById(categoryId: Long) = tagDao.deleteCategoryById(categoryId)
    suspend fun deleteCategoryWithTags(category: CategoryEntity) = tagDao.deleteCategory(category)
    suspend fun deleteCategoryWithTags(categoryId: Long) = tagDao.deleteCategoryById(categoryId)

    // ==================== 标签 ====================

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()
    fun getRootTagsFlow(): Flow<List<TagEntity>> = tagDao.getRootTagsFlow()
    fun getChildTagsFlow(parentId: Long): Flow<List<TagEntity>> = tagDao.getChildTagsFlow(parentId)
    fun getAllChildTagsFlow(): Flow<List<TagEntity>> = tagDao.getAllChildTagsFlow()
    suspend fun getChildTagIds(parentId: Long): List<Long> = withContext(Dispatchers.IO) { tagDao.getChildTagIds(parentId) }

    fun getRecentTags(limit: Int = 6): Flow<List<TagEntity>> = tagDao.getRecentTags(limit)
    fun getTopUsedTags(limit: Int = 8): Flow<List<TagEntity>> = tagDao.getTopUsedTags(limit)
    fun getTagById(tagId: Long): Flow<TagEntity?> = tagDao.getTagById(tagId)
    fun getTagsByCategory(categoryId: Long): Flow<List<TagEntity>> = tagDao.getTagsByCategory(categoryId)
    fun searchTags(query: String): Flow<List<TagEntity>> = tagDao.searchTags(query)
    fun getTagCount(): Flow<Int> = tagDao.getTagCount()
    fun getTagsWithCount(): Flow<List<TagWithCount>> = tagDao.getTagsWithCount()
    fun getTagsWithPhotoCount(): Flow<List<TagWithCount>> = tagDao.getTagsWithCount()

    suspend fun insertTag(tag: TagEntity): Long = tagDao.insertTag(tag)
    suspend fun getTagByName(name: String): TagEntity? = withContext(Dispatchers.IO) { tagDao.getTagByName(name) }

    suspend fun getOrCreateTag(
        name: String,
        categoryName: String = "智能属性 (EXIF)",
        colorHex: String = "#7E9A86"
    ): TagEntity = withContext(Dispatchers.IO) {
        val formatted = if (name.startsWith("#")) name.trim() else "#${name.trim()}"
        val existing = tagDao.getTagByName(formatted)
        if (existing != null) return@withContext existing

        var category = tagDao.getCategoryByName(categoryName)
        if (category == null) {
            val catId = tagDao.insertCategory(CategoryEntity(name = categoryName, sortOrder = 10))
            category = CategoryEntity(categoryId = catId, name = categoryName, sortOrder = 10)
        }

        val newTagId = tagDao.insertTag(
            TagEntity(
                name = formatted,
                categoryId = category.categoryId,
                colorHex = colorHex,
                isSystem = false
            )
        )
        tagDao.getTagByName(formatted) ?: TagEntity(
            tagId = newTagId,
            name = formatted,
            categoryId = category.categoryId,
            colorHex = colorHex,
            isSystem = false
        )
    }

    suspend fun insertTags(tags: List<TagEntity>): List<Long> = tagDao.insertTags(tags)
    suspend fun updateTag(tag: TagEntity) = tagDao.updateTag(tag)
    suspend fun updateTagNameAndColor(tagId: Long, name: String, colorHex: String) =
        tagDao.updateTagNameAndColor(tagId, name, colorHex)
    suspend fun updateTagInfo(tagId: Long, name: String, colorHex: String, categoryId: Long) =
        tagDao.updateTagInfo(tagId, name, colorHex, categoryId)
    suspend fun updateTagFull(tagId: Long, name: String, colorHex: String, categoryId: Long, parentId: Long?) =
        tagDao.updateTagFull(tagId, name, colorHex, categoryId, parentId)
    suspend fun updateTagParent(tagId: Long, parentId: Long?) = tagDao.updateTagParent(tagId, parentId)
    suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)
    suspend fun deleteTagById(tagId: Long) = tagDao.deleteTagById(tagId)
    suspend fun mergeTags(sourceTagId: Long, targetTagId: Long) = tagDao.mergeTags(sourceTagId, targetTagId)

    suspend fun setTagPinned(tagId: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        tagDao.setTagPinned(tagId, isPinned)
    }

    suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int) = withContext(Dispatchers.IO) {
        tagDao.updateTagSortOrder(tagId, sortOrder)
    }

    suspend fun updateTagOrders(orderedTagIds: List<Long>) = withContext(Dispatchers.IO) {
        val pairs = orderedTagIds.mapIndexed { index, tagId -> tagId to index }
        tagDao.updateTagOrders(pairs)
    }

    suspend fun incrementTagUsage(tagId: Long, timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        tagDao.incrementTagUsage(tagId, timestamp)
    }

    // ==================== 📦 归档相册 ====================

    fun getArchivedTagsFlow(): Flow<List<TagEntity>> = tagDao.getArchivedTagsFlow()
    fun getArchivedRootTagsFlow(): Flow<List<TagEntity>> = tagDao.getArchivedRootTagsFlow()
    fun getArchivedTagsWithCount(): Flow<List<TagWithCount>> = tagDao.getArchivedTagsWithCount()
    fun getArchivedTagCount(): Flow<Int> = tagDao.getArchivedTagCount()
    fun getAllTagsIncludingArchived(): Flow<List<TagEntity>> = tagDao.getAllTagsIncludingArchived()

    suspend fun setTagArchived(tagId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        tagDao.setTagArchived(tagId, isArchived)
    }

    suspend fun getOrCreateCategory(categoryName: String): CategoryEntity = withContext(Dispatchers.IO) {
        val clean = categoryName.trim()
        val existing = tagDao.getCategoryByName(clean)
        if (existing != null) return@withContext existing
        val catId = tagDao.insertCategory(CategoryEntity(name = clean, sortOrder = 10))
        CategoryEntity(categoryId = catId, name = clean, sortOrder = 10)
    }

    suspend fun insertCategoryWithTags(
        category: CategoryEntity,
        tagData: List<Triple<String, String, Boolean>>
    ) = tagDao.insertCategoryWithTags(category, tagData)

    // ==================== 🧹 健康巡检与自愈 ====================

    suspend fun getOrphanTags(): List<TagEntity> = withContext(Dispatchers.IO) {
        tagDao.getOrphanTags()
    }

    suspend fun deleteTagsByIds(tagIds: List<Long>): Int = withContext(Dispatchers.IO) {
        tagDao.deleteTagsByIds(tagIds)
    }

    suspend fun deleteOrphanTagCrossRefs(): Int = withContext(Dispatchers.IO) {
        tagDao.deleteOrphanTagCrossRefs()
    }
}
