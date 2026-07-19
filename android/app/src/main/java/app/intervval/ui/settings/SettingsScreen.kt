package app.intervval.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.intervval.domain.IntervalOption

/**
 * Settings. DeepSeek: polish to spec §10.3. Keep the system-screen buttons working.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onClose: () -> Unit) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Default interval", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntervalOption.entries.forEach { opt ->
                    FilterChip(
                        selected = s.intervalMinutes == opt.minutes,
                        onClick = { vm.setInterval(opt.minutes) },
                        label = { Text(opt.label.removePrefix("Every ")) }
                    )
                }
            }

            Text("Default snooze", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 30).forEach { m ->
                    FilterChip(
                        selected = s.snoozeMinutes == m,
                        onClick = { vm.setSnooze(m) },
                        label = { Text("$m min") }
                    )
                }
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = {
                    val i = if (android.os.Build.VERSION.SDK_INT >= 31)
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                    context.startActivity(i)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Fix exact-alarm permission") }

            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Battery optimization") }

            HorizontalDivider()
            Text("intervval 0.1.0-beta · by 0xruzk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
