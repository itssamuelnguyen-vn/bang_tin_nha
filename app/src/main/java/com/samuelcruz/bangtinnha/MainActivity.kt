package com.samuelcruz.bangtinnha

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samuelcruz.bangtinnha.fuel.FuelDetailScreen
import com.samuelcruz.bangtinnha.fx.FxDetailScreen
import com.samuelcruz.bangtinnha.gold.GoldDetailScreen
import com.samuelcruz.bangtinnha.power.PowerDetailScreen
import com.samuelcruz.bangtinnha.power.PowerSettingsScreen
import com.samuelcruz.bangtinnha.ui.BackupScreen
import com.samuelcruz.bangtinnha.ui.BangTinNhaTheme
import com.samuelcruz.bangtinnha.ui.BatteryScreen
import com.samuelcruz.bangtinnha.ui.CurrenciesScreen
import com.samuelcruz.bangtinnha.ui.FuelProductsScreen
import com.samuelcruz.bangtinnha.ui.GoldProductsScreen
import com.samuelcruz.bangtinnha.ui.HomeScreen
import com.samuelcruz.bangtinnha.ui.LocationScreen
import com.samuelcruz.bangtinnha.ui.NotificationsScreen
import com.samuelcruz.bangtinnha.ui.Routes
import com.samuelcruz.bangtinnha.ui.SettingsScreen
import com.samuelcruz.bangtinnha.ui.SourceStatusScreen
import com.samuelcruz.bangtinnha.weather.WeatherDetailScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BangTinNhaTheme {
                AskForNotifications()
                AppNavigation()
            }
        }
    }

    /** Opening the app refreshes whatever is overdue (pull-to-refresh refreshes everything). */
    override fun onStart() {
        super.onStart()
        val app = application as BangTinNhaApp
        app.appScope.launch { app.container.coordinator.refreshDue() }
    }
}

/** Android 13+ needs a runtime permission before any notification can be shown. */
@Composable
private fun AskForNotifications() {
    if (Build.VERSION.SDK_INT < 33) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

/** Screens and their routes (like config/routes.rb). */
@Composable
private fun AppNavigation() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    val open: (String) -> Unit = { nav.navigate(it) }
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onOpen = open) }
        composable(Routes.WEATHER) { WeatherDetailScreen(back) }
        composable(Routes.FX) { FxDetailScreen(back) }
        composable(Routes.FUEL) { FuelDetailScreen(back) }
        composable(Routes.POWER) { PowerDetailScreen(back) }
        composable(Routes.GOLD) { GoldDetailScreen(back) }
        composable(Routes.GOLD_PRODUCTS) { GoldProductsScreen(back) }
        composable(Routes.SETTINGS) { SettingsScreen(back, open) }
        composable(Routes.LOCATION) { LocationScreen(back) }
        composable(Routes.CURRENCIES) { CurrenciesScreen(back) }
        composable(Routes.FUEL_PRODUCTS) { FuelProductsScreen(back) }
        composable(Routes.POWER_AREA) { PowerSettingsScreen(back) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(back) }
        composable(Routes.BATTERY) { BatteryScreen(back) }
        composable(Routes.STATUS) { SourceStatusScreen(back) }
        composable(Routes.BACKUP) { BackupScreen(back) }
    }
}
