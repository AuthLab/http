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

import org.authlab.http.Location
import org.authlab.http.Path
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.CachingDelayedBody
import java.util.regex.Pattern

open class EntryPoint internal constructor(val path: String,
                                           val entryPoints: List<EntryPoint>,
                                           val initializers: List<Initializer>,
                                           val finalizers: List<Finalizer>,
                                           val filters: List<Filter>,
                                           val transformers: List<Transformer>,
                                           val handlers: List<Handler<*>>) {
    val pathPattern: Pattern by lazy {
        Pattern.compile(path.split("*").joinToString(".*") { Regex.escape(it) })
    }

    fun getAllPaths(): List<String> {
        return entryPoints.asSequence()
                .flatMap { getAllPaths().asSequence() }
                .map { "$path/$it".replace(Regex("/*"), "/") }
                .toList()
    }

    val allPathPatterns: List<Pattern> by lazy {
        getAllPaths().map { path -> Pattern.compile(path.split("*")
                .joinToString(".*") { Regex.escape(it) }) }
    }

    fun handles(location: Location): Boolean {
        allPathPatterns
        return pathPattern.matcher(location.safePath).matches()
    }

    fun entryPointsFor(path: Path): List<EntryPoint> {

    }

    fun filter(request: ServerRequest<*>, context: MutableContext) : FilterResult {
        for (filter in filters) {
            val result = filter.onRequest(request, context)

            if (result !is AllowFilterResult) {
                return result
            }
        }

        return AllowFilterResult
    }

    fun onRequest(request: ServerRequest<CachingDelayedBody>,
                  onAbort: () -> ServerResponse): ServerResponse? {
        handlers
    }
}

@ServerMarker
open class EntryPointBuilder constructor(private val path: String = "*") {
    private val entryPointBuilders: MutableList<EntryPointBuilder> = mutableListOf()
    private val initializerBuilders: MutableList<InitializerBuilder> = mutableListOf()
    private val finalizerBuilders: MutableList<FinalizerBuilder> = mutableListOf()
    private val filterBuilders: MutableList<FilterBuilder> = mutableListOf()
    private val transformerBuilders: MutableList<TransformerBuilder> = mutableListOf()
    private val handlerBuilders: MutableList<HandlerBuilder<*>> = mutableListOf()

    constructor(path: String = "*", init: EntryPointBuilder.() -> Unit) : this(path) {
        init()
    }

    fun initialize(builder: InitializerBuilder) {
        initializerBuilders.add(builder)
    }

    fun initialize(initializer: Initializer) {
        initialize(object : InitializerBuilder {
            override fun build(): Initializer {
                return initializer
            }
        })
    }

    fun finalize(builder: FinalizerBuilder) {
        finalizerBuilders.add(builder)
    }

    fun finalize(finalizer: Finalizer) {
        finalize(object : FinalizerBuilder {
            override fun build(): Finalizer {
                return finalizer
            }
        })
    }

    fun filter(builder: FilterBuilder) {
        filterBuilders.add(builder)
    }

    fun filter(filter: Filter) {
        filter(object : FilterBuilder {
            override fun build(): Filter {
                return filter
            }
        })
    }

    fun transform(builder: TransformerBuilder) {
        transformerBuilders.add(builder)
    }

    fun transform(transformer: Transformer) {
        transform(object : TransformerBuilder {
            override fun build(): Transformer {
                return transformer
            }
        })
    }

    fun <B : Body> handle(builder: HandlerBuilder<B>) {
        handlerBuilders.add(builder)
    }

    fun <B : Body> handle(handler: Handler<B>) {
        handle(object : HandlerBuilder<B> {
            override fun build(): Handler<B> {
                return handler
            }
        })
    }

    fun path(path: String, init: EntryPointBuilder.() -> Unit) {
        entryPointBuilders.add(EntryPointBuilder(path, init))
    }

    open fun build(): EntryPoint {
        val entryPoints = entryPointBuilders.map(EntryPointBuilder::build)
        val initializers = initializerBuilders.map(InitializerBuilder::build)
        val finalizers = finalizerBuilders.map(FinalizerBuilder::build)
        val filters = filterBuilders.map(FilterBuilder::build)
        val transformers = transformerBuilders.map(TransformerBuilder::build)
        val handlers = handlerBuilders.map(HandlerBuilder<*>::build)

        return EntryPoint(path, entryPoints, initializers, finalizers, filters, transformers, handlers)
    }
}
