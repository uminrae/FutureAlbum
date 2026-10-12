package com.example.phototag.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google ML Kit 离线文字识别管理器。
 *
 * 采用 [ChineseTextRecognizerOptions] 模型，支持中英文全自动离线识别。
 */
class OcrManager(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
    }

    /**
     * 对指定的图片 URI 进行离线文本识别。
     *
     * @return 识别出的文字内容；若识别失败或无文字则返回空字符串。
     */
    suspend fun recognizeText(uriString: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val uri = Uri.parse(uriString)
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            continuation.resume(visionText.text ?: "")
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            // 识别异常时返回空字符串，避免死循环重试
                            continuation.resume("")
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume("")
                }
            }
        }
    }
}
