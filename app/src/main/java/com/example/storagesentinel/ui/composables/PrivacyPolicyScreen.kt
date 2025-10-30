package com.example.storagesentinel.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrivacyPolicyContent()
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Text(
        text = "Privacy Policy",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    
    Text(
        text = "Last updated: October 30, 2025",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    PrivacySection(
        title = "What We Collect",
        content = """
        Storage Sentinel collects minimal data to provide storage optimization services:
        
        • Storage usage statistics (file sizes, types, locations)
        • App usage patterns for cleaning recommendations
        • Device storage metrics for analytics
        • Crash reports for app stability (anonymized)
        
        We do NOT collect:
        • Personal files or file contents
        • Contact information or personal data
        • Location data or device identifiers
        • Social media or browsing history
        """.trimIndent()
    )
    
    PrivacySection(
        title = "How We Use Your Data",
        content = """
        Your data is used exclusively to:
        
        • Provide storage cleaning and optimization services
        • Generate storage analytics and trends
        • Improve app performance and features
        • Send relevant storage notifications
        
        We never:
        • Sell your data to third parties
        • Use data for advertising purposes
        • Share data with external services
        • Access your personal files or content
        """.trimIndent()
    )
    
    PrivacySection(
        title = "Data Storage & Security",
        content = """
        • All data is stored locally on your device
        • No data is transmitted to external servers
        • Analytics data is encrypted and anonymized
        • You can clear all data by uninstalling the app
        • No user accounts or cloud synchronization
        """.trimIndent()
    )
    
    PrivacySection(
        title = "Your Rights",
        content = """
        You have the right to:
        
        • Access all data collected about your device
        • Delete all stored analytics data
        • Opt out of crash reporting
        • Request data export (device storage only)
        • Disable all data collection features
        
        • Contact us through Google Play Store developer contact
        • Submit feedback via the app store review system
        • Disable all data collection features
        
        For privacy concerns, please contact us through the Google Play Store.
        """.trimIndent()
    )
    
    PrivacySection(
        title = "Third-Party Services",
        content = """
        Storage Sentinel may use:
        
        • Google Play Billing (for PRO purchases only)
        • Android System Services (for storage access)
        
        These services have their own privacy policies and terms.
        """.trimIndent()
    )
    
    PrivacySection(
        title = "Children's Privacy",
        content = """
        Storage Sentinel does not knowingly collect data from children under 13. 
        If you believe a child has provided data to our app, please contact us immediately.
        """.trimIndent()
    )
    
    PrivacySection(
        title = "Changes to This Policy",
        content = """
        We may update this privacy policy from time to time. We will notify users of 
        any material changes through the app or app store updates.
        """.trimIndent()
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Contact Us",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "For privacy questions or support:\n• Google Play Store (search 'Storage Sentinel')\n• App review system for feedback\n• In-app settings for data management",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
        )
    }
}