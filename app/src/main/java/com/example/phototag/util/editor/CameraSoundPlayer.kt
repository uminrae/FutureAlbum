package com.example.phototag.util.editor

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 🔊 DAZZ 机械快门音效播放器（基于 Android SoundPool 零延迟播放，带全量异常防护）
 */
object CameraSoundPlayer {

    @Volatile
    private var soundPool: SoundPool? = null
    private val soundMap = java.util.concurrent.ConcurrentHashMap<String, Int>()
    @Volatile
    private var isInitialized = false
    @Volatile
    private var isInitializing = false

    fun init(context: Context) {
        if (isInitialized || isInitializing) return
        isInitializing = true

        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val pool = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(attrs)
                    .build()
                soundPool = pool

                // 预加载 sound 目录下的快门音频
                val sounds = listOf(
                    "sound_slidep_click.mp3",
                    "sound_slidep_click2.mp3",
                    "sound_slidep_click3.mp3",
                    "sound_slidep_bg.mp3",
                    "sound_slidep_end.mp3"
                )

                for (name in sounds) {
                    try {
                        val afd = appContext.assets.openFd("dazz/sound/$name")
                        val soundId = pool.load(afd, 1)
                        if (soundId > 0) {
                            soundMap[name] = soundId
                        }
                    } catch (_: Exception) {
                        try {
                            val cacheFile = File(appContext.cacheDir, "dazz_sound_$name")
                            if (!cacheFile.exists()) {
                                appContext.assets.open("dazz/sound/$name").use { input ->
                                    FileOutputStream(cacheFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            val soundId = pool.load(cacheFile.absolutePath, 1)
                            if (soundId > 0) {
                                soundMap[name] = soundId
                            }
                        } catch (_: Exception) {}
                    }
                }
                isInitialized = true
            } catch (t: Throwable) {
                android.util.Log.w("CameraSoundPlayer", "Init sound pool failed safely", t)
            } finally {
                isInitializing = false
            }
        }
    }

    /**
     * 播放清脆的机械快门声
     */
    fun playShutter(context: Context? = null, variant: Int = 1) {
        runCatching {
            if (!isInitialized && context != null) {
                init(context.applicationContext)
            }
            val pool = soundPool ?: return@runCatching
            val randomVariant = if (variant == 1) (1..3).random() else variant
            val key = when (randomVariant) {
                2 -> "sound_slidep_click2.mp3"
                3 -> "sound_slidep_click3.mp3"
                else -> "sound_slidep_click.mp3"
            }
            val soundId = soundMap[key] ?: soundMap["sound_slidep_click.mp3"] ?: return@runCatching
            pool.play(soundId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
    }

    /**
     * 播放切换相机的滑动机械音
     */
    fun playSwitch(context: Context? = null) {
        playSlideClick(context)
    }

    /**
     * 播放切换微动触感齿轮声 (使用清脆的轻量级微动音)
     */
    fun playSlideClick(context: Context? = null) {
        runCatching {
            if (!isInitialized && context != null) {
                init(context.applicationContext)
            }
            val pool = soundPool ?: return@runCatching
            val soundId = soundMap["sound_slidep_click.mp3"] ?: soundMap["sound_slidep_bg.mp3"] ?: return@runCatching
            pool.play(soundId, 0.45f, 0.45f, 1, 0, 1.2f)
        }
    }

    /**
     * 播放导出/完成机械提示音
     */
    fun playEnd(context: Context? = null) {
        runCatching {
            if (!isInitialized && context != null) {
                init(context.applicationContext)
            }
            val pool = soundPool ?: return@runCatching
            val soundId = soundMap["sound_slidep_end.mp3"] ?: return@runCatching
            pool.play(soundId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
    }
}

