package org.authlab.io

import java.io.PushbackInputStream

fun PushbackInputStream.readLine(): String? {
    val stringBuilder = StringBuilder()

    var char = read()

    if (char < 0) {
        return null
    }

    while (char >= 0 && char.toChar() != '\r' && char.toChar() != '\n') {
        stringBuilder.append(char.toChar())
        char = read()
    }

    if (char.toChar() == '\r') {
        val nextChar = read()
        if (nextChar >= 0 && nextChar.toChar() != '\n') {
            unread(nextChar)
        }
    }

    return stringBuilder.toString()
}
