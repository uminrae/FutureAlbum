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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.relation.CategoryWithTags
import com.example.phototag.ui.theme.MorandiPalette
import com.example.phototag.ui.theme.parseHexColor

/**
 * 🏷️ 标签深度管理底部操作弹窗（长按标签触发）。
 *
 * 功能：
 * 1. 【修改名称、莫兰迪色、分类与所属父相册】：全面支持编辑所属分类（带新建分类）与母子层级挂载。
 * 2. 【合并标签】：将当前标签合并至另一个已有标签（迁移照片关联并安全清理源标签）。
 * 3. 【删除与归档标签】：安全删除标签或移入归档盒子。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagActionBottomSheet(
    tag: TagEntity,
    allTags: List<TagEntity>,
    categoriesWithTags: List<CategoryWithTags> = emptyList(),
    rootTags: List<TagEntity> = emptyList(),
    onUpdateTag: (tagId: Long, newName: String, newColorHex: String, categoryId: Long, parentId: Long?, newCategoryName: String?) -> Unit,
    onMergeTag: (sourceTagId: Long, targetTagId: Long) -> Unit,
    onDeleteTag: (tagId: Long) -> Unit,
    onArchiveTag: ((tagId: Long, isArchived: Boolean) -> Unit)? = null,
    onCreateCategory: ((name: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentTab by remember { mutableStateOf(0) } // 0 = 编辑属性, 1 = 合并标签

    // 编辑状态
    var editName by remember { mutableStateOf(tag.name.removePrefix("#")) }
    var selectedColorHex by remember { mutableStateOf(tag.colorHex) }

    // 分类状态
    var selectedCategoryId by remember(tag.categoryId) { mutableStateOf(tag.categoryId) }
    var selectedCategoryName by remember(tag.categoryId, categoriesWithTags) {
        mutableStateOf(
            categoriesWithTags.find { it.category.categoryId == tag.categoryId }?.category?.name ?: "默认分类"
        )
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    // 所属父相册
    val availableParentRoots = remember(rootTags, tag) {
        rootTags.filter { it.tagId != tag.tagId }
    }
    var selectedParentTagId by remember(tag.parentId) { mutableStateOf(tag.parentId) }
    var selectedParentTagName by remember(tag.parentId, allTags) {
        val parent = allTags.find { it.tagId == tag.parentId }
        mutableStateOf(if (parent != null) "挂载到 ${parent.name}" else "无（设为顶级独立相册）")
    }
    var parentDropdownExpanded by remember { mutableStateOf(false) }

    // 合并状态
    val availableMergeTargets = remember(allTags, tag) {
        allTags.filter { it.tagId != tag.tagId }
    }
    var selectedTargetTag by remember { mutableStateOf(availableMergeTargets.firstOrNull()) }
    var mergeDropdownExpanded by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMergeConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 顶部标题与标签预览 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = parseHexColor(selectedColorHex).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, parseHexColor(selectedColorHex)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Style,
                            null,
                            tint = parseHexColor(selectedColorHex),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (tag.isSystem) "系统智能属性" else "自定义相册标签 · 所属分类: $selectedCategoryName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 快速切换标签卡片 (编辑 / 合并)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (currentTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentTab = 0 }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "编辑属性",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (currentTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentTab = 1 }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "合并",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ==================== Tab 0: 修改名称、色彩、分类与层级 ====================
            if (currentTab == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ---- 1. 标签名称 ----
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("标签名称") },
                        prefix = { Text("# ", fontWeight = FontWeight.Bold, color = parseHexColor(selectedColorHex)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ---- 2. 莫兰迪配色选择盘 ----
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "标签专属配色：",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MorandiPalette.forEach { morandi ->
                                val isSelected = selectedColorHex.equals(morandi.hex, ignoreCase = true)
                                val color = parseHexColor(morandi.hex)

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedColorHex = morandi.hex
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "已选择",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- 3. 所属父相册/父标签（层级挂载） ----
                    ExposedDropdownMenuBox(
                        expanded = parentDropdownExpanded,
                        onExpandedChange = { parentDropdownExpanded = it }
                    ) {
                        TextField(
                            value = selectedParentTagName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("所属父相册 (层级挂载)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = parentDropdownExpanded,
                            onDismissRequest = { parentDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("无（设为顶级独立相册）", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedParentTagId = null
                                    selectedParentTagName = "无（设为顶级独立相册）"
                                    parentDropdownExpanded = false
                                }
                            )
                            availableParentRoots.forEach { root ->
                                DropdownMenuItem(
                                    text = { Text("挂载到 ${root.name}") },
                                    onClick = {
                                        selectedParentTagId = root.tagId
                                        selectedParentTagName = "挂载到 ${root.name}"
                                        selectedCategoryId = root.categoryId
                                        selectedCategoryName = categoriesWithTags.find { it.category.categoryId == root.categoryId }?.category?.name ?: selectedCategoryName
                                        parentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // ---- 4. 所属分类修改 ----
                    if (selectedParentTagId == null) {
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = it }
                        ) {
                            TextField(
                                value = selectedCategoryName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("所属分类 (系统大类)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categoriesWithTags.forEach { cwt ->
                                    DropdownMenuItem(
                                        text = { Text(cwt.category.name) },
                                        onClick = {
                                            selectedCategoryId = cwt.category.categoryId
                                            selectedCategoryName = cwt.category.name
                                            showNewCategoryInput = false
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 新建分类输入框
                        if (showNewCategoryInput) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = { Text("新建并更改为该分类") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newCategoryName.isNotBlank()) {
                                        onCreateCategory?.invoke(newCategoryName)
                                        selectedCategoryName = newCategoryName
                                        focusManager.clearFocus()
                                    }
                                }),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            TextButton(
                                onClick = { showNewCategoryInput = true },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Text("+ 新建并移动到新分类")
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "已挂载至父相册，分类自动继承母相册",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ---- 5. 保存修改按钮 ----
                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val formatted = if (editName.startsWith("#")) editName.trim() else "#${editName.trim()}"
                                val customCat = if (showNewCategoryInput && newCategoryName.isNotBlank()) newCategoryName.trim() else null
                                onUpdateTag(tag.tagId, formatted, selectedColorHex, selectedCategoryId, selectedParentTagId, customCat)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存修改")
                    }
                }
            }

            // ==================== Tab 1: 标签合并 ====================
            if (currentTab == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "将【${tag.name}】旗下的所有照片全部自动转移至目标标签，并在数据库中移除当前标签：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (availableMergeTargets.isEmpty()) {
                        Text(
                            "暂无可供合并的其它标签（请先在相册中创建另一个标签）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = mergeDropdownExpanded,
                            onExpandedChange = { mergeDropdownExpanded = !mergeDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedTargetTag?.name ?: "请选择目标标签",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("目标标签") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mergeDropdownExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = mergeDropdownExpanded,
                                onDismissRequest = { mergeDropdownExpanded = false }
                            ) {
                                availableMergeTargets.forEach { target ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(parseHexColor(target.colorHex))
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(target.name)
                                            }
                                        },
                                        onClick = {
                                            selectedTargetTag = target
                                            mergeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showMergeConfirmDialog = true },
                            enabled = selectedTargetTag != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CallMerge, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("合并至「${selectedTargetTag?.name ?: ""}」")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // ==================== 底部危险与归档操作区 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 归档 / 取消归档按钮
                if (onArchiveTag != null) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onArchiveTag(tag.tagId, !tag.isArchived)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(
                            if (tag.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (tag.isArchived) "取消归档" else "归档标签")
                    }
                }

                // 删除标签按钮
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("删除标签")
                }
            }
        }
    }

    // 删除确认二次弹窗
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除标签「${tag.name}」？") },
            text = { Text("删除标签仅会解除该标签与照片的关联，相册中的原始物理照片绝不会被删除。") },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirmDialog = false
                        onDeleteTag(tag.tagId)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 合并确认二次弹窗
    if (showMergeConfirmDialog && selectedTargetTag != null) {
        val target = selectedTargetTag!!
        AlertDialog(
            onDismissRequest = { showMergeConfirmDialog = false },
            title = { Text("确认合并标签？") },
            text = { Text("将把「${tag.name}」下的所有照片全部并入「${target.name}」，原「${tag.name}」将被永久删除。该操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMergeConfirmDialog = false
                        onMergeTag(tag.tagId, target.tagId)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("确认合并")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
