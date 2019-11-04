/*
 * MIT License
 *
 * Copyright (c) 2019 Johan Fylling
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
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.X509KeyManager

class SimpleKeyManager(private val delegate: X509KeyManager) : X509KeyManager {
    companion object {
        private val logger = loggerFor<SimpleKeyManager>()
    }

    override fun getClientAliases(keyType: String, issuers: Array<out Principal>?): Array<String>? {
        return delegate.getClientAliases(keyType, issuers)
                .also { aliases -> logger.debug("Server aliases for key-type '{}', issuers '{}': {}",
                        keyType, issuers, aliases) }
    }

    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket): String? {
       return delegate.chooseClientAlias(keyType, issuers, socket)
               .also { alias ->
                   if (alias != null) {
                       logger.debug("Chosen client alias for key-type: '{}', issuers: {}, socket: {}: '{}'",
                               keyType, issuers, socket, alias)
                   } else {
                       logger.debug("No client alias chosen for key-type: '{}', issuers: {}, socket: {}",
                               keyType, issuers, socket)
                   }
               }
    }

    override fun getServerAliases(keyType: String, issuers: Array<out Principal>?): Array<String>? {
        return delegate.getServerAliases(keyType, issuers)
                .also { aliases -> logger.debug("Server aliases for key-type '{}', issuers '{}': {}",
                        keyType, issuers, aliases) }
    }

    override fun chooseServerAlias(keyType: String, issuers: Array<out Principal>?, socket: Socket): String? {
        return delegate.chooseServerAlias(keyType, issuers, socket)
                .also { alias ->
                    if (alias != null) {
                        logger.debug("Chosen server alias for key-type: '{}', issuers: {}, socket: {}: '{}'",
                                keyType, issuers, socket, alias)
                    } else {
                        logger.debug("No server alias chosen for key-type: '{}', issuers: {}, socket: {}",
                                keyType, issuers, socket)
                    }
                }
    }

    override fun getCertificateChain(alias: String): Array<X509Certificate>? {
        return delegate.getCertificateChain(alias)
                .also { chain -> logger.debug("Certificate chain for alias '{}': {}",
                        alias, chain) }
    }

    override fun getPrivateKey(alias: String): PrivateKey? {
        return delegate.getPrivateKey(alias)
                .also { key ->
                    if (key != null) {
                        logger.debug("private key for alias '{}' selected: {}",
                                alias, key.encoded)
                    } else {
                        logger.debug("No private key selected for alias '{}'",
                                alias)
                    }
                }
    }
}