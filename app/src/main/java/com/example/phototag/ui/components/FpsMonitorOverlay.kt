package com.example.phototag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive

/**
 * ⚡ 120Hz 实时高刷物理帧率浮标 (FpsMonitorOverlay)。
 *
 * 核心特性：
 * 1. 采用 withFrameNanos 零卡顿渲染管线统计硬件真实刷新率。
 * 2. 500ms 滑动窗口平滑均值，防止高频数值跳动造成视力疲劳。
 * 3. 苹果/三星极客风格的透明黑胶囊造型，触控穿透，不阻挡任何底层操作。
 */
@Composable
fun FpsMonitorOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var fps by remember { mutableIntStateOf(120) }

    LaunchedEffect(Unit) {
        var frameCount = 0
        var lastTimeNanos = 0L

        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastTimeNanos == 0L) {
                    lastTimeNanos = frameTimeNanos
                }
                frameCount++
                val elapsedNanos = frameTimeNanos - lastTimeNanos
                // 满 500ms 刷新一次帧率指示
                if (elapsedNanos >= 500_000_000L) {
                    fps = ((frameCount * 1_000_000_000L) / elapsedNanos).toInt().coerceIn(1, 144)
                    frameCount = 0
                    lastTimeNanos = frameTimeNanos
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val fpsColor = when {
            fps >= 110 -> Color(0xFF30D158) // 120Hz 满帧翠绿
            fps >= 85 -> Color(0xFF64D2FF)  // 90Hz 科技青
            fps >= 55 -> Color(0xFFFFD60A)  // 60Hz 琥珀金
            else -> Color(0xFFFF453A)       // 掉帧警示红
        }

        Box(
            modifier = Modifier
                .background(Color(0xD91C1C1E), RoundedCornerShape(12.dp))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(fpsColor, CircleShape)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$fps FPS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = fpsColor
                )
            }
        }
    }
}
