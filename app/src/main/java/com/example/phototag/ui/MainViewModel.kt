package com.example.phototag.ui

import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phototag.util.DeleteRequestResult
import com.example.phototag.util.MediaStoreDeleteHelper
import com.example.phototag.util.TrashRequestResult
import com.example.phototag.PhotoTagApplication
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.HealthReport
import com.example.phototag.data.model.ParentTagAlbum
import com.example.phototag.data.model.TagNode
import com.example.phototag.data.model.TagWithCount
import com.example.phototag.data.preference.UserPreferences
import com.example.phototag.data.relation.CategoryWithTags
import com.example.phototag.data.relation.PhotoWithTags
import com.example.phototag.data.repository.PhotoRepository
import com.example.phototag.data.repository.TagRepository
import com.example.phototag.util.AlbumHealthChecker
import com.example.phototag.util.PinyinUtils
import com.example.phototag.util.ThumbnailPreloader
import com.example.phototag.util.TimelineGridItem
import com.example.phototag.util.TimelineUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==================== 筛选逻辑模式 ====================

enum class FilterMode {
    /** 且 (AND)：必须同时包含所有选中的标签（交集） */
    AND,
    /** 或 (OR)：包含任意一个选中的标签即可（并集） */
    OR
}

/**
 * 🌟 Aves 级多维交叉过滤切面 (MediaFacetFilter)
 */
enum class MediaFacetFilter(val label: String) {
    ALL("全部"),
    VIDEO("视频"),
    SCREENSHOT("截图"),
    RAW("RAW"),
    FAVORITE("已收藏"),
    TAGGED("已打标"),
    UNTAGGED("未打标")
}

/**
 * 📊 全局媒体类型资产统计
 */
data class MediaCounts(
    val total: Int = 0,
    val videos: Int = 0,
    val screenshots: Int = 0,
    val raws: Int = 0,
    val favorites: Int = 0,
    val tagged: Int = 0,
    val untagged: Int = 0
)

// ==================== UI State ====================

data class MainUiState(
    val searchQuery: String = "",
    val activeFilterTagIds: Set<Long> = emptySet(),
    val selectedRootTagId: Long? = null,        // 当前选中的顶级父标签
    val selectedChildTagId: Long? = null,       // 当前选中的子标签（若为 null 表示选中父标签下的全部）
    val filterMode: FilterMode = FilterMode.AND,
    val isMultiSelectMode: Boolean = false,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val isScanning: Boolean = false,
    val isOcrScanning: Boolean = false,
    val showBatchTagSheet: Boolean = false,
    val showCreateTagDialog: Boolean = false,
    val managingTag: TagEntity? = null,         // 正在深度管理的标签（重命名/合并/删除弹窗）
    val hasMediaPermission: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: Int = 0,                   // 0 = 全部照片, 1 = 标签相册
    val tagAlbumSelectedTagId: Long? = null,    // 标签相册内已展开的标签
    val pendingDeleteUris: List<Uri> = emptyList(),
    val pendingDeletePhotoIds: List<Long> = emptyList(),
    val pendingTrashIntentSender: android.content.IntentSender? = null,
    val pendingTrashUris: List<String> = emptyList(),
    val pendingTrashIsTrashed: Boolean = true,
    // 大图预览器状态
    val isViewingPhoto: Boolean = false,
    val viewerInitialIndex: Int = 0,
    val viewerPhotos: List<PhotoEntity> = emptyList(),
    val viewerTaggingPhotoId: Long? = null,
    // ⚡ 极速整理模式
    val isQuickTagMode: Boolean = false,
    // ✨ 智能动态虚拟相册
    val selectedSmartAlbum: com.example.phototag.data.entity.SmartAlbumEntity? = null,
    val showCreateSmartAlbumSheet: Boolean = false,
    val editingSmartAlbum: com.example.phototag.data.entity.SmartAlbumEntity? = null,
    // 🔄 标签转移与替换
    val showTransferTagSheet: Boolean = false,
    // 🧹 相册健康巡检与自愈
    val showHealthCheckSheet: Boolean = false,
    // 🗑️ 7天安全回收站
    val isViewingTrash: Boolean = false,
    val lastSoftDeletedUris: List<String> = emptyList(),
    // 📌 未打标照片专属筛选与相册
    val isUntaggedFilterActive: Boolean = false,
    val isViewingUntaggedAlbum: Boolean = false,
    // ✌️ 双指捏合网格列数 (3 <-> 5)
    val gridColumns: Int = 3,
    // 🎨 图片编辑引擎 (裁剪 + 模板水印)
    val isEditingPhoto: Boolean = false,
    val editingPhoto: PhotoEntity? = null,
    // 🎞️ 批量胶片调色
    val showBatchFilterSheet: Boolean = false,
    val isBatchFiltering: Boolean = false,
    // 🌟 Aves 级多维交叉过滤切面 (全部/视频/截图/RAW/收藏/已打标/未打标)
    val mediaFacetFilter: MediaFacetFilter = MediaFacetFilter.ALL,
    // 🧹 Samsung 相册瘦身工坊 (Gallery Cleanup Lab)
    val showCleanupLabSheet: Boolean = false,
    // 🧪 Samsung 相册实验室 (Gallery Labs)
    val showGalleryLabsSheet: Boolean = false
)

// ==================== One-shot Events ====================

sealed class UiEvent {
    data class SharePhotos(val uris: List<Uri>) : UiEvent()
    data class ShowTransferUndoSnackbar(
        val message: String,
        val photoUris: List<String>,
        val fromTagId: Long,
        val toTagId: Long
    ) : UiEvent()
    data class ShowTrashUndoSnackbar(
        val message: String,
        val photoUris: List<String>
    ) : UiEvent()
}

