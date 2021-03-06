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

import kotlin.reflect.KClass

interface Context {
    val data: Map<String, Any>

    fun <T:Any> get(key: String, type: KClass<out T>): T? {
        return type.javaObjectType.cast(data[key])
    }
}

inline fun <reified T:Any> Context.get(key: String): T? {
    return get(key, T::class)
}

class MutableContext(data: Map<String, Any> = mapOf()) : Context {
    override val data: MutableMap<String, Any> = data.toMutableMap()

    companion object {
        fun mutableCopyOf(other: Context): MutableContext {
            return MutableContext(other.data)
        }
    }

    fun set(key: String, data: Any) {
        this.data[key] = data
    }
}
