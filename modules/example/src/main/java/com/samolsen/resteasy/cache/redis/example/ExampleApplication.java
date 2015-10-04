package com.samolsen.resteasy.cache.redis.example;

import com.samolsen.resteasy.cache.redis.RedisCache;
import io.swagger.jaxrs.config.BeanConfig;
import org.jboss.resteasy.plugins.cache.server.ServerCacheFeature;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

@ApplicationPath("api")
public class ExampleApplication extends Application {

    private Set<Object> _singletons = new HashSet<Object>();
    private Set<Class<?>> _classes = new HashSet<Class<?>>();

    @PostConstruct
    void init()
    {
        _singletons.add(createServerCacheFeature());

        _classes.add(ResponseResource.class);
        _classes.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        _classes.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.0");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setHost("localhost:8080");
        beanConfig.setBasePath("/cache-example/api");
        beanConfig.setResourcePackage("com.samolsen.resteasy.cache.redis.example");
        beanConfig.setScan(true);
    }

    @Override
    public Set<Object> getSingletons()
    {
        return _singletons;
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        return _classes;
    }

    private ServerCacheFeature createServerCacheFeature()
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

        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), host, port, timeout, password);
        RedisCache redisCache = new RedisCache(jedisPool, "cacheExample");
        return new ServerCacheFeature(redisCache);
    }

}
