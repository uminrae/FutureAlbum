package com.example.phototag.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalContext
import com.example.phototag.util.HapticFeedbackUtil
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.ui.theme.IosAmberDark
import com.example.phototag.ui.theme.IosBlue
import com.example.phototag.ui.theme.IosBlueDark
import com.example.phototag.ui.theme.IosBorderHairlineDark
import com.example.phototag.ui.theme.IosBorderHairlineLight
import com.example.phototag.ui.theme.IosGlassBackgroundDark
import com.example.phototag.ui.theme.IosGlassBackgroundLight
import com.example.phototag.ui.theme.IosPillBackgroundDark
import com.example.phototag.ui.theme.IosPillBackgroundLight
import com.example.phototag.ui.theme.IosRed
import com.example.phototag.ui.theme.IosRedDark

/**
 * 🍏 获取当前主题对应的 0.5.dp 发丝级高光边框颜色
 */
@Composable
fun getIosHairlineColor(): Color {
    return if (isSystemInDarkTheme()) IosBorderHairlineDark else IosBorderHairlineLight
}

/**
 * 🍏 获取当前主题对应的毛玻璃亚克力背景色
 */
@Composable
fun getIosGlassBackground(): Color {
    return if (isSystemInDarkTheme()) IosGlassBackgroundDark else IosGlassBackgroundLight
}

/**
 * 🍏 获取当前主题对应的胶囊药丸底色
 */
@Composable
fun getIosPillBackground(): Color {
    return if (isSystemInDarkTheme()) IosPillBackgroundDark else IosPillBackgroundLight
}

/**
 * iOS 按钮样式变体
 */
enum class IosButtonVariant {
    PRIMARY,       // 经典 iOS 蓝底白字
    AMBER,         // 经典暖金/琥珀色
    SECONDARY,     // 毛玻璃半透明灰色
    DESTRUCTIVE,   // iOS 警示红底
    GHOST          // 极简微边框线框
}

/**
 * 🍏 iOS 18 标准胶囊药丸按钮 (IosButton)
 *
 * 特性：
 * 1. 纯正全圆角 Capsule 或 16.dp 优雅 Squircle 形状；
 * 2. 0.5.dp 发丝级微光光感边框；
 * 3. 物理阻尼弹性按压 (bounceScale)；
 * 4. 内置加载 Loading 状态与图标排版。
 */
@Composable
fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IosButtonVariant = IosButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 40.dp,
    shape: RoundedCornerShape = RoundedCornerShape(percent = 50)
) {
    val isDark = isSystemInDarkTheme()
    val hairline = getIosHairlineColor()

    val containerColor = when (variant) {
        IosButtonVariant.PRIMARY -> if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)
        IosButtonVariant.AMBER -> if (isDark) IosAmberDark else Color(0xFFFF9500)
        IosButtonVariant.SECONDARY -> if (isDark) Color(0x33FFFFFF) else Color(0x12000000)
        IosButtonVariant.DESTRUCTIVE -> if (isDark) IosRedDark else IosRed
        IosButtonVariant.GHOST -> Color.Transparent
    }

    val contentColor = when (variant) {
        IosButtonVariant.PRIMARY -> if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
        IosButtonVariant.AMBER, IosButtonVariant.DESTRUCTIVE -> Color.White
        IosButtonVariant.SECONDARY -> if (isDark) Color.White else Color(0xFF1C1C1E)
        IosButtonVariant.GHOST -> if (isDark) Color.White else Color(0xFF1C1C1E)
    }

    val borderStroke = when (variant) {
        IosButtonVariant.PRIMARY -> BorderStroke(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x14000000))
        IosButtonVariant.AMBER -> BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
        IosButtonVariant.SECONDARY -> BorderStroke(0.5.dp, hairline)
        IosButtonVariant.DESTRUCTIVE -> BorderStroke(0.5.dp, Color.White.copy(alpha = 0.20f))
        IosButtonVariant.GHOST -> BorderStroke(0.5.dp, hairline)
    }

    Surface(
        shape = shape,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.4f),
        border = borderStroke,
        shadowElevation = if (enabled && (variant == IosButtonVariant.PRIMARY || variant == IosButtonVariant.AMBER)) 3.dp else 0.dp,
        modifier = modifier
            .height(height)
            .clip(shape)
            .bounceScale()
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
            }

            Text(
                text = text,
                color = contentColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * 🍏 iOS 18 磨砂玻璃圆形/微圆角小按钮 (IosIconButton)
 */
@Composable
fun IosIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 36.dp,
    containerColor: Color? = null,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val bg = containerColor ?: (if (isDark) Color(0x2EFFFFFF) else Color(0x12000000))
    val iconTint = tint ?: (if (isDark) Color.White else Color(0xFF1C1C1E))
    val hairline = getIosHairlineColor()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = 0.25f))
            .border(BorderStroke(0.5.dp, if (enabled) hairline else hairline.copy(alpha = 0.3f)), CircleShape)
            .bounceScale()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) iconTint else iconTint.copy(alpha = 0.35f),
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/**
 * 🍏 iOS 18 Inset Grouped 标准卡片容器 (IosCard)
 *
 * 带有 0.5.dp 发丝级高光边与 Apple 经典 20.dp 大圆角
 */
