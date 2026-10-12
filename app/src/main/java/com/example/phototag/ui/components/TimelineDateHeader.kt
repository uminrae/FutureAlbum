package com.example.phototag.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.util.HapticFeedbackUtil

/**
 * 时间轴全宽日期吸顶标题组件（Sticky Date Header，支持整天一键全选/反选）。
 * 纯正 iOS 18 风格排版，与相册网格无缝融为一体，杜绝浮动色块造成的断层空隙。
 */
@Composable
fun TimelineDateHeader(
    title: String,
    subtitle: String,
    count: Int,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    isAllSelected: Boolean = false,
    onToggleSelectGroup: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val isDark = isSystemInDarkTheme()
    val activeCheckColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val checkCheckTint = if (isDark) Color.Black else Color.White

    val checkBgColor by animateColorAsState(
        targetValue = if (isAllSelected) activeCheckColor else Color.Transparent,
        animationSpec = tween(150),
        label = "dateHeaderCheckBg"
    )

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = getIosPillBackground(),
                    border = BorderStroke(0.5.dp, getIosHairlineColor())
                ) {
                    Text(
                        text = "${count}张",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }

                if (onToggleSelectGroup != null && isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(checkBgColor)
                            .border(
                                width = if (isAllSelected) 0.dp else 1.5.dp,
                                color = if (isAllSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                            .clickable {
                                HapticFeedbackUtil.click(context)
                                onToggleSelectGroup()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAllSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "全选整天",
                                tint = checkCheckTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
