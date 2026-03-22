package io.github.gmathi.novellibrary.settings.viewmodel

import app.cash.turbine.test
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AdvancedSettingsViewModel.
 * 
 * Tests state management, repository interactions, and error handling
 * for advanced settings functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedSettingsViewModelTest {
    
    private lateinit var fakeDataStore: FakeSettingsDataStore
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: AdvancedSettingsViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDataStore = FakeSettingsDataStore()
        repository = SettingsRepositoryDataStore(fakeDataStore)
        viewModel = AdvancedSettingsViewModel(repository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    //region Network Settings Tests
    
    @Test
    fun `javascriptDisabled should emit initial value from repository`() = runTest {
        // Given
        fakeDataStore.javascriptDisabled.value = true
        
        // When
        viewModel.javascriptDisabled.test {
            // Then
            awaitItem() shouldBe true
        }
    }
    
    @Test
    fun `setJavascriptDisabled should update repository`() = runTest {
        // When
        viewModel.setJavascriptDisabled(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.javascriptDisabled.test {
            awaitItem() shouldBe true
        }
    }
    
    @Test
    fun `javascriptDisabled should emit updates when repository changes`() = runTest {
        viewModel.javascriptDisabled.test {
            // Initial value
            awaitItem() shouldBe false
            
            // When
            viewModel.setJavascriptDisabled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            awaitItem() shouldBe true
        }
    }
    
    //endregion
    
    //region Debug Settings Tests
    
    @Test
    fun `isDeveloper should emit initial value from repository`() = runTest {
        // Given
        fakeDataStore.isDeveloper.value = true
        
        // When
        viewModel.isDeveloper.test {
            // Then
            awaitItem() shouldBe true
        }
    }
    
    @Test
    fun `setIsDeveloper should update repository`() = runTest {
        // When
        viewModel.setIsDeveloper(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.isDeveloper.test {
            awaitItem() shouldBe true
        }
    }
    
    @Test
    fun `isDeveloper should emit updates when repository changes`() = runTest {
        viewModel.isDeveloper.test {
            // Initial value
            awaitItem() shouldBe false
            
            // When
            viewModel.setIsDeveloper(true)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            awaitItem() shouldBe true
        }
    }
    
    //endregion
    
    //region Multiple Settings Tests
    
    @Test
    fun `multiple settings can be updated independently`() = runTest {
        // When
        viewModel.setJavascriptDisabled(true)
        viewModel.setIsDeveloper(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.javascriptDisabled.test {
            awaitItem() shouldBe true
        }
        viewModel.isDeveloper.test {
            awaitItem() shouldBe true
        }
    }
    
    @Test
    fun `settings should maintain state across multiple updates`() = runTest {
        // When - Update JavaScript disabled multiple times
        viewModel.setJavascriptDisabled(true)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setJavascriptDisabled(false)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setJavascriptDisabled(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.javascriptDisabled.test {
            awaitItem() shouldBe true
        }
    }
    
    //endregion
}
