package com.samolsen.resteasy.cache.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
public class MultivaluedMapDeserializerTest {


    static ObjectMapper OBJECT_MAPPER;
    MultivaluedMap<String, Object> _map;

    @BeforeClass
    public static void beforeAll()
    {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new CacheSerializationModule());
    }

    @Before
    public void beforeEach()
    {
        _map = new MultivaluedHashMap<String, Object>();
    }

    @Test
    public void testStringValue()
            throws IOException
    {
        _map.add("foo", "bar");
        testMultivalueMap(_map);
    }

    @Test
    public void testIntValue()
            throws IOException
    {
        _map.add("foo", 2L);
        testMultivalueMap(_map);
    }

    @Test
    public void testFloatValue()
            throws IOException
    {
        _map.add("foo", 4.4);
        testMultivalueMap(_map);
    }

    @Test
    public void testListValue()
            throws IOException
    {
        List<Object> list = new ArrayList<Object>();
        list.add("bar");
        list.add(2L);
        _map.add("foo", list);
        testMultivalueMap(_map);
    }

    @Test
    public void testMapValue()
            throws IOException
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        _map.add("foo", map);
        testMultivalueMap(_map);
    }

    @Test
    public void testMultiKeys()
            throws IOException
    {
        _map.addAll("foo", "baz");
        _map.addAll("bar", 5L);
        testMultivalueMap(_map);
    }

    @Test
    public void testMultiValues()
            throws IOException
    {
        List<String> list = new ArrayList<String>();
        list.add("hello world");

        HashMap<String, Long> map = new HashMap<String, Long>();
        map.put("abc", 123L);

        _map.addAll("foo", "bar", "baz", list, map);
        testMultivalueMap(_map);
    }


    private void testMultivalueMap( MultivaluedMap<String, Object> map )
            throws IOException
    {
        String asString = OBJECT_MAPPER.writeValueAsString(map);
        MultivaluedMap deserialized = OBJECT_MAPPER.readValue(asString, MultivaluedMap.class);
        Assert.assertEquals("MultivaluedMap instances equal", map, deserialized);
    }

}
