package io.github.gmathi.novellibrary.util.performance

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for performance alerting system
 */
class PerformanceAlertingTest {
    
    private lateinit var context: Context
    private lateinit var performanceMonitor: RuntimePerformanceMonitor
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var performanceAlerting: PerformanceAlerting
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        performanceMonitor = mockk(relaxed = true)
        firebaseAnalytics = mockk(relaxed = true)
        performanceAlerting = PerformanceAlerting(context, performanceMonitor, firebaseAnalytics)
    }
    
    @Test
    fun `startMonitoring enables monitoring`() {
        // When
        performanceAlerting.startMonitoring()
        
        // Then
        val status = performanceAlerting.getAlertingStatus()
        assertTrue("Monitoring should be enabled", status.isMonitoring)
    }
    
    @Test
    fun `stopMonitoring disables monitoring`() {
        // Given
        performanceAlerting.startMonitoring()
        
        // When
        performanceAlerting.stopMonitoring()
        
        // Then
        val status = performanceAlerting.getAlertingStatus()
        assertFalse("Monitoring should be disabled", status.isMonitoring)
    }
    
    @Test
    fun `checkDependencyInjectionPerformance triggers warning for slow injection`() {
        // Given
        val componentName = "SlowComponent"
        val slowDuration = 600L // Above warning threshold (500ms)
        
        // When
        performanceAlerting.checkDependencyInjectionPerformance(componentName, slowDuration)
        
        // Then
        verify { firebaseAnalytics.logEvent("performance_alert", any()) }
    }
    
    @Test
    fun `checkDependencyInjectionPerformance triggers critical alert for very slow injection`() {
        // Given
        val componentName = "VerySlowComponent"
        val criticalDuration = 1200L // Above critical threshold (1000ms)
        
        // When
        performanceAlerting.checkDependencyInjectionPerformance(componentName, criticalDuration)
        
        // Then
        verify { firebaseAnalytics.logEvent("performance_alert", any()) }
    }
    
    @Test
    fun `getAlertingStatus returns correct memory status`() {
        // Given
        val highMemoryUsage = RuntimePerformanceMonitor.MemoryUsage(
            usedMemoryMB = 250L, // Above warning threshold (200MB)
            maxMemoryMB = 512L,
            availableMemoryMB = 100L,
            isLowMemory = false
        )
        every { performanceMonitor.getCurrentMemoryUsage() } returns highMemoryUsage
        every { performanceMonitor.getStartupMetrics() } returns RuntimePerformanceMonitor.StartupMetrics(
            appStartTime = 1000L,
            hiltInitTime = 2000L,
            totalStartupTime = 1000L,
            currentTime = 3000L
        )
        
        // When
        val status = performanceAlerting.getAlertingStatus()
        
        // Then
        assertEquals("Memory status should be WARNING", PerformanceAlerting.AlertLevel.WARNING, status.memoryStatus)
    }
    
    @Test
    fun `getAlertingStatus returns correct startup status`() {
        // Given
        val normalMemoryUsage = RuntimePerformanceMonitor.MemoryUsage(
            usedMemoryMB = 100L, // Below warning threshold
            maxMemoryMB = 512L,
            availableMemoryMB = 200L,
            isLowMemory = false
        )
        val slowStartupMetrics = RuntimePerformanceMonitor.StartupMetrics(
            appStartTime = 1000L,
            hiltInitTime = 5500L,
            totalStartupTime = 4500L, // Above warning threshold (3000ms)
            currentTime = 6000L
        )
        every { performanceMonitor.getCurrentMemoryUsage() } returns normalMemoryUsage
        every { performanceMonitor.getStartupMetrics() } returns slowStartupMetrics
        
        // When
        val status = performanceAlerting.getAlertingStatus()
        
        // Then
        assertEquals("Startup status should be WARNING", PerformanceAlerting.AlertLevel.WARNING, status.startupStatus)
    }
    
    @Test
    fun `getAlertingStatus returns normal status for good performance`() {
        // Given
        val normalMemoryUsage = RuntimePerformanceMonitor.MemoryUsage(
            usedMemoryMB = 100L, // Below warning threshold
            maxMemoryMB = 512L,
            availableMemoryMB = 200L,
            isLowMemory = false
        )
        val fastStartupMetrics = RuntimePerformanceMonitor.StartupMetrics(
            appStartTime = 1000L,
            hiltInitTime = 2500L,
            totalStartupTime = 1500L, // Below warning threshold
            currentTime = 3000L
        )
        every { performanceMonitor.getCurrentMemoryUsage() } returns normalMemoryUsage
        every { performanceMonitor.getStartupMetrics() } returns fastStartupMetrics
        
        // When
        val status = performanceAlerting.getAlertingStatus()
        
        // Then
        assertEquals("Memory status should be NORMAL", PerformanceAlerting.AlertLevel.NORMAL, status.memoryStatus)
        assertEquals("Startup status should be NORMAL", PerformanceAlerting.AlertLevel.NORMAL, status.startupStatus)
    }
}