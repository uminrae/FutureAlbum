package com.example.phototag

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.phototag.data.worker.WorkManagerScheduler
import com.example.phototag.ui.MainScreen
import com.example.phototag.ui.MainViewModel
import com.example.phototag.ui.theme.PhotoTagTheme

class MainActivity : ComponentActivity() {

    private val app: PhotoTagApplication by lazy {
        application as PhotoTagApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enable120HzHighRefreshRate()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            PhotoTagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoTagApp()
                }
            }
        }
    }

    /**
     * 🚀 强制解锁荣耀/小米/一加/三星等 120Hz 原生高刷新率（解决 MagicOS 智能刷新率锁 60fps 导致的掉帧问题）。
     */
    private fun enable120HzHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = display
                val modes = display?.supportedModes
                val max120Mode = modes?.maxByOrNull { it.refreshRate }
                if (max120Mode != null && max120Mode.refreshRate >= 90f) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = max120Mode.modeId
                    window.attributes = lp
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val lp = window.attributes
                lp.preferredRefreshRate = 120f
                window.attributes = lp
            }
        } catch (e: Exception) {
            // 设备不支持时安全降级
        }
    }

    override fun onStart() {
        super.onStart()
        // 注册系统 MediaStore ContentObserver 实时监听外部删除与新增
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            app.photoRepository.startObservingMediaStore(lifecycleScope)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.photoRepository.stopObservingMediaStore()
    }
}

@Composable
fun PhotoTagApp() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val app = context.applicationContext as PhotoTagApplication

    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.factory(
            photoRepository = app.photoRepository,
            tagRepository = app.tagRepository,
            smartAlbumRepository = app.smartAlbumRepository,
            userPreferences = app.userPreferences
        )
    )

    // 媒体权限（仅请求核心照片和视频权限，杜绝与位置/通知等混淆并发导致系统崩溃）
    val requiredMediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Android 13+ 通知权限请求 Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    // 媒体权限请求 Launcher
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasGrantedMedia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
            permissions[Manifest.permission.READ_MEDIA_VIDEO] == true ||
            permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
            permissions[Manifest.permission.READ_MEDIA_VIDEO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (hasGrantedMedia) {
            viewModel.onPermissionGranted()
            activity?.let {
                app.photoRepository.startObservingMediaStore(it.lifecycleScope)
            }
            // 媒体权限授予后，平滑且串行地请求通知权限（绝不在同一帧并发调用两个 launcher）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    try {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } catch (e: Exception) {
                        android.util.Log.w("MainActivity", "Failed to launch notification permission", e)
                    }
                }
            }
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // 启动时检查并申请权限
    LaunchedEffect(Unit) {
        val hasImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val hasVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasVisualSelected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        if (hasImages || hasVideo || hasVisualSelected) {
            viewModel.onPermissionGranted()
            activity?.let {
                app.photoRepository.startObservingMediaStore(it.lifecycleScope)
            }
        } else {
            try {
                mediaPermissionLauncher.launch(requiredMediaPermissions)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Launch media permission failed", e)
            }
        }
    }

    MainScreen(viewModel = viewModel)
}
