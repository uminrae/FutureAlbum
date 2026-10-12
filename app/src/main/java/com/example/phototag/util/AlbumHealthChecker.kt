package com.example.phototag.util

import android.content.Context
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.DuplicateTagPair
import com.example.phototag.data.model.HealthReport
import com.example.phototag.data.repository.PhotoRepository
import com.example.phototag.data.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/**
 * 🧹 相册健康度巡检与自愈分析引擎 (AlbumHealthChecker)。
 *
 * 在后台协程 (Dispatchers.IO) 中全量扫描 Room 数据库与媒体文件，识别：
 * 1. 0 照片幽灵标签 (Orphan Tags)
 * 2. 相似/重复命名标签对 (Near-Duplicate Tags)
 * 3. 悬空/无效脏关联记录 (Dead References)
 * 4. 数据库中存在但本地已删除的照片文件
 */
class AlbumHealthChecker(
    private val tagRepository: TagRepository,
    private val photoRepository: PhotoRepository,
    private val context: Context
) {

    /**
     * 执行全量相册数据库健康度巡检并生成诊断报告。
     */
    suspend fun performHealthCheck(): HealthReport = withContext(Dispatchers.IO) {
        // 1. 查询所有 0 照片引用的幽灵标签
        val orphanTags = tagRepository.getOrphanTags()

        // 2. 查询所有标签与照片统计，供相似度分析与排序权重对比
        val allTags = tagRepository.getAllTagsIncludingArchived().first()
        val tagsWithCount = tagRepository.getTagsWithPhotoCount().first().associateBy { it.tagId }
        val allPhotos = photoRepository.getAllPhotosList()

        // 3. 相似/重复命名标签分析 (Levenshtein Distance + 前缀/后缀包含 + 拼音同音)
        val duplicatePairs = analyzeNearDuplicateTags(allTags, tagsWithCount)

        // 4. 查询悬空/无效关联索引数
        val deadCrossRefCount = photoRepository.getDeadCrossRefCount()

        // 5. 快速抽样/遍历检查照片本地文件是否存在
        var nonExistentPhotos = 0
        for (photo in allPhotos) {
            val uriStr = photo.uri
            if (uriStr.startsWith("/")) {
                val file = File(uriStr)
                if (!file.exists()) {
                    nonExistentPhotos++
                }
            } else if (uriStr.startsWith("file://")) {
                val file = File(uriStr.removePrefix("file://"))
                if (!file.exists()) {
                    nonExistentPhotos++
                }
            }
        }

        // 6. 计算综合健康度评分 (满分 100)
        var score = 100
        score -= (orphanTags.size * 3).coerceAtMost(30)
        score -= (duplicatePairs.size * 6).coerceAtMost(30)
        if (deadCrossRefCount > 0) {
            score -= (10 + (deadCrossRefCount * 2)).coerceAtMost(25)
        }
        score -= (nonExistentPhotos * 2).coerceAtMost(15)
        val finalScore = score.coerceIn(0, 100)

        HealthReport(
            orphanTags = orphanTags,
            duplicatePairs = duplicatePairs,
            deadCrossRefCount = deadCrossRefCount,
            nonExistentPhotoCount = nonExistentPhotos,
            totalCheckedTagsCount = allTags.size,
            totalCheckedPhotosCount = allPhotos.size,
            healthScore = finalScore,
            checkTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * 分析所有标签对，找出相似度极高的候选组。
     */
    private fun analyzeNearDuplicateTags(
        tags: List<TagEntity>,
        tagsWithCount: Map<Long, com.example.phototag.data.model.TagWithCount>
    ): List<DuplicateTagPair> {
        if (tags.size < 2) return emptyList()

        val pairs = mutableListOf<DuplicateTagPair>()
        val processedKeys = mutableSetOf<String>()

        for (i in 0 until tags.size) {
            for (j in i + 1 until tags.size) {
                val t1 = tags[i]
                val t2 = tags[j]

                val name1 = t1.name.removePrefix("#").trim()
                val name2 = t2.name.removePrefix("#").trim()

                if (name1.isBlank() || name2.isBlank()) continue

                val pairKey = if (t1.tagId < t2.tagId) "${t1.tagId}_${t2.tagId}" else "${t2.tagId}_${t1.tagId}"
                if (processedKeys.contains(pairKey)) continue

                // 0. 严禁将数字、月份、年份、序号标签判定为相似标签 (白名单独立规则)
                if (isDateOrOrdinalTag(name1) || isDateOrOrdinalTag(name2)) continue
                if (isDifferOnlyByNumbers(name1, name2)) continue

                var isMatch = false
                var reason = ""
                var similarity = 0.0f

                // A. 忽略大小写后完全相同 (例如 #Food 与 #food)
                if (name1.equals(name2, ignoreCase = true) && name1 != name2) {
                    isMatch = true
                    reason = "英文字母大小写完全相同"
                    similarity = 0.98f
                }

                // 短文本 (长度 <= 2) 必须大小写完全相同，严禁因单字差异提示合并
                val minLen = minOf(name1.length, name2.length)
                val maxLen = maxOf(name1.length, name2.length)

                // B. 前缀/后缀包含 (例如 #猫咪 与 #猫咪s，#风景 与 #风景照)
                if (!isMatch && minLen >= 3) {
                    val lenDiff = abs(name1.length - name2.length)
                    if (lenDiff in 1..2) {
                        if (name1.startsWith(name2) || name2.startsWith(name1)) {
                            isMatch = true
                            reason = "名称前缀高度包容 (如 $name1 / $name2)"
                            similarity = 0.88f
                        } else if (name1.endsWith(name2) || name2.endsWith(name1)) {
                            isMatch = true
                            reason = "名称后缀高度相近"
                            similarity = 0.85f
                        }
                    }
                }

                // C. 编辑距离 (Levenshtein Distance) - 短文本门槛提升
                if (!isMatch && minLen >= 3) {
                    val dist = levenshteinDistance(name1, name2)

                    if (maxLen in 3..4 && dist == 1) {
                        isMatch = true
                        reason = "字形极其相似 (仅相差 1 个字符)"
                        similarity = 1.0f - (dist.toFloat() / maxLen)
                    } else if (maxLen >= 5 && dist <= 2) {
                        isMatch = true
                        reason = "字形高度相似 (编辑距离: $dist)"
                        similarity = 1.0f - (dist.toFloat() / maxLen)
                    }
                }

                // D. 拼音完全相同 (同音词联想，例如 "#写真" 与 "#写照")
                if (!isMatch && minLen >= 2) {
                    val py1 = PinyinUtils.getFullPinyin(name1)
                    val py2 = PinyinUtils.getFullPinyin(name2)
                    if (py1.isNotBlank() && py1 == py2 && name1 != name2) {
                        isMatch = true
                        reason = "汉语拼音读音完全一致 ($py1)"
                        similarity = 0.82f
                    }
                }

                if (isMatch) {
                    processedKeys.add(pairKey)

                    val count1 = tagsWithCount[t1.tagId]?.photoCount ?: 0
                    val count2 = tagsWithCount[t2.tagId]?.photoCount ?: 0

                    // 智能判定保留的主标签 (targetTag) 与待合并吸收的源标签 (sourceTag)
                    val (target, source, targetCount, sourceCount) = when {
                        count1 > count2 -> Quadruple(t1, t2, count1, count2)
                        count2 > count1 -> Quadruple(t2, t1, count2, count1)
                        t1.isPinned && !t2.isPinned -> Quadruple(t1, t2, count1, count2)
                        !t1.isPinned && t2.isPinned -> Quadruple(t2, t1, count2, count1)
                        t1.useCount >= t2.useCount -> Quadruple(t1, t2, count1, count2)
                        else -> Quadruple(t2, t1, count2, count1)
                    }

                    pairs.add(
                        DuplicateTagPair(
                            sourceTag = source,
                            targetTag = target,
                            similarityScore = similarity,
                            matchReason = reason,
                            sourcePhotoCount = sourceCount,
                            targetPhotoCount = targetCount
                        )
                    )
                }
            }
        }

        // 按相似度降序排列
        return pairs.sortedByDescending { it.similarityScore }
    }

    /**
     * 计算两字符串的 Levenshtein 编辑距离。
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // 插入
                    dp[i][j - 1] + 1,       // 删除
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        return dp[m][n]
    }

    /**
     * 判断是否为日期、月份、年份、序号等时间/序列类标签。
     * 例如："1月"、"12月"、"2025年"、"2026"、"第1天"、"Day1" 等。
     */
    private fun isDateOrOrdinalTag(name: String): Boolean {
        val clean = name.trim().lowercase()
        // 正则规则组
        val dateRegexes = listOf(
            Regex("^\\d{1,2}月$"),
            Regex("^\\d{4}年?$"),
            Regex("^\\d+$"),
            Regex("^第[0-9一二三四五六七八九十百千万]+.*$"),
            Regex("^[0-9]+[月日年号期天周季卷章集]$"),
            Regex("^(day|week|month|year|vol|ep|season|v)\\d+$", RegexOption.IGNORE_CASE),
            Regex("^\\d{1,4}[-./_]\\d{1,2}([-./_]\\d{1,2})?$")
        )
        return dateRegexes.any { it.matches(clean) }
    }

    /**
     * 判断两字符串是否仅有数字部分不同。
     * 例如："猫1" 与 "猫2"、"风景2025" 与 "风景2026"、"第1次" 与 "第2次"。
     */
    private fun isDifferOnlyByNumbers(s1: String, s2: String): Boolean {
        val digits1 = s1.filter { it.isDigit() }
        val digits2 = s2.filter { it.isDigit() }
        if (digits1.isNotEmpty() && digits2.isNotEmpty() && digits1 != digits2) {
            val stripped1 = s1.replace(Regex("\\d+"), "")
            val stripped2 = s2.replace(Regex("\\d+"), "")
            if (stripped1.equals(stripped2, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
