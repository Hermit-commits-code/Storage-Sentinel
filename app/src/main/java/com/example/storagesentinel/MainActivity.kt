package com.example.storagesentinel

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storagesentinel.ui.scanner.CleaningStateView
import com.example.storagesentinel.ui.scanner.DetailScreen
import com.example.storagesentinel.ui.scanner.PostCleanSummary
import com.example.storagesentinel.ui.scanner.ReadyStateView
import com.example.storagesentinel.ui.scanner.ResultsDisplay
import com.example.storagesentinel.ui.scanner.ScanningStateView
import com.example.storagesentinel.ui.theme.StorageSentinelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StorageSentinelTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
                    ScannerScreen(context=LocalContext.current)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel = viewModel(), context: Context) {
    val uiState by viewModel.uiState.collectAsState()
    // The context from LocalContext is already available and is the correct one to use.
    val localContext = LocalContext.current

    // Define launchPermissionRequest here, so it's in the scope of settingsLauncher
    fun launchPermissionRequest(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = "package:${localContext.packageName}".toUri()
            launcher.launch(intent)
        } else {
            Toast.makeText(localContext, "This feature requires Android 11+.", Toast.LENGTH_LONG).show()
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            // Check for permission after the user returns from the settings screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                viewModel.onScanRequest(localContext)
            } else {
                // It's good practice to inform the user if permission was not granted
                Toast.makeText(localContext, "Permission is required to scan files.", Toast.LENGTH_LONG).show()
            }
        }
    )

    fun onScanOrRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            viewModel.onScanRequest(localContext)
        } else {
            // Pass the launcher to the function
            launchPermissionRequest(settingsLauncher)
        }
    }


    if (uiState.showConfirmDialog) {
        ConfirmationDialog(
            onConfirm = { viewModel.onCleanRequest(localContext) },
            onDismiss = { viewModel.onDismissConfirmDialog() }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Storage Sentinel") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.viewingDetailsFor?.let { junkType ->
                val items = uiState.scanResults[junkType] ?: emptyList()
                DetailScreen(
                    junkType = junkType,
                    items = items,
                    onBack = { viewModel.onBackFromDetails() },
                    onItemSelectionChanged = { item, isSelected ->
                        viewModel.onItemSelectionChanged(junkType, item, isSelected)
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
                    // Pass the function reference directly
                    ScanState.READY -> ReadyStateView(::onScanOrRequestPermission)
                    ScanState.SCANNING -> ScanningStateView()
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

@Composable
fun ConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to permanently delete the selected files? This action cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
