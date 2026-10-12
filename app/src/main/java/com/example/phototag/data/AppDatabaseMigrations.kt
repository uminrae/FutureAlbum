package com.example.phototag.data

import android.database.Cursor
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 🛡️ 智能防崩溃万能数据库迁移策略集 (Universal Self-Healing Migrations)。
 *
 * 特性：
 * 1. 【自愈式字段校验 (PRAGMA table_info)】：执行 ALTER TABLE 前主动探测字段是否存在，杜绝重复添加异常或字段遗漏。
 * 2. 【全量版本直达迁移】：提供从任何旧版本 (1..11) 直升当前版本 (12) 的自愈式 Migration。
 * 3. 【无损保留数据】：所有老照片、自定义标签、分类与关联关系完全继承，杜绝数据清空。
 */
object AppDatabaseMigrations {

    private const val TAG = "DatabaseMigrations"

    /**
     * 探测表中是否存在指定列名
     */
    fun columnExists(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        var cursor: Cursor? = null
        try {
            cursor = db.query("PRAGMA table_info(`$tableName`)")
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    if (name.equals(columnName, ignoreCase = true)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "检查列 $tableName.$columnName 失败: ${e.message}")
        } finally {
            cursor?.close()
        }
        return false
    }

    /**
     * 安全添加列（仅在列不存在时执行）
     */
    fun safeAddColumn(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        columnDef: String
    ) {
        if (!columnExists(db, tableName, columnName)) {
            try {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDef")
                Log.d(TAG, "成功为表 $tableName 添加列: $columnName $columnDef")
            } catch (e: Exception) {
                Log.e(TAG, "为表 $tableName 添加列 $columnName 失败: ${e.message}")
            }
        }
    }

