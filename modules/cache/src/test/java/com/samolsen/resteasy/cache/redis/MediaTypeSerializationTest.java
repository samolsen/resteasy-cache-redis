package com.samolsen.resteasy.cache.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
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
public class MediaTypeSerializationTest {

    static ObjectMapper OBJECT_MAPPER;

    @BeforeClass
    public static void beforeAll()
    {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new CacheSerializationModule());
    }

    @Test
    public void testMediaTypeSerialization()
            throws IOException
    {
        testMediaType(MediaType.APPLICATION_ATOM_XML_TYPE);
        testMediaType(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        testMediaType(MediaType.APPLICATION_JSON_TYPE);
        testMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        testMediaType(MediaType.APPLICATION_SVG_XML_TYPE);
        testMediaType(MediaType.APPLICATION_XHTML_XML_TYPE);
        testMediaType(MediaType.APPLICATION_XML_TYPE);
        testMediaType(MediaType.TEXT_HTML_TYPE);
        testMediaType(MediaType.TEXT_PLAIN_TYPE);
        testMediaType(MediaType.TEXT_XML_TYPE);
        testMediaType(MediaType.WILDCARD_TYPE);
    }

    private void testMediaType( MediaType mediaType )
            throws IOException
    {
        String asString = OBJECT_MAPPER.writeValueAsString(mediaType);
        MediaType deserialized = OBJECT_MAPPER.readValue(asString, MediaType.class);
        Assert.assertEquals("MediaType instances equal", mediaType, deserialized);
    }

}
