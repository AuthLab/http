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

package org.authlab.util

import java.util.TreeMap

interface CaseInsensitiveMap<E> : Map<String, E> {
    fun toMutableMap(): MutableCaseInsensitiveMap<E>
            = MutableCaseInsensitiveMap(this)
}

class MutableCaseInsensitiveMap<E>() : TreeMap<String, E>(String.CASE_INSENSITIVE_ORDER),
        CaseInsensitiveMap<E> {
    constructor(other: Map<String, E>) : this() {
        putAll(other)
    }
}

private object EmptyCaseSensitiveMap : CaseInsensitiveMap<Nothing> {
    override val entries: Set<Map.Entry<String, Nothing>> = emptySet()
    override val keys: Set<String> = emptySet()
    override val size: Int = 0
    override val values: Collection<Nothing> = emptyList()

    override fun containsKey(key: String): Boolean = false

    override fun containsValue(value: Nothing): Boolean = false

    override fun get(key: String): Nothing? = null

    override fun isEmpty(): Boolean = true
}

@Suppress("UNCHECKED_CAST")
fun <E> emptyCaseInsensitiveMap(): CaseInsensitiveMap<E>
        = EmptyCaseSensitiveMap as CaseInsensitiveMap<E>

fun <E> caseInsensitiveMapOf(): CaseInsensitiveMap<E>
        = MutableCaseInsensitiveMap()

fun <E> caseInsensitiveMapOf(vararg pairs: Pair<String, E>): CaseInsensitiveMap<E>
        = MutableCaseInsensitiveMap<E>().apply { putAll(pairs) }

fun <E> mutableCaseInsensitiveMapOf(): MutableCaseInsensitiveMap<E>
        = MutableCaseInsensitiveMap()

fun <E> mutableCaseInsensitiveMapOf(vararg pairs: Pair<String, E>): MutableCaseInsensitiveMap<E>
        = MutableCaseInsensitiveMap<E>().apply { putAll(pairs) }

fun <E> Map<String, E>.toCaseInsensitiveMap(): CaseInsensitiveMap<E>
        = MutableCaseInsensitiveMap(this)

fun <E> Map<String, E>.toMutableCaseInsensitiveMap(): MutableCaseInsensitiveMap<E>
        = MutableCaseInsensitiveMap(this)

fun <E, M : MutableCaseInsensitiveMap<in E>> Iterable<Pair<String, E>>.toCaseInsensitiveMap(destination: M): M
        = destination.apply { putAll(this@toCaseInsensitiveMap) }

fun <E> Iterable<Pair<String, E>>.toCaseInsensitiveMap(): CaseInsensitiveMap<E> {
    if (this is Collection) {
        return when (size) {
            0 -> emptyCaseInsensitiveMap()
            1 -> caseInsensitiveMapOf(if (this is List) this[0] else iterator().next())
            else -> toCaseInsensitiveMap(MutableCaseInsensitiveMap())
        }
    }
    return toMap().toCaseInsensitiveMap()
}