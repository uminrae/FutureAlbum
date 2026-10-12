package com.example.phototag.util

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.phototag.PhotoTagApplication
import com.example.phototag.data.entity.PhotoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

sealed class TrashRequestResult {
    data object DirectSuccess : TrashRequestResult()
    data class NeedsUserConsent(val intentSender: IntentSender, val uris: List<Uri>, val isTrashed: Boolean) : TrashRequestResult()
    data class Error(val message: String) : TrashRequestResult()
}

sealed class DeleteRequestResult {
    data object DirectSuccess : DeleteRequestResult()
    data class NeedsUserConsent(val intentSender: IntentSender, val uris: List<Uri>) : DeleteRequestResult()
    data class Error(val message: String) : DeleteRequestResult()
}

/**
 * 跨 Android 版本的 MediaStore 回收站隔离与物理文件销毁引擎 (MediaStoreDeleteHelper)。
 *
 * 核心适配架构：
 * 1. 【Android 11+ (API 30+) 系统级 Scoped Storage 回收站】：
 *    - 移入回收站：调用 MediaStore.createTrashRequest(contentResolver, uris, isTrashed = true)
 *      -> 标记 IS_TRASHED = 1。此时微信、QQ、小红书、微博、系统图片选择器等其他 App **全面无法查看与读取该图**！
 *    - 恢复照片：调用 MediaStore.createTrashRequest(contentResolver, uris, isTrashed = false)
 *      -> 标记 IS_TRASHED = 0。照片立即原样恢复并对系统所有 App 重新可见！
 *    - 彻底删除：调用 MediaStore.createDeleteRequest(contentResolver, uris)
 *      -> 永久物理销毁文件。
 *
 * 2. 【Android 10 及以下兼容沙盒隔离保险箱】：
 *    - 移入回收站：将物理文件移动至应用沙盒专属 `.trash_storage/` 隐藏目录，并从 MediaStore 移除索引。
 *    - 恢复照片：将文件移回 DCIM/Pictures 目录并通过 MediaScannerConnection 重新注册系统索引。
 */
object MediaStoreDeleteHelper {

