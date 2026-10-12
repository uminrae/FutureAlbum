package com.example.phototag.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import com.example.phototag.util.HapticFeedbackUtil
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Style
import com.example.phototag.service.PhotoSyncService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.example.phototag.ui.components.TagChip
import com.example.phototag.ui.components.TagManageDialog
import kotlin.math.roundToInt

/**
 * ⚡ 极速打标/相册整理模式（流畅切图防卡死 + 一键智能自动整理）。
 *
 * 核心特性：
 * 1. 【0ms 瞬间响应切图】：UI 状态与后台写入彻底解耦，点击秒切下一张。
 * 2. 【key(photoId) 状态隔离】：每次切换彻底销毁旧手势与残余动画，杜绝卡死。
 * 3. 【⚡ 一键智能自动整理】：一键全自动按时间/来源/城市批量归档所有未整理照片。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickTagScreen(
    quickTagViewModel: QuickTagViewModel,
    onBack: () -> Unit
) {
    val uiState by quickTagViewModel.uiState.collectAsState()
    val totalUntaggedCount by quickTagViewModel.totalUntaggedCount.collectAsState()
    val allTags by quickTagViewModel.allTags.collectAsState()
    val categoriesWithTags by quickTagViewModel.categoriesWithTags.collectAsState()
    val lastUsedTagId by quickTagViewModel.lastUsedTagId.collectAsState()
    val canUndo by quickTagViewModel.canUndo.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val preferredTag = remember(lastUsedTagId, allTags) {
        allTags.find { it.tagId == lastUsedTagId }
            ?: allTags.firstOrNull { !it.isSystem }
            ?: allTags.firstOrNull()
    }

    var showCreateTagDialog by remember { mutableStateOf(false) }

    // 自动整理选项
    var tagByTime by remember { mutableStateOf(true) }
    var tagBySource by remember { mutableStateOf(true) }
    var tagByCity by remember { mutableStateOf(true) }

    val currentPhoto = uiState.currentPhoto
    val remainingCount = uiState.totalRemainingCount.coerceAtLeast(uiState.sessionPhotos.size - uiState.currentIndex)
    val totalSessionCount = uiState.organizedCount + remainingCount

    val coroutineScope = rememberCoroutineScope()
    val animOffsetX = remember { Animatable(0f) }

    // 进入页面时主动触发最新未打标照片拉取
    LaunchedEffect(Unit) {
        quickTagViewModel.loadInitialData()
    }

    // 当切换到新照片时，强制重置卡片拖动偏移量为 0f，杜绝手势死锁
    LaunchedEffect(currentPhoto?.photoId) {
        animOffsetX.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .navigationBarsPadding()
        ) {
            // ==================== 顶部进度与导航栏 ====================
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "退出极速整理"
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "极速整理",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "剩余 $remainingCount 张",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "已整理 ${uiState.organizedCount} 张",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 一键智能自动整理按钮
                        Button(
                            onClick = { quickTagViewModel.showAutoOrganizeDialog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "自动整理",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        if (canUndo) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                quickTagViewModel.undoLastAction()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, "撤回上一步", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        IconButton(onClick = { showCreateTagDialog = true }) {
                            Icon(Icons.Filled.Add, "新建标签", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // 进度条
                    val progress = if (totalSessionCount > 0)
                        (uiState.organizedCount.toFloat() / totalSessionCount).coerceIn(0f, 1f)
                    else 1f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // ==================== 主内容区 ====================
            val isPhotosLoading = uiState.isLoading || (uiState.sessionPhotos.isEmpty() && totalUntaggedCount > 0)
            if (isPhotosLoading) {
                // 骨架加载中
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "正在准备待整理照片…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (remainingCount == 0 || currentPhoto == null) {
                // 整理完成庆祝界面
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            Text(
                                "太棒了！已全部整理完毕",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                "相册中待整理的照片均已打标分类。\n本次会话共打标 ${uiState.organizedCount} 张图片！",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = onBack,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("返回相册")
                            }
                        }
                    }
                }
            } else {
                // ==================== 单图卡片 + 底部快速打标面板 ====================
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

                    // 居中大图卡片区（底层视差预备卡片 + 顶层物理阻尼手势卡片）
                    val nextPhoto = remember(uiState.sessionPhotos, uiState.currentIndex) {
                        uiState.sessionPhotos.getOrNull(uiState.currentIndex + 1)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.25f),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. 底层预备卡片 (Tinder / Card Stack 深度视差堆叠)
                        if (nextPhoto != null) {
                            val nextImageRequest = remember(nextPhoto.uri) {
                                ImageRequest.Builder(context)
                                    .data(nextPhoto.uri)
                                    .size(Size(600, 600))
                                    .crossfade(true)
                                    .build()
                            }
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val dragProgress = (kotlin.math.abs(animOffsetX.value) / 400f).coerceIn(0f, 1f)
                                        scaleX = 0.93f + dragProgress * 0.07f
                                        scaleY = 0.93f + dragProgress * 0.07f
                                        alpha = 0.45f + dragProgress * 0.55f
                                    }
                            ) {
                                AsyncImage(
                                    model = nextImageRequest,
                                    contentDescription = nextPhoto.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // 2. 顶层交互手势卡片
                        AnimatedContent(
                            targetState = currentPhoto,
                            transitionSpec = {
                                (slideInHorizontally(animationSpec = tween(220)) { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { width -> -width } + fadeOut())
                            },
                            label = "quickTagCardAnimation",
                            modifier = Modifier.fillMaxSize()
                        ) { photo ->
                            key(photo.photoId) {
                                val imageRequest = remember(photo.uri) {
                                    ImageRequest.Builder(context)
                                        .data(photo.uri)
                                        .size(Size(800, 800))
                                        .crossfade(true)
                                        .build()
                                }

                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            translationX = animOffsetX.value
                                            rotationZ = (animOffsetX.value * 0.04f).coerceIn(-18f, 18f)
                                            val dragProgress = (kotlin.math.abs(animOffsetX.value) / 600f).coerceIn(0f, 1f)
                                            val scale = 1f - dragProgress * 0.04f
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .pointerInput(photo.photoId) {
                                            detectDragGestures(
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    coroutineScope.launch {
                                                        animOffsetX.snapTo(animOffsetX.value + dragAmount.x)
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (animOffsetX.value < -150f) {
                                                        coroutineScope.launch {
                                                            HapticFeedbackUtil.click(context)
                                                            animOffsetX.animateTo(-1200f, tween(160))
                                                            quickTagViewModel.skipCurrentPhoto()
                                                            animOffsetX.snapTo(0f)
                                                        }
                                                    } else if (animOffsetX.value > 150f) {
                                                        coroutineScope.launch {
                                                            HapticFeedbackUtil.click(context)
                                                            animOffsetX.animateTo(1200f, tween(160))
                                                            if (preferredTag != null) {
                                                                quickTagViewModel.tagCurrentPhoto(preferredTag.tagId)
                                                            } else {
                                                                quickTagViewModel.skipCurrentPhoto()
                                                            }
                                                            animOffsetX.snapTo(0f)
                                                        }
                                                    } else {
                                                        coroutineScope.launch {
                                                            animOffsetX.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec = spring(
                                                                    dampingRatio = 0.72f,
                                                                    stiffness = Spring.StiffnessMediumLow
                                                                )
                                                            )
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    coroutineScope.launch {
                                                        animOffsetX.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(
                                                                dampingRatio = 0.72f,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = imageRequest,
                                            contentDescription = photo.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // 左滑“跳过”视觉胶囊徽章 (基于 graphicsLayer alpha，零重组消耗)
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFFEF4444),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                            shadowElevation = 8.dp,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(20.dp)
                                                .graphicsLayer {
                                                    alpha = (-animOffsetX.value / 140f).coerceIn(0f, 0.95f)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "跳过",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        // 右滑“归入标签”视觉胶囊徽章 (基于 graphicsLayer alpha，零重组消耗)
                                        if (preferredTag != null) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF10B981),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                                shadowElevation = 8.dp,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(20.dp)
                                                    .graphicsLayer {
                                                        alpha = (animOffsetX.value / 140f).coerceIn(0f, 0.95f)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Style,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "归入 #${preferredTag.name.trimStart('#')}",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }

                                        // 底部半透明信息栏
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomStart)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                    )
                                                )
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = photo.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 底部一键标签面板
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.0f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Style,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "单点标签直接打标并切至下一张：",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // 平铺标签列表（点击即刻打标）
                            val distinctTags = remember(allTags) { allTags.distinctBy { it.name } }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        distinctTags.forEach { tag ->
                                            TagChip(
                                                name = tag.name,
                                                colorHex = tag.colorHex,
                                                isSelected = false,
                                                isSystem = tag.isSystem,
                                                onClick = {
                                                    coroutineScope.launch { animOffsetX.snapTo(0f) }
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    quickTagViewModel.tagCurrentPhoto(tag.tagId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // 底部操作栏：撤回 / 跳过 / 删除
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (canUndo) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch { animOffsetX.snapTo(0f) }
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            quickTagViewModel.undoLastAction()
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("撤回")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch { animOffsetX.snapTo(0f) }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        quickTagViewModel.skipCurrentPhoto()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("跳过此图")
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch { animOffsetX.snapTo(0f) }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        quickTagViewModel.deleteCurrentPhoto(currentPhoto)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("删除")
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== ⚡ 一键智能自动整理配置确认弹窗 ====================
        if (uiState.showAutoOrganizeDialog) {
            AlertDialog(
                onDismissRequest = { quickTagViewModel.dismissAutoOrganizeDialog() },
                icon = {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        "自动智能整理",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "系统将为相册中剩余的 $remainingCount 张照片执行全自动智能归档，请勾选自动打标规则：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(4.dp))

                        // 选项 1: 时间打标
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tagByTime = !tagByTime }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tagByTime,
                                    onCheckedChange = { tagByTime = it }
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("按拍摄时间打标", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text("如自动打上 #2026年、#8月 等", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 选项 2: 来源打标
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tagBySource = !tagBySource }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tagBySource,
                                    onCheckedChange = { tagBySource = it }
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Folder, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("按照片来源路径打标", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text("如自动打上 #截图、#微信、#相机拍摄 等", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 选项 3: 城市打标
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tagByCity = !tagByCity }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tagByCity,
                                    onCheckedChange = { tagByCity = it }
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("按照片所在城市打标", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text("如自动提取城市打上 #深圳、#阜阳 等", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            com.example.phototag.service.PhotoSyncService.startAutoOrganize(
                                context = context,
                                tagByTime = tagByTime,
                                tagBySource = tagBySource,
                                tagByCity = tagByCity
                            )
                            quickTagViewModel.startAutoOrganize(
                                AutoOrganizeOptions(
                                    tagByTime = tagByTime,
                                    tagBySource = tagBySource,
                                    tagByCity = tagByCity
                                )
                            )
                        },
                        enabled = tagByTime || tagBySource || tagByCity
                    ) {
                        Text("开始全自动整理")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { quickTagViewModel.dismissAutoOrganizeDialog() }) {
                        Text("取消")
                    }
                }
            )
        }

        // ==================== 正在自动整理全屏/浮层进度 (支持 ⏸️ 暂停 / ▶️ 继续 / ✕ 取消) ====================
        if (uiState.isAutoOrganizing) {
            val isPaused = uiState.syncState == SyncState.PAUSED

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(10.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isPaused) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Pause,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            } else {
                                CircularProgressIndicator(
                                    progress = { uiState.autoOrganizeProgress },
                                    strokeWidth = 5.dp,
                                    modifier = Modifier.size(64.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = if (isPaused) "智能归档已暂停" else "正在全自动智能归档",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (isPaused)
                                    "已暂停（已完成 ${uiState.autoOrganizeCurrent} / ${uiState.autoOrganizeTotal} 张）"
                                else
                                    "已完成: ${uiState.autoOrganizeCurrent} / ${uiState.autoOrganizeTotal} 张 (${(uiState.autoOrganizeProgress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LinearProgressIndicator(
                                progress = { uiState.autoOrganizeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )

                            Spacer(Modifier.height(6.dp))

                            // 底部操作控制按钮组：【⏸️ 暂停 / ▶️ 继续】 与 【✕ 取消】
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPaused) {
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            quickTagViewModel.resumeSync()
                                            PhotoSyncService.resume(context)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("继续整理")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            quickTagViewModel.pauseSync()
                                            PhotoSyncService.pause(context)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.Pause, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("暂停整理")
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        quickTagViewModel.cancelSync()
                                        PhotoSyncService.cancel(context)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(2.dp))
                                    Text("取消", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== 自动整理完成弹窗 ====================
        if (uiState.autoOrganizeFinishedDialog) {
            AlertDialog(
                onDismissRequest = { quickTagViewModel.dismissAutoOrganizeFinishedDialog() },
                icon = {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "相册自动整理完毕",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "本次整理共为 ${uiState.autoOrganizedSuccessCount} 张照片生成了时间、来源与城市标签，分类归档已完成。",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            quickTagViewModel.dismissAutoOrganizeFinishedDialog()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("完成并返回")
                    }
                }
            )
        }

        // ==================== 新建标签弹窗 ====================
        if (showCreateTagDialog) {
            TagManageDialog(
                categoriesWithTags = categoriesWithTags,
                onCreateTag = { name, catId, colorHex, _, newCat ->
                    quickTagViewModel.createTag(name, catId, colorHex)
                },
                onCreateCategory = { name -> quickTagViewModel.createCategory(name) },
                onDismiss = { showCreateTagDialog = false }
            )
        }
    }
}
