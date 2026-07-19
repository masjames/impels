package app.impels.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.impels.domain.IntervalOption
import app.impels.ui.components.PermissionBanner
import app.impels.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    defaultSnooze: Int,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var exactDismissed by remember { mutableStateOf(false) }
    val needsExact = !exactDismissed && Build.VERSION.SDK_INT >= 31 &&
        !(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    var notifDismissed by remember { mutableStateOf(false) }
    val needsNotif = !notifDismissed && Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    var doneExpanded by remember { mutableStateOf(false) }

    val showActive = state.focused != null || state.queue.isNotEmpty()
    val showEmpty = !showActive && state.done.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("impels", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Get it down") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (needsExact) {
                item(key = "b_exact") {
                    PermissionBanner(
                        message = "Turn on exact alarms so reminders fire on time.",
                        onDismiss = { exactDismissed = true },
                        actionLabel = "Fix",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            )
                        }
                    )
                }
            }
            if (needsNotif) {
                item(key = "b_notif") {
                    PermissionBanner(
                        message = "Notifications are off. Turn them on to get nagged.",
                        onDismiss = { notifDismissed = true },
                        actionLabel = "Fix",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                }
            }

            if (showEmpty) {
                item(key = "empty") {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Nothing on your plate.\nCatch the next ask.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onAdd) { Text("Get it down") }
                        }
                    }
                }
                return@LazyColumn
            }

            state.focused?.let { f ->
                item(key = "focused") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {}
                            Text(
                                f.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            f.fromWho?.let {
                                AssistChip(onClick = {}, label = { Text("From $it") })
                            }
                            Text(
                                IntervalOption.fromMinutes(f.intervalMinutes).label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(onClick = { vm.markDone(f.id) }) { Text("Done") }
                                TextButton(onClick = { vm.snooze(f.id, defaultSnooze) }) {
                                    Text("Snooze")
                                }
                                TextButton(onClick = { vm.clearFocus() }) { Text("Unfocus") }
                                TextButton(onClick = { onEdit(f.id) }) { Text("Edit") }
                            }
                        }
                    }
                }
            }

            if (state.queue.isNotEmpty()) {
                item(key = "queue_header") { SectionLabel("Up next") }
                items(state.queue, key = { it.id }) { r ->
                    Card(
                        onClick = { onEdit(r.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    r.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val sub = buildString {
                                    r.fromWho?.let { append("From $it · ") }
                                    append(IntervalOption.fromMinutes(r.intervalMinutes).label)
                                }
                                Text(
                                    sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { vm.setFocus(r.id) }) {
                                Icon(
                                    Icons.Outlined.StarBorder,
                                    contentDescription = "Focus"
                                )
                            }
                            TextButton(onClick = { vm.markDone(r.id) }) { Text("Done") }
                        }
                    }
                }
            }

            if (state.done.isNotEmpty()) {
                item(key = "done_header") {
                    TextButton(
                        onClick = { doneExpanded = !doneExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (doneExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (doneExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Done (${state.done.size})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
                if (doneExpanded) {
                    items(state.done, key = { it.id }) { r ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                r.title,
                                Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { vm.restore(r.id) }) { Text("Restore") }
                            TextButton(onClick = { vm.delete(r.id) }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
