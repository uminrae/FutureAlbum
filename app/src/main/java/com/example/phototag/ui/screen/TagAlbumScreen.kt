package com.example.phototag.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import com.example.phototag.ui.components.FloatingCapsuleBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phototag.data.relation.PhotoWithTags
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.SmartAlbumEntity
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.DeviceBucket
import com.example.phototag.data.model.ParentTagAlbum
import com.example.phototag.data.model.SmartAlbumWithPhotos
import com.example.phototag.data.model.TagWithCount
import com.example.phototag.ui.MainViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.example.phototag.ui.components.ArchivedBoxCard
import com.example.phototag.ui.components.BatchTagBottomSheet
import com.example.phototag.ui.components.CreateSmartAlbumBottomSheet
import com.example.phototag.ui.components.FastScroller
import com.example.phototag.ui.components.HealthCheckBottomSheet
import com.example.phototag.ui.components.IosButton
import com.example.phototag.ui.components.IosButtonVariant
import com.example.phototag.ui.components.IosIconButton
import com.example.phototag.ui.components.IosListRow
import com.example.phototag.ui.components.PhotoGridItem
import com.example.phototag.ui.components.TagActionBottomSheet
import com.example.phototag.ui.components.TagChip
import com.example.phototag.ui.components.TimelineDateHeader
import com.example.phototag.ui.components.bounceScale
import com.example.phototag.ui.components.dragSelectModifier
import com.example.phototag.ui.components.getIosGlassBackground
import com.example.phototag.ui.components.getIosHairlineColor
import com.example.phototag.ui.components.pinchToResizeGrid
import com.example.phototag.ui.components.rememberReorderableAlbumState
import com.example.phototag.ui.components.reorderableAlbumItem
import com.example.phototag.ui.theme.IosAmberDark
import com.example.phototag.ui.theme.IosSystemGray1
import com.example.phototag.ui.theme.IosSystemGray2
import com.example.phototag.util.CoilConfig
import com.example.phototag.util.TimelineGridItem
import com.example.phototag.util.TimelineUtils

/**
 * Tab 2 — 相册空间（【✨ 智能动态相册】 + 【🏷️ 母子层级标签相册】 + 【📌 自由置顶】 + 【手势拖拽重排】）。
 */
