package com.example.phototag.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * 撤销操作模型 (Undoable Action)。
 */
sealed class UndoableAction(val title: String, val timestamp: Long = System.currentTimeMillis()) {
    data class BatchDelete(val uris: List<String>) :
        UndoableAction("批量移入回收站 ${uris.size} 张照片")

    data class BatchTag(val uris: List<String>, val tagId: Long, val tagName: String) :
        UndoableAction("批量为 ${uris.size} 张照片打上标签 #$tagName")

    data class SoftDelete(val uri: String) :
        UndoableAction("移入回收站 1 张照片")
}

/**
 * ⚡ 摇一摇极速撤销引擎 (ShakeUndoManager)。
 *
 * 核心特性：
 * 1. 【SensorManager 加速度监听】：基于手机物理晃动（阈值 > 13.5 m/s²）触发撤销判定。
 * 2. 【事务回滚操作栈】：保留最近 30 秒内的批量删除与打标操作。
 * 3. 【触觉微震联动】：摇动命中时触发 Heavy 震动并呼出悬浮撤销确认框。
 */
class ShakeUndoManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    // 撤销操作栈
    private val actionStack = ArrayDeque<UndoableAction>()

    private val _pendingUndoAction = MutableStateFlow<UndoableAction?>(null)
    val pendingUndoAction = _pendingUndoAction.asStateFlow()

    private val _shakeEvent = MutableSharedFlow<UndoableAction>(extraBufferCapacity = 1)
    val shakeEvent = _shakeEvent.asSharedFlow()

    fun startListening() {
        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    /**
     * 记录一次可撤销操作。
     */
    fun pushAction(action: UndoableAction) {
        actionStack.addLast(action)
        if (actionStack.size > 20) {
            actionStack.removeFirst()
        }
    }

    fun dismissPendingUndo() {
        _pendingUndoAction.value = null
    }

    fun popAction(): UndoableAction? {
        val action = if (actionStack.isNotEmpty()) actionStack.removeLast() else null
        _pendingUndoAction.value = null
        return action
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastX = x
        lastY = y
        lastZ = z

        val speed = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble())
        val now = System.currentTimeMillis()

        // 阈值 > 13.5 m/s² 且两次摇动间隔 > 1000ms
        if (speed > 13.5 && (now - lastShakeTime) > 1000L) {
            lastShakeTime = now

            // 检查最近 30 秒内是否有可撤销操作
            val lastAction = actionStack.lastOrNull()
            if (lastAction != null && (now - lastAction.timestamp) < 30000L) {
                HapticFeedbackUtil.heavy(context)
                _pendingUndoAction.value = lastAction
                _shakeEvent.tryEmit(lastAction)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
