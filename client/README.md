# HTTP Client

The AuthLab HTTP Client library is written in Kotlin.

## Examples

Clients are constructed through a builder:

```kotlin
val client = ClientBuilder("www.example.com") {
	proxy = "localhost:8080"
}.build()
```

There is also a helper function to configure and build (instantiate) a client in one go:

```kotlin
val client = buildClient("www.example.com")
```

```kotlin
val client = buildClient("www.example.com") {
	proxy = "localhost:8080"
}
```

Clients are auto-closeable:

```kotlin
val response = buildClient("www.example.com")
		.use { client ->
			client.request().get()
		}
```

Making a simple get request:

```kotlin
val response = client.request().get()
```

Making it a bit more advanced:

```kotlin
val response = client.request {
	accept = "text/plain"
	query { "foo" to "bar" }
	header { "My-Header" to "foobar" }
}.get("/some/place/nice")
```

Requesting JSON:

```kotlin
val data = client.request().getJson<MyClass>()
```

Posting a form:

```kotlin
val response = client.request {
	accept = "text/plain"
}.postForm("/some/place/nice", 
		mapOf("name" to "Foo", "surname" to "Bar"))
```

or:

```kotlin
val response = client.request {
	accept = "text/plain"
}.postForm("/some/place/nice") {
	param { "name" to "Foo" }
	param { "surname" to "Bar" }
}
```

Posting an object as JSON:

```kotlin
val requestData = mapOf("foo" to "bar")

val responseData = client.request()
		.postJson(requestData, "/some/place/nice")
		.asJson<MyClass>()
```

## TODO

* Authorization
* Connection pooling