@Composable
fun TagAlbumScreen(
    viewModel: MainViewModel,
    innerPadding: PaddingValues,
    onDeleteRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val smartAlbumsWithPhotos by viewModel.smartAlbumsWithPhotos.collectAsState()
    val selectedSmartAlbumPhotos by viewModel.selectedSmartAlbumPhotos.collectAsState()
    val parentTagAlbums by viewModel.parentTagAlbums.collectAsState()
    val archivedParentTagAlbums by viewModel.archivedParentTagAlbums.collectAsState()
    val customAlbumCovers by viewModel.customAlbumCovers.collectAsState()
    val tagAlbumPhotos by viewModel.tagAlbumPhotos.collectAsState()
    val allPhotosWithTags by viewModel.allPhotosWithTags.collectAsState()
    val categoriesWithTags by viewModel.categoriesWithTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val rootTags by viewModel.rootTags.collectAsState()
    val recentTags by viewModel.recentTags.collectAsState()
    val topUsedTags by viewModel.topUsedTags.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val isHealthChecking by viewModel.isHealthChecking.collectAsState()
    val healthReport by viewModel.healthReport.collectAsState()
    val trashPhotoCount by viewModel.trashPhotoCount.collectAsState()
    val untaggedPhotos by viewModel.untaggedPhotos.collectAsState()
    val untaggedCount by viewModel.untaggedCount.collectAsState()
    val deviceBuckets by viewModel.deviceBuckets.collectAsState()
    val selectedDeviceBucket by viewModel.selectedDeviceBucket.collectAsState()
    val deviceBucketPhotos by viewModel.deviceBucketPhotos.collectAsState()

    var isArchivedBoxExpanded by remember { mutableStateOf(false) }
    val tagNamesMap = remember(allTags) { allTags.associate { it.tagId to it.name } }

    // 导航路由：1. 智能相册内页 2. 标签相册文件夹内页 3. 待整理未分类相册内页 4. 相册外层大厅
    when {
        uiState.isViewingUntaggedAlbum -> {
            BackHandler(enabled = true) {
                if (uiState.isMultiSelectMode) {
                    viewModel.exitMultiSelectMode()
                } else {
                    viewModel.closeUntaggedAlbum()
                }
            }
            // ---- 📌 未分类照片专属图片流 ----
            FolderPhotoGridView(
                parentTag = TagEntity(name = "未分类照片", categoryId = 0, colorHex = "#F59E0B"),
                childTags = emptyList(),
                allFolderPhotos = untaggedPhotos,
                gridColumns = gridColumns,
                isMultiSelectMode = uiState.isMultiSelectMode,
                selectedPhotoIds = uiState.selectedPhotoIds,
                innerPadding = innerPadding,
                onBack = { viewModel.closeUntaggedAlbum() },
                onColumnsChanged = { viewModel.setGridColumns(it) },
                onStartDragSelect = { photoId -> viewModel.startDragSelection(photoId) },
                onDragSelectChange = { selectedIds -> viewModel.updateDragSelection(selectedIds) },
                onPhotoClick = { photoId, displayPhotos ->
                    if (uiState.isMultiSelectMode) {
                        viewModel.togglePhotoSelection(photoId)
                    } else {
                        val index = displayPhotos.indexOfFirst { it.photoId == photoId }
                        viewModel.openPhotoViewer(displayPhotos, index.coerceAtLeast(0))
                    }
                },
                onPhotoLongClick = { photoId ->
                    if (!uiState.isMultiSelectMode) viewModel.enterMultiSelectMode(photoId)
                },
                onLoadError = { photoId, uri -> viewModel.onPhotoLoadFailed(photoId, uri) },
                onBatchTag = { viewModel.showBatchTagSheet() },
                onFilterClick = { viewModel.openBatchFilterSheet() },
                onTransferTag = { viewModel.openTransferTagSheet() },
                onShare = { viewModel.shareSelectedPhotos() },
                onDelete = { onDeleteRequest() },
                onCancel = { viewModel.exitMultiSelectMode() },
                onChildTagManage = {},
                onShareAlbum = { photos -> viewModel.shareAlbumPhotos(photos, context) }
            )
        }

        uiState.selectedSmartAlbum != null -> {
            BackHandler(enabled = true) {
                if (uiState.isMultiSelectMode) {
                    viewModel.exitMultiSelectMode()
                } else {
                    viewModel.closeSmartAlbumView()
                }
            }
            // ---- 智能动态相册专属图片流 ----
            val album = uiState.selectedSmartAlbum!!
            SmartAlbumPhotoGridView(
                album = album,
                photos = selectedSmartAlbumPhotos,
                gridColumns = gridColumns,
                tagNamesMap = tagNamesMap,
                isMultiSelectMode = uiState.isMultiSelectMode,
                selectedPhotoIds = uiState.selectedPhotoIds,
                innerPadding = innerPadding,
                onBack = { viewModel.closeSmartAlbumView() },
                onEditRule = { viewModel.openCreateSmartAlbumSheet(album) },
                onColumnsChanged = { viewModel.setGridColumns(it) },
                onStartDragSelect = { photoId -> viewModel.startDragSelection(photoId) },
                onDragSelectChange = { selectedIds -> viewModel.updateDragSelection(selectedIds) },
                onPhotoClick = { photoId, displayPhotos ->
                    if (uiState.isMultiSelectMode) {
                        viewModel.togglePhotoSelection(photoId)
                    } else {
                        val index = displayPhotos.indexOfFirst { it.photoId == photoId }
                        viewModel.openPhotoViewer(displayPhotos, index.coerceAtLeast(0))
                    }
                },
                onPhotoLongClick = { photoId ->
                    if (!uiState.isMultiSelectMode) viewModel.enterMultiSelectMode(photoId)
                },
                onLoadError = { photoId, uri -> viewModel.onPhotoLoadFailed(photoId, uri) },
                onBatchTag = { viewModel.showBatchTagSheet() },
                onFilterClick = { viewModel.openBatchFilterSheet() },
                onTransferTag = { viewModel.openTransferTagSheet() },
                onShare = { viewModel.shareSelectedPhotos() },
                onDelete = { onDeleteRequest() },
                onCancel = { viewModel.exitMultiSelectMode() }
            )
        }


        uiState.tagAlbumSelectedTagId != null -> {
            BackHandler(enabled = true) {
                if (uiState.isMultiSelectMode) {
                    viewModel.exitMultiSelectMode()
                } else {
                    viewModel.closeTagAlbum()
                }
            }
            // ---- 标签相册文件夹内部（展示子相册 Tab 切换 + 全部图片） ----
            val selectedTagId = uiState.tagAlbumSelectedTagId
            val currentAlbum = (parentTagAlbums + archivedParentTagAlbums).find { it.parentTag.tagId == selectedTagId }

            FolderPhotoGridView(
                parentTag = currentAlbum?.parentTag,
                childTags = currentAlbum?.childTags ?: emptyList(),
                allFolderPhotos = tagAlbumPhotos,
                allPhotosWithTags = allPhotosWithTags,
                gridColumns = gridColumns,
                isMultiSelectMode = uiState.isMultiSelectMode,
                selectedPhotoIds = uiState.selectedPhotoIds,
                innerPadding = innerPadding,
                onBack = { viewModel.closeTagAlbum() },
                onColumnsChanged = { viewModel.setGridColumns(it) },
                onStartDragSelect = { photoId -> viewModel.startDragSelection(photoId) },
                onDragSelectChange = { selectedIds -> viewModel.updateDragSelection(selectedIds) },
                onPhotoClick = { photoId, displayPhotos ->
                    if (uiState.isMultiSelectMode) {
                        viewModel.togglePhotoSelection(photoId)
                    } else {
                        val index = displayPhotos.indexOfFirst { it.photoId == photoId }
                        viewModel.openPhotoViewer(displayPhotos, index.coerceAtLeast(0))
                    }
                },
                onPhotoLongClick = { photoId ->
                    if (!uiState.isMultiSelectMode) viewModel.enterMultiSelectMode(photoId)
                },
                onLoadError = { photoId, uri -> viewModel.onPhotoLoadFailed(photoId, uri) },
                onBatchTag = { viewModel.showBatchTagSheet() },
                onFilterClick = { viewModel.openBatchFilterSheet() },
                onTransferTag = { viewModel.openTransferTagSheet() },
                onShare = { viewModel.shareSelectedPhotos() },
                onDelete = { onDeleteRequest() },
                onCancel = { viewModel.exitMultiSelectMode() },
                onChildTagManage = { childTag -> viewModel.openTagManagement(childTag) },
                onShareAlbum = { photos -> viewModel.shareAlbumPhotos(photos, context) }
            )
        }

        selectedDeviceBucket != null -> {
            BackHandler(enabled = true) {
                if (uiState.isMultiSelectMode) {
                    viewModel.exitMultiSelectMode()
                } else {
                    viewModel.closeDeviceBucket()
                }
            }
            // ---- 手机物理文件夹专属图片流 ----
            val bucket = selectedDeviceBucket!!
            FolderPhotoGridView(
                parentTag = TagEntity(name = bucket.bucketName, categoryId = 0, colorHex = "#007AFF"),
                childTags = emptyList(),
                allFolderPhotos = deviceBucketPhotos,
                gridColumns = gridColumns,
                isMultiSelectMode = uiState.isMultiSelectMode,
                selectedPhotoIds = uiState.selectedPhotoIds,
                innerPadding = innerPadding,
                onBack = { viewModel.closeDeviceBucket() },
                onColumnsChanged = { viewModel.setGridColumns(it) },
                onStartDragSelect = { photoId -> viewModel.startDragSelection(photoId) },
                onDragSelectChange = { selectedIds -> viewModel.updateDragSelection(selectedIds) },
                onPhotoClick = { photoId, displayPhotos ->
                    if (uiState.isMultiSelectMode) {
                        viewModel.togglePhotoSelection(photoId)
                    } else {
                        val index = displayPhotos.indexOfFirst { it.photoId == photoId }
                        viewModel.openPhotoViewer(displayPhotos, index.coerceAtLeast(0))
                    }
                },
                onPhotoLongClick = { photoId ->
                    if (!uiState.isMultiSelectMode) viewModel.enterMultiSelectMode(photoId)
                },
                onLoadError = { photoId, uri -> viewModel.onPhotoLoadFailed(photoId, uri) },
                onBatchTag = { viewModel.showBatchTagSheet() },
                onFilterClick = { viewModel.openBatchFilterSheet() },
                onTransferTag = { viewModel.openTransferTagSheet() },
                onShare = { viewModel.shareSelectedPhotos() },
                onDelete = { onDeleteRequest() },
                onCancel = { viewModel.exitMultiSelectMode() },
                onChildTagManage = {},
                onShareAlbum = { photos -> viewModel.shareAlbumPhotos(photos, context) }
            )
        }

        else -> {
            if (uiState.isMultiSelectMode) {
                BackHandler(enabled = true) {
                    viewModel.exitMultiSelectMode()
                }
            }
            // ---- 相册大厅：智能动态相册 + 母子层级标签相册（支持置顶、拖拽排序与归档收纳盒 + 🗑️ 安全回收站） ----
            AlbumsHallView(
                smartAlbums = smartAlbumsWithPhotos,
                parentTagAlbums = parentTagAlbums,
                archivedParentTagAlbums = archivedParentTagAlbums,
                customAlbumCovers = customAlbumCovers,
                isArchivedExpanded = isArchivedBoxExpanded,
                trashPhotoCount = trashPhotoCount,
                untaggedCount = untaggedCount,
                untaggedCoverUri = untaggedPhotos.firstOrNull()?.uri,
                deviceBuckets = deviceBuckets,
                onDeviceBucketClick = { viewModel.openDeviceBucket(it) },
                tagNamesMap = tagNamesMap,
                innerPadding = innerPadding,
                onToggleArchivedExpand = { isArchivedBoxExpanded = !isArchivedBoxExpanded },
                onOpenHealthCheck = { viewModel.openHealthCheckSheet() },
                onOpenTrash = { viewModel.openTrashScreen() },
                onOpenUntaggedAlbum = { viewModel.openUntaggedAlbum() },
                onCreateSmartAlbum = { viewModel.openCreateSmartAlbumSheet(null) },
                onSmartAlbumClick = { smartAlbum -> viewModel.selectSmartAlbum(smartAlbum.album) },
                onEditSmartAlbum = { smartAlbum -> viewModel.openCreateSmartAlbumSheet(smartAlbum) },
                onDeleteSmartAlbum = { smartAlbumId -> viewModel.deleteSmartAlbum(smartAlbumId) },
                onParentTagAlbumClick = { album -> viewModel.openTagAlbum(album.parentTag.tagId) },
                onParentTagAlbumManage = { album -> viewModel.openTagManagement(album.parentTag) },
                onTogglePinned = { tagId, isPinned -> viewModel.toggleTagPinned(tagId, isPinned) },
                onArchiveAlbum = { tagId -> viewModel.setTagArchived(tagId, true) },
                onUnarchiveAlbum = { tagId -> viewModel.setTagArchived(tagId, false) },
                onDeleteAlbum = { tagId -> viewModel.deleteTagById(tagId) },
                onReorderAlbums = { orderedTagIds -> viewModel.reorderParentTagAlbums(orderedTagIds) },
                onShareAlbum = { album -> viewModel.shareParentAlbum(album, context) },
                onResetAlbumCover = { tagId -> viewModel.setAlbumCustomCover(tagId, null) },
                allPhotosWithTags = allPhotosWithTags,
                onOpenFeaturedMoment = {
                    val photos = allPhotosWithTags.map { it.photo }
                    if (photos.isNotEmpty()) {
                        viewModel.openPhotoViewer(photos, 0)
                    }
                },
                onFilterVideos = {
                    viewModel.onSearchQueryChange("视频")
                    viewModel.selectTab(0)
                },
                onFilterScreenshots = {
                    viewModel.onSearchQueryChange("截屏")
                    viewModel.selectTab(0)
                },
                onOpenCleanupLab = { viewModel.openCleanupLab() },
                onOpenGalleryLabs = { viewModel.openGalleryLabs() }
            )
        }
    }

    // ==================== ✨ 新建/编辑智能相册 BottomSheet ====================
    if (uiState.showCreateSmartAlbumSheet) {
        CreateSmartAlbumBottomSheet(
            allTags = allTags,
            editingAlbum = uiState.editingSmartAlbum,
            onSave = { name, icon, color, rule ->
                viewModel.saveSmartAlbum(name, icon, color, rule)
            },
            onDismiss = { viewModel.closeCreateSmartAlbumSheet() }
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

        com.example.phototag.ui.components.TransferTagBottomSheet(
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

    // ==================== 标签深度管理 BottomSheet ====================
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
            onArchiveTag = { tagId, isArchived ->
                viewModel.setTagArchived(tagId, isArchived)
            },
            onCreateCategory = { name -> viewModel.createCategory(name) },
            onDismiss = { viewModel.closeTagManagement() }
        )
    }

    // ==================== 🧹 相册健康体检与自愈 BottomSheet ====================
    if (uiState.showHealthCheckSheet) {
        HealthCheckBottomSheet(
            isChecking = isHealthChecking,
            report = healthReport,
            onRecheck = { viewModel.runHealthCheck() },
            onCleanOrphanTags = { tagIds -> viewModel.cleanOrphanTags(tagIds) },
            onMergeTags = { srcId, targetId -> viewModel.mergeSimilarTags(srcId, targetId) },
            onCleanDeadReferences = { viewModel.cleanDeadReferences() },
            onAutoFixAll = { viewModel.autoFixAllHealthIssues() },
            onDismiss = { viewModel.closeHealthCheckSheet() }
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
}

// ==================== 相册大厅视图 (AlbumsHallView) ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumsHallView(
    smartAlbums: List<SmartAlbumWithPhotos>,
    parentTagAlbums: List<ParentTagAlbum>,
    archivedParentTagAlbums: List<ParentTagAlbum>,
    isArchivedExpanded: Boolean,
    trashPhotoCount: Int,
    untaggedCount: Int,
    untaggedCoverUri: String?,
    deviceBuckets: List<DeviceBucket> = emptyList(),
    onDeviceBucketClick: (DeviceBucket) -> Unit = {},
    tagNamesMap: Map<Long, String>,
    innerPadding: PaddingValues,
    onToggleArchivedExpand: () -> Unit,
    onOpenHealthCheck: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenUntaggedAlbum: () -> Unit,
    onCreateSmartAlbum: () -> Unit,
    onSmartAlbumClick: (SmartAlbumWithPhotos) -> Unit,
    onEditSmartAlbum: (SmartAlbumEntity) -> Unit,
    onDeleteSmartAlbum: (Long) -> Unit,
    onParentTagAlbumClick: (ParentTagAlbum) -> Unit,
    onParentTagAlbumManage: (ParentTagAlbum) -> Unit,
    onTogglePinned: (Long, Boolean) -> Unit,
    onArchiveAlbum: (Long) -> Unit,
    onUnarchiveAlbum: (Long) -> Unit,
    onDeleteAlbum: (Long) -> Unit,
    onReorderAlbums: (List<Long>) -> Unit,
    customAlbumCovers: Map<Long, String> = emptyMap(),
    onShareAlbum: (ParentTagAlbum) -> Unit = {},
    onResetAlbumCover: (Long) -> Unit = {},
    allPhotosWithTags: List<PhotoWithTags> = emptyList(),
    onOpenFeaturedMoment: () -> Unit = {},
    onFilterVideos: () -> Unit = {},
    onFilterScreenshots: () -> Unit = {},
    onOpenCleanupLab: () -> Unit = {},
    onOpenGalleryLabs: () -> Unit = {}
) {
    // 内存中的排序列表（供即时流畅拖拽位移反馈）
    var localAlbums by remember(parentTagAlbums) { mutableStateOf(parentTagAlbums) }

    val featuredPhoto = remember(allPhotosWithTags) { allPhotosWithTags.firstOrNull()?.photo }
    val videoPhotos = remember(allPhotosWithTags) { allPhotosWithTags.map { it.photo }.filter { it.isVideo } }
    val screenshotPhotos = remember(allPhotosWithTags) {
        allPhotosWithTags.map { it.photo }.filter {
            it.displayName.contains("screenshot", ignoreCase = true) ||
            it.displayName.contains("截屏") ||
            it.displayName.contains("Screen_")
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderState = rememberReorderableAlbumState(
        onMove = { fromIndex, toIndex ->
            val list = localAlbums.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                localAlbums = list
            }
        },
        onDragEnd = {
            val orderedIds = localAlbums.map { it.parentTag.tagId }
            onReorderAlbums(orderedIds)
        }
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {

        // ==================== 0. iOS 18 大标题栏与新建操作 ====================
        item(key = "header_album_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "相册",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )

                IosButton(
                    text = "新建相册",
                    icon = Icons.Filled.Add,
                    variant = IosButtonVariant.PRIMARY,
                    onClick = onCreateSmartAlbum,
                    height = 32.dp
                )
            }
        }

        // ==================== 1. iOS 18 Bento Hero：今日回忆与胶卷精选 ====================
        if (featuredPhoto != null) {
            item(key = "bento_hero_featured") {
                HeroMomentBentoCard(
                    photo = featuredPhoto,
                    totalPhotoCount = allPhotosWithTags.size,
                    onClick = onOpenFeaturedMoment,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }

        // ==================== 2. iOS 18 Bento 媒体与快捷整理 4 宫格 ====================
        item(key = "bento_media_types_grid") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = "媒体与整理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 视频卡片 (Videos)
                    MediaBentoCard(
                        title = "视频",
                        count = videoPhotos.size,
                        icon = Icons.Filled.Videocam,
                        gradientColors = listOf(Color(0xFF5E5CE6), Color(0xFF3634A3)),
                        coverUri = videoPhotos.firstOrNull()?.uri,
                        onClick = onFilterVideos,
                        modifier = Modifier.weight(1f)
                    )

                    // 截屏与录屏 (Screenshots)
                    MediaBentoCard(
                        title = "截屏与录屏",
                        count = screenshotPhotos.size,
                        icon = Icons.Filled.PhotoLibrary,
                        gradientColors = listOf(Color(0xFFFF9500), Color(0xFFFF5E3A)),
                        coverUri = screenshotPhotos.firstOrNull()?.uri,
                        onClick = onFilterScreenshots,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 待整理照片 (Untagged)
                    MediaBentoCard(
                        title = "待整理照片",
                        count = untaggedCount,
                        badge = if (untaggedCount > 0) "待分类" else "已清空",
                        icon = Icons.Filled.Bolt,
                        gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
                        coverUri = untaggedCoverUri,
                        onClick = onOpenUntaggedAlbum,
                        modifier = Modifier.weight(1f)
                    )

                    // 最近删除 (Trash)
                    MediaBentoCard(
                        title = "最近删除",
                        count = trashPhotoCount,
                        badge = "7天隔离",
                        icon = Icons.Filled.DeleteSweep,
                        gradientColors = listOf(Color(0xFFFF453A), Color(0xFF9E1B14)),
                        onClick = onOpenTrash,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==================== 2.5 手机物理文件夹 / 来源相册 (Device Albums) ====================
        if (deviceBuckets.isNotEmpty()) {
            item(key = "header_device_buckets") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "手机文件夹",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${deviceBuckets.size} 个来源目录",
                        fontSize = 13.sp,
                        color = IosSystemGray1
                    )
                }
            }

            item(key = "list_device_buckets") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = deviceBuckets,
                        key = { it.bucketName }
                    ) { bucket ->
                        DeviceBucketCard(
                            bucket = bucket,
                            onClick = { onDeviceBucketClick(bucket) }
                        )
                    }
                }
            }
        }

        // ==================== 3. 【我的相册 (标签相册)】区块 ====================
        item(key = "header_tag_albums") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "我的相册",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${localAlbums.size} 个相册",
                    fontSize = 13.sp,
                    color = IosSystemGray1
                )
            }
        }

        // 标签相册网格（Apple Photos 2 列卡片，支持拖拽重排）
        if (localAlbums.isEmpty()) {
            item(key = "empty_parent_tag_albums") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = IosSystemGray2,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "暂无相册",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "给照片打上标签，即可自动聚合成专属相册",
                            fontSize = 12.5.sp,
                            color = IosSystemGray1
                        )
                    }
                }
            }
        } else {
            items(
                items = localAlbums.chunked(2),
                key = { chunk -> "row_" + chunk.joinToString("_") { it.parentTag.tagId.toString() } }
            ) { rowAlbums ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    for (album in rowAlbums) {
                        val index = localAlbums.indexOf(album)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (index >= 0) {
                                        Modifier.reorderableAlbumItem(
                                            state = reorderState,
                                            index = index,
                                            totalItems = localAlbums.size
                                        )
                                    } else Modifier
                                )
                        ) {
                            ParentAlbumCard(
                                album = album,
                                onClick = { onParentTagAlbumClick(album) },
                                onManageClick = { onParentTagAlbumManage(album) },
                                onTogglePinned = { onTogglePinned(album.parentTag.tagId, !album.parentTag.isPinned) },
                                onArchive = { onArchiveAlbum(album.parentTag.tagId) },
                                onShareAlbum = { onShareAlbum(album) },
                                onResetCover = { onResetAlbumCover(album.parentTag.tagId) },
                                hasCustomCover = album.parentTag.tagId in customAlbumCovers
                            )
                        }
                    }
                    if (rowAlbums.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ==================== 2. 【✨ 智能动态虚拟相册】横向滚动流 ====================
        if (smartAlbums.isNotEmpty()) {
            item(key = "header_smart_albums") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "智能相册",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${smartAlbums.size} 个规则相册",
                        fontSize = 13.sp,
                        color = IosSystemGray1
                    )
                }
            }

            // 智能分类横向卡片流
            item(key = "list_smart_albums") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = smartAlbums,
                        key = { it.album.id }
                    ) { smartAlbum ->
                        SmartAlbumCard(
                            albumWithPhotos = smartAlbum,
                            tagNamesMap = tagNamesMap,
                            onClick = { onSmartAlbumClick(smartAlbum) },
                            onEdit = { onEditSmartAlbum(smartAlbum.album) },
                            onDelete = { onDeleteSmartAlbum(smartAlbum.album.id) }
                        )
                    }
                }
            }
        }

        // ==================== 3. 【更多项目 (Utilities / 实用工具)】iOS Inset Grouped 表格 ====================
        item(key = "header_utilities") {
            Text(
                text = "更多项目",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 4.dp)
            )
        }

        item(key = "utilities_grouped_card") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.5.dp,
                    color = getIosHairlineColor()
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Column {
                    // 1. 待整理照片 (Untagged)
                    IosListRow(
                        title = "待整理照片",
                        subtitle = "未打标归类的内容",
                        icon = Icons.Filled.Folder,
                        iconContainerColor = Color(0xFFFF9500),
                        badgeText = if (untaggedCount > 0) "$untaggedCount 张" else null,
                        badgeColor = Color(0xFFFF9500),
                        onClick = onOpenUntaggedAlbum
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        thickness = 0.5.dp,
                        color = getIosHairlineColor()
                    )

                    // 2. 相册体检与自愈 (Health Check)
                    IosListRow(
                        title = "相册体检与优化",
                        subtitle = "清理孤立标签与异常引用",
                        icon = Icons.Filled.HealthAndSafety,
                        iconContainerColor = Color(0xFF34C759),
                        badgeText = "体检",
                        onClick = onOpenHealthCheck
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        thickness = 0.5.dp,
                        color = getIosHairlineColor()
                    )

                    // 3. 🧹 相册瘦身工坊 (Samsung 杀手锏)
                    IosListRow(
                        title = "相册瘦身工坊",
                        subtitle = "相似连拍、截图沉淀与超大文件清理",
                        icon = Icons.Filled.AutoAwesome,
                        iconContainerColor = Color(0xFF007AFF),
                        badgeText = "释放空间",
                        badgeColor = Color(0xFF007AFF),
                        onClick = onOpenCleanupLab
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        thickness = 0.5.dp,
                        color = getIosHairlineColor()
                    )

                    // 4. 🧪 相册实验室 (Samsung 极客彩蛋)
                    IosListRow(
                        title = "相册实验室 (Gallery Labs)",
                        subtitle = "120Hz 实时帧率浮标与极客调试",
                        icon = Icons.Filled.Science,
                        iconContainerColor = Color(0xFF5856D6),
                        badgeText = "Labs",
                        badgeColor = Color(0xFF5856D6),
                        onClick = onOpenGalleryLabs
                    )

                    // 3. 已归档相册 (Archived) - 如果存在已归档相册
                    if (archivedParentTagAlbums.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 62.dp),
                            thickness = 0.5.dp,
                            color = getIosHairlineColor()
                        )

                        IosListRow(
                            title = "已归档相册收纳盒",
                            subtitle = "保持主列表清爽",
                            icon = Icons.Filled.Archive,
                            iconContainerColor = Color(0xFF8E8E93),
                            badgeText = "${archivedParentTagAlbums.size} 个",
                            trailingChevron = if (isArchivedExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.ArrowForwardIos,
                            onClick = onToggleArchivedExpand
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        thickness = 0.5.dp,
                        color = getIosHairlineColor()
                    )

                    // 4. 最近删除 / 回收站 (Recently Deleted / Trash)
                    IosListRow(
                        title = "最近删除",
                        subtitle = "暂存 7 天 · 彻底隔离",
                        icon = Icons.Filled.DeleteSweep,
                        iconContainerColor = Color(0xFFFF3B30),
                        badgeText = if (trashPhotoCount > 0) "$trashPhotoCount 项" else "空",
                        badgeColor = if (trashPhotoCount > 0) Color(0xFFFF3B30) else null,
                        onClick = onOpenTrash
                    )
                }
            }
        }

        // ==================== 4. 如果展开归档收纳盒，显示归档相册详情 ====================
        if (isArchivedExpanded && archivedParentTagAlbums.isNotEmpty()) {
            item(key = "archived_box_card") {
                ArchivedBoxCard(
                    archivedAlbums = archivedParentTagAlbums,
                    isExpanded = isArchivedExpanded,
                    onToggleExpand = onToggleArchivedExpand,
                    onAlbumClick = onParentTagAlbumClick,
                    onUnarchiveAlbum = onUnarchiveAlbum,
                    onDeleteAlbum = { tag -> onDeleteAlbum(tag.tagId) },
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }
    }
}

