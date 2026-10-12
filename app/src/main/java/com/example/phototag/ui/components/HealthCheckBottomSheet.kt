package com.example.phototag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.DuplicateTagPair
import com.example.phototag.data.model.HealthReport
import com.example.phototag.ui.theme.parseHexColor

/**
 * 🧹 相册健康度巡检与自愈诊断面板 (HealthCheckBottomSheet)。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthCheckBottomSheet(
    isChecking: Boolean,
    report: HealthReport?,
    onRecheck: () -> Unit,
    onCleanOrphanTags: (List<Long>) -> Unit,
    onMergeTags: (sourceTagId: Long, targetTagId: Long) -> Unit,
    onCleanDeadReferences: () -> Unit,
    onAutoFixAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var ignoredPairKeys by remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 顶部标题栏 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "相册健康巡检与自愈",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "自动识别幽灵标签、重复冗余与失效索引",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRecheck()
                        },
                        enabled = !isChecking
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "重新巡检", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ==================== 正在巡检扫描动画 ====================
            if (isChecking) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "正在深度扫描相册数据库...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "比对标签关联、交叉引用与编辑距离相似度",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (report != null) {
                // ==================== 诊断报告得分卡片 ====================
                HealthScoreCard(report = report)

                // ==================== 1. 【0 照片幽灵标签】区域 ====================
                OrphanTagsSection(
                    orphanTags = report.orphanTags,
                    onCleanAll = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCleanOrphanTags(report.orphanTags.map { it.tagId })
                    }
                )

                // ==================== 2. 【相似/重复命名标签】区域 ====================
                val visibleDuplicatePairs = report.duplicatePairs.filter {
                    "${it.sourceTag.tagId}_${it.targetTag.tagId}" !in ignoredPairKeys
                }
                DuplicateTagsSection(
                    duplicatePairs = visibleDuplicatePairs,
                    onMerge = { srcId, targetId ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMergeTags(srcId, targetId)
                    },
                    onIgnore = { pair ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        ignoredPairKeys = ignoredPairKeys + "${pair.sourceTag.tagId}_${pair.targetTag.tagId}"
                    }
                )

                // ==================== 3. 【无效脏关联记录】区域 ====================
                DeadReferencesSection(
                    deadCount = report.deadCrossRefCount,
                    nonExistentPhotos = report.nonExistentPhotoCount,
                    onClean = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCleanDeadReferences()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // ==================== 底部一键自愈大按钮 ====================
                if (report.totalIssueCount > 0) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAutoFixAll()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "一键优化修复 (共 ${report.totalIssueCount} 处)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("相册状态良好 · 完成体检", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * 顶部体检得分与状态卡片。
 */
@Composable
private fun HealthScoreCard(report: HealthReport) {
    val score = report.healthScore
    val (scoreColor, scoreGrade, statusIcon) = when {
        score >= 90 -> Triple(Color(0xFF2E7D32), "极佳", Icons.Filled.CheckCircle)
        score >= 70 -> Triple(Color(0xFFE65100), "良好", Icons.Filled.Warning)
        else -> Triple(Color(0xFFC62828), "待优化", Icons.Filled.Error)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, scoreColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 得分圆环展示
            Surface(
                shape = CircleShape,
                color = scoreColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(2.dp, scoreColor),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor
                        )
                        Text(
                            text = "分",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = scoreColor
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "健康状况：$scoreGrade",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(statusIcon, null, tint = scoreColor, modifier = Modifier.size(16.dp))
                }

                Text(
                    text = report.summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = "已全面巡检 ${report.totalCheckedTagsCount} 个标签 · ${report.totalCheckedPhotosCount} 张关联照片",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * 1. 0 照片幽灵标签区块。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrphanTagsSection(
    orphanTags: List<TagEntity>,
    onCleanAll: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "空标签 (0 张照片)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (orphanTags.isEmpty()) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "${orphanTags.size} 个",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (orphanTags.isEmpty()) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (orphanTags.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onCleanAll,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("一键清理", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (orphanTags.isEmpty()) {
                Text(
                    text = "干净整洁，暂无未关联任何照片的空标签",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "以下标签当前无任何关联照片，长期闲置会占满打标列表：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (tag in orphanTags) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(tag.colorHex))
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = tag.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "0张",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. 相似/重复命名标签合并建议区块。
 */
@Composable
private fun DuplicateTagsSection(
    duplicatePairs: List<DuplicateTagPair>,
    onMerge: (sourceTagId: Long, targetTagId: Long) -> Unit,
    onIgnore: (DuplicateTagPair) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "相似/重复命名标签建议",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (duplicatePairs.isEmpty()) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "${duplicatePairs.size} 组",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (duplicatePairs.isEmpty()) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            if (duplicatePairs.isEmpty()) {
                Text(
                    text = "标签命名规范统一，未发现同音词或字形高度相似的冲突标签",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "系统根据编辑距离与字形拼音算法，为您生成如下合并建议：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (pair in duplicatePairs) {
                        DuplicatePairItemCard(
                            pair = pair,
                            onMerge = onMerge,
                            onIgnore = { onIgnore(pair) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个相似标签合并项卡片。
 */
@Composable
private fun DuplicatePairItemCard(
    pair: DuplicateTagPair,
    onMerge: (Long, Long) -> Unit,
    onIgnore: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标签迁移流向展示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 源标签 (待合并删除)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "${pair.sourceTag.name} (${pair.sourcePhotoCount}张)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = " → ",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    // 目标标签 (保留)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "${pair.targetTag.name} (${pair.targetPhotoCount}张)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 忽略建议按钮
                    TextButton(
                        onClick = onIgnore,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.VisibilityOff,
                            contentDescription = "忽略",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "忽略",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // 合并按钮
                    Button(
                        onClick = { onMerge(pair.sourceTag.tagId, pair.targetTag.tagId) },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.CallMerge, null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("合并", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 匹配原因与相似度
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = pair.matchReason,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "相似度: ${(pair.similarityScore * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 3. 悬空与无效脏关联记录清理区块。
 */
@Composable
private fun DeadReferencesSection(
    deadCount: Int,
    nonExistentPhotos: Int,
    onClean: () -> Unit
) {
    val totalDirty = deadCount + nonExistentPhotos

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "无效数据索引",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (totalDirty == 0) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (totalDirty == 0) "正常" else "$totalDirty 条",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDirty == 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (totalDirty > 0) {
                    OutlinedButton(
                        onClick = onClean,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.CleaningServices, null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("一键自愈", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (totalDirty == 0) {
                Text(
                    text = "索引表与本地媒体库严格对应，无悬空引用与脏索引",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "发现 $deadCount 条已删除照片/标签的悬空关系索引，建议自愈清理以提升查询性能。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
