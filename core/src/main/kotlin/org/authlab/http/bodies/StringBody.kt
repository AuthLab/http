package org.authlab.http.bodies

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

open class StringBody protected constructor(val data: String, contentType: String = "text/plain",
                      originalBody: RawBody?): Body(contentType, null, originalBody) {
    constructor(data: String, contentType: String = "text/plain") : this(data, contentType, null)

    companion object {
        fun fromRawBody(rawBody: RawBody): StringBody {
            val rawString = rawBody.bytes.toString(StandardCharsets.UTF_8)

            return StringBody(rawString, originalBody = rawBody)
        }
    }

    override fun calculateSize()
            =  data.length

    override fun doWrite(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(data)
        writer.flush()
    }
}