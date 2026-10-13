package com.utilitylab.easydebloat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore by preferencesDataStore("easy_debloat_history")

class ActionHistoryStore(private val context: Context) {
    private val historyKey = stringPreferencesKey("history_json")

    val history: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        decode(prefs[historyKey].orEmpty())
    }

    suspend fun add(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = decode(prefs[historyKey].orEmpty()).toMutableList()
            current.add(0, entry)
            prefs[historyKey] = encode(current.take(MAX_ENTRIES))
        }
    }

    suspend fun clear() = context.historyDataStore.edit { it.remove(historyKey) }

    suspend fun snapshot(): List<HistoryEntry> = history.first()

    private fun encode(items: List<HistoryEntry>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("timestamp", item.timestamp)
                put("packageName", item.packageName)
                put("appName", item.appName)
                put("action", item.action.name)
                put("success", item.success)
                put("message", item.message)
            })
        }
        return array.toString()
    }

    private fun decode(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        HistoryEntry(
                            timestamp = obj.optLong("timestamp"),
                            packageName = obj.optString("packageName"),
                            appName = obj.optString("appName"),
                            action = runCatching { DebloatAction.valueOf(obj.optString("action")) }.getOrDefault(DebloatAction.DISABLE),
                            success = obj.optBoolean("success"),
                            message = obj.optString("message")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object { private const val MAX_ENTRIES = 500 }
}
