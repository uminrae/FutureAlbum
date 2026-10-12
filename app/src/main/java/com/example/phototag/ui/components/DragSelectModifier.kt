package com.example.phototag.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.phototag.util.HapticFeedbackUtil
import com.example.phototag.util.TimelineGridItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ⚡ 工业级相册滑动整排连选修饰符 (DragSelectModifier)。
 *
 * 核心设计：
 * 1. 【普通滑动放行】：无论在普通模式还是多选模式下，单指快速上下滑动相册 100% 放行给 LazyVerticalGrid，绝不卡死上下浏览相册！
 * 2. 【长按 200ms 触发整排连选】：手指按住照片稍停 200ms 触发触觉反馈并无缝接力拖拽连选！
 * 3. 【往下拉一排一排整排全选 (Range Selection)】：从起点照片索引到当前触点索引之间，所有照片整排整排自动连续勾选！
 * 4. 【边缘平滑自动滚屏 (Auto-Scroll)】：拖至屏幕顶底边缘时自动滚动，且随滚屏持续向下/向上整排无限扩选！
 * 5. 【双向增减反选】：从未选中照片起笔为整排批量勾选，从已选中照片起笔为整排批量取消勾选。
 * 6. 【多指捏合缩放列数防御】：双指操作时完全放行，绝不抢占手势。
 */
