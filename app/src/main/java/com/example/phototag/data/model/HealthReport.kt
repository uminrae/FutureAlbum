package com.example.phototag.data.model

import com.example.phototag.data.entity.TagEntity

/**
 * 相似/高度重复标签对候选模型。
 */
data class DuplicateTagPair(
    val sourceTag: TagEntity,
    val targetTag: TagEntity,
    val similarityScore: Float, // 0.0 ~ 1.0 相似度得分
    val matchReason: String,    // 匹配原因（例如："名称前缀包含关系"、"编辑距离极近"、"拼音完全相同"）
    val sourcePhotoCount: Int = 0,
    val targetPhotoCount: Int = 0
)

/**
 * 相册数据库健康度巡检诊断报告。
 */
data class HealthReport(
    val orphanTags: List<TagEntity> = emptyList(),             // 0 照片引用的幽灵标签
    val duplicatePairs: List<DuplicateTagPair> = emptyList(),  // 相似/重复命名标签对
    val deadCrossRefCount: Int = 0,                           // 悬空/无效脏关联索引数
    val nonExistentPhotoCount: Int = 0,                       // 数据库记录存在但本地文件已丢失的照片数
    val totalCheckedTagsCount: Int = 0,                       // 本次巡检标签总数
    val totalCheckedPhotosCount: Int = 0,                     // 本次巡检照片总数
    val healthScore: Int = 100,                               // 健康得分（0 ~ 100）
    val checkTimestamp: Long = System.currentTimeMillis()
) {
    /** 待处理的优化项总数 */
    val totalIssueCount: Int
        get() = orphanTags.size + duplicatePairs.size + (if (deadCrossRefCount > 0) 1 else 0) + (if (nonExistentPhotoCount > 0) 1 else 0)

    /** 数据库是否完全健康 */
    val isHealthy: Boolean
        get() = totalIssueCount == 0

    /** 总体健康度摘要说明 */
    val summaryText: String
        get() = when {
            isHealthy -> "相册数据库健康度极佳，未发现冗余或失效数据"
            totalIssueCount <= 3 -> "发现 $totalIssueCount 处可优化项，建议执行自愈优化"
            else -> "发现 $totalIssueCount 处冗余数据与相似冲突，建议一键清理"
        }
}
