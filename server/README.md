# HTTP Server

The AuthLab HTTP Server library is written in Kotlin.

## Examples

A server is constructed through a builder:

```kotlin
val server = Serverbuilder {
    listen { 
        host = "localhost"
        port = 8080
    }
    listen {
        host = "localhost"
        port = 8443
        secure = true
    }
    threadPoolSize = 100
}.build()
```

There is also a helper function to configure and build (instantiate) a server in one go:

```kotlin
val server = buildServer {
    listen { 
        host = "localhost"
        port = 8080
    }
}
```

Running the server:

```kotlin
val server = buildServer {
	...
}.start() // This call is non-blocking; each listener will consume a thread on the internal pool
```

Registering a request handler:

```kotlin
val server = buildServer {
    listen { 
        host = "localhost"
        port = 8080
    }
	
    handle("/foo") {
        status { 200 to "OK" }
        body { StringBody("bar") }
    }
}
```

Registering a default handler for when no handler meets the requested requirements:

```kotlin
val server = buildServer {
    listen { 
        host = "localhost"
        port = 8080
    }
	
    handle("/foo") {
        status { 200 to "OK" }
        body { StringBody("bar") }
    }
	
    default { request ->
        status { 404 to "Not Found" }
        body { StringBody("Sorry, I could not find ${request.path}") }
    }
}
```

Creating a simple echo server:

```kotlin
val server = buildServer {
    listen { 
        host = "localhost"
        port = 8080
    }
	
    default { request ->
        status { 200 to "OK" }
        body { JsonBody(request.toHar()) }
    }
}
```