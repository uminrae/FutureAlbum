package com.example.phototag.ui.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phototag.data.entity.PhotoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🗑️ 7天安全回收站独立管理页面（支持物理级彻底销毁与一键原样恢复）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashViewModel: TrashViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by trashViewModel.uiState.collectAsState()
    val trashPhotos by trashViewModel.trashPhotos.collectAsState()
    val trashPhotoCount by trashViewModel.trashPhotoCount.collectAsState()

    val isMultiSelect = uiState.selectedUris.isNotEmpty()

    // 自动清洗历史残留的 .trash 后缀文件，恢复可解码的原格式
    LaunchedEffect(Unit) {
        try {
            com.example.phototag.util.MediaStoreDeleteHelper.migrateTrashStorage(context)
        } catch (_: Throwable) {}
    }

    // ==================== 💥 物理级彻底删除系统原生确认 Launcher ====================
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 用户在系统弹窗中确认删除：底层物理文件已被系统彻底粉碎，同步清理本地数据库
            trashViewModel.onSystemDeleteConfirmed()
        } else {
            trashViewModel.onSystemDeleteCancelled()
        }
    }

    // ==================== ↩️ 系统级原样恢复确认 Launcher ====================
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            trashViewModel.onSystemRestoreConfirmed()
        } else {
            trashViewModel.onSystemRestoreCancelled()
        }
    }

    // 监听 pendingRestoreIntentSender 调起系统原生恢复确认
    LaunchedEffect(uiState.pendingRestoreIntentSender) {
        uiState.pendingRestoreIntentSender?.let { sender ->
            try {
                restoreLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } catch (e: Exception) {
                Toast.makeText(context, "调起恢复授权失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                trashViewModel.onSystemRestoreCancelled()
            }
        }
    }

    // 监听 pendingDeleteIntentSender 调起系统原生物理删除确认
    LaunchedEffect(uiState.pendingDeleteIntentSender) {
        uiState.pendingDeleteIntentSender?.let { sender ->
            try {
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } catch (e: Exception) {
                Toast.makeText(context, "调起物理删除授权失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                trashViewModel.onSystemDeleteCancelled()
            }
        }
    }

    // 监听 Toast 提示
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            trashViewModel.clearToastMessage()
        }
    }

    BackHandler {
        if (isMultiSelect) {
            trashViewModel.clearSelection()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "回收站",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (trashPhotoCount > 0) "共 $trashPhotoCount 项 · 7 天后自动清除" else "暂存区",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    com.example.phototag.ui.components.IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        onClick = {
                            if (isMultiSelect) trashViewModel.clearSelection() else onBack()
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                actions = {
                    if (trashPhotos.isNotEmpty()) {
                        if (isMultiSelect) {
                            com.example.phototag.ui.components.IosButton(
                                text = if (uiState.selectedUris.size == trashPhotos.size) "取消全选" else "全选",
                                variant = com.example.phototag.ui.components.IosButtonVariant.GHOST,
                                onClick = {
                                    if (uiState.selectedUris.size == trashPhotos.size) {
                                        trashViewModel.clearSelection()
                                    } else {
                                        trashViewModel.selectAll()
                                    }
                                },
                                height = 32.dp
                            )
                        } else {
                            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                            com.example.phototag.ui.components.IosIconButton(
                                icon = Icons.Filled.CleaningServices,
                                contentDescription = "清空回收站",
                                tint = if (isDark) com.example.phototag.ui.theme.IosRedDark else com.example.phototag.ui.theme.IosRed,
                                onClick = { trashViewModel.showClearAllDialog() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.example.phototag.ui.components.getIosGlassBackground()
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (trashPhotos.isEmpty()) {
                // ==================== 空状态 ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.AutoDelete,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "回收站空空如也",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "相册中移入回收站的照片与视频将在此安全暂存 7 天，\n期间可随时一键原样恢复，彻底删除将从物理设备永久销毁。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else {
                // ==================== 回收站网格 ====================
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(trashPhotos, key = { it.photoId }) { photo ->
                        val isSelected = photo.uri in uiState.selectedUris
                        TrashPhotoGridItem(
                            photo = photo,
                            isSelected = isSelected,
                            isSelectionMode = isMultiSelect,
                            onClick = {
                                if (isMultiSelect) {
                                    trashViewModel.toggleSelect(photo.uri)
                                } else {
                                    trashViewModel.openPhotoPreview(photo)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelect) {
                                    trashViewModel.toggleSelect(photo.uri)
                                }
                            }
                        )
                    }
                }
            }

            // ==================== 底部悬浮多选操作栏 ====================
            AnimatedVisibility(
                visible = isMultiSelect,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = com.example.phototag.ui.components.getIosGlassBackground(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, com.example.phototag.ui.components.getIosHairlineColor()),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 恢复按钮 (iOS 18 蓝胶囊)
                        com.example.phototag.ui.components.IosButton(
                            text = "恢复 (${uiState.selectedUris.size})",
                            icon = Icons.Filled.Restore,
                            variant = com.example.phototag.ui.components.IosButtonVariant.PRIMARY,
                            onClick = { trashViewModel.restoreSelectedPhotos(context) },
                            height = 42.dp,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(12.dp))

                        // 彻底删除按钮 (iOS 18 红胶囊)
                        com.example.phototag.ui.components.IosButton(
                            text = "彻底删除",
                            icon = Icons.Filled.DeleteForever,
                            variant = com.example.phototag.ui.components.IosButtonVariant.DESTRUCTIVE,
                            onClick = { trashViewModel.showHardDeleteDialog() },
                            height = 42.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // ==================== 单图详情与操作 BottomSheet ====================
    uiState.previewPhoto?.let { photo ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { trashViewModel.closePhotoPreview() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    val previewSource = remember(photo.uri, photo.displayName) {
                        try {
                            com.example.phototag.util.MediaStoreDeleteHelper.getTrashImageSource(context, photo)
                        } catch (_: Throwable) {
                            photo.uri
                        }
                    }
                    val viewerRequest = remember(previewSource) {
                        com.example.phototag.util.CoilConfig.buildViewerRequest(context, previewSource)
                    }
                    AsyncImage(
                        model = viewerRequest,
                        contentDescription = photo.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = photo.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val deleteDateStr = remember(photo.deletedAt) {
                    val date = Date(photo.deletedAt ?: System.currentTimeMillis())
                    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()).format(date)
                }

                Text(
                    text = "移入回收站时间：$deleteDateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { trashViewModel.restoreSinglePhoto(context, photo.uri) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("原样恢复")
                    }

                    FilledTonalButton(
                        onClick = {
                            trashViewModel.requestPermanentDeleteSingle(context, photo)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("彻底删除")
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ==================== 彻底删除确认弹窗 ====================
    if (uiState.showHardDeleteDialog) {
        AlertDialog(
            onDismissRequest = { trashViewModel.dismissHardDeleteDialog() },
            icon = { Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("彻底删除 ${uiState.selectedUris.size} 张照片？", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "此操作将从手机物理存储中彻底销毁文件，并抹除相册与标签记录，无法撤销！确认物理粉碎吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { trashViewModel.requestPermanentDeleteSelected(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认彻底删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { trashViewModel.dismissHardDeleteDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // ==================== 一键清空回收站确认弹窗 ====================
    if (uiState.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { trashViewModel.dismissClearAllDialog() },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("一键清空回收站？", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "回收站中的全部 $trashPhotoCount 张照片与视频的物理文件将被完全删除，此操作无法撤销！确认全部清空吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { trashViewModel.requestEmptyTrash(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认全部清空", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { trashViewModel.dismissClearAllDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 回收站单张照片卡片 ====================

@Composable
private fun TrashPhotoGridItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val countdownText = remember(photo.deletedAt) {
        calculateCountdownText(photo.deletedAt)
    }

    val isUrgent = remember(photo.deletedAt) {
        val now = System.currentTimeMillis()
        val deletedAt = photo.deletedAt ?: now
        val elapsed = now - deletedAt
        val remaining = 7 * 24 * 3600 * 1000L - elapsed
        remaining < 24 * 3600 * 1000L
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val itemContext = LocalContext.current
        val imageSource = remember(photo.uri, photo.displayName) {
            try {
                com.example.phototag.util.MediaStoreDeleteHelper.getTrashImageSource(itemContext, photo)
            } catch (_: Throwable) {
                photo.uri
            }
        }
        val thumbnailRequest = remember(imageSource) {
            com.example.phototag.util.CoilConfig.buildThumbnailRequest(itemContext, imageSource, sizePx = 240)
        }

        // 照片预览图
        AsyncImage(
            model = thumbnailRequest,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 视频时长角标
        if (photo.isVideo) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        photo.formattedDuration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 倒计时胶囊角标
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isUrgent) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            } else {
                Color.Black.copy(alpha = 0.65f)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isUrgent) Icons.Filled.HourglassBottom else Icons.Filled.HourglassTop,
                    contentDescription = null,
                    tint = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    countdownText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else Color.White
                )
            }
        }

        // 多选选中标记 / 遮罩
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(20.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
        }
    }
}

/**
 * 计算 7 天倒计时可读文案。
 */
private fun calculateCountdownText(deletedAt: Long?): String {
    if (deletedAt == null) return "剩 7 天"
    val now = System.currentTimeMillis()
    val elapsed = now - deletedAt
    val remainingMs = (7 * 24 * 60 * 60 * 1000L) - elapsed

    return when {
        remainingMs <= 0 -> "即将清除"
        remainingMs < 60 * 60 * 1000L -> "剩 <1 小时"
        remainingMs < 24 * 60 * 60 * 1000L -> {
            val hours = (remainingMs / (60 * 60 * 1000L)).coerceAtLeast(1)
            "剩 $hours 小时"
        }
        else -> {
            val days = (remainingMs / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
            "剩 $days 天"
        }
    }
}
