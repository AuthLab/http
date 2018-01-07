# HTTP Server

The AuthLab HTTP Server library is written in Kotlin.

## Examples

A server is constructed through a builder:

```kotlin
val server = Serverbuilder {
	host { "localhost" }
	port { 8080 }
}.build
```

There is also a helper function to configure and build (instantiate) a server in one go:

```kotlin
val server = buildServer {
	host { "localhost" }
	port { 8080 }
}
```

Running the server:

```kotlin
val server = buildServer {
	...
}.run()
```

Running the server in another thread:

```kotlin
val server = buildServer {
	...
}.also { Thread(it).start() }
```

Registering a request handler:

```kotlin
val server = buildServer {
	host { "localhost" }
	port { 8080 }
	
	handle("/foo") {
		status { 200 to "OK" }
		body { StringBody("bar") }
	}
}
```

Registering a default handler for when no handler meets the requested requirements:

```kotlin
val server = buildServer {
	host { "localhost" }
	port { 8080 }
	
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
	host { "localhost" }
	port { 8080 }
	
	default { request ->
		status { 200 to "OK" }
		body { JsonBody(request.toHar()) }
	}
}
```