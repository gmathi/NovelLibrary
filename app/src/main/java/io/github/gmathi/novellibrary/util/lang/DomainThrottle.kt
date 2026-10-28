package io.github.gmathi.novellibrary.util.lang

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Bounds concurrent library-sync fetches so novels on different domains run in parallel while
 * requests to the SAME domain are throttled. Prevents the "update all" fan-out from hammering a
 * single host (which triggers Cloudflare / rate-limit blocks).
 *
 * A single global [Semaphore] caps total concurrent fetches; a lazily-created per-host semaphore
 * caps same-domain concurrency. Permits are acquired global-first then host, so acquisition order
 * is stable and cannot deadlock.
 */
class DomainThrottle(
    private val perDomainConcurrency: Int = DEFAULT_PER_DOMAIN,
    globalConcurrency: Int = DEFAULT_GLOBAL
) {
    private val globalGate = Semaphore(globalConcurrency.coerceAtLeast(1))
    private val hostGates = ConcurrentHashMap<String, Semaphore>()

    private fun hostKey(url: String): String =
        url.toHttpUrlOrNull()?.host?.removePrefix("www.")?.lowercase() ?: UNKNOWN_HOST

    private fun gateFor(host: String): Semaphore =
        hostGates.getOrPut(host) { Semaphore(perDomainConcurrency.coerceAtLeast(1)) }

    /** Runs [block] holding one global permit and one permit for [url]'s domain. */
    suspend fun <T> withDomainPermit(url: String, block: suspend () -> T): T =
        globalGate.withPermit {
            gateFor(hostKey(url)).withPermit {
                block()
            }
        }

    companion object {
        private const val UNKNOWN_HOST = "<unknown>"
        const val DEFAULT_PER_DOMAIN = 1
        const val DEFAULT_GLOBAL = 5
    }
}
