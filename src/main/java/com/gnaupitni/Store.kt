package com.gnaupitni

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val DEFAULT_TEMPLATE =
    "{\"bank\":\"{{bank}}\",\"amount\":{{amount}},\"unique_code\":\"{{unique_code}}\"," +
        "\"sender\":\"{{sender}}\",\"time\":\"{{time}}\",\"package\":\"{{package}}\"," +
        "\"raw_text\":\"{{raw_text}}\",\"hash\":\"{{hash}}\"}"

data class Destination(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val secret: String,
    val template: String = DEFAULT_TEMPLATE,
    val headers: String = "",
)

data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pkg: String,
    val keywords: String,
    val excludeKeywords: String = "",
    val digits: Int,
    val senderRegex: String,
    val destIds: List<String>,
    val enabled: Boolean = true,
)

data class LogItem(val time: Long, val text: String, val ok: Boolean)

data class Seen(val time: Long, val pkg: String, val title: String, val text: String)

object Store {
    private var sp: SharedPreferences? = null

    val destinations = mutableStateListOf<Destination>()
    val rules = mutableStateListOf<Rule>()
    val logs = mutableStateListOf<LogItem>()
    val recent = mutableStateListOf<Seen>()

    @Synchronized
    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences("gnaupitni", Context.MODE_PRIVATE)
        sp = p
        runCatching { parseDest(JSONArray(p.getString("dest", "[]"))) }
        runCatching { parseRules(JSONArray(p.getString("rules", "[]"))) }
        runCatching {
            val a = JSONArray(p.getString("logs", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                logs.add(LogItem(o.getLong("t"), o.getString("x"), o.getBoolean("ok")))
            }
        }
    }

    private fun parseDest(a: JSONArray) {
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            destinations.add(
                Destination(
                    o.getString("id"), o.getString("name"), o.getString("url"),
                    o.getString("secret"), o.getString("template"),
                    o.optString("headers", ""),
                ),
            )
        }
    }

    private fun parseRules(a: JSONArray) {
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            val ids = o.getJSONArray("destIds")
            rules.add(
                Rule(
                    o.getString("id"), o.getString("name"), o.getString("pkg"),
                    o.getString("keywords"), o.optString("exclude", ""), o.getInt("digits"),
                    o.getString("senderRegex"),
                    List(ids.length()) { ids.getString(it) }, o.getBoolean("enabled"),
                ),
            )
        }
    }

    @Synchronized
    fun exportJson(): String = JSONObject()
        .put("app", "intipuang")
        .put("dest", JSONArray(sp?.getString("dest", "[]") ?: "[]"))
        .put("rules", JSONArray(sp?.getString("rules", "[]") ?: "[]"))
        .toString()

    @Synchronized
    fun importJson(text: String): Boolean {
        return try {
            val o = JSONObject(text)
            if (o.optString("app") != "intipuang") return false
            val d = o.getJSONArray("dest")
            val r = o.getJSONArray("rules")
            destinations.clear()
            rules.clear()
            parseDest(d)
            parseRules(r)
            saveDest()
            saveRules()
            true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun upsertDest(d: Destination) {
        val i = destinations.indexOfFirst { it.id == d.id }
        if (i >= 0) destinations[i] = d else destinations.add(d)
        saveDest()
    }

    @Synchronized
    fun deleteDest(id: String) {
        destinations.removeAll { it.id == id }
        for (i in rules.indices) {
            val r = rules[i]
            if (id in r.destIds) rules[i] = r.copy(destIds = r.destIds - id)
        }
        saveDest()
        saveRules()
    }

    @Synchronized
    fun upsertRule(r: Rule) {
        val i = rules.indexOfFirst { it.id == r.id }
        if (i >= 0) rules[i] = r else rules.add(r)
        saveRules()
    }

    @Synchronized
    fun deleteRule(id: String) {
        rules.removeAll { it.id == id }
        saveRules()
    }

    @Synchronized
    fun log(text: String, ok: Boolean) {
        logs.add(0, LogItem(System.currentTimeMillis(), text, ok))
        while (logs.size > 100) logs.removeAt(logs.size - 1)
        saveLogs()
    }

    @Synchronized
    fun addSeen(s: Seen) {
        recent.add(0, s)
        while (recent.size > 40) recent.removeAt(recent.size - 1)
    }

    @Synchronized
    fun clearLogs() {
        logs.clear()
        saveLogs()
    }

    private fun saveDest() {
        val a = JSONArray()
        destinations.forEach {
            a.put(
                JSONObject().put("id", it.id).put("name", it.name).put("url", it.url)
                    .put("secret", it.secret).put("template", it.template).put("headers", it.headers),
            )
        }
        sp?.edit()?.putString("dest", a.toString())?.apply()
    }

    private fun saveRules() {
        val a = JSONArray()
        rules.forEach {
            a.put(
                JSONObject().put("id", it.id).put("name", it.name).put("pkg", it.pkg)
                    .put("keywords", it.keywords).put("exclude", it.excludeKeywords).put("digits", it.digits)
                    .put("senderRegex", it.senderRegex).put("enabled", it.enabled)
                    .put("destIds", JSONArray(it.destIds)),
            )
        }
        sp?.edit()?.putString("rules", a.toString())?.apply()
    }

    private fun saveLogs() {
        val a = JSONArray()
        logs.forEach { a.put(JSONObject().put("t", it.time).put("x", it.text).put("ok", it.ok)) }
        sp?.edit()?.putString("logs", a.toString())?.apply()
    }
}
