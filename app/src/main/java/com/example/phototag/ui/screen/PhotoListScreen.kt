package com.example.phototag.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.example.phototag.ui.components.IosHairlineDivider
import com.example.phototag.ui.components.IosIconButton
import com.example.phototag.ui.components.getIosGlassBackground
import com.example.phototag.ui.components.getIosHairlineColor
import com.example.phototag.ui.MediaFacetFilter
import com.example.phototag.ui.components.getIosPillBackground
import com.example.phototag.util.HapticFeedbackUtil
import com.example.phototag.util.ThumbnailPreloader
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import com.example.phototag.ui.theme.IosAmber
import com.example.phototag.ui.theme.IosAmberDark
import com.example.phototag.ui.theme.IosBlue
import com.example.phototag.ui.theme.IosBlueDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.ui.FilterMode
import com.example.phototag.ui.MainViewModel
import com.example.phototag.ui.components.BatchTagBottomSheet
import com.example.phototag.ui.components.FastScroller
import com.example.phototag.ui.components.FloatingCapsuleBar
import com.example.phototag.ui.components.InstagramPeekOverlay
import com.example.phototag.ui.components.MinimalSearchBar
import com.example.phototag.ui.components.PhotoGridItem
import com.example.phototag.ui.components.PhotoMetadataBottomSheet
import com.example.phototag.ui.components.TagActionBottomSheet
import com.example.phototag.ui.components.TagChip
import com.example.phototag.ui.components.TagManageDialog
import com.example.phototag.ui.components.TimelineDateHeader
import com.example.phototag.util.CoilConfig
import com.example.phototag.ui.components.TransferTagBottomSheet
import com.example.phototag.ui.components.bounceScale
import com.example.phototag.ui.components.dragSelectModifier
import com.example.phototag.ui.components.pinchToResizeGrid
import com.example.phototag.util.ExifMetadataUtil
import com.example.phototag.util.PhotoMetadata
import com.example.phototag.util.ShakeUndoManager
import coil3.imageLoader
import com.example.phototag.util.TimelineGridItem
import com.example.phototag.util.UndoableAction

