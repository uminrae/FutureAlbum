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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phototag.util.exif.ExifSpoofPresets
import com.example.phototag.util.exif.SpoofConfig
import com.example.phototag.util.exif.SpoofDevicePreset
import com.example.phototag.util.exif.SpoofLocationPreset
import com.example.phototag.util.exif.SpoofTimeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExifSpoofBottomSheet(
    onDismiss: () -> Unit,
    onConfirmShare: (SpoofConfig) -> Unit,
    onConfirmSaveCopy: (SpoofConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 选中状态
    var selectedLocation by remember { mutableStateOf(ExifSpoofPresets.KOREA_LOCATIONS.first()) }
    var locationCategoryTab by remember { mutableIntStateOf(0) } // 0: 韩国, 1: 全球, 2: 自定义

    // 自定义地点输入
    var customLocName by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf("37.5512") }
    var customLng by remember { mutableStateOf("126.9882") }
    var customAddress by remember { mutableStateOf("") }

    var selectedDevice by remember { mutableStateOf(ExifSpoofPresets.APPLE_DEVICES.first()) }
    var deviceBrandTab by remember { mutableIntStateOf(0) } // 0: Apple, 1: Samsung, 2: Camera, 3: 自定义

    // 自定义设备输入
    var customMake by remember { mutableStateOf("Apple") }
    var customModel by remember { mutableStateOf("iPhone 16 Pro Max") }

    var selectedTimeMode by remember { mutableStateOf(SpoofTimeMode.KEEP_ORIGINAL) }

    fun buildCurrentConfig(): SpoofConfig {
        val (locName, lat, lng, addr) = if (locationCategoryTab == 2) {
            val pLat = customLat.toDoubleOrNull() ?: 37.5512
            val pLng = customLng.toDoubleOrNull() ?: 126.9882
            val name = customLocName.ifBlank { "自定义坐标" }
            val address = customAddress.ifBlank { "$name ($pLat, $pLng)" }
            Tuple4(name, pLat, pLng, address)
        } else {
            Tuple4(selectedLocation.name, selectedLocation.latitude, selectedLocation.longitude, selectedLocation.displayAddress)
        }

        val (make, model, devName) = if (deviceBrandTab == 3) {
            val m = customMake.ifBlank { "Apple" }
            val mo = customModel.ifBlank { "iPhone 16 Pro Max" }
            Tuple3(m, mo, "$m $mo")
        } else {
            Tuple3(selectedDevice.make, selectedDevice.model, selectedDevice.name)
        }

        return SpoofConfig(
            enableLocation = true,
            locationName = locName,
            latitude = lat,
            longitude = lng,
            altitude = 35.0,
            displayAddress = addr,
            enableDevice = true,
            deviceName = devName,
            make = make,
            model = model,
            software = if (make.contains("Apple", true)) "iOS 18.1" else "Android 15",
            enableTime = selectedTimeMode != SpoofTimeMode.KEEP_ORIGINAL,
            timeMode = selectedTimeMode
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ==================== 顶部标题与关闭 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "元数据伪装与隐私保护",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "可写入自定义 GPS 经纬度、机型与拍摄时间",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }
            }

            Spacer(Modifier.height(10.dp))

            // ==================== 实时伪装生效预览卡片 ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("当前伪装参数预览：", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("伪装地点", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Text(
                                    if (locationCategoryTab == 4) customLocName.ifBlank { "自定义坐标" } else selectedLocation.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("伪装设备", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                Text(
                                    if (deviceBrandTab == 3) customModel.ifBlank { "自定义机型" } else selectedDevice.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("拍摄时间", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                Text(
                                    selectedTimeMode.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ==================== 可滚动配置内容 ====================
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ------------------ 1. 地理位置选择 ------------------
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("1. 选择伪装地理位置", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        SecondaryScrollableTabRow(
                            selectedTabIndex = locationCategoryTab,
                            edgePadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(selected = locationCategoryTab == 0, onClick = { locationCategoryTab = 0 }, text = { Text("韩国商圈", fontSize = 12.sp) })
                            Tab(selected = locationCategoryTab == 1, onClick = { locationCategoryTab = 1 }, text = { Text("日本商圈", fontSize = 12.sp) })
                            Tab(selected = locationCategoryTab == 2, onClick = { locationCategoryTab = 2 }, text = { Text("国内商圈", fontSize = 12.sp) })
                            Tab(selected = locationCategoryTab == 3, onClick = { locationCategoryTab = 3 }, text = { Text("国际名胜", fontSize = 12.sp) })
                            Tab(selected = locationCategoryTab == 4, onClick = { locationCategoryTab = 4 }, text = { Text("自定义", fontSize = 12.sp) })
                        }

                        Spacer(Modifier.height(8.dp))

                        when (locationCategoryTab) {
                            0 -> LocationPresetFlowRow(ExifSpoofPresets.KOREA_LOCATIONS, selectedLocation) { selectedLocation = it }
                            1 -> LocationPresetFlowRow(ExifSpoofPresets.JAPAN_LOCATIONS, selectedLocation) { selectedLocation = it }
                            2 -> LocationPresetFlowRow(ExifSpoofPresets.CHINA_LOCATIONS, selectedLocation) { selectedLocation = it }
                            3 -> LocationPresetFlowRow(ExifSpoofPresets.GLOBAL_LOCATIONS, selectedLocation) { selectedLocation = it }
                            4 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customLocName,
                                        onValueChange = { customLocName = it },
                                        label = { Text("地点名称 (如：首尔圣水洞咖啡街 / 上海安福路)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = customLat,
                                            onValueChange = { customLat = it },
                                            label = { Text("纬度 (如：37.5446)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = customLng,
                                            onValueChange = { customLng = it },
                                            label = { Text("经度 (如：127.0560)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ------------------ 2. 拍摄机型选择 ------------------
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("2. 选择伪装拍摄机型", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        SecondaryTabRow(
                            selectedTabIndex = deviceBrandTab,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(selected = deviceBrandTab == 0, onClick = { deviceBrandTab = 0 }, text = { Text("Apple", fontSize = 12.sp) })
                            Tab(selected = deviceBrandTab == 1, onClick = { deviceBrandTab = 1 }, text = { Text("三星", fontSize = 12.sp) })
                            Tab(selected = deviceBrandTab == 2, onClick = { deviceBrandTab = 2 }, text = { Text("专业相机", fontSize = 12.sp) })
                            Tab(selected = deviceBrandTab == 3, onClick = { deviceBrandTab = 3 }, text = { Text("自定义", fontSize = 12.sp) })
                        }

                        Spacer(Modifier.height(8.dp))

                        when (deviceBrandTab) {
                            0 -> DevicePresetList(ExifSpoofPresets.APPLE_DEVICES, selectedDevice) { selectedDevice = it }
                            1 -> DevicePresetList(ExifSpoofPresets.SAMSUNG_DEVICES, selectedDevice) { selectedDevice = it }
                            2 -> DevicePresetList(ExifSpoofPresets.CAMERA_DEVICES, selectedDevice) { selectedDevice = it }
                            3 -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customMake,
                                        onValueChange = { customMake = it },
                                        label = { Text("品牌 (如 Apple / Sony)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customModel,
                                        onValueChange = { customModel = it },
                                        label = { Text("型号 (如 iPhone 16 Pro)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ------------------ 3. 拍摄时间选择 ------------------
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("3. 选择伪装拍摄时间", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(SpoofTimeMode.entries) { mode ->
                                val isSelected = selectedTimeMode == mode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedTimeMode = mode }
                                ) {
                                    Text(
                                        mode.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 提示说明
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "提示：已注入全套工业级 EXIF GPS 元数据与相册经纬度。通过微信/QQ等发送给他人时，请务必勾选「原图」发送（普通发送会被社交软件强制抹除地理信息）。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ------------------ 底部操作按钮 ------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val config = buildCurrentConfig()
                        onConfirmSaveCopy(config)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("保存伪装副本", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val config = buildCurrentConfig()
                        onConfirmShare(config)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("伪装并分享", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DevicePresetList(
    presets: List<SpoofDevicePreset>,
    selected: SpoofDevicePreset,
    onSelect: (SpoofDevicePreset) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        presets.forEach { dev ->
            val isSelected = selected.id == dev.id
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(dev) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dev.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationPresetFlowRow(
    presets: List<SpoofLocationPreset>,
    selected: SpoofLocationPreset,
    onSelect: (SpoofLocationPreset) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        presets.forEach { loc ->
            val isSelected = selected.id == loc.id
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(loc) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        loc.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private data class Tuple3<A, B, C>(val a: A, val b: B, val c: C)
private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)