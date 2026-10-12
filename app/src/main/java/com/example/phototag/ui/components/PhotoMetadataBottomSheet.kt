package com.example.phototag.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.util.MapNavigationHelper
import com.example.phototag.util.PhotoMetadata

/**
 * 照片 EXIF 与真实地理位置、拍摄时段、光学参数详情面板（半透明毛玻璃抽屉 BottomSheet）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoMetadataBottomSheet(
    metadata: PhotoMetadata?,
    isLoading: Boolean,
    tags: List<TagEntity> = emptyList(),
    allAvailableTags: List<TagEntity> = emptyList(),
    onToggleTag: (tagId: Long, isAdded: Boolean) -> Unit = { _, _ -> },
    onUpdateCustomLocation: (photoId: Long, customLocation: String?) -> Unit = { _, _ -> },
    onRefreshMetadata: () -> Unit = {},
    onOpenSpoofSheet: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showEditLocationDialog by remember { mutableStateOf(false) }
    var editLocationText by remember(metadata?.customLocation) {
        mutableStateOf(metadata?.customLocation ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "照片详细信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        metadata?.fileName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                onDismiss()
                                onOpenSpoofSheet()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.VpnKey, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("伪装信息", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            if (isLoading || metadata == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "正在深度解析 EXIF 拍摄参数与地理位置…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val currentTagIds = remember(tags) { tags.map { it.tagId }.toSet() }
                val unassignedCandidateTags = remember(allAvailableTags, currentTagIds) {
                    allAvailableTags.filter { it.tagId !in currentTagIds }.take(10)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ==================== 1. 【📅 拍摄与时间维度】 ====================
                    item {
                        MetadataSectionCard(
                            icon = Icons.Filled.Schedule,
                            title = "拍摄与时间",
                            iconTint = MaterialTheme.colorScheme.primary
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MetadataItemRow("拍摄时间", metadata.dateTakenFormatted)

                                // 智能时段与相对时间组合标签
                                if (metadata.timeOfDayBadge != null || metadata.relativeTimeFormatted != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "时段场景",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            metadata.timeOfDayBadge?.let { badge ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                                ) {
                                                    Text(
                                                        text = badge,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            metadata.relativeTimeFormatted?.let { rel ->
                                                Text(
                                                    text = rel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                if (metadata.dateAddedFormatted != metadata.dateTakenFormatted) {
                                    MetadataItemRow("添加相册", metadata.dateAddedFormatted)
                                }
                                metadata.timezoneFormatted?.let {
                                    MetadataItemRow("本地时区", it)
                                }
                            }
                        }
                    }

                    // ==================== 2. 【📍 拍摄地点与空间维度】 ====================
                    item {
                        val hasAnyLocation = !metadata.customLocation.isNullOrBlank() || !metadata.address.isNullOrBlank() || metadata.hasGps
                        MetadataSectionCard(
                            icon = if (hasAnyLocation) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                            title = "拍摄地点",
                            iconTint = if (hasAnyLocation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            action = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (metadata.hasGps && metadata.latitude != null && metadata.longitude != null) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable {
                                                    MapNavigationHelper.openLocationInMap(
                                                        context = context,
                                                        wgsLat = metadata.latitude,
                                                        wgsLng = metadata.longitude,
                                                        locationTitle = metadata.customLocation ?: metadata.address ?: "照片拍摄地"
                                                    )
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.NearMe,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = "在地图中查看",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(6.dp))
                                    }

                                    IconButton(
                                        onClick = { showEditLocationDialog = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "标注/修改地点",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // A. 用户手动标注地点
                                if (!metadata.customLocation.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondary
                                                    ) {
                                                        Text(
                                                            text = "自定义标注",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = metadata.customLocation,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(metadata.customLocation))
                                                    Toast.makeText(context, "地点已复制", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.ContentCopy,
                                                    contentDescription = "复制地点",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // B. 逆编码真实中文地址 (包含高精度建筑场所 POI 与具体街道)
                                if (!metadata.address.isNullOrBlank()) {
                                    val addressFull = metadata.address
                                    val parts = addressFull.split(" · ")
                                    val baseAddr = parts[0].removePrefix("中华人民共和国").removePrefix("中国").trim()
                                    val poiName = parts.getOrNull(1)?.trim()

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (metadata.hasGps && metadata.latitude != null && metadata.longitude != null) {
                                                    MapNavigationHelper.openLocationInMap(
                                                        context = context,
                                                        wgsLat = metadata.latitude,
                                                        wgsLng = metadata.longitude,
                                                        locationTitle = poiName ?: metadata.customLocation ?: baseAddr
                                                    )
                                                }
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = if (!poiName.isNullOrBlank()) "精确地标 / POI" else "详细街道地址",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "点击在地图中精确定位",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(Modifier.weight(1f))

                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(addressFull))
                                                        Toast.makeText(context, "详细地址已复制", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.ContentCopy,
                                                        contentDescription = "复制地址",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(6.dp))

                                            if (!poiName.isNullOrBlank()) {
                                                Text(
                                                    text = poiName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(Modifier.height(3.dp))
                                                Text(
                                                    text = baseAddr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                                )
                                            } else {
                                                Text(
                                                    text = baseAddr,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                } else if (metadata.hasGps && metadata.customLocation.isNullOrBlank()) {
                                    // 自动逆编码解析微动效
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "正在通过高德地图高精引擎解析建筑与场所...",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // C. 【方案 B 智能高海拔展示】若 >= 1000m (登山/高原/飞行) 高亮突出
                                if (metadata.isHighAltitude && metadata.highAltitudeBadge != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = metadata.highAltitudeBadge,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "(EXIF 高程传感器)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                // D. GPS 经纬度 + 低海拔精简合并行
                                metadata.gpsCoordinatesFormatted?.let { coords ->
                                    val fullCoordText = if (!metadata.isHighAltitude && metadata.altitudeShortFormatted != null) {
                                        "经纬度：$coords · ${metadata.altitudeShortFormatted}"
                                    } else {
                                        "经纬度：$coords"
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = fullCoordText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(coords))
                                                Toast.makeText(context, "经纬度已复制", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.ContentCopy,
                                                contentDescription = "复制经纬度",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // E. 拍摄镜头朝向 (指南针)
                                metadata.compassDirectionFormatted?.let { compass ->
                                    MetadataItemRow("镜头朝向", compass)
                                }

                                // F. 无任何位置数据
                                if (!hasAnyLocation) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "暂无位置数据 (未记录 GPS 经纬度)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = onRefreshMetadata,
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("重新扫描 EXIF / 刷新", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { showEditLocationDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("+ 手动标注地点", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 3. 【📷 相机与镜头专业参数 (机械铭牌)】 ====================
                    val hasCameraInfo = metadata.deviceModel != null || metadata.deviceMake != null || metadata.aperture != null || metadata.iso != null
                    if (hasCameraInfo) {
                        item {
                            MetadataSectionCard(
                                icon = Icons.Filled.CameraAlt,
                                title = "相机与镜头参数",
                                iconTint = MaterialTheme.colorScheme.tertiary
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // 1. 专业相机机身机械铭牌
                                    CameraBrandBadge(
                                        deviceMake = metadata.deviceMake,
                                        deviceModel = metadata.deviceModel
                                    )

                                    // 2. 光学四支柱仪表盘 (光圈 · 快门 · 焦距 · ISO)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OpticalPill(
                                            label = "光圈",
                                            value = metadata.aperture ?: "ƒ/--",
                                            modifier = Modifier.weight(1f)
                                        )
                                        OpticalPill(
                                            label = "快门",
                                            value = metadata.shutterSpeed ?: "--",
                                            modifier = Modifier.weight(1f)
                                        )
                                        OpticalPill(
                                            label = "等效焦距",
                                            value = metadata.focalLengthIn35mm?.let { "$it mm" } ?: metadata.focalLength ?: "--",
                                            modifier = Modifier.weight(1f)
                                        )
                                        OpticalPill(
                                            label = "ISO",
                                            value = metadata.iso ?: "--",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // 3. 镜头型号与其他光学参数
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        metadata.lensModel?.let { lens ->
                                            MetadataItemRow("镜头型号", lens)
                                        }
                                        metadata.exposureBias?.let { MetadataItemRow("曝光补偿", it) }
                                        metadata.whiteBalance?.let { MetadataItemRow("白平衡", it) }
                                        metadata.flash?.let { MetadataItemRow("闪光灯", it) }
                                        metadata.meteringMode?.let { MetadataItemRow("测光模式", it) }
                                        metadata.colorSpace?.let { MetadataItemRow("色彩空间", it) }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 4. 【📄 文件属性与画质质量】 ====================
                    item {
                        MetadataSectionCard(
                            icon = Icons.Filled.Description,
                            title = "文件属性与规格",
                            iconTint = MaterialTheme.colorScheme.secondary
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 规格微光胶囊条
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (metadata.megapixels.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = metadata.megapixels,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    if (metadata.mimeType.isNotBlank()) {
                                        val formatName = metadata.mimeType.substringAfterLast('/').uppercase()
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = formatName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = metadata.fileSizeFormatted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                MetadataItemRow("文件名称", metadata.fileName)
                                if (metadata.resolutionFormatted.isNotBlank()) {
                                    MetadataItemRow("分辨率", metadata.resolutionFormatted)
                                }
                                if (metadata.aspectRatioFormatted.isNotBlank()) {
                                    MetadataItemRow("画幅比例", metadata.aspectRatioFormatted)
                                }
                            }
                        }
                    }

                    // ==================== 5. 【🏷️ 关联相册与标签 (支持即时点按打标)】 ====================
                    if (tags.isNotEmpty() || unassignedCandidateTags.isNotEmpty()) {
                        item {
                            MetadataSectionCard(
                                icon = Icons.Filled.Style,
                                title = "照片标签 (${tags.size})",
                                iconTint = MaterialTheme.colorScheme.primary
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // 现有已关联标签
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        tags.forEach { tag ->
                                            TagChip(
                                                name = tag.name,
                                                colorHex = tag.colorHex,
                                                isSelected = true,
                                                isSystem = tag.isSystem,
                                                onClick = {
                                                    onToggleTag(tag.tagId, false)
                                                }
                                            )
                                        }
                                    }

                                    // 可选添加的快速候选标签
                                    if (unassignedCandidateTags.isNotEmpty()) {
                                        Text(
                                            text = "轻点快速添加标签：",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            unassignedCandidateTags.forEach { candidate ->
                                                TagChip(
                                                    name = "+ ${candidate.name}",
                                                    colorHex = candidate.colorHex,
                                                    isSelected = false,
                                                    isSystem = candidate.isSystem,
                                                    onClick = {
                                                        onToggleTag(candidate.tagId, true)
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

        // ==================== 📍 手动标注/修改地点弹窗 ====================
        if (showEditLocationDialog && metadata != null) {
            AlertDialog(
                onDismissRequest = { showEditLocationDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("手动标注拍摄地点", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "自定义输入具体的地理位置（如商圈、景点、门店或街道名），将优先在照片详情与大图中展示：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editLocationText,
                            onValueChange = { editLocationText = it },
                            placeholder = { Text("例如：首尔圣水洞、深圳南山科技园") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newLocation = editLocationText.trim().ifEmpty { null }
                            onUpdateCustomLocation(metadata.photoId, newLocation)
                            showEditLocationDialog = false
                            Toast.makeText(context, if (newLocation != null) "地点已更新为：$newLocation" else "已清除自定义地点", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("保存标注", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        if (!metadata.customLocation.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    editLocationText = ""
                                    onUpdateCustomLocation(metadata.photoId, null)
                                    showEditLocationDialog = false
                                    Toast.makeText(context, "已清除自定义地点", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("清除自定义", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        TextButton(onClick = { showEditLocationDialog = false }) {
                            Text("取消")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MetadataSectionCard(
    icon: ImageVector,
    title: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun MetadataItemRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
