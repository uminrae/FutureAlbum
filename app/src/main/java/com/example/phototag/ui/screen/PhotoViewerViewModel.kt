package com.example.phototag.ui.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.util.CoilConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ⚡ 大图查看器状态与智能预加载管道 (PhotoViewerViewModel)。
 */
class PhotoViewerViewModel : ViewModel() {

    private val _currentViewingIndex = MutableStateFlow(0)
    val currentViewingIndex: StateFlow<Int> = _currentViewingIndex.asStateFlow()

    fun setCurrentIndex(index: Int) {
        _currentViewingIndex.value = index
    }

    /**
     * 智能后台预加载前后相邻照片到内存缓存池（0 延迟即划即显）。
     */
    fun preloadAdjacentPhotos(
        context: Context,
        photos: List<PhotoEntity>,
        currentPage: Int,
        screenWidthPx: Int,
        screenHeightPx: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val adjacentPhotos = listOfNotNull(
                photos.getOrNull(currentPage - 1),
                photos.getOrNull(currentPage + 1),
                photos.getOrNull(currentPage - 2),
                photos.getOrNull(currentPage + 2)
            )

            val imageLoader = context.imageLoader
            for (photo in adjacentPhotos) {
                val preloadRequest = CoilConfig.buildViewerRequest(
                    context = context,
                    uri = photo.uri,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    isZoomed = false
                )
                imageLoader.enqueue(preloadRequest)
            }
        }
    }
}
