package com.example.phototag.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 标签实体 — 支持【母子层级】、【相册置顶 (Pin)】与【自定义排序 (SortOrder)】。
 */
@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["parentId"]),
        Index(value = ["name"], unique = true),
        Index(value = ["isPinned"]),
        Index(value = ["sortOrder"]),
        Index(value = ["isArchived"]),
        Index(value = ["lastUsedTime"]),
        Index(value = ["useCount"])
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val tagId: Long = 0,

    /** 标签名称，如 "#偶像"、"#自拍"、"#风景" */
    val name: String,

    /** 所属分类 ID（外键） */
    val categoryId: Long,

    /** 所属父标签 ID（外键，指向父级 TagEntity 的 tagId，为 null 时表示顶级父标签） */
    val parentId: Long? = null,

    /** 是否置顶（📌 自由置顶显示在最前） */
    val isPinned: Boolean = false,

    /** 自定义排序权重（拖拽重排时动态更新） */
    val sortOrder: Int = 0,

    /** 是否已归档（📦 归档相册进入收纳盒，不占用主相册流） */
    val isArchived: Boolean = false,

    /** 自定义莫兰迪配色 HEX 字符串，例如 "#7E9A86" */
    val colorHex: String = "#7E9A86",

    /** 是否为系统根据 EXIF/元数据自动提取的智能标签 */
    val isSystem: Boolean = false,

    /** 累计被打标使用的次数 */
    val useCount: Int = 0,

    /** 最后一次被使用/打标的时间戳（毫秒） */
    val lastUsedTime: Long = 0L,

    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 是否为顶级根标签 */
    val isRoot: Boolean
        get() = parentId == null
}
