package org.authlab.http.server

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.authlab.http.StringBody

class ServerResponseBuilderSpec : StringSpec() {
    init {
        "The builder can produce the expected response" {
            val serverResponse = ServerResponseBuilder {
                status { 200 to "OK" }
                header { "foo" to "bar" }
                body { StringBody("lorem ipsum ...") }
            }.build()

            serverResponse.statusCode shouldBe 200
            serverResponse.contentType shouldBe "text/plain"
            serverResponse.contentLength shouldBe 15

            serverResponse.internalResponse.run {
                headers.getHeader("Content-Type")?.values?.size shouldBe 1
                headers.getHeader("Content-Length")?.values?.size shouldBe 1
            }
        }
    }
}
