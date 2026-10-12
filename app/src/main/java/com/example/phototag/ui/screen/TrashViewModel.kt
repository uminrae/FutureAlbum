package com.example.phototag.ui.screen

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.repository.PhotoRepository
import com.example.phototag.util.DeleteRequestResult
import com.example.phototag.util.MediaStoreDeleteHelper
import com.example.phototag.util.TrashRequestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val selectedUris: Set<String> = emptySet(),
    val pendingDeleteIntentSender: IntentSender? = null,
    val pendingDeleteUris: List<String> = emptyList(),
    val pendingRestoreIntentSender: IntentSender? = null,
    val pendingRestoreUris: List<String> = emptyList(),
    val showClearAllDialog: Boolean = false,
    val showHardDeleteDialog: Boolean = false,
    val previewPhoto: PhotoEntity? = null,
    val isProcessing: Boolean = false,
    val toastMessage: String? = null
)

/**
 * 🗑️ 7天安全回收站独立 ViewModel（管理 7 天暂存、一键原样恢复与物理级彻底销毁）。
 */
class TrashViewModel(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    // 观察回收站照片列表
    val trashPhotos: StateFlow<List<PhotoEntity>> =
        photoRepository.trashPhotos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 观察回收站照片总数
    val trashPhotoCount: StateFlow<Int> =
        photoRepository.trashPhotoCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleSelect(uri: String) {
        _uiState.update { state ->
            val current = state.selectedUris
            val updated = if (uri in current) current - uri else current + uri
            state.copy(selectedUris = updated)
        }
    }

    fun selectAll() {
        val all = trashPhotos.value.map { it.uri }.toSet()
        _uiState.update { it.copy(selectedUris = all) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedUris = emptySet()) }
    }

    fun openPhotoPreview(photo: PhotoEntity) {
        _uiState.update { it.copy(previewPhoto = photo) }
    }

    fun closePhotoPreview() {
        _uiState.update { it.copy(previewPhoto = null) }
    }

    fun showClearAllDialog() = _uiState.update { it.copy(showClearAllDialog = true) }
    fun dismissClearAllDialog() = _uiState.update { it.copy(showClearAllDialog = false) }

    fun showHardDeleteDialog() = _uiState.update { it.copy(showHardDeleteDialog = true) }
    fun dismissHardDeleteDialog() = _uiState.update { it.copy(showHardDeleteDialog = false) }

    // ==================== ↩️ 一键原样恢复（全系统重新可见） ====================

    fun restoreSelectedPhotos(context: Context) {
        val uris = _uiState.value.selectedUris.toList()
        if (uris.isEmpty()) return
        executeRestore(context, uris)
    }

    fun restoreSinglePhoto(context: Context, uri: String) {
        _uiState.update { it.copy(previewPhoto = null) }
        executeRestore(context, listOf(uri))
    }

    private fun executeRestore(context: Context, uriStrings: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsedUris = uriStrings.map { Uri.parse(it) }
            val result = MediaStoreDeleteHelper.prepareTrashRequest(context, parsedUris, isTrashed = false)

            when (result) {
                is TrashRequestResult.NeedsUserConsent -> {
                    _uiState.update {
                        it.copy(
                            pendingRestoreIntentSender = result.intentSender,
                            pendingRestoreUris = uriStrings
                        )
                    }
                }
                is TrashRequestResult.DirectSuccess -> {
                    val count = photoRepository.restorePhotosByUris(uriStrings)
                    photoRepository.syncPhotosFromMediaStore()
                    _uiState.update {
                        it.copy(
                            selectedUris = it.selectedUris - uriStrings.toSet(),
                            toastMessage = "已成功恢复 $count 张照片至相册"
                        )
                    }
                }
                is TrashRequestResult.Error -> {
                    _uiState.update {
                        it.copy(
                            toastMessage = "恢复失败：${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun onSystemRestoreConfirmed() {
        val uris = _uiState.value.pendingRestoreUris
        _uiState.update {
            it.copy(pendingRestoreIntentSender = null, pendingRestoreUris = emptyList())
        }
        if (uris.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val count = photoRepository.restorePhotosByUris(uris)
                photoRepository.syncPhotosFromMediaStore()
                _uiState.update {
                    it.copy(
                        selectedUris = it.selectedUris - uris.toSet(),
                        toastMessage = "已成功恢复 $count 张照片至相册"
                    )
                }
            }
        }
    }

    fun onSystemRestoreCancelled() {
        _uiState.update {
            it.copy(pendingRestoreIntentSender = null, pendingRestoreUris = emptyList())
        }
    }

    // ==================== 💥 物理级彻底删除引擎（删除真实存储文件，全系统彻底抹除） ====================

    fun requestPermanentDeleteSelected(context: Context) {
        val uris = _uiState.value.selectedUris.toList()
        if (uris.isEmpty()) return
        _uiState.update { it.copy(showHardDeleteDialog = false, isProcessing = true) }
        executePermanentDelete(context, uris)
    }

    fun requestPermanentDeleteSingle(context: Context, photo: PhotoEntity) {
        _uiState.update { it.copy(previewPhoto = null, isProcessing = true) }
        executePermanentDelete(context, listOf(photo.uri))
    }

    fun requestEmptyTrash(context: Context) {
        val allUris = trashPhotos.value.map { it.uri }
        if (allUris.isEmpty()) return
        _uiState.update { it.copy(showClearAllDialog = false, isProcessing = true) }
        executePermanentDelete(context, allUris)
    }

    private fun executePermanentDelete(context: Context, uriStrings: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsedUris = uriStrings.map { Uri.parse(it) }
            val result = MediaStoreDeleteHelper.prepareDeleteRequest(context, parsedUris)

            when (result) {
                is DeleteRequestResult.DirectSuccess -> {
                    // 直接物理删除成功：立即从 Room 数据库彻底抹除
                    val count = photoRepository.deletePermanentlyByUris(uriStrings)
                    _uiState.update {
                        it.copy(
                            selectedUris = it.selectedUris - uriStrings.toSet(),
                            isProcessing = false,
                            toastMessage = "已彻底物理删除 $count 项内容"
                        )
                    }
                }
                is DeleteRequestResult.NeedsUserConsent -> {
                    // Android 10 / 11+：保存待删除列表并触发系统原生物理删除确认弹窗
                    _uiState.update {
                        it.copy(
                            pendingDeleteIntentSender = result.intentSender,
                            pendingDeleteUris = uriStrings,
                            isProcessing = false
                        )
                    }
                }
                is DeleteRequestResult.Error -> {
                    // 降级兜底：至少清理本地数据库
                    val count = photoRepository.deletePermanentlyByUris(uriStrings)
                    _uiState.update {
                        it.copy(
                            selectedUris = it.selectedUris - uriStrings.toSet(),
                            isProcessing = false,
                            toastMessage = "已从相册清理 $count 项内容"
                        )
                    }
                }
            }
        }
    }

    /**
     * 用户在系统原生物理删除弹窗中确认删除 (RESULT_OK)
     */
    fun onSystemDeleteConfirmed() {
        val uris = _uiState.value.pendingDeleteUris
        if (uris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val count = photoRepository.deletePermanentlyByUris(uris)
            _uiState.update {
                it.copy(
                    pendingDeleteIntentSender = null,
                    pendingDeleteUris = emptyList(),
                    selectedUris = it.selectedUris - uris.toSet(),
                    isProcessing = false,
                    toastMessage = "已彻底物理删除 $count 项内容"
                )
            }
        }
    }

    fun onSystemDeleteCancelled() {
        _uiState.update {
            it.copy(
                pendingDeleteIntentSender = null,
                pendingDeleteUris = emptyList(),
                isProcessing = false
            )
        }
    }

    fun clearToastMessage() = _uiState.update { it.copy(toastMessage = null) }

    companion object {
        fun factory(photoRepository: PhotoRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TrashViewModel(photoRepository) as T
        }
    }
}
