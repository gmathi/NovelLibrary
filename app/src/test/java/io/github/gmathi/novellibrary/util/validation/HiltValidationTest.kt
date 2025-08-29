package io.github.gmathi.novellibrary.util.validation

import android.content.Context
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.util.validation.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Comprehensive tests for Hilt validation utilities
 * Tests compile-time validation, runtime validation, and dependency resolution
 */
@HiltAndroidTest
class HiltValidationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var compileTimeValidator: HiltCompileTimeValidator
    
    @Inject
    lateinit var runtimeValidator: HiltRuntimeValidator
    
    @Inject
    lateinit var dependencyResolver: HiltDependencyResolver
    
    @Mock
    private lateinit var mockContext: Context
    
    @Before
    fun init() {
        MockitoAnnotations.openMocks(this)
        hiltRule.inject()
    }
    
    @Test
    fun `test compile time validator dependency graph validation`() {
        // When
        val validation = compileTimeValidator.validateDependencyGraph()
        
        // Then
        assertNotNull(validation)
        assertTrue(validation.dependencyCount > 0)
        assertNotNull(validation.recommendations)
        
        // Should detect if there are any circular dependencies
        if (validation.circularDependencies.isNotEmpty()) {
            assertFalse(validation.isValid)
            assertTrue(validation.recommendations.any { it.contains("circular") })
        }
    }
    
    @Test
    fun `test compile time validator module validation`() {
        // When
        val validation = compileTimeValidator.validateHiltModules()
        
        // Then
        assertNotNull(validation)
        assertTrue(validation.totalModules > 0)
        assertTrue(validation.moduleValidations.isNotEmpty())
        
        // Check that known modules are validated
        assertTrue(validation.moduleValidations.containsKey("DatabaseModule"))
        assertTrue(validation.moduleValidations.containsKey("NetworkModule"))
        
        // Each module should have some bindings
        validation.moduleValidations.values.forEach { moduleValidation ->
            assertTrue(moduleValidation.bindingCount >= 0)
        }
    }
    
    @Test
    fun `test compile time validator scoping validation`() {
        // When
        val validation = compileTimeValidator.validateComponentScoping()
        
        // Then
        assertNotNull(validation)
        assertNotNull(validation.issues)
        assertNotNull(validation.warnings)
        
        // Should provide warnings about potential scoping issues
        assertTrue(validation.warnings.isNotEmpty())
    }
    
    @Test
    fun `test compile time validator binding completeness`() {
        // When
        val validation = compileTimeValidator.validateBindingCompleteness()
        
        // Then
        assertNotNull(validation)
        assertNotNull(validation.availableBindings)
        assertNotNull(validation.requiredBindings)
        
        // Should have some available bindings
        assertTrue(validation.availableBindings.isNotEmpty())
        
        // Check for known bindings
        assertTrue(validation.availableBindings.contains("DBHelper"))
        assertTrue(validation.availableBindings.contains("NetworkHelper"))
    }
    
    @Test
    fun `test comprehensive validation report generation`() {
        // When
        val report = compileTimeValidator.generateComprehensiveValidationReport()
        
        // Then
        assertNotNull(report)
        assertTrue(report.generatedAt > 0)
        assertNotNull(report.dependencyGraphValidation)
        assertNotNull(report.moduleValidation)
        assertNotNull(report.scopingValidation)
        assertNotNull(report.bindingValidation)
        
        // Should generate a formatted report
        val formattedReport = report.getFormattedReport()
        assertTrue(formattedReport.contains("COMPREHENSIVE HILT VALIDATION REPORT"))
        assertTrue(formattedReport.contains("Overall Status"))
    }
    
    @Test
    fun `test runtime validator component injection validation`() {
        // Given
        val mockComponent = TestComponent()
        val expectedDependencies = listOf("dbHelper", "networkHelper")
        
        // When
        val validation = runtimeValidator.validateComponentInjection(mockComponent, expectedDependencies)
        
        // Then
        assertNotNull(validation)
        assertEquals("TestComponent", validation.componentName)
        assertEquals(expectedDependencies.size, validation.injectionResults.size)
        
        // Should validate each expected dependency
        expectedDependencies.forEach { dependency ->
            assertTrue(validation.injectionResults.containsKey(dependency))
        }
    }
    
    @Test
    fun `test runtime validator lifecycle validation`() {
        // Given
        val mockComponent = TestComponent()
        val expectedScope = ComponentScope.SINGLETON
        
        // When
        val validation = runtimeValidator.validateComponentLifecycle(mockComponent, expectedScope)
        
        // Then
        assertNotNull(validation)
        assertEquals("TestComponent", validation.componentName)
        assertEquals(expectedScope, validation.expectedScope)
        assertNotNull(validation.issues)
        assertNotNull(validation.warnings)
    }
    
    @Test
    fun `test runtime validator performance validation`() {
        // Given
        val mockComponent = TestComponent()
        val dependencies = listOf("DBHelper", "NetworkHelper")
        
        // When
        val validation = runtimeValidator.validateDependencyResolutionPerformance(mockComponent, dependencies)
        
        // Then
        assertNotNull(validation)
        assertEquals("TestComponent", validation.componentName)
        assertEquals(dependencies.size, validation.resolutionTimes.size)
        assertTrue(validation.averageResolutionTime >= 0)
        
        // Should track resolution times for each dependency
        dependencies.forEach { dependency ->
            assertTrue(validation.resolutionTimes.containsKey(dependency))
        }
    }
    
    @Test
    fun `test runtime validator memory validation`() {
        // Given
        val mockComponent = TestComponent()
        
        // When
        val validation = runtimeValidator.validateMemoryUsage(mockComponent)
        
        // Then
        assertNotNull(validation)
        assertEquals("TestComponent", validation.componentName)
        assertNotNull(validation.memoryIssues)
        assertNotNull(validation.recommendations)
        
        // Should provide recommendations
        assertTrue(validation.recommendations.isNotEmpty())
    }
    
    @Test
    fun `test runtime validation report generation`() {
        // Given
        val mockComponent = TestComponent()
        val expectedDependencies = listOf("dbHelper", "networkHelper")
        val expectedScope = ComponentScope.SINGLETON
        
        // When
        val report = runtimeValidator.generateRuntimeValidationReport(
            mockComponent, expectedDependencies, expectedScope
        )
        
        // Then
        assertNotNull(report)
        assertEquals("TestComponent", report.componentName)
        assertTrue(report.validatedAt > 0)
        assertNotNull(report.injectionValidation)
        assertNotNull(report.lifecycleValidation)
        assertNotNull(report.performanceValidation)
        assertNotNull(report.memoryValidation)
        
        // Should generate formatted report
        val formattedReport = report.getFormattedReport()
        assertTrue(formattedReport.contains("RUNTIME VALIDATION REPORT"))
        assertTrue(formattedReport.contains("TestComponent"))
    }
    
    @Test
    fun `test dependency resolver path resolution`() {
        // Given
        val componentType = "ChaptersViewModel"
        
        // When
        val result = dependencyResolver.resolveDependencyPath(componentType)
        
        // Then
        assertNotNull(result)
        assertEquals(componentType, result.componentType)
        assertTrue(result.resolutionSteps.isNotEmpty())
        assertTrue(result.dependencies.isNotEmpty())
        assertTrue(result.totalResolutionTime >= 0)
        
        // Should resolve known dependencies
        assertTrue(result.dependencies.containsKey("DBHelper"))
        assertTrue(result.dependencies.containsKey("NetworkHelper"))
        
        // Should generate formatted result
        val formattedResult = result.getFormattedResult()
        assertTrue(formattedResult.contains("DEPENDENCY RESOLUTION"))
        assertTrue(formattedResult.contains(componentType))
    }
    
    @Test
    fun `test dependency resolver relationship analysis`() {
        // When
        val analysis = dependencyResolver.analyzeDependencyRelationships()
        
        // Then
        assertNotNull(analysis)
        assertTrue(analysis.relationships.isNotEmpty())
        assertNotNull(analysis.issues)
        assertNotNull(analysis.metrics)
        assertNotNull(analysis.recommendations)
        
        // Should have metrics
        assertTrue(analysis.metrics.totalComponents > 0)
        assertTrue(analysis.metrics.averageDependenciesPerComponent >= 0)
        
        // Should provide recommendations
        assertTrue(analysis.recommendations.isNotEmpty())
    }
    
    @Test
    fun `test dependency resolver graph generation`() {
        // When
        val graph = dependencyResolver.generateDependencyGraph()
        
        // Then
        assertNotNull(graph)
        assertTrue(graph.nodes.isNotEmpty())
        assertTrue(graph.edges.isNotEmpty())
        assertEquals(graph.nodes.size, graph.componentCount)
        assertEquals(graph.edges.size, graph.dependencyCount)
        
        // Should have known components
        val nodeIds = graph.nodes.map { it.id }
        assertTrue(nodeIds.contains("ChaptersViewModel"))
        assertTrue(nodeIds.contains("DBHelper"))
        
        // Should have dependency relationships
        val edgeRelationships = graph.edges.map { "${it.from} -> ${it.to}" }
        assertTrue(edgeRelationships.any { it.contains("ChaptersViewModel") })
    }
    
    @Test
    fun `test dependency resolver graph integrity validation`() {
        // When
        val report = dependencyResolver.validateDependencyGraphIntegrity()
        
        // Then
        assertNotNull(report)
        assertTrue(report.nodeCount > 0)
        assertTrue(report.edgeCount > 0)
        assertNotNull(report.issues)
        
        // Should detect any integrity issues
        report.issues.forEach { issue ->
            assertNotNull(issue.type)
            assertNotNull(issue.description)
            assertNotNull(issue.severity)
            assertTrue(issue.affectedNodes.isNotEmpty())
        }
    }
    
    @Test
    fun `test validation utilities integration`() {
        // Test that all validation utilities work together
        
        // Compile-time validation
        val compileTimeReport = compileTimeValidator.generateComprehensiveValidationReport()
        assertNotNull(compileTimeReport)
        
        // Runtime validation
        val mockComponent = TestComponent()
        val runtimeReport = runtimeValidator.generateRuntimeValidationReport(
            mockComponent, listOf("dbHelper"), ComponentScope.SINGLETON
        )
        assertNotNull(runtimeReport)
        
        // Dependency resolution
        val resolutionResult = dependencyResolver.resolveDependencyPath("ChaptersViewModel")
        assertNotNull(resolutionResult)
        
        // All should provide meaningful results
        assertTrue(compileTimeReport.getFormattedReport().isNotEmpty())
        assertTrue(runtimeReport.getFormattedReport().isNotEmpty())
        assertTrue(resolutionResult.getFormattedResult().isNotEmpty())
    }
    
    // Test component for validation testing
    private class TestComponent {
        var dbHelper: Any? = null
        var networkHelper: Any? = null
    }
}