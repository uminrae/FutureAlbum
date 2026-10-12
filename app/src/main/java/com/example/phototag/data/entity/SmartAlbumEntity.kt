package com.example.phototag.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.phototag.data.model.SmartAlbumRule

/**
 * 智能动态虚拟相册实体（Smart Dynamic Album）。
 *
 * 核心设计：
 * 不存储照片副本，仅持久化动态过滤规则（RuleJson）。
 * 只要底层相册扫描到新照片或照片被打上符合规则的标签，相册内内容自动响应式更新。
 */
@Entity(tableName = "smart_albums")
data class SmartAlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Folder",
    val colorHex: String = "#3B82F6",
    val ruleJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
) {
    val rule: SmartAlbumRule
        get() = SmartAlbumRule.fromJson(ruleJson)
}