@Composable
fun IosCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderStroke: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val defaultBg = backgroundColor ?: (if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF))
    val defaultBorder = borderStroke ?: BorderStroke(0.5.dp, getIosHairlineColor())

    Surface(
        shape = shape,
        color = defaultBg,
        border = defaultBorder,
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.bounceScale().clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

/**
 * 🍏 0.5.dp 发丝级标准分割线 (IosHairlineDivider)
 */
@Composable
fun IosHairlineDivider(
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val divColor = color ?: (if (isDark) Color(0x1FFFFFFF) else Color(0x14000000))
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = divColor
    )
}

/**
 * 🍏 iOS 18 标志性悬浮毛玻璃胶囊岛底栏 (IosFloatingTabBar)
 *
 * 悬浮于相册内容底部，带有半透明毛玻璃材质底色与发丝级微光光感边框。
 */
@Composable
fun IosFloatingTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabItems: List<IosTabItem>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val glassBg = getIosGlassBackground()
    val hairline = getIosHairlineColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = glassBg,
            border = BorderStroke(0.5.dp, hairline),
            shadowElevation = 12.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    val activeBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)
                    val activeContent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
                    val inactiveContent = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)

                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) activeBg else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pill_bg"
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = pillBg,
                        border = if (isSelected) BorderStroke(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x14000000)) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .bounceScale()
                            .clickable { onTabSelected(index) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) activeContent else inactiveContent,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = item.title,
                                color = if (isSelected) activeContent else inactiveContent,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 导航项数据模型
 */
data class IosTabItem(
    val title: String,
    val icon: ImageVector
)

/**
 * 🍏 iOS 18 标准分组表格行项 (IosListRow)
 *
 * 经典 Apple 风格：
 * - 左侧圆角方块彩色图标容器 (RoundedCornerShape(8.dp))
 * - 中间主标题 + 可选副标题
 * - 右侧数量角标 / 辅助说明 + Chevron 右箭头
 * - 物理回弹触控 (bounceScale)
 */
@Composable
fun IosListRow(
    title: String,
    icon: ImageVector,
    iconContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color? = null,
    trailingChevron: ImageVector = Icons.AutoMirrored.Filled.ArrowForwardIos
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .bounceScale()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧圆角彩底图标
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconContainerColor,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // 中间标题与副标题
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
                )
            }
        }

        // 右侧徽标/数值
        if (!badgeText.isNullOrBlank()) {
            Text(
                text = badgeText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = badgeColor ?: (if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93))
            )
            Spacer(Modifier.width(6.dp))
        }

        // 右侧指示箭头
        Icon(
            imageVector = trailingChevron,
            contentDescription = null,
            tint = if (isDark) Color(0x66FFFFFF) else Color(0x33000000),
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * 🍏 iOS 18 Bento Grid 便当盒模块化卡片容器 (IosBentoCard)
 *
 * 特性：
 * 1. 22.dp 平滑连续圆角 (Squircle)；
 * 2. 0.5.dp 发丝级高光微边框；
 * 3. 支持自定义微渐变底色与柔和环境阴影；
 * 4. 触觉阻尼按压物理反馈 (bounceScale)。
 */
@Composable
fun IosBentoCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    backgroundColor: Color? = null,
    borderStroke: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val defaultBg = backgroundColor ?: (if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF))
    val defaultBorder = borderStroke ?: BorderStroke(0.5.dp, getIosHairlineColor())

    Surface(
        shape = shape,
        color = defaultBg,
        border = defaultBorder,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.bounceScale().clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

/**
 * 📷 相机机械铭牌徽章 (CameraBrandBadge)
 *
 * 智能识别 Leica、Sony、Apple、Fujifilm、Hasselblad、Canon、Nikon 等设备，
 * 渲染出类似专业相机机身铭牌的高级视觉质感。
 */
@Composable
fun CameraBrandBadge(
    deviceMake: String?,
    deviceModel: String?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val rawText = "${deviceMake.orEmpty()} ${deviceModel.orEmpty()}".lowercase()

    val (brandLabel, brandColor, subLabel) = when {
        rawText.contains("leica") -> Triple("LEICA", Color(0xFFE2001A), "WETZLAR GERMANY")
        rawText.contains("sony") || rawText.contains("ilce") -> Triple("SONY", Color(0xFFFF6600), "α SYSTEM")
        rawText.contains("apple") || rawText.contains("iphone") -> Triple("APPLE", if (isDark) Color.White else Color(0xFF1C1C1E), "CALIFORNIA")
        rawText.contains("fuji") -> Triple("FUJIFILM", Color(0xFF2E7D32), "FILM SIMULATION")
        rawText.contains("hasselblad") -> Triple("HASSELBLAD", Color(0xFFFF9500), "SWEDEN")
        rawText.contains("canon") -> Triple("CANON", Color(0xFFCC0000), "EOS SYSTEM")
        rawText.contains("nikon") -> Triple("NIKON", Color(0xFFFFD700), "NIKKOR")
        rawText.contains("xiaomi") -> Triple("XIAOMI", Color(0xFFFF6900), "OPTICS")
        rawText.contains("huawei") -> Triple("HUAWEI", Color(0xFFCF0A2C), "XMAGE")
        else -> Triple(deviceMake?.takeIf { it.isNotBlank() }?.uppercase() ?: "CAMERA", if (isDark) Color.White else Color(0xFF1C1C1E), "OPTICS")
    }

    val displayModel = deviceModel?.ifBlank { null } ?: deviceMake ?: "Precision Lens"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF141416) else Color(0xFFF2F2F7),
        border = BorderStroke(0.5.dp, getIosHairlineColor()),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 经典品牌色圆角徽标
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = brandColor,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        text = brandLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Column {
                    Text(
                        text = displayModel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    Text(
                        text = subLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
                    )
                }
            }
        }
    }
}

