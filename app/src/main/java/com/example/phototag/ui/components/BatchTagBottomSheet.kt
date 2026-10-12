package com.example.phototag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.relation.CategoryWithTags

/**
 * 批量打标底部弹窗（支持母子层级架构树形展示、智能频次与最近使用快捷排序）。
 *
 * 核心升级：
 * 1. 【⚡ 最近使用】：快速横向展示最近打过标的 5~6 个标签。
 * 2. 【🔥 高频常用】：展示使用频次最高的常用标签。
 * 3. 【🌲 树形层级展开】：顶级父标签与子标签缩进分组展示（带优雅的面包屑或层级箭头）。
 * 4. 【挂载父标签】：新建标签时可选择挂载为某个已有顶级标签的子标签。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchTagBottomSheet(
    selectedCount: Int,
    categoriesWithTags: List<CategoryWithTags>,
    recentTags: List<TagEntity> = emptyList(),
    topUsedTags: List<TagEntity> = emptyList(),
    getPhotoTagIds: () -> Set<Long>,
    onTagToggle: (tagId: Long, add: Boolean) -> Unit,
    onClearAllTags: (() -> Unit)? = null,
    onCreateTag: (name: String, categoryId: Long, parentId: Long?) -> Unit,
    onCreateCategory: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    // 标签即时过滤输入
    var tagFilterQuery by rememberSaveable { mutableStateOf("") }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // 当前已打标的标签 ID 集合（实时响应）
    val taggedIds = remember { mutableStateOf(getPhotoTagIds()) }

    // 每个分类的展开状态
    val expandedCategories = remember {
        mutableStateMapOf<Long, Boolean>().also { map ->
            categoriesWithTags.forEach { map[it.category.categoryId] = true }
        }
    }

    val displayCategoriesWithTags = remember(categoriesWithTags, tagFilterQuery) {
        if (tagFilterQuery.isBlank()) {
            categoriesWithTags
        } else {
            val q = tagFilterQuery.trim().lowercase()
            categoriesWithTags.mapNotNull { catWithTags ->
                val matching = catWithTags.tags.filter { it.name.lowercase().contains(q) }
                if (matching.isNotEmpty()) {
                    catWithTags.copy(tags = matching)
                } else null
            }
        }
    }

    // 新建标签输入状态（按 categoryId 区分）
    var activeNewTagCategoryId by remember { mutableStateOf<Long?>(null) }
    var newTagName by rememberSaveable { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    var parentDropdownExpanded by remember { mutableStateOf(false) }

    // 新建分类输入状态
    var isCreatingCategory by remember { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }

    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text("清空所有标签", fontWeight = FontWeight.Bold) },
            text = { Text("确定要清除所选 $selectedCount 张照片的所有标签关联吗？此操作不会删除照片，可随时重新打标。") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirmDialog = false
                        onClearAllTags?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            // ==================== 顶部把手与标题栏 ====================
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "选择照片标签",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedCount > 0) "已选 $selectedCount 张照片 · 支持母子层级多选" else "点击标签即时打标",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 清空所有标签按钮
                    if (onClearAllTags != null && selectedCount > 0) {
                        TextButton(
                            onClick = { showClearAllConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text("清空", fontSize = 12.sp)
                        }
                    }

                    // "+ 新建分类" 按钮
                    TextButton(
                        onClick = { isCreatingCategory = !isCreatingCategory }
                    ) {
                        Icon(
                            imageVector = if (isCreatingCategory) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isCreatingCategory) "取消" else "新建分类")
                    }
                }
            }

            // 标签快速检索输入框
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = tagFilterQuery,
                        onValueChange = { tagFilterQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (tagFilterQuery.isEmpty()) {
                                Text(
                                    text = "即时检索标签名称...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (tagFilterQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "清除搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { tagFilterQuery = "" }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // ==================== 新建分类输入行 ====================
            AnimatedVisibility(
                visible = isCreatingCategory,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("输入新分类名称 (如：生活、旅行)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newCategoryName.isNotBlank()) {
                                onCreateCategory(newCategoryName.trim())
                                newCategoryName = ""
                                isCreatingCategory = false
                                focusManager.clearFocus()
                            }
                        })
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onCreateCategory(newCategoryName.trim())
                                newCategoryName = ""
                                isCreatingCategory = false
                                focusManager.clearFocus()
                            }
                        },
                        enabled = newCategoryName.isNotBlank()
                    ) {
                        Text("确定")
                    }
                }
            }

            // ==================== 智能快捷区与分类列表 ====================
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ==================== 1. 【⚡ 最近使用】 ====================
                if (recentTags.isNotEmpty()) {
                    item(key = "section_recent_tags") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "最近使用",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recentTags.forEach { tag ->
                                        val isTagged = tag.tagId in taggedIds.value
                                        TagToggleChip(
                                            tag = tag,
                                            isTagged = isTagged,
                                            onToggle = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val nextState = !isTagged
                                                val parentId = tag.parentId
                                                if (nextState) {
                                                    taggedIds.value = if (parentId != null) taggedIds.value + tag.tagId + parentId else taggedIds.value + tag.tagId
                                                } else {
                                                    taggedIds.value = taggedIds.value - tag.tagId
                                                }
                                                onTagToggle(tag.tagId, nextState)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 2. 【🔥 高频常用】 ====================
                if (topUsedTags.isNotEmpty()) {
                    item(key = "section_top_used_tags") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "高频常用",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    topUsedTags.forEach { tag ->
                                        val isTagged = tag.tagId in taggedIds.value
                                        TagToggleChip(
                                            tag = tag,
                                            isTagged = isTagged,
                                            onToggle = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val nextState = !isTagged
                                                val parentId = tag.parentId
                                                if (nextState) {
                                                    taggedIds.value = if (parentId != null) taggedIds.value + tag.tagId + parentId else taggedIds.value + tag.tagId
                                                } else {
                                                    taggedIds.value = taggedIds.value - tag.tagId
                                                }
                                                onTagToggle(tag.tagId, nextState)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 3. 标签分类母子树形列表 ====================
                if (displayCategoriesWithTags.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (tagFilterQuery.isNotBlank()) "未检索到与「$tagFilterQuery」匹配的标签" else "暂无分类，请点击右上角「新建分类」",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        items = displayCategoriesWithTags,
                        key = { it.category.categoryId }
                    ) { categoryWithTags ->
                        val category = categoryWithTags.category
                        val allCategoryTags = categoryWithTags.tags
                        val isExpanded = if (tagFilterQuery.isNotBlank()) true else (expandedCategories[category.categoryId] ?: true)
                        val isAddingTag = activeNewTagCategoryId == category.categoryId

                        HierarchicalCategoryTagCard(
                            category = category,
                            tags = allCategoryTags,
                            taggedIds = taggedIds.value,
                            isExpanded = isExpanded,
                            isAddingTag = isAddingTag,
                            newTagName = newTagName,
                            selectedParentId = selectedParentId,
                            parentDropdownExpanded = parentDropdownExpanded,
                            onToggleExpand = {
                                expandedCategories[category.categoryId] = !isExpanded
                            },
                            onStartAddTag = {
                                activeNewTagCategoryId = category.categoryId
                                newTagName = ""
                                selectedParentId = null
                            },
                            onCancelAddTag = {
                                activeNewTagCategoryId = null
                                newTagName = ""
                                selectedParentId = null
                            },
                            onNewTagNameChange = { newTagName = it },
                            onParentSelected = { selectedParentId = it },
                            onParentDropdownChange = { parentDropdownExpanded = it },
                            onConfirmAddTag = {
                                val cleanName = if (newTagName.startsWith("#")) newTagName else "#$newTagName"
                                if (cleanName.length > 1) {
                                    onCreateTag(cleanName.trim(), category.categoryId, selectedParentId)
                                    activeNewTagCategoryId = null
                                    newTagName = ""
                                    selectedParentId = null
                                    focusManager.clearFocus()
                                }
                            },
                            onTagToggle = { tagId, add ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val currentTag = allCategoryTags.find { it.tagId == tagId }
                                val parentId = currentTag?.parentId
                                if (add) {
                                    taggedIds.value = if (parentId != null) taggedIds.value + tagId + parentId else taggedIds.value + tagId
                                } else {
                                    taggedIds.value = taggedIds.value - tagId
                                }
                                onTagToggle(tagId, add)
                            }
                        )
                    }
                }
            }

            // ==================== 底部完成按钮 ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("完成")
                }
            }
        }
    }
}

/**
 * 标签快捷切换胶囊。
 */