// ==================== 🍏 iOS 18 Bento Grid 核心卡片组件 ====================

/**
 * ✨ iOS 18 今日回忆与胶卷精选全宽大便当盒卡片 (HeroMomentBentoCard)
 */
@Composable
private fun HeroMomentBentoCard(
    photo: PhotoEntity,
    totalPhotoCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val imageRequest = remember(photo.uri) {
        CoilConfig.buildThumbnailRequest(context, photo.uri, sizePx = 640)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border = BorderStroke(0.5.dp, com.example.phototag.ui.components.getIosHairlineColor()),
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(24.dp))
            .bounceScale()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "今日回忆精选",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 电影级暗角与垂直渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // 顶部左侧微光胶囊徽标
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.50f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "精选时刻 · 胶卷回忆",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 底部排版
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今日回忆与光影精选",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "回顾 $totalPhotoCount 项精彩内容 · 轻触沉浸浏览",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "探索",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 📦 媒体类型与快捷整理 Bento 卡片 (MediaBentoCard)
 */
@Composable
private fun MediaBentoCard(
    title: String,
    count: Int,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    coverUri: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val coverRequest = remember(coverUri) {
        if (!coverUri.isNullOrBlank()) {
            CoilConfig.buildThumbnailRequest(context, coverUri, sizePx = 280)
        } else null
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
        border = BorderStroke(0.5.dp, com.example.phototag.ui.components.getIosHairlineColor()),
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(20.dp))
            .bounceScale()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景渐变或缩略图微透
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    gradientColors.first().copy(alpha = 0.82f),
                                    gradientColors.last().copy(alpha = 0.94f)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(colors = gradientColors)
                        )
                )
            }

            // 内容布局
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部：图标与右侧小角标
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!badge.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.30f))
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 底部：主标题与照片数量
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "$count 项",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

