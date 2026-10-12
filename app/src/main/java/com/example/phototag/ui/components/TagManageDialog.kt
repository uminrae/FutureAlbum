package com.example.phototag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.relation.CategoryWithTags
import com.example.phototag.ui.theme.MorandiPalette
import com.example.phototag.ui.theme.parseHexColor

/**
 * 🏷️ 新建标签与相册架构弹窗 (TagManageDialog)。
 *
 * 核心设计：
 * 1. 【顶级父标签 / 子相册层级挂载】：直观挂载至母相册（如 `#i-dle` -> `#曹薇娟`），在 Tab 2「相册」大厅直接展现为文件夹相册。
 * 2. 【智能分类继承与无缝新建】：选定父相册自动继承分类；新建分类在直接点击【保存】时自动入库，绝不丢弃数据。
 * 3. 【莫兰迪高级色彩选择器】：8 色高质感主题色即点即换。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagManageDialog(
    categoriesWithTags: List<CategoryWithTags>,
    rootTags: List<TagEntity> = emptyList(),
    onCreateTag: (name: String, categoryId: Long, colorHex: String, parentId: Long?, newCategoryName: String?) -> Unit,
    onCreateCategory: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var tagName by rememberSaveable { mutableStateOf("") }
    var selectedColorHex by rememberSaveable { mutableStateOf(MorandiPalette.first().hex) }

    var selectedCategoryId by remember {
        mutableStateOf(categoriesWithTags.firstOrNull()?.category?.categoryId ?: 0L)
    }
    var selectedCategoryName by remember {
        mutableStateOf(categoriesWithTags.firstOrNull()?.category?.name ?: "默认分类")
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // 所属父标签（null 表示顶级母标签）
    var selectedParentTagId by remember { mutableStateOf<Long?>(null) }
    var selectedParentTagName by remember { mutableStateOf("无（设为顶级独立相册）") }
    var parentDropdownExpanded by remember { mutableStateOf(false) }

    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "新建标签 / 相册",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // ---- 1. 标签名输入 ----
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名（自动补 # 前缀）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // ---- 2. 莫兰迪高级色彩选择器 ----
                Column {
                    Text(
                        "选择主题配色：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MorandiPalette.forEach { item ->
                            val color = parseHexColor(item.hex)
                            val isSelected = selectedColorHex.equals(item.hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = item.hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = item.name,
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
                        label = { Text("所属父相册 (可选层级挂载)") },
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
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
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
                        rootTags.forEach { root ->
                            DropdownMenuItem(
                                text = { Text("挂载到 ${root.name}") },
                                onClick = {
                                    selectedParentTagId = root.tagId
                                    selectedParentTagName = root.name
                                    // 自动继承父标签所属分类
                                    selectedCategoryId = root.categoryId
                                    selectedCategoryName = categoriesWithTags.find { it.category.categoryId == root.categoryId }?.category?.name ?: selectedCategoryName
                                    parentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ---- 4. 辅助提示 / 分类设置 ----
                if (selectedParentTagId != null) {
                    // 已选定父相册：显示直观的收纳提示，免去用户纠结多余分类的困惑
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "该标签将作为子相册收纳进「$selectedParentTagName」中，在「相册」大厅可分 Tab 浏览",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    // 顶级标签：展示所属大类与新建分类
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = it }
                    ) {
                        TextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("所属分类 (系统分组)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
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

                    // ---- 新建分类输入框 ----
                    if (showNewCategoryInput) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("输入新分类名称") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newCategoryName.isNotBlank()) {
                                    onCreateCategory(newCategoryName)
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
                            Text("+ 新建自定义分类")
                        }
                    }
                }

                // ---- 5. 底部操作按钮 ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val formatted = if (tagName.startsWith("#")) tagName.trim()
                            else "#${tagName.trim()}"
                            val customCat = if (showNewCategoryInput && newCategoryName.isNotBlank()) newCategoryName.trim() else null
                            onCreateTag(formatted, selectedCategoryId, selectedColorHex, selectedParentTagId, customCat)
                            onDismiss()
                        },
                        enabled = tagName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
