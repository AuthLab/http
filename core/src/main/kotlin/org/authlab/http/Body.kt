package org.authlab.http

import com.google.gson.Gson
import org.authlab.util.loggerFor
import org.authlab.io.readLine
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PushbackInputStream
import java.nio.charset.StandardCharsets
import java.util.Arrays

fun emptyBody(): Body = Body.empty()

abstract class Body protected constructor(val contentType: String, val contentEncoding: String?,
                                          private val backingBody: Body? = null) {
    constructor(contentType: String, contentEncoding: String?) : this(contentType, contentEncoding, null)

    companion object {
        private val _logger = loggerFor<Body>()

        fun empty(): Body
                = EmptyBody()

        fun fromString(string: String): Body
                = StringBody(string)

        fun fromBytes(bytes: ByteArray): Body
                = RawBody(bytes)

        fun fromInputStream(inputStream: InputStream, headers: Headers, beforeBody: () -> Unit = {}): Body {
            val contentLengthHeader = headers.getHeader("Content-Length")
            val chunked = headers.getHeader("Transfer-Encoding")?.getFirst() == "chunked"

            return if (contentLengthHeader != null || chunked) {
                beforeBody()

                _logger.debug("Attempting to read body")

                fromInputStream(inputStream,
                        contentLengthHeader?.getFirstAsInt() ?: 0,
                        chunked,
                        headers.getHeader("Content-Type")?.getFirst(),
                        headers.getHeader("Content-Encoding")?.getFirst())
            } else {
                EmptyBody()
            }
        }

        fun fromInputStream(inputStream: InputStream, length: Int = 0, chunked: Boolean = false,
                            contentType: String? = null, contentEncoding: String? = null): Body {
            var rawBody = RawBody.fromInputStream(inputStream, length, chunked)

            if (contentType != null) {
                rawBody = rawBody.withContentType(contentType)
            }

            if (contentEncoding != null) {
                rawBody = rawBody.withContentEncoding(contentEncoding)
                return rawBody
            }

            return when (contentType?.split(";")?.first()) {
                "application/x-www-form-urlencoded" -> FormBody.fromRawBody(rawBody)
                "text/plain" -> StringBody.fromRawBody(rawBody)
                "text/html" -> StringBody.fromRawBody(rawBody)
                "application/json" -> JsonBody.fromRawBody(rawBody)
                else -> rawBody
            }
        }
    }

    val size: Int
        get() = backingBody?.size ?: calculateSize()

    protected abstract fun calculateSize(): Int

    fun write(outputStream: OutputStream) {
        if (backingBody != null) {
            backingBody.write(outputStream)
        } else {
            doWrite(outputStream)
        }
    }

    protected abstract fun doWrite(outputStream: OutputStream)
}

class EmptyBody: Body("", null) {
    override fun calculateSize() = 0

    override fun doWrite(outputStream: OutputStream) {
    }
}

class RawBody(val bytes: ByteArray, contentType: String = "application/octet-stream",
              contentEncoding: String? = null) : Body(contentType, contentEncoding) {
    companion object {
        private val _logger = loggerFor<RawBody>()

        fun fromInputStream(inputStream: InputStream, length: Int = 0, chunked: Boolean = false): RawBody {
            val pushbackInputStream = inputStream as? PushbackInputStream ?: PushbackInputStream(inputStream)

            val initialBufferSize = if (!chunked) {
                _logger.debug("Reading body with expected length: $length")
                length
            } else {
                _logger.debug("Reading body of unknown length (chunked)")
                1024
            }

            _logger.trace("initializing buffer size to $initialBufferSize")
            val buffer = ByteArrayOutputStream(initialBufferSize)

            do {
                val bytesToRead = if (chunked) {
                    readChunkHeader(pushbackInputStream)
                } else {
                    length
                }

                if (bytesToRead > 0) {
                    readStreamToBuffer(pushbackInputStream, buffer, bytesToRead, buffer.size())
                }

                if (chunked) {
                    // Chunk is terminated by new-line
                    pushbackInputStream.readLine()
                }
            } while (chunked && bytesToRead > 0)

            val body = RawBody(buffer.toByteArray())

            _logger.debug("Body read with size ${body.bytes.size}")
            _logger.trace("Body: $body")

            return body
        }

        private fun readChunkHeader(inputStream: PushbackInputStream): Int {
            _logger.trace("Reading chunk header")

            val chunkHeader = inputStream.readLine() ?: throw IllegalStateException("No chunk header")

            _logger.debug("Chunk header: $chunkHeader")

            return Integer.parseInt(chunkHeader.split(";", limit = 2).first().removePrefix("-"), 16)
        }

        private fun readStreamToBuffer(source: InputStream, target: ByteArrayOutputStream,
                                       length: Int, readOffset: Int = 0) {
            val totalBytesToRead = readOffset + length

            var value: Int
            do {
                value = source.read()
                if (value >= 0) {
                    target.write(value)
                    _logger.trace("Byte read: '${value.toChar()}' " +
                            "(${Integer.toHexString(value)} - ${Integer.toBinaryString(value)}); " +
                            "${target.size()} of $totalBytesToRead bytes read")
                }
            } while (value >= 0 && (target.size() < totalBytesToRead))

            if (target.size() < length) {
                _logger.warn("Only ${target.size()} of $totalBytesToRead bytes read")
            }
        }
    }

    fun withContentType(contentType: String) = RawBody(bytes, contentType, contentEncoding)

    fun withContentEncoding(contentEncoding: String) = RawBody(bytes, contentType, contentEncoding)

    override fun calculateSize()
            = bytes.size

    override fun doWrite(outputStream: OutputStream) {
        outputStream.write(bytes)
        outputStream.flush()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawBody

        if (!Arrays.equals(bytes, other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(bytes)
    }
}

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