package com.example.phototag.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import com.example.phototag.ui.components.ExifSpoofBottomSheet
import com.example.phototag.ui.components.IosActionSheet
import com.example.phototag.ui.components.IosActionSheetItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.ui.MainViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.example.phototag.ui.components.BatchTagBottomSheet
import com.example.phototag.ui.components.DismissibleViewerContainer
import com.example.phototag.ui.components.FilmstripBar
import com.example.phototag.ui.components.PhotoMetadataBottomSheet
import com.example.phototag.ui.components.PhotoViewerItem
import com.example.phototag.util.CoilConfig
import com.example.phototag.util.ExifMetadataUtil
import com.example.phototag.util.HapticFeedbackUtil
import com.example.phototag.util.PhotoMetadata
import kotlinx.coroutines.launch

/**
 * 全屏大图查看器（PhotoViewerScreen）。
 *
 * 核心功能：
 * 1. 【HorizontalPager 丝滑翻页】：左右滑动无缝连续切图，实时更新页码与当前大图。
 * 2. 【无冲突手势分流 (Gesture 分流)】：
 *    - 原始尺寸（scale == 1.0f）：完全放行单指滑动，水平翻页零阻滞。
 *    - 双指缩放（Pinch-to-Zoom）与双击放大（Double-Tap Zoom）：支持放大至 5 倍并进行边界限制平移。
 *    - 切页时自动平滑复位上一页缩放与位移。
 * 3. 【ℹ️ 高精度逆地理位置与元数据】：实时触发双通道自动逆地理编码与持久化回填。
 * 4. 【🛡️ 隐私脱敏分享】：支持抹除 GPS 经纬度分享。
 * 5. 【实时打标与回收站软删除】：支持即时增删标签与 7 天安全回收。
 */
