package com.example.phototag.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.PhotoTagCrossRef
import com.example.phototag.data.entity.TagEntity

/**
 * 照片及其关联的所有标签 — Room 关联模型。
 *
 * 通过 [PhotoTagCrossRef] 多对多中间表将 [PhotoEntity] 与 [TagEntity] 关联。
 */
data class PhotoWithTags(
    @Embedded
    val photo: PhotoEntity,

    @Relation(
        parentColumn = "photoId",
        entityColumn = "tagId",
        associateBy = Junction(
            value = PhotoTagCrossRef::class,
            parentColumn = "photoId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
