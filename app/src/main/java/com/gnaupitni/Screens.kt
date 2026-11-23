package com.gnaupitni

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.json.JSONObject
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val idLocale = Locale("id", "ID")

private fun rupiah(n: Long): String = "Rp " + NumberFormat.getInstance(idLocale).format(n)

private fun randomSecret() = UUID.randomUUID().toString().replace("-", "")

/* ------------------------------------------------------------------ */
/* BERANDA                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(12.dp),
    ) {
        Text(value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StepRow(done: Boolean, title: String, desc: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (done) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable
fun HomeScreen(tick: Int, goTo: (Int) -> Unit) {
    val ctx = LocalContext.current
    val accessOn = remember(tick) {
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    val batteryOk = remember(tick) {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }
    val ruleCount = Store.rules.size
    val destCount = Store.destinations.size
    val now = System.currentTimeMillis()
    val day = 86_400_000L
    val sent = Store.logs.count { it.ok && it.text.contains("terkirim") && now - it.time < day }
    val failed = Store.logs.count { !it.ok && !it.text.contains("dilewati") && now - it.time < day }
    val steps = listOf(accessOn, batteryOk, destCount > 0, ruleCount > 0)
    val done = steps.count { it }
    val ready = done == 4
    var sample by remember { mutableStateOf("Kamu menerima Rp 150.123 dari BUDI") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Green1, Green2)))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (ready) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    Column {
                        Text("Intipuang", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (ready) "Siap meneruskan transfer masuk" else "Selesaikan persiapan di bawah",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Aturan", ruleCount.toString(), Modifier.weight(1f))
                    StatTile("Tujuan", destCount.toString(), Modifier.weight(1f))
                    StatTile("Terkirim 24 jam", sent.toString(), Modifier.weight(1f))
                }
                if (failed > 0) {
                    Text(
                        "$failed catatan gagal dalam 24 jam terakhir. Lihat tab Log.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Persiapan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("$done/4", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier.fillMaxWidth(done / 4f).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Green1, Green2))),
                )
            }
            Spacer(Modifier.height(6.dp))
            StepRow(accessOn, "Akses notifikasi", "Izinkan Intipuang membaca notifikasi", "Aktifkan") {
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            StepRow(batteryOk, "Hemat baterai", "Pilih Tidak dibatasi supaya tidak dimatikan sistem", "Atur") {
                ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            StepRow(destCount > 0, "Tujuan webhook", "Alamat tempat kiriman dikirim", "Buat") { goTo(2) }
            StepRow(ruleCount > 0, "Aturan", "Notifikasi mana yang diteruskan", "Buat") { goTo(1) }
        }

        if (!accessOn) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Tombol akses notifikasi abu-abu?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    "Android 13 ke atas: buka Info Aplikasi Intipuang, tekan titik tiga di kanan atas, " +
                        "pilih \"Izinkan pengaturan terbatas\", lalu ulangi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        AppCard {
            Text("Uji baca nominal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Tempel teks notifikasi untuk melihat nominal dan kode unik yang akan terbaca.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = sample,
                onValueChange = { sample = it },
                label = { Text("Teks notifikasi") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            val amount = Parser.parseAmount(sample)
            if (amount == null) {
                Text("Nominal tidak terbaca. Harus ada \"Rp\" atau \"IDR\" diikuti angka.", color = MaterialTheme.colorScheme.error)
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(rupiah(amount), selected = true)
                    Chip("2 digit: ${Parser.uniqueCode(amount, 2)}")
                    Chip("3 digit: ${Parser.uniqueCode(amount, 3)}")
                    Chip("4 digit: ${Parser.uniqueCode(amount, 4)}")
                }
            }
        }
        AppCard {
            Text("Cadangan pengaturan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Salin semua aturan dan tujuan ke clipboard, lalu tempel di catatan pribadimu. " +
                    "Isinya memuat secret, jadi jangan dibagikan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("intipuang", Store.exportJson()))
                    Toast.makeText(ctx, "Pengaturan disalin", Toast.LENGTH_SHORT).show()
                }) { Text("Salin") }
                OutlinedButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val t = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                    val ok = Store.importJson(t)
                    Toast.makeText(ctx, if (ok) "Pengaturan dipulihkan" else "Isi clipboard bukan cadangan Intipuang", Toast.LENGTH_SHORT).show()
                }) { Text("Pulihkan dari clipboard") }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/* ------------------------------------------------------------------ */
/* ATURAN                                                              */
/* ------------------------------------------------------------------ */

