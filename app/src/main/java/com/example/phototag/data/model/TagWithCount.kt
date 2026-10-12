package com.example.phototag.data.model

/**
 * 标签相册卡片数据模型。
 */
data class TagWithCount(
    val tagId: Long,
    val name: String,
    val categoryId: Long,
    val sortOrder: Int,
    val colorHex: String = "#7E9A86",
    val isSystem: Boolean = false,
    val photoCount: Int,
    val coverUri: String?
)