@Composable
fun PhotoViewerScreen(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // 拦截返回手势，确保大图预览按返回键时仅退出大图，绝不直接退出应用
    BackHandler(enabled = true) {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var lastKnownPage by remember { mutableIntStateOf(initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
        pageCount = {
            if (photos.isEmpty()) 0 else maxOf(photos.size, lastKnownPage + 1)
        }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (lastKnownPage != pagerState.currentPage) {
            HapticFeedbackUtil.tick(context)
        }
        lastKnownPage = pagerState.currentPage
    }

    var isControlsVisible by remember { mutableStateOf(true) }
    var showMetadataSheet by remember { mutableStateOf(false) }
    var showSharePrivacyDialog by remember { mutableStateOf(false) }
    var showSpoofBottomSheet by remember { mutableStateOf(false) }
    var stripGpsPrivacy by remember { mutableStateOf(true) }
    var isSortingMode by remember { mutableStateOf(false) }
    var showFilmWorkshopSheet by remember { mutableStateOf(false) }

    LaunchedEffect(photos.size) {
        if (photos.isEmpty()) {
            onBack()
        } else if (pagerState.currentPage >= photos.size) {
            try {
                pagerState.scrollToPage((photos.size - 1).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }

    val safeIndex = pagerState.currentPage.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    val currentPhoto = photos.getOrNull(safeIndex)
    if (currentPhoto == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val allPhotosWithTags by viewModel.allPhotosWithTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val lastUsedTag by viewModel.lastUsedTag.collectAsState()
    val categoriesWithTags by viewModel.categoriesWithTags.collectAsState()
    val recentTags by viewModel.recentTags.collectAsState()
    val topUsedTags by viewModel.topUsedTags.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val parentTagAlbums by viewModel.parentTagAlbums.collectAsState()

    var showMoreMenu by remember { mutableStateOf(false) }
    var showSetCoverDialog by remember { mutableStateOf(false) }


    // 当前大图的 EXIF 元数据状态（支持地点修改与自动逆编码后即时刷新）
    var currentMetadata by remember { mutableStateOf<PhotoMetadata?>(null) }
    var isMetadataLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentPhoto.photoId, currentPhoto.customLocation, currentPhoto.address) {
        isMetadataLoading = true
        val meta = ExifMetadataUtil.parseMetadata(context, currentPhoto)
        currentMetadata = meta
        isMetadataLoading = false

        // 立即持久化解析出的经纬度、海拔与地址至本地数据库（保证第二次打开 0ms 瞬间秒出）
        if (meta.hasGps && (currentPhoto.latitude == null || currentPhoto.altitude == null || currentPhoto.address.isNullOrBlank())) {
            viewModel.saveParsedMetadata(
                photoId = currentPhoto.photoId,
                latitude = meta.latitude,
                longitude = meta.longitude,
                address = meta.address,
                altitude = meta.altitude
            )
        }

        // 若当前照片带有 GPS 坐标但尚未解析为高精街道/建筑地标，在后台发起高精逆编码并持久化
        if (meta.hasGps && (meta.address.isNullOrBlank() || !com.example.phototag.util.AutoLocationResolver.isDetailedAddress(meta.address))) {
            viewModel.resolvePhotoAddressSilently(currentPhoto) { resolvedAddress ->
                if (!resolvedAddress.isNullOrBlank()) {
                    currentMetadata = currentMetadata?.copy(address = resolvedAddress)
                }
            }
        }
    }

    val displayMetrics = context.resources.displayMetrics
    val screenWidthPx = displayMetrics.widthPixels
    val screenHeightPx = displayMetrics.heightPixels

    // ⚡ 智能后台预加载前后相邻照片到内存缓存池（0 延迟即划即显）
    LaunchedEffect(pagerState.currentPage, photos) {
        val currentPage = pagerState.currentPage
        val adjacentPhotos = listOfNotNull(
            photos.getOrNull(currentPage - 1),
            photos.getOrNull(currentPage + 1),
            photos.getOrNull(currentPage - 2),
            photos.getOrNull(currentPage + 2)
        )
        val imageLoader = context.imageLoader
        for (adjacentPhoto in adjacentPhotos) {
            val preloadRequest = CoilConfig.buildViewerRequest(
                context = context,
                uri = adjacentPhoto.uri,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                isZoomed = false
            )
            imageLoader.enqueue(preloadRequest)
        }
    }

    // 获取当前大图绑定的所有标签
    val currentPhotoTags = remember(allPhotosWithTags, currentPhoto.photoId) {
        allPhotosWithTags.find { it.photo.photoId == currentPhoto.photoId }?.tags ?: emptyList()
    }

    BackHandler(enabled = true) {
        onBack()
    }

    DismissibleViewerContainer(
        currentKey = currentPhoto.photoId,
        onDismiss = {
            if (isSortingMode) {
                // 分拣模式：下滑执行移入回收站
                HapticFeedbackUtil.heavy(context)
                val currentIdx = pagerState.currentPage
                val safeIdx = currentIdx.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
                val targetPhoto = photos.getOrNull(safeIdx) ?: currentPhoto
                val uriToDelete = targetPhoto.uri
                if (photos.size <= 1) {
                    onBack()
                    viewModel.requestSoftDeletePhotos(listOf(uriToDelete), context)
                } else {
                    coroutineScope.launch {
                        if (currentIdx >= photos.size - 1) {
                            try {
                                pagerState.scrollToPage((photos.size - 2).coerceAtLeast(0))
                            } catch (_: Exception) {}
                        }
                        viewModel.requestSoftDeletePhotos(listOf(uriToDelete), context)
                    }
                }
            } else {
                onBack()
            }
        },
        onSwipeUpDetails = {
            if (isSortingMode) {
                // 分拣模式：上滑立即打上记忆标签
                val targetTag = lastUsedTag ?: allTags.firstOrNull()
                if (targetTag != null) {
                    HapticFeedbackUtil.click(context)
                    viewModel.addTagToPhoto(currentPhoto.photoId, targetTag.tagId)
                    if (pagerState.currentPage < photos.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                } else {
                    showMetadataSheet = true
                }
            } else {
                showMetadataSheet = true
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // ==================== 照片 HorizontalPager ====================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val photo = photos.getOrNull(page)
                if (photo != null) {
                    PhotoViewerItem(
                        photo = photo,
                        screenWidthPx = screenWidthPx,
                        screenHeightPx = screenHeightPx,
                        isActive = safeIndex == page,
                        onSingleTap = { isControlsVisible = !isControlsVisible }
                    )
                }
            }

        // ==================== 顶部导航与操作栏 ====================
        val dismissProgress = com.example.phototag.ui.components.LocalViewerDismissProgress.current
        val chromeAlpha = (1f - dismissProgress * 1.5f).coerceIn(0f, 1f)

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(250)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = chromeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 返回按钮
                    com.example.phototag.ui.components.IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        onClick = onBack,
                        size = 36.dp
                    )

                    // 2. 居中拍摄日期时间与页码比例 (iOS 18 经典微胶囊中心排版)
                    val formattedDateTime = remember(currentPhoto.dateAdded) {
                        try {
                            SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()).format(Date(currentPhoto.dateAdded * 1000L))
                        } catch (_: Exception) {
                            ""
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedDateTime.ifBlank { currentPhoto.displayName },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (currentPhoto.isGif) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        "GIF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (currentPhoto.isVideo) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Videocam,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            currentPhoto.formattedDuration,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 居中副信息区：页码 · 分辨率 · 文件大小（默认常驻显示，方便秒级对比清晰度）
                        val resolutionText = remember(currentPhoto.width, currentPhoto.height, currentMetadata?.width, currentMetadata?.height) {
                            val w = if (currentPhoto.width > 0) currentPhoto.width else (currentMetadata?.width ?: 0)
                            val h = if (currentPhoto.height > 0) currentPhoto.height else (currentMetadata?.height ?: 0)
                            if (w > 0 && h > 0) "${w}×${h}" else ""
                        }

                        val fileSizeText = remember(currentPhoto.size, currentMetadata?.fileSizeBytes) {
                            val bytes = if (currentPhoto.size > 0L) currentPhoto.size else (currentMetadata?.fileSizeBytes ?: 0L)
                            if (bytes > 0L) ExifMetadataUtil.formatFileSize(bytes) else ""
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${safeIndex + 1} / ${photos.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )

                            if (resolutionText.isNotEmpty() || fileSizeText.isNotEmpty()) {
                                Text(
                                    text = "  ·  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.45f)
                                )
                                if (resolutionText.isNotEmpty()) {
                                    Text(
                                        text = resolutionText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                                if (fileSizeText.isNotEmpty()) {
                                    if (resolutionText.isNotEmpty()) {
                                        Text(
                                            text = "  ·  ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                    }
                                    Text(
                                        text = fileSizeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                            }
                        }
                    }

                    // 3. 右侧操作区：编辑 (图片可用) + 更多菜单 (···)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!currentPhoto.isVideo) {
                            com.example.phototag.ui.components.IosIconButton(
                                icon = Icons.Filled.AutoFixHigh,
                                contentDescription = "编辑图片",
                                tint = Color.White,
                                onClick = { viewModel.openImageEditor(currentPhoto) },
                                size = 36.dp
                            )
                            Spacer(Modifier.width(2.dp))
                        }

                        com.example.phototag.ui.components.IosIconButton(
                            icon = Icons.Filled.MoreVert,
                            contentDescription = "更多操作",
                            tint = Color.White,
                            onClick = {
                                HapticFeedbackUtil.click(context)
                                showMoreMenu = true
                            },
                            size = 36.dp
                        )
                    }
                }
            }
        }


        // ⚡ 闪速分拣模式悬浮提示条
        AnimatedVisibility(
            visible = isSortingMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 52.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .graphicsLayer { alpha = chromeAlpha }
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "分拣模式：上滑打标 · 下滑移入回收站 · 左右翻页跳过",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // ==================== 底部标签实时管理面板 ====================
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = chromeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 🎞️ 悬浮胶卷微缩条 (Bottom Filmstrip Scrubber)
                    FilmstripBar(
                        photos = photos,
                        currentIndex = pagerState.currentPage,
                        onSelectPhoto = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "照片标签 (${currentPhotoTags.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )

                        // 快速查看位置摘要（点击可展开完整元数据）
                        val locationSummary = currentPhoto.customLocation ?: currentMetadata?.address
                        if (!locationSummary.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showMetadataSheet = true }
                                    .padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = locationSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        // 1. "+ 打标" 按钮
                        item(key = "btn_add_tag_viewer") {
                            com.example.phototag.ui.components.IosButton(
                                text = "打标",
                                icon = Icons.Filled.Add,
                                variant = com.example.phototag.ui.components.IosButtonVariant.PRIMARY,
                                onClick = {
                                    viewModel.openViewerTaggingSheet(currentPhoto.photoId)
                                },
                                height = 30.dp
                            )
                        }

                        // 2. 当前已打的所有标签（点击右侧 × 即时解除）
                        items(
                            items = currentPhotoTags,
                            key = { it.tagId }
                        ) { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clip(RoundedCornerShape(50))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .clickable {
                                                viewModel.removeTagFromPhoto(currentPhoto.photoId, tag.tagId)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "移除标签",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.15f))
                    Spacer(Modifier.height(4.dp))

                    // ==================== 底部 5 大核心高频操作栏 (iOS 原生触控) ====================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 分享照片 (带隐私保护)
                        com.example.phototag.ui.components.IosIconButton(
                            icon = Icons.Filled.Share,
                            contentDescription = "分享照片",
                            tint = Color.White,
                            onClick = { showSharePrivacyDialog = true },
                            size = 38.dp
                        )

                        // 2. 收藏照片 ❤️ (一键关联/取消 #收藏 标签)
                        val isFavorite = currentPhotoTags.any { it.name == "#收藏" || it.name == "收藏" }
                        val favColor by animateColorAsState(
                            targetValue = if (isFavorite) Color(0xFFFF3B30) else Color.White,
                            animationSpec = tween(200),
                            label = "favColor"
                        )
                        com.example.phototag.ui.components.IosIconButton(
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏照片",
                            tint = favColor,
                            onClick = {
                                HapticFeedbackUtil.click(context)
                                viewModel.toggleFavoriteTag(currentPhoto.photoId)
                            },
                            size = 38.dp
                        )

                        // 3. 详细信息 ℹ️
                        com.example.phototag.ui.components.IosIconButton(
                            icon = Icons.Outlined.Info,
                            contentDescription = "照片详细信息",
                            tint = Color.White,
                            onClick = { showMetadataSheet = true },
                            size = 38.dp
                        )

                        // 4. 闪速分拣 ⚡
                        com.example.phototag.ui.components.IosIconButton(
                            icon = Icons.Filled.Bolt,
                            contentDescription = "闪速分拣模式",
                            tint = if (isSortingMode) Color(0xFFFF9F0A) else Color.White,
                            containerColor = if (isSortingMode) Color(0x33FF9F0A) else Color.Transparent,
                            onClick = {
                                isSortingMode = !isSortingMode
                                HapticFeedbackUtil.click(context)
                            },
                            size = 38.dp
                        )

                        // 5. 移入回收站
                        com.example.phototag.ui.components.IosIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "移入回收站",
                            tint = com.example.phototag.ui.theme.IosRedDark,
                            containerColor = Color(0x2A000000),
                            onClick = {
                                val currentIdx = pagerState.currentPage
                                val safeIdx = currentIdx.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
                                val targetPhoto = photos.getOrNull(safeIdx) ?: currentPhoto
                                val uriToDelete = targetPhoto.uri
                                if (photos.size <= 1) {
                                    onBack()
                                    viewModel.requestSoftDeletePhotos(listOf(uriToDelete), context)
                                } else {
                                    coroutineScope.launch {
                                        if (currentIdx >= photos.size - 1) {
                                            try {
                                                pagerState.scrollToPage((photos.size - 2).coerceAtLeast(0))
                                            } catch (_: Exception) {}
                                        }
                                        viewModel.requestSoftDeletePhotos(listOf(uriToDelete), context)
                                    }
                                }
                            },
                            size = 38.dp
                        )
                    }
                }
            }
        }

    }
}

    // ==================== ℹ️ 照片详细信息抽屉面板 ====================
    if (showMetadataSheet) {
        PhotoMetadataBottomSheet(
            metadata = currentMetadata,
            isLoading = isMetadataLoading,
            tags = currentPhotoTags,
            allAvailableTags = allTags,
            onToggleTag = { tagId, isAdded ->
                if (isAdded) {
                    viewModel.addTagToPhoto(currentPhoto.photoId, tagId)
                } else {
                    viewModel.removeTagFromPhoto(currentPhoto.photoId, tagId)
                }
            },
            onUpdateCustomLocation = { photoId, loc ->
                viewModel.updatePhotoCustomLocation(photoId, loc)
            },
            onRefreshMetadata = {
                coroutineScope.launch {
                    isMetadataLoading = true
                    val meta = ExifMetadataUtil.parseMetadata(context, currentPhoto)
                    currentMetadata = meta
                    if (meta.hasGps) {
                        viewModel.resolvePhotoAddressSilently(currentPhoto) { resolved ->
                            if (!resolved.isNullOrBlank()) {
                                currentMetadata = currentMetadata?.copy(address = resolved)
                                Toast.makeText(context, "成功解析高精地址：$resolved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "已成功提取 GPS 经纬度", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "经多通道深度扫描，该照片未记录拍摄 GPS 经纬度（可点击手动标注地点）", Toast.LENGTH_LONG).show()
                    }
                    isMetadataLoading = false
                }
            },
            onOpenSpoofSheet = { showSpoofBottomSheet = true },
            onDismiss = { showMetadataSheet = false }
        )
    }

    // ==================== 🛡️ 隐私脱敏分享对话框 ====================
    if (showSharePrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showSharePrivacyDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("分享照片") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "即将调起系统分享该图片。为了防止泄露家庭或常住地位置，可自动脱敏 GPS 经纬度数据。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, com.example.phototag.ui.components.getIosHairlineColor()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stripGpsPrivacy = !stripGpsPrivacy }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = stripGpsPrivacy,
                                onCheckedChange = { stripGpsPrivacy = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    "抹除 GPS 拍摄位置 (隐私保护)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "自动清除经纬度与海拔等敏感元数据",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 伪装信息分享快捷入口
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, com.example.phototag.ui.components.getIosHairlineColor()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSharePrivacyDialog = false
                                showSpoofBottomSheet = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.VpnKey, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("伪装信息分享 (自定义地标/机型)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("将地点伪装成首尔/釜山/济州岛/水原等", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                com.example.phototag.ui.components.IosButton(
                    text = "立即分享",
                    variant = com.example.phototag.ui.components.IosButtonVariant.PRIMARY,
                    onClick = {
                        showSharePrivacyDialog = false
                        viewModel.shareSinglePhoto(currentPhoto, stripLocation = stripGpsPrivacy, context = context)
                    },
                    height = 36.dp
                )
            },
            dismissButton = {
                com.example.phototag.ui.components.IosButton(
                    text = "取消",
                    variant = com.example.phototag.ui.components.IosButtonVariant.GHOST,
                    onClick = { showSharePrivacyDialog = false },
                    height = 36.dp
                )
            }
        )
    }

    // ==================== 🍏 iOS 18 风格磨砂毛玻璃浮动操作面板 ====================
    IosActionSheet(
        visible = showMoreMenu,
        onDismissRequest = { showMoreMenu = false },
        title = currentPhoto.displayName,
        subtitle = "拍摄于 ${SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()).format(Date(currentPhoto.dateAdded * 1000L))}",
        items = listOf(
            IosActionSheetItem(
                title = "设为相册封面",
                subtitle = "将当前照片设为标签相册的主视觉封面",
                icon = Icons.Filled.PhotoLibrary,
                iconTint = Color(0xFF0A84FF),
                onClick = { showSetCoverDialog = true }
            ),
            IosActionSheetItem(
                title = "复古胶片工坊",
                subtitle = "Leica / Dazz / 日系复古 14 款名机色彩与配件",
                icon = Icons.Filled.AutoFixHigh,
                iconTint = Color(0xFFFF9F0A),
                badge = "14款名机",
                onClick = { showFilmWorkshopSheet = true }
            ),
            IosActionSheetItem(
                title = "隐私伪装与 EXIF 伪造",
                subtitle = "自由定制拍摄机型、镜头规格与虚假坐标",
                icon = Icons.Filled.VpnKey,
                iconTint = Color(0xFF34C759),
                onClick = { showSpoofBottomSheet = true }
            ),
            IosActionSheetItem(
                title = "查看 EXIF 深度参数",
                subtitle = "光圈、快门、焦距、感光度精密铭牌",
                icon = Icons.Filled.Info,
                iconTint = Color(0xFF0A84FF),
                onClick = { showMetadataSheet = true }
            ),
            IosActionSheetItem(
                title = "抹除 GPS 隐私分享",
                subtitle = "抹除精确地理坐标与相机型号后安全分享",
                icon = Icons.Filled.Security,
                iconTint = Color(0xFF5856D6),
                onClick = { showSharePrivacyDialog = true }
            ),
            IosActionSheetItem(
                title = "移入回收站",
                subtitle = "安全暂存 7 天，期间可随时一键原样恢复",
                icon = Icons.Filled.Delete,
                iconTint = com.example.phototag.ui.theme.IosRedDark,
                isDestructive = true,
                onClick = {
                    val currentIdx = pagerState.currentPage
                    val safeIdx = currentIdx.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
                    val targetPhoto = photos.getOrNull(safeIdx) ?: currentPhoto
                    val uriToDelete = targetPhoto.uri
                    if (photos.size <= 1) {
                        onBack()
                        viewModel.requestSoftDeletePhotos(listOf(uriToDelete), context)
                    } else {
                        coroutineScope.launch {
                            if (currentIdx >= photos.size - 1) {
                                try {
                                    pagerState.scrollToPage((photos.size - 2).coerceAtLeast(0))
                                } catch (_: Exception) {}
                            }
                            viewModel.requestSoftDeletePhotos(listOf(uriToDelete), context)
                        }
                    }
                }
            )
        )
    )

    // ==================== 🎭 隐私伪装与 EXIF 深度伪造 BottomSheet ====================
    if (showSpoofBottomSheet) {
        ExifSpoofBottomSheet(
            onDismiss = { showSpoofBottomSheet = false },
            onConfirmShare = { config ->
                viewModel.shareSpoofedPhoto(currentPhoto, config, context)
            },
            onConfirmSaveCopy = { config ->
                viewModel.saveSpoofedPhotoCopy(currentPhoto, config, context)
            }
        )
    }

    // ==================== 单图打标 BottomSheet ====================
    if (uiState.viewerTaggingPhotoId == currentPhoto.photoId) {
        val currentTaggedIds = currentPhotoTags.map { it.tagId }.toSet()

        BatchTagBottomSheet(
            selectedCount = 1,
            categoriesWithTags = categoriesWithTags,
            recentTags = recentTags,
            topUsedTags = topUsedTags,
            getPhotoTagIds = { currentTaggedIds },
            onTagToggle = { tagId, add ->
                if (add) {
                    viewModel.addTagToPhoto(currentPhoto.photoId, tagId)
                } else {
                    viewModel.removeTagFromPhoto(currentPhoto.photoId, tagId)
                }
            },
            onCreateTag = { name, catId, parentId -> viewModel.createTag(name, catId, parentId = parentId) },
            onCreateCategory = { name -> viewModel.createCategory(name) },
            onDismiss = { viewModel.closeViewerTaggingSheet() }
        )
    }

    // ==================== 🖼️ 设为相册封面选择弹窗 ====================
    if (showSetCoverDialog) {
        AlertDialog(
            onDismissRequest = { showSetCoverDialog = false },
            title = {
                Text(
                    text = "设为相册封面",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (parentTagAlbums.isEmpty()) {
                    Text(
                        text = "暂无可用相册，请先在相册空间通过打标创建相册。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(parentTagAlbums, key = { it.parentTag.tagId }) { album ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setAlbumCustomCover(album.parentTag.tagId, currentPhoto.uri)
                                        Toast.makeText(context, "已成功设为「${album.parentTag.name}」的相册封面", Toast.LENGTH_SHORT).show()
                                        showSetCoverDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.parentTag.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "包含 ${album.totalPhotoCount} 张照片",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSetCoverDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // ==================== 🎞️ 单图复古胶片冲印工坊 BottomSheet ====================
    if (showFilmWorkshopSheet) {
        com.example.phototag.ui.components.BatchFilterBottomSheet(
            selectedCount = 1,
            onSelectPreset = { preset, options ->
                showFilmWorkshopSheet = false
                viewModel.applyFilmFilterToSinglePhoto(
                    photo = currentPhoto,
                    context = context,
                    preset = preset,
                    options = options
                )
            },
            onDismiss = { showFilmWorkshopSheet = false }
        )
    }
}

