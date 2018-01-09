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
