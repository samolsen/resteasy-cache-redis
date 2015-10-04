package com.samolsen.resteasy.cache.redis;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.plugins.cache.server.ServerCacheFeature;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ResourceBundle;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

/**
 * Copy of https://github.com/resteasy/Resteasy/blob/master/jaxrs/resteasy-cache/resteasy-cache-core/src/test/java/org/jboss/resteasy/test/cache/ServerCacheTest.java
 * updated for {@link RedisCache}
 *
 * Integration test requires Redis to be running. Connection configuration in
 * resources/redis.properties
 */
public class RedisCacheIT extends BaseResourceTest {
    private static int count = 0;
    private static int plainCount = 0;
    private static int htmlCount = 0;

    private static RedisCache redisCache;

    @Path("/cache")
    public static class MyService {
        @GET
        @Produces("text/plain")
        @Cache(maxAge = 2)
        public String get() {
            count++;
            return "hello world" + count;
        }

        @PUT
        @Consumes("text/plain")
        public void put(String val) {
        }

        @GET
        @Produces("text/plain")
        @Path("accepts")
        @Cache(maxAge = 2)
        public String getPlain() {
            plainCount++;
            return "plain" + plainCount;
        }

        @GET
        @Produces("text/html")
        @Path("accepts")
        @Cache(maxAge = 2)
        public String getHtml() {
            htmlCount++;
            return "html" + htmlCount;
        }

        @GET
        @Produces("text/plain")
        @Path("stuff")
        @Cache(maxAge = 2)
        public String getStuff() {
            count++;
            return "stuff";
        }
    }

    @Path("/cache")
    public static interface MyProxy {
        @GET
        @Produces("text/plain")
        public String get();

    }


    @Before
    public void setUp() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("redis");
        String host = bundle.getString("host");
        int port = Integer.parseInt(bundle.getString("port"));
        int timeout = Integer.parseInt(bundle.getString("timeout"));
        String password = bundle.getString("password");
        if ("".equals(password)) password = null;

        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), host, port, timeout, password);
        redisCache = new RedisCache(jedisPool, "test");
        ServerCacheFeature cacheFeature = new ServerCacheFeature(redisCache);

        getProviderFactory().register(cacheFeature);
        getProviderFactory().register(JacksonJsonProvider.class);
        addPerRequestResource(MyService.class);
    }

    @After
    public void tearDown() {
        redisCache.clear();
    }

    @Test
    public void testNoCacheHitValidation() throws Exception {
        // test that after a cache expiration NOT MODIFIED is still returned if matching etags

        count = 0;
        String etag = null;
        {
            ClientRequest request = new ClientRequest(generateURL("/cache/stuff"));
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals(response.getEntity(), "stuff");
        }


        Thread.sleep(2000);

        {
            ClientRequest request = new ClientRequest(generateURL("/cache/stuff"));
            request.header(HttpHeaders.IF_NONE_MATCH, etag);
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            Assert.assertEquals(2, count);
        }
    }


    @Test
    public void testCache() throws Exception {
        count = 0;
        String etag = null;
        {
            ClientRequest request = new ClientRequest(generateURL("/cache"));
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals(response.getEntity(), "hello world" + 1);
        }


        {
            ClientRequest request = new ClientRequest(generateURL("/cache"));
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals(response.getEntity(), "hello world" + 1);
        }
        // test if-not-match
        {
            ClientRequest request = new ClientRequest(generateURL("/cache"));
            request.header(HttpHeaders.IF_NONE_MATCH, etag);
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
        }


        Thread.sleep(2000);

        {
            ClientRequest request = new ClientRequest(generateURL("/cache"));
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals(response.getEntity(), "hello world" + 2);
        }

        {
            ClientRequest request = new ClientRequest(generateURL("/cache"));
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals(response.getEntity(), "hello world" + 2);
        }

        {
            ClientRequest request = new ClientRequest(generateURL("/cache"));
            ClientResponse response = request.body("text/plain", "yo").put();
            Assert.assertEquals(204, response.getStatus());
        }
        {
            ClientRequest request = new ClientRequest(generateURL("/cache"));
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals(response.getEntity(), "hello world" + 3);
        }
    }


    @Test
    public void testAccepts() throws Exception {
        count = 0;
        plainCount = 0;
        htmlCount = 0;
        String etag = null;
        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.accept("text/plain");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals(response.getEntity(), "plain" + 1);
        }

        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.accept("text/plain");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("plain" + 1, response.getEntity());
        }

        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.accept("text/html");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("html" + 1, response.getEntity());
        }
        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.accept("text/html");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("html" + 1, response.getEntity());
        }
    }

    @Test
    public void testPreferredAccepts() throws Exception {
        count = 0;
        plainCount = 0;
        htmlCount = 0;
        String etag = null;
        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.accept("text/plain");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("plain" + 1, response.getEntity());
        }

        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.accept("text/html");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("html" + 1, response.getEntity());
        }

        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.header(HttpHeaders.ACCEPT, "text/html;q=0.5, text/plain");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("plain" + 1, response.getEntity());
        }
        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.header(HttpHeaders.ACCEPT, "text/plain;q=0.5, text/html");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("html" + 1, response.getEntity());
        }
    }

    @Test
    public void testPreferredButNotCachedAccepts() throws Exception {
        count = 0;
        plainCount = 0;
        htmlCount = 0;
        String etag = null;
        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.accept("text/plain");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("plain" + 1, response.getEntity());
        }

        // we test that the preferred can be handled
        {
            ClientRequest request = new ClientRequest(generateURL("/cache/accepts"));
            request.header(HttpHeaders.ACCEPT, "text/plain;q=0.5, text/html");
            ClientResponse<String> response = request.get(String.class);
            Assert.assertEquals(200, response.getStatus());
            String cc = response.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            Assert.assertNotNull(cc);
            etag = response.getResponseHeaders().getFirst(HttpHeaders.ETAG);
            Assert.assertNotNull(etag);
            Assert.assertEquals("html" + 1, response.getEntity());
        }
    }


}