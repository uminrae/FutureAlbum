package com.example.phototag.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 标签分类实体 — 二级标签体系的一级分类。
 *
 * 例如："梗图表情"、"壁纸"、"摄影"等。
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Long = 0,

    /** 分类名称，如"梗图表情" */
    val name: String,

    /** 排序权重，数值小排前 */
    val sortOrder: Int = 0,

    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis()
)
