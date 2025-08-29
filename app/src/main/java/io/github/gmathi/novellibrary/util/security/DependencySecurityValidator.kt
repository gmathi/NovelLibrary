package io.github.gmathi.novellibrary.util.security

import android.app.Application
import android.content.Context
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates security aspects of injected dependencies
 * Ensures all dependencies meet security requirements before injection
 */
@Singleton
class DependencySecurityValidator @Inject constructor() {
    
    companion object {
        private const val TAG = "DependencySecurityValidator"
    }
    
    /**
     * Validates that the provided context is an Application context
     * Prevents potential security issues from using Activity contexts inappropriately
     */
    fun validateApplicationContext(context: Context) {
        require(context is Application) {
            "Security violation: Context must be Application context, got ${context::class.simpleName}"
        }
        
        Logs.debug(TAG, "Application context validation passed")
    }
    
    /**
     * Validates network configuration for security compliance
     */
    fun validateNetworkConfiguration(
        allowHttps: Boolean = true,
        allowHttp: Boolean = false,
        requireCertificatePinning: Boolean = false
    ) {
        require(allowHttps) {
            "Security violation: HTTPS must be allowed for secure network communication"
        }
        
        if (allowHttp) {
            Logs.warning(TAG, "HTTP connections are allowed - this may pose security risks")
        }
        
        if (requireCertificatePinning) {
            Logs.info(TAG, "Certificate pinning is required for enhanced security")
        }
        
        Logs.debug(TAG, "Network configuration validation passed")
    }
    
    /**
     * Validates database configuration for security
     */
    fun validateDatabaseSecurity(
        encryptionEnabled: Boolean = false,
        allowExternalAccess: Boolean = false
    ) {
        if (!encryptionEnabled) {
            Logs.warning(TAG, "Database encryption is disabled - consider enabling for sensitive data")
        }
        
        require(!allowExternalAccess) {
            "Security violation: External database access must be disabled"
        }
        
        Logs.debug(TAG, "Database security validation passed")
    }
    
    /**
     * Validates that a dependency is not null and properly initialized
     */
    fun <T : Any> validateDependency(dependency: T?, dependencyName: String): T {
        requireNotNull(dependency) {
            "Security violation: Required dependency '$dependencyName' is null"
        }
        
        Logs.debug(TAG, "Dependency '$dependencyName' validation passed")
        return dependency
    }
    
    /**
     * Validates injection timing to prevent race conditions
     */
    fun validateInjectionTiming(componentName: String, isInitialized: Boolean) {
        require(isInitialized) {
            "Security violation: Component '$componentName' accessed before proper initialization"
        }
        
        Logs.debug(TAG, "Injection timing validation passed for '$componentName'")
    }
}