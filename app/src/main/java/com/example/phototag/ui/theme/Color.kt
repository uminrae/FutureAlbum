package com.example.phototag.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 亮色主题色板 (iOS / Instagram 极简冷灰白) ====================
val Primary = Color(0xFF111111)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFF1F3F5)
val OnPrimaryContainer = Color(0xFF111111)

val Secondary = Color(0xFF6E6E73)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFF2F2F7)
val OnSecondaryContainer = Color(0xFF1C1C1E)

val Tertiary = Color(0xFF007AFF) // iOS Signature Accent Blue
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFE5F1FF)
val OnTertiaryContainer = Color(0xFF004080)

val ErrorLight = Color(0xFFFF3B30)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFEBEA)
val OnErrorContainerLight = Color(0xFF8B0000)

val BackgroundLight = Color(0xFFF8F9FA)
val OnBackgroundLight = Color(0xFF111111)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF111111)
val SurfaceVariantLight = Color(0xFFF2F2F7)
val OnSurfaceVariantLight = Color(0xFF6E6E73)
val OutlineLight = Color(0xFFE5E5EA)
val OutlineVariantLight = Color(0xFFF2F2F7)

// ==================== 暗色主题色板 (OLED 极致纯黑) ====================
val PrimaryDark = Color(0xFFFFFFFF)
val OnPrimaryDark = Color(0xFF000000)
val PrimaryContainerDark = Color(0xFF2C2C2E)
val OnPrimaryContainerDark = Color(0xFFFFFFFF)

val SecondaryDark = Color(0xFF8E8E93)
val OnSecondaryDark = Color(0xFF1C1C1E)
val SecondaryContainerDark = Color(0xFF1C1C1E)
val OnSecondaryContainerDark = Color(0xFFE5E5EA)

val TertiaryDark = Color(0xFF0A84FF)
val OnTertiaryDark = Color(0xFF000000)
val TertiaryContainerDark = Color(0xFF003366)
val OnTertiaryContainerDark = Color(0xFFB3D7FF)

val ErrorDark = Color(0xFFFF453A)
val OnErrorDark = Color(0xFF4A0000)
val ErrorContainerDark = Color(0xFF5A1010)
val OnErrorContainerDark = Color(0xFFFFB4AB)

val BackgroundDark = Color(0xFF000000)
val OnBackgroundDark = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF121212)
val OnSurfaceDark = Color(0xFFFFFFFF)
val SurfaceVariantDark = Color(0xFF1C1C1E)
val OnSurfaceVariantDark = Color(0xFF8E8E93)
val OutlineDark = Color(0xFF2C2C2E)
val OutlineVariantDark = Color(0xFF1C1C1E)

// ==================== 🍏 iOS 18 系统设计规范令牌 (Apple Design Tokens) ====================
val IosBlue = Color(0xFF007AFF)
val IosBlueDark = Color(0xFF0A84FF)
val IosAmber = Color(0xFFFF9500)
val IosAmberDark = Color(0xFFFF9F0A)
val IosRed = Color(0xFFFF3B30)
val IosRedDark = Color(0xFFFF453A)
val IosGreen = Color(0xFF34C759)
val IosGreenDark = Color(0xFF30D158)
val IosPurple = Color(0xFFAF52DE)
val IosPurpleDark = Color(0xFFBF5AF2)

// iOS 灰阶阶梯 (Grayscale Ramp)
val IosSystemGray1 = Color(0xFF8E8E93)
val IosSystemGray2 = Color(0xFFAEAEB2)
val IosSystemGray3 = Color(0xFFC7C7CC)
val IosSystemGray4 = Color(0xFFD1D1D6)
val IosSystemGray5 = Color(0xFFE5E5EA)
val IosSystemGray6 = Color(0xFFF2F2F7)

// 0.5.dp 发丝级微光光感边框 (Hairline Edges)
val IosHairlineDark = Color(0x24FFFFFF)      // 暗色高光发丝微边
val IosHairlineLight = Color(0x14000000)     // 亮色低反差发丝微边
val IosBorderHairlineDark = IosHairlineDark
val IosBorderHairlineLight = IosHairlineLight
val IosHairlineSubtleDark = Color(0x14FFFFFF)
val IosHairlineSubtleLight = Color(0x0A000000)

// 毛玻璃亚克力材质底色 (Frosted Glass & Materials)
val IosGlassBackgroundDark = Color(0xDD1C1C1E)
val IosGlassBackgroundLight = Color(0xEEFFFFFF)
val IosPillBackgroundDark = Color(0x28FFFFFF)
val IosPillBackgroundLight = Color(0x0F000000)
val IosCardSurfaceDark = Color(0xFF18181A)
val IosCardElevatedDark = Color(0xFF242426)

// ==================== 莫兰迪高级色彩规范体系 ====================

data class MorandiColorItem(val hex: String, val name: String)

val MorandiPalette = listOf(
    MorandiColorItem("#7E9A86", "鼠尾草绿"),
    MorandiColorItem("#7A90A4", "冷雾蓝"),
    MorandiColorItem("#D99B6A", "暖杏黄"),
    MorandiColorItem("#B57F89", "烟粉"),
    MorandiColorItem("#8F87A1", "薰衣草灰"),
    MorandiColorItem("#BD7C5F", "焦糖陶土"),
    MorandiColorItem("#6B9E9B", "极地青冰"),
    MorandiColorItem("#5C6B73", "深墨石板"),
    MorandiColorItem("#D4A373", "暮色暖赭"),
    MorandiColorItem("#606C38", "橄榄墨绿")
)

/**
 * 将 Hex 字符串安全转换为 Compose Color。
 */
fun parseHexColor(hex: String, default: Color = Color(0xFF7E9A86)): Color {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleanHex))
    } catch (_: Exception) {
        default
    }
}