// ==================== 智能相册精美卡片 (SmartAlbumCard) ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmartAlbumCard(
    albumWithPhotos: SmartAlbumWithPhotos,
    tagNamesMap: Map<Long, String>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val album = albumWithPhotos.album
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val ruleSummary = remember(album.ruleJson, tagNamesMap) {
        album.rule.getSummaryDescription(tagNamesMap)
    }

    val themeColor = remember(album.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(album.colorHex))
        } catch (_: Exception) {
            Color(0xFF7E9A86)
        }
    }

    val coverRequest = remember(albumWithPhotos.coverUri) {
        albumWithPhotos.coverUri?.let { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build()
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .width(220.dp)
            .height(145.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 0.5.dp,
                color = com.example.phototag.ui.components.getIosHairlineColor(),
                shape = RoundedCornerShape(20.dp)
            )
            .bounceScale()
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 全幅封面大图 / 占位
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(album.iconName, fontSize = 42.sp)
                }
            }

            // 2. 杂志式垂直暗色渐变蒙层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // 3. 右上角：菜单
            // 3. 右上角：菜单
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { showMenu = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.MoreVert,
                            "菜单",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑规则") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除智能相册", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }

            // 4. 左下角：杂志标题与照片数排版
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${albumWithPhotos.photoCount} 张 · $ruleSummary",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 📁 未分类/未打标照片专属智能相册封面卡片 (UntaggedAlbumCard)。
 */
