package com.example.phototag.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.TagEntity

/**
 * 分类及其包含的所有标签 — Room 关联模型。
 *
 * 通过 [CategoryEntity.categoryId] → [TagEntity.categoryId] 一对多外键关联。
 */
data class CategoryWithTags(
    @Embedded
    val category: CategoryEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "categoryId"
    )
    val tags: List<TagEntity>
)
