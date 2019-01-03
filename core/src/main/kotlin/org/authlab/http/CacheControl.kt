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

package org.authlab.http

import org.authlab.util.CaseInsensitiveMap
import org.authlab.util.caseInsensitiveMapOf
import org.authlab.util.toCaseInsensitiveMap
import org.authlab.util.toMutableCaseInsensitiveMap
import java.time.Duration

class CacheControl(parameters: CaseInsensitiveMap<Parameter> = caseInsensitiveMapOf())
    : Parameters<CacheControl>(parameters) {
    companion object {
        fun fromString(input: String?): CacheControl {
            return CacheControl(Parameter.fromString(input, ",").toCaseInsensitiveMap())
        }

        fun fromHeader(header: Header): CacheControl {
            return header.values.fold(CacheControl()) { left, right ->
                left.join(fromString(right))
            }
        }

        fun fromHeaders(headers: Headers): CacheControl {
            val cacheControlHeader = headers["Cache-Control"]

            return if (cacheControlHeader != null) {
                fromHeader(cacheControlHeader)
            } else {
                CacheControl()
            }
        }
    }

    fun join(other: CacheControl): CacheControl {
        val mutableParameters = parameters.toMutableCaseInsensitiveMap()

        other.parameters.forEach { parameterName, otherParameter ->
            mutableParameters.merge(parameterName, otherParameter) { _, currentParameter ->
                currentParameter.withValues(otherParameter.values)
            }
        }

        return CacheControl(mutableParameters)
    }

    override fun newInstance(parameters: Map<String, Parameter>): CacheControl
            = CacheControl(parameters.toCaseInsensitiveMap())

    val noCache: Boolean
        get() = parameters["no-cache"] != null

    val noStore: Boolean
        get() = parameters["no-store"] != null

    val noTransform: Boolean
        get() = parameters["no-transform"] != null

    val maxAge: Duration?
        get() = parameters["max-age"]?.firstOrNull()?.toLongOrNull()
                ?.let { Duration.ofSeconds(it) }

    // request directives

    val maxStale: Duration?
        get() = parameters["max-stale"]?.firstOrDefault("0")
                ?.toLongOrNull()?.let { Duration.ofSeconds(it) }

    val minFresh: Duration?
        get() = parameters["min-fresh"]?.firstOrNull()?.toLongOrNull()
                ?.let { Duration.ofSeconds(it) }

    val onlyIfCached: Boolean
        get() = parameters["only-if-cached"] != null

    // response directives

    val mustRevalidate: Boolean
        get() = parameters["must-revalidate"] != null

    val public: Boolean
        get() = parameters["public"] != null

    val private: Boolean
        get() = parameters["private"] != null

    val proxyRevalidate: Boolean
        get() = parameters["proxy-revalidate"] != null

    val sMaxAge: Duration?
        get() = parameters["s-maxage"]?.firstOrNull()?.toLongOrNull()
                ?.let { Duration.ofSeconds(it) }

    // extension directives

    val immutable: Boolean
        get() = parameters["immutable"] != null

    val staleWhileRevalidate: Duration?
        get() = parameters["stale-while-revalidate"]?.firstOrNull()?.toLongOrNull()
                ?.let { Duration.ofSeconds(it) }

    val staleIfError: Duration?
        get() = parameters["stale-if-error"]?.firstOrNull()?.toLongOrNull()
                ?.let { Duration.ofSeconds(it) }

    fun getParameter(name: String): Parameter? {
        return getParameter(name, true)
    }

    override fun toString() = toString(",")
}