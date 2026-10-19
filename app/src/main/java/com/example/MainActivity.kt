package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WafiViewModel
import com.example.ui.components.FirstLaunchCurrencyDialog
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MainLedgerScreen
import com.example.ui.screens.PersonStatementScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

sealed class WafiScreen {
    object Main : WafiScreen()
    data class PersonStatement(val personId: Long) : WafiScreen()
    data class AddTransaction(val personId: Long? = null) : WafiScreen()
    object Settings : WafiScreen()
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: WafiViewModel = viewModel()
                val isAdsRemoved by viewModel.billingManager.isAdsRemoved.collectAsState()
                val isUnlocked by viewModel.securityManager.isUnlocked.collectAsState()
                val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

                // Trigger App Open Ad conditional check on app launch
                LaunchedEffect(isAdsRemoved) {
                    viewModel.adManager.loadAd(isAdsRemoved)
                    viewModel.adManager.showAdIfAvailable(this@MainActivity, isAdsRemoved)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isUnlocked) {
                        LockScreen(securityManager = viewModel.securityManager)
                    } else {
                        WafiAppNavigation(viewModel = viewModel)

                        // Onboarding dialog for first launch currency selection
                        if (isFirstLaunch) {
                            FirstLaunchCurrencyDialog(
                                onSelectCurrency = { selectedCurr ->
                                    viewModel.completeFirstLaunch(selectedCurr)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WafiAppNavigation(viewModel: WafiViewModel) {
    var currentScreen by remember { mutableStateOf<WafiScreen>(WafiScreen.Main) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is WafiScreen.Main -> {
                MainLedgerScreen(
                    viewModel = viewModel,
                    onNavigateToPerson = { id ->
                        currentScreen = WafiScreen.PersonStatement(id)
                    },
                    onNavigateToAddTransaction = { id ->
                        currentScreen = WafiScreen.AddTransaction(id)
                    },
                    onNavigateToSettings = {
                        currentScreen = WafiScreen.Settings
                    }
                )
            }
            is WafiScreen.PersonStatement -> {
                PersonStatementScreen(
                    personId = screen.personId,
                    viewModel = viewModel,
                    onBack = { currentScreen = WafiScreen.Main },
                    onAddTransaction = { id ->
                        currentScreen = WafiScreen.AddTransaction(id)
                    }
                )
            }
            is WafiScreen.AddTransaction -> {
                AddTransactionScreen(
                    initialPersonId = screen.personId,
                    viewModel = viewModel,
                    onBack = { currentScreen = WafiScreen.Main }
                )
            }
            is WafiScreen.Settings -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = WafiScreen.Main }
                )
            }
        }
    }
}
