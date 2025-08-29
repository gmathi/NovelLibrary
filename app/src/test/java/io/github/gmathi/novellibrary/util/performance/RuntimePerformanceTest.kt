package io.github.gmathi.novellibrary.util.performance

import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for runtime performance optimization components
 */
class RuntimePerformanceTest {
    
    private lateinit var context: Context
    private lateinit var performanceMonitor: RuntimePerformanceMonitor
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        performanceMonitor = RuntimePerformanceMonitor(context)
    }
    
    @Test
    fun `recordAppStartTime sets start time`() {
        // When
        performanceMonitor.recordAppStartTime()
        
        // Then
        val metrics = performanceMonitor.getStartupMetrics()
        assertTrue("App start time should be set", metrics.appStartTime > 0)
    }
    
    @Test
    fun `recordHiltInitTime calculates duration correctly`() {
        // Given
        performanceMonitor.recordAppStartTime()
        Thread.sleep(10) // Small delay to ensure different timestamps
        
        // When
        performanceMonitor.recordHiltInitTime()
        
        // Then
        val metrics = performanceMonitor.getStartupMetrics()
        assertTrue("Hilt init time should be after app start", metrics.hiltInitTime > metrics.appStartTime)
        assertTrue("Total startup time should be positive", metrics.totalStartupTime > 0)
    }
    
    @Test
    fun `measureDependencyInjectionTime returns positive duration`() {
        // Given
        val componentName = "TestComponent"
        var actionExecuted = false
        
        // When
        val duration = performanceMonitor.measureDependencyInjectionTime(componentName) {
            Thread.sleep(10)
            actionExecuted = true
        }
        
        // Then
        assertTrue("Action should be executed", actionExecuted)
        assertTrue("Duration should be positive", duration > 0)
    }
    
    @Test
    fun `getCurrentMemoryUsage returns valid memory info`() {
        // When
        val memoryUsage = performanceMonitor.getCurrentMemoryUsage()
        
        // Then
        assertTrue("Used memory should be positive", memoryUsage.usedMemoryMB > 0)
        assertTrue("Max memory should be positive", memoryUsage.maxMemoryMB > 0)
        assertTrue("Available memory should be positive", memoryUsage.availableMemoryMB > 0)
        assertTrue("Used memory should not exceed max", memoryUsage.usedMemoryMB <= memoryUsage.maxMemoryMB)
    }
    
    @Test
    fun `checkMemoryUsage logs appropriate warnings`() {
        // This test would require mocking the logging system
        // For now, we just verify the method doesn't throw exceptions
        
        // When & Then (should not throw)
        performanceMonitor.checkMemoryUsage()
    }
    
    @Test
    fun `getStartupMetrics returns consistent data`() {
        // Given
        performanceMonitor.recordAppStartTime()
        performanceMonitor.recordHiltInitTime()
        
        // When
        val metrics1 = performanceMonitor.getStartupMetrics()
        val metrics2 = performanceMonitor.getStartupMetrics()
        
        // Then
        assertEquals("App start time should be consistent", metrics1.appStartTime, metrics2.appStartTime)
        assertEquals("Hilt init time should be consistent", metrics1.hiltInitTime, metrics2.hiltInitTime)
        assertEquals("Total startup time should be consistent", metrics1.totalStartupTime, metrics2.totalStartupTime)
    }
}