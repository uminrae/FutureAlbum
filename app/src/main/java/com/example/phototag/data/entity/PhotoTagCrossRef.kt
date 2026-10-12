package com.example.phototag.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 照片—标签多对多关联表。
 *
 * 一张照片可以有多个标签，一个标签也可以关联到多张照片。
 * 当照片或标签被删除时，对应的关联记录级联删除。
 */
@Entity(
    tableName = "photo_tag_cross_ref",
    primaryKeys = ["photoId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["photoId"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photoId"]),
        Index(value = ["tagId"])
    ]
)
data class PhotoTagCrossRef(
    val photoId: Long,
    val tagId: Long
)