// ==================== ViewModel ====================

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(
    val photoRepository: PhotoRepository,
    val tagRepository: TagRepository,
    val smartAlbumRepository: com.example.phototag.data.repository.SmartAlbumRepository,
    private val userPreferences: UserPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 自动预装系统默认智能相册（📍 地点足迹）
                smartAlbumRepository.ensurePreloadedSmartAlbums()
                // 2. 自动预装系统专有相册与打标（#视频、#动图、#地点）
                photoRepository.ensureMediaSpecialTagsApplied()
            } catch (_: Exception) {}
        }
    }

    enum class ScrollTarget { TOP, BOTTOM }
    private val _scrollEvents = MutableSharedFlow<ScrollTarget>(extraBufferCapacity = 2)
    val scrollEvents = _scrollEvents.asSharedFlow()

    fun requestScrollToTop() {
        _scrollEvents.tryEmit(ScrollTarget.TOP)
    }

    fun requestScrollToBottom() {
        _scrollEvents.tryEmit(ScrollTarget.BOTTOM)
    }

    private val _searchQuery = MutableStateFlow("")
    private val _selectedRootTagId = MutableStateFlow<Long?>(null)
    private val _selectedChildTagId = MutableStateFlow<Long?>(null)
    private val _activeFilterTagIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _filterMode = MutableStateFlow(FilterMode.AND)
    private val _isUntaggedFilterActive = MutableStateFlow(false)

    // 网格列数（支持双指捏合缩放切换并持久化）
    private val _gridColumns = MutableStateFlow(userPreferences?.gridColumns ?: 3)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    // 拖动连选基准选区
    private var baseDragSelection: Set<Long> = emptySet()

    // 分类+标签树
    val categoriesWithTags: StateFlow<List<CategoryWithTags>> =
        tagRepository.getAllCategoriesWithTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 所有标签
    val allTags: StateFlow<List<TagEntity>> =
        tagRepository.getAllTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 顶级根标签（parentId == null）
    val rootTags: StateFlow<List<TagEntity>> =
        allTags.map { list -> list.filter { it.parentId == null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 子标签字典 (parentId -> List<ChildTags>)
    val childTagsMap: StateFlow<Map<Long, List<TagEntity>>> =
        allTags.map { list -> list.filter { it.parentId != null }.groupBy { it.parentId!! } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 标签相册基础数据
    val tagsWithCount: StateFlow<List<TagWithCount>> =
        tagRepository.getTagsWithPhotoCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 自定义相册封面映射表 (tagId -> coverUri)
    private val _customAlbumCovers = MutableStateFlow<Map<Long, String>>(userPreferences?.getAllCustomAlbumCovers() ?: emptyMap())
    val customAlbumCovers: StateFlow<Map<Long, String>> = _customAlbumCovers.asStateFlow()

    // 母子层级文件夹相册模型（置顶优先，其次按自定义 sortOrder 排序）
    val parentTagAlbums: StateFlow<List<ParentTagAlbum>> =
        combine(allTags, tagsWithCount, _customAlbumCovers) { tags, tagCounts, customCovers ->
            val countMap = tagCounts.associateBy { it.tagId }
            val rootTagsList = tags.filter { it.parentId == null }
                .sortedWith(
                    compareByDescending<TagEntity> { it.isPinned }
                        .thenBy { it.sortOrder }
                        .thenByDescending { it.createdAt }
                )
            rootTagsList.map { root ->
                val children = tags.filter { it.parentId == root.tagId }
                    .sortedWith(
                        compareByDescending<TagEntity> { it.isPinned }
                            .thenBy { it.sortOrder }
                            .thenByDescending { it.createdAt }
                    )
                val directCount = countMap[root.tagId]?.photoCount ?: 0
                val totalCount = directCount + children.sumOf { countMap[it.tagId]?.photoCount ?: 0 }
                val cover = customCovers[root.tagId]
                    ?: countMap[root.tagId]?.coverUri
                    ?: children.firstNotNullOfOrNull { customCovers[it.tagId] ?: countMap[it.tagId]?.coverUri }
                ParentTagAlbum(
                    parentTag = root,
                    childTags = children,
                    directPhotoCount = directCount,
                    totalPhotoCount = totalCount,
                    coverUri = cover
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 📦 已归档母子层级文件夹相册模型
    val archivedParentTagAlbums: StateFlow<List<ParentTagAlbum>> =
        combine(
            tagRepository.getArchivedTagsFlow(),
            tagRepository.getArchivedTagsWithCount(),
            _customAlbumCovers
        ) { tags, tagCounts, customCovers ->
            val countMap = tagCounts.associateBy { it.tagId }
            val rootTagsList = tags.filter { it.parentId == null }
                .sortedByDescending { it.createdAt }
            rootTagsList.map { root ->
                val children = tags.filter { it.parentId == root.tagId }
                    .sortedByDescending { it.createdAt }
                val directCount = countMap[root.tagId]?.photoCount ?: 0
                val totalCount = directCount + children.sumOf { countMap[it.tagId]?.photoCount ?: 0 }
                val cover = customCovers[root.tagId]
                    ?: countMap[root.tagId]?.coverUri
                    ?: children.firstNotNullOfOrNull { customCovers[it.tagId] ?: countMap[it.tagId]?.coverUri }
                ParentTagAlbum(
                    parentTag = root,
                    childTags = children,
                    directPhotoCount = directCount,
                    totalPhotoCount = totalCount,
                    coverUri = cover
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // 响应式过滤后的照片列表（支持拼音首字母/全拼智能联想 + 全局 OCR 文本搜索 + 母子标签级联筛选 + AND/OR 标签联合查询 + 未打标独立筛选 + Aves 级多维媒体类型快筛）
    val filteredPhotos: StateFlow<List<PhotoEntity>> =
        combine(
            _searchQuery.debounce(250),
            _uiState,
            allTags
        ) { query, state, tags ->
            SearchContext(
                query = query,
                selectedRootTagId = state.selectedRootTagId,
                selectedChildTagId = state.selectedChildTagId,
                tagIds = state.activeFilterTagIds,
                mode = state.filterMode,
                isUntagged = state.isUntaggedFilterActive,
                allTags = tags,
                facet = state.mediaFacetFilter
            )
        }.flatMapLatest { ctx ->
            val query = ctx.query.trim()
            val baseFlow = when {
                query.isNotBlank() -> {
                    if (ctx.isUntagged) {
                        photoRepository.getUntaggedPhotosFlow().map { list ->
                            list.filter { photo ->
                                photo.displayName.contains(query, ignoreCase = true) ||
                                (photo.ocrText?.contains(query, ignoreCase = true) == true)
                            }
                        }
                    } else {
                        // 1. 拼音首字母/全拼智能匹配出对应的 Tag IDs
                        val pinyinMatchedTagIds = ctx.allTags
                            .filter { PinyinUtils.matchesQuery(it.name, query) }
                            .map { it.tagId }

                        if (pinyinMatchedTagIds.isNotEmpty()) {
                            // 联合查询：OCR文字/文件名匹配 + 拼音命中标签关联的照片
                            combine(
                                photoRepository.searchPhotosByTextAndTags(query.trimStart('#')),
                                photoRepository.getPhotosByAnyTagIds(pinyinMatchedTagIds)
                            ) { ocrPhotos, tagPhotos ->
                                (ocrPhotos + tagPhotos).distinctBy { it.photoId }
                            }
                        } else {
                            photoRepository.searchPhotosByTextAndTags(query.trimStart('#'))
                        }
                    }
                }
                ctx.isUntagged -> {
                    photoRepository.getUntaggedPhotosFlow()
                }
                ctx.selectedRootTagId != null -> {
                    val children = ctx.allTags.filter { it.parentId == ctx.selectedRootTagId }.map { it.tagId }
                    when {
                        ctx.selectedChildTagId == -1L -> {
                            // 🎯 用户点击了【仅 #父标签】-> 只查询被打上该父标签本身、且未打上任何下属子标签的照片！
                            photoRepository.getPhotosByParentTagExcludingChildren(ctx.selectedRootTagId, children)
                        }
                        ctx.selectedChildTagId != null -> {
                            // 🎯 用户点击了二级栏的具体子标签（如 #叶舒华）-> 精确查询属于该子标签的照片
                            photoRepository.getPhotosByAllTagIds(listOf(ctx.selectedChildTagId))
                        }
                        else -> {
                            // 🎯 用户点击了顶级父标签（如 #i-dle）的【全部】-> 级联查询属于父标签本身，或属于其下任意子标签的照片 (OR 级联)
                            val allRelatedIds = (setOf(ctx.selectedRootTagId) + children).toList()
                            photoRepository.getPhotosByAnyTagIds(allRelatedIds)
                        }
                    }
                }
                ctx.tagIds.isNotEmpty() -> {
                    if (ctx.tagIds.size == 1 || ctx.mode == FilterMode.AND) {
                        photoRepository.getPhotosByAllTagIds(ctx.tagIds.toList())
                    } else {
                        photoRepository.getPhotosByAnyTagIds(ctx.tagIds.toList())
                    }
                }
                else -> photoRepository.getAllPhotos()
            }

            // 🌟 Aves 级多维交叉过滤逻辑结合
            if (ctx.facet == MediaFacetFilter.ALL) {
                baseFlow
            } else {
                combine(baseFlow, photoRepository.getAllPhotosWithTags()) { photos, photosWithTags ->
                    when (ctx.facet) {
                        MediaFacetFilter.ALL -> photos
                        MediaFacetFilter.VIDEO -> photos.filter { it.isVideo }
                        MediaFacetFilter.SCREENSHOT -> photos.filter { it.isScreenshot }
                        MediaFacetFilter.RAW -> photos.filter { it.isRaw }
                        MediaFacetFilter.FAVORITE -> {
                            val favIds = photosWithTags
                                .filter { item -> item.tags.any { it.name.trimStart('#') == "收藏" } }
                                .map { it.photo.photoId }
                                .toSet()
                            photos.filter { it.photoId in favIds }
                        }
                        MediaFacetFilter.TAGGED -> {
                            val taggedIds = photosWithTags
                                .filter { it.tags.isNotEmpty() }
                                .map { it.photo.photoId }
                                .toSet()
                            photos.filter { it.photoId in taggedIds }
                        }
                        MediaFacetFilter.UNTAGGED -> {
                            val taggedIds = photosWithTags
                                .filter { it.tags.isNotEmpty() }
                                .map { it.photo.photoId }
                                .toSet()
                            photos.filter { it.photoId !in taggedIds }
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 按日期时间轴重组的网格数据流（包含 Sticky Date Header 与 Photo 项）。
     */
    val timelineItems: StateFlow<List<TimelineGridItem>> = filteredPhotos
        .map { photos -> TimelineUtils.groupPhotosByDate(photos) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class SearchContext(
        val query: String,
        val selectedRootTagId: Long?,
        val selectedChildTagId: Long?,
        val tagIds: Set<Long>,
        val mode: FilterMode,
        val isUntagged: Boolean,
        val allTags: List<TagEntity>,
        val facet: MediaFacetFilter
    )

    // 📊 全局媒体类型资产统计流 (支持秒级联动)
    val mediaCounts: StateFlow<MediaCounts> = photoRepository.getAllPhotosWithTags()
        .map { list ->
            val total = list.size
            val videos = list.count { it.photo.isVideo }
            val screenshots = list.count { it.photo.isScreenshot }
            val raws = list.count { it.photo.isRaw }
            val favorites = list.count { item -> item.tags.any { it.name.trimStart('#') == "收藏" } }
            val tagged = list.count { it.tags.isNotEmpty() }
            val untagged = total - tagged
            MediaCounts(
                total = total,
                videos = videos,
                screenshots = screenshots,
                raws = raws,
                favorites = favorites,
                tagged = tagged,
                untagged = untagged
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaCounts())

    // 收藏照片 ID 集合（支持网格秒级纯文字角标渲染，0 重组）
    val favoritePhotoIds: StateFlow<Set<Long>> = photoRepository.getAllPhotosWithTags()
        .map { list ->
            list.filter { item -> item.tags.any { it.name.trimStart('#') == "收藏" } }
                .map { it.photo.photoId }
                .toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // 📌 未打标照片专属数据流与总数
    val untaggedPhotos: StateFlow<List<PhotoEntity>> =
        photoRepository.getUntaggedPhotosFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val untaggedCount: StateFlow<Int> =
        photoRepository.getUntaggedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setMediaFacetFilter(facet: MediaFacetFilter) {
        _uiState.update { current ->
            val next = if (current.mediaFacetFilter == facet) MediaFacetFilter.ALL else facet
            current.copy(mediaFacetFilter = next)
        }
    }

    // 🗑️ 7天安全回收站数据流
    val trashPhotos: StateFlow<List<PhotoEntity>> =
        photoRepository.trashPhotos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashPhotoCount: StateFlow<Int> =
        photoRepository.trashPhotoCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 🗺️ 所有带有经纬度的足迹照片流（用于照片足迹地图）
    val photosWithLocation: StateFlow<List<PhotoEntity>> =
        photoRepository.getAllPhotosWithCoordinatesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 重新扫描并补齐相册中已有照片的 GPS 经纬度与海拔 */
    fun refreshGpsMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            photoRepository.syncMissingGpsMetadata()
        }
    }

    // 全部照片+标签（实时关联）
    val allPhotosWithTags: StateFlow<List<PhotoWithTags>> =
        photoRepository.getAllPhotosWithTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 标签相册内的照片列表
    val tagAlbumPhotos: StateFlow<List<PhotoEntity>> =
        _uiState.flatMapLatest { state ->
            val tagId = state.tagAlbumSelectedTagId
            if (tagId != null) {
                // 如果是父标签，则联合查询父标签+子标签的照片
                val children = allTags.value.filter { it.parentId == tagId }.map { it.tagId }
                val allRelatedIds = setOf(tagId) + children
                photoRepository.getPhotosByAnyTagIds(allRelatedIds.toList())
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== 📱 手机物理文件夹 / 来源相册 (MediaStore Bucket) ====================
    val deviceBuckets: StateFlow<List<com.example.phototag.data.model.DeviceBucket>> = photoRepository.deviceBuckets

    val selectedDeviceBucket = MutableStateFlow<com.example.phototag.data.model.DeviceBucket?>(null)

    val deviceBucketPhotos: StateFlow<List<PhotoEntity>> = combine(
        selectedDeviceBucket,
        filteredPhotos
    ) { bucket, photos ->
        if (bucket == null) emptyList()
        else {
            val uriSet = bucket.photoUris.toSet()
            photos.filter { it.uri in uriSet }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openDeviceBucket(bucket: com.example.phototag.data.model.DeviceBucket) {
        selectedDeviceBucket.value = bucket
    }

    fun closeDeviceBucket() {
        selectedDeviceBucket.value = null
    }

    // 最近使用标签（按使用时间排序，最多 6 个）
    val recentTags: StateFlow<List<TagEntity>> =
        tagRepository.getRecentTags(6)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ⚡ 记忆最后一次使用的标签
    val lastUsedTag: StateFlow<TagEntity?> =
        recentTags.map { it.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 高频常用标签（按使用频次排序，最多 8 个）
    val topUsedTags: StateFlow<List<TagEntity>> =
        tagRepository.getTopUsedTags(8)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ✨ 智能动态虚拟相册（响应式流）
    val smartAlbumsWithPhotos: StateFlow<List<com.example.phototag.data.model.SmartAlbumWithPhotos>> =
        smartAlbumRepository.getSmartAlbumsWithPhotosFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选中的智能动态相册照片流
    val selectedSmartAlbumPhotos: StateFlow<List<PhotoEntity>> =
        _uiState.map { it.selectedSmartAlbum }
            .flatMapLatest { album ->
                if (album != null) {
                    smartAlbumRepository.getPhotosByRuleFlow(album.rule)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== 网格列数持久化 ====================

    fun setGridColumns(columns: Int) {
        val safeColumns = columns.coerceIn(2, 5)
        _gridColumns.value = safeColumns
        userPreferences?.gridColumns = safeColumns
        _uiState.update { it.copy(gridColumns = safeColumns) }
    }

    fun toggleGridColumns() {
        val next = when (_gridColumns.value) {
            2 -> 3
            3 -> 4
            4 -> 5
            else -> 3
        }
        setGridColumns(next)
    }

    // ==================== 滑动批量连选 (Drag-to-Select) ====================

    fun startDragSelection(initialPhotoId: Long) {
        val base = if (_uiState.value.isMultiSelectMode) _uiState.value.selectedPhotoIds else emptySet()
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                selectedPhotoIds = base + initialPhotoId
            )
        }
    }

    fun updateDragSelection(draggedPhotoIds: Set<Long>) {
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                selectedPhotoIds = draggedPhotoIds
            )
        }
    }

    /**
     * 📅 日期分组一键全选/反选整天照片。
     */
    fun toggleGroupSelection(photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        val currentSelected = _uiState.value.selectedPhotoIds
        val isAllInGroupSelected = photoIds.all { it in currentSelected }

        val newSelected = if (isAllInGroupSelected) {
            currentSelected - photoIds.toSet()
        } else {
            currentSelected + photoIds
        }

        _uiState.update {
            it.copy(
                isMultiSelectMode = newSelected.isNotEmpty(),
                selectedPhotoIds = newSelected
            )
        }
    }

    // ==================== 权限 ====================

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasMediaPermission = true) }
        syncPhotos()
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(hasMediaPermission = false) }
    }

    // ==================== 相册同步 ====================

    private var isSyncInProgress = false

    fun syncPhotos() {
        if (isSyncInProgress) return
        viewModelScope.launch {
            isSyncInProgress = true
            _uiState.update { it.copy(isScanning = true) }
            try {
                photoRepository.syncPhotosFromMediaStore()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "同步相册失败：${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
                isSyncInProgress = false
            }
        }
    }

    // ==================== 搜索 & 筛选 ====================

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 切换【📌 未打标照片】专属筛选模式。
     */
    fun toggleUntaggedFilter() {
        val next = !_isUntaggedFilterActive.value
        _isUntaggedFilterActive.value = next
        if (next) {
            _activeFilterTagIds.value = emptySet()
            _uiState.update {
                it.copy(
                    isUntaggedFilterActive = true,
                    activeFilterTagIds = emptySet(),
                    selectedRootTagId = null,
                    selectedChildTagId = null
                )
            }
        } else {
            _uiState.update { it.copy(isUntaggedFilterActive = false) }
        }
    }

    fun openUntaggedAlbum() {
        _uiState.update { it.copy(isViewingUntaggedAlbum = true) }
    }

    fun closeUntaggedAlbum() {
        _uiState.update {
            it.copy(
                isViewingUntaggedAlbum = false,
                isMultiSelectMode = false,
                selectedPhotoIds = emptySet()
            )
        }
    }

    /**
     * 点击顶级父标签胶囊（母标签两级联动展开）。
     */
    fun selectRootTag(rootTagId: Long) {
        _isUntaggedFilterActive.value = false
        if (_selectedRootTagId.value == rootTagId && _selectedChildTagId.value == null) {
            // 再次点击已选中的父标签 -> 取消选中并回到全部
            clearFilters()
        } else {
            _selectedRootTagId.value = rootTagId
            _selectedChildTagId.value = null
            _activeFilterTagIds.value = emptySet()
            _uiState.update {
                it.copy(
                    isUntaggedFilterActive = false,
                    selectedRootTagId = rootTagId,
                    selectedChildTagId = null,
                    activeFilterTagIds = emptySet()
                )
            }
        }
    }

    /**
     * 点击子标签（精确过滤该子标签，或传入 null 查看该父标签下全部）。
     */
    fun selectChildTag(childTagId: Long?) {
        _isUntaggedFilterActive.value = false
        _selectedChildTagId.value = childTagId
        _activeFilterTagIds.value = emptySet()
        _uiState.update {
            it.copy(
                isUntaggedFilterActive = false,
                selectedChildTagId = childTagId,
                activeFilterTagIds = emptySet()
            )
        }
    }

    fun toggleFilterTag(tagId: Long) {
        _isUntaggedFilterActive.value = false
        _selectedRootTagId.value = null
        _selectedChildTagId.value = null
        val current = _activeFilterTagIds.value
        val updated = if (tagId in current) current - tagId else current + tagId
        _activeFilterTagIds.value = updated
        _uiState.update {
            it.copy(
                isUntaggedFilterActive = false,
                selectedRootTagId = null,
                selectedChildTagId = null,
                activeFilterTagIds = updated
            )
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedRootTagId.value = null
        _selectedChildTagId.value = null
        _activeFilterTagIds.value = emptySet()
        _isUntaggedFilterActive.value = false
        _uiState.update {
            it.copy(
                searchQuery = "",
                activeFilterTagIds = emptySet(),
                selectedRootTagId = null,
                selectedChildTagId = null,
                isUntaggedFilterActive = false
            )
        }
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
        _uiState.update { it.copy(filterMode = mode) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    // ==================== 标签相册导航 ====================

    fun openTagAlbum(tagId: Long) {
        _uiState.update { it.copy(tagAlbumSelectedTagId = tagId) }
    }

    fun closeTagAlbum() {
        _uiState.update { it.copy(tagAlbumSelectedTagId = null, isMultiSelectMode = false, selectedPhotoIds = emptySet()) }
    }

    // ==================== 标签深度管理弹窗 ====================

    fun openTagManagement(tag: TagEntity) {
        _uiState.update { it.copy(managingTag = tag) }
    }

    fun closeTagManagement() {
        _uiState.update { it.copy(managingTag = null) }
    }

    fun updateTagNameAndColor(tagId: Long, newName: String, newColorHex: String) {
        viewModelScope.launch {
            try {
                val formatted = if (newName.startsWith("#")) newName.trim() else "#${newName.trim()}"
                tagRepository.updateTagNameAndColor(tagId, formatted, newColorHex)
                closeTagManagement()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "修改标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun updateTagFull(
        tagId: Long,
        newName: String,
        newColorHex: String,
        categoryId: Long,
        parentId: Long?,
        newCategoryName: String? = null
    ) {
        viewModelScope.launch {
            try {
                val finalCategoryId = if (!newCategoryName.isNullOrBlank()) {
                    val cat = tagRepository.getOrCreateCategory(newCategoryName)
                    cat.categoryId
                } else {
                    categoryId
                }
                val formatted = if (newName.startsWith("#")) newName.trim() else "#${newName.trim()}"
                tagRepository.updateTagFull(tagId, formatted, newColorHex, finalCategoryId, parentId)
                closeTagManagement()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun mergeTags(sourceTagId: Long, targetTagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.mergeTags(sourceTagId, targetTagId)
                _activeFilterTagIds.update { it - sourceTagId }
                closeTagManagement()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "合并标签失败：${e.localizedMessage}") }
            }
        }
    }

    /** 📦 归档 / 取消归档相册标签 */
    fun setTagArchived(tagId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            try {
                tagRepository.setTagArchived(tagId, isArchived)
                closeTagManagement()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "操作归档失败：${e.localizedMessage}") }
            }
        }
    }

    /** 🖼️ 设置或重置标签相册的自定义封面 */
    fun setAlbumCustomCover(tagId: Long, uri: String?) {
        userPreferences?.setCustomAlbumCover(tagId, uri)
        _customAlbumCovers.update { current ->
            if (uri == null) current - tagId else current + (tagId to uri)
        }
    }

    /** 📤 一键批量分享/导出相册内全部照片 */
    fun shareAlbumPhotos(photos: List<PhotoEntity>, context: Context, stripLocation: Boolean = true) {
        if (photos.isEmpty()) return
        val uris = photos.map { Uri.parse(it.uri) }
        viewModelScope.launch {
            val shareUris = if (stripLocation) {
                com.example.phototag.util.ExifMetadataUtil.stripGpsMetadataForShare(context, uris)
            } else {
                uris
            }
            _events.emit(UiEvent.SharePhotos(shareUris))
        }
    }

    // ==================== 大图查看器 ====================


    fun openPhotoViewer(photos: List<PhotoEntity>, initialIndex: Int) {
        _uiState.update {
            it.copy(
                isViewingPhoto = true,
                viewerPhotos = photos,
                viewerInitialIndex = initialIndex
            )
        }
    }

    fun closePhotoViewer() {
        _uiState.update { it.copy(isViewingPhoto = false, viewerTaggingPhotoId = null) }
    }

    fun openViewerTaggingSheet(photoId: Long) {
        _uiState.update { it.copy(viewerTaggingPhotoId = photoId) }
    }

    fun closeViewerTaggingSheet() {
        _uiState.update { it.copy(viewerTaggingPhotoId = null) }
    }

    /**
     * 更新照片的手动标注地点名称
     */
    fun updatePhotoCustomLocation(photoId: Long, customLocation: String?) {
        viewModelScope.launch {
            try {
                photoRepository.updatePhotoCustomLocation(photoId, customLocation)
                // 同步刷新查看器中的照片列表缓存
                _uiState.update { state ->
                    val updatedPhotos = state.viewerPhotos.map {
                        if (it.photoId == photoId) it.copy(customLocation = customLocation) else it
                    }
                    state.copy(viewerPhotos = updatedPhotos)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "更新地点失败", e)
            }
        }
    }

    /**
     * 后台静默发起全自动双通道逆地理编码，并将地址持久化保存至数据库
     */
    fun resolvePhotoAddressSilently(photo: PhotoEntity, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val resolved = photoRepository.resolveAndSavePhotoAddress(photo)
                if (!resolved.isNullOrBlank()) {
                    _uiState.update { state ->
                        val updatedPhotos = state.viewerPhotos.map {
                            if (it.photoId == photo.photoId) it.copy(address = resolved) else it
                        }
                        state.copy(viewerPhotos = updatedPhotos)
                    }
                }
                onResult(resolved)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "自动逆编码失败", e)
                onResult(null)
            }
        }
    }

    /**
     * 将从 EXIF / 物理文件实时解析出的经纬度、海拔与逆编码地址持久化写入 Room 数据库
     */
    fun saveParsedMetadata(photoId: Long, latitude: Double?, longitude: Double?, address: String?, altitude: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                photoRepository.updatePhotoLocationFull(photoId, latitude, longitude, address, altitude)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "保存解析元数据失败", e)
            }
        }
    }

    fun shareSinglePhoto(photo: PhotoEntity, stripLocation: Boolean = true, context: android.content.Context? = null) {
        val uri = Uri.parse(photo.uri)
        viewModelScope.launch {
            val shareUris = if (stripLocation && context != null) {
                com.example.phototag.util.ExifMetadataUtil.stripGpsMetadataForShare(context, listOf(uri))
            } else {
                listOf(uri)
            }
            _events.emit(UiEvent.SharePhotos(shareUris))
        }
    }

    fun shareSpoofedPhoto(photo: PhotoEntity, config: com.example.phototag.util.exif.SpoofConfig, context: android.content.Context) {
        val uri = Uri.parse(photo.uri)
        viewModelScope.launch {
            try {
                val spoofedUri = com.example.phototag.util.exif.ExifSpoofEngine.createSpoofedTempFile(context, uri, config)
                _events.emit(UiEvent.SharePhotos(listOf(spoofedUri)))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "生成伪装分享图片失败：${e.localizedMessage}") }
            }
        }
    }

    fun saveSpoofedPhotoCopy(photo: PhotoEntity, config: com.example.phototag.util.exif.SpoofConfig, context: android.content.Context) {
        val uri = Uri.parse(photo.uri)
        viewModelScope.launch {
            try {
                val savedUri = com.example.phototag.util.exif.ExifSpoofEngine.saveSpoofedPhotoToMediaStore(context, uri, config)
                if (savedUri != null) {
                    _uiState.update { it.copy(errorMessage = "已成功保存伪装副本到系统相册") }
                } else {
                    _uiState.update { it.copy(errorMessage = "保存伪装副本失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "保存失败：${e.localizedMessage}") }
            }
        }
    }

    fun requestDeleteSinglePhoto(photo: PhotoEntity) {
        val uri = Uri.parse(photo.uri)
        _uiState.update {
            it.copy(
                pendingDeleteUris = listOf(uri),
                pendingDeletePhotoIds = listOf(photo.photoId)
            )
        }
    }

    fun onPhotoLoadFailed(photoId: Long, uri: String) {
        // 🚨 严防误删：滑动渲染临时未命中或协程取消时绝不删除本地媒体记录，仅记录调试日志
        // 只有用户主动进入「相册健康体检」并确认清理时才进行物理死链核查
        android.util.Log.d("MainViewModel", "Photo thumbnail load error: id=$photoId, uri=$uri")
    }

    // ==================== 多选模式 ====================

    fun enterMultiSelectMode(initialPhotoId: Long? = null) {
        val initial = if (initialPhotoId != null) setOf(initialPhotoId) else emptySet()
        _uiState.update { it.copy(isMultiSelectMode = true, selectedPhotoIds = initial) }
    }

    fun exitMultiSelectMode() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = false,
                selectedPhotoIds = emptySet(),
                showBatchTagSheet = false,
                showTransferTagSheet = false,
                showBatchFilterSheet = false
            )
        }
    }

    fun togglePhotoSelection(photoId: Long) {
        _uiState.update { state ->
            val current = state.selectedPhotoIds
            val updated = if (photoId in current) current - photoId else current + photoId
            state.copy(
                selectedPhotoIds = updated,
                isMultiSelectMode = true
            )
        }
    }

    fun selectAll(allPhotoIds: List<Long>) {
        _uiState.update { it.copy(selectedPhotoIds = allPhotoIds.toSet()) }
    }

    // ==================== 批量打标与标签转移 ====================

    fun showBatchTagSheet() = _uiState.update { it.copy(showBatchTagSheet = true) }
    fun hideBatchTagSheet() = _uiState.update { it.copy(showBatchTagSheet = false) }

    fun openTransferTagSheet() = _uiState.update { it.copy(showTransferTagSheet = true) }
    fun closeTransferTagSheet() = _uiState.update { it.copy(showTransferTagSheet = false) }

    fun openBatchFilterSheet() = _uiState.update { it.copy(showBatchFilterSheet = true) }
    fun closeBatchFilterSheet() = _uiState.update { it.copy(showBatchFilterSheet = false) }

    /**
     * 🎞️ 批量为所选照片应用复古胶片/CCD滤镜与石英后盖时间戳并生成无损副本
     */
    /**
     * 🎞️ 批量为所选照片应用复古胶片/CCD滤镜与石英后盖时间戳/漏光/星光镜/拍立得相框并生成副本
     */
    fun batchApplyFilmFilter(
        context: Context,
        preset: com.example.phototag.util.editor.FilmPreset,
        options: com.example.phototag.util.editor.FilmEffectOptions = com.example.phototag.util.editor.FilmEffectOptions()
    ) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        if (photoIds.isEmpty()) return
        closeBatchFilterSheet()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBatchFiltering = true, errorMessage = "正在批量应用 ${preset.displayName} 调色...") }
            var successCount = 0
            val allPhotos = allPhotosWithTags.value.map { it.photo }
            val targetPhotos = allPhotos.filter { it.photoId in photoIds }
            for ((index, photo) in targetPhotos.withIndex()) {
                try {
                    _uiState.update { it.copy(errorMessage = "正在冲印 (${index + 1}/${targetPhotos.size})...") }
                    val uri = Uri.parse(photo.uri)
                    val bmp = com.example.phototag.util.editor.ImageEditorEngine.loadBitmapWithCorrectOrientation(context, uri)
                    if (bmp != null) {
                        var filtered = com.example.phototag.util.editor.FilmFilterEngine.applyFilmFilter(
                            source = bmp,
                            preset = preset,
                            enableGrain = true,
                            enableVignette = true,
                            enableLightLeak = options.withLightLeak,
                            enableStarFilter = options.withStarFilter
                        )
                        val timeMs = if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L

                        if (options.withTimestamp) {
                            val stamped = com.example.phototag.util.editor.WatermarkRenderer.applyWatermark(
                                source = filtered,
                                type = com.example.phototag.util.editor.WatermarkType.DAZZ_VCR_TILTED,
                                metadata = com.example.phototag.util.editor.WatermarkMetadata(
                                    dateTaken = java.util.Date(timeMs),
                                    position = com.example.phototag.util.editor.WatermarkPosition.BOTTOM_RIGHT,
                                    pattern = "''yy MM dd"
                                ),
                                context = context
                            )
                            if (stamped !== filtered && filtered !== bmp) {
                                filtered.recycle()
                            }
                            filtered = stamped
                        }

                        if (options.withPolaroidFrame) {
                            val framed = com.example.phototag.util.editor.WatermarkRenderer.applyWatermark(
                                source = filtered,
                                type = com.example.phototag.util.editor.WatermarkType.POLAROID_FRAME,
                                metadata = com.example.phototag.util.editor.WatermarkMetadata(dateTaken = java.util.Date(timeMs))
                            )
                            if (framed !== filtered && filtered !== bmp) {
                                filtered.recycle()
                            }
                            filtered = framed
                        }

                        val savedUri = com.example.phototag.util.editor.ImageEditorEngine.saveEditedPhoto(context, photo, filtered)
                        if (bmp !== filtered) {
                            bmp.recycle()
                        }
                        filtered.recycle()
                        if (savedUri != null) {
                            successCount++
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "批量调色单张失败", e)
                }
            }
            exitMultiSelectMode()
            try {
                photoRepository.syncPhotosFromMediaStore()
            } catch (_: Exception) {}
            _uiState.update {
                it.copy(
                    isBatchFiltering = false,
                    errorMessage = "已成功完成批量调色，生成 $successCount 张 ${preset.displayName} 副本！"
                )
            }
        }
    }

    /**
     * 🎞️ 为单张照片快速套用 Dazz 复古胶片/CCD滤镜与石英后盖时间戳/漏光/星光镜/拍立得相框并生成副本
     */
    fun applyFilmFilterToSinglePhoto(
        photo: PhotoEntity,
        context: Context,
        preset: com.example.phototag.util.editor.FilmPreset = com.example.phototag.util.editor.FilmPreset.CPM35,
        options: com.example.phototag.util.editor.FilmEffectOptions = com.example.phototag.util.editor.FilmEffectOptions(withTimestamp = false)
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(errorMessage = "正在冲印 ${preset.displayName} 胶片副本...") }
            try {
                val uri = Uri.parse(photo.uri)
                val bmp = com.example.phototag.util.editor.ImageEditorEngine.loadBitmapWithCorrectOrientation(context, uri)
                if (bmp != null) {
                    var filtered = com.example.phototag.util.editor.FilmFilterEngine.applyFilmFilter(
                        source = bmp,
                        preset = preset,
                        enableGrain = true,
                        enableVignette = true,
                        enableLightLeak = options.withLightLeak,
                        enableStarFilter = options.withStarFilter
                    )
                    val timeMs = if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L

                    if (options.withTimestamp) {
                        val stamped = com.example.phototag.util.editor.WatermarkRenderer.applyWatermark(
                            source = filtered,
                            type = com.example.phototag.util.editor.WatermarkType.DAZZ_VCR_TILTED,
                            metadata = com.example.phototag.util.editor.WatermarkMetadata(
                                dateTaken = java.util.Date(timeMs),
                                position = com.example.phototag.util.editor.WatermarkPosition.BOTTOM_RIGHT,
                                pattern = "''yy MM dd"
                            ),
                            context = context
                        )
                        if (stamped !== filtered && filtered !== bmp) {
                            filtered.recycle()
                        }
                        filtered = stamped
                    }

                    if (options.withPolaroidFrame) {
                        val framed = com.example.phototag.util.editor.WatermarkRenderer.applyWatermark(
                            source = filtered,
                            type = com.example.phototag.util.editor.WatermarkType.POLAROID_FRAME,
                            metadata = com.example.phototag.util.editor.WatermarkMetadata(dateTaken = java.util.Date(timeMs))
                        )
                        if (framed !== filtered && filtered !== bmp) {
                            filtered.recycle()
                        }
                        filtered = framed
                    }

                    val savedUri = com.example.phototag.util.editor.ImageEditorEngine.saveEditedPhoto(context, photo, filtered)
                    if (bmp !== filtered) {
                        bmp.recycle()
                    }
                    filtered.recycle()
                    if (savedUri != null) {
                        try {
                            photoRepository.syncPhotosFromMediaStore()
                        } catch (_: Exception) {}
                        _uiState.update { it.copy(errorMessage = "已成功生成 ${preset.displayName} 复古胶片副本！") }
                    } else {
                        _uiState.update { it.copy(errorMessage = "保存胶片照片失败") }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "单张套用胶片滤镜失败", e)
                _uiState.update { it.copy(errorMessage = "冲印失败：${e.localizedMessage}") }
            }
        }
    }

    fun transferPhotosTag(fromTagId: Long?, toTagId: Long) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        if (photoIds.isEmpty()) return
        val photoUris = allPhotosWithTags.value
            .filter { it.photo.photoId in photoIds }
            .map { it.photo.uri }

        val fromTag = allTags.value.find { it.tagId == fromTagId }
        val toTag = allTags.value.find { it.tagId == toTagId }

        viewModelScope.launch {
            try {
                val count = photoRepository.transferPhotosToTag(photoIds, fromTagId, toTagId)
                closeTransferTagSheet()
                exitMultiSelectMode()
                if (fromTagId != null) {
                    val msg = "已成功将 $count 张照片从「${fromTag?.name ?: "原标签"}」转移至「${toTag?.name ?: "新标签"}」"
                    _events.emit(
                        UiEvent.ShowTransferUndoSnackbar(
                            message = msg,
                            photoUris = photoUris,
                            fromTagId = fromTagId,
                            toTagId = toTagId
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "转移替换标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun transferPhotosBetweenTags(photoUris: List<String>, fromTagId: Long, toTagId: Long) {
        val fromTag = allTags.value.find { it.tagId == fromTagId }
        val toTag = allTags.value.find { it.tagId == toTagId }

        viewModelScope.launch {
            try {
                val result = photoRepository.transferPhotosBetweenTags(photoUris, fromTagId, toTagId)
                result.onSuccess { count ->
                    closeTransferTagSheet()
                    exitMultiSelectMode()
                    val msg = "已成功将 $count 张照片从「${fromTag?.name ?: "原标签"}」转移至「${toTag?.name ?: "新标签"}」"
                    _events.emit(
                        UiEvent.ShowTransferUndoSnackbar(
                            message = msg,
                            photoUris = photoUris,
                            fromTagId = fromTagId,
                            toTagId = toTagId
                        )
                    )
                }.onFailure { e ->
                    _uiState.update { it.copy(errorMessage = "转移失败：${e.localizedMessage}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "转移失败：${e.localizedMessage}") }
            }
        }
    }

    fun undoTransfer(photoUris: List<String>, originalFromTagId: Long, originalToTagId: Long) {
        viewModelScope.launch {
            try {
                photoRepository.undoTransferPhotos(photoUris, originalFromTagId, originalToTagId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "撤销失败：${e.localizedMessage}") }
            }
        }
    }

    fun batchToggleTag(tagId: Long, add: Boolean) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            try {
                if (add) {
                    photoRepository.batchTagPhotos(photoIds, listOf(tagId))
                } else {
                    photoRepository.batchRemoveTagsFromPhotos(photoIds, listOf(tagId))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "操作失败：${e.localizedMessage}") }
            }
        }
    }

    /**
     * ⚡ 直接为所选照片批量打上指定标签（一键同上/记忆打标）。
     */
    fun applyTagToPhotos(photoIds: Collection<Long>, tag: TagEntity) {
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            try {
                photoRepository.batchTagPhotos(photoIds.toList(), listOf(tag.tagId))
                exitMultiSelectMode()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "打标失败：${e.localizedMessage}") }
            }
        }
    }

    fun addTagToPhoto(photoId: Long, tagId: Long) {
        viewModelScope.launch {
            try {
                photoRepository.addTagToPhoto(photoId, tagId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun removeTagFromPhoto(photoId: Long, tagId: Long) {
        viewModelScope.launch {
            try {
                photoRepository.removeTagFromPhoto(photoId, tagId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移除标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun toggleFavoriteTag(photoId: Long) {
        viewModelScope.launch {
            try {
                val favTag = tagRepository.getOrCreateTag(
                    name = "#收藏",
                    colorHex = "#E57373"
                )
                val isTagged = photoRepository.isPhotoTagged(photoId, favTag.tagId)
                if (isTagged) {
                    photoRepository.removeTagFromPhoto(photoId, favTag.tagId)
                } else {
                    photoRepository.addTagToPhoto(photoId, favTag.tagId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "收藏操作失败：${e.localizedMessage}") }
            }
        }
    }

    fun removeTagFromPhotos(photoUris: List<String>, tagId: Long) {
        if (photoUris.isEmpty()) return
        viewModelScope.launch {
            try {
                val photos = allPhotosWithTags.value.filter { it.photo.uri in photoUris }
                val ids = photos.map { it.photo.photoId }
                if (ids.isNotEmpty()) {
                    photoRepository.batchRemoveTagsFromPhotos(ids, listOf(tagId))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "撤销打标失败：${e.localizedMessage}") }
            }
        }
    }

    /**
     * 批量收藏/取消收藏所选照片
     */
    fun batchToggleFavoriteSelectedPhotos() {
        val selectedIds = _uiState.value.selectedPhotoIds.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            try {
                val favTag = tagRepository.getOrCreateTag(
                    name = "#收藏",
                    colorHex = "#E57373"
                )
                // 检查所选照片中是否全部都已被收藏
                val allWithTags = allPhotosWithTags.value.filter { it.photo.photoId in selectedIds }
                val allAlreadyFavorited = allWithTags.isNotEmpty() && allWithTags.all { item ->
                    item.tags.any { it.tagId == favTag.tagId }
                }

                if (allAlreadyFavorited) {
                    // 全体取消收藏
                    photoRepository.batchRemoveTagsFromPhotos(selectedIds, listOf(favTag.tagId))
                    _uiState.update { it.copy(errorMessage = "已取消 ${selectedIds.size} 项的收藏") }
                } else {
                    // 全体添加收藏
                    photoRepository.batchTagPhotos(selectedIds, listOf(favTag.tagId))
                    _uiState.update { it.copy(errorMessage = "已将 ${selectedIds.size} 项加入收藏") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "批量收藏操作失败：${e.localizedMessage}") }
            }
        }
    }

    /**
     * 一键清空所选照片的所有标签
     */
    fun batchClearAllTagsFromSelectedPhotos() {
        val selectedIds = _uiState.value.selectedPhotoIds.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            try {
                photoRepository.clearAllTagsForPhotos(selectedIds)
                _uiState.update {
                    it.copy(
                        showBatchTagSheet = false,
                        isMultiSelectMode = false,
                        selectedPhotoIds = emptySet(),
                        errorMessage = "已清空所选 ${selectedIds.size} 项的所有标签关联"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "清空标签失败：${e.localizedMessage}") }
            }
        }
    }

    /**
     * 反选当前列表中的照片
     */
    fun invertSelection(visiblePhotoIds: List<Long>) {
        val currentSelected = _uiState.value.selectedPhotoIds
        val newSelected = visiblePhotoIds.filter { it !in currentSelected }.toSet()
        _uiState.update {
            it.copy(
                selectedPhotoIds = newSelected,
                isMultiSelectMode = newSelected.isNotEmpty()
            )
        }
    }

    // ==================== 批量分享 ====================

    fun shareSelectedPhotos(stripLocation: Boolean = true, context: android.content.Context? = null) {
        val photoIds = _uiState.value.selectedPhotoIds
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            photoRepository.getAllPhotos().collect { list ->
                val selected = list.filter { it.photoId in photoIds }
                val uris = selected.map { Uri.parse(it.uri) }
                if (uris.isNotEmpty()) {
                    val shareUris = if (stripLocation && context != null) {
                        com.example.phototag.util.ExifMetadataUtil.stripGpsMetadataForShare(context, uris)
                    } else {
                        uris
                    }
                    _events.emit(UiEvent.SharePhotos(shareUris))
                }
                return@collect
            }
        }
    }



    // ==================== ⚡ 极速整理模式 ====================

    fun openQuickTagMode() {
        _uiState.update { it.copy(isQuickTagMode = true) }
    }

    fun closeQuickTagMode() {
        _uiState.update { it.copy(isQuickTagMode = false) }
    }

    // ==================== 批量删除 ====================

    fun requestDeleteSelected(photos: List<PhotoEntity>) {
        val selectedIds = _uiState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        val targets = photos.filter { it.photoId in selectedIds }
        val uris = targets.map { Uri.parse(it.uri) }
        val ids = targets.map { it.photoId }
        _uiState.update {
            it.copy(
                pendingDeleteUris = uris,
                pendingDeletePhotoIds = ids
            )
        }
    }

    fun requestDeleteSelectedPhotos() {
        val selectedIds = _uiState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            photoRepository.getAllPhotos().collect { allPhotos ->
                requestDeleteSelected(allPhotos)
                return@collect
            }
        }
    }

    fun clearPendingDelete() {
        _uiState.update {
            it.copy(
                pendingDeleteUris = emptyList(),
                pendingDeletePhotoIds = emptyList()
            )
        }
    }

    /**
     * 确认删除（执行软删除移入 7 天安全回收站，完全不删原文件，且弹出撤销 Snackbar）。
     */
    fun onDeleteConfirmed() {
        val photoIds = _uiState.value.pendingDeletePhotoIds
        val pendingUris = _uiState.value.pendingDeleteUris.map { it.toString() }
        if (photoIds.isEmpty() && pendingUris.isEmpty()) return

        viewModelScope.launch {
            try {
                val urisToDelete = if (pendingUris.isNotEmpty()) {
                    pendingUris
                } else {
                    allPhotosWithTags.value.filter { it.photo.photoId in photoIds }.map { it.photo.uri }
                }

                if (urisToDelete.isNotEmpty()) {
                    photoRepository.softDeletePhotosByUris(urisToDelete)
                } else {
                    photoRepository.softDeletePhotosByIds(photoIds)
                }

                _uiState.update {
                    val remainingViewerPhotos = it.viewerPhotos.filter { photo -> photo.photoId !in photoIds && photo.uri !in urisToDelete }
                    it.copy(
                        pendingDeleteUris = emptyList(),
                        pendingDeletePhotoIds = emptyList(),
                        lastSoftDeletedUris = urisToDelete,
                        isMultiSelectMode = false,
                        selectedPhotoIds = emptySet(),
                        viewerPhotos = remainingViewerPhotos,
                        isViewingPhoto = if (remainingViewerPhotos.isEmpty()) false else it.isViewingPhoto
                    )
                }

                _events.emit(
                    UiEvent.ShowTrashUndoSnackbar(
                        message = "已移至回收站（7天内可随时恢复）",
                        photoUris = urisToDelete
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移入回收站失败：${e.localizedMessage}") }
            }
        }
    }

    // ==================== 🗑️ 7天安全回收站核心业务 ====================

    fun openTrashScreen() {
        _uiState.update { it.copy(isViewingTrash = true) }
    }

    fun closeTrashScreen() {
        _uiState.update { it.copy(isViewingTrash = false) }
    }

    fun requestSoftDeletePhotos(uris: List<String>, context: Context) {
        if (uris.isEmpty()) return
        val parsedUris = uris.map { Uri.parse(it) }
        when (val result = MediaStoreDeleteHelper.prepareTrashRequest(context, parsedUris, isTrashed = true)) {
            is TrashRequestResult.NeedsUserConsent -> {
                _uiState.update {
                    it.copy(
                        pendingTrashIntentSender = result.intentSender,
                        pendingTrashUris = uris,
                        pendingTrashIsTrashed = true
                    )
                }
            }
            is TrashRequestResult.DirectSuccess, is TrashRequestResult.Error -> {
                softDeletePhotos(uris)
            }
        }
    }

    fun requestSoftDeleteSelectedPhotos(context: Context) {
        val selectedIds = _uiState.value.selectedPhotoIds
        if (selectedIds.isEmpty()) return
        val uris = allPhotosWithTags.value
            .filter { it.photo.photoId in selectedIds }
            .map { it.photo.uri }
        if (uris.isNotEmpty()) {
            requestSoftDeletePhotos(uris, context)
        }
    }

    fun onTrashConsentConfirmed() {
        val uris = _uiState.value.pendingTrashUris
        val isTrashed = _uiState.value.pendingTrashIsTrashed
        _uiState.update {
            it.copy(
                pendingTrashIntentSender = null,
                pendingTrashUris = emptyList()
            )
        }
        if (uris.isNotEmpty()) {
            if (isTrashed) {
                softDeletePhotos(uris)
            } else {
                restorePhotos(uris)
            }
        }
    }

    fun onTrashConsentCancelled() {
        _uiState.update {
            it.copy(
                pendingTrashIntentSender = null,
                pendingTrashUris = emptyList()
            )
        }
    }

    fun softDeletePhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                photoRepository.softDeletePhotosByUris(uris)
                _uiState.update { state ->
                    val remainingViewerPhotos = state.viewerPhotos.filter { photo -> photo.uri !in uris }
                    state.copy(
                        lastSoftDeletedUris = uris,
                        isMultiSelectMode = false,
                        selectedPhotoIds = emptySet(),
                        viewerPhotos = remainingViewerPhotos,
                        isViewingPhoto = if (remainingViewerPhotos.isEmpty()) false else state.isViewingPhoto,
                        viewerInitialIndex = state.viewerInitialIndex.coerceIn(0, (remainingViewerPhotos.size - 1).coerceAtLeast(0))
                    )
                }
                _events.emit(
                    UiEvent.ShowTrashUndoSnackbar(
                        message = "已移至回收站（7天内可随时恢复）",
                        photoUris = uris
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移入回收站失败：${e.localizedMessage}") }
            }
        }
    }

    fun softDeleteSelectedPhotos() {
        val selectedIds = _uiState.value.selectedPhotoIds.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            try {
                val activePhotos = photoRepository.getAllPhotosList()
                val uris = activePhotos.filter { it.photoId in selectedIds }.map { it.uri }
                if (uris.isNotEmpty()) {
                    softDeletePhotos(uris)
                } else {
                    photoRepository.softDeletePhotosByIds(selectedIds)
                    _uiState.update { state ->
                        val remainingViewerPhotos = state.viewerPhotos.filter { photo -> photo.photoId !in selectedIds }
                        state.copy(
                            isMultiSelectMode = false,
                            selectedPhotoIds = emptySet(),
                            viewerPhotos = remainingViewerPhotos,
                            isViewingPhoto = if (remainingViewerPhotos.isEmpty()) false else state.isViewingPhoto,
                            viewerInitialIndex = state.viewerInitialIndex.coerceIn(0, (remainingViewerPhotos.size - 1).coerceAtLeast(0))
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移入回收站失败：${e.localizedMessage}") }
            }
        }
    }

    fun restorePhotos(uris: List<String>, context: Context? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                if (context != null) {
                    val parsed = uris.map { Uri.parse(it) }
                    MediaStoreDeleteHelper.prepareTrashRequest(context, parsed, isTrashed = false)
                }
                val restored = photoRepository.restorePhotosByUris(uris)
                photoRepository.syncPhotosFromMediaStore()
                _uiState.update {
                    it.copy(
                        errorMessage = "已成功恢复 $restored 项内容到相册",
                        lastSoftDeletedUris = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "恢复照片失败：${e.localizedMessage}") }
            }
        }
    }

    fun undoLastSoftDelete(context: Context? = null) {
        val uris = _uiState.value.lastSoftDeletedUris
        if (uris.isNotEmpty()) {
            restorePhotos(uris, context)
        }
    }

    fun hardDeletePhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                val count = photoRepository.hardDeletePhotosByUris(uris)
                _uiState.update { it.copy(errorMessage = "已彻底删除 $count 项内容") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "彻底删除失败：${e.localizedMessage}") }
            }
        }
    }

    fun clearAllTrash() {
        viewModelScope.launch {
            try {
                val count = photoRepository.clearAllTrash()
                _uiState.update { it.copy(errorMessage = "回收站已彻底清空（共清空 $count 项）") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "清空回收站失败：${e.localizedMessage}") }
            }
        }
    }

    // ==================== 🎨 图片编辑引擎 (裁剪与模板水印) ====================

    fun openImageEditor(photo: PhotoEntity) {
        _uiState.update { it.copy(isEditingPhoto = true, editingPhoto = photo) }
    }

    fun closeImageEditor() {
        _uiState.update { it.copy(isEditingPhoto = false, editingPhoto = null) }
    }

    fun onPhotoEdited(newUri: Uri) {
        _uiState.update { it.copy(isEditingPhoto = false, editingPhoto = null) }
        syncPhotos()
    }

    // ==================== 标签 & 分类 CRUD ====================

    fun showCreateTagDialog() = _uiState.update { it.copy(showCreateTagDialog = true) }
    fun hideCreateTagDialog() = _uiState.update { it.copy(showCreateTagDialog = false) }

    fun createTag(
        name: String,
        categoryId: Long,
        colorHex: String = "#7E9A86",
        parentId: Long? = null,
        newCategoryName: String? = null
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val finalCategoryId = if (!newCategoryName.isNullOrBlank()) {
                    val cat = tagRepository.getOrCreateCategory(newCategoryName)
                    cat.categoryId
                } else {
                    categoryId
                }
                val formatted = if (name.startsWith("#")) name.trim() else "#${name.trim()}"
                tagRepository.insertTag(
                    TagEntity(
                        name = formatted,
                        categoryId = finalCategoryId,
                        parentId = parentId,
                        colorHex = colorHex,
                        isSystem = false
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "创建标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun deleteTag(tag: TagEntity) {
        deleteTagById(tag.tagId)
    }

    fun deleteTagById(tagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.deleteTagById(tagId)
                _activeFilterTagIds.update { it - tagId }
                _uiState.update {
                    it.copy(
                        activeFilterTagIds = _activeFilterTagIds.value,
                        tagAlbumSelectedTagId = if (it.tagAlbumSelectedTagId == tagId) null else it.tagAlbumSelectedTagId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun createCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                tagRepository.insertCategory(CategoryEntity(name = name.trim()))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "创建分类失败：${e.localizedMessage}") }
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            try {
                tagRepository.deleteCategoryWithTags(category)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除分类失败：${e.localizedMessage}") }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    // ==================== ✨ 智能动态虚拟相册管理 ====================

    fun openCreateSmartAlbumSheet(editing: com.example.phototag.data.entity.SmartAlbumEntity? = null) {
        _uiState.update {
            it.copy(showCreateSmartAlbumSheet = true, editingSmartAlbum = editing)
        }
    }

    fun closeCreateSmartAlbumSheet() {
        _uiState.update {
            it.copy(showCreateSmartAlbumSheet = false, editingSmartAlbum = null)
        }
    }

    fun saveSmartAlbum(name: String, iconName: String, colorHex: String, rule: com.example.phototag.data.model.SmartAlbumRule) {
        viewModelScope.launch {
            try {
                val editing = _uiState.value.editingSmartAlbum
                if (editing != null) {
                    smartAlbumRepository.updateSmartAlbum(
                        id = editing.id,
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        rule = rule
                    )
                } else {
                    smartAlbumRepository.createSmartAlbum(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        rule = rule
                    )
                }
                closeCreateSmartAlbumSheet()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "保存智能相册失败：${e.localizedMessage}") }
            }
        }
    }

    fun deleteSmartAlbum(albumId: Long) {
        viewModelScope.launch {
            try {
                smartAlbumRepository.deleteSmartAlbum(albumId)
                if (_uiState.value.selectedSmartAlbum?.id == albumId) {
                    closeSmartAlbumView()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除智能相册失败：${e.localizedMessage}") }
            }
        }
    }

    fun selectSmartAlbum(album: com.example.phototag.data.entity.SmartAlbumEntity?) {
        _uiState.update { it.copy(selectedSmartAlbum = album) }
    }

    fun closeSmartAlbumView() {
        _uiState.update { it.copy(selectedSmartAlbum = null) }
    }

    // ==================== 📌 相册置顶与拖拽重排 ====================

    fun toggleTagPinned(tagId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                tagRepository.setTagPinned(tagId, isPinned)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "设置置顶失败：${e.localizedMessage}") }
            }
        }
    }

    fun reorderParentTagAlbums(orderedTagIds: List<Long>) {
        viewModelScope.launch {
            try {
                tagRepository.updateTagOrders(orderedTagIds)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新排序失败：${e.localizedMessage}") }
            }
        }
    }


    /**
     * 一键导出并分享整个母子相册内的所有照片
     */
    fun shareParentAlbum(album: ParentTagAlbum, context: Context, stripLocation: Boolean = true) {
        val tagIds = listOf(album.parentTag.tagId) + album.childTags.map { it.tagId }
        viewModelScope.launch {
            try {
                val photos = photoRepository.getPhotosByAnyTagIds(tagIds).firstOrNull() ?: emptyList()
                if (photos.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "当前相册暂无可分享的照片") }
                } else {
                    shareAlbumPhotos(photos, context, stripLocation)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "分享相册失败：${e.localizedMessage}") }
            }
        }
    }

    // ==================== 🧹 相册健康度巡检与自愈 ====================

    private val healthChecker by lazy {
        AlbumHealthChecker(tagRepository, photoRepository, PhotoTagApplication.instance)
    }

    private val _isHealthChecking = MutableStateFlow(false)
    val isHealthChecking: StateFlow<Boolean> = _isHealthChecking.asStateFlow()

    private val _healthReport = MutableStateFlow<HealthReport?>(null)
    val healthReport: StateFlow<HealthReport?> = _healthReport.asStateFlow()

    fun openHealthCheckSheet() {
        _uiState.update { it.copy(showHealthCheckSheet = true) }
        runHealthCheck()
    }

    fun closeHealthCheckSheet() {
        _uiState.update { it.copy(showHealthCheckSheet = false) }
    }

    fun runHealthCheck() {
        viewModelScope.launch {
            _isHealthChecking.value = true
            try {
                val report = healthChecker.performHealthCheck()
                _healthReport.value = report
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "健康巡检失败：${e.localizedMessage}") }
            } finally {
                _isHealthChecking.value = false
            }
        }
    }

    fun cleanOrphanTags(tagIds: List<Long>) {
        viewModelScope.launch {
            try {
                val deleted = tagRepository.deleteTagsByIds(tagIds)
                _uiState.update { it.copy(errorMessage = "已成功清理 $deleted 个 0 照片幽灵标签") }
                runHealthCheck()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "清理幽灵标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun mergeSimilarTags(sourceTagId: Long, targetTagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.mergeTags(sourceTagId, targetTagId)
                _activeFilterTagIds.update { it - sourceTagId }
                _uiState.update { it.copy(errorMessage = "已成功合并相似标签") }
                runHealthCheck()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "合并相似标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun cleanDeadReferences() {
        viewModelScope.launch {
            try {
                val deadCrossRefs = photoRepository.deleteDeadCrossRefs()
                val deadTagRefs = tagRepository.deleteOrphanTagCrossRefs()
                val total = deadCrossRefs + deadTagRefs
                _uiState.update { it.copy(errorMessage = "已自愈修复 $total 条失效脏关联索引") }
                runHealthCheck()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "自愈脏索引失败：${e.localizedMessage}") }
            }
        }
    }

    fun autoFixAllHealthIssues() {
        val currentReport = _healthReport.value ?: return
        viewModelScope.launch {
            try {
                // 1. 批量清理所有幽灵标签
                if (currentReport.orphanTags.isNotEmpty()) {
                    tagRepository.deleteTagsByIds(currentReport.orphanTags.map { it.tagId })
                }
                // 2. 清理脏索引
                photoRepository.deleteDeadCrossRefs()
                tagRepository.deleteOrphanTagCrossRefs()
                _uiState.update { it.copy(errorMessage = "相册全盘自愈与冗余清理完成") }
                runHealthCheck()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "一键全盘自愈失败：${e.localizedMessage}") }
            }
        }
    }

    // ==================== 🧹 Samsung 相册瘦身工坊 (Gallery Cleanup Lab) ====================
    fun openCleanupLab() {
        _uiState.update { it.copy(showCleanupLabSheet = true) }
    }

    fun closeCleanupLab() {
        _uiState.update { it.copy(showCleanupLabSheet = false) }
    }

    // ==================== 🧪 Samsung 相册实验室 (Gallery Labs) ====================
    fun openGalleryLabs() {
        _uiState.update { it.copy(showGalleryLabsSheet = true) }
    }

    fun closeGalleryLabs() {
        _uiState.update { it.copy(showGalleryLabsSheet = false) }
    }

    // ==================== Factory ====================

    companion object {
        fun factory(
            photoRepository: PhotoRepository,
            tagRepository: TagRepository,
            smartAlbumRepository: com.example.phototag.data.repository.SmartAlbumRepository,
            userPreferences: UserPreferences? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(photoRepository, tagRepository, smartAlbumRepository, userPreferences) as T
                }
            }
    }
}
