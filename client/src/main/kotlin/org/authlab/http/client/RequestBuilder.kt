package org.authlab.http.client

import org.authlab.http.Body
import org.authlab.http.Response

interface RequestBuilder {
    fun path(path: String): RequestBuilder

    fun path(init: () -> String) {
        path(init())
    }

    fun query(name: String, value: String? = null): RequestBuilder

    fun query(param: Pair<String, String>): RequestBuilder

    fun query(init: () -> Pair<String, String>) {
        query(init())
    }

    fun contentType(contentType: String): RequestBuilder

    fun contentType(init: () -> String) {
        contentType(init())
    }

    fun accept(accept: String): RequestBuilder

    fun accept(init: () -> String) {
        accept(init())
    }

    fun header(name: String, value: String): RequestBuilder

    fun header(header: Pair<String, String>): RequestBuilder

    fun header(init: () -> Pair<String, String>) {
        header(init())
    }

    fun get(): Response
    fun post(body: Body): Response
    fun put(body: Body): Response
    fun delete(): Response
    fun patch(body: Body): Response
    fun execute(method: String, body: Body): Response
}
