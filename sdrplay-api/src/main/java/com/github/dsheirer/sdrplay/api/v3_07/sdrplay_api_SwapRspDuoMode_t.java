// Generated by jextract

package com.github.dsheirer.sdrplay.api.v3_07;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
public interface sdrplay_api_SwapRspDuoMode_t {

    int apply(java.lang.foreign.MemoryAddress currDevice, java.lang.foreign.MemoryAddress deviceParams, int rspDuoMode, double sampleRate, int tuner, int bwType, int ifType, int tuner1AmPortSel);
    static MemorySegment allocate(sdrplay_api_SwapRspDuoMode_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(sdrplay_api_SwapRspDuoMode_t.class, fi, constants$6.sdrplay_api_SwapRspDuoMode_t$FUNC, session);
    }
    static sdrplay_api_SwapRspDuoMode_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _currDevice, java.lang.foreign.MemoryAddress _deviceParams, int _rspDuoMode, double _sampleRate, int _tuner, int _bwType, int _ifType, int _tuner1AmPortSel) -> {
            try {
                return (int)constants$6.sdrplay_api_SwapRspDuoMode_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_currDevice, (java.lang.foreign.Addressable)_deviceParams, _rspDuoMode, _sampleRate, _tuner, _bwType, _ifType, _tuner1AmPortSel);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


