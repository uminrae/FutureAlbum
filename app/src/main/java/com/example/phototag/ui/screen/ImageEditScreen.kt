package com.example.phototag.ui.screen

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import com.example.phototag.ui.components.bounceScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import com.example.phototag.util.HapticFeedbackUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.mutableStateListOf

import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.phototag.util.editor.ColorAdjustmentParams
import com.example.phototag.util.editor.ColorGradingEngine
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.util.editor.CropAspectRatio
import com.example.phototag.util.editor.DazzCategory
import com.example.phototag.util.editor.DazzFilmEngine
import com.example.phototag.util.editor.DazzFilmPreset
import com.example.phototag.util.editor.FilmFilterEngine
import com.example.phototag.util.editor.FilmPreset
import com.example.phototag.util.editor.ImageEditorEngine
import com.example.phototag.util.editor.MosaicPoint
import com.example.phototag.util.editor.MosaicStroke
import com.example.phototag.util.editor.RetroDotMatrixRenderer
import com.example.phototag.util.editor.WatermarkMetadata
import com.example.phototag.util.editor.WatermarkPosition
import com.example.phototag.util.editor.WatermarkRenderer
import com.example.phototag.util.editor.WatermarkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.max
import kotlin.math.min

import com.example.phototag.util.editor.CameraSoundPlayer

/**
 * 编辑功能大类
 */
private enum class EditTab(val label: String) {
    CROP("构图裁剪"),
    FILTER("胶片相机"),
    ADJUST("专业调色"),
    WATERMARK("复古时间戳"),
    MOSAIC("隐私打码")
}

/**
 * 专业调色参数维度
 */
enum class AdjustParamType(
    val label: String,
    val minVal: Float,
    val maxVal: Float,
    val isBipolar: Boolean
) {
    EXPOSURE("曝光", -100f, 100f, true),
    CONTRAST("对比度", -100f, 100f, true),
    HIGHLIGHTS("高光", -100f, 100f, true),
    SHADOWS("阴影", -100f, 100f, true),
    SATURATION("饱和度", -100f, 100f, true),
    WARMTH("色温", -100f, 100f, true),
    TINT("色调", -100f, 100f, true),
    VIGNETTE("暗角", 0f, 100f, false),
    SHARPNESS("锐度", 0f, 100f, false),
    BRIGHTNESS("明度", -100f, 100f, true)
}

/**
 * 编辑历史快照状态（支持多级撤销/重做）
 */
data class EditStateSnapshot(
    val selectedRatio: CropAspectRatio,
    val rotationDegrees: Int,
    val flipHorizontal: Boolean,
    val cropRectNorm: RectF,
    val selectedDazzPresetId: String? = null,
    val filterIntensity: Float,
    val filterEnableGrain: Boolean,
    val filterEnableVignette: Boolean,
    val filterEnableLightLeak: Boolean = false,
    val adjustmentParams: ColorAdjustmentParams,
    val watermarkType: WatermarkType,
    val watermarkPattern: String = "''yy年  M月d日",
    val watermarkPosition: WatermarkPosition,
    val mosaicStrokes: List<MosaicStroke> = emptyList()
)

