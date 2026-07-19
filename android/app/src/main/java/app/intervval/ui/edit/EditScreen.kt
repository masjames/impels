package app.intervval.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.intervval.domain.IntervalOption

/**
 * Quick Add / Edit. DeepSeek: refine to spec §10.2 (bottom-sheet feel, nicer
 * chips) without changing the VM API. Keep two-tap-fast capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: EditViewModel, onClose: () -> Unit) {
    val s by vm.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.isExisting) "Edit" else "Catch it") },
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
            OutlinedTextField(
                value = s.title,
                onValueChange = vm::onTitle,
                label = { Text("What") },
                singleLine = true,
                isError = s.error != null,
                supportingText = { s.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = s.fromWho,
                onValueChange = vm::onFromWho,
                label = { Text("Who asked (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Boss", "Coworker", "Me").forEach { who ->
                    AssistChip(onClick = { vm.onFromWho(who) }, label = { Text(who) })
                }
            }

            Text("Remind me", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntervalOption.entries.forEach { opt ->
                    FilterChip(
                        selected = s.intervalMinutes == opt.minutes,
                        onClick = { vm.onInterval(opt.minutes) },
                        label = { Text(opt.label.removePrefix("Every ")) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.save(focus = false, onDone = onClose) },
                enabled = !s.saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (s.isExisting) "Save" else "Catch it") }

            if (!s.isExisting) {
                OutlinedButton(
                    onClick = { vm.save(focus = true, onDone = onClose) },
                    enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save & focus") }
            }

            if (s.isExisting) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this reminder?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; vm.delete(onClose) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}
