package com.example.phototag.ui

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.phototag.PhotoTagApplication
import com.example.phototag.ui.screen.ImageEditScreen
import com.example.phototag.ui.screen.PhotoListScreen
import com.example.phototag.ui.screen.PhotoViewerScreen
import com.example.phototag.ui.screen.QuickTagScreen
import com.example.phototag.ui.screen.QuickTagViewModel
import com.example.phototag.ui.screen.TagAlbumScreen
import com.example.phototag.ui.screen.TrashScreen
import com.example.phototag.ui.screen.TrashViewModel
import com.example.phototag.ui.components.FpsMonitorOverlay
import com.example.phototag.ui.components.GalleryCleanupLabBottomSheet
import com.example.phototag.ui.components.GalleryLabsBottomSheet
import com.example.phototag.util.GalleryLabsPreferences
import com.example.phototag.util.HapticFeedbackUtil

/**
 * 应用主 Shell。
 */
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDeviceBucket by viewModel.selectedDeviceBucket.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAllFilesPermissionGuideDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableStateOf(0L) }
    var lastPhotoTabClickTime by remember { mutableStateOf(0L) }
    val photoGridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        GalleryLabsPreferences.init(context)
    }

    // ==================== 返回键层级拦截与首页双击防误退 ====================
    BackHandler(enabled = true) {
        when {
            uiState.isEditingPhoto -> viewModel.closeImageEditor()
            uiState.isViewingPhoto -> viewModel.closePhotoViewer()
            uiState.isQuickTagMode -> viewModel.closeQuickTagMode()
            uiState.isViewingTrash -> viewModel.closeTrashScreen()
            uiState.showCleanupLabSheet -> viewModel.closeCleanupLab()
            uiState.showGalleryLabsSheet -> viewModel.closeGalleryLabs()
            uiState.isMultiSelectMode -> viewModel.exitMultiSelectMode()
            uiState.selectedSmartAlbum != null -> viewModel.closeSmartAlbumView()
            uiState.tagAlbumSelectedTagId != null -> viewModel.closeTagAlbum()
            selectedDeviceBucket != null -> viewModel.closeDeviceBucket()
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "再按一次退出相册", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==================== MediaStore 系统级删除确认 Launcher ====================
    val deleteResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDeleteConfirmed()
        } else {
            viewModel.clearPendingDelete()
        }
    }

    // ==================== MediaStore 系统级回收站（隔离/恢复）确认 Launcher ====================
    val trashResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onTrashConsentConfirmed()
        } else {
            viewModel.onTrashConsentCancelled()
        }
    }

    // 监听 pendingTrashIntentSender 发起系统级回收站隔离/恢复请求
    LaunchedEffect(uiState.pendingTrashIntentSender) {
        uiState.pendingTrashIntentSender?.let { sender ->
            try {
                trashResultLauncher.launch(
                    IntentSenderRequest.Builder(sender).build()
                )
            } catch (e: Exception) {
                viewModel.onTrashConsentCancelled()
                snackbarHostState.showSnackbar("调起回收站授权失败：${e.localizedMessage}")
            }
        }
    }

    // 监听 pendingDeleteUris 发起删除请求
    LaunchedEffect(uiState.pendingDeleteUris) {
        val uris = uiState.pendingDeleteUris
        if (uris.isEmpty()) return@LaunchedEffect

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val deleteRequest = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    uris
                )
                deleteResultLauncher.launch(
                    IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                )
            } catch (e: Exception) {
                viewModel.clearPendingDelete()
                snackbarHostState.showSnackbar("发起删除失败：${e.localizedMessage}")
            }
        } else {
            uris.forEach { uri ->
                try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            viewModel.onDeleteConfirmed()
        }
    }

    // ==================== 事件监听 (分享 / 转移撤销 / 回收站撤销) ====================
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.SharePhotos -> {
                    if (event.uris.isEmpty()) return@collect
                    try {
                        val isSingle = event.uris.size == 1
                        val firstUri = event.uris.first()
                        val mimeType = try {
                            context.contentResolver.getType(firstUri) ?: "image/*"
                        } catch (_: Exception) {
                            "image/*"
                        }

                        val shareIntent = if (isSingle) {
                            Intent(Intent.ACTION_SEND).apply {
                                type = mimeType
                                putExtra(Intent.EXTRA_STREAM, firstUri)
                                clipData = ClipData.newRawUri("image", firstUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        } else {
                            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "image/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(event.uris))
                                val clipData = ClipData.newRawUri("image", firstUri)
                                for (i in 1 until event.uris.size) {
                                    clipData.addItem(ClipData.Item(event.uris[i]))
                                }
                                this.clipData = clipData
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }

                        val chooser = Intent.createChooser(
                            shareIntent,
                            if (isSingle) "分享内容" else "批量分享 ${event.uris.size} 项内容"
                        ).apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        // 明确授权给所有可能处理此分享的应用包（彻底解决 QQ / 微信等读取 FileProvider 失败导致的发送失败）
                        val resInfoList = context.packageManager.queryIntentActivities(
                            chooser,
                            PackageManager.MATCH_DEFAULT_ONLY
                        )
                        for (resolveInfo in resInfoList) {
                            val packageName = resolveInfo.activityInfo.packageName
                            for (uri in event.uris) {
                                context.grantUriPermission(
                                    packageName,
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }
                        }

                        context.startActivity(chooser)
                    } catch (e: Exception) {
                        Toast.makeText(context, "调起分享失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
                is UiEvent.ShowTransferUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "撤销",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.undoTransfer(
                            photoUris = event.photoUris,
                            originalFromTagId = event.fromTagId,
                            originalToTagId = event.toTagId
                        )
                    }
                }
                is UiEvent.ShowTrashUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "撤销",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.undoLastSoftDelete(context)
                    }
                }
            }
        }
    }

    // ==================== 错误 Snackbar ====================
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val onDeleteRequest: () -> Unit = {
        // 直接触发移入回收站，跳过应用内确认弹窗
        viewModel.requestSoftDeleteSelectedPhotos(context)

        // 若为 Android 11+ 且尚未开启所有文件访问权限，提示一次开启指南以彻底跳过系统二次确认弹窗
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val prefs = context.getSharedPreferences("trash_prefs", Context.MODE_PRIVATE)
            val hasPrompted = prefs.getBoolean("has_prompted_all_files", false)
            if (!hasPrompted) {
                showAllFilesPermissionGuideDialog = true
            }
        }
    }

    Scaffold(
        snackbarHost = {
            val isDark = isSystemInDarkTheme()
            val isViewing = uiState.isViewingPhoto
            val isMultiSelect = uiState.isMultiSelectMode
            val bottomPad = when {
                isViewing -> 208.dp
                isMultiSelect -> 92.dp
                else -> 72.dp
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = bottomPad, start = 16.dp, end = 16.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (isDark || isViewing) Color(0xEE2C2C2E) else Color(0xEE1C1C1E),
                    contentColor = Color.White,
                    actionColor = Color(0xFF0A84FF),
                    dismissActionContentColor = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val isTab0 = uiState.selectedTab == 0
                val isTab1 = uiState.selectedTab == 1

                // Tab 0: 照片列表（持久化持有 photoGridState，保留浏览位置且切走时 0 额外 CPU/GPU 开销）
                if (isTab0) {
                    PhotoListScreen(
                        viewModel = viewModel,
                        innerPadding = innerPadding,
                        onDeleteRequest = onDeleteRequest,
                        gridState = photoGridState
                    )
                }

                // Tab 1: 相册空间
                if (isTab1) {
                    TagAlbumScreen(
                        viewModel = viewModel,
                        innerPadding = innerPadding,
                        onDeleteRequest = onDeleteRequest
                    )
                }

                // 🍏 真正纯净浮空的底部胶囊岛 (直接悬浮在照片内容层上方，周围完全透明，照片在背后自由穿透滚动)
                val isSubAlbumOpen = uiState.selectedSmartAlbum != null || uiState.tagAlbumSelectedTagId != null || selectedDeviceBucket != null
                if (!uiState.isViewingPhoto && !uiState.isQuickTagMode && !uiState.isViewingTrash && !uiState.isMultiSelectMode && !uiState.isEditingPhoto && !isSubAlbumOpen) {
                    com.example.phototag.ui.components.IosFloatingTabBar(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { tabIndex ->
                            if (tabIndex == 0 && uiState.selectedTab == 0) {
                                val now = System.currentTimeMillis()
                                if (now - lastPhotoTabClickTime < 350L) {
                                    // 快速双击 -> 直达相册底部（看最早照片）
                                    HapticFeedbackUtil.doubleClick(context)
                                    viewModel.requestScrollToBottom()
                                    lastPhotoTabClickTime = 0L
                                } else {
                                    // 单击 -> 平滑回到相册最顶部
                                    HapticFeedbackUtil.click(context)
                                    viewModel.requestScrollToTop()
                                    lastPhotoTabClickTime = now
                                }
                            } else {
                                HapticFeedbackUtil.click(context)
                                viewModel.selectTab(tabIndex)
                            }
                        },
                        tabItems = listOf(
                            com.example.phototag.ui.components.IosTabItem("照片", Icons.Filled.PhotoLibrary),
                            com.example.phototag.ui.components.IosTabItem("相册", Icons.Filled.Style)
                        ),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        // ==================== 🖼️ 全屏大图查看器（即时销毁杜绝半透明虚影残留在网格上方） ====================
        if (uiState.isViewingPhoto && uiState.viewerPhotos.isNotEmpty() && !uiState.isEditingPhoto) {
            PhotoViewerScreen(
                photos = uiState.viewerPhotos,
                initialIndex = uiState.viewerInitialIndex,
                viewModel = viewModel,
                onBack = { viewModel.closePhotoViewer() }
            )
        }

        // ==================== 🎨 全屏图片编辑器（裁剪 + Instagram 模板水印） ====================
        if (uiState.isEditingPhoto && uiState.editingPhoto != null) {
            ImageEditScreen(
                photo = uiState.editingPhoto!!,
                onDismiss = { viewModel.closeImageEditor() },
                onSaveSuccess = { newUri ->
                    viewModel.onPhotoEdited(newUri)
                }
            )
        }

        // ==================== ⚡ 极速整理模式 ====================
        AnimatedVisibility(
            visible = uiState.isQuickTagMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val quickTagViewModel: QuickTagViewModel = viewModel(
                factory = QuickTagViewModel.factory(
                    photoRepository = viewModel.photoRepository,
                    tagRepository = viewModel.tagRepository
                )
            )

            QuickTagScreen(
                quickTagViewModel = quickTagViewModel,
                onBack = { viewModel.closeQuickTagMode() }
            )
        }

        // ==================== 🗑️ 安全回收站页面 ====================
        AnimatedVisibility(
            visible = uiState.isViewingTrash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val app = context.applicationContext as PhotoTagApplication
            val trashViewModel: TrashViewModel = viewModel(
                factory = TrashViewModel.factory(photoRepository = app.photoRepository)
            )
            TrashScreen(
                trashViewModel = trashViewModel,
                onBack = { viewModel.closeTrashScreen() }
            )
        }

        // ==================== 🧹 Samsung 相册瘦身工坊 BottomSheet ====================
        if (uiState.showCleanupLabSheet) {
            val allPhotos by viewModel.filteredPhotos.collectAsState()
            GalleryCleanupLabBottomSheet(
                allPhotos = allPhotos,
                onDeletePhotos = { uris ->
                    viewModel.softDeletePhotos(uris)
                },
                onPreviewPhoto = { photo ->
                    viewModel.openPhotoViewer(listOf(photo), 0)
                },
                onDismiss = { viewModel.closeCleanupLab() }
            )
        }

        // ==================== 🧪 Samsung 相册实验室 (Gallery Labs) BottomSheet ====================
        if (uiState.showGalleryLabsSheet) {
            GalleryLabsBottomSheet(
                onDismiss = { viewModel.closeGalleryLabs() }
            )
        }

        // ==================== ⚡ 120Hz 实时帧率浮标 (FPS Overlay) ====================
        val isFpsOverlayEnabled by GalleryLabsPreferences.isFpsOverlayEnabled.collectAsState()
        FpsMonitorOverlay(
            isVisible = isFpsOverlayEnabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        )

        // ==================== 提示开启所有文件访问权限（实现免系统弹窗秒删与全系统隔离） ====================
        if (showAllFilesPermissionGuideDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAllFilesPermissionGuideDialog = false
                    context.getSharedPreferences("trash_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("has_prompted_all_files", true).apply()
                },
                title = { Text("开启秒删免确认与深度隔离", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "系统正在弹出确认弹窗。若希望以后移入回收站「免去系统确认弹窗」（一键秒删），并让微信、QQ 等外部软件彻底无法看到回收站照片，建议前往系统设置开启【所有文件访问权限】。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAllFilesPermissionGuideDialog = false
                            context.getSharedPreferences("trash_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("has_prompted_all_files", true).apply()
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    ) {
                        Text("前往开启", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAllFilesPermissionGuideDialog = false
                            context.getSharedPreferences("trash_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("has_prompted_all_files", true).apply()
                        }
                    ) {
                        Text("暂不开启")
                    }
                }
            )
        }
        }
    }
}
