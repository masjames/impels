package app.impels

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.impels.ui.edit.EditScreen
import app.impels.ui.edit.EditViewModel
import app.impels.ui.home.HomeScreen
import app.impels.ui.home.HomeViewModel
import app.impels.ui.settings.SettingsScreen
import app.impels.ui.settings.SettingsViewModel
import app.impels.ui.theme.ImpelsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ImpelsApp).container

        // Deep link from a notification: focus that reminder.
        val openId = intent.getLongExtra("openReminderId", -1L)
        if (openId > 0) {
            lifecycleScope.launch { container.repository.setFocus(openId) }
        }

        // Ask for notification permission on launch (API 33+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            ImpelsTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {

                    composable("home") {
                        val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container.repository))
                        val snooze by container.settings.defaultSnoozeMinutes.collectAsState(initial = 10)
                        HomeScreen(
                            vm = vm,
                            defaultSnooze = snooze,
                            onAdd = { nav.navigate("edit?id=-1") },
                            onEdit = { id -> nav.navigate("edit?id=$id") },
                            onSettings = { nav.navigate("settings") }
                        )
                    }

                    composable(
                        route = "edit?id={id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: -1L
                        val vm: EditViewModel = viewModel(factory = EditViewModel.Factory(container.repository, container.settings))
                        LaunchedEffect(id) { vm.load(if (id > 0) id else null) }
                        EditScreen(vm = vm, onClose = { nav.popBackStack() })
                    }

                    composable("settings") {
                        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(container.settings))
                        SettingsScreen(vm = vm, onClose = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
