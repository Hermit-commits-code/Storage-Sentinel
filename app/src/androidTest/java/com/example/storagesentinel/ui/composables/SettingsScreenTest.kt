package com.example.storagesentinel.ui.composables

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.storagesentinel.ui.theme.StorageSentinelTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenAutoCleanEnabled_andBatteryOptimizationActive_showsDialog() {
        // Arrange: Set the content for the test
        composeTestRule.setContent {
            StorageSentinelTheme {
                SettingsScreen(onNavigateBack = {}) 
            }
        }

        // Act: Find the switch and click it.
        // We assume the default state in a test environment is that battery optimizations are NOT ignored.
        composeTestRule.onNodeWithText("Enable Automatic Cleaning").performClick()

        // Assert: The dialog should now be visible.
        composeTestRule.onNodeWithText("Battery Optimization Required").assertIsDisplayed()
    }
}
