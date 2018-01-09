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
import sun.security.x509.AlgorithmId
import sun.security.x509.CertificateAlgorithmId
import sun.security.x509.CertificateExtensions
import sun.security.x509.CertificateSerialNumber
import sun.security.x509.CertificateValidity
import sun.security.x509.CertificateVersion
import sun.security.x509.CertificateX509Key
import sun.security.x509.DNSName
import sun.security.x509.GeneralName
import sun.security.x509.GeneralNames
import sun.security.x509.IPAddressName
import sun.security.x509.SubjectAlternativeNameExtension
import sun.security.x509.X500Name
import sun.security.x509.X509CertImpl
import sun.security.x509.X509CertInfo
import java.io.IOException
import java.net.Socket
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.Principal
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.SignatureException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Random
import javax.net.ssl.X509KeyManager

class CertificateGeneratingKeyManager(val caPrivateKey: PrivateKey, val caCertificate: X509Certificate,
                                      val defaultHostname: String? = null) : X509KeyManager {
    private val _KeyAndCertByAlias = mutableMapOf<String, Pair<PrivateKey, X509Certificate>>()

    companion object {
        private val _logger = loggerFor<CertificateGeneratingKeyManager>()

        private val _socketToHostname = mutableMapOf<Socket, String>()

        @Synchronized
        fun findHostname(socket: Socket) : String?
                = _socketToHostname[socket]

        @Synchronized
        fun mapSocketAndHostname(socket: Socket, hostname: String) {
            _logger.trace("Mapping $socket -> $hostname")
            _socketToHostname.put(socket, hostname)
        }
    }

    override fun getClientAliases(keyType: String, issuers: Array<out Principal>?): Array<String>? {
        _logger.warn("Client certificate authentication not supported")
        return null
    }

    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket): String? {
        _logger.warn("Client certificate authentication not supported")
        return null
    }

    override fun getServerAliases(keyType: String, issuers: Array<out Principal>?): Array<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun chooseServerAlias(keyType: String, issuers: Array<out Principal>?, socket: Socket): String? {
        _logger.trace("Choosing server alias for key type '$keyType' on socket $socket for issuers $issuers")

        if (!keyType.equals("RSA", ignoreCase = true)) {
            _logger.debug("Key type $keyType not supported")
            return null
        }

        return findHostname(socket).also { alias ->
            if (alias != null) {
                _logger.debug("Alias '$alias' chosen for socket $socket")
            } else {
                _logger.debug("No alias found for socket $socket")

                if (defaultHostname != null) {
                    _logger.debug("Mapping socket $socket to default hostname '$defaultHostname'")
                    mapSocketAndHostname(socket, defaultHostname)
                }
            }
        }
    }

    override fun getCertificateChain(alias: String): Array<X509Certificate>? {
        _logger.debug("Fetching certificate chain for '$alias'")

        return getKeyAndCertificate(alias).second
    }

    override fun getPrivateKey(alias: String): PrivateKey? {
        _logger.debug("Fetching private key for '$alias'")

        return getKeyAndCertificate(alias).first
    }

    @Synchronized
    private fun getKeyAndCertificate(alias: String) : Pair<PrivateKey, Array<X509Certificate>> {
        val keyAndCertificate = _KeyAndCertByAlias.computeIfAbsent(alias) {
            val extensions = CertificateExtensions()

            if (alias.first().isDigit()) {
                extensions.set(SubjectAlternativeNameExtension.NAME,
                        SubjectAlternativeNameExtension(GeneralNames().add(GeneralName(IPAddressName(alias)))))
            } else {
                extensions.set(SubjectAlternativeNameExtension.NAME,
                        SubjectAlternativeNameExtension(GeneralNames().add(GeneralName(DNSName(alias)))))
            }

            getSelfCertificate(X500Name("CN=$alias"), Date(), 365L * 24 * 3600, extensions, caPrivateKey, caCertificate)
        }

        return keyAndCertificate.first to arrayOf(keyAndCertificate.second, caCertificate)
    }

    /*
     * Extracted from sun.security.tools.keytool.CertAndKeyGen#getSelfCertificate(X500Name, Date, Long, CertificateExtensions)
     */
    @Throws(CertificateException::class, InvalidKeyException::class, SignatureException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class)
    private fun getSelfCertificate(name: X500Name, notBefore: Date, validFor: Long, extensions: CertificateExtensions?,
                                   signingKey: PrivateKey, signingCertificate: X509Certificate): Pair<PrivateKey, X509Certificate> {
        val keys = generate(1024)

        try {
            val notAfter = Date()
            notAfter.time = notBefore.time + validFor * 1000L
            val validity = CertificateValidity(notBefore, notAfter)
            val certInfo = X509CertInfo()
            certInfo.set("version", CertificateVersion(2))
            certInfo.set("serialNumber", CertificateSerialNumber(Random().nextInt() and 2147483647))
            val var10 = AlgorithmId.get("SHA256WithRSA")
            certInfo.set("algorithmID", CertificateAlgorithmId(var10))
            certInfo.set("subject", name)
            certInfo.set("key", CertificateX509Key(keys.second))
            certInfo.set("validity", validity)
            certInfo.set("issuer", signingCertificate.subjectDN)
            if (extensions != null) {
                certInfo.set("extensions", extensions)
            }

            val certificate = X509CertImpl(certInfo)
            certificate.sign(signingKey, "SHA256WithRSA")
            return keys.first to certificate
        } catch (e: IOException) {
            throw CertificateEncodingException("getSignedCertificate: " + e.message)
        }
    }

    /*
     * Extracted from sun.security.tools.keytool.CertAndKeyGen#generate(int)
     */
    @Throws(InvalidKeyException::class)
    private fun generate(keySize: Int): Pair<PrivateKey, PublicKey> {
        val var2: KeyPair
        val keyGen = KeyPairGenerator.getInstance("RSA")

        try {
            keyGen.initialize(keySize, SecureRandom())
            var2 = keyGen.generateKeyPair()
        } catch (var4: Exception) {
            throw IllegalArgumentException(var4.message)
        }

        val publicKey = var2.public
        val privateKey = var2.private

        if (!"X.509".equals(publicKey.getFormat(), ignoreCase = true)) {
            throw IllegalArgumentException("publicKey's is not X.509, but " + publicKey.getFormat())
        }

        return privateKey to publicKey
    }
}
