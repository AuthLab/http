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

package org.authlab.http.server

interface Filter {
    /**
     * Throws [FilterException] if handling should be aborted.
     */
    fun onRequest(request: ServerRequest<*>, context: MutableContext)

    fun abort(init: ServerResponseBuilder.() -> Unit) {
        throw FilterException(ServerResponseBuilder(init).build())
    }
}

interface FilterBuilder {
    fun build(): Filter
}

class FilterException(val response: ServerResponse) : Exception()

typealias FilterCallback = (ServerRequest<*>, MutableContext, abort: (ServerResponseBuilder.() -> Unit) -> Unit) -> Unit

class CallbackFilter(private val callback: FilterCallback) : Filter {
    override fun onRequest(request: ServerRequest<*>, context: MutableContext) {
        callback(request, context) { responseBuilderInit ->
            throw FilterException(ServerResponseBuilder(responseBuilderInit).build())
        }
    }
}

class CallbackFilterBuilder private constructor() : FilterBuilder {
    var callback: FilterCallback? = null

    constructor(init: CallbackFilterBuilder.() -> Unit) : this() {
        init()
    }

    fun onRequest(callback: FilterCallback) {
        this.callback = callback
    }

    override fun build(): Filter {
        val callback = this.callback ?: throw IllegalStateException("Callback not defined on CallbackFilterBuilder")

        return CallbackFilter(callback)
    }
}

class FilterHolder(entryPoint: String, val filter: Filter) : EntryPoint(entryPoint)

@ServerMarker
class FilterHolderBuilder private constructor() {
    var entryPoint: String = "/*"
    var filter: Filter? = null
    var filterBuilder: FilterBuilder? = null

    constructor(init: FilterHolderBuilder.() -> Unit) : this() {
        init()
    }

    fun onRequest(callback: FilterCallback) {
        filterBuilder = CallbackFilterBuilder {
            this.callback = callback
        }
    }

    fun build(): FilterHolder {
        val filter = this.filter ?: filterBuilder?.build() ?: throw IllegalStateException("Filter or FilterBuilder not defined for FilterHolder")

        return FilterHolder(entryPoint, filter)
    }
}