package com.example.storagesentinel

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.storagesentinel.ui.components.ConfirmationDialog
import com.example.storagesentinel.ui.scanner.CleaningStateView
import com.example.storagesentinel.ui.scanner.DetailScreen
import com.example.storagesentinel.ui.scanner.PostCleanSummary
import com.example.storagesentinel.ui.scanner.ReadyStateView
import com.example.storagesentinel.ui.scanner.ResultsDisplay
import com.example.storagesentinel.ui.scanner.ScanningStateView
import com.example.storagesentinel.ui.settings.SettingsScreen
import com.example.storagesentinel.ui.theme.StorageSentinelTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StorageSentinelTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
                    ScannerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // After returning from settings, check the permission status again.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            viewModel.onPermissionResult(granted = Environment.isExternalStorageManager())
        } else {
            viewModel.onPermissionResult(granted = true) // On older versions, permission is granted at install time
        }
    }

    fun launchPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                viewModel.onScanRequest()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${context.packageName}".toUri()
                settingsLauncher.launch(intent)
            }
        } else {
            viewModel.onScanRequest()
        }
    }

    // Effect to show toasts for cleaning errors
    LaunchedEffect(uiState.cleaningErrors) {
        if (uiState.cleaningErrors.isNotEmpty()) {
            uiState.cleaningErrors.forEach {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
            viewModel.onErrorsShown()
        }
    }

    if (uiState.showConfirmDialog) {
        ConfirmationDialog(
            onConfirm = { viewModel.onCleanRequest() },
            onDismiss = { viewModel.onDismissConfirmDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Sentinel") },
                actions = {
                    IconButton(onClick = { viewModel.onShowSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.isShowingSettings) {
                SettingsScreen(onBack = { viewModel.onHideSettings() })
            } else {
                uiState.viewingDetailsFor?.let { junkType ->
                    val items = uiState.scanResults[junkType] ?: emptyList()
                    DetailScreen(
                        junkType = junkType,
                        items = items,
                        onBack = { viewModel.onBackFromDetails() },
                        onItemSelectionChanged = { item, isSelected ->
                            viewModel.onItemSelectionChanged(junkType, item, isSelected)
                        },
                        onAddToIgnoreList = { item ->
                            viewModel.onAddToIgnoreList(item)
                        }
                    )
                } ?: Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = when (uiState.scanState) {
                        ScanState.READY, ScanState.CLEAN_COMPLETE -> Arrangement.Center
                        else -> Arrangement.Top
                    }
                ) {
                    when (uiState.scanState) {
                        ScanState.READY -> ReadyStateView(
                            onScanClick = { launchPermissionRequest() },
                            onCreateDummyFilesClick = { viewModel.createDummyFiles() }
                        )
                        ScanState.SCANNING -> ScanningStateView(uiState.currentlyScanningPath)
                        ScanState.CLEANING -> CleaningStateView(uiState.currentlyCleaning?.label)
                        ScanState.FINISHED -> ResultsDisplay(
                            results = uiState.scanResults,
                            selectedCategories = uiState.selectionToClean,
                            onCleanClick = { viewModel.onShowConfirmDialog() },
                            onCategoryClick = { viewModel.onCategoryClick(it) },
                            onCategorySelectionChanged = { jt, isSelected -> viewModel.onCategorySelectionChanged(jt, isSelected) }
                        )
                        ScanState.CLEAN_COMPLETE -> PostCleanSummary(
                            cleanedItems = uiState.cleanedItems,
                            onFinish = { viewModel.onFinishCleaning() }
                        )
                    }
                }
            }
        }
    }
}
