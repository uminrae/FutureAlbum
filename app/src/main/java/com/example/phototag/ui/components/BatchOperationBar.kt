package com.example.phototag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.util.HapticFeedbackUtil

/**
 * 现代多选底部悬浮操作栏（BatchOperationBar，支持一键同上与最近记忆标签快速直出）。
 */
@Composable
fun BatchOperationBar(
    selectedCount: Int,
    lastUsedTag: TagEntity? = null,
    recentTags: List<TagEntity> = emptyList(),
    onApplyDirectTag: ((TagEntity) -> Unit)? = null,
    onBatchTag: () -> Unit,
    onBatchFavorite: (() -> Unit)? = null,
    onMoveTag: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 16.dp,
        color = getIosGlassBackground(),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 顶层：已选数量 + 一键同上 / 记忆标签横向滑动条
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "已选 $selectedCount 项",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // ⚡ 一键同上 / 最近标签直接点击应用
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lastUsedTag != null && onApplyDirectTag != null) {
                        item(key = "last_used_tag_btn") {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        HapticFeedbackUtil.click(context)
                                        onApplyDirectTag(lastUsedTag)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = "一键同上",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = "同上: #${lastUsedTag.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (onApplyDirectTag != null) {
                        items(
                            items = recentTags.filter { it.tagId != lastUsedTag?.tagId }.take(2),
                            key = { it.tagId }
                        ) { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        HapticFeedbackUtil.click(context)
                                        onApplyDirectTag(tag)
                                    }
                            ) {
                                Text(
                                    text = "#${tag.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                IosButton(
                    text = "取消",
                    variant = IosButtonVariant.GHOST,
                    onClick = onCancel,
                    height = 32.dp
                )
            }

            // 底层：4 大标准操作胶囊按钮 (iOS 18 规范)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IosButton(
                    text = "打标",
                    icon = Icons.Filled.Style,
                    variant = IosButtonVariant.PRIMARY,
                    onClick = onBatchTag,
                    height = 38.dp,
                    modifier = Modifier.weight(1f)
                )

                if (onBatchFavorite != null) {
                    IosButton(
                        text = "收藏",
                        icon = Icons.Filled.Favorite,
                        variant = IosButtonVariant.SECONDARY,
                        onClick = onBatchFavorite,
                        height = 38.dp,
                        modifier = Modifier.weight(1f)
                    )
                }

                IosButton(
                    text = "转移",
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    variant = IosButtonVariant.SECONDARY,
                    onClick = onMoveTag,
                    height = 38.dp,
                    modifier = Modifier.weight(1f)
                )

                IosButton(
                    text = "分享",
                    icon = Icons.Filled.Share,
                    variant = IosButtonVariant.SECONDARY,
                    onClick = onShare,
                    height = 38.dp,
                    modifier = Modifier.weight(1f)
                )

                IosButton(
                    text = "回收站",
                    icon = Icons.Filled.Delete,
                    variant = IosButtonVariant.DESTRUCTIVE,
                    onClick = onDelete,
                    height = 38.dp,
                    modifier = Modifier.weight(1.1f)
                )
            }
        }
    }
}
