package app.impels.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.impels.domain.IntervalOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: EditViewModel, onClose: () -> Unit) {
    val s by vm.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!s.isExisting) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (s.isExisting) "Edit" else "Catch it",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = s.title,
                onValueChange = vm::onTitle,
                label = { Text("What") },
                placeholder = { Text("e.g. Review the Q3 report") },
                singleLine = true,
                isError = s.error != null,
                supportingText = { s.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_what").focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = s.fromWho,
                    onValueChange = vm::onFromWho,
                    label = { Text("Who asked (optional)") },
                    placeholder = { Text("Coworker, boss, me...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("field_who"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Boss", "Coworker", "Me").forEach { who ->
                        AssistChip(
                            onClick = { vm.onFromWho(who) },
                            label = { Text(who) },
                            modifier = Modifier.testTag("chip_who_$who")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Remind me in",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IntervalOption.entries.forEach { opt ->
                        FilterChip(
                            selected = s.intervalMinutes == opt.minutes,
                            onClick = { vm.onInterval(opt.minutes) },
                            label = { Text(opt.label.removePrefix("Every ")) },
                            modifier = Modifier.testTag("chip_int_${opt.minutes}")
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.save(focus = false, onDone = onClose) },
                enabled = !s.saving,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save")
            ) {
                Text(
                    if (s.isExisting) "Save" else "Catch it",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!s.isExisting) {
                OutlinedButton(
                    onClick = { vm.save(focus = true, onDone = onClose) },
                    enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_focus")
                ) {
                    Text("Save & focus", fontWeight = FontWeight.SemiBold)
                }
            }

            if (s.isExisting) {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this reminder?") },
            text = {
                Text(
                    "This cannot be undone. The reminder and all its scheduled nags will be removed."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; vm.delete(onClose) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
