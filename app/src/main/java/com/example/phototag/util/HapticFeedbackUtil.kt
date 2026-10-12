package com.example.phototag.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * ⚡ 全系统细腻触觉反馈引擎 (HapticFeedbackUtil)。
 *
 * 提供多档位细腻物理触感：
 * 1. [tick]：滑动多选每划过一张照片时触发的极轻微微震（10ms 轻触感）。
 * 2. [click]：月份边界跨越、Tab 切换、分组全选时触发的标准档位震动（20ms 清脆感）。
 * 3. [heavy]：长按进入多选、删除确认等重要操作时的明显长震动（45ms 重质感）。
 * 4. [doubleClick]：双击操作的轻盈双连震。
 */
object HapticFeedbackUtil {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * 极轻微震（用于滑动连选每个 Item、微调 Scrubber）。
     */
    fun tick(context: Context, view: View? = null) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
            return
        }
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 60))
            }
        } catch (_: Exception) {}
    }

    /**
     * 标准档位震动（用于月份切换、Tab 切换、分组全选勾选）。
     */
    fun click(context: Context, view: View? = null) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return
        }
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(20, 150))
            }
        } catch (_: Exception) {}
    }

    /**
     * 重质感强震（用于长按启动多选模式、删除确认、危险操作）。
     */
    fun heavy(context: Context, view: View? = null) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return
        }
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(45, 255))
            }
        } catch (_: Exception) {}
    }

    /**
     * 长按振动别名
     */
    fun longPress(context: Context, view: View? = null) = heavy(context, view)

    /**
     * 双连震（用于双击缩放、重置）。
     */
    fun doubleClick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                val timings = longArrayOf(0, 15, 50, 15)
                val amplitudes = intArrayOf(0, 120, 0, 120)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        } catch (_: Exception) {}
    }
}
