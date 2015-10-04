# Resteasy Redis Cache

A [Redis](http://redis.io) backed server-side JAX-RS request cache for
[RESTEasy](http://resteasy.jboss.org).

See <https://docs.jboss.org/resteasy/docs/3.0.9.Final/userguide/html/Cache_NoCache_CacheControl.html>
for a description of the `@Cache` annotation for setting cache-control
headers and triggering back-end response caching.

## Installation

Include the cache as a dependency with Maven:

```xml
<dependency>
    <groupId>com.samolsen</groupId>
    <artifactId>resteasy-cache-redis</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Use

