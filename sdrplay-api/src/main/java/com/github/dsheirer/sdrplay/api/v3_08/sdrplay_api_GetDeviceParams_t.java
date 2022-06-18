// Generated by jextract

package com.github.dsheirer.sdrplay.api.v3_08;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
public interface sdrplay_api_GetDeviceParams_t {

    int apply(java.lang.foreign.MemoryAddress dev, java.lang.foreign.MemoryAddress deviceParams);
    static MemorySegment allocate(sdrplay_api_GetDeviceParams_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(sdrplay_api_GetDeviceParams_t.class, fi, constants$4.sdrplay_api_GetDeviceParams_t$FUNC, session);
    }
    static sdrplay_api_GetDeviceParams_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _dev, java.lang.foreign.MemoryAddress _deviceParams) -> {
            try {
                return (int)constants$4.sdrplay_api_GetDeviceParams_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_dev, (java.lang.foreign.Addressable)_deviceParams);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