@Composable
fun ImageEditScreen(
    photo: PhotoEntity,
    onDismiss: () -> Unit,
    onSaveSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // 编辑模式 Tab
    var currentTab by remember { mutableStateOf(EditTab.CROP) }

    // 裁剪与旋转状态
    var selectedRatio by remember { mutableStateOf(CropAspectRatio.FREE) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var cropRectNorm by remember { mutableStateOf(RectF(0f, 0f, 1f, 1f)) }

    // 🎞️ DAZZ 67款 3D HALD LUT 胶片滤镜状态
    var selectedDazzPreset by remember { mutableStateOf<DazzFilmPreset?>(null) }
    var selectedDazzCategory by remember { mutableStateOf(DazzCategory.ALL) }
    var filterIntensity by remember { mutableFloatStateOf(1.0f) }
    var filterEnableGrain by remember { mutableStateOf(false) }
    var filterEnableVignette by remember { mutableStateOf(false) }
    var filterEnableLightLeak by remember { mutableStateOf(false) }
    var previewFilteredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingFilterPreview by remember { mutableStateOf(false) }
    var isInteractingSlider by remember { mutableStateOf(false) }

    // 🏷️ DAZZ 官方复古时间戳状态 (统一遵循原版 Dazz 规范：默认右下角，格式 ''yy MM dd)
    var watermarkType by remember { mutableStateOf(WatermarkType.NONE) }
    var watermarkPattern by remember { mutableStateOf("''yy MM dd") }
    var watermarkPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }

    // 🎨 专业调色状态
    var adjustmentParams by remember { mutableStateOf(ColorAdjustmentParams()) }
    var activeAdjustParam by remember { mutableStateOf(AdjustParamType.EXPOSURE) }

    // 🧱 隐私打码状态
    val mosaicStrokes = remember { mutableStateListOf<MosaicStroke>() }
    var mosaicBrushRadiusNorm by remember { mutableFloatStateOf(0.035f) }

    val hasModifications = selectedDazzPreset != null || !adjustmentParams.isDefault || filterEnableGrain || filterEnableVignette || filterEnableLightLeak || watermarkType != WatermarkType.NONE || mosaicStrokes.isNotEmpty()

    // 历史状态栈（支持多级撤销 / 重做）
    val undoStack = remember { mutableStateListOf<EditStateSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditStateSnapshot>() }

    fun captureSnapshot(): EditStateSnapshot = EditStateSnapshot(
        selectedRatio = selectedRatio,
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        cropRectNorm = RectF(cropRectNorm),
        selectedDazzPresetId = selectedDazzPreset?.id,
        filterIntensity = filterIntensity,
        filterEnableGrain = filterEnableGrain,
        filterEnableVignette = filterEnableVignette,
        filterEnableLightLeak = filterEnableLightLeak,
        adjustmentParams = adjustmentParams,
        watermarkType = watermarkType,
        watermarkPattern = watermarkPattern,
        watermarkPosition = watermarkPosition,
        mosaicStrokes = mosaicStrokes.toList()
    )

    fun applySnapshot(snapshot: EditStateSnapshot) {
        selectedRatio = snapshot.selectedRatio
        rotationDegrees = snapshot.rotationDegrees
        flipHorizontal = snapshot.flipHorizontal
        cropRectNorm = RectF(snapshot.cropRectNorm)
        selectedDazzPreset = snapshot.selectedDazzPresetId?.let { DazzFilmEngine.findPresetById(it) }
        filterIntensity = snapshot.filterIntensity
        filterEnableGrain = snapshot.filterEnableGrain
        filterEnableVignette = snapshot.filterEnableVignette
        filterEnableLightLeak = snapshot.filterEnableLightLeak
        adjustmentParams = snapshot.adjustmentParams
        watermarkType = snapshot.watermarkType
        watermarkPattern = snapshot.watermarkPattern
        watermarkPosition = snapshot.watermarkPosition
        mosaicStrokes.clear()
        mosaicStrokes.addAll(snapshot.mosaicStrokes)
    }

    fun pushState() {
        undoStack.add(captureSnapshot())
        redoStack.clear()
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
    }

    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val currentState = captureSnapshot()
            redoStack.add(currentState)
            val previousState = undoStack.removeAt(undoStack.lastIndex)
            applySnapshot(previousState)
            HapticFeedbackUtil.click(context)
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val currentState = captureSnapshot()
            undoStack.add(currentState)
            val nextState = redoStack.removeAt(redoStack.lastIndex)
            applySnapshot(nextState)
            HapticFeedbackUtil.click(context)
        }
    }

    // 加载轻量级预览位图 (1280px 限制，内存仅 ~4MB，保证 60fps 实时流畅且彻底杜绝 OOM)
    LaunchedEffect(photo.uri) {
        isLoadingBitmap = true
        val bmp = ImageEditorEngine.loadPreviewBitmapWithCorrectOrientation(context, Uri.parse(photo.uri), 1280)
            ?: ImageEditorEngine.loadBitmapWithCorrectOrientation(context, Uri.parse(photo.uri))
        sourceBitmap = bmp
        if (bmp != null) {
            watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
        }
        isLoadingBitmap = false
    }

    // 响应式物理变换后底图（旋转与翻转即时生效）
    val transformedBitmap = remember(sourceBitmap, rotationDegrees, flipHorizontal) {
        val raw = sourceBitmap ?: return@remember null
        ImageEditorEngine.transformBitmap(raw, rotationDegrees, flipHorizontal)
    }

    // 🎯 当前激活的实时裁切位图（使胶片滤镜与水印时间戳在选定裁切比例后 100% 作用于裁切后构图）
    val activeCroppedBitmap = remember(transformedBitmap, cropRectNorm) {
        val raw = transformedBitmap ?: return@remember null
        val isDefaultFull = cropRectNorm.left <= 0.001f && cropRectNorm.top <= 0.001f &&
                cropRectNorm.right >= 0.999f && cropRectNorm.bottom >= 0.999f
        if (isDefaultFull) raw else ImageEditorEngine.cropBitmap(raw, cropRectNorm)
    }

    // 根据选中的长宽比更新裁切框
    fun applyAspectRatio(aspectRatio: CropAspectRatio) {
        selectedRatio = aspectRatio
        val bmp = transformedBitmap ?: return
        val targetRatio = aspectRatio.ratio

        if (targetRatio == null) {
            // 自由模式：若用户已有裁切范围则保持当前范围，允许直接在当前构图上自由调节各边
            if (cropRectNorm.width() <= 0f || cropRectNorm.height() <= 0f) {
                cropRectNorm = RectF(0f, 0f, 1f, 1f)
            }
            return
        }

        val effectiveW = bmp.width.toFloat()
        val effectiveH = bmp.height.toFloat()
        val currentImgRatio = effectiveW / effectiveH

        if (currentImgRatio > targetRatio) {
            // 图片比目标比例更宽，裁切左右
            val newNormW = (targetRatio / currentImgRatio).coerceIn(0.1f, 1.0f)
            val left = (1f - newNormW) / 2f
            cropRectNorm = RectF(left, 0f, left + newNormW, 1f)
        } else {
            // 图片比目标比例更高，裁切上下
            val newNormH = (currentImgRatio / targetRatio).coerceIn(0.1f, 1.0f)
            val top = (1f - newNormH) / 2f
            cropRectNorm = RectF(0f, top, 1f, top + newNormH)
        }
    }

    // 当旋转/翻转触发时，若当前选有固定比例（如 4:5），自动适配新构图
    LaunchedEffect(rotationDegrees, flipHorizontal) {
        if (transformedBitmap != null && selectedRatio != CropAspectRatio.FREE) {
            applyAspectRatio(selectedRatio)
        }
    }

    // 实时复合调色与胶片滤镜渲染（双轨极速架构：滑杆拖动 30ms 节流 360p 极速查表稳锁 120fps，松手 80ms 防抖高清渲染）
    LaunchedEffect(
        activeCroppedBitmap,
        selectedDazzPreset,
        filterIntensity,
        filterEnableGrain,
        filterEnableVignette,
        filterEnableLightLeak,
        adjustmentParams,
        isInteractingSlider
    ) {
        val bmp = activeCroppedBitmap ?: return@LaunchedEffect
        val hasAdjustments = !adjustmentParams.isDefault
        val hasDazz = selectedDazzPreset != null

        if (hasAdjustments || hasDazz || filterEnableLightLeak || filterEnableGrain || filterEnableVignette) {
            val maxPreviewDim = if (isInteractingSlider) 320 else 960
            if (isInteractingSlider) {
                kotlinx.coroutines.delay(35)
            } else {
                kotlinx.coroutines.delay(70)
            }
            isRenderingFilterPreview = true
            val maxActual = max(bmp.width, bmp.height)
            val previewSource = if (maxActual > maxPreviewDim) {
                val scale = maxPreviewDim.toFloat() / maxActual
                Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * scale).toInt().coerceAtLeast(1),
                    (bmp.height * scale).toInt().coerceAtLeast(1),
                    false
                )
            } else {
                bmp
            }

            // 1. 应用专业调色
            val adjusted = if (hasAdjustments) {
                ColorGradingEngine.applyAdjustments(previewSource, adjustmentParams)
            } else {
                previewSource
            }

            // 2. 全量应用 DAZZ 3D HALD LUT 胶片调色 (滑动滑杆期间跳过重型噪点/漏光以锁定 120fps，松手即刻无损叠加)
            val useGrain = if (isInteractingSlider) false else filterEnableGrain
            val useVignette = if (isInteractingSlider) false else filterEnableVignette
            val useLeak = if (isInteractingSlider) false else filterEnableLightLeak

            val filtered = if (hasDazz) {
                DazzFilmEngine.applyFilmColorGrading(
                    context = context,
                    source = adjusted,
                    lutFileName = selectedDazzPreset!!.lutFileName,
                    intensity = filterIntensity,
                    enableGrain = useGrain,
                    enableVignette = useVignette,
                    enableLightLeak = useLeak
                )
            } else if (useLeak || useGrain || useVignette) {
                DazzFilmEngine.applyFilmColorGrading(
                    context = context,
                    source = adjusted,
                    lutFileName = "LookupTableDefault.png",
                    intensity = 0f,
                    enableGrain = useGrain,
                    enableVignette = useVignette,
                    enableLightLeak = useLeak
                )
            } else {
                adjusted
            }

            // 及时回收中间态位图，消除内存抖动与 GC 掉帧
            if (previewSource !== bmp) {
                previewSource.recycle()
            }
            if (adjusted !== previewSource && adjusted !== bmp && adjusted !== filtered) {
                adjusted.recycle()
            }

            val oldPreview = previewFilteredBitmap
            previewFilteredBitmap = filtered
            if (oldPreview != null && oldPreview !== bmp && oldPreview !== filtered) {
                oldPreview.recycle()
            }
            isRenderingFilterPreview = false
        } else {
            val oldPreview = previewFilteredBitmap
            previewFilteredBitmap = null
            if (oldPreview != null && oldPreview !== bmp) {
                oldPreview.recycle()
            }
            isRenderingFilterPreview = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        if (isLoadingBitmap || sourceBitmap == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("正在载入图片...", color = Color(0xFF8E8E93), fontSize = 13.sp)
            }
        } else {
            val bitmap = transformedBitmap ?: sourceBitmap!!

            Column(modifier = Modifier.fillMaxSize()) {
                // ==================== 顶部导航操作栏 (iOS 经典相册暗夜工作室风格) ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮 (iOS 纯正经典文字按钮)
                    Text(
                        text = "取消",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .bounceScale()
                            .clickable { onDismiss() }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    // 撤销与重做微岛 (iOS 毛玻璃悬浮微岛)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x331C1C1E),
                        border = BorderStroke(0.5.dp, Color(0x2AFFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.example.phototag.ui.components.IosIconButton(
                                icon = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "撤销",
                                tint = if (undoStack.isNotEmpty()) Color.White else Color(0x44FFFFFF),
                                enabled = undoStack.isNotEmpty(),
                                onClick = { performUndo() },
                                size = 30.dp
                            )
                            Spacer(Modifier.width(4.dp))
                            com.example.phototag.ui.components.IosIconButton(
                                icon = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "重做",
                                tint = if (redoStack.isNotEmpty()) Color.White else Color(0x44FFFFFF),
                                enabled = redoStack.isNotEmpty(),
                                onClick = { performRedo() },
                                size = 30.dp
                            )
                        }
                    }

                    // 完成导出按钮 (iOS 经典琥珀金色操作胶囊)
                    com.example.phototag.ui.components.IosButton(
                        text = if (isSaving) "正在导出..." else "导出",
                        icon = null,
                        variant = com.example.phototag.ui.components.IosButtonVariant.AMBER,
                        isLoading = isSaving,
                        enabled = !isSaving,
                        height = 34.dp,
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        // 1. 读取原图高精度全尺寸
                                        val fullRaw = ImageEditorEngine.loadBitmapWithCorrectOrientation(context, Uri.parse(photo.uri))
                                            ?: (transformedBitmap ?: bitmap)

                                        // 2. 几何变换 (旋转与镜像)
                                        val fullTransformed = ImageEditorEngine.transformBitmap(fullRaw, rotationDegrees, flipHorizontal)
                                        if (fullTransformed !== fullRaw && fullRaw !== bitmap) {
                                            fullRaw.recycle()
                                        }

                                        // 3. 裁剪
                                        val fullCropped = ImageEditorEngine.cropBitmap(fullTransformed, cropRectNorm)
                                        if (fullCropped !== fullTransformed && fullTransformed !== bitmap) {
                                            fullTransformed.recycle()
                                        }

                                        // 4. 全精度应用专业调色
                                        val fullGraded = if (!adjustmentParams.isDefault) {
                                            ColorGradingEngine.applyAdjustments(fullCropped, adjustmentParams)
                                        } else {
                                            fullCropped
                                        }
                                        if (fullGraded !== fullCropped && fullCropped !== bitmap) {
                                            fullCropped.recycle()
                                        }

                                        // 5. 全量无损应用 DAZZ 3D HALD LUT 胶片调色
                                        val fullFiltered = if (selectedDazzPreset != null) {
                                            DazzFilmEngine.applyFilmColorGradingSync(
                                                context = context,
                                                source = fullGraded,
                                                lutFileName = selectedDazzPreset!!.lutFileName,
                                                intensity = filterIntensity,
                                                enableGrain = filterEnableGrain,
                                                enableVignette = filterEnableVignette,
                                                enableLightLeak = filterEnableLightLeak
                                            )
                                        } else if (filterEnableLightLeak || filterEnableGrain || filterEnableVignette) {
                                            DazzFilmEngine.applyFilmColorGradingSync(
                                                context = context,
                                                source = fullGraded,
                                                lutFileName = "LookupTableDefault.png",
                                                intensity = 0f,
                                                enableGrain = filterEnableGrain,
                                                enableVignette = filterEnableVignette,
                                                enableLightLeak = filterEnableLightLeak
                                            )
                                        } else {
                                            fullGraded
                                        }
                                        if (fullFiltered !== fullGraded && fullGraded !== bitmap) {
                                            fullGraded.recycle()
                                        }

                                        // 5.5 应用隐私打码
                                        val fullMosaiced = if (mosaicStrokes.isNotEmpty()) {
                                            ImageEditorEngine.applyMosaic(fullFiltered, mosaicStrokes)
                                        } else {
                                            fullFiltered
                                        }
                                        if (fullMosaiced !== fullFiltered && fullFiltered !== bitmap) {
                                            fullFiltered.recycle()
                                        }

                                        // 6. 注入 DAZZ 官方复古时间戳 (严格遵循时间戳实现指南.md 1:1 规范)
                                        val saveTimeMs = if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L
                                        val metadata = WatermarkMetadata(
                                            dateTaken = Date(saveTimeMs),
                                            locationName = photo.address ?: photo.customLocation,
                                            pattern = watermarkPattern,
                                            position = watermarkPosition
                                        )
                                        val fullWatermarked = WatermarkRenderer.applyWatermark(
                                            source = fullMosaiced,
                                            type = watermarkType,
                                            metadata = metadata,
                                            context = context
                                        )
                                        if (fullWatermarked !== fullMosaiced && fullMosaiced !== bitmap) {
                                            fullMosaiced.recycle()
                                        }

                                        // 7. 高质量保存至系统相册并完整继承 EXIF 元数据
                                        val newUri = ImageEditorEngine.saveEditedPhoto(
                                            context = context,
                                            originalPhoto = photo,
                                            finalBitmap = fullWatermarked
                                        )
                                        if (fullWatermarked !== bitmap) {
                                            fullWatermarked.recycle()
                                        }

                                        withContext(Dispatchers.Main) {
                                            if (newUri != null) {
                                                CameraSoundPlayer.playEnd(context)
                                                Toast.makeText(context, "已保存高清副本至系统相册", Toast.LENGTH_SHORT).show()
                                                onSaveSuccess(newUri)
                                            } else {
                                                Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } catch (e: Throwable) {
                                    android.util.Log.e("ImageEdit", "Save failed", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "导出失败: ${e.message ?: "未知异常"}", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    )
                }

                // ==================== 中部可交互画板区域 ====================
                var isComparingOriginal by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .pointerInput(currentTab, hasModifications) {
                            if (currentTab != EditTab.CROP && currentTab != EditTab.MOSAIC && hasModifications) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    isComparingOriginal = true
                                    HapticFeedbackUtil.click(context)
                                    waitForUpOrCancellation()
                                    isComparingOriginal = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val baseBmp = transformedBitmap ?: bitmap
                    val displayBitmap = if (!isComparingOriginal && hasModifications && previewFilteredBitmap != null) {
                        previewFilteredBitmap!!
                    } else {
                        if (currentTab == EditTab.CROP) baseBmp else (activeCroppedBitmap ?: baseBmp)
                    }

                    EditorCanvas(
                        bitmap = if (currentTab == EditTab.CROP) baseBmp else displayBitmap,
                        cropRectNorm = cropRectNorm,
                        selectedRatio = selectedRatio,
                        isCropMode = (currentTab == EditTab.CROP),
                        isMosaicMode = (currentTab == EditTab.MOSAIC),
                        mosaicStrokes = mosaicStrokes,
                        mosaicBrushRadiusNorm = mosaicBrushRadiusNorm,
                        onAddMosaicStroke = { stroke ->
                            pushState()
                            mosaicStrokes.add(stroke)
                        },
                        watermarkType = watermarkType,
                        watermarkPosition = watermarkPosition,
                        watermarkPattern = watermarkPattern,
                        photoDate = Date(if (photo.dateAdded > 100000000000L) photo.dateAdded else photo.dateAdded * 1000L),
                        photoAddress = photo.address ?: photo.customLocation,
                        onCropRectChanged = { newRect ->
                            pushState()
                            cropRectNorm = newRect
                        }
                    )

                    // ⚡ 长按对比原图浮动按钮 (右上角常驻提示)
                    if (hasModifications && currentTab != EditTab.CROP) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x991C1C1E),
                            border = BorderStroke(0.5.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        isComparingOriginal = true
                                        HapticFeedbackUtil.click(context)
                                        waitForUpOrCancellation()
                                        isComparingOriginal = false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB340),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isComparingOriginal) "松开恢复" else "长按对比",
                                    color = if (isComparingOriginal) Color(0xFFFFD180) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // ⚡ 长按对比原图指示浮动胶囊
                    if (isComparingOriginal) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xCC000000),
                            border = BorderStroke(0.5.dp, Color(0x33FFFFFF)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = "正在对比原图...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // ==================== 底部模式切换与工具面板 (iOS 沉浸式暗夜毛玻璃胶囊) ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF000000))
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp)
                ) {
                    // iOS 悬浮分段控制器 (Segmented Control)
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0x661C1C1E),
                        border = BorderStroke(0.5.dp, Color(0x26FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val tabs = listOf(
                                Triple(EditTab.CROP, "构图", Icons.Filled.Crop),
                                Triple(EditTab.FILTER, "滤镜", Icons.Filled.CameraAlt),
                                Triple(EditTab.ADJUST, "调节", Icons.Filled.Tune),
                                Triple(EditTab.WATERMARK, "水印", Icons.Filled.AutoFixHigh),
                                Triple(EditTab.MOSAIC, "打码", Icons.Filled.Details)
                            )

                            tabs.forEach { (tab, title, icon) ->
                                val isSelected = (currentTab == tab)
                                val isTabActive = when (tab) {
                                    EditTab.FILTER -> selectedDazzPreset != null || filterEnableLightLeak || filterEnableGrain || filterEnableVignette
                                    EditTab.ADJUST -> !adjustmentParams.isDefault
                                    EditTab.WATERMARK -> watermarkType != WatermarkType.NONE
                                    EditTab.MOSAIC -> mosaicStrokes.isNotEmpty()
                                    else -> false
                                }

                                val pillBg by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else Color.Transparent,
                                    animationSpec = spring<Color>(stiffness = Spring.StiffnessMediumLow),
                                    label = "edit_tab_pill"
                                )

                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = pillBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(22.dp))
                                        .bounceScale()
                                        .clickable {
                                            currentTab = tab
                                            HapticFeedbackUtil.click(context)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Black else if (isTabActive) Color(0xFFFF9F0A) else Color(0xFF8E8E93),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = title,
                                            color = if (isSelected) Color.Black else if (isTabActive) Color(0xFFFFB74D) else Color(0xFF8E8E93),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.5.sp
                                        )
                                        if (isTabActive && !isSelected) {
                                            Spacer(Modifier.width(3.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(Color(0xFFFF9F0A), CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 选项面板 (iOS 物理弹簧平滑切换)
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(180)) { it / 8 })
                                .togetherWith(fadeOut(animationSpec = tween(120)))
                        },
                        label = "EditTabPanelTransition"
                    ) { tab ->
                        when (tab) {
                            EditTab.CROP -> {
                            CropControlPanel(
                                selectedRatio = selectedRatio,
                                onSelectRatio = {
                                    pushState()
                                    applyAspectRatio(it)
                                },
                                onRotate = {
                                    pushState()
                                    rotationDegrees = (rotationDegrees + 90) % 360
                                },
                                onFlip = {
                                    pushState()
                                    flipHorizontal = !flipHorizontal
                                },
                                onReset = {
                                    pushState()
                                    rotationDegrees = 0
                                    flipHorizontal = false
                                    applyAspectRatio(CropAspectRatio.FREE)
                                }
                            )
                        }
                        EditTab.FILTER -> {
                            FilterControlPanel(
                                selectedPreset = selectedDazzPreset,
                                onSelectPreset = { preset ->
                                    if (selectedDazzPreset != preset) {
                                        pushState()
                                        selectedDazzPreset = preset
                                        CameraSoundPlayer.playShutter(context)
                                    }
                                },
                                selectedCategory = selectedDazzCategory,
                                onSelectCategory = { selectedDazzCategory = it },
                                intensity = filterIntensity,
                                onIntensityChange = { filterIntensity = it },
                                onIntensityChangeStart = { isInteractingSlider = true },
                                onIntensityChangeFinished = {
                                    isInteractingSlider = false
                                    pushState()
                                },
                                enableGrain = filterEnableGrain,
                                onToggleGrain = {
                                    pushState()
                                    filterEnableGrain = !filterEnableGrain
                                },
                                enableVignette = filterEnableVignette,
                                onToggleVignette = {
                                    pushState()
                                    filterEnableVignette = !filterEnableVignette
                                },
                                enableLightLeak = filterEnableLightLeak,
                                onToggleLightLeak = {
                                    pushState()
                                    filterEnableLightLeak = !filterEnableLightLeak
                                }
                            )
                        }
                        EditTab.ADJUST -> {
                            AdjustControlPanel(
                                params = adjustmentParams,
                                activeParam = activeAdjustParam,
                                onSelectParam = { activeAdjustParam = it },
                                onParamValueChange = { param, value ->
                                    adjustmentParams = when (param) {
                                        AdjustParamType.EXPOSURE -> adjustmentParams.copy(exposure = value)
                                        AdjustParamType.CONTRAST -> adjustmentParams.copy(contrast = value)
                                        AdjustParamType.HIGHLIGHTS -> adjustmentParams.copy(highlights = value)
                                        AdjustParamType.SHADOWS -> adjustmentParams.copy(shadows = value)
                                        AdjustParamType.SATURATION -> adjustmentParams.copy(saturation = value)
                                        AdjustParamType.WARMTH -> adjustmentParams.copy(warmth = value)
                                        AdjustParamType.TINT -> adjustmentParams.copy(tint = value)
                                        AdjustParamType.VIGNETTE -> adjustmentParams.copy(vignette = value)
                                        AdjustParamType.SHARPNESS -> adjustmentParams.copy(sharpness = value)
                                        AdjustParamType.BRIGHTNESS -> adjustmentParams.copy(brightness = value)
                                    }
                                },
                                onParamValueChangeStart = {
                                    isInteractingSlider = true
                                },
                                onParamChangeFinished = {
                                    isInteractingSlider = false
                                    pushState()
                                },
                                onResetParam = { param ->
                                    pushState()
                                    adjustmentParams = when (param) {
                                        AdjustParamType.EXPOSURE -> adjustmentParams.copy(exposure = 0f)
                                        AdjustParamType.CONTRAST -> adjustmentParams.copy(contrast = 0f)
                                        AdjustParamType.HIGHLIGHTS -> adjustmentParams.copy(highlights = 0f)
                                        AdjustParamType.SHADOWS -> adjustmentParams.copy(shadows = 0f)
                                        AdjustParamType.SATURATION -> adjustmentParams.copy(saturation = 0f)
                                        AdjustParamType.WARMTH -> adjustmentParams.copy(warmth = 0f)
                                        AdjustParamType.TINT -> adjustmentParams.copy(tint = 0f)
                                        AdjustParamType.VIGNETTE -> adjustmentParams.copy(vignette = 0f)
                                        AdjustParamType.SHARPNESS -> adjustmentParams.copy(sharpness = 0f)
                                        AdjustParamType.BRIGHTNESS -> adjustmentParams.copy(brightness = 0f)
                                    }
                                },
                                onResetAll = {
                                    pushState()
                                    adjustmentParams = ColorAdjustmentParams()
                                }
                            )
                        }
                        EditTab.WATERMARK -> {
                            TimestampControlPanel(
                                selectedType = watermarkType,
                                onSelectType = {
                                    if (watermarkType != it) {
                                        runCatching { pushState() }
                                        watermarkType = it
                                        when (it) {
                                            WatermarkType.DAZZ_VCR_TILTED -> {
                                                watermarkPattern = "''yy MM dd"
                                                watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                            }
                                            WatermarkType.DAZZ_VCR_ORANGE -> {
                                                watermarkPattern = "''yy MM dd"
                                                watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                            }
                                            WatermarkType.DAZZ_LCD_RED, WatermarkType.DAZZ_LCD_GREEN -> {
                                                watermarkPattern = "''yy MM dd"
                                                watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                            }
                                            WatermarkType.DAZZ_VHS -> {
                                                watermarkPattern = "yyyy.MM.dd HH:mm"
                                                watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                            }
                                            WatermarkType.DAZZ_DEFAULT_YELLOW -> {
                                                watermarkPattern = "''yy MM dd"
                                                watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                            }
                                            WatermarkType.DAZZ_CPM35_DATE -> {
                                                watermarkPattern = "''yy MM dd"
                                                watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                            }
                                            else -> {
                                                watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                            }
                                        }
                                        runCatching { CameraSoundPlayer.playSlideClick(context) }
                                    }
                                },
                                selectedPosition = watermarkPosition,
                                onSelectPosition = {
                                    if (watermarkPosition != it) {
                                        runCatching { pushState() }
                                        watermarkPosition = it
                                        runCatching { CameraSoundPlayer.playSlideClick(context) }
                                    }
                                },
                                selectedPattern = watermarkPattern,
                                onSelectPattern = {
                                    if (watermarkPattern != it) {
                                        runCatching { pushState() }
                                        watermarkPattern = it
                                        runCatching { CameraSoundPlayer.playSlideClick(context) }
                                    }
                                }
                            )
                        }
                        EditTab.MOSAIC -> {
                            MosaicControlPanel(
                                selectedRadiusNorm = mosaicBrushRadiusNorm,
                                onSelectRadiusNorm = { mosaicBrushRadiusNorm = it },
                                strokeCount = mosaicStrokes.size,
                                onUndoStroke = {
                                    if (mosaicStrokes.isNotEmpty()) {
                                        pushState()
                                        mosaicStrokes.removeAt(mosaicStrokes.lastIndex)
                                        HapticFeedbackUtil.click(context)
                                    }
                                },
                                onClearStrokes = {
                                    if (mosaicStrokes.isNotEmpty()) {
                                        pushState()
                                        mosaicStrokes.clear()
                                        HapticFeedbackUtil.click(context)
                                    }
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

/**
 * 🎞️ DAZZ 全量 67 款胶片滤镜控制面板 (大类筛选 + 3D HALD LUT 实时查表 + 颗粒/暗角/漏光微调)
 */
@Composable
private fun FilterControlPanel(
    selectedPreset: DazzFilmPreset?,
    onSelectPreset: (DazzFilmPreset?) -> Unit,
    selectedCategory: DazzCategory,
    onSelectCategory: (DazzCategory) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    onIntensityChangeStart: () -> Unit = {},
    onIntensityChangeFinished: () -> Unit = {},
    enableGrain: Boolean,
    onToggleGrain: () -> Unit,
    enableVignette: Boolean,
    onToggleVignette: () -> Unit,
    enableLightLeak: Boolean,
    onToggleLightLeak: () -> Unit
) {
    val context = LocalContext.current
    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == DazzCategory.ALL) {
            DazzFilmEngine.ALL_PRESETS
        } else {
            DazzFilmEngine.ALL_PRESETS.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // 1. 分类选择胶囊栏 (全部 / 经典胶片 / 拍立得 / 复古数码 / 电影复古 / 名机名作 / 艺术影调)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DazzCategory.values().forEach { category ->
                val isSelected = (selectedCategory == category)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFFFF9F0A) else Color(0x262C2C2E),
                    border = BorderStroke(0.5.dp, if (isSelected) Color(0xFFFFB340) else Color(0x22FFFFFF)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onSelectCategory(category)
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Text(
                        text = category.label,
                        color = if (isSelected) Color.Black else Color(0xFFD1D1D6),
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 2. 胶片滤镜横向滚动卡片组 (涵盖 67 款真实 3D HALD LUT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // RAW 原图直出卡片
            val isRawSelected = (selectedPreset == null)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isRawSelected) Color(0x33FFB340) else Color(0x261C1C1E),
                border = BorderStroke(
                    width = if (isRawSelected) 1.5.dp else 0.5.dp,
                    color = if (isRawSelected) Color(0xFFFFB340) else Color(0x22FFFFFF)
                ),
                modifier = Modifier
                    .width(88.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onSelectPreset(null)
                        HapticFeedbackUtil.tick(context)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ORIGINAL",
                        color = if (isRawSelected) Color(0xFFFFD180) else Color(0xFF8E8E93),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "原图直出",
                        color = if (isRawSelected) Color.White else Color(0xFFE5E5EA),
                        fontSize = 12.sp,
                        fontWeight = if (isRawSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                    Text(
                        text = "RAW",
                        color = if (isRawSelected) Color(0xFFFFCA7A) else Color(0xFF8E8E93),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 全部 67 款 DAZZ 滤镜预设卡片
            filteredPresets.forEach { preset ->
                val isSelected = (selectedPreset?.id == preset.id)
                val toneColor = try {
                    Color(android.graphics.Color.parseColor(preset.colorTag))
                } catch (_: Exception) {
                    Color(0xFFFFB340)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0x33FFB340) else Color(0x261C1C1E),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) Color(0xFFFFB340) else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .width(92.dp)
                        .height(82.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onSelectPreset(preset)
                            HapticFeedbackUtil.tick(context)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 7.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 顶部风格色调小标
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(toneColor, CircleShape)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = preset.nameEn.take(10),
                                color = if (isSelected) Color(0xFFFFD180) else Color(0xFF8E8E93),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                letterSpacing = 0.3.sp
                            )
                        }

                        // 居中预设主名称
                        Text(
                            text = preset.nameZh.replace(" 经典胶片", "").replace(" 复古数码", ""),
                            color = if (isSelected) Color.White else Color(0xFFE5E5EA),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1
                        )

                        // 底部副标
                        Text(
                            text = preset.category.label,
                            color = if (isSelected) Color(0xFFFFCA7A) else Color(0xFF8E8E93),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 3. 当选中任意胶片预设时，展示专业级微调滑块与颗粒/暗角/漏光开关
        if (selectedPreset != null) {
            Spacer(Modifier.height(8.dp))

            // 滤镜浓度滑块与数值胶囊 (iOS 18 风格)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "滤镜浓度",
                    color = Color(0xFFE5E5EA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x332C2C2E),
                    border = BorderStroke(0.5.dp, Color(0x22FFFFFF))
                ) {
                    Text(
                        text = "${(intensity * 100).toInt()}%",
                        color = Color(0xFFFFB340),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Slider(
                value = intensity,
                onValueChange = {
                    onIntensityChangeStart()
                    onIntensityChange(it)
                },
                onValueChangeFinished = onIntensityChangeFinished,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFFFB340),
                    inactiveTrackColor = Color(0x332C2C2E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            // 特效开关微调胶囊 (35mm胶片颗粒 / 光学暗角 / 随机漏光)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 颗粒
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (enableGrain) Color(0x33FF9F0A) else Color(0x262C2C2E),
                    border = BorderStroke(0.5.dp, if (enableGrain) Color(0xFFFF9F0A) else Color(0x22FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .bounceScale()
                        .clickable {
                            onToggleGrain()
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (enableGrain) "颗粒 ON" else "胶片颗粒",
                            color = if (enableGrain) Color(0xFFFFB340) else Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            fontWeight = if (enableGrain) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // 暗角
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (enableVignette) Color(0x33FF9F0A) else Color(0x262C2C2E),
                    border = BorderStroke(0.5.dp, if (enableVignette) Color(0xFFFF9F0A) else Color(0x22FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .bounceScale()
                        .clickable {
                            onToggleVignette()
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (enableVignette) "暗角 ON" else "镜头暗角",
                            color = if (enableVignette) Color(0xFFFFB340) else Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            fontWeight = if (enableVignette) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // 漏光
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (enableLightLeak) Color(0x33FF9F0A) else Color(0x262C2C2E),
                    border = BorderStroke(0.5.dp, if (enableLightLeak) Color(0xFFFF9F0A) else Color(0x22FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .bounceScale()
                        .clickable {
                            onToggleLightLeak()
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (enableLightLeak) "漏光 ON" else "复古漏光",
                            color = if (enableLightLeak) Color(0xFFFFB340) else Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            fontWeight = if (enableLightLeak) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}



/**
 * 🎛️ 触感刻度滑块 (带 0 点磁吸吸附、整十步进震动触感反馈与一键归零)
 */
@Composable
private fun HapticDialSlider(
    param: AdjustParamType,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeStart: () -> Unit = {},
    onValueChangeFinished: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var lastHapticStep by remember(param) { mutableIntStateOf((value / 10f).toInt()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // 参数名、数值胶囊与快捷归零
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = param.label,
                    color = Color(0xFFE5E5EA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (value != 0f) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "归零",
                        color = Color(0xFFFF9F0A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x26FF9F0A))
                            .clickable {
                                onReset()
                                HapticFeedbackUtil.click(context)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 数值指示胶囊
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (value != 0f) Color(0x33FF9F0A) else Color(0x262C2C2E),
                border = BorderStroke(0.5.dp, if (value != 0f) Color(0x80FF9F0A) else Color(0x22FFFFFF)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (value != 0f) {
                            onReset()
                            HapticFeedbackUtil.click(context)
                        }
                    }
            ) {
                val valueStr = if (param.isBipolar) {
                    if (value > 0) "+${value.toInt()}" else "${value.toInt()}"
                } else {
                    "${value.toInt()}"
                }
                Text(
                    text = valueStr,
                    color = if (value != 0f) Color(0xFFFFB340) else Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // 磁吸刻度滑块
        Slider(
            value = value,
            onValueChange = { rawVal ->
                onValueChangeStart()
                // 磁吸吸附零点：在 [-2.5, +2.5] 之间自动磁吸归零
                val snapped = if (param.isBipolar && kotlin.math.abs(rawVal) < 2.5f) {
                    0f
                } else {
                    rawVal
                }

                val currentStep = (snapped / 10f).toInt()
                if (currentStep != lastHapticStep) {
                    lastHapticStep = currentStep
                    HapticFeedbackUtil.tick(context)
                }

                onValueChange(snapped)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = param.minVal..param.maxVal,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFFFF9F0A),
                inactiveTrackColor = Color(0x332C2C2E)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )
    }
}

/**
 * 🎨 专业调色控制面板 (10 项专业参数、参数徽章微标、刻度滑块与全局重置)
 */
@Composable
private fun AdjustControlPanel(
    params: ColorAdjustmentParams,
    activeParam: AdjustParamType,
    onSelectParam: (AdjustParamType) -> Unit,
    onParamValueChange: (AdjustParamType, Float) -> Unit,
    onParamValueChangeStart: () -> Unit = {},
    onParamChangeFinished: () -> Unit,
    onResetParam: (AdjustParamType) -> Unit,
    onResetAll: () -> Unit
) {
    val context = LocalContext.current

    fun getValue(param: AdjustParamType): Float = when (param) {
        AdjustParamType.EXPOSURE -> params.exposure
        AdjustParamType.CONTRAST -> params.contrast
        AdjustParamType.HIGHLIGHTS -> params.highlights
        AdjustParamType.SHADOWS -> params.shadows
        AdjustParamType.SATURATION -> params.saturation
        AdjustParamType.WARMTH -> params.warmth
        AdjustParamType.TINT -> params.tint
        AdjustParamType.VIGNETTE -> params.vignette
        AdjustParamType.SHARPNESS -> params.sharpness
        AdjustParamType.BRIGHTNESS -> params.brightness
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 1. 横向滚动参数选择器 (iOS 标杆级圆环标尺轮盘)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdjustParamType.values().forEach { param ->
                val isSelected = (activeParam == param)
                val currentVal = getValue(param)
                val isModified = (currentVal != 0f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(54.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isModified -> Color(0x33FF9F0A)
                            isSelected -> Color(0x33FFFFFF)
                            else -> Color(0x2E1C1C1E)
                        },
                        border = BorderStroke(
                            width = if (isSelected || isModified) 1.5.dp else 0.5.dp,
                            color = when {
                                isModified -> Color(0xFFFF9F0A)
                                isSelected -> Color.White
                                else -> Color(0x2EFFFFFF)
                            }
                        ),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .bounceScale()
                            .clickable {
                                onSelectParam(param)
                                HapticFeedbackUtil.click(context)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val iconVector = when (param) {
                                AdjustParamType.EXPOSURE -> Icons.Filled.WbSunny
                                AdjustParamType.BRIGHTNESS -> Icons.Filled.LightMode
                                AdjustParamType.CONTRAST -> Icons.Filled.Contrast
                                AdjustParamType.HIGHLIGHTS -> Icons.Filled.Highlight
                                AdjustParamType.SHADOWS -> Icons.Filled.Bedtime
                                AdjustParamType.SATURATION -> Icons.Filled.ColorLens
                                AdjustParamType.WARMTH -> Icons.Filled.Thermostat
                                AdjustParamType.TINT -> Icons.Filled.Palette
                                AdjustParamType.VIGNETTE -> Icons.Filled.Lens
                                AdjustParamType.SHARPNESS -> Icons.Filled.Details
                            }
                            Icon(
                                imageVector = iconVector,
                                contentDescription = param.label,
                                tint = when {
                                    isModified -> Color(0xFFFFB340)
                                    isSelected -> Color.White
                                    else -> Color(0xFF8E8E93)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(5.dp))

                    // 下方标签或数值 (若修改则高亮数值，未修改则显示标签名)
                    Text(
                        text = if (isModified) {
                            if (param.isBipolar) {
                                if (currentVal > 0) "+${currentVal.toInt()}" else "${currentVal.toInt()}"
                            } else "${currentVal.toInt()}"
                        } else {
                            param.label
                        },
                        color = when {
                            isModified -> Color(0xFFFFB340)
                            isSelected -> Color.White
                            else -> Color(0xFF8E8E93)
                        },
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected || isModified) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        // 2. 当前选中参数的触感刻度滑块
        HapticDialSlider(
            param = activeParam,
            value = getValue(activeParam),
            onValueChange = { newVal ->
                onParamValueChange(activeParam, newVal)
            },
            onValueChangeStart = onParamValueChangeStart,
            onValueChangeFinished = onParamChangeFinished,
            onReset = {
                onResetParam(activeParam)
            }
        )

        // 3. 底部重置全部快捷按钮 (若有任何调色更改)
        if (!params.isDefault) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x26FF453A),
                    border = BorderStroke(0.5.dp, Color(0x66FF453A)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onResetAll()
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "重置所有参数",
                            color = Color(0xFFFF453A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🧱 隐私打码控制面板 (MosaicControlPanel) - 纯文字精致风格，零 Emoji
 */
@Composable
private fun MosaicControlPanel(
    selectedRadiusNorm: Float,
    onSelectRadiusNorm: (Float) -> Unit,
    strokeCount: Int,
    onUndoStroke: () -> Unit,
    onClearStrokes: () -> Unit
) {
    val context = LocalContext.current
    val brushSizes = listOf(
        Triple("细笔触", 0.020f, "细节隐私"),
        Triple("中笔触", 0.035f, "常用打码"),
        Triple("粗笔触", 0.060f, "大面积遮挡")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "笔触粗细",
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = if (strokeCount > 0) "已遮挡 $strokeCount 处" else "滑动屏幕即可直接打码",
                color = if (strokeCount > 0) Color(0xFFFF9F0A) else Color(0xFF636366),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(8.dp))

        // 笔触大小胶囊选择器 (纯文字)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            brushSizes.forEach { (label, radius, desc) ->
                val isSelected = (Math.abs(selectedRadiusNorm - radius) < 0.005f)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFFFF9F0A) else Color(0x262C2C2E),
                    border = BorderStroke(0.5.dp, if (isSelected) Color(0xFFFFB340) else Color(0x22FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .bounceScale()
                        .clickable {
                            onSelectRadiusNorm(radius)
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = desc,
                            color = if (isSelected) Color(0xCC000000) else Color(0xFF8E8E93),
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 操作栏：撤销上一步笔触 / 清除全部打码 (纯文字)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (strokeCount > 0) Color(0x332C2C2E) else Color(0x1A2C2C2E),
                border = BorderStroke(0.5.dp, if (strokeCount > 0) Color(0x44FFFFFF) else Color(0x1AFFFFFF)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .bounceScale()
                    .clickable(enabled = strokeCount > 0) {
                        onUndoStroke()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "撤销上一步笔触",
                        color = if (strokeCount > 0) Color.White else Color(0xFF636366),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (strokeCount > 0) Color(0x33FF453A) else Color(0x1AFF453A),
                border = BorderStroke(0.5.dp, if (strokeCount > 0) Color(0x66FF453A) else Color(0x22FF453A)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .bounceScale()
                    .clickable(enabled = strokeCount > 0) {
                        onClearStrokes()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "清除所有打码",
                        color = if (strokeCount > 0) Color(0xFFFF6961) else Color(0x66FF453A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 中部画板与裁剪遮罩/水印实时预览组件 (视口动态放大居中 + 所见即所得)
 */
@Composable
private fun EditorCanvas(
    bitmap: Bitmap,
    cropRectNorm: RectF,
    selectedRatio: CropAspectRatio,
    isCropMode: Boolean,
    isMosaicMode: Boolean = false,
    mosaicStrokes: List<MosaicStroke> = emptyList(),
    mosaicBrushRadiusNorm: Float = 0.035f,
    onAddMosaicStroke: (MosaicStroke) -> Unit = {},
    watermarkType: WatermarkType,
    watermarkPosition: WatermarkPosition,
    watermarkPattern: String,
    photoDate: Date,
    photoAddress: String?,
    onCropRectChanged: (RectF) -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        if (containerW <= 0f || containerH <= 0f) return@BoxWithConstraints

        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()
        if (imgW <= 0f || imgH <= 0f) return@BoxWithConstraints

        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

        if (!isCropMode) {
            // ==================== 非裁剪模式 (滤镜 / 调色 / 时间戳 / 打码预览) ====================
            val scale = min(containerW / imgW, containerH / imgH)
            val drawnW = imgW * scale
            val drawnH = imgH * scale

            val pixelatedPreviewBitmap = remember(bitmap) {
                ImageEditorEngine.createPixelatedBitmap(
                    bitmap,
                    blockSize = max(6, min(bitmap.width, bitmap.height) / 45)
                )
            }

            var currentActivePoints by remember { mutableStateOf<List<MosaicPoint>?>(null) }

            val mosaicModifier = if (isMosaicMode) {
                Modifier.pointerInput(mosaicBrushRadiusNorm, drawnW, drawnH) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val normX = (offset.x / drawnW).coerceIn(0f, 1f)
                            val normY = (offset.y / drawnH).coerceIn(0f, 1f)
                            currentActivePoints = listOf(MosaicPoint(normX, normY))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val normX = (change.position.x / drawnW).coerceIn(0f, 1f)
                            val normY = (change.position.y / drawnH).coerceIn(0f, 1f)
                            val current = currentActivePoints ?: emptyList()
                            currentActivePoints = current + MosaicPoint(normX, normY)
                        },
                        onDragEnd = {
                            val pts = currentActivePoints
                            if (!pts.isNullOrEmpty()) {
                                onAddMosaicStroke(MosaicStroke(pts, mosaicBrushRadiusNorm))
                            }
                            currentActivePoints = null
                        },
                        onDragCancel = {
                            currentActivePoints = null
                        }
                    )
                }
            } else Modifier

            val reusableClipPath = remember { android.graphics.Path() }
            val reusableStrokePath = remember { android.graphics.Path() }
            val reusableFillPath = remember { android.graphics.Path() }
            val reusablePaint = remember {
                android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
            }
            val reusableDstRect = remember { android.graphics.Rect() }

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { drawnW.toDp() },
                        height = with(density) { drawnH.toDp() }
                    )
                    .then(mosaicModifier)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = imageBitmap,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )

                    // 绘制马赛克遮罩 (已完成笔触 + 当前正在绘制的笔触)
                    val allStrokes = if (currentActivePoints.isNullOrEmpty()) {
                        mosaicStrokes
                    } else {
                        mosaicStrokes + MosaicStroke(currentActivePoints!!, mosaicBrushRadiusNorm)
                    }

                    if (allStrokes.isNotEmpty()) {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            reusableClipPath.reset()

                            for (stroke in allStrokes) {
                                if (stroke.points.isEmpty()) continue
                                val strokeWidthPx = stroke.radiusNorm * size.width * 2f
                                reusablePaint.strokeWidth = strokeWidthPx
                                if (stroke.points.size == 1) {
                                    reusableClipPath.addCircle(
                                        stroke.points[0].x * size.width,
                                        stroke.points[0].y * size.height,
                                        strokeWidthPx / 2f,
                                        android.graphics.Path.Direction.CW
                                    )
                                } else {
                                    reusableStrokePath.reset()
                                    reusableFillPath.reset()
                                    reusableStrokePath.moveTo(stroke.points[0].x * size.width, stroke.points[0].y * size.height)
                                    for (i in 1 until stroke.points.size) {
                                        reusableStrokePath.lineTo(stroke.points[i].x * size.width, stroke.points[i].y * size.height)
                                    }
                                    reusablePaint.getFillPath(reusableStrokePath, reusableFillPath)
                                    reusableClipPath.addPath(reusableFillPath)
                                }
                            }

                            nativeCanvas.save()
                            nativeCanvas.clipPath(reusableClipPath)
                            reusableDstRect.set(0, 0, size.width.toInt(), size.height.toInt())
                            nativeCanvas.drawBitmap(
                                pixelatedPreviewBitmap,
                                null,
                                reusableDstRect,
                                null
                            )
                            nativeCanvas.restore()
                        }
                    }
                }

                // 模板水印与复古时间戳实时预览 (所见即所得)
                if (watermarkType != WatermarkType.NONE) {
                    WatermarkOverlay(
                        modifier = Modifier.fillMaxSize(),
                        watermarkType = watermarkType,
                        watermarkPosition = watermarkPosition,
                        watermarkPattern = watermarkPattern,
                        photoDate = photoDate,
                        photoAddress = photoAddress
                    )
                }
            }
        } else {
            // ==================== 裁剪模式 (视口动态放大居中 Auto-Zoom) ====================
            val marginPx = with(density) { 20.dp.toPx() }
            val usableW = (containerW - marginPx * 2f).coerceAtLeast(100f)
            val usableH = (containerH - marginPx * 2f).coerceAtLeast(100f)

            val baseScale = min(usableW / imgW, usableH / imgH)
            val normCropW = cropRectNorm.width().coerceIn(0.02f, 1f)
            val normCropH = cropRectNorm.height().coerceIn(0.02f, 1f)

            val rawFitScale = min(usableW / (imgW * normCropW), usableH / (imgH * normCropH))
            // 视口放大倍率上限放开至 8.0 倍，使小选框能自适应放大占满视口中央；同时配合视口相交裁剪渲染，确保零 GPU 纹理溢出
            val maxSafeScale = baseScale * 8.0f
            val targetFitScale = rawFitScale.coerceIn(baseScale, maxSafeScale)

            val targetImgW = imgW * targetFitScale
            val targetImgH = imgH * targetFitScale
            val targetBoxW = imgW * normCropW * targetFitScale
            val targetBoxH = imgH * normCropH * targetFitScale

            val targetBoxLeft = (containerW - targetBoxW) / 2f
            val targetBoxTop = (containerH - targetBoxH) / 2f

            val targetImgLeft = targetBoxLeft - cropRectNorm.left * targetImgW
            val targetImgTop = targetBoxTop - cropRectNorm.top * targetImgH

            // 视口弹性平滑缩放动画 (240ms 迅速响应，零卡顿)
            val animImgLeft by animateFloatAsState(
                targetValue = targetImgLeft,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "cropAnimImgLeft"
            )
            val animImgTop by animateFloatAsState(
                targetValue = targetImgTop,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "cropAnimImgTop"
            )
            val animImgW by animateFloatAsState(
                targetValue = targetImgW,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "cropAnimImgW"
            )
            val animImgH by animateFloatAsState(
                targetValue = targetImgH,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "cropAnimImgH"
            )

            CropOverlay(
                modifier = Modifier.fillMaxSize(),
                imageBitmap = imageBitmap,
                imgLeft = animImgLeft,
                imgTop = animImgTop,
                imgW = animImgW,
                imgH = animImgH,
                cropRectNorm = cropRectNorm,
                selectedRatio = selectedRatio,
                imagePixelW = imgW,
                imagePixelH = imgH,
                onCropRectChanged = onCropRectChanged
            )
        }
    }
}

/**
 * 📐 专业级裁剪蒙版与手势交互 (视口放大居中 + 零延迟手势跟踪)
 */
@Composable
private fun CropOverlay(
    modifier: Modifier = Modifier,
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap,
    imgLeft: Float,
    imgTop: Float,
    imgW: Float,
    imgH: Float,
    cropRectNorm: RectF,
    selectedRatio: CropAspectRatio,
    imagePixelW: Float,
    imagePixelH: Float,
    onCropRectChanged: (RectF) -> Unit
) {
    val context = LocalContext.current
    val currentRatioState by rememberUpdatedState(selectedRatio)
    val currentOnCropRectChanged by rememberUpdatedState(onCropRectChanged)
    val currentImgPixelW by rememberUpdatedState(imagePixelW)
    val currentImgPixelH by rememberUpdatedState(imagePixelH)

    var liveRect by remember(cropRectNorm) { mutableStateOf(RectF(cropRectNorm)) }
    var isInteracting by remember { mutableStateOf(false) }

    // 交互拖动期间锚定底图位置（杜绝滑动中正反馈跳跃，保证手指 1:1 跟手）
    var frozenImgLeft by remember { mutableFloatStateOf(imgLeft) }
    var frozenImgTop by remember { mutableFloatStateOf(imgTop) }
    var frozenImgW by remember { mutableFloatStateOf(imgW) }
    var frozenImgH by remember { mutableFloatStateOf(imgH) }

    val activeImgLeft = if (isInteracting) frozenImgLeft else imgLeft
    val activeImgTop = if (isInteracting) frozenImgTop else imgTop
    val activeImgW = if (isInteracting) frozenImgW else imgW
    val activeImgH = if (isInteracting) frozenImgH else imgH

    val gridAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 180),
        label = "gridAlpha"
    )

    Canvas(
        modifier = modifier
            .pointerInput(currentRatioState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    if (canvasW <= 0f || canvasH <= 0f) return@awaitEachGesture

                    // 记录手势开始瞬间的静态底图变换
                    frozenImgLeft = imgLeft
                    frozenImgTop = imgTop
                    frozenImgW = imgW
                    frozenImgH = imgH

                    val curImgLeft = frozenImgLeft
                    val curImgTop = frozenImgTop
                    val curImgW = frozenImgW
                    val curImgH = frozenImgH

                    val currentRect = liveRect
                    val leftPx = curImgLeft + currentRect.left * curImgW
                    val topPx = curImgTop + currentRect.top * curImgH
                    val rightPx = curImgLeft + currentRect.right * curImgW
                    val bottomPx = curImgTop + currentRect.bottom * curImgH

                    val cornerHitRadius = 44.dp.toPx()
                    val edgeHitDistance = 28.dp.toPx()
                    val initialPos = down.position

                    // 1. 角落手柄判定 (以最近角点为准，计算欧氏距离，防止小选框时误触其他角点)
                    val distTL = (initialPos - Offset(leftPx, topPx)).getDistance()
                    val distTR = (initialPos - Offset(rightPx, topPx)).getDistance()
                    val distBL = (initialPos - Offset(leftPx, bottomPx)).getDistance()
                    val distBR = (initialPos - Offset(rightPx, bottomPx)).getDistance()
                    val minCornerDist = minOf(distTL, distTR, distBL, distBR)

                    var handle = if (minCornerDist <= cornerHitRadius) {
                        when (minCornerDist) {
                            distTL -> "TOP_LEFT"
                            distTR -> "TOP_RIGHT"
                            distBL -> "BOTTOM_LEFT"
                            distBR -> "BOTTOM_RIGHT"
                            else -> null
                        }
                    } else null

                    // 2. 边框手柄或内部平移 (Pan) 判定
                    if (handle == null) {
                        val inX = initialPos.x in (leftPx - edgeHitDistance)..(rightPx + edgeHitDistance)
                        val inY = initialPos.y in (topPx - edgeHitDistance)..(bottomPx + edgeHitDistance)
                        val distTop = if (inX) kotlin.math.abs(initialPos.y - topPx) else Float.MAX_VALUE
                        val distBottom = if (inX) kotlin.math.abs(initialPos.y - bottomPx) else Float.MAX_VALUE
                        val distLeft = if (inY) kotlin.math.abs(initialPos.x - leftPx) else Float.MAX_VALUE
                        val distRight = if (inY) kotlin.math.abs(initialPos.x - rightPx) else Float.MAX_VALUE

                        val minDist = minOf(distTop, distBottom, distLeft, distRight)
                        if (minDist <= edgeHitDistance) {
                            handle = when (minDist) {
                                distTop -> "EDGE_TOP"
                                distBottom -> "EDGE_BOTTOM"
                                distLeft -> "EDGE_LEFT"
                                distRight -> "EDGE_RIGHT"
                                else -> null
                            }
                        } else if (initialPos.x in leftPx..rightPx && initialPos.y in topPx..bottomPx) {
                            handle = "CENTER"
                        }
                    }

                    if (handle != null) {
                        isInteracting = true
                        HapticFeedbackUtil.tick(context)
                    }

                    val dragStartRect = RectF(liveRect)
                    val startPoint = initialPos
                    var lastPinchDistance = 0f
                    var lastPinchCentroid = Offset.Zero

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }
                        if (activePointers.isEmpty()) break

                        if (activePointers.size == 1 && handle != null) {
                            val change = activePointers[0]
                            val curPos = change.position
                            change.consume()

                            // 将像素位移精确换算为相对于底层图片的归一化位移
                            val cumDx = (curPos.x - startPoint.x) / curImgW
                            val cumDy = (curPos.y - startPoint.y) / curImgH

                            val updated = computeUpdatedCropRect(
                                handle = handle,
                                startRect = dragStartRect,
                                cumulativeDx = cumDx,
                                cumulativeDy = cumDy,
                                targetRatio = currentRatioState.ratio,
                                imgW = currentImgPixelW,
                                imgH = currentImgPixelH
                            )
                            liveRect = updated
                        } else if (activePointers.size >= 2) {
                            isInteracting = true
                            activePointers.forEach { it.consume() }

                            val p0 = activePointers[0].position
                            val p1 = activePointers[1].position
                            val centroid = (p0 + p1) / 2f
                            val distance = (p0 - p1).getDistance()

                            if (lastPinchDistance > 10f) {
                                val zoomRatio = distance / lastPinchDistance
                                val panDx = (centroid.x - lastPinchCentroid.x) / curImgW
                                val panDy = (centroid.y - lastPinchCentroid.y) / curImgH

                                val cur = liveRect
                                val curW = cur.width()
                                val curH = cur.height()
                                val targetRatio = currentRatioState.ratio
                                val minSize = 0.05f

                                val scale = 1f / zoomRatio.coerceIn(0.5f, 2.0f)
                                var newW = (curW * scale).coerceIn(minSize, 1.0f)
                                var newH = if (targetRatio != null && currentImgPixelW > 0 && currentImgPixelH > 0) {
                                    val k = targetRatio * (currentImgPixelH / currentImgPixelW)
                                    (newW / k).coerceIn(minSize / k, 1.0f)
                                } else {
                                    (curH * scale).coerceIn(minSize, 1.0f)
                                }

                                if (targetRatio != null && currentImgPixelW > 0 && currentImgPixelH > 0) {
                                    val k = targetRatio * (currentImgPixelH / currentImgPixelW)
                                    newW = newH * k
                                }

                                val curCenterX = (cur.left + cur.right) / 2f + panDx
                                val curCenterY = (cur.top + cur.bottom) / 2f + panDy

                                var nL = curCenterX - newW / 2f
                                var nT = curCenterY - newH / 2f
                                var nR = curCenterX + newW / 2f
                                var nB = curCenterY + newH / 2f

                                if (nL < 0f) { nR -= nL; nL = 0f }
                                if (nR > 1f) { nL -= (nR - 1f); nR = 1f }
                                if (nT < 0f) { nB -= nT; nT = 0f }
                                if (nB > 1f) { nT -= (nB - 1f); nB = 1f }

                                val clamped = RectF(
                                    nL.coerceIn(0f, 1f - minSize),
                                    nT.coerceIn(0f, 1f - minSize),
                                    nR.coerceIn(minSize, 1f),
                                    nB.coerceIn(minSize, 1f)
                                )
                                liveRect = clamped
                            }

                            lastPinchDistance = distance
                            lastPinchCentroid = centroid
                        }
                    }

                    isInteracting = false
                    handle = null
                    // 🎯 手势抬起：提交最终矩形，触发视口平滑弹性缩放动画放大居中！
                    currentOnCropRectChanged(liveRect)
                }
            }
    ) {
        val canvasW = size.width
        val canvasH = size.height

        // 1. 绘制底图 (视口相交区域精准投影：无论放大多少倍，dstSize 严格约束在屏幕视口内，杜绝 GPU 溢出)
        val visL = maxOf(0f, activeImgLeft)
        val visT = maxOf(0f, activeImgTop)
        val visR = minOf(canvasW, activeImgLeft + activeImgW)
        val visB = minOf(canvasH, activeImgTop + activeImgH)

        if (visR > visL && visB > visT && activeImgW > 0f && activeImgH > 0f) {
            val srcL = (((visL - activeImgLeft) / activeImgW) * imageBitmap.width).toInt().coerceIn(0, imageBitmap.width - 1)
            val srcT = (((visT - activeImgTop) / activeImgH) * imageBitmap.height).toInt().coerceIn(0, imageBitmap.height - 1)
            val srcR = (((visR - activeImgLeft) / activeImgW) * imageBitmap.width).toInt().coerceIn(srcL + 1, imageBitmap.width)
            val srcB = (((visB - activeImgTop) / activeImgH) * imageBitmap.height).toInt().coerceIn(srcT + 1, imageBitmap.height)

            drawImage(
                image = imageBitmap,
                srcOffset = androidx.compose.ui.unit.IntOffset(srcL, srcT),
                srcSize = androidx.compose.ui.unit.IntSize(srcR - srcL, srcB - srcT),
                dstOffset = androidx.compose.ui.unit.IntOffset(visL.toInt(), visT.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize((visR - visL).toInt(), (visB - visT).toInt())
            )
        }

        // 2. 当前选框在屏幕上的像素坐标
        val l = activeImgLeft + liveRect.left * activeImgW
        val t = activeImgTop + liveRect.top * activeImgH
        val r = activeImgLeft + liveRect.right * activeImgW
        val b = activeImgTop + liveRect.bottom * activeImgH

        val scrimColor = Color(0xB3000000)

        // 3. 绘制外部暗色遮罩
        if (t > 0f) drawRect(scrimColor, topLeft = Offset(0f, 0f), size = Size(canvasW, t))
        if (b < canvasH) drawRect(scrimColor, topLeft = Offset(0f, b), size = Size(canvasW, canvasH - b))
        if (l > 0f) drawRect(scrimColor, topLeft = Offset(0f, t), size = Size(l, b - t))
        if (r < canvasW) drawRect(scrimColor, topLeft = Offset(r, t), size = Size(canvasW - r, b - t))

        // 4. 裁剪选框白色主边框
        drawRect(
            color = Color.White,
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 5. 摄影三等分线与兴趣交点
        if (gridAlpha > 0.01f) {
            val thirdW = (r - l) / 3f
            val thirdH = (b - t) / 3f
            val gridColor = Color.White.copy(alpha = 0.55f * gridAlpha)
            val gridStroke = 1.dp.toPx()

            drawLine(gridColor, Offset(l + thirdW, t), Offset(l + thirdW, b), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(l + thirdW * 2, t), Offset(l + thirdW * 2, b), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(l, t + thirdH), Offset(r, t + thirdH), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(l, t + thirdH * 2), Offset(r, t + thirdH * 2), strokeWidth = gridStroke)

            val dotRadius = 2.5.dp.toPx()
            val dotColor = Color.White.copy(alpha = 0.85f * gridAlpha)
            drawCircle(dotColor, radius = dotRadius, center = Offset(l + thirdW, t + thirdH))
            drawCircle(dotColor, radius = dotRadius, center = Offset(l + thirdW * 2, t + thirdH))
            drawCircle(dotColor, radius = dotRadius, center = Offset(l + thirdW, t + thirdH * 2))
            drawCircle(dotColor, radius = dotRadius, center = Offset(l + thirdW * 2, t + thirdH * 2))
        }

        // 6. 四角取景器拐角强化直角手柄
        val handleLen = 22.dp.toPx()
        val handleStroke = 3.5.dp.toPx()
        val handleColor = Color.White

        // 左上
        drawLine(handleColor, Offset(l - 1f, t), Offset(l + handleLen, t), strokeWidth = handleStroke)
        drawLine(handleColor, Offset(l, t - 1f), Offset(l + handleLen, t), strokeWidth = handleStroke)
        // 右上
        drawLine(handleColor, Offset(r + 1f, t), Offset(r - handleLen, t), strokeWidth = handleStroke)
        drawLine(handleColor, Offset(r, t - 1f), Offset(r, t + handleLen), strokeWidth = handleStroke)
        // 左下
        drawLine(handleColor, Offset(l - 1f, b), Offset(l + handleLen, b), strokeWidth = handleStroke)
        drawLine(handleColor, Offset(l, b + 1f), Offset(l, b - handleLen), strokeWidth = handleStroke)
        // 右下
        drawLine(handleColor, Offset(r + 1f, b), Offset(r - handleLen, b), strokeWidth = handleStroke)
        drawLine(handleColor, Offset(r, b + 1f), Offset(r - handleLen, b), strokeWidth = handleStroke)

        // 7. 4 条边中点手柄指示器
        val midEdgeBarLen = 16.dp.toPx()
        val midEdgeBarStroke = 3.dp.toPx()
        val midX = (l + r) / 2f
        val midY = (t + b) / 2f

        drawLine(handleColor, Offset(midX - midEdgeBarLen, t), Offset(midX + midEdgeBarLen, t), strokeWidth = midEdgeBarStroke)
        drawLine(handleColor, Offset(midX - midEdgeBarLen, b), Offset(midX + midEdgeBarLen, b), strokeWidth = midEdgeBarStroke)
        drawLine(handleColor, Offset(l, midY - midEdgeBarLen), Offset(l, midY + midEdgeBarLen), strokeWidth = midEdgeBarStroke)
        drawLine(handleColor, Offset(r, midY - midEdgeBarLen), Offset(r, midY + midEdgeBarLen), strokeWidth = midEdgeBarStroke)
    }
}

/**
 * 📐 专业级裁剪区域自然形变计算引擎 (对角固定锚点 + 投影缩放 + 边界平滑吸附)
 */
private fun computeUpdatedCropRect(
    handle: String,
    startRect: RectF,
    cumulativeDx: Float,
    cumulativeDy: Float,
    targetRatio: Float?,
    imgW: Float,
    imgH: Float,
    minSize: Float = 0.05f
): RectF {
    if (handle == "CENTER") {
        val boxW = startRect.width()
        val boxH = startRect.height()
        val maxLeft = (1f - boxW).coerceAtLeast(0f)
        val maxTop = (1f - boxH).coerceAtLeast(0f)
        val newLeft = (startRect.left + cumulativeDx).coerceIn(0f, maxLeft)
        val newTop = (startRect.top + cumulativeDy).coerceIn(0f, maxTop)
        return RectF(newLeft, newTop, newLeft + boxW, newTop + boxH)
    }

    if (targetRatio == null || imgW <= 0f || imgH <= 0f) {
        // 自由模式：四边与四角完全自由伸缩，对侧固定锚定
        return when (handle) {
            "EDGE_LEFT" -> {
                val newLeft = (startRect.left + cumulativeDx).coerceIn(0f, startRect.right - minSize)
                RectF(newLeft, startRect.top, startRect.right, startRect.bottom)
            }
            "EDGE_RIGHT" -> {
                val newRight = (startRect.right + cumulativeDx).coerceIn(startRect.left + minSize, 1f)
                RectF(startRect.left, startRect.top, newRight, startRect.bottom)
            }
            "EDGE_TOP" -> {
                val newTop = (startRect.top + cumulativeDy).coerceIn(0f, startRect.bottom - minSize)
                RectF(startRect.left, newTop, startRect.right, startRect.bottom)
            }
            "EDGE_BOTTOM" -> {
                val newBottom = (startRect.bottom + cumulativeDy).coerceIn(startRect.top + minSize, 1f)
                RectF(startRect.left, startRect.top, startRect.right, newBottom)
            }
            "TOP_LEFT" -> {
                val newLeft = (startRect.left + cumulativeDx).coerceIn(0f, startRect.right - minSize)
                val newTop = (startRect.top + cumulativeDy).coerceIn(0f, startRect.bottom - minSize)
                RectF(newLeft, newTop, startRect.right, startRect.bottom)
            }
            "TOP_RIGHT" -> {
                val newRight = (startRect.right + cumulativeDx).coerceIn(startRect.left + minSize, 1f)
                val newTop = (startRect.top + cumulativeDy).coerceIn(0f, startRect.bottom - minSize)
                RectF(startRect.left, newTop, newRight, startRect.bottom)
            }
            "BOTTOM_LEFT" -> {
                val newLeft = (startRect.left + cumulativeDx).coerceIn(0f, startRect.right - minSize)
                val newBottom = (startRect.bottom + cumulativeDy).coerceIn(startRect.top + minSize, 1f)
                RectF(newLeft, startRect.top, startRect.right, newBottom)
            }
            "BOTTOM_RIGHT" -> {
                val newRight = (startRect.right + cumulativeDx).coerceIn(startRect.left + minSize, 1f)
                val newBottom = (startRect.bottom + cumulativeDy).coerceIn(startRect.top + minSize, 1f)
                RectF(startRect.left, startRect.top, newRight, newBottom)
            }
            else -> startRect
        }
    }

    // 锁定比例模式 (iOS 级别：角点缩放以对角为锚点等比自然缩放)
    val k = targetRatio * (imgH / imgW)
    val startW = startRect.width()
    val startH = startRect.height()

    return when (handle) {
        "BOTTOM_RIGHT" -> {
            val x0 = startRect.left
            val y0 = startRect.top
            val vx = startW + cumulativeDx
            val vy = startH + cumulativeDy
            val scale = (vx * k + vy) / (k * k + 1f)
            val maxW = minOf(1f - x0, (1f - y0) * k)
            val newW = (scale * k).coerceIn(minSize, maxW)
            val newH = newW / k
            RectF(x0, y0, x0 + newW, y0 + newH)
        }
        "TOP_LEFT" -> {
            val x1 = startRect.right
            val y1 = startRect.bottom
            val vx = startW - cumulativeDx
            val vy = startH - cumulativeDy
            val scale = (vx * k + vy) / (k * k + 1f)
            val maxW = minOf(x1, y1 * k)
            val newW = (scale * k).coerceIn(minSize, maxW)
            val newH = newW / k
            RectF(x1 - newW, y1 - newH, x1, y1)
        }
        "TOP_RIGHT" -> {
            val x0 = startRect.left
            val y1 = startRect.bottom
            val vx = startW + cumulativeDx
            val vy = startH - cumulativeDy
            val scale = (vx * k + vy) / (k * k + 1f)
            val maxW = minOf(1f - x0, y1 * k)
            val newW = (scale * k).coerceIn(minSize, maxW)
            val newH = newW / k
            RectF(x0, y1 - newH, x0 + newW, y1)
        }
        "BOTTOM_LEFT" -> {
            val x1 = startRect.right
            val y0 = startRect.top
            val vx = startW - cumulativeDx
            val vy = startH + cumulativeDy
            val scale = (vx * k + vy) / (k * k + 1f)
            val maxW = minOf(x1, (1f - y0) * k)
            val newW = (scale * k).coerceIn(minSize, maxW)
            val newH = newW / k
            RectF(x1 - newW, y0, x1, y0 + newH)
        }
        "EDGE_RIGHT" -> {
            val x0 = startRect.left
            val centerY = (startRect.top + startRect.bottom) / 2f
            val maxW_byRight = 1f - x0
            val maxH = 2f * minOf(centerY, 1f - centerY)
            val maxW = minOf(maxW_byRight, maxH * k)
            val newW = (startW + cumulativeDx).coerceIn(minSize, maxW)
            val newH = newW / k
            val newTop = centerY - newH / 2f
            RectF(x0, newTop, x0 + newW, newTop + newH)
        }
        "EDGE_LEFT" -> {
            val x1 = startRect.right
            val centerY = (startRect.top + startRect.bottom) / 2f
            val maxW_byLeft = x1
            val maxH = 2f * minOf(centerY, 1f - centerY)
            val maxW = minOf(maxW_byLeft, maxH * k)
            val newW = (startW - cumulativeDx).coerceIn(minSize, maxW)
            val newH = newW / k
            val newTop = centerY - newH / 2f
            RectF(x1 - newW, newTop, x1, newTop + newH)
        }
        "EDGE_BOTTOM" -> {
            val y0 = startRect.top
            val centerX = (startRect.left + startRect.right) / 2f
            val maxH_byBottom = 1f - y0
            val maxW = 2f * minOf(centerX, 1f - centerX)
            val maxH = minOf(maxH_byBottom, maxW / k)
            val newH = (startH + cumulativeDy).coerceIn(minSize / k, maxH)
            val newW = newH * k
            val newLeft = centerX - newW / 2f
            RectF(newLeft, y0, newLeft + newW, y0 + newH)
        }
        "EDGE_TOP" -> {
            val y1 = startRect.bottom
            val centerX = (startRect.left + startRect.right) / 2f
            val maxH_byTop = y1
            val maxW = 2f * minOf(centerX, 1f - centerX)
            val maxH = minOf(maxH_byTop, maxW / k)
            val newH = (startH - cumulativeDy).coerceIn(minSize / k, maxH)
            val newW = newH * k
            val newLeft = centerX - newW / 2f
            RectF(newLeft, y1 - newH, newLeft + newW, y1)
        }
        else -> startRect
    }
}

/**
 * 水印实时预览浮层 (严格遵循时间戳实现指南.md 1:1 规范)
 */
@Composable
private fun WatermarkOverlay(
    modifier: Modifier = Modifier,
    watermarkType: WatermarkType,
    watermarkPosition: WatermarkPosition,
    watermarkPattern: String,
    photoDate: Date,
    photoAddress: String?
) {
    if (watermarkType == WatermarkType.NONE) return
    val context = LocalContext.current

    Canvas(modifier = modifier) {
        val canvasW = size.width.toInt()
        val canvasH = size.height.toInt()
        if (canvasW <= 0 || canvasH <= 0) return@Canvas

        runCatching {
            drawIntoCanvas { composeCanvas ->
                val metadata = WatermarkMetadata(
                    dateTaken = photoDate,
                    locationName = photoAddress,
                    pattern = watermarkPattern,
                    position = watermarkPosition
                )
                WatermarkRenderer.drawWatermarkOverlay(
                    canvas = composeCanvas.nativeCanvas,
                    width = canvasW,
                    height = canvasH,
                    type = watermarkType,
                    metadata = metadata,
                    context = context
                )
            }
        }.onFailure { t ->
            android.util.Log.e("WatermarkOverlay", "Draw watermark safely intercepted error", t)
        }
    }
}

/**
 * 裁剪与旋转控制面板 (iOS 风格磨砂工具胶囊与圆形操作按钮)
 */
@Composable
private fun CropControlPanel(
    selectedRatio: CropAspectRatio,
    onSelectRatio: (CropAspectRatio) -> Unit,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // 比例选择胶囊栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropAspectRatio.values().forEach { ratio ->
                val isSelected = (selectedRatio == ratio)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) Color.White else Color(0x332C2C2E),
                    border = BorderStroke(if (isSelected) 1.dp else 0.5.dp, if (isSelected) Color.White else Color(0x22FFFFFF)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .bounceScale()
                        .clickable {
                            onSelectRatio(ratio)
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Text(
                        text = ratio.label,
                        color = if (isSelected) Color.Black else Color(0xFFD1D1D6),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // iOS 风格操作按钮 (旋转、翻转、重置)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 旋转 90°
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x332C2C2E),
                    border = BorderStroke(0.5.dp, Color(0x28FFFFFF)),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .bounceScale()
                        .clickable {
                            onRotate()
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "旋转90°", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("旋转90°", color = Color(0xFF8E8E93), fontSize = 10.5.sp)
            }

            // 水平翻转
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x332C2C2E),
                    border = BorderStroke(0.5.dp, Color(0x28FFFFFF)),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .bounceScale()
                        .clickable {
                            onFlip()
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Flip, contentDescription = "水平翻转", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("镜像翻转", color = Color(0xFF8E8E93), fontSize = 10.5.sp)
            }

            // 重置
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x332C2C2E),
                    border = BorderStroke(0.5.dp, Color(0x28FFFFFF)),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .bounceScale()
                        .clickable {
                            onReset()
                            HapticFeedbackUtil.click(context)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "还原", tint = Color(0xFFFF453A), modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("还原", color = Color(0xFF8E8E93), fontSize = 10.5.sp)
            }
        }
    }
}

/**
 * 🏷️ 模板水印与复古时间戳控制面板 (1:1 对标用户实机界面：模板胶囊 + 位置切换 + 日期格式)
 */
@Composable
private fun TimestampControlPanel(
    selectedType: WatermarkType,
    onSelectType: (WatermarkType) -> Unit,
    selectedPosition: WatermarkPosition,
    onSelectPosition: (WatermarkPosition) -> Unit,
    selectedPattern: String,
    onSelectPattern: (String) -> Unit
) {
    val context = LocalContext.current

    val templates = listOf(
        WatermarkType.DAZZ_VCR_TILTED,
        WatermarkType.DAZZ_VCR_ORANGE,
        WatermarkType.DAZZ_LCD_RED,
        WatermarkType.DAZZ_LCD_GREEN,
        WatermarkType.DAZZ_VHS,
        WatermarkType.DAZZ_DEFAULT_YELLOW,
        WatermarkType.DAZZ_CPM35_DATE,
        WatermarkType.FILM_METADATA_STRIP,
        WatermarkType.LEICA_CAMERA_FRAME,
        WatermarkType.POLAROID_FRAME,
        WatermarkType.INSTA_GEOTAG_PILL,
        WatermarkType.NONE
    )

    val positions = listOf(
        WatermarkPosition.BOTTOM_RIGHT,
        WatermarkPosition.BOTTOM_LEFT,
        WatermarkPosition.BOTTOM_CENTER,
        WatermarkPosition.LEFT_VERTICAL
    )

    val patterns = listOf(
        "''yy MM dd" to "'24 08 15",
        "yy MM dd" to "24 08 15",
        "yyyy.MM.dd HH:mm" to "2024.08.15 14:30",
        "yyyy-MM-dd HH:mm:ss" to "2024-08-15 14:30:25",
        "MM.dd.yyyy HH:mm" to "08.15.2024 14:30",
        "dd.MM.yyyy HH:mm" to "15.08.2024 14:30",
        "MMM.dd yyyy" to "AUG.15 2024",
        "yyyy. MM. dd" to "2024. 08. 15",
        "''yy年  M月d日" to "'24年 8月15日"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // 1. 第一行：模板水印横向滚动胶囊栏 (1:1 对齐用户实机设计)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            templates.forEach { type ->
                val isSelected = (selectedType == type)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) Color.White else Color(0x332C2C2E),
                    border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.5.dp,
                        color = if (isSelected) Color.White else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .bounceScale()
                        .clickable {
                            runCatching { onSelectType(type) }
                            HapticFeedbackUtil.click(context)
                            CameraSoundPlayer.playSlideClick(context)
                        }
                ) {
                    Text(
                        text = type.label,
                        color = if (isSelected) Color.Black else Color(0xFFD1D1D6),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // 2. 第二行：水印位置分段控制器 (当开启水印时展示)
        if (selectedType != WatermarkType.NONE) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "位置",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x331C1C1E),
                    border = BorderStroke(0.5.dp, Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        positions.forEach { pos ->
                            val isPosSelected = (selectedPosition == pos)
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = if (isPosSelected) Color.White else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(13.dp))
                                    .bounceScale()
                                    .clickable {
                                        runCatching { onSelectPosition(pos) }
                                        HapticFeedbackUtil.click(context)
                                        CameraSoundPlayer.playSlideClick(context)
                                    }
                            ) {
                                Text(
                                    text = pos.label,
                                    color = if (isPosSelected) Color.Black else Color(0xFFD1D1D6),
                                    fontWeight = if (isPosSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. 第三行：日期格式选择器
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "格式",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    patterns.forEach { (pat, previewStr) ->
                        val isPatSelected = (selectedPattern == pat)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPatSelected) Color.White else Color(0x332C2C2E),
                            border = BorderStroke(
                                width = if (isPatSelected) 1.dp else 0.5.dp,
                                color = if (isPatSelected) Color.White else Color(0x22FFFFFF)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .bounceScale()
                                .clickable {
                                    runCatching { onSelectPattern(pat) }
                                    HapticFeedbackUtil.click(context)
                                    CameraSoundPlayer.playSlideClick(context)
                                }
                        ) {
                            Text(
                                text = previewStr,
                                color = if (isPatSelected) Color.Black else Color(0xFFD1D1D6),
                                fontWeight = if (isPatSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 4. 水印特性说明与参数指示
            Spacer(Modifier.height(6.dp))
            Text(
                text = selectedType.subtitle,
                color = Color(0xFF636366),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}
