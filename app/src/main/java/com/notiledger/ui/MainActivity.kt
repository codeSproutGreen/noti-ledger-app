package com.notiledger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notiledger.ui.screens.FilterScreen
import com.notiledger.ui.screens.HomeScreen
import com.notiledger.ui.screens.SettingsScreen
import com.notiledger.ui.theme.NotiLedgerTheme
import com.notiledger.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 권한 결과 처리 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            NotiLedgerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // SMS 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
            permissions.add(Manifest.permission.READ_SMS)
        }

        // 알림 권한 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

enum class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("알림", Icons.Filled.Home, Icons.Outlined.Home),
    FILTER("필터", Icons.Filled.FilterList, Icons.Outlined.FilterList),
    SETTINGS("설정", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MainApp() {
    val viewModel: MainViewModel = viewModel()
    var selectedNav by remember { mutableStateOf(NavItem.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedNav == item) item.selectedIcon
                                else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedNav == item,
                        onClick = { selectedNav = item }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedNav) {
                NavItem.HOME -> HomeScreen(viewModel)
                NavItem.FILTER -> FilterScreen(viewModel)
                NavItem.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}
