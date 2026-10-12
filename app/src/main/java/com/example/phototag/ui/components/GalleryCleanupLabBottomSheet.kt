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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * 🧹 Samsung 风格“相册瘦身工坊 (Gallery Cleanup Lab)”
 *
 * 核心功能：
 * 1. 【相似连拍照片聚类】：自动检测 3 秒内抓拍的连续照片，标出最佳留存项，一键清理多余项。
 * 2. 【截屏沉淀清理】：全量探测屏幕截图，展示累计空间占用，支持一键清空或批量移入回收站。
 * 3. 【超大体积媒体雷达】：倒序排列 >20MB 的高清大视频与专业 RAW 文件，直击存储大户。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryCleanupLabBottomSheet(
    allPhotos: List<PhotoEntity>,
    onDeletePhotos: (List<String>) -> Unit,
    onPreviewPhoto: (PhotoEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // 1. 相似连拍聚类分析
    val burstGroups = remember(allPhotos) {
        val sorted = allPhotos.filter { !it.isDeleted && !it.isVideo }
            .sortedByDescending { it.dateAdded }
        val groups = mutableListOf<List<PhotoEntity>>()
        var currentGroup = mutableListOf<PhotoEntity>()

        for (photo in sorted) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(photo)
            } else {
                val last = currentGroup.last()
                val lastTimeMs = if (last.dateAdded > 100000000000L) last.dateAdded else last.dateAdded * 1000L
                val photoTimeMs = if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L
                if (abs(lastTimeMs - photoTimeMs) <= 3500L) {
                    currentGroup.add(photo)
                } else {
                    if (currentGroup.size >= 2) {
                        groups.add(currentGroup.toList())
                    }
                    currentGroup = mutableListOf(photo)
                }
            }
        }
        if (currentGroup.size >= 2) {
            groups.add(currentGroup.toList())
        }
        groups
    }

    // 2. 屏幕截图扫描
    val screenshots = remember(allPhotos) {
        allPhotos.filter { !it.isDeleted && it.isScreenshot }
            .sortedByDescending { it.dateAdded }
    }

    // 3. 超大体积媒体扫描 (按大小倒序，限制 > 20MB 或长视频)
    val largeMedia = remember(allPhotos) {
        allPhotos.filter { !it.isDeleted && (it.size > 20 * 1024 * 1024L || it.isVideo) }
            .sortedByDescending { it.size }
    }

    val selectedUris = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(650.dp)
                .padding(top = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF34C759).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "相册瘦身工坊",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "释放无用连拍与沉淀文件，极速瘦身",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 维度 Tab
            val tabs = listOf(
                "相似连拍 (${burstGroups.size} 组)",
                "屏幕截图 (${screenshots.size} 张)",
                "超大文件 (${largeMedia.size} 项)"
            )
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 20.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            HapticFeedbackUtil.click(context)
                            selectedTab = index
                            selectedUris.clear()
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // 内容视图
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> BurstCleanupView(
                        groups = burstGroups,
                        selectedUris = selectedUris,
                        onToggleSelect = { uri ->
                            if (selectedUris.contains(uri)) selectedUris.remove(uri) else selectedUris.add(uri)
                        },
                        onPreview = onPreviewPhoto
                    )
                    1 -> ScreenshotCleanupView(
                        screenshots = screenshots,
                        selectedUris = selectedUris,
                        onToggleSelect = { uri ->
                            if (selectedUris.contains(uri)) selectedUris.remove(uri) else selectedUris.add(uri)
                        },
                        onSelectAll = {
                            selectedUris.clear()
                            selectedUris.addAll(screenshots.map { it.uri })
                        },
                        onPreview = onPreviewPhoto
                    )
                    2 -> LargeMediaCleanupView(
                        largeMedia = largeMedia,
                        selectedUris = selectedUris,
                        onToggleSelect = { uri ->
                            if (selectedUris.contains(uri)) selectedUris.remove(uri) else selectedUris.add(uri)
                        },
                        onPreview = onPreviewPhoto
                    )
                }
            }

            // 底部一键清理操作栏
            AnimatedVisibility(
                visible = selectedUris.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val totalSelectedSize = allPhotos.filter { it.uri in selectedUris }.sumOf { it.size }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "已选择 ${selectedUris.size} 项",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "可释放约 ${formatFileSize(totalSelectedSize)} 存储空间",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                HapticFeedbackUtil.click(context)
                                val toDelete = selectedUris.toList()
                                selectedUris.clear()
                                onDeletePhotos(toDelete)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("移入回收站", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BurstCleanupView(
    groups: List<List<PhotoEntity>>,
    selectedUris: List<String>,
    onToggleSelect: (String) -> Unit,
    onPreview: (PhotoEntity) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyCleanState(icon = Icons.Filled.PhotoAlbum, message = "未发现相似连拍照片，相册非常干净！")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(groups) { group ->
            // 自动推荐文件最大的为最佳保留项
            val bestPhoto = remember(group) { group.maxByOrNull { it.size } ?: group.first() }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val dateStr = remember(group) {
                        val timeMs = if (group.first().dateAdded > 100000000000L) group.first().dateAdded else group.first().dateAdded * 1000L
                        SimpleDateFormat("M月d日 HH:mm 连拍", Locale.getDefault()).format(Date(timeMs))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$dateStr (${group.size} 张)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "轻触图片勾选/取消",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(group) { photo ->
                            val isBest = photo.photoId == bestPhoto.photoId
                            val isSelected = selectedUris.contains(photo.uri)

                            Box(
                                modifier = Modifier
                                    .size(105.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleSelect(photo.uri) }
                            ) {
                                AsyncImage(
                                    model = CoilConfig.buildThumbnailRequest(LocalContext.current, photo.uri, sizePx = 240),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // 最佳留存标签
                                if (isBest) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .background(Color(0xFF34C759), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "推荐保留",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // 选中勾选指示标
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .background(
                                            if (isSelected) Color(0xFFFF3B30) else Color(0x66000000),
                                            CircleShape
                                        )
                                        .border(1.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotCleanupView(
    screenshots: List<PhotoEntity>,
    selectedUris: List<String>,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onPreview: (PhotoEntity) -> Unit
) {
    if (screenshots.isEmpty()) {
        EmptyCleanState(icon = Icons.Filled.Screenshot, message = "未发现屏幕截图文件！")
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val totalSize = remember(screenshots) { screenshots.sumOf { it.size } }
            Text(
                text = "共 ${screenshots.size} 张截屏 · 占用约 ${formatFileSize(totalSize)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "全选所有截屏",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSelectAll() }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(screenshots, key = { it.photoId }) { photo ->
                val isSelected = selectedUris.contains(photo.uri)

                Box(
                    modifier = Modifier
                        .aspectRatio(0.6f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleSelect(photo.uri) }
                ) {
                    AsyncImage(
                        model = CoilConfig.buildThumbnailRequest(LocalContext.current, photo.uri, sizePx = 280),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .background(
                                if (isSelected) Color(0xFFFF3B30) else Color(0x66000000),
                                CircleShape
                            )
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeMediaCleanupView(
    largeMedia: List<PhotoEntity>,
    selectedUris: List<String>,
    onToggleSelect: (String) -> Unit,
    onPreview: (PhotoEntity) -> Unit
) {
    if (largeMedia.isEmpty()) {
        EmptyCleanState(icon = Icons.Filled.VideoLibrary, message = "未发现大于 20MB 的超大文件！")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(largeMedia, key = { it.photoId }) { photo ->
            val isSelected = selectedUris.contains(photo.uri)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSelect(photo.uri) }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = CoilConfig.buildThumbnailRequest(LocalContext.current, photo.uri, sizePx = 160),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (photo.isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(3.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = photo.formattedDuration,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatFileSize(photo.size),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (photo.size > 100 * 1024 * 1024L) Color(0xFFFF3B30) else Color(0xFFFF9500)
                            )
                            if (photo.width > 0 && photo.height > 0) {
                                Text(
                                    text = " · ${photo.width}×${photo.height}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isSelected) Color(0xFFFF3B30) else Color(0x22000000),
                                CircleShape
                            )
                            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCleanState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        else -> String.format(Locale.getDefault(), "%.1f KB", kb)
    }
}
