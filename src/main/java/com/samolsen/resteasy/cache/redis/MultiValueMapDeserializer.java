package com.samolsen.resteasy.cache.redis;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
class MultiValueMapDeserializer extends JsonDeserializer<MultivaluedMap> {

    @Override
    public MultivaluedMap deserialize( JsonParser jsonParser,
                                       DeserializationContext deserializationContext )
            throws IOException
    {

        MultivaluedMap<String, Object> map = new MultivaluedHashMap<String, Object>();


        JsonToken token = jsonParser.getCurrentToken();

        if( token != JsonToken.START_OBJECT )
        {
            throw deserializationContext.mappingException(MultivaluedMap.class);
        }

        while( ( token = jsonParser.nextToken() ) != JsonToken.END_OBJECT )
        {
            if( token == JsonToken.START_ARRAY )
            {
                String key = jsonParser.getCurrentName();
                List<Object> value = parseList(jsonParser);
                map.put(key, value);
            }
        }

        return map;
    }

    private List<Object> parseList( JsonParser jsonParser )
            throws IOException
    {
        List<Object> list = new LinkedList<Object>();

        JsonToken token;
        while( ( token = jsonParser.nextToken() ) != JsonToken.END_ARRAY )
        {
            if( token == JsonToken.START_ARRAY )
            {
                list.add(parseList(jsonParser));
            }
            else if( token == JsonToken.START_OBJECT )
            {
                list.add(parseMap(jsonParser));
            }
            else if( token == JsonToken.VALUE_STRING )
            {
                list.add(jsonParser.getValueAsString());
            }
            else if( token == JsonToken.VALUE_NUMBER_INT )
            {
                list.add(jsonParser.getLongValue());
            }
            else if( token == JsonToken.VALUE_NUMBER_FLOAT )
            {
                list.add(jsonParser.getDoubleValue());
            }
            else if( token == JsonToken.VALUE_TRUE )
            {
                list.add(true);
            }
            else if( token == JsonToken.VALUE_FALSE )
            {
                list.add(false);
            }
            else if( token == JsonToken.VALUE_NULL )
            {
                list.add(null);
            }
        }

        return list;
    }

    private Map<String, Object> parseMap( JsonParser jsonParser )
            throws IOException
    {
        Map<String, Object> map = new HashMap<String, Object>();

        JsonToken token;
        while( ( token = jsonParser.nextToken() ) != JsonToken.END_OBJECT )
        {
            String key = jsonParser.getCurrentName();

            if( token == JsonToken.START_OBJECT )
            {
                Map<String, Object> value = parseMap(jsonParser);
                map.put(key, value);
            }
            else if( token == JsonToken.START_ARRAY )
            {
                List<Object> value = parseList(jsonParser);
                map.put(key, value);
            }
            else if( token == JsonToken.VALUE_STRING )
            {
                map.put(key, jsonParser.getValueAsString());
            }
            else if( token == JsonToken.VALUE_NUMBER_INT )
            {
                map.put(key, jsonParser.getLongValue());
            }
            else if( token == JsonToken.VALUE_NUMBER_FLOAT )
            {
                map.put(key, jsonParser.getDoubleValue());
            }
            else if( token == JsonToken.VALUE_TRUE )
            {
                map.put(key, true);
            }
            else if( token == JsonToken.VALUE_FALSE )
            {
                map.put(key, false);
            }
            else if( token == JsonToken.VALUE_NULL )
            {
                map.put(key, null);
            }
        }

        return map;
    }
}
