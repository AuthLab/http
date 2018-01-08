package org.authlab.http.bodies

import com.google.gson.Gson
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class JsonBody private constructor(data: Any?, originalBody: RawBody?) :
        Body("application/json", null, originalBody) {
    private val _data = data

    constructor(data: Any) : this(data, null)

    companion object {
        private val _gson = Gson()

        fun fromRawBody(rawBody: RawBody): JsonBody {
            return JsonBody(null, rawBody)
        }
    }

    val json: String by lazy {
        originalBody?.let {
            originalBody.bytes.toString(StandardCharsets.UTF_8)
        } ?: _gson.toJson(data)
    }

    val data: Any by lazy {
        data ?: _gson.fromJson(json, Any::class.java)
    }

    inline fun <reified T> getTypedData()
            = getTypedData(T::class.java)

    fun <T> getTypedData(type: Class<T>): T {
        return _gson.fromJson(json, type)
    }

    override fun calculateSize()
            = json.length

    override fun doWrite(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(json)
        writer.flush()
    }

    override fun toString(): String {
        return json
    }
}