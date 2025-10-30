package com.example.storagesentinel.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.model.JunkCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsManager: SettingsManager
    private lateinit var workManager: WorkManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsManager = mock()
        workManager = mock()
    }

    @Test
    fun `initial uiState correctly reflects SettingsManager`() = runTest {
        // Arrange
        whenever(settingsManager.largeFileThresholdMb).thenReturn(flowOf(250L))
        whenever(settingsManager.isAutoCleanEnabled).thenReturn(flowOf(true))
        whenever(settingsManager.autoCleanFrequency).thenReturn(flowOf("MONTHLY"))
        whenever(settingsManager.autoCleanCategories).thenReturn(flowOf(setOf("EMPTY_FOLDER")))

        // Act
        viewModel = SettingsViewModel(settingsManager, workManager)
        testDispatcher.scheduler.advanceUntilIdle() // Allow the combine flow to emit

        // Assert
        val expectedState = SettingsUiState(
            largeFileThresholdMb = 250L,
            isAutoCleanEnabled = true,
            autoCleanFrequency = "MONTHLY",
            autoCleanCategories = setOf(JunkCategory.EMPTY_FOLDER)
        )
        assertEquals(expectedState, viewModel.uiState.value)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
