package com.samolsen.resteasy.cache.redis;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jboss.resteasy.plugins.cache.server.ServerCache;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

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
class CacheEntry implements ServerCache.Entry {

    byte[] _cached;
    int _expires;
    long _timestamp;
    MultivaluedMap<String, Object> _headers;
    String _etag;
    MediaType _mediaType;

    CacheEntry( MultivaluedMap<String, Object> headers,
                byte[] cached,
                int expires,
                String etag,
                MediaType mediaType )
    {
        _headers = headers;
        _cached = cached;
        _expires = expires;
        _etag = etag;
        _mediaType = mediaType;
        _timestamp = System.currentTimeMillis();
    }

    private CacheEntry( MultivaluedMap<String, Object> headers,
                        byte[] cached,
                        int expires,
                        String etag,
                        MediaType mediaType,
                        long timestamp )
    {
        _headers = headers;
        _cached = cached;
        _expires = expires;
        _timestamp = timestamp;
        _etag = etag;
        _mediaType = mediaType;
    }


    @Override
    public int getExpirationInSeconds()
    {
        return _expires - (int) ( ( System.currentTimeMillis() - _timestamp ) / 1000 );
    }

    @Override
    public boolean isExpired()
    {
        return System.currentTimeMillis() - _timestamp >= _expires * 1000;
    }

    @Override
    public String getEtag()
    {
        return _etag;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders()
    {
        return _headers;
    }

    @Override
    public byte[] getCached()
    {
        return _cached;
    }

    public MediaType getMediaType()
    {
        return _mediaType;
    }

    public static class CacheEntrySerializer extends JsonSerializer<CacheEntry> {
        @Override
        public void serialize( CacheEntry cacheEntry,
                               JsonGenerator jsonGenerator,
                               SerializerProvider serializerProvider )
                throws IOException
        {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeBinaryField("cached", cacheEntry._cached);
            jsonGenerator.writeNumberField("expires", cacheEntry._expires);
            jsonGenerator.writeNumberField("timestamp", cacheEntry._timestamp);
            jsonGenerator.writeObjectField("headers", cacheEntry._headers);
            jsonGenerator.writeStringField("etag", cacheEntry._etag);
            jsonGenerator.writeObjectField("mediaType", cacheEntry._mediaType);
            jsonGenerator.writeEndObject();
        }
    }

    public static class CacheEntryDeserializer extends JsonDeserializer<CacheEntry> {

        @Override
        public CacheEntry deserialize( JsonParser jsonParser,
                                       DeserializationContext deserializationContext )
                throws IOException
        {

            byte[] cached = null;
            Integer expires = null;
            Long timestamp = null;
            MultivaluedMap headers = null;
            String etag = null;
            MediaType mediaType = null;

            JsonToken token = jsonParser.getCurrentToken();

            if( token != JsonToken.START_OBJECT )
            {
                throw deserializationContext.mappingException(CacheEntry.class);
            }

            while( ( token = jsonParser.nextToken() ) != JsonToken.END_OBJECT )
            {
                String name = jsonParser.getCurrentName();

                if( "cached".equals(name) && token == JsonToken.VALUE_STRING )
                {
                    cached = jsonParser.getBinaryValue();
                }
                else if( "expires".equals(name) && token == JsonToken.VALUE_NUMBER_INT )
                {
                    expires = jsonParser.getIntValue();
                }
                else if( "timestamp".equals(name) && token == JsonToken.VALUE_NUMBER_INT )
                {
                    timestamp = jsonParser.getLongValue();
                }
                else if( "etag".equals(name) && token == JsonToken.VALUE_STRING )
                {
                    etag = jsonParser.getValueAsString();
                }
                else if( "mediaType".equals(name) && token == JsonToken.VALUE_STRING )
                {
                    mediaType = jsonParser.readValueAs(MediaType.class);
                }
                else if( "headers".equals(name) && token == JsonToken.START_OBJECT )
                {
                    headers = jsonParser.readValueAs(MultivaluedMap.class);
                }
            }

            if( headers == null )
            {
                throw new JsonParseException(
                        "Error parsing " + CacheEntry.class.getName() + ": headers is null",
                        jsonParser.getCurrentLocation());
            }
            if( cached == null )
            {
                throw new JsonParseException(
                        "Error parsing " + CacheEntry.class.getName() + ": cached is null",
                        jsonParser.getCurrentLocation());
            }
            if( expires == null )
            {
                throw new JsonParseException(
                        "Error parsing " + CacheEntry.class.getName() + ": expires is null",
                        jsonParser.getCurrentLocation());
            }
            if( mediaType == null )
            {
                throw new JsonParseException(
                        "Error parsing " + CacheEntry.class.getName() + ": mediaType is null",
                        jsonParser.getCurrentLocation());
            }
            if( timestamp == null )
            {
                throw new JsonParseException("Error parsing " + CacheEntry.class.getName() + ": timestamp is null",
                        jsonParser.getCurrentLocation());
            }

            // noinspection unchecked
            return new CacheEntry(headers, cached, expires, etag, mediaType, timestamp);
        }
    }
}
