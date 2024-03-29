/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

// Generated by jextract

package io.github.dsheirer.source.tuner.sdrplay.api.v3_07;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;

/**
 * {@snippet :
 * enum  (*sdrplay_api_ApiVersion_t)(float* apiVer);
 * }
 */
public interface sdrplay_api_ApiVersion_t {

    int apply(java.lang.foreign.MemorySegment currentSampleRate);
    static MemorySegment allocate(sdrplay_api_ApiVersion_t fi, SegmentScope scope) {
        return RuntimeHelper.upcallStub(constants$1.sdrplay_api_ApiVersion_t_UP$MH, fi, constants$1.sdrplay_api_ApiVersion_t$FUNC, scope);
    }
    static sdrplay_api_ApiVersion_t ofAddress(MemorySegment addr, SegmentScope scope) {
        MemorySegment symbol = MemorySegment.ofAddress(addr.address(), 0, scope);
        return (java.lang.foreign.MemorySegment _currentSampleRate) -> {
            try {
                return (int)constants$1.sdrplay_api_ApiVersion_t_DOWN$MH.invokeExact(symbol, _currentSampleRate);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


