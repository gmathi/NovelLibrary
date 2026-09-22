package io.github.gmathi.novellibrary.network.cloudflare

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
     * Get all valid cookies for a host.
     *
     * Cloudflare issues cf_clearance for a specific host, but the challenge is often
     * solved on a redirected variant (e.g. the request targets `novelupdates.com` but
     * Cloudflare redirects to `www.novelupdates.com` and binds the cookie there). To make
     * a cookie solved on one variant usable by requests to the other, we look up the exact
     * host first and then fall back to the registrable-domain variants (bare / www / m).
     */
    fun getCookies(url: HttpUrl): List<Cookie> {
        val host = url.host
        cleanExpiredCookies(host)
        cookieStore[host]?.let { if (it.isNotEmpty()) return it.toList() }

        // Fall back to sibling host variants that share the same registrable domain.
        for (variant in hostVariants(host)) {
            if (variant == host) continue
            cleanExpiredCookies(variant)
            cookieStore[variant]?.let { if (it.isNotEmpty()) return it.toList() }
        }
        return emptyList()
    }

    /**
     * Build the set of host variants that should share Cloudflare clearance, e.g.
     * `www.novelupdates.com` -> [novelupdates.com, www.novelupdates.com, m.novelupdates.com].
     */
    private fun hostVariants(host: String): List<String> {
        val bare = host.removePrefix("www.").removePrefix("m.")
        return listOf(bare, "www.$bare", "m.$bare")
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
     * Remove Cloudflare cookies for a host and all of its sibling variants (bare / www / m),
     * so a stale clearance solved on one variant is fully cleared.
     */
    fun clearCookiesAllVariants(url: HttpUrl) {
        hostVariants(url.host).forEach { cookieStore.remove(it) }
    }
    
    /**
     * Remove all cookies for a specific host
     */
    fun clearCookies(url: HttpUrl) {
        cookieStore.remove(url.host)
    }

    /**
     * All hosts currently tracked in the in-memory store. Used to also clear the matching
     * cookies from the underlying WebView/OkHttp cookie jar when wiping everything.
     */
    fun knownHosts(): List<String> = cookieStore.keys.toList()

    /**
     * Remove every tracked Cloudflare cookie for every host, used by the "Clear Cloudflare
     * cookies" setting.
     */
    fun clearAllCookies() {
        cookieStore.clear()
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
