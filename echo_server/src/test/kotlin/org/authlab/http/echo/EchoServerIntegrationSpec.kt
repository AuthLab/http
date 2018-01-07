package org.authlab.http.echo

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.authlab.http.JsonBody
import org.authlab.http.client.ClientBuilder

class EchoServerIntegrationSpec : StringSpec() {
    private val _port = 9090

    @Suppress("unused")
    private val _echoServer = autoClose(EchoServerBuilder {
        host { "localhost" }
        port { _port }
    }.build().also { Thread(it).start() })

    private var _client = autoClose(ClientBuilder("http://localhost:$_port").build())

    override val oneInstancePerTest = false

    init {
        "A simple GET request is properly echoed as JSON" {
            val response = _client.request {
                path { "/foo/bar" }
                query { "1337" to "42" }
                header { "Custom-Header" to "Not a footer" }
            }.get()

            response.responseLine.statusCode shouldBe 200

            response.headers.getHeader("Content-Type")!!.getFirst() shouldBe "application/json"

            val json = (response.body as JsonBody).getTypedData<Map<String, Any>>()

            println(json)

            json["url"] shouldBe "http://localhost:$_port/foo/bar?1337=42"
            json["httpVersion"] shouldBe "HTTP/1.1"
            json["bodySize"] shouldBe 0.0

            val query = (json["queryString"] as List<*>).filterIsInstance<Map<String, String>>()
            query.find { it["name"] == "1337" }!!["value"] shouldBe "42"

            val headers = (json["headers"] as List<*>).filterIsInstance<Map<String, String>>()
            headers.find { it["name"] == "Host" }!!["value"] shouldBe "localhost:$_port"
            headers.find { it["name"] == "Custom-Header" }!!["value"] shouldBe "Not a footer"
        }
    }
}
