package com.example.phototag.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.phototag.util.HapticFeedbackUtil

/**
 * 🍏 iOS / Instagram 风格物理阻尼回弹修饰符 (Physics Spring Bounce)。
 *
 * 按下时平滑轻微缩放至 [scaleDown]（默认 0.96f），松开时通过高品质弹簧阻尼弹性回弹。
 */
fun Modifier.bounceScale(
    scaleDown: Float = 0.96f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * 为 Modifier 添加支持长按的点击扩展，封装实验性 API 注解。
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClick(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = this.combinedClickable(
    onClick = onClick,
    onLongClick = onLongClick
)

/**
 * 捏合缩放网格列数修饰符 (仅在检测到双指捏合时触发，单指滑动完全放行以保障 120Hz 满帧极速滑动)。
 *
 * 优化点：
 * 1. 使用 rememberUpdatedState 保证在列数变化时手势监听协程绝不中断或被迫重启；
 * 2. 消费 (consume) 双指捏合事件，彻底阻断网格在捏合缩放时产生任何纵向跳变或抖动；
 * 3. 动态触觉反馈 (HapticFeedbackUtil.click)。
 */
@Composable
fun Modifier.pinchToResizeGrid(
    currentColumns: Int,
    onColumnsChanged: (Int) -> Unit
): Modifier {
    val context = LocalContext.current
    val currentColsState = rememberUpdatedState(currentColumns)
    val onColsChangedState = rememberUpdatedState(onColumnsChanged)

    return this.pointerInput(Unit) {
        awaitEachGesture {
            var zoom = 1f

            do {
                val event = awaitPointerEvent()
                if (event.changes.size >= 2) {
                    val first = event.changes[0]
                    val second = event.changes[1]
                    val currentDistance = (first.position - second.position).getDistance()
                    val previousDistance = (first.previousPosition - second.previousPosition).getDistance()

                    // ⚡ 消费双指位移，彻底杜绝网格在双指捏合时发生上下串动
                    first.consume()
                    second.consume()

                    if (previousDistance > 0f) {
                        val zoomChange = currentDistance / previousDistance
                        zoom *= zoomChange
                        if (zoom > 1.25f) {
                            val cols = currentColsState.value
                            if (cols > 2) {
                                HapticFeedbackUtil.click(context)
                                onColsChangedState.value(cols - 1)
                            }
                            zoom = 1f
                        } else if (zoom < 0.80f) {
                            val cols = currentColsState.value
                            if (cols < 5) {
                                HapticFeedbackUtil.click(context)
                                onColsChangedState.value(cols + 1)
                            }
                            zoom = 1f
                        }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
