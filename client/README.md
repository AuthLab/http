# HTTP Client

The AuthLab HTTP Client library is written in Kotlin.

## Examples

Clients are constructed through a builder:

```kotlin
val client = ClientBuilder {
	host { "www.example.com" }
	proxy { "localhost:8080" }
}.build()
```

There is also a pair of helper functions to configure and build (instantiate) a client in one go:

```kotlin
val client = buildClient("www.example.com")
```

```kotlin
val client = buildClient("www.example.com") {
	proxy { "localhost:8080" }
}
```

```kotlin
val client = buildClient {
	host { "www.example.com" }
	proxy { "localhost:8080" }
}
```


Clients are auto-closeable:

```kotlin
val response = buildClient("www.example.com")
		.use { client ->
			client.request().get()
		}.request().get()
```

Making a simple get request:

```kotlin
val response = client.request().get()
```

Making it a bit more advanced:

```kotlin
val response = client.request {
	path { "/some/place/nice" }
	query { "foo" to "bar" }
	header { "My-Header" to "foobar" }
}.get()
```

Posting a form:

```kotlin
val response = client.request {
	path { "/some/place/nice" }
}.post(FormBody(FormParameters().withParameter("foo", "bar")))
```

Posting an object as JSON:

```kotlin
val data = mapOf("foo" to "bar")
val response = client.request {
	path { "/some/place/nice" }
	accept { "application/json" }
}.post(JsonBody(data))

val responseData = (response.body as JsonBody).getTypedData<MyClass>()
```

## TODO

* Make how request-/response bodies are handled fit better with the overall build pattern.