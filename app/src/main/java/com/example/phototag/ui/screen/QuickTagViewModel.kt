package com.example.phototag.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phototag.PhotoTagApplication
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.relation.CategoryWithTags
import com.example.phototag.data.repository.PhotoRepository
import com.example.phototag.data.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SyncState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class AutoOrganizeOptions(
    val tagByTime: Boolean = true,
    val tagBySource: Boolean = true,
    val tagByCity: Boolean = true
)

data class QuickTagUiState(
    val isLoading: Boolean = true,
    val sessionPhotos: List<PhotoEntity> = emptyList(),
    val currentIndex: Int = 0,
    val totalRemainingCount: Int = 0,
    val organizedCount: Int = 0,
    val showAutoOrganizeDialog: Boolean = false,
    val isAutoOrganizing: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val autoOrganizeProgress: Float = 0f,
    val autoOrganizeCurrent: Int = 0,
    val autoOrganizeTotal: Int = 0,
    val autoOrganizeFinishedDialog: Boolean = false,
    val autoOrganizedSuccessCount: Int = 0,
    val errorMessage: String? = null
) {
    val currentPhoto: PhotoEntity?
        get() = sessionPhotos.getOrNull(currentIndex)
}

sealed class QuickTagUndoItem {
    data class Tagged(val photo: PhotoEntity, val tagId: Long) : QuickTagUndoItem()
    data class Deleted(val photo: PhotoEntity) : QuickTagUndoItem()
    data class Skipped(val photo: PhotoEntity) : QuickTagUndoItem()
}

/**
 * 极速整理专用轻量级 ViewModel（UI 切图与数据库写入彻底解耦 + 一键智能全自动整理）。
 */
