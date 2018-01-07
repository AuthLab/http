package org.authlab.crypto

import org.authlab.util.loggerFor
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext

private val _logger = loggerFor("SSL")

fun setupDefaultSslContext(caFilePath: String = "./ca.p12", password: String = "rootroot",
                           defaultHostname: String = "localhost") {
    val keyStore = KeyStore.getInstance("PKCS12")

    val caFile = try {
        File(caFilePath).inputStream()
    } catch (e: FileNotFoundException) {
        _logger.debug("CA file not found, attempting to load from resource instead")
        ClassLoader.getSystemResourceAsStream(caFilePath)
    }

    keyStore.load(caFile, password.toCharArray())

    val aliases = keyStore.aliases().toList()

    _logger.trace("CA keystore aliases: $aliases")

    val alias = when {
        aliases.isEmpty() -> throw IllegalStateException("No CA certificate found in keystore")
        aliases.size == 1 -> aliases.first()
        else -> aliases.find { it.equals("CA", ignoreCase = true) } ?: aliases.first()
    }

    _logger.debug("Using CA '$alias'")

    val certificate = keyStore.getCertificate(alias) as? X509Certificate ?:
            throw IllegalStateException("Found no X.509 certificate with alias '$alias'")
    val privateKey = keyStore.getKey(alias, password.toCharArray()) as? PrivateKey ?:
            throw IllegalStateException("Found no private key with alias '$alias'")

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(arrayOf(CertificateGeneratingKeyManager(privateKey, certificate, defaultHostname)), arrayOf(TrustAllTrustManager()), SecureRandom())
    SSLContext.setDefault(sslContext)
}
