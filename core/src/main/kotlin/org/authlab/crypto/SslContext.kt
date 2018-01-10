/*
 * MIT License
 *
 * Copyright (c) 2018 Johan Fylling
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
                           defaultHostname: String = "localhost", storeGeneratedCertificates: Boolean = false) {
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

    val certificate = keyStore.getCertificateChain(alias)
            ?.filterIsInstance<X509Certificate>()
            ?.toTypedArray()
            ?: throw IllegalStateException("Found no X.509 certificate with alias '$alias'")
    val privateKey = keyStore.getKey(alias, password.toCharArray()) as? PrivateKey ?:
            throw IllegalStateException("Found no private key with alias '$alias'")

    val keyManager = CertificateGeneratingKeyManager(privateKey, certificate, defaultHostname, storeGeneratedCertificates)
    val trustManager = TrustAllTrustManager()

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(arrayOf(keyManager), arrayOf(trustManager), SecureRandom())
    SSLContext.setDefault(sslContext)
}
