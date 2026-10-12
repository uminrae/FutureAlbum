package com.example.phototag.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.relation.CategoryWithTags
import com.example.phototag.util.PinyinUtils

/**
 * 原子级标签转移与替换弹窗（TransferTagBottomSheet）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransferTagBottomSheet(
    selectedPhotoCount: Int,
    selectedPhotosTags: List<TagEntity>,
    categoriesWithTags: List<CategoryWithTags>,
    allTags: List<TagEntity>,
    onTransfer: (fromTagId: Long?, toTagId: Long) -> Unit,
    onCreateTag: (name: String, catId: Long, parentId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 源标签（null 表示选定照片上的全部已有旧标签）
    var selectedFromTagId by remember { mutableStateOf<Long?>(selectedPhotosTags.firstOrNull()?.tagId) }

    // 目标标签
    var selectedToTagId by remember { mutableStateOf<Long?>(null) }

    // 搜索过滤目标标签
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    // 新建目标标签输入
    var showCreateTagInput by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    val filteredTargetTags = remember(allTags, searchQuery, selectedCategoryId) {
        val query = searchQuery.trim()
        allTags.filter { tag ->
            val matchCat = selectedCategoryId == null || tag.categoryId == selectedCategoryId
            val matchQuery = query.isBlank() || PinyinUtils.matchesQuery(tag.name, query)
            matchCat && matchQuery
        }
    }

    val fromTagName = remember(selectedFromTagId, selectedPhotosTags, allTags) {
        if (selectedFromTagId == null) {
            "全部已有标签"
        } else {
            allTags.find { it.tagId == selectedFromTagId }?.name ?: "指定标签"
        }
    }

    val toTagName = remember(selectedToTagId, allTags) {
        allTags.find { it.tagId == selectedToTagId }?.name
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "标签转移与替换",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "选定 $selectedPhotoCount 张照片 · 原子级事务更新",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==================== 1. 选择源标签 (转出/移除) ====================
                item {
                    Text(
                        "1. 选择需要移除/转出的【源标签】：",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 选项：全部已有标签（完全清空旧标签并换新）
                        FilterChip(
                            selected = selectedFromTagId == null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFromTagId = null
                            },
                            label = { Text("全部已有标签 (完全替换)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(50)
                        )

                        // 选定照片已包含的具体标签
                        for (tag in selectedPhotosTags) {
                            val isSelected = selectedFromTagId == tag.tagId
                            val tagColor = try {
                                Color(android.graphics.Color.parseColor(tag.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFromTagId = tag.tagId
                                },
                                label = { Text(tag.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(tagColor)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tagColor.copy(alpha = 0.22f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = if (isSelected) FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = true,
                                    borderColor = tagColor,
                                    borderWidth = 1.5.dp
                                ) else null,
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }

                // ==================== 2. 选择目标标签 (转入/替换) ====================
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "2. 选择转移绑定的【目标标签】：",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Text(
                            text = if (toTagName != null) "已选: $toTagName" else "未选择",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (toTagName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // 搜索框与新建标签
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索标签 (全拼/简拼)", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { showCreateTagInput = !showCreateTagInput },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("新建", fontSize = 13.sp)
                        }
                    }

                    // 展开新建标签输入
                    AnimatedVisibility(visible = showCreateTagInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTagName,
                                onValueChange = { newTagName = it },
                                placeholder = { Text("#新标签名", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newTagName.isNotBlank()) {
                                        val defaultCatId = categoriesWithTags.firstOrNull()?.category?.categoryId ?: 1L
                                        val formatted = if (newTagName.startsWith("#")) newTagName else "#$newTagName"
                                        onCreateTag(formatted, defaultCatId, null)
                                        newTagName = ""
                                        showCreateTagInput = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("保存", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 分类横向切换
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text("全部分类") },
                                shape = RoundedCornerShape(50)
                            )
                        }
                        items(categoriesWithTags) { catWithTags ->
                            FilterChip(
                                selected = selectedCategoryId == catWithTags.category.categoryId,
                                onClick = {
                                    selectedCategoryId = if (selectedCategoryId == catWithTags.category.categoryId) null else catWithTags.category.categoryId
                                },
                                label = { Text(catWithTags.category.name) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 目标标签候选列表
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (tag in filteredTargetTags) {
                            // 排除自身作为目标标签（避免无意义的同名自转）
                            if (tag.tagId == selectedFromTagId) continue

                            val isSelected = selectedToTagId == tag.tagId
                            val tagColor = try {
                                Color(android.graphics.Color.parseColor(tag.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.secondary
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedToTagId = tag.tagId
                                },
                                label = { Text(tag.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) }
                                } else {
                                    {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(tagColor)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }

                // ==================== 3. 动态操作预览摘要 ====================
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (selectedToTagId != null) {
                                        if (selectedFromTagId == null) {
                                            "将选中的 $selectedPhotoCount 张照片上的旧标签全部清除，统一替换为「$toTagName」"
                                        } else {
                                            "将选中的照片中的「$fromTagName」移除，并转移绑定至「$toTagName」（其余标签保留）"
                                        }
                                    } else {
                                        "请在上方选择目标标签以完成转移配置"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedToTagId != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 底部确认与取消操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("取消")
                }

                Button(
                    onClick = {
                        val targetId = selectedToTagId
                        if (targetId != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTransfer(selectedFromTagId, targetId)
                        }
                    },
                    enabled = selectedToTagId != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("确认转移并替换", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
