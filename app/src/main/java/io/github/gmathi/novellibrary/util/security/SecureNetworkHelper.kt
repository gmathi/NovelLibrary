package io.github.gmathi.novellibrary.util.security

import android.content.Context
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.NetworkTimeoutConfig
import io.github.gmathi.novellibrary.util.Logs
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.CertificateException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Secure wrapper around NetworkHelper with enhanced security features
 * Provides additional security validation and configuration
 */
@Singleton
class SecureNetworkHelper @Inject constructor(
    private val context: Context,
    private val validator: DependencySecurityValidator,
    private val dataCenter: io.github.gmathi.novellibrary.model.preference.DataCenter
) {
    
    companion object {
        private const val TAG = "SecureNetworkHelper"
    }
    
    private val _networkHelper: NetworkHelper by lazy {
        validator.validateApplicationContext(context)
        validator.validateNetworkConfiguration(
            allowHttps = true,
            allowHttp = false, // Disable HTTP for security
            requireCertificatePinning = false
        )
        val timeoutConfig = NetworkTimeoutConfig(dataCenter)
        NetworkHelper(context, dataCenter, timeoutConfig)
    }
    
    /**
     * Creates a secure OkHttpClient with enhanced security settings
     */
    fun createSecureHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                // Add certificate pinning for critical domains
                certificatePinner(createCertificatePinner())
                
                // Add security interceptors
                addInterceptor { chain ->
                    val request = chain.request()
                    
                    // Validate HTTPS usage
                    if (request.url.scheme != "https") {
                        throw SecurityException("HTTP requests are not allowed for security reasons")
                    }
                    
                    // Add security headers
                    val secureRequest = request.newBuilder()
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .addHeader("Cache-Control", "no-cache")
                        .build()
                    
                    chain.proceed(secureRequest)
                }
            }
            .build()
    }
    
    /**
     * Creates certificate pinner for enhanced security
     */
    private fun createCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            // Add certificate pins for critical novel sources
            // Note: These should be updated based on actual novel source domains
            .add("*.googleapis.com", "sha256/example-pin-here")
            .build()
    }
    
    /**
     * Validates URL for security before making requests
     */
    fun validateUrl(url: String): Boolean {
        return try {
            require(url.startsWith("https://")) {
                "Security violation: Only HTTPS URLs are allowed"
            }
            
            // Additional URL validation
            require(!url.contains("localhost") && !url.contains("127.0.0.1")) {
                "Security violation: Local URLs are not allowed"
            }
            
            Logs.debug(TAG, "URL validation passed for: $url")
            true
        } catch (e: Exception) {
            Logs.error(TAG, "URL validation failed for: $url", e)
            false
        }
    }
    
    /**
     * Secure method to get network helper with validation
     */
    fun getNetworkHelper(): NetworkHelper {
        // Note: _networkHelper is lazy-initialized, so no need to check isInitialized
        return _networkHelper
    }
}