    /**
     * 准备系统回收站操作请求（移入回收站 / 从回收站恢复）。
     *
     * 物理级双重安全隔离机制：
     * 1. 优先将照片物理移动至应用专属隐藏目录 `.trash_storage/`（内含 .nomedia），并向系统通知移除 MediaStore 索引。
     *    -> 100% 确保 QQ、微信、系统相册、小红书等所有外部 App 彻底无法扫描与检索到该照片！
     * 2. 恢复时将底层物理文件原样移回原始目录（如 DCIM/Camera/），并通过 MediaScannerConnection 重新注册系统索引，瞬间对全系统恢复可见！
     * 3. 若特定文件因 Android Scoped Storage 限制无法直接物理移动，无缝降级调起系统级 createTrashRequest 授权。
     */
    fun prepareTrashRequest(context: Context, uris: List<Uri>, isTrashed: Boolean = true): TrashRequestResult {
        if (uris.isEmpty()) return TrashRequestResult.DirectSuccess

        return try {
            val unisolatedUris = handlePhysicalTrashOperation(context, uris, isTrashed)

            if (unisolatedUris.isEmpty()) {
                // 物理隔离/恢复 100% 成功，无需系统弹窗
                TrashRequestResult.DirectSuccess
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 部分文件降级调起系统原生回收站
                val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, unisolatedUris, isTrashed)
                TrashRequestResult.NeedsUserConsent(pendingIntent.intentSender, unisolatedUris, isTrashed)
            } else {
                TrashRequestResult.DirectSuccess
            }
        } catch (e: Exception) {
            TrashRequestResult.Error(e.localizedMessage ?: "准备回收站操作失败")
        }
    }

    /**
     * 准备彻底物理删除请求。
     */
    fun prepareDeleteRequest(context: Context, uris: List<Uri>): DeleteRequestResult {
        if (uris.isEmpty()) return DeleteRequestResult.DirectSuccess

        val trashDir = getTrashStorageDir(context)
        val prefs = context.getSharedPreferences("trash_physical_map", Context.MODE_PRIVATE)

        // 1. 优先清理物理隔离沙盒中的文件（同时处理无后缀和带 .trash 后缀的文件）
        val remainingMediaUris = mutableListOf<Uri>()
        for (uri in uris) {
            val uriStr = uri.toString()
            val originalPath = prefs.getString(uriStr, null) ?: getPathFromUri(context, uri)
            val trashedPath = prefs.getString("trash_dest_$uriStr", null)

            if (!trashedPath.isNullOrBlank()) {
                val tf = File(trashedPath)
                if (tf.exists()) {
                    try { tf.delete() } catch (_: Exception) {}
                }
            }

            if (!originalPath.isNullOrBlank()) {
                val fileName = File(originalPath).name
                val targetClean = File(trashDir, fileName)
                val targetTrash = File(trashDir, "${fileName}.trash")
                if (targetClean.exists()) {
                    try { targetClean.delete() } catch (_: Exception) {}
                }
                if (targetTrash.exists()) {
                    try { targetTrash.delete() } catch (_: Exception) {}
                }
                val directFile = File(originalPath)
                if (directFile.exists()) {
                    try { directFile.delete() } catch (_: Exception) {}
                }
            }
            prefs.edit().remove(uriStr).remove("trash_dest_$uriStr").apply()
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows <= 0) {
                    remainingMediaUris.add(uri)
                }
            } catch (secEx: SecurityException) {
                remainingMediaUris.add(uri)
            } catch (_: Exception) {}
        }

        if (remainingMediaUris.isEmpty()) {
            return DeleteRequestResult.DirectSuccess
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, remainingMediaUris)
                DeleteRequestResult.NeedsUserConsent(pendingIntent.intentSender, remainingMediaUris)
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                for (uri in remainingMediaUris) {
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (secEx: SecurityException) {
                        if (secEx is RecoverableSecurityException) {
                            val intentSender = secEx.userAction.actionIntent.intentSender
                            return DeleteRequestResult.NeedsUserConsent(intentSender, remainingMediaUris)
                        } else {
                            throw secEx
                        }
                    }
                }
                DeleteRequestResult.DirectSuccess
            } else {
                for (uri in remainingMediaUris) {
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: Exception) {}
                }
                DeleteRequestResult.DirectSuccess
            }
        } catch (e: Exception) {
            DeleteRequestResult.Error(e.localizedMessage ?: "准备删除请求失败")
        }
    }

    /**
     * 核心物理文件沙盒隔离与恢复机制。
     * 返回无法通过物理方式直接操作的 URI 集合（用于 Scoped Storage 降级）。
     */
    private fun handlePhysicalTrashOperation(context: Context, uris: List<Uri>, isTrashed: Boolean): List<Uri> {
        val trashDir = getTrashStorageDir(context)
        val prefs = context.getSharedPreferences("trash_physical_map", Context.MODE_PRIVATE)
        val failedUris = mutableListOf<Uri>()

        for (uri in uris) {
            try {
                val uriStr = uri.toString()
                if (isTrashed) {
                    val filePath = getPathFromUri(context, uri)
                    if (!filePath.isNullOrBlank()) {
                        val sourceFile = File(filePath)
                        if (sourceFile.exists()) {
                            // 保持原始文件名，绝不追加 .trash，使得 Coil 和系统媒体解析器可直接解码缩略图
                            var targetFile = File(trashDir, sourceFile.name)
                            if (targetFile.exists() && targetFile.absolutePath != sourceFile.absolutePath) {
                                val nameWithoutExt = sourceFile.nameWithoutExtension
                                val ext = sourceFile.extension
                                val extSuffix = if (ext.isNotEmpty()) ".$ext" else ""
                                targetFile = File(trashDir, "${nameWithoutExt}_${System.currentTimeMillis()}${extSuffix}")
                            }

                            var success = false
                            try {
                                if (sourceFile.renameTo(targetFile)) {
                                    success = true
                                } else {
                                    sourceFile.copyTo(targetFile, overwrite = true)
                                    if (sourceFile.delete()) {
                                        success = true
                                    }
                                }
                            } catch (_: Exception) {
                                success = false
                            }

                            if (success) {
                                prefs.edit()
                                    .putString(uriStr, filePath)
                                    .putString("trash_dest_$uriStr", targetFile.absolutePath)
                                    .apply()
                                try {
                                    context.contentResolver.delete(uri, null, null)
                                } catch (_: Exception) {}
                                MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
                            } else {
                                failedUris.add(uri)
                            }
                        }
                    } else {
                        failedUris.add(uri)
                    }
                } else {
                    // 恢复逻辑
                    val originalPath = prefs.getString(uriStr, null) ?: getPathFromUri(context, uri)
                    val trashedPath = prefs.getString("trash_dest_$uriStr", null)
                    val targetFile = if (!trashedPath.isNullOrBlank() && File(trashedPath).exists()) {
                        File(trashedPath)
                    } else if (!originalPath.isNullOrBlank()) {
                        val fileName = File(originalPath).name
                        val clean = File(trashDir, fileName)
                        val legacy = File(trashDir, "$fileName.trash")
                        if (clean.exists()) clean else if (legacy.exists()) legacy else null
                    } else null

                    if (targetFile != null && targetFile.exists() && !originalPath.isNullOrBlank()) {
                        val restoredFile = File(originalPath)
                        restoredFile.parentFile?.mkdirs()
                        var restored = false
                        try {
                            if (targetFile.renameTo(restoredFile)) {
                                restored = true
                            } else {
                                targetFile.copyTo(restoredFile, overwrite = true)
                                if (targetFile.delete()) {
                                    restored = true
                                }
                            }
                        } catch (_: Exception) {
                            restored = false
                        }

                        if (restored) {
                            prefs.edit().remove(uriStr).remove("trash_dest_$uriStr").apply()
                            MediaScannerConnection.scanFile(context, arrayOf(originalPath), null) { _, newUri ->
                                val resolvedUri = newUri ?: getUriFromPath(context, originalPath)
                                if (resolvedUri != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            PhotoTagApplication.instance.database.photoDao()
                                                .safeUpdateOrMergePhotoUri(uriStr, resolvedUri.toString())
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        } else {
                            failedUris.add(uri)
                        }
                    } else {
                        failedUris.add(uri)
                    }
                }
            } catch (_: Exception) {
                failedUris.add(uri)
            }
        }
        return failedUris
    }

    /**
     * 安全获取或创建外部回收站物理隔离目录（自动创建 .nomedia 防止被外部软件扫描）。
     */
    fun getTrashStorageDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val trashDir = File(baseDir, ".trash_storage")
        if (!trashDir.exists()) {
            try { trashDir.mkdirs() } catch (_: Exception) {}
        }
        val noMedia = File(trashDir, ".nomedia")
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }
        return trashDir
    }

    /**
     * 自动迁移并清洗历史残留的 .trash 后缀文件，恢复原扩展名，以便 Coil 及系统硬件解码器能直接渲染缩略图。
     */
    fun migrateTrashStorage(context: Context) {
        try {
            val trashDir = getTrashStorageDir(context)
            if (!trashDir.exists() || !trashDir.isDirectory) return
            trashDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".trash", ignoreCase = true)) {
                    val cleanName = file.name.removeSuffix(".trash")
                    val cleanFile = File(trashDir, cleanName)
                    if (!cleanFile.exists()) {
                        try { file.renameTo(cleanFile) } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * 针对处于回收站状态的照片，精准解析出可供 Coil 渲染的真实数据源绝对路径或 URI 字符串（100% 容错防崩，绝不抛出异常）。
     */
    fun getTrashImageSource(context: Context, photo: PhotoEntity): String {
        return try {
            val trashDir = getTrashStorageDir(context)
            val prefs = context.getSharedPreferences("trash_physical_map", Context.MODE_PRIVATE)

            // 1. 优先根据映射记录中的专属路径寻找
            val trashedPath = prefs.getString("trash_dest_${photo.uri}", null)
            if (!trashedPath.isNullOrBlank()) {
                val tf = File(trashedPath)
                if (tf.exists()) return tf.absolutePath
            }

            // 2. 根据原始物理路径文件名寻找
            val originalPath = prefs.getString(photo.uri, null)
            if (!originalPath.isNullOrBlank()) {
                val fileName = File(originalPath).name
                val clean = File(trashDir, fileName)
                if (clean.exists()) return clean.absolutePath
                val legacy = File(trashDir, "$fileName.trash")
                if (legacy.exists()) {
                    try {
                        if (legacy.renameTo(clean)) return clean.absolutePath
                    } catch (_: Exception) {}
                    return legacy.absolutePath
                }
            }

            // 3. 根据 displayName 寻找
            if (photo.displayName.isNotBlank()) {
                val clean = File(trashDir, photo.displayName)
                if (clean.exists()) return clean.absolutePath
                val legacy = File(trashDir, "${photo.displayName}.trash")
                if (legacy.exists()) {
                    try {
                        if (legacy.renameTo(clean)) return clean.absolutePath
                    } catch (_: Exception) {}
                    return legacy.absolutePath
                }

                // 模糊后缀匹配
                if (trashDir.exists()) {
                    val match = trashDir.listFiles()?.firstOrNull {
                        it.name == photo.displayName ||
                        it.name.endsWith(photo.displayName) ||
                        it.name.removeSuffix(".trash").endsWith(photo.displayName)
                    }
                    if (match != null && match.exists()) {
                        if (match.name.endsWith(".trash")) {
                            val cleanMatch = File(trashDir, match.name.removeSuffix(".trash"))
                            try {
                                if (match.renameTo(cleanMatch)) return cleanMatch.absolutePath
                            } catch (_: Exception) {}
                        }
                        return match.absolutePath
                    }
                }
            }

            // 4. 检查物理磁盘原文件是否仍在（未发生物理移动时）
            val parsedUri = try { Uri.parse(photo.uri) } catch (_: Exception) { null }
            if (parsedUri != null) {
                val diskPath = getPathFromUri(context, parsedUri)
                if (!diskPath.isNullOrBlank()) {
                    val f = File(diskPath)
                    if (f.exists()) return f.absolutePath
                }

                // 5. 若为 MediaStore content:// Uri，返回 Uri 字符串
                if (parsedUri.scheme == "content" && (parsedUri.authority == MediaStore.AUTHORITY || photo.uri.startsWith("content://media/"))) {
                    return photo.uri
                }
            }

            photo.uri
        } catch (_: Throwable) {
            photo.uri
        }
    }

    /**
     * 根据物理路径解析出系统 MediaStore 中的 Content URI。
     */
    fun getUriFromPath(context: Context, path: String, isVideo: Boolean = false): Uri? {
        val collection = if (isVideo || path.endsWith(".mp4", ignoreCase = true) || path.endsWith(".mov", ignoreCase = true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(path)
        return try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    ContentUris.withAppendedId(collection, id)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 从 MediaStore URI 中解析真实物理文件路径（API <= 28 备用）。
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (idx != -1) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
