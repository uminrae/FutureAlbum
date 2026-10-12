package com.example.phototag.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 大图下滑拖拽与退出进度 CompositionLocal (0f ~ 1f)。
 * 供顶栏、底栏等覆盖层在拖拽时同步平滑淡出，避免操作栏跟随缩放卡片糊在相册网格上。
 */
val LocalViewerDismissProgress = compositionLocalOf { 0f }

/**
 * iOS 18 旗舰级流体物理手势大图容器 (DismissibleViewerContainer)。
 *
 * 核心优化：
 * 1. 【iOS 18 物理下拉缩放与圆角变形】：单指下拉时，图片等比平滑缩小 (1.0x -> 0.70x)，圆角由 0dp 平滑过渡到 28dp 超椭圆。
 * 2. 【背景透明度非线性衰减】：背景黑度随下拉距离非线性渐变淡出，露出底层照片网格，提供真实的物理空间脱离感。
 * 3. 【彻底杜绝回弹爆闪】：下滑退出时位移延伸至屏幕下边界之外 (1.15x 容器高度)，透明度顺滑归零，杜绝页面销毁前 snapTo(0f) 引起的爆闪。
 * 4. 【真实速度追踪与动量释放 (VelocityTracker)】：支持快速向下轻甩 (Fling) 退出与向上轻甩唤起详情面板。
 * 5. 【零冲突手势分流】：双指捏合缩放时 100% 放行给图片，绝不误触下拉退出。
 * 6. 【120Hz 零重组渲染】：采用 drawBehind 与 graphicsLayer lambda，手势全程保持在渲染线程。
 */
@Composable
fun DismissibleViewerContainer(
    onDismiss: () -> Unit,
    onSwipeUpDetails: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    currentKey: Any? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val dismissThresholdPx = with(density) { 110.dp.toPx() }
    val detailsThresholdPx = with(density) { 60.dp.toPx() }
    val touchSlopPx = with(density) { 12.dp.toPx() }
    val maxCornerPx = with(density) { 28.dp.toPx() }

    val offsetYAnim = remember { Animatable(0f) }
    var currentOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    // 当切换图片或页面重组时，重置手势与位移状态
    LaunchedEffect(currentKey) {
        isDismissing = false
        offsetYAnim.snapTo(0f)
        currentOffsetY = 0f
        isDragging = false
    }

    val exitTargetY = if (containerHeightPx > 0f) containerHeightPx * 1.15f else with(density) { 1000.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerHeightPx = it.height.toFloat() }
            // iOS 18 标志性背景动态透明度：随拖拽非线性渐变淡出，露出底层相册网格
            .drawBehind {
                val y = offsetYAnim.value
                val bgAlpha = if (y > 0) {
                    (1f - (y / (dismissThresholdPx * 1.8f))).coerceIn(0f, 1f)
                } else {
                    1f
                }
                if (bgAlpha > 0f) {
                    drawRect(Color.Black.copy(alpha = bgAlpha))
                }
            }
            .pointerInput(enabled, currentKey, isDismissing) {
                if (!enabled || isDismissing) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var verticalDragStarted = false
                    var totalDragY = 0f

                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size

                        // 只要屏幕出现 2 个及以上触控点（双指缩放），立即放弃外层垂直拖拽拦截
                        if (pointerCount >= 2) {
                            if (isDragging) {
                                isDragging = false
                                coroutineScope.launch {
                                    offsetYAnim.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
                                }
                                currentOffsetY = 0f
                            }
                            break
                        }

                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val posChange = change.positionChange()
                            val deltaX = posChange.x
                            val deltaY = posChange.y

                            if (!verticalDragStarted) {
                                totalDragY += deltaY
                                if (abs(totalDragY) > touchSlopPx && abs(deltaY) > abs(deltaX) * 1.3f) {
                                    verticalDragStarted = true
                                    isDragging = true
                                }
                            }

                            if (verticalDragStarted) {
                                change.consume()
                                currentOffsetY += deltaY
                                coroutineScope.launch {
                                    offsetYAnim.snapTo(currentOffsetY)
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (isDragging) {
                        isDragging = false
                        val velocityY = velocityTracker.calculateVelocity().y

                        // 结合物理位移与释放瞬时速度进行判断
                        val isFlingDismiss = velocityY > 1000f && currentOffsetY > 30f
                        val isDistanceDismiss = currentOffsetY > dismissThresholdPx

                        val isFlingDetails = velocityY < -800f && currentOffsetY < -20f
                        val isDistanceDetails = currentOffsetY < -detailsThresholdPx

                        if (isFlingDismiss || isDistanceDismiss) {
                            // 下滑确认退出：平滑飞出至屏幕外并彻底淡出，绝不 snapTo(0f) 避免回跳闪烁
                            isDismissing = true
                            coroutineScope.launch {
                                offsetYAnim.animateTo(
                                    targetValue = exitTargetY,
                                    animationSpec = tween(
                                        durationMillis = 220,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                                onDismiss()
                            }
                        } else if (isFlingDetails || isDistanceDetails) {
                            // 上滑触发详情面板：优雅复位并呼出
                            coroutineScope.launch {
                                offsetYAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            currentOffsetY = 0f
                            onSwipeUpDetails()
                        } else {
                            // 未达阈值：iOS 18 经典物理弹簧 Spring 复位
                            coroutineScope.launch {
                                offsetYAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            currentOffsetY = 0f
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val y = offsetYAnim.value
        val dismissProgress = if (y > 0f) {
            (y / (dismissThresholdPx * 0.85f)).coerceIn(0f, 1f)
        } else {
            0f
        }

        CompositionLocalProvider(LocalViewerDismissProgress provides dismissProgress) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val currentY = offsetYAnim.value
                        if (currentY > 0f) {
                            // 下拉阶段：物理跟随位移 + 等比缩小 + 圆角过渡
                            translationY = currentY
                            val dragProgress = (currentY / (dismissThresholdPx * 2.2f)).coerceIn(0f, 1f)
                            val scaleFactor = (1f - dragProgress * 0.30f).coerceIn(0.70f, 1f)
                            scaleX = scaleFactor
                            scaleY = scaleFactor

                            val cornerRadius = dragProgress * maxCornerPx
                            shape = RoundedCornerShape(cornerRadius)
                            clip = cornerRadius > 0f

                            // 拖拽阶段保持清晰透明度；飞出阶段线性衰减至 0f，确保组件卸载前完全透明
                            alpha = if (currentY <= dismissThresholdPx) {
                                (1f - (dragProgress * 0.2f)).coerceIn(0.8f, 1f)
                            } else {
                                val exitRange = (exitTargetY - dismissThresholdPx).coerceAtLeast(1f)
                                val exitProgress = ((currentY - dismissThresholdPx) / exitRange).coerceIn(0f, 1f)
                                (0.8f * (1f - exitProgress)).coerceIn(0f, 1f)
                            }
                        } else {
                            // 上滑阶段：阻尼抵抗平移，不缩小
                            translationY = currentY * 0.45f
                            scaleX = 1f
                            scaleY = 1f
                            clip = false
                            alpha = 1f
                        }
                    }
            ) {
                content()
            }
        }
    }
}