@Composable
private fun UntaggedAlbumCard(
    untaggedCount: Int,
    coverUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val coverRequest = remember(coverUri) {
        if (!coverUri.isNullOrBlank()) {
            CoilConfig.buildThumbnailRequest(context, coverUri, sizePx = 360)
        } else null
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .width(170.dp)
            .aspectRatio(1.15f)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 0.5.dp,
                color = com.example.phototag.ui.components.getIosHairlineColor(),
                shape = RoundedCornerShape(20.dp)
            )
            .bounceScale()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = "未分类照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.25f),
                                    Color(0xFFD97706).copy(alpha = 0.45f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            // 渐变蒙层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // 右上角角标
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFF59E0B).copy(alpha = 0.90f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    "待整理",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // 左下角：杂志标题与照片数
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "未分类照片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "$untaggedCount 张未打标",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 📱 手机物理文件夹 / 来源相册卡片 (DeviceBucketCard) - 纯文字精致风格，零 Emoji
 */
@Composable
private fun DeviceBucketCard(
    bucket: DeviceBucket,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val coverRequest = remember(bucket.coverUri) {
        if (!bucket.coverUri.isNullOrBlank()) {
            CoilConfig.buildThumbnailRequest(context, bucket.coverUri, sizePx = 360)
        } else null
    }

    Column(
        modifier = modifier
            .width(140.dp)
            .bounceScale()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(0.5.dp, getIosHairlineColor()),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (coverRequest != null) {
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = bucket.bucketName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF007AFF).copy(alpha = 0.2f),
                                        Color(0xFF5856D6).copy(alpha = 0.3f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bucket.bucketName.take(2),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }
                }

                // 纯文字小徽标：右下角张数
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.60f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${bucket.count} 项",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = bucket.bucketName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "来源相册",
            fontSize = 11.5.sp,
            color = IosSystemGray1
        )
    }
}

