package com.gnaupitni

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Parser {
    private val rp = Regex("(?i)(?:rp\\.?|idr)\\s*([0-9][0-9.,]*)")
    private val decimalTail = Regex("[.,]\\d{1,2}$")

    /** Ambil nominal pertama setelah "Rp" / "IDR". Contoh: "Rp 150.123,00" -> 150123 */
    fun parseAmount(text: String): Long? {
        val m = rp.find(text) ?: return null
        var s = m.groupValues[1].trimEnd('.', ',')
        s = decimalTail.replace(s, "")
        s = s.replace(".", "").replace(",", "")
        return s.toLongOrNull()
    }

    /** digits = 0 -> semua digit nominal; selain itu N digit terakhir. */
    fun uniqueCode(amount: Long, digits: Int): String {
        val s = amount.toString()
        return if (digits <= 0 || digits >= s.length) s else s.takeLast(digits)
    }

    fun skipReason(rule: Rule, text: String): String? {
        val words = rule.keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val bad = rule.excludeKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (bad.any { text.contains(it, ignoreCase = true) }) return "kena kata pengecualian"
        if (words.isNotEmpty() && words.none { text.contains(it, ignoreCase = true) }) return "kata kunci tidak cocok"
        return null
    }

    fun matches(rule: Rule, text: String): Boolean = skipReason(rule, text) == null

    fun sender(rule: Rule, text: String): String {
        if (rule.senderRegex.isBlank()) return ""
        return try {
            Regex(rule.senderRegex, RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

object Dispatcher {

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun esc(s: String): String {
        val q = JSONObject.quote(s)
        return q.substring(1, q.length - 1)
    }

    fun fill(template: String, values: Map<String, String>): String {
        var out = template
        for ((k, v) in values) out = out.replace("{{$k}}", esc(v))
        return out
    }

    private fun isoNow(t: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date(t))

    private val seenHash = LinkedHashSet<String>()
    private val lastText = HashMap<String, Long>()

    private fun duplicate(ruleId: String, hash: String, textKey: String): Boolean {
        synchronized(this) {
            val now = System.currentTimeMillis()
            if (!seenHash.add(ruleId + hash)) return true
            if (seenHash.size > 300) seenHash.remove(seenHash.first())
            if (lastText.size > 200) lastText.entries.removeAll { now - it.value > 300_000L }
            val last = lastText[ruleId + textKey]
            lastText[ruleId + textKey] = now
            return last != null && now - last < 20_000L
        }
    }

    fun handle(ctx: Context, pkg: String, title: String, text: String, time: Long) {
        val forPkg = Store.rules.toList().filter { it.pkg == pkg }
        if (forPkg.isEmpty()) {
            if (Parser.parseAmount(text) != null && !duplicate("norule", sha256("$pkg|$time|$text"), "$pkg|$text")) {
                Store.log("Ada notifikasi uang dari $pkg, tapi belum ada aturan untuk aplikasi ini", false)
            }
            return
        }
        for (rule in forPkg) {
            if (!rule.enabled) {
                Store.log("${rule.name}: aturan sedang dimatikan, notifikasi dilewati", false)
                continue
            }
            val why = Parser.skipReason(rule, text)
            if (why != null) {
                Store.log("${rule.name}: dilewati ($why)", false)
                continue
            }
            val amount = Parser.parseAmount(text)
            if (amount == null) {
                Store.log("${rule.name}: dilewati (nominal tidak terbaca)", false)
                continue
            }
            val hash = sha256("$pkg|$time|$text").take(32)
            if (duplicate(rule.id, hash, text)) {
                Store.log("${rule.name}: dilewati (notifikasi yang sama baru saja diproses)", false)
                continue
            }
            if (rule.destIds.isEmpty()) {
                Store.log("${rule.name}: Rp ${amount} terbaca, tapi aturan belum punya tujuan", false)
                continue
            }
            val values = mapOf(
                "bank" to rule.name,
                "amount" to amount.toString(),
                "unique_code" to Parser.uniqueCode(amount, rule.digits),
                "sender" to Parser.sender(rule, text),
                "time" to isoNow(time),
                "package" to pkg,
                "raw_text" to text,
                "title" to title,
                "hash" to hash,
            )
            for (destId in rule.destIds) {
                val dest = Store.destinations.find { it.id == destId }
                if (dest == null) {
                    Store.log("${rule.name}: tujuan sudah tidak ada", false)
                    continue
                }
                Store.log("${rule.name}: Rp $amount terbaca, mengirim ke ${dest.name}", true)
                enqueue(ctx, dest, fill(dest.template, values + ("secret" to dest.secret)), "${rule.name} Rp $amount → ${dest.name}", hash)
            }
        }
    }

    fun sendTest(ctx: Context, dest: Destination) {
        val now = System.currentTimeMillis()
        val hash = UUID.randomUUID().toString().replace("-", "")
        val values = mapOf(
            "bank" to "TES",
            "amount" to "150123",
            "unique_code" to "123",
            "sender" to "Tes Intipuang",
            "time" to isoNow(now),
            "package" to "com.gnaupitni.test",
            "raw_text" to "Tes: kamu menerima Rp 150.123 dari Tes Intipuang",
            "title" to "Tes",
            "hash" to hash,
        )
        enqueue(ctx, dest, fill(dest.template, values + ("secret" to dest.secret)), "TES → ${dest.name}", hash)
    }

    private fun enqueue(ctx: Context, dest: Destination, body: String, label: String, hash: String) {
        val req = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(workDataOf("dest" to dest.id, "body" to body, "label" to label))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx.applicationContext)
            .enqueueUniqueWork("$hash-${dest.id}", ExistingWorkPolicy.KEEP, req)
    }
}

private val REDIRECTS = setOf(301, 302, 303, 307, 308)

class SendWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private fun hmac(secret: String, msg: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(msg.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Store.init(applicationContext)
        val label = inputData.getString("label") ?: "?"
        val body = inputData.getString("body") ?: return@withContext Result.failure()
        val dest = Store.destinations.find { it.id == inputData.getString("dest") }
        if (dest == null) {
            Store.log("$label: tujuan sudah dihapus", false)
            return@withContext Result.failure()
        }
        if (!dest.url.startsWith("https://")) {
            Store.log("$label: URL harus diawali https://", false)
            return@withContext Result.failure()
        }
        try {
            var cur = URL(dest.url).openConnection() as HttpURLConnection
            cur.requestMethod = "POST"
            cur.instanceFollowRedirects = false
            cur.connectTimeout = 15000
            cur.readTimeout = 15000
            cur.doOutput = true
            cur.setRequestProperty("Content-Type", "application/json")
            dest.headers.lines().forEach { line ->
                val i = line.indexOf(':')
                if (i > 0) cur.setRequestProperty(line.substring(0, i).trim(), line.substring(i + 1).trim())
            }
            if (dest.secret.isNotBlank()) {
                val ts = (System.currentTimeMillis() / 1000).toString()
                cur.setRequestProperty("X-Timestamp", ts)
                cur.setRequestProperty("X-Signature", hmac(dest.secret, "$ts.$body"))
            }
            cur.outputStream.use { it.write(body.toByteArray()) }
            var code = cur.responseCode
            // Ikuti redirect secara manual: selalu dengan GET tanpa body dan tanpa header rahasia,
            // jadi POST asli tidak pernah dikirim ulang (Apps Script membalas POST lewat 302).
            var hops = 0
            while (code in REDIRECTS && hops < 4) {
                val loc = cur.getHeaderField("Location")
                cur.disconnect()
                if (loc.isNullOrBlank()) break
                val target = URL(cur.url, loc)
                if (target.protocol != "https") break
                val next = target.openConnection() as HttpURLConnection
                next.requestMethod = "GET"
                next.instanceFollowRedirects = false
                next.connectTimeout = 15000
                next.readTimeout = 15000
                code = next.responseCode
                cur = next
                hops++
            }
            val text = try {
                val st = if (code in 200..399) cur.inputStream else cur.errorStream
                st?.bufferedReader()?.use { it.readText().take(4000) }.orEmpty()
            } catch (e: Exception) {
                ""
            }
            cur.disconnect()
            val snippet = text.replace(Regex("\\s+"), " ").trim().take(150)
            val tail = if (snippet.isBlank()) "" else " $snippet"
            val rejected = Regex("\"ok\"\\s*:\\s*false").containsMatchIn(text)
            val disabled = Regex("\"status\"\\s*:\\s*\"disabled\"").containsMatchIn(text)
            val htmlFromScript = URL(dest.url).host.endsWith("google.com") &&
                (text.trimStart().startsWith("<!DOCTYPE", true) || text.trimStart().startsWith("<html", true))
            when {
                code in 200..299 && rejected -> {
                    Store.log("$label: ditolak penerima ($code)$tail", false)
                    Result.failure()
                }
                code in 200..299 && disabled -> {
                    Store.log("$label: penerima sedang dimatikan, belum diproses ($code)$tail", false)
                    Result.failure()
                }
                code in 200..299 && htmlFromScript -> {
                    Store.log("$label: balasan berupa halaman web, bukan JSON. Cek akses deploy Apps Script ($code)", false)
                    Result.failure()
                }
                code in 200..299 -> {
                    Store.log("$label: terkirim ($code)$tail", true)
                    Result.success()
                }
                code in 400..499 && code != 408 && code != 429 -> {
                    Store.log("$label: ditolak server ($code)$tail", false)
                    Result.failure()
                }
                runAttemptCount >= 6 -> {
                    Store.log("$label: gagal ($code), berhenti mencoba$tail", false)
                    Result.failure()
                }
                else -> {
                    Store.log("$label: gagal ($code), coba lagi$tail", false)
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.w("Gnaupitni", "send error", e)
            if (runAttemptCount >= 6) {
                Store.log("$label: error ${e.javaClass.simpleName}, berhenti mencoba", false)
                Result.failure()
            } else {
                Store.log("$label: error ${e.javaClass.simpleName}, coba lagi", false)
                Result.retry()
            }
        }
    }
}
