package io.github.gmathi.novellibrary.network

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.X509TrustManager

class MultiTrustManager : X509TrustManager {

    private val trustManagers: MutableSet<X509TrustManager> = HashSet()

    fun addTrustManager(trustManager: X509TrustManager) {
        trustManagers.add(trustManager)
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        if (trustManagers.isEmpty()) {
            throw CertificateException("No trust managers installed!")
        }
        var ce: CertificateException? = null
        for (trustManager in trustManagers) {
            ce = try {
                trustManager.checkClientTrusted(chain, authType)
                return
            } catch (trustCe: CertificateException) {
                trustCe
            }
        }
        ce?.let { throw it }
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        if (trustManagers.isEmpty()) {
            throw CertificateException("No trust managers installed!")
        }
        var ce: CertificateException? = null
        for (trustManager in trustManagers) {
            ce = try {
                trustManager.checkServerTrusted(chain, authType)
                return
            } catch (trustCe: CertificateException) {
                trustCe
            }
        }
        throw ce!!
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val certificates: MutableSet<X509Certificate> = HashSet()
        for (trustManager in trustManagers) {
            certificates.addAll(Arrays.asList(*trustManager.acceptedIssuers))
        }
        return certificates.toTypedArray()
    }
}