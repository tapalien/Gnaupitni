package com.gnaupitni

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent { App(tick) }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}

@Composable
fun App(tick: Int) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Beranda", "Aturan", "Tujuan", "Log")
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    titles.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {},
                            label = { Text(t) },
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.padding(pad).statusBarsPadding().padding(12.dp)) {
                when (tab) {
                    0 -> HomeScreen(tick)
                    1 -> RulesScreen()
                    2 -> DestScreen()
                    else -> LogScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(tick: Int) {
    val ctx = LocalContext.current
    val enabled = remember(tick) {
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    var sample by remember { mutableStateOf("Kamu menerima Rp 150.123 dari BUDI") }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Intipuang", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (enabled) "Akses notifikasi: AKTIF" else "Akses notifikasi: BELUM aktif", fontWeight = FontWeight.Bold)
                Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                    Text("Buka pengaturan akses notifikasi")
                }
                Text(
                    "Android 13+: kalau tombol akses notifikasi abu-abu (aplikasi dipasang dari APK), " +
                        "buka Info Aplikasi Intipuang, tekan titik tiga di kanan atas, pilih " +
                        "\"Izinkan pengaturan terbatas\", lalu ulangi.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }) {
                    Text("Atur hemat baterai (pilih Tidak dibatasi)")
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tes baca nominal", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = sample,
                    onValueChange = { sample = it },
                    label = { Text("Tempel teks notifikasi") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val amount = Parser.parseAmount(sample)
                if (amount == null) {
                    Text("Nominal tidak terbaca (harus ada \"Rp\" atau \"IDR\" diikuti angka)")
                } else {
                    Text("Nominal: $amount")
                    Text("Kode unik 2 digit: ${Parser.uniqueCode(amount, 2)}")
                    Text("Kode unik 3 digit: ${Parser.uniqueCode(amount, 3)}")
                    Text("Kode unik 4 digit: ${Parser.uniqueCode(amount, 4)}")
                }
            }
        }
    }
}

private fun digitLabel(d: Int) = if (d == 0) "semua digit" else "$d digit terakhir"

@Composable
fun RulesScreen() {
    var editing by remember { mutableStateOf<Rule?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            editing = Rule(
                name = "", pkg = "", keywords = "masuk,menerima,diterima",
                excludeKeywords = "keluar,dikirim,pembayaran berhasil,saldo",
                digits = 3, senderRegex = "", destIds = emptyList(),
            )
        }) { Text("+ Tambah aturan") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Store.rules, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, fontWeight = FontWeight.Bold)
                                Text(r.pkg, style = MaterialTheme.typography.bodySmall)
                                Text("Kode unik: ${digitLabel(r.digits)}", style = MaterialTheme.typography.bodySmall)
                                val names = r.destIds.mapNotNull { id -> Store.destinations.find { it.id == id }?.name }
                                Text("Kirim ke: " + if (names.isEmpty()) "(belum ada)" else names.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = r.enabled, onCheckedChange = { on -> Store.upsertRule(r.copy(enabled = on)) })
                        }
                        Row {
                            TextButton(onClick = { editing = r }) { Text("Ubah") }
                            TextButton(onClick = { Store.deleteRule(r.id) }) { Text("Hapus") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { start ->
        RuleDialog(
            initial = start,
            onSave = { Store.upsertRule(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
fun RuleDialog(initial: Rule, onSave: (Rule) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var pkg by remember { mutableStateOf(initial.pkg) }
    var keywords by remember { mutableStateOf(initial.keywords) }
    var exclude by remember { mutableStateOf(initial.excludeKeywords) }
    var digits by remember { mutableIntStateOf(initial.digits) }
    var sender by remember { mutableStateOf(initial.senderRegex) }
    var dest by remember { mutableStateOf(initial.destIds.toSet()) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aturan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama (mis. GoPay, BCA)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pkg, { pkg = it }, label = { Text("Package aplikasi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { picking = true }) { Text("Pilih dari aplikasi terpasang") }
                OutlinedTextField(keywords, { keywords = it }, label = { Text("Kata kunci (pisah koma, kosong = semua)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(exclude, { exclude = it }, label = { Text("Abaikan jika memuat (pisah koma)") }, modifier = Modifier.fillMaxWidth())
                Text("Kode unik yang dikirim:", fontWeight = FontWeight.Bold)
                listOf(0, 2, 3, 4).forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = digits == d, onClick = { digits = d })
                        Text(digitLabel(d))
                    }
                }
                OutlinedTextField(sender, { sender = it }, label = { Text("Regex nama pengirim (opsional, grup 1)") }, modifier = Modifier.fillMaxWidth())
                Text("Kirim ke tujuan:", fontWeight = FontWeight.Bold)
                if (Store.destinations.isEmpty()) Text("Buat tujuan dulu di tab Tujuan.")
                Store.destinations.forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = d.id in dest, onCheckedChange = { on -> dest = if (on) dest + d.id else dest - d.id })
                        Text(d.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && pkg.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(), pkg = pkg.trim(), keywords = keywords, excludeKeywords = exclude,
                            digits = digits, senderRegex = sender, destIds = dest.toList(),
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
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
fun AppPicker(onPick: (Pair<String, String>) -> Unit, onDismiss: () -> Unit) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih aplikasi") },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, label = { Text("Cari") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.height(320.dp)) {
                    items(apps.filter { it.first.contains(q, true) || it.second.contains(q, true) }) { a ->
                        TextButton(onClick = { onPick(a) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${a.first}\n${a.second}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

private fun randomSecret() = UUID.randomUUID().toString().replace("-", "")

@Composable
fun DestScreen() {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<Destination?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            editing = Destination(name = "", url = "https://", secret = randomSecret())
        }) { Text("+ Tambah tujuan") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Store.destinations, key = { it.id }) { d ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(d.name, fontWeight = FontWeight.Bold)
                        Text(d.url, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { Dispatcher.sendTest(ctx, d) }) { Text("Kirim tes") }
                            TextButton(onClick = { editing = d }) { Text("Ubah") }
                            TextButton(onClick = { Store.deleteDest(d.id) }) { Text("Hapus") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { start ->
        DestDialog(
            initial = start,
            onSave = { Store.upsertDest(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
fun DestDialog(initial: Destination, onSave: (Destination) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var url by remember { mutableStateOf(initial.url) }
    var secret by remember { mutableStateOf(initial.secret) }
    var template by remember { mutableStateOf(initial.template) }
    var headers by remember { mutableStateOf(initial.headers) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tujuan webhook") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama (mis. Kastral)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it }, label = { Text("URL (harus https)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(secret, { secret = it }, label = { Text("Secret tanda tangan (HMAC)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(headers, { headers = it }, label = { Text("Header tambahan (satu per baris, Nama: nilai)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(template, { template = it }, label = { Text("Template JSON") }, modifier = Modifier.fillMaxWidth())
                Text(
                    "Isian yang tersedia: {{bank}} {{amount}} {{unique_code}} {{sender}} {{time}} {{package}} {{title}} {{raw_text}} {{hash}}",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { template = DEFAULT_TEMPLATE }) { Text("Kembalikan template standar") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && url.startsWith("https://"),
                onClick = { onSave(initial.copy(name = name.trim(), url = url.trim(), secret = secret.trim(), template = template, headers = headers)) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun LogScreen() {
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.US) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { Store.clearLogs() }) { Text("Hapus log") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(Store.logs.toList()) { l ->
                Text("${if (l.ok) "OK" else "GAGAL"} ${fmt.format(Date(l.time))}  ${l.text}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