class QuickTagViewModel(
    private val photoRepository: PhotoRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickTagUiState())
    val uiState: StateFlow<QuickTagUiState> = _uiState.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val undoStack = ArrayDeque<QuickTagUndoItem>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private var isPrefetching = false

    // 全量未打标照片总数（响应式流）
    val totalUntaggedCount: StateFlow<Int> =
        photoRepository.getUntaggedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allTags: StateFlow<List<TagEntity>> =
        tagRepository.getAllTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesWithTags: StateFlow<List<CategoryWithTags>> =
        tagRepository.getAllCategoriesWithTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastUsedTagId = MutableStateFlow<Long?>(null)
    val lastUsedTagId: StateFlow<Long?> = _lastUsedTagId.asStateFlow()

    init {
        // 持续观察未打标照片数量，当发现有新照片且当前未加载时自动拉取
        viewModelScope.launch {
            photoRepository.getUntaggedCount().collect { count ->
                val currentRemaining = _uiState.value.totalRemainingCount
                if (count != currentRemaining) {
                    _uiState.update { it.copy(totalRemainingCount = count) }
                }
                if (_uiState.value.sessionPhotos.isEmpty() && count > 0) {
                    loadInitialData()
                }
            }
        }
        loadInitialData()
    }

    /**
     * 打开页面或刷新时异步加载真实未打标照片列表。
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val count = photoRepository.getUntaggedCount().firstOrNull() ?: 0
            val photos = withContext(Dispatchers.IO) {
                photoRepository.getUntaggedPhotosPaged(limit = 200, offset = 0)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sessionPhotos = photos,
                    currentIndex = 0,
                    totalRemainingCount = if (count > 0) count else photos.size
                )
            }
        }
    }

    private fun checkAndFetchMore(nextIndex: Int) {
        val state = _uiState.value
        if (nextIndex >= state.sessionPhotos.size - 10 && state.totalRemainingCount > 0 && !isPrefetching) {
            isPrefetching = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val more = photoRepository.getUntaggedPhotosPaged(limit = 100, offset = state.sessionPhotos.size)
                    if (more.isNotEmpty()) {
                        _uiState.update { it.copy(sessionPhotos = it.sessionPhotos + more) }
                    }
                } finally {
                    isPrefetching = false
                }
            }
        }
    }

    /**
     * 给当前照片打标并秒切下一张。
     */
    fun tagCurrentPhoto(tagId: Long) {
        val photo = _uiState.value.currentPhoto ?: return
        val nextIndex = _uiState.value.currentIndex + 1
        val newOrganized = _uiState.value.organizedCount + 1
        val newRemaining = (_uiState.value.totalRemainingCount - 1).coerceAtLeast(0)

        // 记录撤回操作
        undoStack.addLast(QuickTagUndoItem.Tagged(photo, tagId))
        _canUndo.value = undoStack.isNotEmpty()

        // 1. 瞬时刷新 UI，0 延迟切图
        _lastUsedTagId.value = tagId
        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                organizedCount = newOrganized,
                totalRemainingCount = newRemaining
            )
        }

        checkAndFetchMore(nextIndex)

        // 2. 后台协程静默落库
        viewModelScope.launch(Dispatchers.IO) {
            try {
                photoRepository.addTagToPhoto(photo.photoId, tagId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "打标失败：${e.localizedMessage}") }
            }
        }
    }

    /**
     * 0ms 瞬间响应打标（通过 Tag 实体）。
     */
    fun applyTagAndNext(photo: PhotoEntity, tag: TagEntity) {
        tagCurrentPhoto(tag.tagId)
    }

    /**
     * 0ms 瞬间跳过当前照片。
     */
    fun skipCurrentPhoto() {
        val photo = _uiState.value.currentPhoto
        if (photo != null) {
            undoStack.addLast(QuickTagUndoItem.Skipped(photo))
            _canUndo.value = undoStack.isNotEmpty()
        }
        val nextIndex = _uiState.value.currentIndex + 1
        _uiState.update { it.copy(currentIndex = nextIndex) }
        checkAndFetchMore(nextIndex)
    }

    /**
     * 移入安全回收站并切到下一张。
     */
    fun deleteCurrentPhoto(photo: PhotoEntity) {
        val nextIndex = _uiState.value.currentIndex + 1
        val newOrganized = _uiState.value.organizedCount + 1
        val newRemaining = (_uiState.value.totalRemainingCount - 1).coerceAtLeast(0)

        undoStack.addLast(QuickTagUndoItem.Deleted(photo))
        _canUndo.value = undoStack.isNotEmpty()

        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                organizedCount = newOrganized,
                totalRemainingCount = newRemaining
            )
        }

        checkAndFetchMore(nextIndex)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                photoRepository.softDeletePhotosByUris(listOf(photo.uri))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移入回收站失败：${e.localizedMessage}") }
            }
        }
    }

    /**
     * 撤销上一步操作（打标/移入回收站/跳过）。
     */
    fun undoLastAction() {
        val lastAction = undoStack.removeLastOrNull() ?: return
        _canUndo.value = undoStack.isNotEmpty()

        when (lastAction) {
            is QuickTagUndoItem.Tagged -> {
                val prevIndex = (_uiState.value.currentIndex - 1).coerceAtLeast(0)
                val newOrganized = (_uiState.value.organizedCount - 1).coerceAtLeast(0)
                val newRemaining = _uiState.value.totalRemainingCount + 1
                _uiState.update {
                    it.copy(
                        currentIndex = prevIndex,
                        organizedCount = newOrganized,
                        totalRemainingCount = newRemaining
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        photoRepository.removeTagFromPhoto(lastAction.photo.photoId, lastAction.tagId)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "撤销打标失败：${e.localizedMessage}") }
                    }
                }
            }
            is QuickTagUndoItem.Deleted -> {
                val prevIndex = (_uiState.value.currentIndex - 1).coerceAtLeast(0)
                val newOrganized = (_uiState.value.organizedCount - 1).coerceAtLeast(0)
                val newRemaining = _uiState.value.totalRemainingCount + 1
                _uiState.update {
                    it.copy(
                        currentIndex = prevIndex,
                        organizedCount = newOrganized,
                        totalRemainingCount = newRemaining
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        photoRepository.restorePhotosByUris(listOf(lastAction.photo.uri))
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "撤销删除失败：${e.localizedMessage}") }
                    }
                }
            }
            is QuickTagUndoItem.Skipped -> {
                val prevIndex = (_uiState.value.currentIndex - 1).coerceAtLeast(0)
                _uiState.update {
                    it.copy(currentIndex = prevIndex)
                }
            }
        }
    }

    /**
     * 极速创建并应用新标签。
     */
    fun createAndApplyTag(photo: PhotoEntity, tagName: String, categoryId: Long) {
        val formatted = if (tagName.startsWith("#")) tagName.trim() else "#${tagName.trim()}"
        viewModelScope.launch {
            try {
                val newTagId = withContext(Dispatchers.IO) {
                    tagRepository.insertTag(
                        TagEntity(
                            name = formatted,
                            categoryId = categoryId,
                            colorHex = "#7E9A86"
                        )
                    )
                }
                val newTag = TagEntity(
                    tagId = newTagId,
                    name = formatted,
                    categoryId = categoryId,
                    colorHex = "#7E9A86"
                )
                applyTagAndNext(photo, newTag)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "创建标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun createTag(name: String, categoryId: Long, colorHex: String = "#7E9A86") {
        val formatted = if (name.startsWith("#")) name.trim() else "#${name.trim()}"
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    tagRepository.insertTag(
                        TagEntity(
                            name = formatted,
                            categoryId = categoryId,
                            colorHex = colorHex
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "创建标签失败：${e.localizedMessage}") }
            }
        }
    }

    fun createCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    tagRepository.insertCategory(CategoryEntity(name = name.trim()))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "创建分类失败：${e.localizedMessage}") }
            }
        }
    }

    // ==================== ⚡ 一键智能全自动整理 ====================

    fun showAutoOrganizeDialog() {
        _uiState.update { it.copy(showAutoOrganizeDialog = true) }
    }

    fun dismissAutoOrganizeDialog() {
        _uiState.update { it.copy(showAutoOrganizeDialog = false) }
    }

    fun dismissAutoOrganizeFinishedDialog() {
        _uiState.update { it.copy(autoOrganizeFinishedDialog = false) }
        loadInitialData()
    }

    fun pauseSync() {
        _syncState.value = SyncState.PAUSED
        _uiState.update { it.copy(syncState = SyncState.PAUSED) }
    }

    fun resumeSync() {
        _syncState.value = SyncState.RUNNING
        _uiState.update { it.copy(syncState = SyncState.RUNNING) }
    }

    fun cancelSync() {
        _syncState.value = SyncState.IDLE
        _uiState.update {
            it.copy(
                isAutoOrganizing = false,
                syncState = SyncState.IDLE
            )
        }
        loadInitialData()
    }

    /**
     * 启动一键全自动智能整理后台批处理任务。
     */
    fun startAutoOrganize(options: AutoOrganizeOptions) {
        _uiState.update {
            it.copy(
                showAutoOrganizeDialog = false,
                isAutoOrganizing = true,
                syncState = SyncState.RUNNING,
                autoOrganizeProgress = 0f,
                autoOrganizeCurrent = 0,
                autoOrganizeTotal = 0
            )
        }
        _syncState.value = SyncState.RUNNING

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取全量未打标照片
                val untaggedPhotos = photoRepository.getUntaggedPhotosPaged(limit = 10000, offset = 0)
                val totalCount = untaggedPhotos.size

                if (totalCount == 0) {
                    _uiState.update {
                        it.copy(
                            isAutoOrganizing = false,
                            syncState = SyncState.COMPLETED,
                            autoOrganizeFinishedDialog = true,
                            autoOrganizedSuccessCount = 0
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(autoOrganizeTotal = totalCount) }

                // 2. 预备系统内置分类
                val timeCatId = ensureCategory("时间足迹", 1)
                val sourceCatId = ensureCategory("媒体来源", 2)
                val cityCatId = ensureCategory("地点足迹", 3)

                var processedCount = 0
                var successCount = 0

                val yearFormat = SimpleDateFormat("yyyy年", Locale.getDefault())
                val monthFormat = SimpleDateFormat("M月", Locale.getDefault())

                // 3. 批量循环处理
                for (photo in untaggedPhotos) {
                    // 挂起暂停检测
                    while (_syncState.value == SyncState.PAUSED) {
                        kotlinx.coroutines.delay(200)
                    }
                    if (_syncState.value == SyncState.IDLE) {
                        break // 用户取消
                    }

                    var hasTagged = false

                    // (1) 时间分类
                    if (options.tagByTime) {
                        val timeMs = if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L
                        val date = Date(timeMs)
                        val yearTag = "#${yearFormat.format(date)}"
                        val monthTag = "#${monthFormat.format(date)}"

                        val yearTagId = ensureTag(yearTag, timeCatId, "#7A90A4")
                        val monthTagId = ensureTag(monthTag, timeCatId, "#7A90A4")

                        photoRepository.addTagToPhoto(photo.photoId, yearTagId)
                        photoRepository.addTagToPhoto(photo.photoId, monthTagId)
                        hasTagged = true
                    }

                    // (2) 来源/格式分类
                    if (options.tagBySource) {
                        val sourceTag = when {
                            photo.isVideo -> "#视频影像"
                            photo.isGif -> "#GIF动图"
                            photo.displayName.contains("Screenshot", ignoreCase = true) ||
                            photo.displayName.contains("截图") -> "#屏幕截图"
                            photo.displayName.contains("Camera", ignoreCase = true) ||
                            photo.displayName.startsWith("IMG_") -> "#相机拍摄"
                            else -> null
                        }
                        if (sourceTag != null) {
                            val tagId = ensureTag(sourceTag, sourceCatId, "#D99B6A")
                            photoRepository.addTagToPhoto(photo.photoId, tagId)
                            hasTagged = true
                        }
                    }

                    // (3) 地理城市分类
                    if (options.tagByCity) {
                        var cityName: String? = null
                        var addr = photo.address

                        if (addr.isNullOrBlank()) {
                            // 自动全通道提取 EXIF GPS 经纬度并逆地理编码（结果自动持久化入库）
                            addr = photoRepository.resolveAndSavePhotoAddress(photo)
                        }

                        val targetLoc = photo.customLocation ?: addr
                        if (!targetLoc.isNullOrBlank()) {
                            cityName = extractCityFromAddress(targetLoc)
                        }

                        if (cityName != null) {
                            val cityTag = "#$cityName"
                            val tagId = ensureTag(cityTag, cityCatId, "#7E9A86")
                            photoRepository.addTagToPhoto(photo.photoId, tagId)
                            hasTagged = true
                        }
                    }


                    if (hasTagged) {
                        successCount++
                    }

                    processedCount++
                    val currentProgress = processedCount.toFloat() / totalCount
                    _uiState.update {
                        it.copy(
                            autoOrganizeCurrent = processedCount,
                            autoOrganizeProgress = currentProgress
                        )
                    }

                    // 释放 CPU 时间片，确保 UI 响应
                    if (processedCount % 15 == 0) {
                        kotlinx.coroutines.delay(10)
                    }
                }

                _syncState.value = SyncState.COMPLETED
                _uiState.update {
                    it.copy(
                        isAutoOrganizing = false,
                        syncState = SyncState.COMPLETED,
                        autoOrganizeFinishedDialog = true,
                        autoOrganizedSuccessCount = successCount
                    )
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.IDLE
                _uiState.update {
                    it.copy(
                        isAutoOrganizing = false,
                        syncState = SyncState.IDLE,
                        errorMessage = "自动整理异常：${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun ensureCategory(name: String, sortOrder: Int): Long {
        val existing = tagRepository.getCategoryByName(name)
        if (existing != null) return existing.categoryId
        return tagRepository.insertCategory(CategoryEntity(name = name, sortOrder = sortOrder))
    }

    private suspend fun ensureTag(name: String, categoryId: Long, colorHex: String): Long {
        val existing = tagRepository.getTagByName(name)
        if (existing != null) return existing.tagId
        return tagRepository.insertTag(
            TagEntity(
                name = name,
                categoryId = categoryId,
                colorHex = colorHex,
                isSystem = true
            )
        )
    }

    private fun extractCityFromAddress(address: String): String? {
        val clean = address.removePrefix("中华人民共和国").removePrefix("中国").trim()
        if (clean.isBlank()) return null

        // 1. 直辖市与特别行政区
        if (clean.contains("北京")) return "北京市"
        if (clean.contains("上海")) return "上海市"
        if (clean.contains("天津")) return "天津市"
        if (clean.contains("重庆")) return "重庆市"
        if (clean.contains("香港")) return "香港"
        if (clean.contains("澳门")) return "澳门"

        // 2. 标准 中国地级市/县级市 提取 (如 "吉林省长春市永吉街道..." -> "长春市")
        val cityRegex = Regex("(?:[\\u4e00-\\u9fa5]{2,10}?省)?([\\u4e00-\\u9fa5]{2,10}?市)")
        val cityMatch = cityRegex.find(clean)
        if (cityMatch != null && cityMatch.groupValues.size >= 2) {
            val candidate = cityMatch.groupValues[1].removePrefix("省")
            if (candidate.isNotBlank() && candidate.length in 2..8) {
                return candidate
            }
        }

        // 3. 自治州 / 地区 / 盟
        val regionRegex = Regex("(?:[\\u4e00-\\u9fa5]{2,10}?省)?([\\u4e00-\\u9fa5]{2,10}?(?:自治州|地区|盟))")
        val regionMatch = regionRegex.find(clean)
        if (regionMatch != null && regionMatch.groupValues.size >= 2) {
            val candidate = regionMatch.groupValues[1].removePrefix("省")
            if (candidate.isNotBlank() && candidate.length in 2..10) {
                return candidate
            }
        }

        // 4. 省份兜底
        val provinceRegex = Regex("([\\u4e00-\\u9fa5]{2,8}?省)")
        val provinceMatch = provinceRegex.find(clean)
        if (provinceMatch != null) {
            return provinceMatch.value
        }

        // 5. 常见海外大城市
        val foreignCities = listOf("首尔", "东京", "大阪", "京都", "巴黎", "伦敦", "纽约", "旧金山", "洛杉矶", "新加坡", "曼谷", "悉尼", "多伦多")
        for (fc in foreignCities) {
            if (clean.contains(fc)) return fc
        }

        // 6. 短地名兜底
        if (clean.length in 2..6 && !clean.contains("路") && !clean.contains("号") && !clean.contains("街")) {
            return clean
        }

        return null
    }

    companion object {
        fun factory(
            photoRepository: PhotoRepository,
            tagRepository: TagRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuickTagViewModel(photoRepository, tagRepository) as T
            }
        }
    }
}
