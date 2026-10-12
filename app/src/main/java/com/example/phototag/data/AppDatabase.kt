package com.example.phototag.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.phototag.data.dao.PhotoDao
import com.example.phototag.data.dao.SmartAlbumDao
import com.example.phototag.data.dao.TagDao
import com.example.phototag.data.entity.CategoryEntity
import com.example.phototag.data.entity.PhotoEntity
import com.example.phototag.data.entity.PhotoTagCrossRef
import com.example.phototag.data.entity.SmartAlbumEntity
import com.example.phototag.data.entity.TagEntity
import com.example.phototag.data.model.SmartAlbumRule

/**
 * Room 数据库。
 */
@Database(
    entities = [
        PhotoEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        PhotoTagCrossRef::class,
        SmartAlbumEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao
    abstract fun smartAlbumDao(): SmartAlbumDao

    companion object {
        private const val DATABASE_NAME = "photo_tag_database"
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    buildDatabase(context).also { INSTANCE = it }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "标准数据库初始化失败，启动自愈防崩溃安全构建器", e)
                    buildSafeFallbackDatabase(context).also { INSTANCE = it }
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*AppDatabaseMigrations.getAllMigrations())
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDefaultData(db)
                    }
                })
                .build()
        }

        private fun buildSafeFallbackDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*AppDatabaseMigrations.getAllMigrations())
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        /**
         * 首次创建数据库时预填充经典分类、母子标签与精选智能动态相册。
         * 使用底层 SupportSQLiteDatabase 同步安全写入，杜绝冷启动多协程并发锁库闪退。
         */
        private fun seedDefaultData(db: SupportSQLiteDatabase) {
            try {
                fun insertCat(name: String, sortOrder: Int): Long {
                    try {
                        val values = ContentValues().apply {
                            put("name", name)
                            put("sortOrder", sortOrder)
                            put("createdAt", System.currentTimeMillis())
                        }
                        val id = db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
                        if (id > 0) return id
                        db.query("SELECT categoryId FROM categories WHERE name = ?", arrayOf(name)).use { cursor ->
                            if (cursor.moveToFirst()) {
                                return cursor.getLong(0)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "seed insertCat failed: $name", e)
                    }
                    return -1L
                }

                fun insertTag(name: String, catId: Long, parentId: Long? = null, isPinned: Boolean = false, colorHex: String = "#7E9A86"): Long {
                    if (catId <= 0) return -1L
                    try {
                        val values = ContentValues().apply {
                            put("name", name)
                            put("categoryId", catId)
                            if (parentId != null && parentId > 0) {
                                put("parentId", parentId)
                            }
                            put("isPinned", if (isPinned) 1 else 0)
                            put("sortOrder", 0)
                            put("isArchived", 0)
                            put("colorHex", colorHex)
                            put("createdAt", System.currentTimeMillis())
                            put("useCount", 0)
                            put("lastUsedTime", 0L)
                            put("isSystem", 0)
                            put("isLocked", 0)
                        }
                        val id = db.insert("tags", SQLiteDatabase.CONFLICT_IGNORE, values)
                        if (id > 0) return id
                        db.query("SELECT tagId FROM tags WHERE name = ?", arrayOf(name)).use { cursor ->
                            if (cursor.moveToFirst()) {
                                return cursor.getLong(0)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "seed insertTag failed: $name", e)
                    }
                    return -1L
                }

                fun insertSmartAlbum(name: String, colorHex: String, ruleJson: String): Long {
                    try {
                        val values = ContentValues().apply {
                            put("name", name)
                            put("iconName", "")
                            put("colorHex", colorHex)
                            put("ruleJson", ruleJson)
                            put("createdAt", System.currentTimeMillis())
                        }
                        return db.insert("smart_albums", SQLiteDatabase.CONFLICT_IGNORE, values)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "seed insertSmartAlbum failed: $name", e)
                    }
                    return -1L
                }

                // 1. 场景
                val sceneCatId = insertCat("场景", 1)
                val sceneryTagId = insertTag("#风景", sceneCatId, isPinned = true, colorHex = "#7E9A86")
                insertTag("#日落", sceneCatId, parentId = sceneryTagId, colorHex = "#D99B6A")
                insertTag("#山川", sceneCatId, parentId = sceneryTagId, colorHex = "#7E9A86")
                insertTag("#大海", sceneCatId, parentId = sceneryTagId, colorHex = "#7A90A4")

                val foodTagId = insertTag("#美食", sceneCatId, colorHex = "#D99B6A")
                insertTag("#甜点", sceneCatId, parentId = foodTagId, colorHex = "#B57F89")
                insertTag("#火锅", sceneCatId, parentId = foodTagId, colorHex = "#D99B6A")
                insertTag("#咖啡", sceneCatId, parentId = foodTagId, colorHex = "#BD7C5F")

                insertTag("#人像", sceneCatId, colorHex = "#B57F89")
                insertTag("#旅行", sceneCatId, isPinned = true, colorHex = "#7A90A4")
                insertTag("#夜景", sceneCatId, colorHex = "#5C6B73")

                // 2. 类型
                val typeCatId = insertCat("类型", 2)
                insertTag("#截图", typeCatId, colorHex = "#7A90A4")
                insertTag("#发票单据", typeCatId, colorHex = "#D4A373")
                insertTag("#文档", typeCatId, colorHex = "#8F87A1")
                insertTag("#壁纸", typeCatId, colorHex = "#7E9A86")

                // 3. 趣味
                val funCatId = insertCat("趣味", 3)
                val memeTagId = insertTag("#梗图", funCatId, colorHex = "#D99B6A")
                insertTag("#猫咪", funCatId, parentId = memeTagId, colorHex = "#BD7C5F")
                insertTag("#狗狗", funCatId, parentId = memeTagId, colorHex = "#D4A373")
                insertTag("#表情包", funCatId, parentId = memeTagId, colorHex = "#B57F89")

                // 4. 预置精选智能动态相册
                insertSmartAlbum("2026 精选", "#7E9A86", SmartAlbumRule(yearFilter = 2026).toJson())
                insertSmartAlbum("动图与视频", "#D99B6A", SmartAlbumRule(mediaTypeFilter = "GIF").toJson())
            } catch (e: Exception) {
                android.util.Log.e(TAG, "预填充默认数据异常", e)
            }
        }
    }
}
