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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
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
    private static int COUNT = 0;
    private static int PLAIN_COUNT = 0;
    private static int HTML_COUNT = 0;

    private static RedisCache _redisCache;
    private static JedisPool _jedisPool;

    @Path("/cache")
    public static class MyService {
        @GET
        @Produces("text/plain")
        @Cache(maxAge = 2)
        public String get()
        {
            COUNT++;
            return "hello world" + COUNT;
        }

        @PUT
        @Consumes("text/plain")
        public void put( String val )
        {
        }

        @GET
        @Produces("text/plain")
        @Path("accepts")
        @Cache(maxAge = 2)
        public String getPlain()
        {
            PLAIN_COUNT++;
            return "plain" + PLAIN_COUNT;
        }

        @GET
        @Produces("text/html")
        @Path("accepts")
        @Cache(maxAge = 2)
        public String getHtml()
        {
            HTML_COUNT++;
            return "html" + HTML_COUNT;
        }

        @GET
        @Produces("text/plain")
        @Path("stuff")
        @Cache(maxAge = 2)
        public String getStuff()
        {
            COUNT++;
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
    public void setUp()
            throws Exception
    {
        ResourceBundle bundle = ResourceBundle.getBundle("redis");
        String host = bundle.getString("host");
        int port = Integer.parseInt(bundle.getString("port"));
        int timeout = Integer.parseInt(bundle.getString("timeout"));
        String password = bundle.getString("password");
        if( "".equals(password) )
        {
            password = null;
        }

        _jedisPool = new JedisPool(new JedisPoolConfig(), host, port, timeout, password);
        _redisCache = new RedisCache(_jedisPool, "cacheTest");
        ServerCacheFeature cacheFeature = new ServerCacheFeature(_redisCache);

        getProviderFactory().register(cacheFeature);
        getProviderFactory().register(JacksonJsonProvider.class);
        addPerRequestResource(MyService.class);
    }

    @After
    public void tearDown()
    {
        _redisCache.clear();
    }

    @Test
    public void testNoCacheHitValidation()
            throws Exception
    {
        // test that after a cache expiration NOT MODIFIED is still returned if matching etags

        COUNT = 0;
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
            Assert.assertEquals(2, COUNT);
        }
    }


    @Test
    public void testCache()
            throws Exception
    {
        COUNT = 0;
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
    public void testAccepts()
            throws Exception
    {
        COUNT = 0;
        PLAIN_COUNT = 0;
        HTML_COUNT = 0;
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
    public void testPreferredAccepts()
            throws Exception
    {
        COUNT = 0;
        PLAIN_COUNT = 0;
        HTML_COUNT = 0;
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
    public void testPreferredButNotCachedAccepts()
            throws Exception
    {
        COUNT = 0;
        PLAIN_COUNT = 0;
        HTML_COUNT = 0;
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

    @Test
    public void testDeletePrefixedKeys()
    {
        String prefix = "myPrefix:";
        // use a large enough iteration count so SCAN does not return
        // the full set on the 0 cursor
        Map<String, String> kvPairs = new HashMap<String, String>();
        for( int i = 0; i < 50; i++ )
        {
            String key = prefix + "key:" + i;
            kvPairs.put(key, "value" + i);
        }

        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();

            for( Map.Entry<String, String> pair : kvPairs.entrySet() )
            {
                jedis.set(pair.getKey(), pair.getValue());
            }

            _redisCache.deletePrefixedKeys(prefix + "*");

            for( String key : kvPairs.keySet() )
            {
                String cachedValue = jedis.get(key);
                Assert.assertNull(cachedValue);
            }
        }
        finally
        {
            if( jedis != null )
            {
                jedis.close();
            }
        }


    }

}