// ==================== 智能相册详情图片流视图 (SmartAlbumPhotoGridView) ====================

@Composable
private fun SmartAlbumPhotoGridView(
    album: SmartAlbumEntity,
    photos: List<PhotoEntity>,
    gridColumns: Int,
    tagNamesMap: Map<Long, String>,
    isMultiSelectMode: Boolean,
    selectedPhotoIds: Set<Long>,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onEditRule: () -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onStartDragSelect: (Long) -> Unit,
    onDragSelectChange: (Set<Long>) -> Unit,
    onPhotoClick: (Long, List<PhotoEntity>) -> Unit,
    onPhotoLongClick: (Long) -> Unit,
    onLoadError: (Long, String) -> Unit,
    onBatchTag: () -> Unit,
    onFilterClick: () -> Unit = {},
    onTransferTag: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()
    val timelineItems = remember(photos) { TimelineUtils.groupPhotosByDate(photos) }

    val ruleSummary = remember(album.ruleJson, tagNamesMap) {
        album.rule.getSummaryDescription(tagNamesMap)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // 顶部导航栏 (iOS 18 毛玻璃胶囊条)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getIosGlassBackground(),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, getIosHairlineColor()),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack
                )

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "匹配 ${photos.size} 张照片 · $ruleSummary",
                        fontSize = 11.5.sp,
                        color = IosSystemGray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IosButton(
                    text = "规则",
                    icon = Icons.Filled.Edit,
                    variant = IosButtonVariant.SECONDARY,
                    onClick = onEditRule,
                    height = 32.dp
                )
            }
        }

        // 照片网格或空状态
        if (photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    Text("当前相册规则下暂无匹配照片", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    IosButton(
                        text = "修改相册规则",
                        icon = Icons.Filled.Edit,
                        variant = IosButtonVariant.PRIMARY,
                        onClick = onEditRule,
                        height = 38.dp
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pinchToResizeGrid(
                            currentColumns = gridColumns,
                            onColumnsChanged = onColumnsChanged
                        )
                        .dragSelectModifier(
                            gridState = gridState,
                            items = timelineItems,
                            isMultiSelectMode = isMultiSelectMode,
                            selectedPhotoIds = selectedPhotoIds,
                            onEnterMultiSelect = { photoId ->
                                if (!isMultiSelectMode) onPhotoLongClick(photoId)
                            },
                            onDragSelectChange = onDragSelectChange
                        )
                ) {
                    items(
                        count = timelineItems.size,
                        key = { index ->
                            when (val item = timelineItems.getOrNull(index)) {
                                is TimelineGridItem.DateHeader -> "header_${item.headerId}"
                                is TimelineGridItem.Photo -> "photo_${item.photo.photoId}"
                                null -> "unknown_folder_timeline_$index"
                            }
                        },
                        span = { index ->
                            when (timelineItems.getOrNull(index)) {
                                is TimelineGridItem.DateHeader -> GridItemSpan(maxLineSpan)
                                else -> GridItemSpan(1)
                            }
                        }
                    ) { index ->
                        val item = timelineItems.getOrNull(index) ?: return@items
                        when (item) {
                            is TimelineGridItem.DateHeader -> {
                                TimelineDateHeader(
                                    title = item.dateTitle,
                                    subtitle = item.dateSubtitle,
                                    count = item.photoCount
                                )
                            }
                            is TimelineGridItem.Photo -> {
                                val photo = item.photo
                                val isSelected = photo.photoId in selectedPhotoIds

                                PhotoGridItem(
                                    photo = photo,
                                    isSelected = isSelected,
                                    isMultiSelectMode = isMultiSelectMode,
                                    onClick = { onPhotoClick(photo.photoId, photos) },
                                    onLongClick = { onPhotoLongClick(photo.photoId) },
                                    onLoadError = { onLoadError(photo.photoId, photo.uri) }
                                )
                            }
                        }
                    }
                }

                // 侧边快速滑动条 (FastScroller)
                FastScroller(
                    gridState = gridState,
                    timelineItems = timelineItems,
                    isMultiSelectMode = isMultiSelectMode,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // 🍏 底部悬浮多选操作胶囊 (FloatingCapsuleBar)
        FloatingCapsuleBar(
            visible = isMultiSelectMode && selectedPhotoIds.isNotEmpty(),
            selectedCount = selectedPhotoIds.size,
            onTagClick = onBatchTag,
            onFilterClick = onFilterClick,
            onTransferClick = onTransferTag,
            onShareClick = onShare,
            onDeleteClick = onDelete,
            onCloseSelection = onCancel
        )
    }
}

// ==================== 顶级母标签文件夹卡片 (ParentAlbumCard) ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParentAlbumCard(
    album: ParentTagAlbum,
    onClick: () -> Unit,
    onManageClick: () -> Unit,
    onTogglePinned: () -> Unit,
    onArchive: () -> Unit,
    onShareAlbum: (() -> Unit)? = null,
    onResetCover: (() -> Unit)? = null,
    hasCustomCover: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val parentTag = album.parentTag
    var showMenu by remember { mutableStateOf(false) }

    val coverRequest = remember(album.coverUri) {
        album.coverUri?.let { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build()
        }
    }

    val tagColor = remember(parentTag.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(parentTag.colorHex))
        } catch (_: Exception) {
            Color(0xFF7E9A86)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceScale()
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
    ) {
        // 1. 经典 Apple Photos 方形圆角封面 (1:1 纯正比例)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 0.5.dp,
                    color = if (parentTag.isPinned)
                        com.example.phototag.ui.theme.IosAmberDark.copy(alpha = 0.7f)
                    else
                        com.example.phototag.ui.components.getIosHairlineColor(),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 封面图片 / 占位
                if (coverRequest != null) {
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = parentTag.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(tagColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // 右上角置顶图标与操作菜单
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (parentTag.isPinned) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.PushPin,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.38f),
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable { showMenu = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    "菜单",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (parentTag.isPinned) "取消置顶" else "置顶相册") },
                                leadingIcon = { Icon(Icons.Filled.PushPin, null) },
                                onClick = {
                                    showMenu = false
                                    onTogglePinned()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("归档此相册") },
                                leadingIcon = { Icon(Icons.Filled.Archive, null) },
                                onClick = {
                                    showMenu = false
                                    onArchive()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("管理与重命名") },
                                leadingIcon = { Icon(Icons.Filled.Settings, null) },
                                onClick = {
                                    showMenu = false
                                    onManageClick()
                                }
                            )
                            if (onShareAlbum != null) {
                                DropdownMenuItem(
                                    text = { Text("分享此相册") },
                                    leadingIcon = { Icon(Icons.Filled.Share, null) },
                                    onClick = {
                                        showMenu = false
                                        onShareAlbum()
                                    }
                                )
                            }
                            if (hasCustomCover && onResetCover != null) {
                                DropdownMenuItem(
                                    text = { Text("恢复默认封面") },
                                    leadingIcon = { Icon(Icons.Filled.RestartAlt, null) },
                                    onClick = {
                                        showMenu = false
                                        onResetCover()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 2. 封面下方的排版信息 (Apple Photos 原汁原味排版)
        Text(
            text = parentTag.name,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(1.dp))

        val subAlbumText = if (album.subAlbumCount > 0)
            "${album.totalPhotoCount} 张 · ${album.subAlbumCount}个子相册"
        else
            "${album.totalPhotoCount} 张"

        Text(
            text = subAlbumText,
            fontSize = 12.sp,
            color = com.example.phototag.ui.theme.IosSystemGray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== 文件夹内页子相册流视图 (FolderPhotoGridView) ====================

@Composable
private fun FolderPhotoGridView(
    parentTag: TagEntity?,
    childTags: List<TagEntity>,
    allFolderPhotos: List<PhotoEntity>,
    allPhotosWithTags: List<PhotoWithTags> = emptyList(),
    gridColumns: Int,
    isMultiSelectMode: Boolean,
    selectedPhotoIds: Set<Long>,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onStartDragSelect: (Long) -> Unit,
    onDragSelectChange: (Set<Long>) -> Unit,
    onPhotoClick: (Long, List<PhotoEntity>) -> Unit,
    onPhotoLongClick: (Long) -> Unit,
    onLoadError: (Long, String) -> Unit,
    onBatchTag: () -> Unit,
    onFilterClick: () -> Unit = {},
    onTransferTag: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onChildTagManage: (TagEntity) -> Unit,
    onShareAlbum: (List<PhotoEntity>) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var selectedSubTagId by remember { mutableStateOf<Long?>(null) }

    // 建立 photoId -> Set<tagId> 快速索引表
    val photoToTagIdsMap = remember(allPhotosWithTags) {
        allPhotosWithTags.associate { it.photo.photoId to it.tags.map { t -> t.tagId }.toSet() }
    }

    val childTagIdsSet = remember(childTags) { childTags.map { it.tagId }.toSet() }

    // ⚡ 真实根据所选子相册过滤照片
    val displayPhotos = remember(allFolderPhotos, selectedSubTagId, parentTag, childTagIdsSet, photoToTagIdsMap) {
        when (selectedSubTagId) {
            null -> allFolderPhotos
            -1L -> {
                if (parentTag != null) {
                    allFolderPhotos.filter { photo ->
                        val tagIds = photoToTagIdsMap[photo.photoId] ?: emptySet()
                        tagIds.contains(parentTag.tagId) && tagIds.none { it in childTagIdsSet }
                    }
                } else {
                    allFolderPhotos
                }
            }
            else -> {
                allFolderPhotos.filter { photo ->
                    photoToTagIdsMap[photo.photoId]?.contains(selectedSubTagId) == true
                }
            }
        }
    }

    // 计算仅打上父标签本身（且未细分到任何子标签）的照片数量
    val parentDirectCount = remember(parentTag, childTagIdsSet, allFolderPhotos, photoToTagIdsMap) {
        if (parentTag != null) {
            allFolderPhotos.count { photo ->
                val tagIds = photoToTagIdsMap[photo.photoId] ?: emptySet()
                tagIds.contains(parentTag.tagId) && tagIds.none { it in childTagIdsSet }
            }
        } else 0
    }

    // 计算每个子相册的实际照片数量
    val subTagPhotoCounts = remember(childTags, allFolderPhotos, photoToTagIdsMap) {
        childTags.associate { child ->
            child.tagId to allFolderPhotos.count { photo ->
                photoToTagIdsMap[photo.photoId]?.contains(child.tagId) == true
            }
        }
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(selectedSubTagId) {
        gridState.scrollToItem(0)
        kotlinx.coroutines.delay(30)
        gridState.scrollToItem(0)
    }
    val timelineItems = remember(displayPhotos) {
        TimelineUtils.groupPhotosByDate(displayPhotos)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // 顶部导航栏 (iOS 18 毛玻璃与胶囊药丸)
        Surface(
            color = getIosGlassBackground(),
            border = BorderStroke(0.5.dp, getIosHairlineColor()),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                        size = 36.dp
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = parentTag?.name ?: "相册文件夹",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "共 ${displayPhotos.size} 张照片" + if (childTags.isNotEmpty()) " · ${childTags.size} 个子相册" else "",
                            fontSize = 11.5.sp,
                            color = IosSystemGray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (displayPhotos.isNotEmpty()) {
                        IosIconButton(
                            icon = Icons.Filled.Share,
                            contentDescription = "分享全部照片",
                            onClick = { onShareAlbum(displayPhotos) },
                            size = 36.dp
                        )
                    }
                }

                // 子相册横向快捷切换 Tab (iOS 18 胶囊 Pill 样式)
                if (childTags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item(key = "sub_all") {
                            val isSelected = selectedSubTagId == null
                            TagChip(
                                name = "全部 (${allFolderPhotos.size})",
                                isSelected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSubTagId = null
                                }
                            )
                        }

                        // 🎯 仅父标签专属选项（例如：仅 #i-dle）
                        if (parentTag != null) {
                            item(key = "sub_parent_only") {
                                val isParentOnlySelected = selectedSubTagId == -1L
                                val cleanName = parentTag.name.trimStart('#')
                                TagChip(
                                    name = "仅 $cleanName ($parentDirectCount)",
                                    colorHex = parentTag.colorHex,
                                    isSelected = isParentOnlySelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedSubTagId = if (isParentOnlySelected) null else -1L
                                    }
                                )
                            }
                        }

                        items(
                            items = childTags,
                            key = { it.tagId }
                        ) { childTag ->
                            val isSelected = selectedSubTagId == childTag.tagId
                            val count = subTagPhotoCounts[childTag.tagId] ?: 0
                            TagChip(
                                name = "${childTag.name} ($count)",
                                colorHex = childTag.colorHex,
                                isSelected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSubTagId = if (isSelected) null else childTag.tagId
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onChildTagManage(childTag)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 相册照片列表与时间轴
        if (displayPhotos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该相册内暂无照片", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pinchToResizeGrid(
                            currentColumns = gridColumns,
                            onColumnsChanged = onColumnsChanged
                        )
                        .dragSelectModifier(
                            gridState = gridState,
                            items = timelineItems,
                            isMultiSelectMode = isMultiSelectMode,
                            selectedPhotoIds = selectedPhotoIds,
                            onEnterMultiSelect = { photoId ->
                                if (!isMultiSelectMode) onPhotoLongClick(photoId)
                            },
                            onDragSelectChange = onDragSelectChange
                        )
                ) {
                    items(
                        count = timelineItems.size,
                        key = { index ->
                            when (val item = timelineItems.getOrNull(index)) {
                                is TimelineGridItem.DateHeader -> "header_${item.headerId}"
                                is TimelineGridItem.Photo -> "photo_${item.photo.photoId}"
                                null -> "unknown_smart_album_timeline_$index"
                            }
                        },
                        span = { index ->
                            when (timelineItems.getOrNull(index)) {
                                is TimelineGridItem.DateHeader -> GridItemSpan(maxLineSpan)
                                else -> GridItemSpan(1)
                            }
                        }
                    ) { index ->
                        val item = timelineItems.getOrNull(index) ?: return@items
                        when (item) {
                            is TimelineGridItem.DateHeader -> {
                                TimelineDateHeader(
                                    title = item.dateTitle,
                                    subtitle = item.dateSubtitle,
                                    count = item.photoCount
                                )
                            }
                            is TimelineGridItem.Photo -> {
                                val photo = item.photo
                                val isSelected = photo.photoId in selectedPhotoIds

                                PhotoGridItem(
                                    photo = photo,
                                    isSelected = isSelected,
                                    isMultiSelectMode = isMultiSelectMode,
                                    onClick = { onPhotoClick(photo.photoId, displayPhotos) },
                                    onLongClick = { onPhotoLongClick(photo.photoId) },
                                    onLoadError = { onLoadError(photo.photoId, photo.uri) }
                                )
                            }
                        }
                    }
                }

                // 侧边快速滑动条 (FastScroller)
                FastScroller(
                    gridState = gridState,
                    timelineItems = timelineItems,
                    isMultiSelectMode = isMultiSelectMode,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // 🍏 底部悬浮多选操作胶囊 (FloatingCapsuleBar)
        FloatingCapsuleBar(
            visible = isMultiSelectMode && selectedPhotoIds.isNotEmpty(),
            selectedCount = selectedPhotoIds.size,
            onTagClick = onBatchTag,
            onFilterClick = onFilterClick,
            onTransferClick = onTransferTag,
            onShareClick = onShare,
            onDeleteClick = onDelete,
            onCloseSelection = onCancel
        )
    }
}
