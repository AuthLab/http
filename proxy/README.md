# Proxy

## Building with Gradle

```
../gradlew jar
```

## Packaging and running distribution

Package with:

```
../gradlew installDist
```

Run with:

```
./build/install/proxy/run
```

## Docker

Build container with:

```
docker build -t my-proxy .
```

Run container with:

```
docker run -p 8080:8080 -it --rm --name my-running-proxy my-proxy
```

# TODO

* Persistent connections (keep-alive)
* WebSockets
* HTTP/2
