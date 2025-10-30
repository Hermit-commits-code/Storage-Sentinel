package com.example.storagesentinel.ui.composables

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.storagesentinel.billing.BillingManager
import com.example.storagesentinel.billing.PurchaseState

@Composable
fun ProUpgradeDialog(
    billingManager: BillingManager, 
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val billingState by billingManager.billingState.collectAsState()
    
    // Handle purchase success
    LaunchedEffect(billingState.purchaseState) {
        if (billingState.purchaseState == PurchaseState.SUCCESS) {
            billingManager.clearPurchaseState()
            onDismiss()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "PRO Feature",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Upgrade to Storage Sentinel PRO",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Unlock powerful features like residual data, large file, and duplicate file cleaning to maximize your storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                // Show price if available, otherwise show development mode info
                val proProduct = billingState.availableProducts.find { it.productId == "storage_sentinel_pro" }
                if (proProduct != null) {
                    Text(
                        text = "Price: ${proProduct.oneTimePurchaseOfferDetails?.formattedPrice ?: "Loading..."}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Development Mode: $4.99 (Will connect to Play Store in production)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Show error message if any
                billingState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("No Thanks")
                    }
                    
                    when (billingState.purchaseState) {
                        PurchaseState.PURCHASING -> {
                            Button(onClick = { }) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    val activity = context as? ComponentActivity
                                    if (activity != null) {
                                        billingManager.purchaseProVersion(activity)
                                    } else {
                                        // Fallback for testing
                                        billingManager.simulateProPurchase()
                                        onDismiss()
                                    }
                                },
                                enabled = billingState.isConnected
                            ) {
                                Text(if (proProduct != null) "Upgrade Now" else "Test Upgrade")
                            }
                        }
                    }
                }
            }
        }
    }
}
