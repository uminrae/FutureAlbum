package com.example.phototag.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.phototag.data.dao.PhotoDao
import com.example.phototag.data.dao.TagDao
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.PhotoTagCrossRef
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.exif.ExifUtil
import com.example.phototag.data.ocr.OcrManager
import com.example.phototag.data.relation.PhotoWithTags
import com.example.phototag.util.AutoLocationResolver
import com.example.phototag.util.ExifMetadataUtil
import com.example.phototag.util.exif.ExifSpoofPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val tagDao: TagDao
) {

    private val ocrManager = OcrManager(context)

    // ==================== MediaStore ContentObserver 实时监听 ====================

    private var contentObserver: ContentObserver? = null
    private var observerJob: Job? = null

    /**
     * 注册 MediaStore 变更监听器（监听系统外部删除/新增操作，防抖 800ms 自动增量校验）。
     */
    fun startObservingMediaStore(scope: CoroutineScope) {
        if (contentObserver != null) return

        val handler = Handler(Looper.getMainLooper())
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                observerJob?.cancel()
                observerJob = scope.launch(Dispatchers.IO) {
                    delay(800) // 防抖
                    try {
                        syncPhotosFromMediaStore()
                    } catch (_: Exception) {}
                }
            }
        }

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        try {
            context.contentResolver.registerContentObserver(uri, true, contentObserver!!)
        } catch (e: Exception) {
            android.util.Log.w("PhotoRepository", "Failed to register MediaStore content observer", e)
            contentObserver = null
        }
    }

    fun stopObservingMediaStore() {
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
            contentObserver = null
        }
        observerJob?.cancel()
        observerJob = null
    }

    // ==================== 手机物理文件夹 / 来源相册 (MediaStore Bucket) ====================

    private val _deviceBuckets = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.phototag.data.model.DeviceBucket>>(emptyList())
    val deviceBuckets: kotlinx.coroutines.flow.StateFlow<List<com.example.phototag.data.model.DeviceBucket>> = _deviceBuckets

    // ==================== Flow 查询 ====================

    fun getAllPhotosWithTags(): Flow<List<PhotoWithTags>> = photoDao.getAllPhotosWithTags()
    fun getPhotoWithTags(photoId: Long): Flow<PhotoWithTags?> = photoDao.getPhotoWithTags(photoId)
    fun getAllPhotos(): Flow<List<PhotoEntity>> = photoDao.getAllPhotos()
    fun getUntaggedPhotos(): Flow<List<PhotoEntity>> = photoDao.getUntaggedPhotos()
    fun getUntaggedPhotosFlow(): Flow<List<PhotoEntity>> = photoDao.getUntaggedPhotosFlow()
    fun getUntaggedCount(): Flow<Int> = photoDao.getUntaggedCount()
    fun getUntaggedPhotosCountFlow(): Flow<Int> = photoDao.getUntaggedPhotosCountFlow()
    fun getPhotosByTagId(tagId: Long): Flow<List<PhotoEntity>> = photoDao.getPhotosByTagId(tagId)

    /** 多标签 AND 查询（同时包含所有 tagId） */
    fun getPhotosByAllTagIds(tagIds: List<Long>): Flow<List<PhotoEntity>> =
        if (tagIds.isEmpty()) photoDao.getAllPhotos()
        else photoDao.getPhotosByAllTagIds(tagIds, tagIds.size)

    /** 多标签 OR 查询（包含任意一个 tagId） */
    fun getPhotosByAnyTagIds(tagIds: List<Long>): Flow<List<PhotoEntity>> =
        if (tagIds.isEmpty()) photoDao.getAllPhotos()
        else photoDao.getPhotosByAnyTagIds(tagIds)

    /** 仅父标签查询：包含父标签，但排除所有下属子标签 */
    fun getPhotosByParentTagExcludingChildren(parentTagId: Long, childTagIds: List<Long>): Flow<List<PhotoEntity>> =
        if (childTagIds.isEmpty()) getPhotosByAllTagIds(listOf(parentTagId))
        else photoDao.getPhotosByParentTagExcludingChildren(parentTagId, childTagIds)

    fun getPhotosByAllTags(tagNames: List<String>): Flow<List<PhotoEntity>> =
        if (tagNames.isEmpty()) photoDao.getAllPhotos()
        else photoDao.getPhotosByAllTags(tagNames, tagNames.size)

    /** 联合检索：标签名称 + OCR 离线文本 + 文件名 */
    fun searchPhotosByTextAndTags(query: String): Flow<List<PhotoEntity>> =
        photoDao.searchPhotosByTextAndTags(query)

    fun searchPhotosByTagName(query: String): Flow<List<PhotoEntity>> =
        photoDao.searchPhotosByTagName(query)

    fun getPhotoCount(): Flow<Int> = photoDao.getPhotoCount()

    /**
     * 高性能分页拉取未打标照片。
     */
    suspend fun getUntaggedPhotosPaged(limit: Int, offset: Int): List<PhotoEntity> =
        withContext(Dispatchers.IO) {
            photoDao.getUntaggedPhotosPaged(limit, offset)
        }

    // ==================== MediaStore 扫描（增量同步 + 差量清理死链 + 视频/GIF 识别） ====================

    suspend fun syncPhotosFromMediaStore() = withContext(Dispatchers.IO) {
        val scanned = scanMediaStore()
        if (scanned.isEmpty()) return@withContext
        val mediaStoreUris: Set<String> = scanned.map { it.uri }.toSet()

        // 1. 增量插入新照片 / 视频（IGNORE 冲突策略）
        photoDao.insertPhotos(scanned)

        // 2. 仅对缺少宽/高或时长的媒体进行轻量增量补齐（避免每次刷新全量逐条更新导致卡死）
        val unindexedMedia = photoDao.getPhotosNeedingMediaMeta()
        if (unindexedMedia.isNotEmpty()) {
            val scannedMap = scanned.associateBy { it.uri }
            for (item in unindexedMedia) {
                val fresh = scannedMap[item.uri]
                if (fresh != null) {
                    photoDao.updatePhotoMediaMeta(
                        uri = fresh.uri,
                        mimeType = fresh.mimeType,
                        durationMs = fresh.durationMs,
                        width = fresh.width,
                        height = fresh.height
                    )
                }
            }
        }

        // 3. 差量比对：只比对活跃照片，清理系统相册外部物理删除但 Room 仍残留的失效死链（严格保护回收站照片）
        val dbPhotos = photoDao.getActivePhotoIdAndUriList()
        if (dbPhotos.isNotEmpty() && mediaStoreUris.isNotEmpty()) {
            val candidateDead = dbPhotos.filter { it.uri !in mediaStoreUris }
            if (candidateDead.isNotEmpty()) {
                // 安全深度校验：对疑似死链通过 ContentResolver 快速探测系统 MediaStore 是否真实存在
                // 严格防御 Android 14+ READ_MEDIA_VISUAL_USER_SELECTED 部分授权或外置存储未就绪导致的误清空标签数据
                val trulyDeadIds = mutableListOf<Long>()
                for (candidate in candidateDead) {
                    val uri = android.net.Uri.parse(candidate.uri)
                    var existsInSystem = false
                    try {
                        context.contentResolver.query(
                            uri,
                            arrayOf(android.provider.MediaStore.MediaColumns._ID),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            existsInSystem = cursor.moveToFirst()
                        }
                    } catch (_: Exception) {
                        existsInSystem = false
                    }
                    if (!existsInSystem) {
                        try {
                            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                                existsInSystem = true
                            }
                        } catch (_: Exception) {
                            existsInSystem = false
                        }
                    }
                    if (!existsInSystem) {
                        trulyDeadIds.add(candidate.photoId)
                    }
                }

                if (trulyDeadIds.isNotEmpty() && (dbPhotos.size <= 5 || trulyDeadIds.size < dbPhotos.size * 0.75)) {
                    photoDao.deletePhotosAndRelations(trulyDeadIds)
                }
            }
        }

        // 4. 默认预装与绑定「#视频」与「#动图」官方专属标签（高精底层 SQL 秒级执行，0 内存压力）
        ensureMediaSpecialTagsApplied()

        // 5. 智能去重清洗：自动合并相同物理文件的多余重复记录并继承标签
        photoDao.deduplicateDuplicatePhotos()
    }

    /**
     * 自动预装系统专有相册与打标（#视频、#动图/GIF、#地点）。
     * 底层采用 SQLite 集合式批量写入，耗时低于 5ms，杜绝内存堆积。
     */
    suspend fun ensureMediaSpecialTagsApplied() = withContext(Dispatchers.IO) {
        try {
            var mediaCat = tagDao.getCategoryByName("媒体类型")
            if (mediaCat == null) {
                val catId = tagDao.insertCategory(CategoryEntity(name = "媒体类型", sortOrder = 1))
                mediaCat = CategoryEntity(categoryId = catId, name = "媒体类型", sortOrder = 1)
            }

            // 1. 预装 #视频 标签
            var videoTag = tagDao.getTagByName("#视频")
            if (videoTag == null) {
                val tagId = tagDao.insertTag(
                    TagEntity(
                        name = "#视频",
                        categoryId = mediaCat.categoryId,
                        colorHex = "#5C6BC0",
                        isSystem = true,
                        isPinned = true
                    )
                )
                videoTag = tagDao.getTagByName("#视频") ?: TagEntity(
                    tagId = tagId,
                    name = "#视频",
                    categoryId = mediaCat.categoryId,
                    colorHex = "#5C6BC0",
                    isSystem = true,
                    isPinned = true
                )
            }

            // 2. 预装 #动图 标签
            var gifTag = tagDao.getTagByName("#动图")
            if (gifTag == null) {
                val tagId = tagDao.insertTag(
                    TagEntity(
                        name = "#动图",
                        categoryId = mediaCat.categoryId,
                        colorHex = "#FF7043",
                        isSystem = true,
                        isPinned = true
                    )
                )
                gifTag = tagDao.getTagByName("#动图") ?: TagEntity(
                    tagId = tagId,
                    name = "#动图",
                    categoryId = mediaCat.categoryId,
                    colorHex = "#FF7043",
                    isSystem = true,
                    isPinned = true
                )
            }

            // 3. 预装 #地点 标签（地理足迹）
            var locCat = tagDao.getCategoryByName("地理足迹")
            if (locCat == null) {
                val catId = tagDao.insertCategory(CategoryEntity(name = "地理足迹", sortOrder = 2))
                locCat = CategoryEntity(categoryId = catId, name = "地理足迹", sortOrder = 2)
            }

            var locTag = tagDao.getTagByName("#地点")
            if (locTag == null) {
                val tagId = tagDao.insertTag(
                    TagEntity(
                        name = "#地点",
                        categoryId = locCat.categoryId,
                        colorHex = "#10B981",
                        isSystem = true,
                        isPinned = true
                    )
                )
                locTag = tagDao.getTagByName("#地点") ?: TagEntity(
                    tagId = tagId,
                    name = "#地点",
                    categoryId = locCat.categoryId,
                    colorHex = "#10B981",
                    isSystem = true,
                    isPinned = true
                )
            }

            // 4. 底层 SQLite 原生极速打标（单条 SQL 批量插入，杜绝 6000+ 实体载入内存引发的 GC 顿挫）
            if (videoTag != null && videoTag.tagId > 0) photoDao.autoTagAllVideos(videoTag.tagId)
            if (gifTag != null && gifTag.tagId > 0) photoDao.autoTagAllGifs(gifTag.tagId)
            if (locTag != null && locTag.tagId > 0) photoDao.autoTagAllLocations(locTag.tagId)
        } catch (_: Exception) {}
    }

    /**
     * 异步后台扫描并全量补齐历史老照片的 GPS 经纬度与海拔，并自动触发详细地址逆编码。
     */
    suspend fun syncMissingGpsMetadata() = withContext(Dispatchers.IO) {
        try {
            val pendingPhotos = photoDao.getPhotosNeedingGpsExtraction(limit = 1000)
            for ((index, photo) in pendingPhotos.withIndex()) {
                kotlinx.coroutines.yield()
                val locationInfo = ExifMetadataUtil.extractGpsCoordinatesAndAltitude(context, photo.uri)
                if (locationInfo != null) {
                    photoDao.updatePhotoCoordinatesAndAltitude(
                        photoId = photo.photoId,
                        latitude = locationInfo.latitude,
                        longitude = locationInfo.longitude,
                        altitude = locationInfo.altitude
                    )
                    // 立即解析地址：
                    val matchedPreset = ExifSpoofPresets.findMatchingPreset(locationInfo.latitude, locationInfo.longitude)
                    if (matchedPreset != null) {
                        val cleanAddr = matchedPreset.displayAddress.ifBlank { matchedPreset.name }
                        photoDao.updatePhotoAddress(photo.photoId, cleanAddr)
                    } else if (!locationInfo.description.isNullOrBlank() && !ExifMetadataUtil.isGarbageAddress(locationInfo.description)) {
                        photoDao.updatePhotoAddress(photo.photoId, locationInfo.description)
                    } else {
                        val resolved = AutoLocationResolver.resolveAddress(context, locationInfo.latitude, locationInfo.longitude)
                        if (!resolved.isNullOrBlank() && !ExifMetadataUtil.isGarbageAddress(resolved)) {
                            photoDao.updatePhotoAddress(photo.photoId, resolved)
                        }
                    }
                }
                if ((index + 1) % 20 == 0) {
                    delay(30)
                }
            }

            // 自愈清理本地数据库中残留的 ???? 问号乱码地址
            val allPhotos = photoDao.getAllPhotosList()
            val garbageList = allPhotos.filter {
                ExifMetadataUtil.isGarbageAddress(it.address) && it.latitude != null && it.longitude != null
            }
            for (gp in garbageList) {
                val matched = ExifSpoofPresets.findMatchingPreset(gp.latitude!!, gp.longitude!!)
                if (matched != null) {
                    photoDao.updatePhotoAddress(gp.photoId, matched.displayAddress.ifBlank { matched.name })
                }
            }

            // 重新补齐未逆编码的地址
            batchResolveMissingAddresses()
            ensureMediaSpecialTagsApplied()
        } catch (_: Exception) {}
    }

    /**
     * 批量自动为所有带经纬度但无地址的照片解析详细街道/建筑地址。
     */
    suspend fun batchResolveMissingAddresses() = withContext(Dispatchers.IO) {
        try {
            val pending = photoDao.getPhotosNeedingReverseGeocode(limit = 300)
            for ((index, photo) in pending.withIndex()) {
                kotlinx.coroutines.yield()
                resolveAndSavePhotoAddress(photo)
                if ((index + 1) % 15 == 0) {
                    delay(40)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * 单张图片加载失败（Dead URI）时即时清理（严格校验真实底层文件是否存在，防止临时加载异常引发误删与流重发）。
     */
    suspend fun onDeadPhotoDetected(photoId: Long, uri: String) = withContext(Dispatchers.IO) {
        try {
            val exists = try {
                context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { true } ?: false
            } catch (_: java.io.FileNotFoundException) {
                false
            } catch (_: Exception) {
                true // 权限/临时IO/并发等非确实文件丢失异常，保守视作有效，不误删
            }
            if (!exists) {
                photoDao.deletePhotosAndRelations(listOf(photoId))
            }
        } catch (_: Exception) {}
    }

    /**
     * 自动化 EXIF 元数据解析并生成智能标签（如 #2026年、#8月、#横屏壁纸、#长图截图、#GIF动图）。
     */
    suspend fun autoTagPhotosFromExif(photos: List<PhotoEntity>) = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) return@withContext

        try {
            var timeCat = tagDao.getCategoryByName("时间足迹 (EXIF)")
            if (timeCat == null) {
                val catId = tagDao.insertCategory(CategoryEntity(name = "时间足迹 (EXIF)", sortOrder = 10))
                timeCat = CategoryEntity(categoryId = catId, name = "时间足迹 (EXIF)", sortOrder = 10)
            }

            var attrCat = tagDao.getCategoryByName("智能属性 (EXIF)")
            if (attrCat == null) {
                val catId = tagDao.insertCategory(CategoryEntity(name = "智能属性 (EXIF)", sortOrder = 11))
                attrCat = CategoryEntity(categoryId = catId, name = "智能属性 (EXIF)", sortOrder = 11)
            }

            for ((index, photo) in photos.withIndex()) {
                kotlinx.coroutines.yield()
                val dbPhoto = photoDao.getPhotoByUri(photo.uri) ?: continue
                val exifData = ExifUtil.extractMetadata(
                    context = context,
                    uriString = photo.uri,
                    mimeType = photo.mimeType,
                    dateAddedSec = photo.dateAdded,
                    width = photo.width,
                    height = photo.height
                )

                val tagsToBind = mutableListOf<String>()
                exifData.yearTag?.let { tagsToBind.add(it) }
                exifData.monthTag?.let { tagsToBind.add(it) }
                exifData.orientationTag?.let { tagsToBind.add(it) }
                exifData.typeTag?.let { tagsToBind.add(it) }

                for (tagName in tagsToBind) {
                    val isTime = tagName.endsWith("年") || tagName.endsWith("月")
                    val targetCatId = if (isTime) timeCat.categoryId else attrCat.categoryId
                    val colorHex = if (isTime) "#7A90A4" else if (tagName.contains("壁纸")) "#7E9A86" else "#D99B6A"

                    var tag = tagDao.getTagByName(tagName)
                    if (tag == null) {
                        val newTagId = tagDao.insertTag(
                            TagEntity(
                                name = tagName,
                                categoryId = targetCatId,
                                colorHex = colorHex,
                                isSystem = true
                            )
                        )
                        if (newTagId > 0) {
                            photoDao.insertPhotoTagCrossRef(PhotoTagCrossRef(photoId = dbPhoto.photoId, tagId = newTagId))
                        }
                    } else {
                        photoDao.insertPhotoTagCrossRef(PhotoTagCrossRef(photoId = dbPhoto.photoId, tagId = tag.tagId))
                    }
                }

                if ((index + 1) % 20 == 0) {
                    delay(50)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * 后台静默运行 OCR：只识别 ocrText == null 的照片（低功耗让步调度，每 20 张休眠 50ms）。
     */
    suspend fun runOcrOnPendingPhotos() = withContext(Dispatchers.Default) {
        val pending = photoDao.getPhotosWithoutOcr()
        for ((index, photo) in pending.withIndex()) {
            kotlinx.coroutines.yield()
            try {
                val recognizedText = ocrManager.recognizeText(photo.uri)
                photoDao.updatePhotoOcrText(photo.photoId, recognizedText)
            } catch (_: Exception) {
                photoDao.updatePhotoOcrText(photo.photoId, "")
            }

            if ((index + 1) % 20 == 0) {
                delay(50)
            }
        }
    }

    private fun normalizeBucketName(rawName: String?, fileName: String): String {
        if (rawName.isNullOrBlank()) {
            return if (fileName.contains("screenshot", ignoreCase = true) || fileName.contains("screen_shot", ignoreCase = true)) "屏幕截图" else "其他文件"
        }
        return when {
            rawName.equals("Camera", ignoreCase = true) || rawName.equals("100ANDRO", ignoreCase = true) || rawName.equals("DCIM", ignoreCase = true) -> "相机"
            rawName.contains("screenshot", ignoreCase = true) || rawName.contains("截屏", ignoreCase = true) || rawName.contains("Screenshots", ignoreCase = true) -> "屏幕截图"
            rawName.contains("weixin", ignoreCase = true) || rawName.contains("wechat", ignoreCase = true) || rawName.contains("micromsg", ignoreCase = true) || rawName.contains("微信", ignoreCase = true) -> "微信"
            rawName.equals("Download", ignoreCase = true) || rawName.equals("Downloads", ignoreCase = true) || rawName.contains("下载", ignoreCase = true) -> "下载"
            rawName.equals("Pictures", ignoreCase = true) -> "图库"
            rawName.contains("douyin", ignoreCase = true) || rawName.contains("抖音", ignoreCase = true) -> "抖音"
            rawName.contains("xiaohongshu", ignoreCase = true) || rawName.contains("小红书", ignoreCase = true) -> "小红书"
            rawName.contains("weibo", ignoreCase = true) || rawName.contains("微博", ignoreCase = true) -> "微博"
            rawName.contains("qq", ignoreCase = true) -> "QQ"
            else -> rawName
        }
    }

    private fun scanMediaStore(): List<PhotoEntity> {
        val mediaList = mutableListOf<PhotoEntity>()
        val bucketMap = mutableMapOf<String, MutableList<String>>()

        // 1. 扫描图片资源（包含静图与 GIF 动图）
        try {
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )

            context.contentResolver.query(
                imageCollection,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val wCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val hCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                if (idCol != -1) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        )
                        val uriString = contentUri.toString()
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "" else ""
                        val rawMime = if (mimeCol != -1) cursor.getString(mimeCol) else null
                        val resolvedMime = if (!rawMime.isNullOrBlank()) {
                            rawMime
                        } else if (name.endsWith(".gif", ignoreCase = true)) {
                            "image/gif"
                        } else if (name.endsWith(".png", ignoreCase = true)) {
                            "image/png"
                        } else if (name.endsWith(".webp", ignoreCase = true)) {
                            "image/webp"
                        } else {
                            "image/jpeg"
                        }

                        val rawBucket = if (bucketCol != -1) cursor.getString(bucketCol) else null
                        val bucketName = normalizeBucketName(rawBucket, name)
                        bucketMap.getOrPut(bucketName) { mutableListOf() }.add(uriString)

                        mediaList.add(
                            PhotoEntity(
                                uri = uriString,
                                displayName = name,
                                dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else (System.currentTimeMillis() / 1000),
                                size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L,
                                mimeType = resolvedMime,
                                width = if (wCol != -1) cursor.getInt(wCol) else 0,
                                height = if (hCol != -1) cursor.getInt(hCol) else 0,
                                durationMs = null,
                                latitude = null,
                                longitude = null,
                                altitude = null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PhotoRepository", "扫描 MediaStore 图片媒体发生异常", e)
        }

        // 2. 扫描本地视频媒体资源
        try {
            val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

            context.contentResolver.query(
                videoCollection,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val wCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val hCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                if (idCol != -1) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                        val uriString = contentUri.toString()
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "" else ""
                        val rawMime = if (mimeCol != -1) cursor.getString(mimeCol) else null
                        val resolvedMime = if (!rawMime.isNullOrBlank()) {
                            rawMime
                        } else if (name.endsWith(".mp4", ignoreCase = true)) {
                            "video/mp4"
                        } else if (name.endsWith(".mov", ignoreCase = true)) {
                            "video/quicktime"
                        } else if (name.endsWith(".mkv", ignoreCase = true)) {
                            "video/x-matroska"
                        } else if (name.endsWith(".3gp", ignoreCase = true)) {
                            "video/3gpp"
                        } else {
                            "video/mp4"
                        }
                        val dur = if (durCol != -1) cursor.getLong(durCol) else 0L

                        val rawBucket = if (bucketCol != -1) cursor.getString(bucketCol) else null
                        val bucketName = normalizeBucketName(rawBucket, name)
                        bucketMap.getOrPut(bucketName) { mutableListOf() }.add(uriString)

                        mediaList.add(
                            PhotoEntity(
                                uri = uriString,
                                displayName = name,
                                dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else (System.currentTimeMillis() / 1000),
                                size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L,
                                mimeType = resolvedMime,
                                width = if (wCol != -1) cursor.getInt(wCol) else 0,
                                height = if (hCol != -1) cursor.getInt(hCol) else 0,
                                durationMs = if (dur > 0) dur else 0L
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. 构建设备文件夹相册列表（纯文字无 Emoji）
        val buckets = bucketMap.map { (name, uris) ->
            com.example.phototag.data.model.DeviceBucket(
                bucketName = name,
                count = uris.size,
                coverUri = uris.firstOrNull() ?: "",
                photoUris = uris
            )
        }.sortedWith(
            compareByDescending<com.example.phototag.data.model.DeviceBucket> { it.bucketName == "相机" }
                .thenByDescending { it.bucketName == "屏幕截图" }
                .thenByDescending { it.bucketName == "微信" }
                .thenByDescending { it.count }
        )
        _deviceBuckets.value = buckets

        return mediaList.sortedByDescending { it.dateAdded }
    }

    // ==================== 照片增删改查 ====================

    suspend fun insertPhoto(photo: PhotoEntity): Long = photoDao.insertPhoto(photo)
    suspend fun updatePhoto(photo: PhotoEntity) = photoDao.updatePhoto(photo)
    suspend fun deletePhoto(photo: PhotoEntity) = photoDao.deletePhoto(photo)

    suspend fun deletePhotosByIds(photoIds: List<Long>) = withContext(Dispatchers.IO) {
        photoDao.deletePhotosAndRelations(photoIds)
    }

    suspend fun deletePhotosByUris(uriStrings: List<String>) = withContext(Dispatchers.IO) {
        val photos = photoDao.getPhotosByUris(uriStrings)
        if (photos.isNotEmpty()) {
            val ids = photos.map { it.photoId }
            photoDao.deletePhotosAndRelations(ids)
        }
    }

    // ==================== 标签关联管理 ====================

    suspend fun addTagToPhoto(photoId: Long, tagId: Long) = withContext(Dispatchers.IO) {
        val tag = tagDao.getTagByIdSync(tagId)
        val parentId = tag?.parentId
        val tagsToApply = if (parentId != null) listOf(tagId, parentId) else listOf(tagId)
        for (id in tagsToApply) {
            photoDao.insertPhotoTagCrossRef(PhotoTagCrossRef(photoId, id))
            tagDao.incrementTagUsage(id)
        }
    }

    suspend fun removeTagFromPhoto(photoId: Long, tagId: Long) = withContext(Dispatchers.IO) {
        photoDao.deletePhotoTagCrossRef(PhotoTagCrossRef(photoId, tagId))
    }

    suspend fun isPhotoTagged(photoId: Long, tagId: Long): Boolean = withContext(Dispatchers.IO) {
        photoDao.isPhotoTagged(photoId, tagId) > 0
    }

    suspend fun clearAllTagsForPhoto(photoId: Long) = withContext(Dispatchers.IO) {
        photoDao.deleteAllTagsForPhoto(photoId)
    }

    suspend fun clearAllTagsForPhotos(photoIds: List<Long>) = withContext(Dispatchers.IO) {
        if (photoIds.isNotEmpty()) {
            photoDao.deleteCrossRefsForPhotos(photoIds)
        }
    }

    suspend fun batchTagPhotos(photoIds: List<Long>, tagIds: List<Long>) = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty() || tagIds.isEmpty()) return@withContext
        val allTags = tagDao.getAllTagsList()
        val tagMap = allTags.associateBy { it.tagId }
        val expandedTagIds = tagIds.flatMap { id ->
            val parentId = tagMap[id]?.parentId
            if (parentId != null) listOf(id, parentId) else listOf(id)
        }.distinct()

        photoDao.batchTagPhotos(photoIds, expandedTagIds)
        for (tagId in expandedTagIds) {
            tagDao.incrementTagUsage(tagId)
        }
    }

    suspend fun batchRemoveTagsFromPhotos(photoIds: List<Long>, tagIds: List<Long>) = withContext(Dispatchers.IO) {
        photoDao.batchRemoveTagsFromPhotos(photoIds, tagIds)
    }

    // ==================== 原子级标签转移与替换 (Move & Replace) ====================

    /**
     * 封装 transferPhotosBetweenTags 方法：
     * 1. 在单个 SQLite 事务中执行 Room transferPhotosToTag
     * 2. 更新目标标签与来源标签的 useCount 与 lastUsedTime
     * 3. 返回成功转移的照片数量
     */
    suspend fun transferPhotosBetweenTags(
        photoUris: List<String>,
        fromTagId: Long,
        toTagId: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = photoDao.transferPhotosToTag(photoUris, fromTagId, toTagId)
            if (count > 0) {
                tagDao.incrementTagUsage(toTagId)
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transferPhotosToTag(photoIds: List<Long>, fromTagId: Long?, toTagId: Long): Int = withContext(Dispatchers.IO) {
        val count = photoDao.transferPhotoIdsToTag(photoIds, fromTagId, toTagId)
        if (count > 0) {
            tagDao.incrementTagUsage(toTagId)
        }
        count
    }

    /**
     * 撤销 (Undo) 标签转移事务：
     * 自动将已转移到 toTagId 的照片逆向恢复关联到 originalFromTagId，并从 toTagId 移除。
     */
    suspend fun undoTransferPhotos(
        photoUris: List<String>,
        originalFromTagId: Long,
        originalToTagId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            photoDao.transferPhotosToTag(photoUris, fromTagId = originalToTagId, toTagId = originalFromTagId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 🧹 健康巡检与自愈 ====================

    suspend fun getAllPhotosList(): List<PhotoEntity> = withContext(Dispatchers.IO) {
        photoDao.getAllPhotosList()
    }

    suspend fun getDeadCrossRefCount(): Int = withContext(Dispatchers.IO) {
        photoDao.getDeadCrossRefCount()
    }

    suspend fun deleteDeadCrossRefs(): Int = withContext(Dispatchers.IO) {
        photoDao.deleteDeadCrossRefs()
    }

    // ==================== 🗑️ 7天安全回收站 (Soft Delete & Trash Bin) ====================

    /** 观察回收站照片列表 Flow */
    val trashPhotos: Flow<List<PhotoEntity>> = photoDao.getTrashPhotos()

    /** 观察回收站照片数量 Flow */
    val trashPhotoCount: Flow<Int> = photoDao.getTrashPhotoCount()

    /** 软删除：移入回收站（通过 URI 列表） */
    suspend fun softDeletePhotosByUris(
        uris: List<String>,
        timestamp: Long = System.currentTimeMillis()
    ): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0
        uris.chunked(400).sumOf { chunk ->
            photoDao.softDeletePhotosByUris(chunk, timestamp)
        }
    }

    /** 软删除：移入回收站（通过 ID 列表） */
    suspend fun softDeletePhotosByIds(
        photoIds: List<Long>,
        timestamp: Long = System.currentTimeMillis()
    ): Int = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext 0
        photoIds.chunked(400).sumOf { chunk ->
            photoDao.softDeletePhotosByIds(chunk, timestamp)
        }
    }

    /** 恢复回收站中的照片（通过 URI 列表） */
    suspend fun restorePhotosByUris(uris: List<String>): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0
        uris.chunked(400).sumOf { chunk ->
            photoDao.restorePhotosByUris(chunk)
        }
    }

    /** 恢复回收站中的照片（通过 ID 列表） */
    suspend fun restorePhotosByIds(photoIds: List<Long>): Int = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext 0
        photoIds.chunked(400).sumOf { chunk ->
            photoDao.restorePhotosByIds(chunk)
        }
    }

    /** 彻底硬删除：永久抹除数据库记录与标签关联 */
    suspend fun hardDeletePhotosByUris(uris: List<String>): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0
        uris.chunked(400).sumOf { chunk ->
            photoDao.hardDeletePhotosByUris(chunk)
        }
    }

    /** 物理级彻底删除：通过 URI 列表永久硬删除记录与标签关联 */
    suspend fun deletePermanentlyByUris(uris: List<String>): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0
        uris.chunked(400).sumOf { chunk ->
            photoDao.deletePermanentlyByUris(chunk)
        }
    }

    /** 物理级彻底删除：通过 ID 列表永久硬删除记录与标签关联 */
    suspend fun deletePermanentlyByIds(photoIds: List<Long>): Int = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext 0
        photoIds.chunked(400).sumOf { chunk ->
            photoDao.deletePermanentlyByIds(chunk)
        }
    }

    /** 一键清空回收站 */
    suspend fun clearAllTrash(): Int = withContext(Dispatchers.IO) {
        photoDao.clearAllTrash()
    }

    /** 更新照片在系统相册中的 URI（用于物理回收站恢复后同步 MediaStore 新生成的 ID） */
    suspend fun updatePhotoUri(oldUri: String, newUri: String): Int = withContext(Dispatchers.IO) {
        photoDao.updatePhotoUri(oldUri, newUri)
    }

    /** 安全更新或合并照片 URI，防止 MediaStore 重扫描生成的重复项并保证用户标签不丢失 */
    suspend fun safeUpdateOrMergePhotoUri(oldUri: String, newUri: String) = withContext(Dispatchers.IO) {
        photoDao.safeUpdateOrMergePhotoUri(oldUri, newUri)
    }

    /** 查询已超过 7 天的超期回收站照片 */
    suspend fun getExpiredTrashPhotos(expiryTimestamp: Long): List<PhotoEntity> = withContext(Dispatchers.IO) {
        photoDao.getExpiredTrashPhotos(expiryTimestamp)
    }

    /** 更新单张照片的手动标注地点 */
    suspend fun updatePhotoCustomLocation(photoId: Long, customLocation: String?) = withContext(Dispatchers.IO) {
        photoDao.updateCustomLocation(photoId, customLocation)
    }

    /** 更新单张照片的真实中文地址 */
    suspend fun updatePhotoAddress(photoId: Long, address: String) = withContext(Dispatchers.IO) {
        photoDao.updatePhotoAddress(photoId, address)
    }

    /** 更新单张照片的全量位置元数据（经纬度、地址、海拔） */
    suspend fun updatePhotoLocationFull(photoId: Long, latitude: Double?, longitude: Double?, address: String?, altitude: Double?) = withContext(Dispatchers.IO) {
        photoDao.updatePhotoLocationFull(photoId, latitude, longitude, address, altitude)
    }

    suspend fun getPhotoByUri(uri: String): PhotoEntity? = withContext(Dispatchers.IO) {
        photoDao.getPhotoByUri(uri)
    }

    /** 所有带有经纬度的有效照片流（用于照片足迹地图） */
    fun getAllPhotosWithCoordinatesFlow(): Flow<List<PhotoEntity>> = photoDao.getAllPhotosWithCoordinatesFlow()

    /**
     * 对单张照片执行全自动双通道逆地理编码解析，并将结果持久化回填至 Room 数据库。
     *
     * @param photo 目标照片实体
     * @return 解析成功后的中文地址，失败时返回 null
     */
    suspend fun resolveAndSavePhotoAddress(photo: PhotoEntity): String? = withContext(Dispatchers.IO) {
        // 1. 获取经纬度与海拔高程
        var lat = photo.latitude
        var lng = photo.longitude
        var alt = photo.altitude

        if (lat == null || lng == null || alt == null) {
            val coords = ExifMetadataUtil.extractGpsCoordinatesAndAltitude(context, photo.uri)
            if (coords != null) {
                if (lat == null) lat = coords.latitude
                if (lng == null) lng = coords.longitude
                if (alt == null) alt = coords.altitude
            }
        }

        // 若本地数据库已有 address 且包含高精建筑/道路/POI 标记且非乱码，直接返回
        if (!photo.address.isNullOrBlank() && !ExifMetadataUtil.isGarbageAddress(photo.address) && AutoLocationResolver.isDetailedAddress(photo.address)) {
            if ((photo.latitude == null && lat != null) || (photo.altitude == null && alt != null)) {
                photoDao.updatePhotoLocationFull(photo.photoId, lat, lng, photo.address, alt)
            }
            return@withContext photo.address
        }

        // 若无 GPS 坐标则无法逆编码
        if (lat == null || lng == null) {
            return@withContext photo.address
        }

        // 1. 优先从知名地标预设库匹配 (0ms 秒级识别)
        val matchedPreset = ExifSpoofPresets.findMatchingPreset(lat, lng)
        val finalAddr = if (matchedPreset != null) {
            matchedPreset.displayAddress.ifBlank { matchedPreset.name }
        } else {
            AutoLocationResolver.resolveAddress(context, lat, lng)
        }

        if (!finalAddr.isNullOrBlank() && !ExifMetadataUtil.isGarbageAddress(finalAddr)) {
            // 解析成功：持久化写入 Room 本地数据库
            photoDao.updatePhotoLocationFull(photo.photoId, lat, lng, finalAddr, alt)
        } else if (photo.latitude == null) {
            photoDao.updatePhotoLocationFull(photo.photoId, lat, lng, null, alt)
        }

        return@withContext finalAddr
    }
}
