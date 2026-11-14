/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.service.radioreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.cache.LoadingCache;
import io.github.dsheirer.rrapi.RadioReferenceException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests the caching and error handling of the caching radio reference service, without calling the web service
 */
public class CachingRadioReferenceServiceTest
{
    @Test
    void failedCallIsThrownOnceAndNotCached()
    {
        AtomicInteger calls = new AtomicInteger();
        RadioReferenceException error = new RadioReferenceException(RadioReferenceException.ErrorType.TIMEOUT, "timeout");
        LoadingCache<Integer,String> cache = CachingRadioReferenceService.cache(10, id -> {
            calls.incrementAndGet();
            throw error;
        });

        RadioReferenceException thrown = assertThrows(RadioReferenceException.class,
            () -> CachingRadioReferenceService.get(cache, 1));
        assertSame(error, thrown, "the web service exception is thrown unwrapped");
        assertEquals(1, calls.get(), "a failed call isn't repeated");

        assertThrows(RadioReferenceException.class, () -> CachingRadioReferenceService.get(cache, 1));
        assertEquals(2, calls.get(), "a failed call isn't cached");
    }

    @Test
    void valueIsCached() throws RadioReferenceException
    {
        AtomicInteger calls = new AtomicInteger();
        LoadingCache<Integer,String> cache = CachingRadioReferenceService.cache(10, id -> {
            calls.incrementAndGet();
            return "value " + id;
        });

        assertEquals("value 1", CachingRadioReferenceService.get(cache, 1));
        assertEquals("value 1", CachingRadioReferenceService.get(cache, 1));
        assertEquals(1, calls.get());
    }

    @Test
    void nullValueIsReturnedAndNotCached() throws RadioReferenceException
    {
        AtomicInteger calls = new AtomicInteger();
        LoadingCache<Integer,String> cache = CachingRadioReferenceService.cache(10, id -> {
            calls.incrementAndGet();
            return null;
        });

        assertNull(CachingRadioReferenceService.get(cache, 1));
        assertEquals(1, calls.get(), "a null result isn't loaded a second time");
        assertNull(CachingRadioReferenceService.get(cache, 1));
        assertEquals(2, calls.get(), "a null result isn't cached");
    }

    @Test
    void runtimeExceptionIsThrownUnwrapped()
    {
        IllegalStateException error = new IllegalStateException("parse error");
        LoadingCache<Integer,String> cache = CachingRadioReferenceService.cache(10, id -> {
            throw error;
        });

        assertSame(error, assertThrows(IllegalStateException.class, () -> CachingRadioReferenceService.get(cache, 1)));
    }
}
