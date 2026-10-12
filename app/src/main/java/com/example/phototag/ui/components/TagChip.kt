package com.example.phototag.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.ui.theme.parseHexColor

/**
 * 获取标签专属前缀图标（EXIF 智能标签与普通标签视觉区分）。
 */
fun getTagIcon(tagName: String): ImageVector {
    return when {
        tagName.matches(Regex("#\\d{4}年")) || tagName.matches(Regex("#\\d{1,2}月")) -> Icons.Filled.CalendarMonth
        tagName.contains("壁纸") -> Icons.Filled.Wallpaper
        tagName.contains("截图") -> Icons.Filled.Screenshot
        tagName.contains("GIF") || tagName.contains("动图") -> Icons.Filled.Gif
        tagName.contains("视频") -> Icons.Filled.Videocam
        else -> Icons.Filled.Tag
    }
}

/**
 * 高质感莫兰迪风格标签胶囊（TagChip - 纯正苹果极简无表情设计）。
 *
 * @param name 标签名称
 * @param colorHex 莫兰迪 HEX 颜色
 * @param isSelected 是否处于选中状态
 * @param isSystem 是否为系统自动生成的 EXIF 标签
 * @param showRemoveButton 是否在右侧显示移除 "×" 图标
 * @param onClick 单击回调
 * @param onLongClick 长按回调
 * @param onRemoveClick 点击 "×" 移除回调
 */
@Composable
fun TagChip(
    name: String,
    colorHex: String = "#7E9A86",
    isSelected: Boolean = false,
    isSystem: Boolean = false,
    showRemoveButton: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val cleanName = remember(name) { name.trimStart('#').trim() }
    val baseColor = remember(colorHex) { parseHexColor(colorHex) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val activeAccent = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)

    val containerColor = if (isSelected) {
        activeAccent
    } else {
        getIosPillBackground()
    }

    val contentColor = if (isSelected) {
        if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = if (isSelected) {
        BorderStroke(0.5.dp, if (isDark) Color(0x40FFFFFF) else Color(0x20000000))
    } else {
        BorderStroke(0.5.dp, getIosHairlineColor())
    }

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        border = borderStroke,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .bounceScale()
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp,
                end = if (showRemoveButton) 8.dp else 12.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 只有非「全部」的分类标签才显示小圆点指示器
            if (cleanName != "全部") {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) contentColor else baseColor,
                    modifier = Modifier.size(6.dp)
                ) {}
                Spacer(Modifier.width(6.dp))
            }

            Text(
                text = cleanName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                fontSize = 13.sp,
                letterSpacing = 0.1.sp
            )

            if (showRemoveButton) {
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.20f),
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRemoveClick()
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "解除标签",
                        tint = contentColor,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}