@Composable
private fun TagToggleChip(
    tag: TagEntity,
    isTagged: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isTagged,
        onClick = onToggle,
        label = {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isTagged) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = if (isTagged) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}

/**
 * 母子层级分类卡片：树形缩进分组展示顶级父标签及其子标签。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HierarchicalCategoryTagCard(
    category: CategoryEntity,
    tags: List<TagEntity>,
    taggedIds: Set<Long>,
    isExpanded: Boolean,
    isAddingTag: Boolean,
    newTagName: String,
    selectedParentId: Long?,
    parentDropdownExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onStartAddTag: () -> Unit,
    onCancelAddTag: () -> Unit,
    onNewTagNameChange: (String) -> Unit,
    onParentSelected: (Long?) -> Unit,
    onParentDropdownChange: (Boolean) -> Unit,
    onConfirmAddTag: () -> Unit,
    onTagToggle: (tagId: Long, add: Boolean) -> Unit
) {
    // 区分根标签与子标签
    val rootTags = remember(tags) { tags.filter { it.parentId == null } }
    val childTagsMap = remember(tags) { tags.filter { it.parentId != null }.groupBy { it.parentId!! } }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // 分类标题栏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${tags.size} 个标签",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "折叠" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 树形标签列表与新建行（可折叠）
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // 遍历每个根标签
                    rootTags.forEach { root ->
                        val children = childTagsMap[root.tagId] ?: emptyList()
                        val isRootTagged = root.tagId in taggedIds

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // 顶级标签行
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TagToggleChip(
                                    tag = root,
                                    isTagged = isRootTagged,
                                    onToggle = { onTagToggle(root.tagId, !isRootTagged) }
                                )

                                if (children.isNotEmpty()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "(${children.size} 个子标签)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 子标签缩进展示（带 ↳ 连接符）
                            if (children.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SubdirectoryArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 6.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        children.forEach { child ->
                                            val isChildTagged = child.tagId in taggedIds
                                            TagToggleChip(
                                                tag = child,
                                                isTagged = isChildTagged,
                                                onToggle = { onTagToggle(child.tagId, !isChildTagged) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // "+ 新建标签" 按钮
                    if (!isAddingTag) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { onStartAddTag() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "新增标签",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "新建标签",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 内联新建标签输入框（支持选择父标签）
                    AnimatedVisibility(visible = isAddingTag) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newTagName,
                                    onValueChange = onNewTagNameChange,
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { onConfirmAddTag() })
                                )
                                Spacer(Modifier.width(6.dp))
                                Button(
                                    onClick = onConfirmAddTag,
                                    enabled = newTagName.isNotBlank()
                                ) {
                                    Text("添加")
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = onCancelAddTag) {
                                    Icon(Icons.Filled.Close, contentDescription = "取消")
                                }
                            }

                            // 挂载父标签下拉选择
                            if (rootTags.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = parentDropdownExpanded,
                                    onExpandedChange = { onParentDropdownChange(it) }
                                ) {
                                    val parentText = rootTags.find { it.tagId == selectedParentId }?.name ?: "无（设为顶级父标签）"
                                    TextField(
                                        value = parentText,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("挂载父标签 (可选)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentDropdownExpanded) },
                                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = parentDropdownExpanded,
                                        onDismissRequest = { onParentDropdownChange(false) }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("无（设为顶级父标签）", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                onParentSelected(null)
                                                onParentDropdownChange(false)
                                            }
                                        )
                                        rootTags.forEach { root ->
                                            DropdownMenuItem(
                                                text = { Text("↳ ${root.name}") },
                                                onClick = {
                                                    onParentSelected(root.tagId)
                                                    onParentDropdownChange(false)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