@Composable
fun Modifier.dragSelectModifier(
    gridState: LazyGridState,
    items: List<TimelineGridItem>,
    isMultiSelectMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onEnterMultiSelect: ((Long) -> Unit)? = null,
    onDragSelectChange: (selectedIds: Set<Long>) -> Unit
): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current

    val edgeScrollThresholdPx = with(density) { 72.dp.toPx() }
    val touchSlopPx = with(density) { 10.dp.toPx() }

    val currentSelectedPhotoIds by rememberUpdatedState(selectedPhotoIds)
    val currentItems by rememberUpdatedState(items)
    val currentIsMultiSelectMode by rememberUpdatedState(isMultiSelectMode)
    val currentOnDragSelectChange by rememberUpdatedState(onDragSelectChange)
    val currentOnEnterMultiSelect by rememberUpdatedState(onEnterMultiSelect)

    var isDragging by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
    var isSelectingAction by remember { mutableStateOf(true) }
    var startItemIndex by remember { mutableStateOf<Int?>(null) }
    var currentItemIndex by remember { mutableStateOf<Int?>(null) }
    var initialSnapshotIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var lastSamplePosition by remember { mutableStateOf<Offset?>(null) }

    // 智能定位触点所在或最接近的网格项索引 (支持边界投影与整排行对齐)
    fun findGridIndexAt(pos: Offset): Int? {
        val visibleItems = gridState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return null

        // 1. 精确包围盒碰撞
        val directHit = visibleItems.firstOrNull { item ->
            pos.x >= item.offset.x && pos.x <= (item.offset.x + item.size.width) &&
                    pos.y >= item.offset.y && pos.y <= (item.offset.y + item.size.height)
        }
        if (directHit != null) return directHit.index

        // 2. Y 轴行对齐：若落在图片间隙，匹配手指所在水平行中最近的图片
        val rowItems = visibleItems.filter { item ->
            pos.y >= item.offset.y && pos.y <= (item.offset.y + item.size.height)
        }
        if (rowItems.isNotEmpty()) {
            return rowItems.minByOrNull { item ->
                val centerX = item.offset.x + item.size.width / 2f
                kotlin.math.abs(pos.x - centerX)
            }?.index
        }

        // 3. 超出顶部可视区 -> 第一可见项
        if (pos.y < 0) return visibleItems.first().index

        // 4. 超出底部可视区 -> 最后一项
        val viewportHeight = gridState.layoutInfo.viewportSize.height
        if (pos.y > viewportHeight) return visibleItems.last().index

        // 5. 其它情况找屏幕中物理距离最近项
        return visibleItems.minByOrNull { item ->
            val cx = item.offset.x + item.size.width / 2f
            val cy = item.offset.y + item.size.height / 2f
            val dx = pos.x - cx
            val dy = pos.y - cy
            dx * dx + dy * dy
        }?.index
    }

    // 获取从 startIndex 到 endIndex 区间内的所有照片 ID (一排一排整排全选)
    fun getPhotoIdsInRange(startIndex: Int, endIndex: Int): Set<Long> {
        val s = minOf(startIndex, endIndex).coerceAtLeast(0)
        val e = maxOf(startIndex, endIndex).coerceAtMost(currentItems.lastIndex)
        val result = mutableSetOf<Long>()
        for (i in s..e) {
            val item = currentItems.getOrNull(i)
            if (item is TimelineGridItem.Photo) {
                result.add(item.photo.photoId)
            }
        }
        return result
    }

    // 提交区间选中
    fun updateSelectionRange(startIdx: Int, currIdx: Int) {
        val rangeIds = getPhotoIdsInRange(startIdx, currIdx)
        val updated = if (isSelectingAction) {
            initialSnapshotIds + rangeIds
        } else {
            initialSnapshotIds - rangeIds
        }
        currentOnDragSelectChange(updated)
    }

    // 拖动位移更新
    fun processMove(pos: Offset) {
        val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()

        // 边缘自动滚屏动态速度
        autoScrollSpeed = when {
            pos.y < edgeScrollThresholdPx -> {
                -((edgeScrollThresholdPx - pos.y) / edgeScrollThresholdPx * 32f).coerceIn(-36f, -4f)
            }
            pos.y > viewportHeight - edgeScrollThresholdPx -> {
                ((pos.y - (viewportHeight - edgeScrollThresholdPx)) / edgeScrollThresholdPx * 32f).coerceIn(4f, 32f)
            }
            else -> 0f
        }

        val idx = findGridIndexAt(pos)
        if (idx != null && idx != currentItemIndex) {
            currentItemIndex = idx
            startItemIndex?.let { s ->
                HapticFeedbackUtil.tick(context)
                updateSelectionRange(s, idx)
            }
        }
    }

    val resetDragState = {
        isDragging = false
        autoScrollSpeed = 0f
        lastSamplePosition = null
        startItemIndex = null
        currentItemIndex = null
        initialSnapshotIds = emptySet()
    }

    // 边缘自动滚屏协程驱动 (滚屏时持续扩选新露出的整排照片)
    LaunchedEffect(isDragging, autoScrollSpeed) {
        if (isDragging && autoScrollSpeed != 0f) {
            while (isActive && isDragging && autoScrollSpeed != 0f) {
                gridState.scrollBy(autoScrollSpeed)
                lastSamplePosition?.let { pos ->
                    processMove(pos)
                }
                delay(16)
            }
        }
    }

    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val downPos = down.position
            val pointerId = down.id
            val startTime = System.currentTimeMillis()
            val longPressTimeoutMs = 200L // 200ms 短暂轻按阈值，极大降低连选等待感，同时完全不卡上下快速划动相册

            var longPressTriggered = false

            while (true) {
                val event = withTimeoutOrNull(30L) {
                    awaitPointerEvent(pass = PointerEventPass.Initial)
                }

                if (event != null) {
                    // 🛡️ 多指判定防御：双指捏合缩放网格列数时彻底放行
                    if (event.changes.count { it.pressed } > 1) {
                        if (longPressTriggered) resetDragState()
                        break
                    }

                    val pointerChange = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!pointerChange.pressed) break

                    val currentPos = pointerChange.position
                    val distance = (currentPos - downPos).getDistance()

                    // 若未达到 200ms 就快速移动，说明用户正在普通上下划动浏览相册，直接退出放行！
                    if (!longPressTriggered && distance >= touchSlopPx) {
                        break
                    }

                    if (longPressTriggered) {
                        // 连选已激活，消费手势并执行一排一排区间圈选！
                        pointerChange.consume()
                        lastSamplePosition = currentPos
                        processMove(currentPos)
                    }
                }

                // 达到 200ms 停顿判定，激活一排一排滑动连选！
                if (!longPressTriggered && System.currentTimeMillis() - startTime >= longPressTimeoutMs) {
                    val sIdx = findGridIndexAt(downPos)
                    if (sIdx != null) {
                        val item = currentItems.getOrNull(sIdx)
                        if (item is TimelineGridItem.Photo) {
                            longPressTriggered = true
                            isDragging = true
                            startItemIndex = sIdx
                            currentItemIndex = sIdx
                            HapticFeedbackUtil.longPress(context)

                            val firstPhotoId = item.photo.photoId
                            if (!currentIsMultiSelectMode) {
                                currentOnEnterMultiSelect?.invoke(firstPhotoId)
                                initialSnapshotIds = emptySet()
                                isSelectingAction = true
                                currentOnDragSelectChange(setOf(firstPhotoId))
                            } else {
                                initialSnapshotIds = currentSelectedPhotoIds
                                isSelectingAction = firstPhotoId !in currentSelectedPhotoIds
                                val initialUpdated = if (isSelectingAction) {
                                    initialSnapshotIds + firstPhotoId
                                } else {
                                    initialSnapshotIds - firstPhotoId
                                }
                                currentOnDragSelectChange(initialUpdated)
                            }
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
            }

            if (longPressTriggered) {
                resetDragState()
            }
        }
    }
}
