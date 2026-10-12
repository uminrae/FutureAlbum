package com.example.phototag.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.util.HapticFeedbackUtil
import com.example.phototag.util.editor.FilmEffectOptions
import com.example.phototag.util.editor.FilmPreset

private data class PresetOption(
    val preset: FilmPreset,
    val title: String,
    val subtitle: String,
    val colorTag: String
)

private val ALL_FILM_PRESETS = listOf(
    PresetOption(
        preset = FilmPreset.CPM35,
        title = "Dazz CPM35",
        subtitle = "经典 35mm 胶片 · 哑光黑位、柔焦弥散、暖奶油高光与通透肤色",
        colorTag = "#D99B6A"
    ),
    PresetOption(
        preset = FilmPreset.D_FUNS,
        title = "D FunS (富士一次性)",
        subtitle = "Dazz 霸榜推荐！直闪高反差、暗部复古橄榄绿与通透冷白皮",
        colorTag = "#2A9D8F"
    ),
    PresetOption(
        preset = FilmPreset.INST_C,
        title = "Inst C (拍立得显影)",
        subtitle = "经典富士拍立得 · 乳化显影相纸感、柔和发灰暗部与微粉奶油高光",
        colorTag = "#E76F51"
    ),
    PresetOption(
        preset = FilmPreset.FX_67,
        title = "FX 67 (宾得中画幅)",
        subtitle = "画质天花板神机 · 极致大动态范围、深沉浓郁油画感与电影级层次",
        colorTag = "#264653"
    ),
    PresetOption(
        preset = FilmPreset.CCD_2000,
        title = "CCD 2000 (千禧数码)",
        subtitle = "复古数码卡片机 · 清透冷蓝、纯净微反差与日光通透空气感",
        colorTag = "#7A90A4"
    ),
    PresetOption(
        preset = FilmPreset.CT24,
        title = "CT24 (柯达反转片)",
        subtitle = "Kodachrome 64 名作 · 浓艳红黄阶、历史厚重感与高饱和纯色",
        colorTag = "#E63946"
    ),
    PresetOption(
        preset = FilmPreset.CLASSIC_U,
        title = "Classic U (奥林巴斯傻瓜)",
        subtitle = "日常随手拍机皇 · 经典 μ-II 街头纪实、微暖灰胶调与银盐颗粒",
        colorTag = "#A8DADC"
    ),
    PresetOption(
        preset = FilmPreset.CINE_800T,
        title = "Cine 800T (诺兰电影)",
        subtitle = "夜景电影青橙 · 冷青暗部、暖橙高光与钨丝灯辉光氛围",
        colorTag = "#E07A5F"
    ),
    PresetOption(
        preset = FilmPreset.GR_D,
        title = "GR D (理光街头)",
        subtitle = "森山大道质感 · 高反差深沉黑场、鲜明对比与广角光学暗角",
        colorTag = "#5C6B73"
    ),
    PresetOption(
        preset = FilmPreset.KODAK_GOLD,
        title = "Kodak Gold 200",
        subtitle = "柯达金暖阳 · 温暖阳光色调、金黄泛光与暖棕红暗部",
        colorTag = "#F4A261"
    ),
    PresetOption(
        preset = FilmPreset.PORTRA_400,
        title = "Portra 400",
        subtitle = "自然人像胶片 · 极高宽容度、柔和温润肤色与平滑高光滚降",
        colorTag = "#E9C46A"
    ),
    PresetOption(
        preset = FilmPreset.FUJI_PRO,
        title = "FUJI PRO",
        subtitle = "清透日系人像 · 低反差淡雅、柔和橄榄绿暗调与粉润肤感",
        colorTag = "#7E9A86"
    ),
    PresetOption(
        preset = FilmPreset.BW_CONTRAST,
        title = "森山黑白",
        subtitle = "街头高反差黑白 · 极致纯黑透白、粗粝颗粒与抓拍张力",
        colorTag = "#222222"
    ),
    PresetOption(
        preset = FilmPreset.NEOPAN_135,
        title = "Neopan 135 (富士黑白)",
        subtitle = "细腻银盐微粒 · 经典纪实黑白、丰富温润灰阶过渡",
        colorTag = "#555555"
    )
)

