package com.example.phototag.util

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import java.util.concurrent.ConcurrentHashMap

/**
 * 汉字拼音与首字母智能检索工具类。
 *
 * 特性：
 * 1. 支持中文全拼转换（如 "梗图" -> "gengtu"、"风景" -> "fengjing"）。
 * 2. 支持拼音首字母提取（如 "梗图" -> "gt"、"美食" -> "ms"）。
 * 3. 内存 ConcurrentHashMap 缓存加速，百万次检索 0 卡顿。
 */
object PinyinUtils {

    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
    }

    private val fullPinyinCache = ConcurrentHashMap<String, String>()
    private val initialsCache = ConcurrentHashMap<String, String>()

    /**
     * 获取汉字字符串的全拼（小写，不带声调）。
     */
    fun getFullPinyin(text: String): String {
        val clean = text.trimStart('#').trim()
        if (clean.isEmpty()) return ""
        return fullPinyinCache.getOrPut(clean) {
            val sb = StringBuilder()
            for (char in clean) {
                if (char.isChinese()) {
                    try {
                        val pinyins = PinyinHelper.toHanyuPinyinStringArray(char, format)
                        if (!pinyins.isNullOrEmpty()) {
                            sb.append(pinyins[0])
                        } else {
                            sb.append(char.lowercaseChar())
                        }
                    } catch (_: Exception) {
                        sb.append(char.lowercaseChar())
                    }
                } else {
                    sb.append(char.lowercaseChar())
                }
            }
            sb.toString()
        }
    }

    /**
     * 获取汉字字符串的拼音首字母缩写（小写）。
     */
    fun getInitials(text: String): String {
        val clean = text.trimStart('#').trim()
        if (clean.isEmpty()) return ""
        return initialsCache.getOrPut(clean) {
            val sb = StringBuilder()
            for (char in clean) {
                if (char.isChinese()) {
                    try {
                        val pinyins = PinyinHelper.toHanyuPinyinStringArray(char, format)
                        if (!pinyins.isNullOrEmpty() && pinyins[0].isNotEmpty()) {
                            sb.append(pinyins[0][0])
                        } else {
                            sb.append(char.lowercaseChar())
                        }
                    } catch (_: Exception) {
                        sb.append(char.lowercaseChar())
                    }
                } else {
                    sb.append(char.lowercaseChar())
                }
            }
            sb.toString()
        }
    }

    /**
     * 智能拼音匹配：
     * 1. 原始文本匹配 (如 "梗" in "#梗图")
     * 2. 全拼匹配 (如 "gengtu" 或 "geng" in "gengtu")
     * 3. 首字母匹配 (如 "gt" 或 "g" in "gt")
     */
    fun matchesQuery(text: String, query: String): Boolean {
        val q = query.trim().trimStart('#').lowercase()
        if (q.isEmpty()) return true

        val clean = text.trimStart('#')
        if (clean.contains(q, ignoreCase = true)) return true

        val full = getFullPinyin(clean)
        if (full.contains(q)) return true

        val initials = getInitials(clean)
        if (initials.contains(q)) return true

        return false
    }

    private fun Char.isChinese(): Boolean {
        val ub = Character.UnicodeBlock.of(this)
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
    }
}
