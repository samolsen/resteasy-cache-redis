# Resteasy Redis Cache

A [Redis](http://redis.io) backed server-side JAX-RS request cache for
[RESTEasy 3.x](http://resteasy.jboss.org).

See <https://docs.jboss.org/resteasy/docs/3.0.9.Final/userguide/html/Cache_NoCache_CacheControl.html>
for a description of the `@Cache` annotation for setting cache-control
headers and triggering back-end response caching.

## Installation

Include the cache as a dependency with Maven:

```xml
<dependency>
    <groupId>com.samolsen.resteasy-cache-redis</groupId>
    <artifactId>resteasy-cache-redis</artifactId>
    <version>1.0.0</version>
</dependency>
```

_Note_: For the time being, this artifact is only available on jCenter (maybe?). Maven Central is (hopefully) coming soon. 

## Use

In your `javax.ws.rs.core.Application` subclass, create a JedisPool
and provide it to the application via `getSingletons()`:

```java
@ApplicationPath("api")
public class ExampleApplication extends Application {

    private Set<Object> _singletons = new HashSet<Object>();
    private Set<Class<?>> _classes = new HashSet<Class<?>>();

    @PostConstruct
    void init()
    {
        JedisPool jedisPool = new JedisPool(); // create and configure
        RedisCache redisCache = new RedisCache(jedisPool, "keyNamespace");
        ServerCacheFeature cacheFeature = ServerCacheFeature(redisCache);
        _singletons.add(cacheFeature);

        /* Add your resource classes */
        _classes.add(ResponseResource.class);
    }

    @Override
    public Set<Object> getSingletons() { return _singletons; }

    @Override
    public Set<Class<?>> getClasses() { return _classes; }
}
```

You may then use the `@Cache` annotation to control caching for resource methods.

```java
public class ResponseResource {

    /**
     *  A positive max-age must be set, or the response will not be cached
     *  server side
     */
    @GET
    @Cache(maxAge = 30)
    public ResponseModel getCount()
    {
        return new ResponseModel();
    }
}
```

## Example Application

An example application is included in [modules/example](modules/example).

## License

Copyright (c) 2015, Sam Olsen

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