    /**
     * 执行全库自愈与对齐至版本 15 标准 Schema
     */
    fun selfHealDatabaseToVersion15(db: SupportSQLiteDatabase) {
        Log.i(TAG, "开始执行数据库自愈与结构对齐 (至版本 15)...")

        // 1. 确保 categories 表存在
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `categories` (
                `categoryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        safeAddColumn(db, "categories", "createdAt", "INTEGER NOT NULL DEFAULT 0")

        // 2. 确保 photos 表存在且列完全对齐
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `photos` (
                `photoId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uri` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `dateAdded` INTEGER NOT NULL,
                `size` INTEGER NOT NULL DEFAULT 0,
                `mimeType` TEXT NOT NULL DEFAULT '',
                `width` INTEGER NOT NULL DEFAULT 0,
                `height` INTEGER NOT NULL DEFAULT 0,
                `ocrText` TEXT DEFAULT NULL,
                `durationMs` INTEGER DEFAULT NULL,
                `isDeleted` INTEGER NOT NULL DEFAULT 0,
                `deletedAt` INTEGER DEFAULT NULL,
                `latitude` REAL DEFAULT NULL,
                `longitude` REAL DEFAULT NULL,
                `address` TEXT DEFAULT NULL,
                `altitude` REAL DEFAULT NULL,
                `customLocation` TEXT DEFAULT NULL
            )
        """.trimIndent())

        safeAddColumn(db, "photos", "ocrText", "TEXT DEFAULT NULL")
        safeAddColumn(db, "photos", "durationMs", "INTEGER DEFAULT NULL")
        safeAddColumn(db, "photos", "isDeleted", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "photos", "deletedAt", "INTEGER DEFAULT NULL")
        safeAddColumn(db, "photos", "latitude", "REAL DEFAULT NULL")
        safeAddColumn(db, "photos", "longitude", "REAL DEFAULT NULL")
        safeAddColumn(db, "photos", "address", "TEXT DEFAULT NULL")
        safeAddColumn(db, "photos", "altitude", "REAL DEFAULT NULL")
        safeAddColumn(db, "photos", "customLocation", "TEXT DEFAULT NULL")

        // 确保 photos 表关键索引存在
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_photos_uri` ON `photos` (`uri`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_ocrText` ON `photos` (`ocrText`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_isDeleted` ON `photos` (`isDeleted`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_deletedAt` ON `photos` (`deletedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_address` ON `photos` (`address`)")

        // 3. 确保 tags 表存在且列完全对齐
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tags` (
                `tagId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `parentId` INTEGER DEFAULT NULL,
                `isPinned` INTEGER NOT NULL DEFAULT 0,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `isArchived` INTEGER NOT NULL DEFAULT 0,
                `colorHex` TEXT NOT NULL DEFAULT '#7E9A86',
                `isSystem` INTEGER NOT NULL DEFAULT 0,
                `useCount` INTEGER NOT NULL DEFAULT 0,
                `lastUsedTime` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`categoryId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`parentId`) REFERENCES `tags`(`tagId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        safeAddColumn(db, "tags", "parentId", "INTEGER DEFAULT NULL")
        safeAddColumn(db, "tags", "isPinned", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "tags", "sortOrder", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "tags", "isArchived", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "tags", "colorHex", "TEXT NOT NULL DEFAULT '#7E9A86'")
        safeAddColumn(db, "tags", "isSystem", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "tags", "useCount", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "tags", "lastUsedTime", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "tags", "createdAt", "INTEGER NOT NULL DEFAULT 0")

        // 确保 tags 表关键索引存在
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_categoryId` ON `tags` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_parentId` ON `tags` (`parentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_isPinned` ON `tags` (`isPinned`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_sortOrder` ON `tags` (`sortOrder`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_isArchived` ON `tags` (`isArchived`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_lastUsedTime` ON `tags` (`lastUsedTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_useCount` ON `tags` (`useCount`)")

        // 4. 确保 photo_tag_cross_ref 表存在
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `photo_tag_cross_ref` (
                `photoId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`photoId`, `tagId`),
                FOREIGN KEY(`photoId`) REFERENCES `photos`(`photoId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`tagId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photo_tag_cross_ref_photoId` ON `photo_tag_cross_ref` (`photoId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photo_tag_cross_ref_tagId` ON `photo_tag_cross_ref` (`tagId`)")

        // 5. 确保 smart_albums 表存在
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `smart_albums` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `iconName` TEXT NOT NULL DEFAULT '✨',
                `colorHex` TEXT NOT NULL DEFAULT '#3B82F6',
                `ruleJson` TEXT NOT NULL DEFAULT '{}',
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 6. 确保 persons (人物主体表) 存在
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `persons` (
                `personId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `customCoverFaceUri` TEXT DEFAULT NULL,
                `tagId` INTEGER DEFAULT NULL,
                `photoCount` INTEGER NOT NULL DEFAULT 0,
                `representativeEmbeddings` TEXT NOT NULL DEFAULT '[]',
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        safeAddColumn(db, "persons", "customCoverFaceUri", "TEXT DEFAULT NULL")
        safeAddColumn(db, "persons", "tagId", "INTEGER DEFAULT NULL")
        safeAddColumn(db, "persons", "photoCount", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "persons", "representativeEmbeddings", "TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_persons_name` ON `persons` (`name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_persons_tagId` ON `persons` (`tagId`)")

        // 7. 确保 faces (单人脸检测记录表) 存在
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `faces` (
                `faceId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `photoId` INTEGER NOT NULL,
                `personId` INTEGER NOT NULL,
                `boundingBox` TEXT NOT NULL,
                `embedding` TEXT NOT NULL,
                `faceCropPath` TEXT DEFAULT NULL,
                `isManualExcluded` INTEGER NOT NULL DEFAULT 0,
                `detectedAt` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`photoId`) REFERENCES `photos`(`photoId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`personId`) REFERENCES `persons`(`personId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        safeAddColumn(db, "faces", "faceCropPath", "TEXT DEFAULT NULL")
        safeAddColumn(db, "faces", "isManualExcluded", "INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_faces_photoId` ON `faces` (`photoId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_faces_personId` ON `faces` (`personId`)")

        Log.i(TAG, "数据库自愈与结构对齐完成 (版本 16)！")
    }

    /**
     * 生成所有支持的 Migration 列表（包含单步步进与任意旧版本直升 17）
     */
    fun getAllMigrations(): Array<Migration> {
        val migrations = mutableListOf<Migration>()

        // 1. 任意历史版本 (1..16) -> 17 的直达自愈式迁移
        for (fromVersion in 1..16) {
            migrations.add(object : Migration(fromVersion, 17) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    Log.i(TAG, "正在执行从版本 $fromVersion 直升至版本 17 的自愈式迁移...")
                    selfHealDatabaseToVersion17(db)
                }
            })
        }

        // 2. 传统单步步进迁移作为补充
        for (v in 1..16) {
            migrations.add(object : Migration(v, v + 1) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    selfHealDatabaseToVersion17(db)
                }
            })
        }

        return migrations.toTypedArray()
    }

    fun selfHealDatabaseToVersion17(db: SupportSQLiteDatabase) {
        selfHealDatabaseToVersion15(db)
    }
}
