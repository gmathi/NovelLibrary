package io.github.gmathi.novellibrary.util.migration

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class MigrationFallbackTest {

    private lateinit var migrationLogger: MigrationLogger
    private lateinit var migrationFallback: MigrationFallback

    @Before
    fun setup() {
        migrationLogger = mockk(relaxed = true)
        migrationFallback = MigrationFallback(migrationLogger)
    }

    @Test
    fun `registerFallback stores fallback provider`() {
        // Given
        val componentName = "TestComponent"
        val fallbackProvider = { "fallback_instance" }

        // When
        migrationFallback.registerFallback(componentName, fallbackProvider)

        // Then
        assertFalse(migrationFallback.isUsingFallback(componentName))
    }

    @Test
    fun `getComponentWithFallback returns Hilt component when available`() {
        // Given
        val componentName = "TestComponent"
        val hiltComponent = "hilt_instance"
        val hiltProvider = { hiltComponent }

        // When
        val result = migrationFallback.getComponentWithFallback(componentName, hiltProvider)

        // Then
        assertEquals(hiltComponent, result)
        assertFalse(migrationFallback.isUsingFallback(componentName))
    }

    @Test
    fun `getComponentWithFallback uses fallback when Hilt returns null`() {
        // Given
        val componentName = "TestComponent"
        val fallbackInstance = "fallback_instance"
        val hiltProvider = { null }
        
        migrationFallback.registerFallback(componentName) { fallbackInstance }

        // When
        val result = migrationFallback.getComponentWithFallback(componentName, hiltProvider)

        // Then
        assertEquals(fallbackInstance, result)
        assertTrue(migrationFallback.isUsingFallback(componentName))
        verify { migrationLogger.logFallbackActivation(componentName, any()) }
    }

    @Test
    fun `getComponentWithFallback uses fallback when Hilt throws exception`() {
        // Given
        val componentName = "TestComponent"
        val fallbackInstance = "fallback_instance"
        val hiltProvider = { throw RuntimeException("Hilt failed") }
        
        migrationFallback.registerFallback(componentName) { fallbackInstance }

        // When
        val result = migrationFallback.getComponentWithFallback(componentName, hiltProvider)

        // Then
        assertEquals(fallbackInstance, result)
        assertTrue(migrationFallback.isUsingFallback(componentName))
        verify { migrationLogger.logFallbackActivation(componentName, any()) }
    }

    @Test
    fun `getActiveFallbacks returns correct set`() {
        // Given
        val component1 = "Component1"
        val component2 = "Component2"
        
        migrationFallback.registerFallback(component1) { "fallback1" }
        migrationFallback.registerFallback(component2) { "fallback2" }

        // When - trigger fallback for component1
        migrationFallback.getComponentWithFallback(component1) { null }
        
        val activeFallbacks = migrationFallback.getActiveFallbacks()

        // Then
        assertTrue(activeFallbacks.contains(component1))
        assertFalse(activeFallbacks.contains(component2))
    }

    @Test
    fun `validateFallbacks returns true when all fallbacks work`() {
        // Given
        migrationFallback.registerFallback("Component1") { "fallback1" }
        migrationFallback.registerFallback("Component2") { "fallback2" }

        // When
        val result = migrationFallback.validateFallbacks()

        // Then
        assertTrue(result)
    }

    @Test
    fun `validateFallbacks returns false when fallback returns null`() {
        // Given
        migrationFallback.registerFallback("Component1") { null }

        // When
        val result = migrationFallback.validateFallbacks()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getMigrationStatus returns correct status`() {
        // Given
        migrationFallback.registerFallback("Component1") { "fallback1" }
        migrationFallback.registerFallback("Component2") { "fallback2" }
        
        // Trigger fallback for one component
        migrationFallback.getComponentWithFallback("Component1") { null }

        // When
        val status = migrationFallback.getMigrationStatus()

        // Then
        assertEquals(2, status.totalComponents)
        assertEquals(1, status.migratedComponents)
        assertEquals(1, status.fallbackComponents)
        assertEquals(50f, status.migrationProgress, 0.01f)
        assertTrue(status.activeFallbacks.contains("Component1"))
    }

    @Test
    fun `clearFallbacks removes all registrations`() {
        // Given
        migrationFallback.registerFallback("Component1") { "fallback1" }
        migrationFallback.getComponentWithFallback("Component1") { null }

        // When
        migrationFallback.clearFallbacks()

        // Then
        assertTrue(migrationFallback.getActiveFallbacks().isEmpty())
        assertEquals(0, migrationFallback.getMigrationStatus().totalComponents)
    }
}