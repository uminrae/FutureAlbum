package com.example.phototag.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.util.CoilConfig
import com.example.phototag.util.HapticFeedbackUtil

/**
 * Instagram / iOS 风格长按悬浮卡片预览与快捷菜单 (Peek & Pop Overlay)。
 *
 * 特性：
 * 1. 【高斯虚化与遮罩背景】：全屏背景平滑暗化虚化。
 * 2. 【弹簧动力浮层卡片】：照片以高质感圆角大卡片居中弹出。
 * 3. 【iOS 18 快捷操作矩阵】：收藏、参数EXIF、修图调色、标签、分享、设为封面与回收站。
 */
@Composable
fun InstagramPeekOverlay(
    photo: PhotoEntity?,
    isFavorite: Boolean = false,
    onDismiss: () -> Unit,
    onOpenFullViewer: (PhotoEntity) -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit,
    onViewDetails: (PhotoEntity) -> Unit = {},
    onEditPhoto: (PhotoEntity) -> Unit = {},
    onQuickTag: (PhotoEntity) -> Unit,
    onShare: (PhotoEntity) -> Unit,
    onSetAsCover: (PhotoEntity) -> Unit = {},
    onDelete: (PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (photo == null) return

    BackHandler(enabled = true) {
        onDismiss()
    }

    val context = LocalContext.current

    LaunchedEffect(photo.photoId) {
        HapticFeedbackUtil.heavy(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==================== 1. Instagram 悬浮主预览卡片 ====================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onDismiss()
                            onOpenFullViewer(photo)
                        }
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 卡片顶部信息栏 (Instagram 风格 Author Header)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Fullscreen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = photo.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "长按预览 · 轻触进入全屏",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // 中间高清图片/视频/动图展示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        if (photo.isVideo) {
                            // 视频长按悬浮：自动开播、静音循环
                            VideoPlayerView(
                                uri = photo.uri,
                                autoPlay = true,
                                isLooping = true,
                                isMuted = true,
                                isPeekMode = true,
                                showControls = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val imageRequest = remember(photo.uri, photo.isGif) {
                                CoilConfig.buildViewerRequest(
                                    context = context,
                                    uri = photo.uri,
                                    screenWidthPx = 1080,
                                    screenHeightPx = 1080,
                                    isGif = photo.isGif
                                )
                            }
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = photo.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (photo.isGif) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "GIF 动图",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 2. iOS 18 风格悬浮快捷操作矩阵 ====================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 14.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    // 1. 设为收藏 / 取消收藏
                    InstagramMenuItem(
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        title = if (isFavorite) "取消收藏" else "设为收藏",
                        iconTint = if (isFavorite) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            HapticFeedbackUtil.click(context)
                            onDismiss()
                            onToggleFavorite(photo)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)

                    // 2. 详细参数 (EXIF 与真实位置)
                    InstagramMenuItem(
                        icon = Icons.Outlined.Info,
                        title = "详细参数与位置",
                        onClick = {
                            HapticFeedbackUtil.click(context)
                            onDismiss()
                            onViewDetails(photo)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)

                    // 3. 胶片暗房调色修图
                    if (!photo.isVideo) {
                        InstagramMenuItem(
                            icon = Icons.Filled.AutoFixHigh,
                            title = "胶片暗房调色修图",
                            onClick = {
                                HapticFeedbackUtil.click(context)
                                onDismiss()
                                onEditPhoto(photo)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)
                    }

                    // 4. 管理标签
                    InstagramMenuItem(
                        icon = Icons.Filled.Label,
                        title = "管理标签",
                        onClick = {
                            HapticFeedbackUtil.click(context)
                            onDismiss()
                            onQuickTag(photo)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)

                    // 5. 分享照片
                    InstagramMenuItem(
                        icon = Icons.Filled.Share,
                        title = "系统外发分享",
                        onClick = {
                            HapticFeedbackUtil.click(context)
                            onDismiss()
                            onShare(photo)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)

                    // 6. 设为相册封面
                    InstagramMenuItem(
                        icon = Icons.Filled.PhotoLibrary,
                        title = "设为相册封面",
                        onClick = {
                            HapticFeedbackUtil.click(context)
                            onDismiss()
                            onSetAsCover(photo)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)

                    // 7. 移入回收站
                    InstagramMenuItem(
                        icon = Icons.Filled.Delete,
                        title = "移入安全回收站",
                        isDestructive = true,
                        onClick = {
                            HapticFeedbackUtil.heavy(context)
                            onDismiss()
                            onDelete(photo)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstagramMenuItem(
    icon: ImageVector,
    title: String,
    isDestructive: Boolean = false,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    val tintColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val resolvedIconTint = iconTint ?: tintColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = resolvedIconTint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium,
            color = tintColor,
            fontSize = 14.sp
        )
    }
}
