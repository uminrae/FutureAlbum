package com.example.phototag.data.model

import androidx.compose.runtime.Immutable

/**
 * 手机物理文件夹 / 来源相册数据模型 (MediaStore Bucket)。
 *
 * @param bucketName 文件夹展示名称（如：相机、屏幕截图、微信、下载等）
 * @param count 文件夹内包含的媒体总数
 * @param coverUri 最新一张照片的 content URI（用作封面缩略图）
 * @param photoUris 该文件夹包含的所有照片 content URI 列表
 */
@Immutable
data class DeviceBucket(
    val bucketName: String,
    val count: Int,
    val coverUri: String,
    val photoUris: List<String> = emptyList()
)
