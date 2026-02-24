package io.github.gmathi.novellibrary.network

import okhttp3.Cookie
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced Cloudflare cookie manager for better cookie persistence and management
 */
class CloudflareCookieManager {
    
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
    
    /**
     * Store Cloudflare cookies for a specific host
     */
    fun storeCookies(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val existingCookies = cookieStore.getOrPut(host) { mutableListOf() }
        
        cookies.forEach { newCookie ->
            // Remove old cookie with same name if exists
            existingCookies.removeAll { it.name == newCookie.name }
            existingCookies.add(newCookie)
        }
        
        // Clean up expired cookies
        cleanExpiredCookies(host)
    }
    
    /**
     * Get all valid cookies for a host
     */
    fun getCookies(url: HttpUrl): List<Cookie> {
        val host = url.host
        cleanExpiredCookies(host)
        return cookieStore[host]?.toList() ?: emptyList()
    }
    
    /**
     * Get specific Cloudflare clearance cookie
     */
    fun getClearanceCookie(url: HttpUrl): Cookie? {
        return getCookies(url).firstOrNull { it.name == "cf_clearance" }
    }
    
    /**
     * Check if valid Cloudflare cookies exist for a host
     */
    fun hasValidCookies(url: HttpUrl): Boolean {
        val clearance = getClearanceCookie(url)
        return clearance != null && !isCookieExpired(clearance)
    }
    
    /**
     * Remove all cookies for a specific host
     */
    fun clearCookies(url: HttpUrl) {
        cookieStore.remove(url.host)
    }
    
    /**
     * Remove expired cookies for a host
     */
    private fun cleanExpiredCookies(host: String) {
        cookieStore[host]?.removeAll { isCookieExpired(it) }
    }
    
    /**
     * Check if a cookie is expired
     */
    private fun isCookieExpired(cookie: Cookie): Boolean {
        return cookie.expiresAt < System.currentTimeMillis()
    }
    
    /**
     * Get all Cloudflare-related cookie names
     */
    companion object {
        val CLOUDFLARE_COOKIE_NAMES = listOf(
            "cf_clearance",
            "__cf_bm",
            "cf_chl_2",
            "cf_chl_prog",
            "__cfduid"
        )
        
        /**
         * Check if a cookie is Cloudflare-related
         */
        fun isCloudflareCookie(cookie: Cookie): Boolean {
            return cookie.name in CLOUDFLARE_COOKIE_NAMES
        }
    }
}
