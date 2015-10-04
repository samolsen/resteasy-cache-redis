package com.samolsen.resteasy.cache.redis;


import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

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
class CacheSerializationModule extends SimpleModule {

    private static final String NAME = "CacheSerializationModule";
    private static final Version VERSION = new Version(1, 0, 0, null, null, null);

    CacheSerializationModule()
    {
        super(NAME, VERSION);

        addSerializer(CacheEntry.class, new CacheEntry.CacheEntrySerializer());
        addDeserializer(CacheEntry.class, new CacheEntry.CacheEntryDeserializer());

        addSerializer(MediaType.class, new MediaTypeSerializer());
        addDeserializer(MediaType.class, new MediaTypeDeserializer());

        addDeserializer(MultivaluedMap.class, new MultivaluedMapDeserializer());
    }

}
