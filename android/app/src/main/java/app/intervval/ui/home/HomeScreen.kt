package app.intervval.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.intervval.domain.IntervalOption
import app.intervval.domain.Reminder

/**
 * Functional Home/Focus screen. DeepSeek: elevate to spec §10.1 visuals
 * (bigger focus card, who-chips, Done section polish) without changing the VM API.
 */
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("intervval") },
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
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add reminder") },
                text = { Text("Get it down") }
            )
        }
    ) { padding ->
        if (state.focused == null && state.queue.isEmpty() && state.done.isEmpty()) {
            EmptyState(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.focused?.let { f ->
                item {
                    FocusedCard(
                        reminder = f,
                        onDone = { vm.markDone(f.id) },
                        onSnooze = { vm.snooze(f.id, defaultSnooze) },
                        onEdit = { onEdit(f.id) },
                        onUnfocus = { vm.clearFocus() }
                    )
                }
            }
            if (state.queue.isNotEmpty()) {
                item { SectionLabel("Up next") }
                items(state.queue, key = { it.id }) { r ->
                    ReminderRow(
                        reminder = r,
                        onClick = { onEdit(r.id) },
                        onFocus = { vm.setFocus(r.id) },
                        onDone = { vm.markDone(r.id) }
                    )
                }
            }
            if (state.done.isNotEmpty()) {
                item { SectionLabel("Done") }
                items(state.done, key = { "done-${it.id}" }) { r ->
                    DoneRow(reminder = r, onRestore = { vm.restore(r.id) }, onDelete = { vm.delete(r.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun FocusedCard(
    reminder: Reminder,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onUnfocus: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FOCUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(reminder.title, style = MaterialTheme.typography.headlineSmall)
            reminder.fromWho?.let { AssistChip(onClick = {}, label = { Text("From $it") }) }
            Text(IntervalOption.fromMinutes(reminder.intervalMinutes).label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onDone) { Text("Done") }
                TextButton(onClick = onSnooze) { Text("Snooze") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onUnfocus) { Text("Unfocus") }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onDone: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                val sub = buildString {
                    reminder.fromWho?.let { append("From $it · ") }
                    append(IntervalOption.fromMinutes(reminder.intervalMinutes).label)
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onFocus) { Icon(Icons.Outlined.StarBorder, contentDescription = "Focus") }
            TextButton(onClick = onDone) { Text("Done") }
        }
    }
}

@Composable
private fun DoneRow(reminder: Reminder, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(reminder.title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRestore) { Text("Restore") }
        TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "Nothing on your plate. Catch the next ask.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