@Composable
fun RulesScreen(prefill: Rule?, onUsed: () -> Unit) {
    var editing by remember { mutableStateOf<Rule?>(null) }
    var deleting by remember { mutableStateOf<Rule?>(null) }
    LaunchedEffect(prefill) {
        if (prefill != null) {
            editing = prefill
            onUsed()
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("Aturan", "Notifikasi mana yang diteruskan") {
            Button(onClick = {
                editing = Rule(
                    name = "", pkg = "", keywords = "masuk,menerima,diterima",
                    excludeKeywords = "keluar,dikirim,pembayaran berhasil,saldo",
                    digits = 3, senderRegex = "", destIds = emptyList(),
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambah")
            }
        }
        if (Store.rules.isEmpty()) {
            EmptyState(
                Icons.Filled.Settings,
                "Belum ada aturan",
                "Tekan Tambah, atau buka tab Intip lalu pilih notifikasi yang masuk.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            ) {
                items(Store.rules, key = { it.id }) { r ->
                    val names = r.destIds.mapNotNull { id -> Store.destinations.find { it.id == id }?.name }
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(r.name)
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(r.pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Switch(checked = r.enabled, onCheckedChange = { on -> Store.upsertRule(r.copy(enabled = on)) })
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(digitLabel(r.digits))
                            if (names.isEmpty()) Chip("Belum ada tujuan") else names.forEach { Chip("→ $it", selected = true) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editing = r }) { Text("Ubah") }
                            TextButton(
                                onClick = { deleting = r },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("Hapus") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { start ->
        RuleEditor(
            initial = start,
            onSave = { Store.upsertRule(it); editing = null },
            onClose = { editing = null },
        )
    }
    deleting?.let { r ->
        ConfirmDialog(
            title = "Hapus aturan?",
            text = "Aturan \"${r.name}\" akan dihapus.",
            confirmLabel = "Hapus",
            onConfirm = { Store.deleteRule(r.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RuleEditor(initial: Rule, onSave: (Rule) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var pkg by remember { mutableStateOf(initial.pkg) }
    var keywords by remember { mutableStateOf(initial.keywords) }
    var exclude by remember { mutableStateOf(initial.excludeKeywords) }
    var digits by remember { mutableIntStateOf(initial.digits) }
    var sender by remember { mutableStateOf(initial.senderRegex) }
    var dest by remember { mutableStateOf(initial.destIds.toSet()) }
    var picking by remember { mutableStateOf(false) }

    FullScreenEditor(
        title = if (initial.name.isBlank()) "Aturan baru" else "Ubah aturan",
        saveEnabled = name.isNotBlank() && pkg.isNotBlank(),
        onClose = onClose,
        onSave = {
            onSave(
                initial.copy(
                    name = name.trim(), pkg = pkg.trim(), keywords = keywords,
                    excludeKeywords = exclude, digits = digits,
                    senderRegex = sender, destIds = dest.toList(),
                ),
            )
        },
    ) {
        SectionLabel("Aplikasi sumber")
        OutlinedTextField(name, { name = it }, label = { Text("Nama tampilan (mis. GoPay)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pkg, { pkg = it }, label = { Text("Package aplikasi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { picking = true }) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pilih dari aplikasi terpasang")
        }

        SectionLabel("Filter notifikasi")
        OutlinedTextField(
            keywords, { keywords = it },
            label = { Text("Harus memuat salah satu kata") },
            supportingText = { Text("Pisahkan dengan koma. Kosong = semua notifikasi.") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            exclude, { exclude = it },
            label = { Text("Abaikan jika memuat") },
            supportingText = { Text("Untuk menyaring transfer keluar atau info saldo.") },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Kode unik")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 2, 3, 4).forEach { d ->
                Chip(digitLabel(d), selected = digits == d) { digits = d }
            }
        }
        Text(
            "Contoh: Rp 150.123 menghasilkan ${Parser.uniqueCode(150123L, digits)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionLabel("Nama pengirim (opsional)")
        OutlinedTextField(
            sender, { sender = it },
            label = { Text("Pola regex, ambil grup pertama") },
            supportingText = { Text("Kosongkan jika tidak perlu.") },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Kirim ke")
        if (Store.destinations.isEmpty()) {
            Text("Belum ada tujuan. Buat dulu di tab Tujuan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Store.destinations.forEach { d ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { dest = if (d.id in dest) dest - d.id else dest + d.id }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = d.id in dest, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                Column {
                    Text(d.name, fontWeight = FontWeight.SemiBold)
                    Text(hostOf(d.url), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (picking) {
        AppPicker(
            onPick = { picked ->
                pkg = picked.second
                if (name.isBlank()) name = picked.first
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun AppPicker(onPick: (Pair<String, String>) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val apps = remember {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(i, 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
    }
    var q by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Tutup") }
                    Text("Pilih aplikasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    q, { q = it },
                    label = { Text("Cari aplikasi") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(apps.filter { it.first.contains(q, true) || it.second.contains(q, true) }) { a ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onPick(a) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(a.first, 40.dp)
                            Column {
                                Text(a.first, fontWeight = FontWeight.SemiBold)
                                Text(a.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* TUJUAN                                                              */
/* ------------------------------------------------------------------ */

private const val TEMPLATE_RINGKAS =
    "{\"amount\":{{amount}},\"unique_code\":\"{{unique_code}}\",\"bank\":\"{{bank}}\",\"hash\":\"{{hash}}\"}"

private val PLACEHOLDERS = listOf(
    "bank", "amount", "unique_code", "sender", "time", "package", "title", "raw_text", "hash", "secret",
)

private fun templateProblem(t: String): String? {
    val sample = mapOf(
        "bank" to "Gopay", "amount" to "150123", "unique_code" to "23", "sender" to "Tes",
        "time" to "2026-01-01T00:00:00+07:00", "package" to "com.contoh", "title" to "Judul",
        "raw_text" to "Rp 150.123", "hash" to "abc123", "secret" to "rahasia",
    )
    return try {
        JSONObject(Dispatcher.fill(t, sample))
        null
    } catch (e: Exception) {
        "JSON belum valid: " + (e.message ?: "periksa tanda kutip dan koma").take(80)
    }
}

@Composable
fun DestScreen() {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<Destination?>(null) }
    var deleting by remember { mutableStateOf<Destination?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("Tujuan", "Alamat webhook penerima kiriman") {
            Button(onClick = { editing = Destination(name = "", url = "https://", secret = randomSecret()) }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambah")
            }
        }
        if (Store.destinations.isEmpty()) {
            EmptyState(
                Icons.Filled.Send,
                "Belum ada tujuan",
                "Tambahkan alamat https tempat kiriman dikirim, misalnya Worker Kastral atau webhook.site untuk tes.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            ) {
                items(Store.destinations, key = { it.id }) { d ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(d.name)
                            Column(Modifier.weight(1f)) {
                                Text(d.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(hostOf(d.url), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(if (d.secret.isBlank()) "Tanpa tanda tangan" else "Ditandatangani HMAC", selected = d.secret.isNotBlank())
                            val extra = d.headers.lines().count { it.contains(":") }
                            if (extra > 0) Chip("$extra header tambahan")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                Dispatcher.sendTest(ctx, d)
                                Toast.makeText(ctx, "Tes dikirim, cek tab Log", Toast.LENGTH_SHORT).show()
                            }) { Text("Tes kirim") }
                            TextButton(onClick = { editing = d }) { Text("Ubah") }
                            TextButton(
                                onClick = { deleting = d },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("Hapus") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { start ->
        DestEditor(
            initial = start,
            onSave = { Store.upsertDest(it); editing = null },
            onClose = { editing = null },
        )
    }
    deleting?.let { d ->
        ConfirmDialog(
            title = "Hapus tujuan?",
            text = "Tujuan \"${d.name}\" akan dihapus dan dilepas dari semua aturan.",
            confirmLabel = "Hapus",
            onConfirm = { Store.deleteDest(d.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun DestEditor(initial: Destination, onSave: (Destination) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var url by remember { mutableStateOf(initial.url) }
    var secret by remember { mutableStateOf(initial.secret) }
    var headers by remember { mutableStateOf(initial.headers) }
    var template by remember { mutableStateOf(initial.template) }

    FullScreenEditor(
        title = if (initial.name.isBlank()) "Tujuan baru" else "Ubah tujuan",
        saveEnabled = name.isNotBlank() && url.trim().startsWith("https://") && url.trim().length > 8,
        onClose = onClose,
        onSave = {
            onSave(
                initial.copy(
                    name = name.trim(), url = url.trim(), secret = secret.trim(),
                    template = template, headers = headers,
                ),
            )
        },
    ) {
        SectionLabel("Tujuan")
        OutlinedTextField(name, { name = it }, label = { Text("Nama (mis. Kastral)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            url, { url = it },
            label = { Text("URL webhook") },
            supportingText = { Text("Harus diawali https://") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Keamanan")
        OutlinedTextField(
            secret, { secret = it },
            label = { Text("Secret tanda tangan (HMAC)") },
            supportingText = { Text("Samakan dengan secret di sisi penerima. Kosong = tanpa tanda tangan.") },
            singleLine = true,
            trailingIcon = { TextButton(onClick = { secret = randomSecret() }) { Text("Acak") } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            headers, { headers = it },
            label = { Text("Header tambahan") },
            supportingText = { Text("Satu per baris, contoh: Authorization: Bearer abc123") },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Isi kiriman (JSON)")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Lengkap", selected = template == DEFAULT_TEMPLATE) { template = DEFAULT_TEMPLATE }
            Chip("Ringkas", selected = template == TEMPLATE_RINGKAS) { template = TEMPLATE_RINGKAS }
            Chip("Dengan token", selected = template == TEMPLATE_TOKEN) { template = TEMPLATE_TOKEN }
        }
        OutlinedTextField(
            template, { template = it },
            label = { Text("Template") },
            modifier = Modifier.fillMaxWidth(),
        )
        val tplProblem = templateProblem(template)
        if (tplProblem != null) {
            Text(tplProblem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else {
            Text(
                "JSON valid. Baris tetap seperti \"token\": \"abc\" boleh ditulis langsung di samping isian {{...}}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Ketuk untuk menambahkan isian ke template:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PLACEHOLDERS.forEach { key -> Chip("{{$key}}") { template += "{{$key}}" } }
        }
    }
}

/* ------------------------------------------------------------------ */
/* INTIP                                                               */
/* ------------------------------------------------------------------ */

@Composable
fun IntipScreen(onUse: (Seen) -> Unit) {
    val fmt = remember { SimpleDateFormat("HH:mm:ss", idLocale) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("Intip", "Notifikasi terakhir yang masuk ke HP") {
            if (Store.recent.isNotEmpty()) TextButton(onClick = { Store.recent.clear() }) { Text("Bersihkan") }
        }
        Text(
            "Hanya disimpan di memori dan hilang saat aplikasi ditutup. Tidak pernah dikirim ke mana pun.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Rekam semua", selected = Store.intipMode == 0) { Store.changeIntipMode(0) }
            Chip("Hanya ada nominal", selected = Store.intipMode == 1) { Store.changeIntipMode(1) }
            Chip("Jangan rekam", selected = Store.intipMode == 2) { Store.changeIntipMode(2) }
        }
        if (Store.recent.isEmpty()) {
            EmptyState(
                Icons.Filled.Search,
                "Belum ada notifikasi",
                "Pastikan akses notifikasi aktif, lalu kirim transfer kecil. Notifikasinya akan muncul di sini.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            ) {
                items(Store.recent.toList()) { s ->
                    val hasRule = Store.rules.any { it.pkg == s.pkg }
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(s.pkg.substringAfterLast('.'), 36.dp)
                            Column(Modifier.weight(1f)) {
                                Text(s.pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(fmt.format(Date(s.time)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            if (hasRule) Chip("Sudah ada aturan", selected = true)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (s.title.isNotBlank()) Text(s.title, fontWeight = FontWeight.Bold)
                        Text(s.text, style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onUse(s) }) { Text("Buat aturan dari ini") }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* LOG                                                                 */
/* ------------------------------------------------------------------ */

@Composable
fun LogScreen() {
    val fmt = remember { SimpleDateFormat("dd MMM HH:mm:ss", idLocale) }
    var filter by remember { mutableIntStateOf(0) }
    val all = Store.logs.toList()
    val shown = when (filter) {
        1 -> all.filter { it.ok }
        2 -> all.filter { !it.ok }
        else -> all
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("Log", "${all.size} catatan terakhir") {
            if (all.isNotEmpty()) TextButton(onClick = { Store.clearLogs() }) { Text("Hapus") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Semua", selected = filter == 0) { filter = 0 }
            Chip("Berhasil (${all.count { it.ok }})", selected = filter == 1) { filter = 1 }
            Chip("Gagal dan dilewati (${all.count { !it.ok }})", selected = filter == 2) { filter = 2 }
        }
        if (shown.isEmpty()) {
            EmptyState(Icons.Filled.Info, "Belum ada catatan", "Hasil pengiriman dan alasan notifikasi dilewati akan muncul di sini.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            ) {
                items(shown) { l ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.padding(top = 5.dp).size(10.dp).clip(CircleShape)
                                .background(if (l.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                        )
                        Column {
                            Text(l.text, style = MaterialTheme.typography.bodyMedium)
                            Text(fmt.format(Date(l.time)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}
