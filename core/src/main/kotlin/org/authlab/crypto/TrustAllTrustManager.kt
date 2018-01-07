package org.authlab.crypto

import org.authlab.util.loggerFor
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class TrustAllTrustManager : X509TrustManager {
    companion object {
        private val _logger = loggerFor<TrustAllTrustManager>()
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        _logger.debug("Checking client trust for $authType")
        _logger.debug("Certificate chain: {}", chain)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        _logger.debug("Checking server trust for $authType")
        _logger.debug("Certificate chain: {}", chain)
    }
}
