package com.mrkuzumi.polish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 单日磨剑记录
 */
data class Record(
    val dateIso: String,           // "2026-08-08"
    val count: Int = 0,            // 🦌 次数
    val timestamps: List<Long> = emptyList(), // 每次点击的 epoch millis
    val dish: String = "",         // 下饭菜
    val hand: String = "",         // "left" | "right" | ""
)

/**
 * 基于内部存储 JSON 文件的轻量持久化层（无 Room 依赖）。
 * 数据量极小（每年 ≤ 366 条），全量加载完全可行。
 */
object RecordRepository {
    private const val FILE_NAME = "polish_records.json"

    // ---- 全量加载 ----

    fun loadAll(context: Context): Map<String, Record> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyMap()
        return try {
            val json = JSONObject(file.readText())
            val result = mutableMapOf<String, Record>()
            for (key in json.keys()) {
                val obj = json.getJSONObject(key)
                val ts = obj.optJSONArray("timestamps")
                result[key] = Record(
                    dateIso = key,
                    count = obj.optInt("count", 0),
                    timestamps = ts?.let { arr -> (0 until arr.length()).map { arr.getLong(it) } } ?: emptyList(),
                    dish = obj.optString("dish", ""),
                    hand = obj.optString("hand", ""),
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // ---- 单条查询 ----

    fun get(context: Context, dateIso: String): Record? = loadAll(context)[dateIso]

    // ---- 保存 ----

    fun save(context: Context, record: Record) {
        val all = loadAll(context).toMutableMap()
        if (record.count == 0 && record.dish.isEmpty() && record.hand.isEmpty()) {
            // 空记录不存文件，避免膨胀
            all.remove(record.dateIso)
        } else {
            all[record.dateIso] = record
        }
        writeAll(context, all)
    }

    // ---- 批量覆写（高性能路径） ----

    fun saveAll(context: Context, records: Map<String, Record>) {
        writeAll(context, records)
    }

    // ---- 内部写文件 ----

    private fun writeAll(context: Context, records: Map<String, Record>) {
        val json = JSONObject()
        for ((_, r) in records) {
            json.put(r.dateIso, JSONObject().apply {
                put("count", r.count)
                put("timestamps", JSONArray().apply { r.timestamps.forEach { put(it) } })
                put("dish", r.dish)
                put("hand", r.hand)
            })
        }
        File(context.filesDir, FILE_NAME).writeText(json.toString(2))
    }
}
