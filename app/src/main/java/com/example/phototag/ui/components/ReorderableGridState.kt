package com.example.phototag.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * 相册卡片拖拽自定义重排状态管理器（ReorderableAlbumState）。
 */
class ReorderableAlbumState(
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    private val onDragEnd: () -> Unit
) {
    var draggedIndex by mutableIntStateOf(-1)
        private set

    var dragOffsetX by mutableFloatStateOf(0f)
        private set
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    val isDragging: Boolean
        get() = draggedIndex >= 0

    // 卡片标准尺寸（用于 2 列网格换位阈值估算）
    private var itemWidthPx = 500f
    private var itemHeightPx = 600f

    fun onDragStart(index: Int) {
        draggedIndex = index
        dragOffsetX = 0f
        dragOffsetY = 0f
    }

    fun onDrag(dragAmount: Offset, totalItems: Int) {
        if (draggedIndex < 0) return
        dragOffsetX += dragAmount.x
        dragOffsetY += dragAmount.y

        // 水平换位检测（同行的左右互换）
        val colDelta = when {
            dragOffsetX > itemWidthPx * 0.45f -> 1
            dragOffsetX < -itemWidthPx * 0.45f -> -1
            else -> 0
        }

        // 垂直换位检测（跨行互换，一行 2 列即 delta = 2）
        val rowDelta = when {
            dragOffsetY > itemHeightPx * 0.45f -> 1
            dragOffsetY < -itemHeightPx * 0.45f -> -1
            else -> 0
        }

        if (totalItems <= 0) return
        val maxIdx = (totalItems - 1).coerceAtLeast(0)
        val targetIndex = (draggedIndex + colDelta + rowDelta * 2).coerceIn(0, maxIdx)
        if (targetIndex != draggedIndex && (colDelta != 0 || rowDelta != 0)) {
            val from = draggedIndex
            onMove(from, targetIndex)
            draggedIndex = targetIndex
            dragOffsetX = 0f
            dragOffsetY = 0f
        }
    }

    fun onDragStopped() {
        if (draggedIndex >= 0) {
            draggedIndex = -1
            dragOffsetX = 0f
            dragOffsetY = 0f
            onDragEnd()
        }
    }
}

@Composable
fun rememberReorderableAlbumState(
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit
): ReorderableAlbumState {
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    return remember {
        ReorderableAlbumState(
            onMove = { from, to -> currentOnMove(from, to) },
            onDragEnd = { currentOnDragEnd() }
        )
    }
}

/**
 * 拖拽重排手势与弹性动效修饰符。
 */
@Composable
fun Modifier.reorderableAlbumItem(
    state: ReorderableAlbumState,
    index: Int,
    totalItems: Int
): Modifier {
    val haptic = LocalHapticFeedback.current
    val isItemDragging = state.draggedIndex == index

    val scaleAnim by animateFloatAsState(
        targetValue = if (isItemDragging) 1.08f else 1f,
        animationSpec = spring(),
        label = "reorderScale"
    )

    return this
        .zIndex(if (isItemDragging) 3f else 1f)
        .graphicsLayer {
            if (isItemDragging) {
                translationX = state.dragOffsetX
                translationY = state.dragOffsetY
                scaleX = scaleAnim
                scaleY = scaleAnim
                shadowElevation = 16.dp.toPx()
            }
        }
        .pointerInput(state, index, totalItems) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.onDragStart(index)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount, totalItems)
                },
                onDragEnd = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    state.onDragStopped()
                },
                onDragCancel = {
                    state.onDragStopped()
                }
            )
        }
}
