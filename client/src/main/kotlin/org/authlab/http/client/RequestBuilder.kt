package org.authlab.http.client

import org.authlab.http.bodies.Body
import org.authlab.http.Response

interface RequestBuilder {
    var path: String
    var contentType: String?
    var accept: String?

    fun query(name: String, value: String? = null): RequestBuilder

    fun query(param: Pair<String, String>): RequestBuilder

    fun query(init: () -> Pair<String, String>) {
        query(init())
    }

    fun header(name: String, value: String): RequestBuilder

    fun header(header: Pair<String, String>): RequestBuilder

    fun header(init: () -> Pair<String, String>) {
        header(init())
    }

    fun get(path: String? = null): Response
    fun post(body: Body, path: String? = null): Response
    fun put(body: Body, path: String? = null): Response
    fun delete(path: String? = null): Response
    fun patch(body: Body, path: String? = null): Response
    fun execute(method: String, body: Body, path: String? = null): Response
}
