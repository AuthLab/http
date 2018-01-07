package org.authlab.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

inline fun <reified T:Any> loggerFor() = LoggerFactory.getLogger(T::class.java)

fun loggerFor(name: String) = LoggerFactory.getLogger(name)

fun markerFor(name: String) = MarkerFactory.getMarker(name)

fun Logger.debugIf(format: String, args: () -> Array<Any>) {
    if (isTraceEnabled) {
        trace(format, *args()) // Spread the args array into the vararg
    }
}

fun Logger.traceIf(format: String, args: () -> Array<Any>) {
    if (isTraceEnabled) {
        trace(format, *args()) // Spread the args array into the vararg
    }
}