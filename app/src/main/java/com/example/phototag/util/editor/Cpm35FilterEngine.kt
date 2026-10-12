package com.example.phototag.util.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.example.phototag.PhotoTagApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 🎞️ 经典复古胶片机、拍立得与 CCD 色彩预设枚举 (兼容旧版调用并直接桥接至 3D HALD LUT 引擎)
 */
enum class FilmPreset(
    val displayName: String,
    val subtitle: String,
    val lutFileName: String
) {
    CPM35("Dazz CPM35", "DAZZ 经典原版", "DAZZ_CPM35_DEFAULT.png"),
    D_FUNS("D FunS", "富士一次性闪光", "DAZZ_DFUNS_DEFAULT.png"),
    INST_C("Inst C", "富士拍立得显影", "DAZZ_INSTC_DEFAULT.png"),
    FX_67("FX 67", "宾得中画幅神机", "DAZZ_S67_DEFAULT.png"),
    CCD_2000("CCD 2000", "千禧复古数码", "DAZZ_CCDR_DEFAULT.png"),
    CT24("CT24", "柯达彩色反转片", "DAZZ_CT2F_DEFAULT.png"),
    CLASSIC_U("Classic U", "奥林巴斯傻瓜机", "DAZZ_UCLASSIC_DEFAULT.png"),
    CINE_800T("Cine 800T", "电影夜景青橙", "DAZZ_KINO_DEFAULT.png"),
    GR_D("GR D", "街头理光质感", "DAZZ_GRDR_DEFAULT.png"),
    KODAK_GOLD("Kodak Gold", "柯达金暖阳", "DAZZ_KV80_KG200.png"),
    PORTRA_400("Portra 400", "自然通透人像", "DAZZ_135NE_19P.png"),
    FUJI_PRO("FUJI PRO", "清透日系人像", "DAZZ_FXN_FXN2.png"),
    BW_CONTRAST("森山黑白", "街头高反差", "DAZZ_GRF_400TX.png"),
    NEOPAN_135("Neopan 135", "富士细腻黑白", "DAZZ_GRF_NEOP100.png")
}

/**
 * 漏光光晕锚点
 */
enum class LightLeakPosition(val label: String) {
    LEFT("左侧漏光"),
    RIGHT("右侧漏光"),
    TOP_LEFT("左上漏光"),
    BOTTOM_RIGHT("右下漏光"),
    RANDOM("随机自然漏光")
}

/**
 * 🎞️ 胶片冲印附加特效选项
 */
data class FilmEffectOptions(
    val withTimestamp: Boolean = false,
    val withLightLeak: Boolean = false,
    val withStarFilter: Boolean = false,
    val withPolaroidFrame: Boolean = false
)

/**
 * 🎞️ 胶片滤镜调度引擎 (已升级为 3D HALD LUT 纯净查表架构)
 */
object FilmFilterEngine {

    /**
     * ✨ Dazz 招牌星光镜 (Star Cross Screen Filter)
     * 自动捕获画面高光耀斑（灯光、夜景、车灯、水面），绽放四芒/八芒浪漫十字星光！
     */
    fun applyStarFilter(source: Bitmap, maxStars: Int = 12): Bitmap {
        val width = source.width
        val height = source.height
        val scale = (width / 360).coerceAtLeast(1)
        val smallW = width / scale
        val smallH = height / scale

        val smallBmp = Bitmap.createScaledBitmap(source, smallW, smallH, false)
        val smallPixels = IntArray(smallW * smallH)
        smallBmp.getPixels(smallPixels, 0, smallW, 0, 0, smallW, smallH)
        smallBmp.recycle()

        data class StarPoint(val x: Float, val y: Float, val brightness: Int)
        val candidates = mutableListOf<StarPoint>()

        for (y in 2 until smallH - 2 step 3) {
            for (x in 2 until smallW - 2 step 3) {
                val p = smallPixels[y * smallW + x]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                val lum = (r * 77 + g * 150 + b * 29) shr 8
                if (lum > 240) {
                    candidates.add(StarPoint(x * scale.toFloat(), y * scale.toFloat(), lum))
                }
            }
        }

        val distinctStars = mutableListOf<StarPoint>()
        val minDist = min(width, height) * 0.08f
        candidates.sortByDescending { it.brightness }
        for (cand in candidates) {
            val tooClose = distinctStars.any {
                val dx = it.x - cand.x
                val dy = it.y - cand.y
                sqrt(dx * dx + dy * dy) < minDist
            }
            if (!tooClose) {
                distinctStars.add(cand)
                if (distinctStars.size >= maxStars) break
            }
        }

        if (distinctStars.isEmpty()) return source

        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val rayLength = min(width, height) * 0.07f

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = (min(width, height) * 0.0035f).coerceAtLeast(2f)
            strokeCap = Paint.Cap.ROUND
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 255, 240, 200)
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }

        for (star in distinctStars) {
            canvas.drawCircle(star.x, star.y, rayLength * 0.20f, glowPaint)

            // 主 4 芒十字光芒
            canvas.drawLine(star.x - rayLength, star.y, star.x + rayLength, star.y, starPaint)
            canvas.drawLine(star.x, star.y - rayLength, star.x, star.y + rayLength, starPaint)

            // 次级对角 8 芒微星光
            val subRay = rayLength * 0.45f
            canvas.drawLine(star.x - subRay, star.y - subRay, star.x + subRay, star.y + subRay, starPaint)
            canvas.drawLine(star.x - subRay, star.y + subRay, star.x + subRay, star.y - subRay, starPaint)
        }

        return output
    }

    /**
     * 对目标位图应用复古胶片机预设（基于真实 3D HALD LUT 查表），可选叠加颗粒、暗角、漏光与星光镜
     */
    suspend fun applyFilmFilter(
        source: Bitmap,
        preset: FilmPreset,
        intensity: Float = 1.0f,
        enableGrain: Boolean = true,
        enableVignette: Boolean = true,
        enableLightLeak: Boolean = false,
        enableStarFilter: Boolean = false,
        context: Context = PhotoTagApplication.instance
    ): Bitmap = withContext(Dispatchers.Default) {
        val graded = DazzFilmEngine.applyFilmColorGrading(
            context = context,
            source = source,
            lutFileName = preset.lutFileName,
            intensity = intensity,
            enableGrain = enableGrain,
            enableVignette = enableVignette,
            enableLightLeak = enableLightLeak
        )

        if (enableStarFilter) {
            val withStar = applyStarFilter(graded)
            if (withStar !== graded && graded !== source) {
                graded.recycle()
            }
            withStar
        } else {
            graded
        }
    }

    /**
     * 同步版本（用于批量后台处理）
     */
    fun applyFilmFilterSync(
        source: Bitmap,
        preset: FilmPreset,
        intensity: Float = 1.0f,
        enableGrain: Boolean = true,
        enableVignette: Boolean = true,
        enableLightLeak: Boolean = false,
        enableStarFilter: Boolean = false,
        context: Context = PhotoTagApplication.instance
    ): Bitmap {
        val graded = DazzFilmEngine.applyFilmColorGradingSync(
            context = context,
            source = source,
            lutFileName = preset.lutFileName,
            intensity = intensity,
            enableGrain = enableGrain,
            enableVignette = enableVignette,
            enableLightLeak = enableLightLeak
        )

        return if (enableStarFilter) {
            val withStar = applyStarFilter(graded)
            if (withStar !== graded && graded !== source) {
                graded.recycle()
            }
            withStar
        } else {
            graded
        }
    }
}
