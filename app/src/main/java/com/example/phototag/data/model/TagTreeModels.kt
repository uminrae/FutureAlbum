package com.example.phototag.data.model

import com.example.phototag.data.entity.TagEntity

/**
 * 树形母子标签节点数据模型。
 */
data class TagNode(
    val tag: TagEntity,
    val children: List<TagEntity> = emptyList(),
    val directPhotoCount: Int = 0,
    val totalPhotoCount: Int = 0,
    val coverUri: String? = null
) {
    val isParent: Boolean get() = children.isNotEmpty()
    val hasChildren: Boolean get() = children.isNotEmpty()
    val childCount: Int get() = children.size
}

/**
 * 父标签文件夹相册展示模型（包含子相册数量与汇总照片数）。
 */
data class ParentTagAlbum(
    val parentTag: TagEntity,
    val childTags: List<TagEntity> = emptyList(),
    val directPhotoCount: Int = 0,
    val totalPhotoCount: Int = 0,
    val coverUri: String? = null
) {
    val subAlbumCount: Int get() = childTags.size
}
