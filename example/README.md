# Resteasy Redis Cache Example

This module contains an example application using Resteasy Redis Cache.

First, run `mvn package`. Then you may either deploy `target/cache-example.war`
to Wildfly, or use the included `Dockerfile` or `docker-compose.yml`.


```sh
mvn package && docker-compose build && docker-compose up
```

Depending on your environment, you may need to update values in
`host.properties` and/or `redis.properties`.

Once deployed, you may direct your browser to `http://{host}:{port}/cache-example/`
to view an API console demonstrating cache-able and no-cache methods.
