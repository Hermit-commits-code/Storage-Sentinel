package com.example.storagesentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.storagesentinel.ui.composables.AnalyticsScreen
import com.example.storagesentinel.ui.composables.DuplicateFilesScreen
import com.example.storagesentinel.ui.composables.JunkListScreen
import com.example.storagesentinel.ui.composables.PrivacyPolicyScreen
import com.example.storagesentinel.ui.composables.ScannerScreen
import com.example.storagesentinel.ui.composables.SettingsScreen
import com.example.storagesentinel.ui.theme.StorageSentinelTheme

class MainActivity : ComponentActivity() {
    
    private val billingManager by lazy { 
        (application as StorageSentinelApplication).billingManager 
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from splash screen theme to main theme
        setTheme(R.style.Theme_StorageSentinel)
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StorageSentinelTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "scanner") {
                        composable("scanner") { 
                            ScannerScreen(
                                billingManager = billingManager,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToAnalytics = { navController.navigate("analytics") },
                                onNavigateToDuplicates = { navController.navigate("duplicate_files") },
                                onNavigateToJunkList = { category -> navController.navigate("junk_list/$category") }
                            )
                        }
                        composable("settings") { 
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") }
                            )
                        }
                        composable("privacy_policy") {
                            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("analytics") { 
                            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("duplicate_files") { 
                            DuplicateFilesScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(
                            route = "junk_list/{category}",
                            arguments = listOf(navArgument("category") { type = NavType.StringType })
                        ) { 
                            JunkListScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
