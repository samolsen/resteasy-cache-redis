package com.samolsen.resteasy.cache.redis;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.plugins.cache.server.ServerCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.*;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (c) 2015, Sam Olsen
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/**
 * ServerCache implementation backed by a JedisPool instance.
 *
 * Cribbed from {@link org.jboss.resteasy.plugins.cache.server.InfinispanCache}
 *
 * @see {https://github.com/xetorthio/jedis}
 */
public class RedisCache implements ServerCache {

    static final String KEY_DELIMITER = ":";

    @NotNull
    protected JedisPool _jedisPool;
    @Nullable
    protected String _namespace;
    @NotNull
    protected ObjectMapper _objectMapper;

    protected int _uriExpirationSeconds = (int) TimeUnit.DAYS.toSeconds(30);

    public RedisCache( JedisPool jedisPool )
    {
        this(jedisPool, null);
    }

    public RedisCache( @NotNull JedisPool jedisPool,
                       @Nullable String namespace )
    {
        this(jedisPool, namespace, null);
    }


    public RedisCache( @NotNull JedisPool jedisPool,
                       @Nullable String namespace,
                       @Nullable ObjectMapper objectMapper )
    {
        this._jedisPool = jedisPool;
        this._namespace = namespace;
        this._objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        this._objectMapper.registerModule(new CacheSerializationModule());
    }

    @Nullable
    @Override
    public Entry get( @NotNull String uri,
                      @NotNull MediaType accept )
    {
        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();
            Set<String> entries = jedis.smembers(toCacheKey(uri));
            if( entries == null )
            {
                return null;
            }

            for( String entry : entries )
            {
                String cacheEntryJSON = jedis.get(entry);
                if( cacheEntryJSON == null )
                {
                    continue;
                }

                CacheEntry cacheEntry = asObject(cacheEntryJSON, CacheEntry.class);
                if( cacheEntry == null )
                {
                    continue;
                }
                if( accept.isCompatible(cacheEntry.getMediaType()) )
                {
                    return cacheEntry;
                }
            }
            return null;
        }
        finally
        {
            close(jedis);
        }
    }

    @NotNull
    @Override
    public Entry add( @NotNull String uri,
                      @NotNull MediaType mediaType,
                      @NotNull CacheControl cc,
                      @NotNull MultivaluedMap<String, Object> headers,
                      @NotNull byte[] entity,
                      @Nullable String etag )
    {
        String entryName = toCacheKey(uri + KEY_DELIMITER + mediaType.toString());
        int entryMaxAge = cc.getMaxAge();
        CacheEntry cacheEntry = new CacheEntry(headers, entity, entryMaxAge, etag, mediaType);

        String uriCacheKey = toCacheKey(uri);
        int uriMaxAge = Math.max(_uriExpirationSeconds, entryMaxAge);

        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();
            pipeline.sadd(uriCacheKey, entryName);
            pipeline.expire(uriCacheKey, uriMaxAge);
            pipeline.setex(entryName, entryMaxAge, stringRepresentation(cacheEntry));
            pipeline.sync();
            return cacheEntry;
        }
        finally
        {
            close(jedis);
        }
    }

    @Override
    public void remove( @NotNull String uri )
    {
        deletePrefixedKeys(toCacheKey(uri));
    }

    @Override
    public void clear()
    {
        if( _namespace == null )
        {
            flushDB();
        }
        else
        {
            deletePrefixedKeys(_namespace);
        }
    }

    @NotNull
    private String toCacheKey( @NotNull String key )
    {
        return _namespace == null ? key : _namespace + KEY_DELIMITER + key;
    }

    @Nullable
    protected <T> T asObject( String json,
                              Class<T> clazz )
    {
        try
        {
            return _objectMapper.readValue(json, clazz);
        }
        catch( IOException e )
        {
            return null;
        }
    }

    @NotNull
    protected String stringRepresentation( @NotNull Object o )
    {
        try
        {
            return _objectMapper.writeValueAsString(o);
        }
        catch( JsonProcessingException e )
        {
            throw new IllegalStateException("Error serializing object", e);
        }
    }

    void deletePrefixedKeys( @NotNull String keyMatch )
    {
        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();

            ScanParams scanParams = new ScanParams()
                    .match(keyMatch + "*");

            String cursor = "0";
            do
            {
                ScanResult<String> result = jedis.scan(cursor, scanParams);
                List<String> keys = result.getResult();
                if( !keys.isEmpty() )
                {
                    jedis.del(keys.toArray(new String[keys.size()]));
                }
                cursor = result.getStringCursor();
            }
            while( !cursor.equals("0") );
        }
        finally
        {
            close(jedis);
        }
    }

    private void flushDB()
    {
        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();
            jedis.flushDB();
        }
        finally
        {
            close(jedis);
        }
    }

    private void close( @Nullable Jedis jedis )
    {
        if( jedis != null )
        {
            jedis.close();
        }
    }

}