package com.example.phototag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.SmartAlbumEntity
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.SmartAlbumRule

private val COLOR_OPTIONS = listOf("#7E9A86", "#D99B6A", "#B57F89", "#7A90A4", "#BD7C5F", "#5C6B73", "#D4A373", "#8F87A1", "#3B82F6", "#10B981")

/**
 * 智能动态相册新建与规则编辑 BottomSheet。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateSmartAlbumBottomSheet(
    allTags: List<TagEntity>,
    editingAlbum: SmartAlbumEntity? = null,
    onSave: (name: String, iconName: String, colorHex: String, rule: SmartAlbumRule) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialRule = editingAlbum?.rule ?: SmartAlbumRule()

    var name by remember { mutableStateOf(editingAlbum?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(editingAlbum?.iconName ?: "") }
    var selectedColor by remember { mutableStateOf(editingAlbum?.colorHex ?: "#7E9A86") }

    // 规则状态
    var includeTagIds by remember { mutableStateOf(initialRule.includeTagIds.toSet()) }
    var isAndLogic by remember { mutableStateOf(initialRule.isAndLogic) }
    var excludeTagIds by remember { mutableStateOf(initialRule.excludeTagIds.toSet()) }
    var yearFilter by remember { mutableStateOf(initialRule.yearFilter) }
    var mediaTypeFilter by remember { mutableStateOf(initialRule.mediaTypeFilter) }
    var mustHaveLocation by remember { mutableStateOf(initialRule.mustHaveLocation) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // ==================== 顶部把手与标题 ====================
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (editingAlbum == null) "新建智能动态相册" else "编辑智能相册规则",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 相册名称
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("相册名称") },
                        placeholder = { Text("如：2026 精选") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. 主题色
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("相册主题色", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(COLOR_OPTIONS) { hex ->
                                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                        .clickable { selectedColor = hex }
                                        .then(
                                            if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 【包含标签 (Include Tags)】
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Label, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("必须包含的标签", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            // 且/或 切换开关
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isAndLogic) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.clickable { isAndLogic = true }
                                ) {
                                    Text(
                                        "且 (全部)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isAndLogic) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (!isAndLogic) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.clickable { isAndLogic = false }
                                ) {
                                    Text(
                                        "或 (其一)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (!isAndLogic) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allTags.forEach { tag ->
                                val isSelected = tag.tagId in includeTagIds
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        includeTagIds = if (isSelected) includeTagIds - tag.tagId else includeTagIds + tag.tagId
                                    },
                                    label = { Text(tag.name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }
                }

                // 4. 【排除标签 (Exclude Tags)】
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("必须排除的标签 (可选)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allTags.forEach { tag ->
                                val isExcluded = tag.tagId in excludeTagIds
                                FilterChip(
                                    selected = isExcluded,
                                    onClick = {
                                        excludeTagIds = if (isExcluded) excludeTagIds - tag.tagId else excludeTagIds + tag.tagId
                                    },
                                    label = { Text("排除 ${tag.name}", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }
                }

                // 5. 【高级附加过滤 (Filters)】
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FilterAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("附加属性过滤", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        // 年份过滤
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("拍摄年份", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(null to "全部", 2026 to "2026年", 2025 to "2025年", 2024 to "2024年").forEach { (yr, label) ->
                                    val isSelected = yearFilter == yr
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable { yearFilter = yr }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 媒体类型
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("媒体格式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(null to "全部", "IMAGE" to "仅照片", "GIF" to "仅动图 GIF", "VIDEO" to "仅视频").forEach { (type, label) ->
                                    val isSelected = mediaTypeFilter == type
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable { mediaTypeFilter = type }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 地理位置足迹过滤
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("地理位置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(false to "不限位置", true to "仅含 GPS/地点 (足迹相册)").forEach { (hasLoc, label) ->
                                    val isSelected = mustHaveLocation == hasLoc
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable { mustHaveLocation = hasLoc }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ==================== 底部保存操作按钮 (iOS 18 胶囊按钮) ====================
            IosButton(
                text = if (editingAlbum == null) "生成智能相册" else "保存相册规则",
                icon = if (editingAlbum == null) Icons.Filled.AutoAwesome else Icons.Filled.Check,
                variant = IosButtonVariant.PRIMARY,
                onClick = {
                    val finalName = name.trim().ifBlank { "$selectedIcon 智能精选" }
                    val rule = SmartAlbumRule(
                        includeTagIds = includeTagIds.toList(),
                        isAndLogic = isAndLogic,
                        excludeTagIds = excludeTagIds.toList(),
                        yearFilter = yearFilter,
                        mediaTypeFilter = mediaTypeFilter,
                        mustHaveLocation = mustHaveLocation
                    )
                    onSave(finalName, selectedIcon, selectedColor, rule)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                height = 46.dp
            )
        }
    }
}
