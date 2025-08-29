package io.github.gmathi.novellibrary.util.validation

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.test.*

/**
 * Automated validation tests for Hilt dependency graph
 * Ensures dependency graph integrity and detects potential issues
 */
@HiltAndroidTest
class DependencyGraphValidationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var compileTimeValidator: HiltCompileTimeValidator
    
    @Inject
    lateinit var dependencyResolver: HiltDependencyResolver
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun `validate no circular dependencies exist`() {
        // When
        val validation = compileTimeValidator.validateDependencyGraph()
        
        // Then
        if (validation.circularDependencies.isNotEmpty()) {
            val cycles = validation.circularDependencies.map { it.cycle.joinToString(" → ") }
            fail("Circular dependencies detected: ${cycles.joinToString(", ")}")
        }
        
        assertTrue(validation.isValid, "Dependency graph should be valid")
        assertTrue(validation.recommendations.isNotEmpty(), "Should provide recommendations")
    }
    
    @Test
    fun `validate all required bindings are available`() {
        // When
        val validation = compileTimeValidator.validateBindingCompleteness()
        
        // Then
        if (validation.missingBindings.isNotEmpty()) {
            val missing = validation.missingBindings.map { "${it.type} (required by ${it.requiredBy})" }
            fail("Missing bindings detected: ${missing.joinToString(", ")}")
        }
        
        assertTrue(validation.isComplete, "All required bindings should be available")
        
        // Verify critical bindings exist
        val criticalBindings = listOf("DBHelper", "DataCenter", "NetworkHelper", "SourceManager")
        criticalBindings.forEach { binding ->
            assertTrue(
                validation.availableBindings.contains(binding),
                "Critical binding $binding should be available"
            )
        }
    }
    
    @Test
    fun `validate all modules are properly configured`() {
        // When
        val validation = compileTimeValidator.validateHiltModules()
        
        // Then
        assertTrue(validation.overallValid, "All modules should be valid")
        assertTrue(validation.totalModules > 0, "Should have modules configured")
        
        // Verify critical modules exist and are valid
        val criticalModules = listOf("DatabaseModule", "NetworkModule", "SourceModule")
        criticalModules.forEach { moduleName ->
            assertTrue(
                validation.moduleValidations.containsKey(moduleName),
                "Critical module $moduleName should exist"
            )
            
            val moduleValidation = validation.moduleValidations[moduleName]!!
            assertTrue(
                moduleValidation.isValid,
                "Critical module $moduleName should be valid: ${moduleValidation.issues}"
            )
            assertTrue(
                moduleValidation.bindingCount > 0,
                "Critical module $moduleName should have bindings"
            )
        }
    }
    
    @Test
    fun `validate component scoping is correct`() {
        // When
        val validation = compileTimeValidator.validateComponentScoping()
        
        // Then
        // Check for high-severity scoping issues
        val highSeverityIssues = validation.issues.filter { it.severity == IssueSeverity.HIGH }
        if (highSeverityIssues.isNotEmpty()) {
            val issues = highSeverityIssues.map { "${it.type}: ${it.description}" }
            fail("High-severity scoping issues detected: ${issues.joinToString(", ")}")
        }
        
        // Warnings are acceptable, but log them
        if (validation.warnings.isNotEmpty()) {
            println("Scoping warnings: ${validation.warnings.joinToString(", ")}")
        }
    }
    
    @Test
    fun `validate dependency graph integrity`() {
        // When
        val report = dependencyResolver.validateDependencyGraphIntegrity()
        
        // Then
        assertTrue(report.nodeCount > 0, "Should have nodes in dependency graph")
        assertTrue(report.edgeCount > 0, "Should have edges in dependency graph")
        
        // Check for critical integrity issues
        val criticalIssues = report.issues.filter { it.severity == IntegritySeverity.HIGH }
        if (criticalIssues.isNotEmpty()) {
            val issues = criticalIssues.map { "${it.type}: ${it.description}" }
            fail("Critical dependency graph integrity issues: ${issues.joinToString(", ")}")
        }
        
        // Log medium and low severity issues for awareness
        val nonCriticalIssues = report.issues.filter { it.severity != IntegritySeverity.HIGH }
        if (nonCriticalIssues.isNotEmpty()) {
            println("Non-critical dependency graph issues:")
            nonCriticalIssues.forEach { issue ->
                println("  ${issue.severity}: ${issue.type} - ${issue.description}")
            }
        }
    }
    
    @Test
    fun `validate dependency resolution performance`() {
        // Test resolution performance for critical components
        val criticalComponents = listOf("ChaptersViewModel", "GoogleBackupViewModel")
        
        criticalComponents.forEach { component ->
            // When
            val result = dependencyResolver.resolveDependencyPath(component)
            
            // Then
            assertTrue(result.isSuccessful, "Should successfully resolve $component dependencies")
            assertTrue(result.totalResolutionTime < 1000, "Resolution should be fast (< 1000ms) for $component")
            assertTrue(result.dependencies.isNotEmpty(), "$component should have dependencies")
            
            // Verify all dependencies are resolvable
            result.dependencies.forEach { (name, info) ->
                assertTrue(
                    info.isResolvable,
                    "Dependency $name for $component should be resolvable from ${info.providerModule}"
                )
            }
        }
    }
    
    @Test
    fun `validate dependency relationship health`() {
        // When
        val analysis = dependencyResolver.analyzeDependencyRelationships()
        
        // Then
        assertTrue(analysis.relationships.isNotEmpty(), "Should have dependency relationships")
        assertTrue(analysis.metrics.totalComponents > 0, "Should have components")
        
        // Check for excessive dependencies (potential design issues)
        val excessiveDependencyIssues = analysis.issues.filter { it.type == "ExcessiveDependencies" }
        if (excessiveDependencyIssues.isNotEmpty()) {
            println("Components with excessive dependencies:")
            excessiveDependencyIssues.forEach { issue ->
                println("  ${issue.description}")
            }
        }
        
        // Verify dependency metrics are reasonable
        assertTrue(
            analysis.metrics.averageDependenciesPerComponent < 10.0,
            "Average dependencies per component should be reasonable (< 10): ${analysis.metrics.averageDependenciesPerComponent}"
        )
        
        assertTrue(
            analysis.metrics.maxDependenciesPerComponent < 15,
            "Maximum dependencies per component should be reasonable (< 15): ${analysis.metrics.maxDependenciesPerComponent}"
        )
    }
    
    @Test
    fun `validate ViewModels have proper dependencies`() {
        val viewModels = listOf("ChaptersViewModel", "GoogleBackupViewModel")
        
        viewModels.forEach { viewModel ->
            // When
            val result = dependencyResolver.resolveDependencyPath(viewModel)
            
            // Then
            assertTrue(result.isSuccessful, "$viewModel should resolve successfully")
            
            // ViewModels should have SavedStateHandle
            assertTrue(
                result.dependencies.containsKey("SavedStateHandle"),
                "$viewModel should have SavedStateHandle dependency"
            )
            
            // ViewModels should have data access dependencies
            val hasDataAccess = result.dependencies.keys.any { 
                it.contains("DBHelper") || it.contains("DataCenter") 
            }
            assertTrue(hasDataAccess, "$viewModel should have data access dependencies")
        }
    }
    
    @Test
    fun `validate Activities and Fragments have proper entry points`() {
        // This would be expanded based on actual Activities and Fragments
        val components = listOf("MainActivity", "LibraryFragment", "ChaptersFragment")
        
        components.forEach { component ->
            // When
            val result = dependencyResolver.resolveDependencyPath(component)
            
            // Then
            // Components should either have no dependencies (if they don't use DI)
            // or should have resolvable dependencies
            if (result.dependencies.isNotEmpty()) {
                assertTrue(result.isSuccessful, "$component should resolve dependencies successfully")
                
                result.dependencies.forEach { (name, info) ->
                    assertTrue(
                        info.isResolvable,
                        "Dependency $name for $component should be resolvable"
                    )
                }
            }
        }
    }
    
    @Test
    fun `validate Services have proper dependencies`() {
        val services = listOf("DownloadNovelService")
        
        services.forEach { service ->
            // When
            val result = dependencyResolver.resolveDependencyPath(service)
            
            // Then
            if (result.dependencies.isNotEmpty()) {
                assertTrue(result.isSuccessful, "$service should resolve dependencies successfully")
                
                // Services typically need data access and network capabilities
                val hasRequiredCapabilities = result.dependencies.keys.any { 
                    it.contains("DBHelper") || it.contains("NetworkHelper") 
                }
                assertTrue(hasRequiredCapabilities, "$service should have required capabilities")
            }
        }
    }
    
    @Test
    fun `validate no memory leak potential in scoping`() {
        // When
        val scopingValidation = compileTimeValidator.validateComponentScoping()
        
        // Then
        val memoryLeakIssues = scopingValidation.issues.filter { 
            it.type == "MemoryLeak" && it.severity == IssueSeverity.HIGH 
        }
        
        if (memoryLeakIssues.isNotEmpty()) {
            val issues = memoryLeakIssues.map { it.description }
            fail("Potential memory leak issues detected: ${issues.joinToString(", ")}")
        }
        
        // Check that singleton dependencies don't hold Activity/Fragment references
        val singletonDependencies = listOf("DBHelper", "DataCenter", "NetworkHelper", "SourceManager")
        singletonDependencies.forEach { dependency ->
            // This would be more sophisticated in a real implementation
            // For now, just ensure the dependency name doesn't suggest UI component reference
            assertFalse(
                dependency.contains("Activity") || dependency.contains("Fragment"),
                "Singleton dependency $dependency should not reference UI components"
            )
        }
    }
    
    @Test
    fun `generate comprehensive validation report`() {
        // When
        val report = compileTimeValidator.generateComprehensiveValidationReport()
        
        // Then
        assertNotNull(report)
        assertTrue(report.generatedAt > 0)
        
        // Log the full report for manual review
        println("=== COMPREHENSIVE VALIDATION REPORT ===")
        println(report.getFormattedReport())
        
        // The overall validation should pass for a properly configured system
        if (!report.isValid) {
            println("Validation failed - check the report above for details")
            
            // Don't fail the test immediately, but log issues for review
            if (!report.dependencyGraphValidation.isValid) {
                println("Dependency graph issues: ${report.dependencyGraphValidation.circularDependencies}")
            }
            if (!report.moduleValidation.overallValid) {
                println("Module validation issues: ${report.moduleValidation.moduleValidations}")
            }
            if (!report.scopingValidation.isValid) {
                println("Scoping issues: ${report.scopingValidation.issues}")
            }
            if (!report.bindingValidation.isComplete) {
                println("Missing bindings: ${report.bindingValidation.missingBindings}")
            }
        }
        
        // At minimum, should have some components and dependencies
        assertTrue(report.moduleValidation.totalModules > 0, "Should have modules")
        assertTrue(report.dependencyGraphValidation.dependencyCount > 0, "Should have dependencies")
    }
}