/**
 * 🍏 iOS 18 风格胶片滤镜与复古相机冲印弹窗 (BatchFilterBottomSheet)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchFilterBottomSheet(
    selectedCount: Int,
    onSelectPreset: (FilmPreset, FilmEffectOptions) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var withTimestamp by remember { mutableStateOf(false) }
    var withLightLeak by remember { mutableStateOf(false) }
    var withStarFilter by remember { mutableStateOf(false) }
    var withPolaroidFrame by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = getIosGlassBackground(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            // 顶部把手与标题
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "复古胶片冲印工坊",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedCount > 1) "已选 $selectedCount 张照片 · 批量生成复古副本" else "为当前照片套用经典相机预设与光学配件",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ==================== 4 大经典胶片配件开关 ====================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. CPM35 复古石英时间戳开关
                    FilterOptionSwitchItem(
                        icon = Icons.Filled.Schedule,
                        iconTint = Color(0xFFFFB300),
                        title = "印上 CPM35 复古石英时间戳",
                        subtitle = "纯正 7x9 金点阵 LED 发光字模 (''26年 10月 4日)",
                        checked = withTimestamp,
                        onCheckedChange = {
                            HapticFeedbackUtil.click(context)
                            withTimestamp = it
                        }
                    )

                    // 2. 真实胶片漏光烧边
                    FilterOptionSwitchItem(
                        icon = Icons.Filled.Whatshot,
                        iconTint = Color(0xFFFF5722),
                        title = "叠加 Dazz 胶片漏光光晕 (Light Leak)",
                        subtitle = "真实红橙与品红胶卷侧边烧边漫射",
                        checked = withLightLeak,
                        onCheckedChange = {
                            HapticFeedbackUtil.click(context)
                            withLightLeak = it
                        }
                    )

                    // 3. 星光镜十字星芒
                    FilterOptionSwitchItem(
                        icon = Icons.Filled.AutoAwesome,
                        iconTint = Color(0xFF00B4D8),
                        title = "加装星光镜十字星芒 (Star Filter)",
                        subtitle = "画面高光点与车灯绽放 4 芒 / 8 芒浪漫星光",
                        checked = withStarFilter,
                        onCheckedChange = {
                            HapticFeedbackUtil.click(context)
                            withStarFilter = it
                        }
                    )

                    // 4. 拍立得相纸相框
                    FilterOptionSwitchItem(
                        icon = Icons.Filled.CropSquare,
                        iconTint = Color(0xFF9C27B0),
                        title = "套用 Instax 经典拍立得相框",
                        subtitle = "复古暖白相纸下宽留白与手写体日期标记",
                        checked = withPolaroidFrame,
                        onCheckedChange = {
                            HapticFeedbackUtil.click(context)
                            withPolaroidFrame = it
                        }
                    )
                }
            }

            HorizontalDivider(
                color = getIosHairlineColor(),
                thickness = 0.5.dp
            )

            // ==================== 14 大经典相机预设列表 ====================
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ALL_FILM_PRESETS, key = { it.preset.name }) { item ->
                    val tagColor = try {
                        Color(android.graphics.Color.parseColor(item.colorTag))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = getIosPillBackground(),
                        border = BorderStroke(0.5.dp, getIosHairlineColor()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .bounceScale()
                            .clickable {
                                HapticFeedbackUtil.click(context)
                                val options = FilmEffectOptions(
                                    withTimestamp = withTimestamp,
                                    withLightLeak = withLightLeak,
                                    withStarFilter = withStarFilter,
                                    withPolaroidFrame = withPolaroidFrame
                                )
                                onSelectPreset(item.preset, options)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 颜色识别圆点
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tagColor)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOptionSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = getIosPillBackground(),
        border = BorderStroke(0.5.dp, getIosHairlineColor()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = iconTint
                )
            )
        }
    }
}
