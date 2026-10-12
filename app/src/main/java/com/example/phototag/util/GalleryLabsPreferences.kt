package com.example.phototag.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 🧪 Samsung 相册实验室 (Gallery Labs) 运行时极客配置单例。
 */
object GalleryLabsPreferences {

    private const val PREFS_NAME = "gallery_labs_prefs"
    private const val KEY_FPS_OVERLAY = "key_fps_overlay"
    private const val KEY_LOSSLESS_HISTOGRAM = "key_lossless_histogram"
    private const val KEY_ADAPTIVE_GRID = "key_adaptive_grid"

    private val _isFpsOverlayEnabled = MutableStateFlow(false)
    val isFpsOverlayEnabled: StateFlow<Boolean> = _isFpsOverlayEnabled.asStateFlow()

    private val _isLosslessHistogramEnabled = MutableStateFlow(false)
    val isLosslessHistogramEnabled: StateFlow<Boolean> = _isLosslessHistogramEnabled.asStateFlow()

    private val _isAdaptiveGridEnabled = MutableStateFlow(false)
    val isAdaptiveGridEnabled: StateFlow<Boolean> = _isAdaptiveGridEnabled.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isFpsOverlayEnabled.value = sp.getBoolean(KEY_FPS_OVERLAY, false)
        _isLosslessHistogramEnabled.value = sp.getBoolean(KEY_LOSSLESS_HISTOGRAM, false)
        _isAdaptiveGridEnabled.value = sp.getBoolean(KEY_ADAPTIVE_GRID, false)
    }

    fun setFpsOverlayEnabled(context: Context, enabled: Boolean) {
        _isFpsOverlayEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FPS_OVERLAY, enabled)
            .apply()
    }

    fun setLosslessHistogramEnabled(context: Context, enabled: Boolean) {
        _isLosslessHistogramEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOSSLESS_HISTOGRAM, enabled)
            .apply()
    }

    fun setAdaptiveGridEnabled(context: Context, enabled: Boolean) {
        _isAdaptiveGridEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ADAPTIVE_GRID, enabled)
            .apply()
    }
}
