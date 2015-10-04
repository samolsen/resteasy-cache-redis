package com.samolsen.resteasy.cache.redis;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.plugins.cache.server.ServerCache;
import redis.clients.jedis.*;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Set;

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

    protected JedisPool _jedisPool;
    protected String _namespace;
    protected ObjectMapper _objectMapper;

    public RedisCache( JedisPool jedisPool )
    {
        this(jedisPool, null);
    }

    public RedisCache( JedisPool jedisPool,
                       String namespace )
    {
        this(jedisPool, namespace, null);
    }


    public RedisCache( JedisPool jedisPool,
                       String namespace,
                       ObjectMapper objectMapper )
    {
        this._jedisPool = jedisPool;
        this._namespace = namespace;
        this._objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        this._objectMapper.registerModule(new CacheSerializationModule());
    }

    @Override
    public Entry get( String uri,
                      MediaType accept )
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
            if( jedis != null )
            {
                jedis.close();
            }
        }
    }

    @Override
    public Entry add( String uri,
                      MediaType mediaType,
                      CacheControl cc,
                      MultivaluedMap<String, Object> headers,
                      byte[] entity,
                      String etag )
    {
        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();

            CacheEntry cacheEntry = new CacheEntry(headers, entity, cc.getMaxAge(), etag, mediaType);
            String entryName = toCacheKey(uri + KEY_DELIMITER + mediaType.toString());

            pipeline.sadd(toCacheKey(uri), entryName);
            pipeline.setex(entryName, cc.getMaxAge(), stringRepresentation(cacheEntry));
            pipeline.sync();
            return cacheEntry;
        }
        finally
        {
            if( jedis != null )
            {
                jedis.close();
            }
        }
    }

    @Override
    public void remove( String uri )
    {
        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();
            jedis.del(toCacheKey(uri));
        }
        finally
        {
            if( jedis != null )
            {
                jedis.close();
            }
        }
    }

    @Override
    public void clear()
    {
        Jedis jedis = null;
        try
        {
            jedis = _jedisPool.getResource();

            ScanParams scanParams = new ScanParams();
            if( _namespace != null )
            {
                scanParams.match(_namespace + KEY_DELIMITER + "*");
            }

            String cursor = "0";
            do
            {
                ScanResult<String> result = jedis.scan(cursor, scanParams);
                List<String> keys = result.getResult();
                jedis.del(keys.toArray(new String[keys.size()]));
                cursor = result.getStringCursor();
            }
            while( !cursor.equals("0") );
        }
        finally
        {
            if( jedis != null )
            {
                jedis.close();
            }
        }
    }

    private String toCacheKey( String key )
    {
        return _namespace == null ? key : _namespace + KEY_DELIMITER + key;
    }

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

    protected String stringRepresentation( Object o )
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

}