/**
 * 🔬 光学参数专业仪表盘胶囊 (OpticalPill)
 *
 * 针对光圈 (ƒ/x.x)、快门速度 (1/xxxxs)、等效焦距 (xx mm)、ISO 感光度，
 * 设计专用精密机械胶囊。
 */
@Composable
fun OpticalPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF242426) else Color(0xFFE5E5EA),
        border = BorderStroke(0.5.dp, getIosHairlineColor()),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF1C1C1E),
                letterSpacing = (-0.2).sp
            )
        }
    }
}

/**
 * iOS Action Sheet 操作项模型
 */
data class IosActionSheetItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val badge: String? = null,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * 🍏 iOS 18 风格磨砂毛玻璃浮动操作菜单 (IosActionSheet)
 *
 * 特性：
 * 1. 28.dp 超椭圆圆角与磨砂亚克力玻璃底色；
 * 2. 独立功能区组合卡片，0.5.dp 发丝级微光分割线；
 * 3. 弹簧微按压与触觉反馈 (HapticFeedback)；
 * 4. 底部独立经典圆角胶囊“取消”按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosActionSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    items: List<IosActionSheetItem>,
    cancelText: String = "取消"
) {
    if (!visible) return

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = getIosGlassBackground(),
        contentColor = if (isDark) Color.White else Color(0xFF1C1C1E),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = if (isDark) Color(0x44FFFFFF) else Color(0x33000000),
                shape = RoundedCornerShape(percent = 50)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.5.dp))
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 头部标题与描述 (若有)
            if (!title.isNullOrBlank() || !subtitle.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E)
                        )
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
                        )
                    }
                }
            }

            // 主菜单项卡片组
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
                border = BorderStroke(0.5.dp, getIosHairlineColor()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        val itemColor = when {
                            item.isDestructive -> if (isDark) IosRedDark else IosRed
                            else -> if (isDark) Color.White else Color(0xFF1C1C1E)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticFeedbackUtil.click(context)
                                    onDismissRequest()
                                    item.onClick()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                if (item.icon != null) {
                                    val iconTint = item.iconTint
                                        ?: (if (item.isDestructive) (if (isDark) IosRedDark else IosRed)
                                        else (if (isDark) IosBlueDark else IosBlue))

                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .padding(end = 2.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }

                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = itemColor
                                    )
                                    if (!item.subtitle.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
                                        )
                                    }
                                }
                            }

                            if (!item.badge.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isDark) Color(0x33FF9F0A) else Color(0x1AFF9500)
                                ) {
                                    Text(
                                        text = item.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) IosAmberDark else Color(0xFFFF9500),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        if (index < items.size - 1) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = getIosHairlineColor()
                            )
                        }
                    }
                }
            }

            // 底部独立“取消”按钮 (iOS 经典独立药丸卡片)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
                border = BorderStroke(0.5.dp, getIosHairlineColor()),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceScale()
                    .clickable {
                        HapticFeedbackUtil.click(context)
                        onDismissRequest()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cancelText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) IosBlueDark else IosBlue
                    )
                }
            }
        }
    }
}

