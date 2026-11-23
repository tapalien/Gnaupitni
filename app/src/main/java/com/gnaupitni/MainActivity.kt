package com.gnaupitni

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge()
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
    var prefill by remember { mutableStateOf<Rule?>(null) }
    val tabs = listOf(
        "Beranda" to Icons.Filled.Home,
        "Aturan" to Icons.Filled.Settings,
        "Tujuan" to Icons.Filled.Send,
        "Intip" to Icons.Filled.Search,
        "Log" to Icons.Filled.Info,
    )
    AppTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.second, contentDescription = null) },
                            label = { Text(t.first) },
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> HomeScreen(tick) { tab = it }
                    1 -> RulesScreen(prefill) { prefill = null }
                    2 -> DestScreen()
                    3 -> IntipScreen { s ->
                        prefill = Rule(
                            name = s.pkg.substringAfterLast('.'),
                            pkg = s.pkg,
                            keywords = "masuk,menerima,diterima",
                            excludeKeywords = "keluar,dikirim,pembayaran berhasil,saldo",
                            digits = 3,
                            senderRegex = "",
                            destIds = emptyList(),
                        )
                        tab = 1
                    }
                    else -> LogScreen()
                }
            }
        }
    }
}
