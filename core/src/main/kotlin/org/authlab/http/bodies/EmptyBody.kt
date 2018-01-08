package org.authlab.http.bodies

import java.io.OutputStream

class EmptyBody: Body("", null) {
    override fun calculateSize() = 0

    override fun doWrite(outputStream: OutputStream) {
    }
}

fun emptyBody(): Body = Body.empty()