/**
 * Tab 1 — 全部照片（支持时间轴日期吸顶分组 + 侧边快速滑动条 FastScroller + 滑动连选 + 捏合列数缩放）。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoListScreen(
    viewModel: MainViewModel,
    innerPadding: PaddingValues,
    onDeleteRequest: () -> Unit,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredPhotos by viewModel.filteredPhotos.collectAsStateWithLifecycle()
    val timelineItems by viewModel.timelineItems.collectAsStateWithLifecycle()
    val untaggedCount by viewModel.untaggedCount.collectAsStateWithLifecycle()
    val mediaCounts by viewModel.mediaCounts.collectAsStateWithLifecycle()
    val favoritePhotoIds by viewModel.favoritePhotoIds.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val lastUsedTag by viewModel.lastUsedTag.collectAsStateWithLifecycle()
    val rootTags by viewModel.rootTags.collectAsStateWithLifecycle()
    val childTagsMap by viewModel.childTagsMap.collectAsStateWithLifecycle()
    val recentTags by viewModel.recentTags.collectAsStateWithLifecycle()
    val topUsedTags by viewModel.topUsedTags.collectAsStateWithLifecycle()
    val categoriesWithTags by viewModel.categoriesWithTags.collectAsStateWithLifecycle()
    val allPhotosWithTags by viewModel.allPhotosWithTags.collectAsStateWithLifecycle()
    val parentTagAlbums by viewModel.parentTagAlbums.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val gridItemSizePx = remember(configuration.screenWidthDp, gridColumns, density) {
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        (screenWidthPx / gridColumns.coerceAtLeast(1)).coerceIn(180, 480)
    }

    var peekPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    var metadataPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    var parsedMetadata by remember { mutableStateOf<PhotoMetadata?>(null) }
    var isMetadataLoading by remember { mutableStateOf(false) }
    var showSetCoverPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterRow by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // 🔍 捏合缩放网格列数反馈气泡状态
    var showColumnBadge by remember { mutableStateOf(false) }
    var badgeColumnValue by remember { mutableStateOf(gridColumns) }
    var lastSeenColumns by remember { mutableStateOf(gridColumns) }

    LaunchedEffect(gridColumns) {
        if (gridColumns != lastSeenColumns) {
            lastSeenColumns = gridColumns
            badgeColumnValue = gridColumns
            showColumnBadge = true
            kotlinx.coroutines.delay(1200)
            showColumnBadge = false
        }
    }

    // 搜索状态下拦截返回键优先退出搜索
    BackHandler(enabled = (isSearchExpanded || uiState.searchQuery.isNotBlank()) && !uiState.isMultiSelectMode) {
        viewModel.onSearchQueryChange("")
        isSearchExpanded = false
        focusManager.clearFocus()
    }

    // 摇一摇极速撤销管理器
    val shakeUndoManager = remember { ShakeUndoManager(context) }
    DisposableEffect(Unit) {
        shakeUndoManager.startListening()
        onDispose { shakeUndoManager.stopListening() }
    }
    val pendingUndoAction by shakeUndoManager.pendingUndoAction.collectAsState()

    // ⚡ 监听 Tab 点击回顶与双击直达底部事件
    LaunchedEffect(Unit) {
        viewModel.scrollEvents.collect { target ->
            when (target) {
                MainViewModel.ScrollTarget.TOP -> gridState.animateScrollToItem(0)
                MainViewModel.ScrollTarget.BOTTOM -> {
                    val lastIndex = (timelineItems.size - 1).coerceAtLeast(0)
                    gridState.animateScrollToItem(lastIndex)
                }
            }
        }
    }

    // ⚡ 仅当用户真正主动切换标签、子标签、筛选或搜索关键词时，才自动回滚到网格顶部（避免切 Tab 重新进入时误滚到顶部）
    val filterKey = "${uiState.selectedRootTagId}_${uiState.selectedChildTagId}_${uiState.activeFilterTagIds}_${uiState.isUntaggedFilterActive}_${uiState.searchQuery}"
    var previousFilterKey by rememberSaveable { mutableStateOf(filterKey) }
    LaunchedEffect(filterKey) {
        if (previousFilterKey != filterKey) {
            previousFilterKey = filterKey
            gridState.scrollToItem(0)
            kotlinx.coroutines.delay(30)
            gridState.scrollToItem(0)
        }
    }

    // 确保冷启动与新添照片同步到达时，若用户尚未手动滑动浏览，默认始终定格在最新照片的最顶部 (Index 0)，绝不停留在昨天日期部分
    var hasUserManuallyScrolled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            hasUserManuallyScrolled = true
        }
    }

    val topItemKey = remember(timelineItems) {
        when (val first = timelineItems.firstOrNull()) {
            is TimelineGridItem.DateHeader -> first.headerId
            is TimelineGridItem.Photo -> first.photo.photoId.toString()
            null -> ""
        }
    }

    LaunchedEffect(topItemKey) {
        if (topItemKey.isNotBlank() && !hasUserManuallyScrolled) {
            gridState.scrollToItem(0)
        }
    }

    LaunchedEffect(uiState.isScanning) {
        if (!uiState.isScanning && !hasUserManuallyScrolled && timelineItems.isNotEmpty()) {
            gridState.scrollToItem(0)
        }
    }

    val isFiltered = uiState.activeFilterTagIds.isNotEmpty() || uiState.searchQuery.isNotBlank() || uiState.isUntaggedFilterActive || uiState.selectedRootTagId != null
    val isAllSelected = !isFiltered && !uiState.isUntaggedFilterActive

    val isDarkTheme = isSystemInDarkTheme()
    val amberTint = if (isDarkTheme) IosAmberDark else IosAmber

    val infiniteTransition = rememberInfiniteTransition(label = "scan_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_rotation"
    )

    var isPullRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isScanning) {
        if (!uiState.isScanning) {
            isPullRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ==================== 🍏 磨砂玻璃质感顶栏 (iOS 极简一体化) ====================
            Surface(
                color = getIosGlassBackground(),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // ⚡ 轻触顶部状态栏安全区平滑回顶 (iOS 经典交互)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(innerPadding.calculateTopPadding().coerceAtLeast(24.dp))
                            .align(Alignment.TopCenter)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                HapticFeedbackUtil.click(context)
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                            }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                    val topBarMode = when {
                        uiState.isMultiSelectMode -> 0
                        isSearchExpanded || uiState.searchQuery.isNotBlank() -> 1
                        else -> 2
                    }

                    AnimatedContent(
                        targetState = topBarMode,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { -it / 3 })
                                .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutVertically(animationSpec = tween(160)) { -it / 3 })
                        },
                        label = "TopBarCrossfade"
                    ) { mode ->
                        when (mode) {
                            0 -> {
                                MultiSelectTopBar(
                                    selectedCount = uiState.selectedPhotoIds.size,
                                    totalCount = filteredPhotos.size,
                                    onSelectAll = { viewModel.selectAll(filteredPhotos.map { it.photoId }) },
                                    onInvertSelect = { viewModel.invertSelection(filteredPhotos.map { it.photoId }) },
                                    onExit = { viewModel.exitMultiSelectMode() },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                            1 -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MinimalSearchBar(
                                        query = uiState.searchQuery,
                                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                                        onClearQuery = { viewModel.onSearchQueryChange("") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = "取消",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (isDarkTheme) IosBlueDark else IosBlue,
                                        modifier = Modifier
                                            .bounceScale()
                                            .clickable {
                                                HapticFeedbackUtil.click(context)
                                                viewModel.onSearchQueryChange("")
                                                isSearchExpanded = false
                                                focusManager.clearFocus()
                                            }
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            else -> {
                                val collapseFraction by remember {
                                    derivedStateOf {
                                        if (gridState.firstVisibleItemIndex > 0) 1f
                                        else (gridState.firstVisibleItemScrollOffset / with(density) { 90.dp.toPx() }).coerceIn(0f, 1f)
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 18.dp, end = 14.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 🏆 Samsung One UI 大标题下沉与平滑收缩（完全基于 GPU graphicsLayer 调度，零 Recomposition 与测量损耗）
                                        Column(
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .graphicsLayer {
                                                    val scale = (1f - collapseFraction * 0.22f).coerceIn(0.78f, 1f)
                                                    scaleX = scale
                                                    scaleY = scale
                                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                                    translationY = -collapseFraction * 8f
                                                }
                                        ) {
                                            Text(
                                                text = "照片",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                letterSpacing = (-0.5).sp
                                            )
                                            Text(
                                                text = if (isFiltered) "已筛选 · ${filteredPhotos.size} 张照片" else "共 ${mediaCounts.total} 张照片 · ${mediaCounts.videos} 个视频",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.graphicsLayer {
                                                    alpha = (1f - collapseFraction * 1.8f).coerceIn(0f, 0.75f)
                                                }
                                            )
                                        }

                                        // 右侧：iOS 18 / One UI 极简微胶囊操作簇 (搜索、筛选、选择、更多)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 1. 搜索
                                            IosIconButton(
                                                icon = Icons.Filled.Search,
                                                contentDescription = "搜索照片",
                                                onClick = {
                                                    HapticFeedbackUtil.click(context)
                                                    isSearchExpanded = true
                                                },
                                                size = 36.dp
                                            )

                                            // 2. 筛选 (支持指示当前筛选状态)
                                            val isFilterActive = isFiltered || uiState.mediaFacetFilter != MediaFacetFilter.ALL
                                            Surface(
                                                shape = RoundedCornerShape(percent = 50),
                                                color = if (showFilterRow || isFilterActive) (if (isDarkTheme) Color(0x330A84FF) else Color(0x22007AFF)) else getIosPillBackground(),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    0.5.dp,
                                                    if (showFilterRow || isFilterActive) (if (isDarkTheme) IosBlueDark else IosBlue) else getIosHairlineColor()
                                                ),
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(percent = 50))
                                                    .bounceScale()
                                                    .clickable(onClick = {
                                                        HapticFeedbackUtil.click(context)
                                                        showFilterRow = !showFilterRow
                                                    })
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Tune,
                                                        contentDescription = "筛选",
                                                        tint = if (showFilterRow || isFilterActive) (if (isDarkTheme) IosBlueDark else IosBlue) else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        text = if (isFilterActive) "已筛选" else "筛选",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (showFilterRow || isFilterActive) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (showFilterRow || isFilterActive) (if (isDarkTheme) IosBlueDark else IosBlue) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            // 3. 选择 (经典黑/白高级药丸)
                                            Surface(
                                                shape = RoundedCornerShape(percent = 50),
                                                color = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF1C1C1E),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDarkTheme) Color(0x33FFFFFF) else Color(0x14000000)),
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(percent = 50))
                                                    .bounceScale()
                                                    .clickable(onClick = {
                                                        HapticFeedbackUtil.click(context)
                                                        viewModel.enterMultiSelectMode()
                                                    })
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 13.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "选择",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDarkTheme) Color(0xFF000000) else Color(0xFFFFFFFF)
                                                    )
                                                }
                                            }

                                            // 4. 更多菜单 (三点图标)
                                            Box {
                                                IosIconButton(
                                                    icon = Icons.Filled.MoreVert,
                                                    contentDescription = "更多选项",
                                                    onClick = {
                                                        HapticFeedbackUtil.click(context)
                                                        showMoreMenu = true
                                                    },
                                                    size = 36.dp
                                                )

                                                DropdownMenu(
                                                    expanded = showMoreMenu,
                                                    onDismissRequest = { showMoreMenu = false },
                                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("极速整理${if (untaggedCount > 0) " ($untaggedCount)" else ""}") },
                                                        leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = amberTint) },
                                                        onClick = {
                                                            showMoreMenu = false
                                                            viewModel.openQuickTagMode()
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("相册瘦身工坊") },
                                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                                        onClick = {
                                                            showMoreMenu = false
                                                            viewModel.openCleanupLab()
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("同步本地相册") },
                                                        leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                                        onClick = {
                                                            showMoreMenu = false
                                                            viewModel.syncPhotos()
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("相册实验室设置") },
                                                        leadingIcon = { Icon(Icons.Filled.Style, contentDescription = null) },
                                                        onClick = {
                                                            showMoreMenu = false
                                                            viewModel.openGalleryLabs()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 搜索快捷建议条 (动态真实地点/年份/类型联想) ====================
                    if (uiState.searchQuery.isNotEmpty()) {
                        val dynamicSuggestions = remember(allPhotosWithTags) {
                            val cities = allPhotosWithTags
                                .mapNotNull { item ->
                                    val loc = item.photo.customLocation?.trim()?.takeIf { it.isNotBlank() }
                                        ?: item.photo.address?.let { addr ->
                                            val match = Regex("""(?:省|自治区)?([^省市区]+(?:市|区|县))""").find(addr)?.groupValues?.getOrNull(1)
                                            match ?: addr.take(4)
                                        }
                                    loc
                                }
                                .filter { it.isNotBlank() && it.length in 2..8 }
                                .groupingBy { it }
                                .eachCount()
                                .entries
                                .sortedByDescending { it.value }
                                .take(4)
                                .map { it.key }

                            val baseFilters = listOf("未打标", "视频", "GIF", "2026", "2025")
                            (baseFilters + cities).distinct().take(8)
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(dynamicSuggestions) { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = getIosPillBackground(),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
                                    modifier = Modifier
                                        .bounceScale()
                                        .clickable {
                                            HapticFeedbackUtil.click(context)
                                            if (suggestion == "未打标") {
                                                viewModel.onSearchQueryChange("")
                                                viewModel.toggleUntaggedFilter()
                                            } else {
                                                viewModel.onSearchQueryChange(suggestion)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ==================== 统一高阶媒体与标签过滤胶囊流 (按需展开，默认纯净沉浸) ====================
                    val activeFacet = uiState.mediaFacetFilter
                    val distinctRootTags = remember(rootTags) { rootTags.distinctBy { it.name } }
                    val isFacetActive = activeFacet != MediaFacetFilter.ALL
                    val isAnyFilterActive = isFiltered || isFacetActive
                    val shouldShowFilters = !uiState.isMultiSelectMode && uiState.searchQuery.isEmpty() && (showFilterRow || isAnyFilterActive)

                    AnimatedVisibility(
                        visible = shouldShowFilters,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(200)),
                        exit = fadeOut(tween(160)) + slideOutVertically(tween(160))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 6.dp)
                            ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 1. 【全部】胶囊（当无任何筛选与 Facet 时高亮）
                                item(key = "chip_unified_all") {
                                    TagChip(
                                        name = "全部",
                                        isSelected = !isAnyFilterActive,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.clearFilters()
                                            viewModel.setMediaFacetFilter(MediaFacetFilter.ALL)
                                        }
                                    )
                                }

                                // 2. 【清除筛选】（当存在任何筛选时显示）
                                if (isAnyFilterActive) {
                                    item(key = "chip_unified_reset") {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(50))
                                                .bounceScale()
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.clearFilters()
                                                    viewModel.setMediaFacetFilter(MediaFacetFilter.ALL)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.FilterAltOff,
                                                    contentDescription = "清除筛选",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    "清除筛选",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. 媒体智能分类项
                                val facetItems = listOf(
                                    MediaFacetFilter.VIDEO to mediaCounts.videos,
                                    MediaFacetFilter.SCREENSHOT to mediaCounts.screenshots,
                                    MediaFacetFilter.FAVORITE to mediaCounts.favorites,
                                    MediaFacetFilter.UNTAGGED to untaggedCount
                                )

                                items(facetItems, key = { it.first.name }) { (facet, count) ->
                                    val isSelected = activeFacet == facet || (facet == MediaFacetFilter.UNTAGGED && uiState.isUntaggedFilterActive)
                                    val label = if (count > 0) "${facet.label} $count" else facet.label
                                    TagChip(
                                        name = label,
                                        isSelected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (facet == MediaFacetFilter.UNTAGGED) {
                                                viewModel.toggleUntaggedFilter()
                                            } else {
                                                viewModel.setMediaFacetFilter(facet)
                                            }
                                        }
                                    )
                                }

                                if (mediaCounts.raws > 0) {
                                    item(key = "facet_raw") {
                                        val isSelected = activeFacet == MediaFacetFilter.RAW
                                        TagChip(
                                            name = "RAW ${mediaCounts.raws}",
                                            isSelected = isSelected,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setMediaFacetFilter(MediaFacetFilter.RAW)
                                            }
                                        )
                                    }
                                }

                                // 4. 垂直分割细线
                                item(key = "divider_facets_tags") {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .width(1.dp)
                                            .height(16.dp)
                                            .background(getIosHairlineColor())
                                    )
                                }

                                // 5. 【+ 新建】标签胶囊
                                item(key = "btn_create_tag") {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = getIosPillBackground(),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(50))
                                            .bounceScale()
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.showCreateTagDialog()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = "新建标签",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                "新建",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                }

                                // 6. 所有顶级父标签
                                items(
                                    items = distinctRootTags,
                                    key = { it.tagId }
                                ) { tag ->
                                    val isSelected = uiState.selectedRootTagId == tag.tagId || tag.tagId in uiState.activeFilterTagIds
                                    TagChip(
                                        name = tag.name,
                                        colorHex = tag.colorHex,
                                        isSelected = isSelected,
                                        isSystem = tag.isSystem,
                                        onClick = { viewModel.selectRootTag(tag.tagId) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.openTagManagement(tag)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                    // ==================== 子标签二级展开联动栏 ====================
                    val activeRootTag = remember(uiState.selectedRootTagId, rootTags) {
                        rootTags.find { it.tagId == uiState.selectedRootTagId }
                    }
                    val currentChildTags = remember(uiState.selectedRootTagId, childTagsMap) {
                        uiState.selectedRootTagId?.let { childTagsMap[it] } ?: emptyList()
                    }

                    AnimatedVisibility(
                        visible = activeRootTag != null && currentChildTags.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = getIosPillBackground(),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${activeRootTag?.name?.trimStart('#')} ·",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 【全部 (父标签+子标签)】
                                    item(key = "sub_all_${activeRootTag?.tagId}") {
                                        val isSubAllSelected = uiState.selectedChildTagId == null
                                        TagChip(
                                            name = "全部",
                                            isSelected = isSubAllSelected,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.selectChildTag(null)
                                            }
                                        )
                                    }

                                    // 【仅父标签】(例如: 仅 i-dle)
                                    if (activeRootTag != null) {
                                        item(key = "sub_parent_only_${activeRootTag.tagId}") {
                                            val isParentOnlySelected = uiState.selectedChildTagId == -1L
                                            val cleanName = activeRootTag.name.trimStart('#')
                                            TagChip(
                                                name = "仅 $cleanName",
                                                colorHex = activeRootTag.colorHex,
                                                isSelected = isParentOnlySelected,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.selectChildTag(if (isParentOnlySelected) null else -1L)
                                                }
                                            )
                                        }
                                    }

                                    // 每个子标签
                                    items(
                                        items = currentChildTags,
                                        key = { it.tagId }
                                    ) { child ->
                                        val isChildSelected = uiState.selectedChildTagId == child.tagId
                                        TagChip(
                                            name = child.name,
                                            colorHex = child.colorHex,
                                            isSelected = isChildSelected,
                                            isSystem = child.isSystem,
                                            onClick = { viewModel.selectChildTag(child.tagId) },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.openTagManagement(child)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. 【AND / OR 逻辑切换开关】
                    AnimatedVisibility(
                        visible = uiState.activeFilterTagIds.size >= 2,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            color = getIosPillBackground(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已选 ${uiState.activeFilterTagIds.size} 个标签",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                val activeSwitchColor = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
                                val activeSwitchText = if (isDarkTheme) Color.Black else Color.White
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(getIosPillBackground())
                                        .padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (uiState.filterMode == FilterMode.AND)
                                                    activeSwitchColor
                                                else
                                                    Color.Transparent
                                            )
                                            .bounceScale()
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setFilterMode(FilterMode.AND)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "且 (AND)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (uiState.filterMode == FilterMode.AND) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.filterMode == FilterMode.AND)
                                                activeSwitchText
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (uiState.filterMode == FilterMode.OR)
                                                    activeSwitchColor
                                                else
                                                    Color.Transparent
                                            )
                                            .bounceScale()
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setFilterMode(FilterMode.OR)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "或 (OR)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (uiState.filterMode == FilterMode.OR) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.filterMode == FilterMode.OR)
                                                activeSwitchText
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 底部发丝级分割线，直连照片网格，缝隙为零
                    IosHairlineDivider()
                }
            }
        }

            // ==================== 时间轴网格 + 侧边 FastScroller ====================
            Box(modifier = Modifier.weight(1f)) {
                // 浮动吸顶时间轴状态：二分检索当前可视区域所属的日期分组（O(log K) 极速计算，0 帧延迟与每像素重组损耗）
                val headerIndices = remember(timelineItems) {
                    val list = ArrayList<Int>()
                    timelineItems.forEachIndexed { index, item ->
                        if (item is TimelineGridItem.DateHeader) list.add(index)
                    }
                    list
                }

                val currentStickyHeader by remember(timelineItems, headerIndices) {
                    derivedStateOf {
                        val firstIdx = gridState.firstVisibleItemIndex
                        if (firstIdx in timelineItems.indices && headerIndices.isNotEmpty()) {
                            val binarySearchIndex = headerIndices.binarySearch(firstIdx)
                            val headerIdx = if (binarySearchIndex >= 0) {
                                headerIndices[binarySearchIndex]
                            } else {
                                val insertionPoint = -(binarySearchIndex + 1)
                                val targetIndex = insertionPoint - 1
                                if (targetIndex in headerIndices.indices) headerIndices[targetIndex] else -1
                            }
                            if (headerIdx in timelineItems.indices) {
                                timelineItems[headerIdx] as? TimelineGridItem.DateHeader
                            } else null
                        } else null
                    }
                }

                // ⚡ 消除每一像素滚动带来的整屏重组：隔离吸顶胶囊显隐判断，仅在滚过首项后呈现
                val isStickyPillVisible by remember(currentStickyHeader) {
                    derivedStateOf {
                        currentStickyHeader != null && gridState.firstVisibleItemIndex > 0
                    }
                }

                // ⚡ 120Hz 丝滑渲染通道：
                // 1. 滚动进行中（高速飞滑与时间轴拖拽）：100% CPU/GPU 与 Binder 算力让位给当前视口解码与渲染，彻底杜绝白块与掉帧。
                // 2. 滚动停驻静止时（防抖 50ms）：智能预热视口前后 1~2 屏的缩略图，杜绝多线程竞争造成的卡顿风暴。
                var isFastScrollerDragging by remember { mutableStateOf(false) }

                if (!uiState.hasMediaPermission) {
                    EmptyPermission()
                } else if (filteredPhotos.isEmpty() && uiState.isScanning) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "正在极速载入相册照片...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (filteredPhotos.isEmpty()) {
                    EmptyPhotos(
                        hasFilter = isFiltered,
                        onClearFilters = { viewModel.clearFilters() }
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isPullRefreshing,
                        onRefresh = {
                            isPullRefreshing = true
                            viewModel.syncPhotos()
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(650)
                                isPullRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(
                                top = 0.dp,
                                bottom = innerPadding.calculateBottomPadding() + 92.dp,
                                start = 2.dp,
                                end = 2.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .dragSelectModifier(
                                    gridState = gridState,
                                    items = timelineItems,
                                    isMultiSelectMode = uiState.isMultiSelectMode,
                                    selectedPhotoIds = uiState.selectedPhotoIds,
                                    onEnterMultiSelect = { photoId ->
                                        viewModel.enterMultiSelectMode(photoId)
                                    },
                                    onDragSelectChange = { selectedIds ->
                                        viewModel.updateDragSelection(selectedIds)
                                    }
                                )
                                .pinchToResizeGrid(
                                    currentColumns = gridColumns,
                                    onColumnsChanged = { newCols: Int ->
                                        viewModel.setGridColumns(newCols)
                                    }
                                )
                        ) {
                            items(
                                count = timelineItems.size,
                                key = { index ->
                                    when (val item = timelineItems.getOrNull(index)) {
                                        is TimelineGridItem.DateHeader -> item.headerId
                                        is TimelineGridItem.Photo -> item.photo.photoId
                                        null -> "unknown_timeline_key_$index"
                                    }
                                },
                                span = { index ->
                                    when (timelineItems.getOrNull(index)) {
                                        is TimelineGridItem.DateHeader -> GridItemSpan(maxLineSpan)
                                        else -> GridItemSpan(1)
                                    }
                                },
                                contentType = { index ->
                                    when (timelineItems.getOrNull(index)) {
                                        is TimelineGridItem.DateHeader -> "header"
                                        else -> "photo"
                                    }
                                }
                            ) { index ->
                                val item = timelineItems.getOrNull(index) ?: return@items
                                when (item) {
                                    is TimelineGridItem.DateHeader -> {
                                        val isGroupAllSelected = if (uiState.isMultiSelectMode && item.photoIds.isNotEmpty()) {
                                            item.photoIds.all { it in uiState.selectedPhotoIds }
                                        } else false
                                        TimelineDateHeader(
                                            title = item.dateTitle,
                                            subtitle = item.dateSubtitle,
                                            count = item.photoCount,
                                            isMultiSelectMode = uiState.isMultiSelectMode,
                                            isAllSelected = isGroupAllSelected,
                                            onToggleSelectGroup = {
                                                viewModel.toggleGroupSelection(item.photoIds)
                                            }
                                        )
                                    }
                                    is TimelineGridItem.Photo -> {
                                        val photo = item.photo
                                        val isSelected = photo.photoId in uiState.selectedPhotoIds

                                        PhotoGridItem(
                                            photo = photo,
                                            isMultiSelectMode = uiState.isMultiSelectMode,
                                            isSelected = isSelected,
                                            isFavorite = photo.photoId in favoritePhotoIds,
                                            sizePx = gridItemSizePx,
                                            onClick = {
                                                if (uiState.isMultiSelectMode) {
                                                    viewModel.togglePhotoSelection(photo.photoId)
                                                } else {
                                                    val photoIndex = filteredPhotos.indexOfFirst { it.photoId == photo.photoId }
                                                    viewModel.openPhotoViewer(filteredPhotos, photoIndex.coerceAtLeast(0))
                                                }
                                            },
                                            onLongClick = {
                                                if (!uiState.isMultiSelectMode) {
                                                    peekPhoto = photo
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ==================== 屏幕右侧快速滑动指示器 (FastScroller) ====================
                        FastScroller(
                            gridState = gridState,
                            timelineItems = timelineItems,
                            isMultiSelectMode = uiState.isMultiSelectMode,
                            onDraggingChanged = { isFastScrollerDragging = it },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )

                        // ==================== 浮动吸顶时间轴微胶囊 (Floating Sticky Date Header) ====================
                        TimelineStickyDatePill(
                            header = currentStickyHeader,
                            visible = isStickyPillVisible,
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                val target = currentStickyHeader ?: return@TimelineStickyDatePill
                                coroutineScope.launch {
                                    val headerIdx = timelineItems.indexOf(target)
                                    if (headerIdx >= 0) {
                                        gridState.animateScrollToItem(headerIdx)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ==================== 🔍 捏合缩放列数动态气泡提示 (iOS 18 风格) ====================
        androidx.compose.animation.AnimatedVisibility(
            visible = showColumnBadge,
            enter = fadeIn(animationSpec = tween(160)) + scaleIn(initialScale = 0.88f),
            exit = fadeOut(animationSpec = tween(240)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 64.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDarkTheme) Color(0xDD1C1C1E) else Color(0xEEFFFFFF),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = if (isDarkTheme) IosBlueDark else IosBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    val label = when (badgeColumnValue) {
                        2 -> "双列大图 (2列)"
                        3 -> "标准视图 (3列)"
                        4 -> "紧凑平铺 (4列)"
                        5 -> "密集微缩 (5列)"
                        else -> "${badgeColumnValue} 列视图"
                    }
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ==================== 🍏 底部悬浮多选操作胶囊 (FloatingCapsuleBar) ====================
        FloatingCapsuleBar(
            visible = uiState.isMultiSelectMode && uiState.selectedPhotoIds.isNotEmpty(),
            selectedCount = uiState.selectedPhotoIds.size,
            lastUsedTag = lastUsedTag,
            recentTags = recentTags,
            onApplyDirectTag = { tag ->
                viewModel.applyTagToPhotos(uiState.selectedPhotoIds, tag)
            },
            onTagClick = { viewModel.showBatchTagSheet() },
            onFavoriteClick = { viewModel.batchToggleFavoriteSelectedPhotos() },
            onFilterClick = { viewModel.openBatchFilterSheet() },
            onTransferClick = { viewModel.openTransferTagSheet() },
            onShareClick = { viewModel.shareSelectedPhotos(context = context) },
            onDeleteClick = { onDeleteRequest() },
            onCloseSelection = { viewModel.exitMultiSelectMode() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ==================== 🎞️ 批量胶片调色弹窗 ====================
    if (uiState.showBatchFilterSheet) {
        com.example.phototag.ui.components.BatchFilterBottomSheet(
            selectedCount = uiState.selectedPhotoIds.size,
            onSelectPreset = { preset, options ->
                viewModel.batchApplyFilmFilter(context, preset, options)
            },
            onDismiss = { viewModel.closeBatchFilterSheet() }
        )
    }

    // ==================== ⚡ 摇一摇极速撤销对话框 ====================
    if (pendingUndoAction != null) {
        val action = pendingUndoAction!!
        AlertDialog(
            onDismissRequest = { shakeUndoManager.dismissPendingUndo() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("撤销操作", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "检测到摇一摇手势！是否撤销刚才的「${action.title}」？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val undoAct = shakeUndoManager.popAction()
                        if (undoAct != null) {
                            when (undoAct) {
                                is UndoableAction.BatchDelete -> viewModel.restorePhotos(undoAct.uris)
                                is UndoableAction.SoftDelete -> viewModel.restorePhotos(listOf(undoAct.uri))
                                is UndoableAction.BatchTag -> viewModel.removeTagFromPhotos(undoAct.uris, undoAct.tagId)
                            }
                        }
                    }
                ) {
                    Text("确定撤销", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { shakeUndoManager.dismissPendingUndo() }) {
                    Text("忽略")
                }
            }
        )
    }

    // ==================== 批量打标弹窗 ====================
    if (uiState.showBatchTagSheet) {
        val taggedIds: Set<Long> = allPhotosWithTags
            .filter { it.photo.photoId in uiState.selectedPhotoIds }
            .flatMap { it.tags.map { tag -> tag.tagId } }
            .toSet()

        BatchTagBottomSheet(
            selectedCount = uiState.selectedPhotoIds.size,
            categoriesWithTags = categoriesWithTags,
            recentTags = recentTags,
            topUsedTags = topUsedTags,
            getPhotoTagIds = { taggedIds },
            onTagToggle = { tagId, add -> viewModel.batchToggleTag(tagId, add) },
            onClearAllTags = { viewModel.batchClearAllTagsFromSelectedPhotos() },
            onCreateTag = { name, catId, parentId -> viewModel.createTag(name, catId, parentId = parentId) },
            onCreateCategory = { name -> viewModel.createCategory(name) },
            onDismiss = { viewModel.hideBatchTagSheet() }
        )
    }

    // ==================== 🔄 标签转移与替换弹窗 ====================
    if (uiState.showTransferTagSheet) {
        val selectedPhotosTags = allPhotosWithTags
            .filter { it.photo.photoId in uiState.selectedPhotoIds }
            .flatMap { it.tags }
            .distinctBy { it.tagId }

        TransferTagBottomSheet(
            selectedPhotoCount = uiState.selectedPhotoIds.size,
            selectedPhotosTags = selectedPhotosTags,
            categoriesWithTags = categoriesWithTags,
            allTags = allTags,
            onTransfer = { fromTagId, toTagId ->
                viewModel.transferPhotosTag(fromTagId, toTagId)
            },
            onCreateTag = { name, catId, parentId ->
                viewModel.createTag(name, catId, parentId = parentId)
            },
            onDismiss = { viewModel.closeTransferTagSheet() }
        )
    }

    // ==================== 新建标签弹窗 ====================
    if (uiState.showCreateTagDialog) {
        TagManageDialog(
            categoriesWithTags = categoriesWithTags,
            rootTags = rootTags,
            onCreateTag = { name, catId, colorHex, parentId, newCat ->
                viewModel.createTag(name, catId, colorHex, parentId, newCat)
            },
            onCreateCategory = { name -> viewModel.createCategory(name) },
            onDismiss = { viewModel.hideCreateTagDialog() }
        )
    }

    // ==================== 标签深度管理 BottomSheet (长按触发：重命名、换色、合并、删除) ====================
    uiState.managingTag?.let { tag ->
        TagActionBottomSheet(
            tag = tag,
            allTags = allTags,
            categoriesWithTags = categoriesWithTags,
            rootTags = rootTags,
            onUpdateTag = { tagId, newName, newColorHex, categoryId, parentId, newCat ->
                viewModel.updateTagFull(tagId, newName, newColorHex, categoryId, parentId, newCat)
            },
            onMergeTag = { sourceTagId, targetTagId ->
                viewModel.mergeTags(sourceTagId, targetTagId)
            },
            onDeleteTag = { tagId ->
                viewModel.deleteTagById(tagId)
            },
            onCreateCategory = { name -> viewModel.createCategory(name) },
            onDismiss = { viewModel.closeTagManagement() }
        )
    }

    // ==================== Instagram / iOS 风格长按悬浮预览卡片 ====================
    BackHandler(enabled = peekPhoto != null) {
        peekPhoto = null
    }

    AnimatedVisibility(
        visible = peekPhoto != null,
        enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(tween(140)) + scaleOut(tween(140))
    ) {
        val currentPeek = peekPhoto
        if (currentPeek != null) {
            val isFav = allPhotosWithTags.find { it.photo.photoId == currentPeek.photoId }?.tags?.any { it.name == "#收藏" } == true
            InstagramPeekOverlay(
                photo = currentPeek,
                isFavorite = isFav,
                onDismiss = { peekPhoto = null },
                onOpenFullViewer = { targetPhoto ->
                    peekPhoto = null
                    val photoIndex = filteredPhotos.indexOfFirst { it.photoId == targetPhoto.photoId }
                    viewModel.openPhotoViewer(filteredPhotos, photoIndex.coerceAtLeast(0))
                },
                onToggleFavorite = { targetPhoto ->
                    viewModel.toggleFavoriteTag(targetPhoto.photoId)
                },
                onViewDetails = { targetPhoto ->
                    peekPhoto = null
                    metadataPhoto = targetPhoto
                    coroutineScope.launch {
                        isMetadataLoading = true
                        parsedMetadata = ExifMetadataUtil.parseMetadata(context, targetPhoto)
                        isMetadataLoading = false
                    }
                },
                onEditPhoto = { targetPhoto ->
                    peekPhoto = null
                    viewModel.openImageEditor(targetPhoto)
                },
                onQuickTag = { targetPhoto ->
                    viewModel.enterMultiSelectMode(targetPhoto.photoId)
                    viewModel.showBatchTagSheet()
                },
                onShare = { targetPhoto ->
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = if (targetPhoto.isVideo) "video/*" else "image/*"
                        putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(targetPhoto.uri))
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "分享照片"))
                },
                onSetAsCover = { targetPhoto ->
                    peekPhoto = null
                    showSetCoverPhoto = targetPhoto
                },
                onDelete = { targetPhoto ->
                    peekPhoto = null
                    viewModel.requestSoftDeletePhotos(listOf(targetPhoto.uri), context)
                }
            )
        }
    }

    // ==================== 照片详细参数抽屉面板 ====================
    if (metadataPhoto != null) {
        val target = metadataPhoto!!
        val tags = remember(allPhotosWithTags, target.photoId) {
            allPhotosWithTags.find { it.photo.photoId == target.photoId }?.tags ?: emptyList()
        }
        PhotoMetadataBottomSheet(
            metadata = parsedMetadata,
            isLoading = isMetadataLoading,
            tags = tags,
            allAvailableTags = allTags,
            onToggleTag = { tagId, isAdded ->
                if (isAdded) {
                    viewModel.addTagToPhoto(target.photoId, tagId)
                } else {
                    viewModel.removeTagFromPhoto(target.photoId, tagId)
                }
            },
            onUpdateCustomLocation = { pId, loc ->
                viewModel.updatePhotoCustomLocation(pId, loc)
            },
            onRefreshMetadata = {
                coroutineScope.launch {
                    isMetadataLoading = true
                    parsedMetadata = ExifMetadataUtil.parseMetadata(context, target)
                    isMetadataLoading = false
                }
            },
            onDismiss = {
                metadataPhoto = null
                parsedMetadata = null
            }
        )
    }

    // ==================== 设为相册封面选择弹窗 ====================
    if (showSetCoverPhoto != null) {
        val targetPhoto = showSetCoverPhoto!!
        AlertDialog(
            onDismissRequest = { showSetCoverPhoto = null },
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
                    androidx.compose.foundation.lazy.LazyColumn(
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
                                        viewModel.setAlbumCustomCover(album.parentTag.tagId, targetPhoto.uri)
                                        android.widget.Toast.makeText(context, "已成功设为「${album.parentTag.name}」的相册封面", android.widget.Toast.LENGTH_SHORT).show()
                                        showSetCoverPhoto = null
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
                TextButton(onClick = { showSetCoverPhoto = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 顶部搜索栏组件（支持中文全拼与首字母简拼检索） ====================

@Composable
private fun MultiSelectTopBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onInvertSelect: (() -> Unit)? = null,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 取消按钮 (灰色微药丸胶囊)
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = com.example.phototag.ui.components.getIosPillBackground(),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, com.example.phototag.ui.components.getIosHairlineColor()),
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(percent = 50))
                .bounceScale()
                .clickable(onClick = onExit)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "取消多选",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "取消",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 中间计数文本
        Text(
            text = if (selectedCount == 0) "选择照片" else "已选择 $selectedCount 项",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.2).sp
        )

        // 右侧操作组：反选 + 全选 (iOS 18 微药丸组合)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onInvertSelect != null) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = com.example.phototag.ui.components.getIosPillBackground(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, com.example.phototag.ui.components.getIosHairlineColor()),
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .bounceScale()
                        .clickable(onClick = onInvertSelect)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "反选",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 全选按钮 (Apple Black / OLED White 药丸胶囊)
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x14000000)),
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .bounceScale()
                    .clickable(onClick = onSelectAll)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "全选",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPhotos(hasFilter: Boolean, onClearFilters: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            Text(
                if (hasFilter) "没有符合条件的照片" else "相册为空，点击右上角同步",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            if (hasFilter) {
                Button(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("查看全部照片")
                }
            }
        }
    }
}

@Composable
private fun EmptyPermission() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            Text("需要相册访问权限", style = MaterialTheme.typography.titleMedium)
            Text("请在系统设置中授予「媒体文件」访问权限", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/**
 * 浮动吸顶时间轴微胶囊 (Floating Sticky Date Header)
 */
@Composable
private fun BoxScope.TimelineStickyDatePill(
    header: TimelineGridItem.DateHeader?,
    visible: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible && header != null,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 2 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 2 },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp)
    ) {
        if (header != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDarkTheme) Color(0xDD1C1C1E) else Color(0xEEFFFFFF),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .bounceScale()
                    .clickable(onClick = onClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = header.dateTitle,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (header.dateSubtitle.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = header.dateSubtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = getIosPillBackground()
                    ) {
                        Text(
                            text = "${header.photoCount}张",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
