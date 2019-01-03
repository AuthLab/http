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
