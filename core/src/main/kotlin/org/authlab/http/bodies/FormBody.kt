package org.authlab.http.bodies

import org.authlab.http.FormParameters
import org.authlab.http.ParametersBuilder
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class FormBody private constructor(val parameters: FormParameters, originalBody: RawBody?) :
        Body("application/x-www-form-urlencoded", null, originalBody) {
    constructor(parameters: FormParameters = FormParameters()) : this(parameters, null)

    companion object {
        fun fromRawBody(rawBody: RawBody): FormBody {
            val rawString = rawBody.bytes.toString(StandardCharsets.UTF_8)

            val parameters = FormParameters.fromString(rawString)

            return FormBody(parameters, rawBody)
        }
    }

    private val _string = lazy {
        parameters.toString()
    }

    fun withParameter(name: String, value: String): FormBody {
        return FormBody(parameters.withParameter(name, value)) // forget about the original raw body
    }

    override fun calculateSize()
            = toString().toByteArray().size

    override fun doWrite(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(toString())
        writer.flush()
    }

    override fun toString(): String {
        return _string.value
    }
}
