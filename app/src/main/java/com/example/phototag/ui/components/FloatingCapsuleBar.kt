package com.example.phototag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.util.HapticFeedbackUtil

/**
 * 🍏 iOS 灵动岛风格底部悬浮多选操作胶囊 (FloatingCapsuleBar)。
 *
 * 核心特性：
 * 1. 【一键同上快速直出】：呈现 [ 同上: #标签名 ]，省去弹窗步骤直接打标。
 * 2. 【高频记忆标签】：展示最近使用的 2 个标签胶囊，点击直接批量应用。
 * 3. 【5 大标准操作】：打标、收藏、调色、转移、分享与移入回收站。
 */
@Composable
fun FloatingCapsuleBar(
    visible: Boolean,
    selectedCount: Int,
    lastUsedTag: TagEntity? = null,
    recentTags: List<TagEntity> = emptyList(),
    onApplyDirectTag: ((TagEntity) -> Unit)? = null,
    onTagClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    onTransferClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCloseSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding()
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = getIosGlassBackground(),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 第一行：已选数量 + ⚡ 一键同上 / 最近标签快捷条
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 已选数量角标 + 取消按钮
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .bounceScale()
                                .clickable(onClick = onCloseSelection),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "取消多选",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$selectedCount 项",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // ⚡ 一键同上与最近标签
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lastUsedTag != null && onApplyDirectTag != null) {
                            item(key = "capsule_last_used_tag") {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .bounceScale()
                                        .clickable {
                                            HapticFeedbackUtil.click(context)
                                            onApplyDirectTag(lastUsedTag)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = "一键同上",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "同上: #${lastUsedTag.name.trimStart('#')}",
                                            fontSize = 11.sp,
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
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .bounceScale()
                                        .clickable {
                                            HapticFeedbackUtil.click(context)
                                            onApplyDirectTag(tag)
                                        }
                                ) {
                                    Text(
                                        text = "#${tag.name.trimStart('#')}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 第二行：标准操作按钮 (打标 / 调色 / 转移 / 分享 / 回收站)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CapsuleActionButton(
                        icon = Icons.Filled.Style,
                        label = "打标",
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = onTagClick
                    )

                    if (onFavoriteClick != null) {
                        CapsuleActionButton(
                            icon = Icons.Filled.Favorite,
                            label = "收藏",
                            tint = Color(0xFFFF3B30),
                            onClick = onFavoriteClick
                        )
                    }

                    if (onFilterClick != null) {
                        CapsuleActionButton(
                            icon = Icons.Filled.AutoFixHigh,
                            label = "调色",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = onFilterClick
                        )
                    }

                    CapsuleActionButton(
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        label = "转移",
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = onTransferClick
                    )

                    CapsuleActionButton(
                        icon = Icons.Filled.Share,
                        label = "分享",
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = onShareClick
                    )

                    CapsuleActionButton(
                        icon = Icons.Filled.Delete,
                        label = "回收站",
                        tint = if (androidx.compose.foundation.isSystemInDarkTheme()) com.example.phototag.ui.theme.IosRedDark else com.example.phototag.ui.theme.IosRed,
                        isDestructive = true,
                        onClick = onDeleteClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bg = if (isDestructive) {
        (if (isDark) com.example.phototag.ui.theme.IosRedDark else com.example.phototag.ui.theme.IosRed).copy(alpha = 0.14f)
    } else {
        getIosPillBackground()
    }
    val border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        if (isDestructive) (if (isDark) com.example.phototag.ui.theme.IosRedDark else com.example.phototag.ui.theme.IosRed).copy(alpha = 0.35f) else getIosHairlineColor()
    )

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = bg,
        border = border,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .bounceScale()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.5.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}
