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

package io.github.dsheirer.source.tuner.sdrplay.api.v3_08;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

/**
 * {@snippet :
 * struct {
 *     unsigned char biasTEnable;
 *     sdrplay_api_RspDuo_AmPortSelectT tuner1AmPortSel;
 *     unsigned char tuner1AmNotchEnable;
 *     unsigned char rfNotchEnable;
 *     unsigned char rfDabNotchEnable;
 * };
 * }
 */
public class sdrplay_api_RspDuoTunerParamsT {

    static final StructLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_CHAR$LAYOUT.withName("biasTEnable"),
        MemoryLayout.paddingLayout(24),
        Constants$root.C_INT$LAYOUT.withName("tuner1AmPortSel"),
        Constants$root.C_CHAR$LAYOUT.withName("tuner1AmNotchEnable"),
        Constants$root.C_CHAR$LAYOUT.withName("rfNotchEnable"),
        Constants$root.C_CHAR$LAYOUT.withName("rfDabNotchEnable"),
        MemoryLayout.paddingLayout(8)
    );
    public static MemoryLayout $LAYOUT() {
        return sdrplay_api_RspDuoTunerParamsT.$struct$LAYOUT;
    }
    static final VarHandle biasTEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("biasTEnable"));
    public static VarHandle biasTEnable$VH() {
        return sdrplay_api_RspDuoTunerParamsT.biasTEnable$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char biasTEnable;
     * }
     */
    public static byte biasTEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.biasTEnable$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char biasTEnable;
     * }
     */
    public static void biasTEnable$set(MemorySegment seg, byte x) {
        sdrplay_api_RspDuoTunerParamsT.biasTEnable$VH.set(seg, x);
    }
    public static byte biasTEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.biasTEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void biasTEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDuoTunerParamsT.biasTEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle tuner1AmPortSel$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tuner1AmPortSel"));
    public static VarHandle tuner1AmPortSel$VH() {
        return sdrplay_api_RspDuoTunerParamsT.tuner1AmPortSel$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * sdrplay_api_RspDuo_AmPortSelectT tuner1AmPortSel;
     * }
     */
    public static int tuner1AmPortSel$get(MemorySegment seg) {
        return (int)sdrplay_api_RspDuoTunerParamsT.tuner1AmPortSel$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * sdrplay_api_RspDuo_AmPortSelectT tuner1AmPortSel;
     * }
     */
    public static void tuner1AmPortSel$set(MemorySegment seg, int x) {
        sdrplay_api_RspDuoTunerParamsT.tuner1AmPortSel$VH.set(seg, x);
    }
    public static int tuner1AmPortSel$get(MemorySegment seg, long index) {
        return (int)sdrplay_api_RspDuoTunerParamsT.tuner1AmPortSel$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void tuner1AmPortSel$set(MemorySegment seg, long index, int x) {
        sdrplay_api_RspDuoTunerParamsT.tuner1AmPortSel$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle tuner1AmNotchEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tuner1AmNotchEnable"));
    public static VarHandle tuner1AmNotchEnable$VH() {
        return sdrplay_api_RspDuoTunerParamsT.tuner1AmNotchEnable$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char tuner1AmNotchEnable;
     * }
     */
    public static byte tuner1AmNotchEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.tuner1AmNotchEnable$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char tuner1AmNotchEnable;
     * }
     */
    public static void tuner1AmNotchEnable$set(MemorySegment seg, byte x) {
        sdrplay_api_RspDuoTunerParamsT.tuner1AmNotchEnable$VH.set(seg, x);
    }
    public static byte tuner1AmNotchEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.tuner1AmNotchEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void tuner1AmNotchEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDuoTunerParamsT.tuner1AmNotchEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rfNotchEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rfNotchEnable"));
    public static VarHandle rfNotchEnable$VH() {
        return sdrplay_api_RspDuoTunerParamsT.rfNotchEnable$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char rfNotchEnable;
     * }
     */
    public static byte rfNotchEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.rfNotchEnable$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char rfNotchEnable;
     * }
     */
    public static void rfNotchEnable$set(MemorySegment seg, byte x) {
        sdrplay_api_RspDuoTunerParamsT.rfNotchEnable$VH.set(seg, x);
    }
    public static byte rfNotchEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.rfNotchEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rfNotchEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDuoTunerParamsT.rfNotchEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rfDabNotchEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rfDabNotchEnable"));
    public static VarHandle rfDabNotchEnable$VH() {
        return sdrplay_api_RspDuoTunerParamsT.rfDabNotchEnable$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char rfDabNotchEnable;
     * }
     */
    public static byte rfDabNotchEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.rfDabNotchEnable$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char rfDabNotchEnable;
     * }
     */
    public static void rfDabNotchEnable$set(MemorySegment seg, byte x) {
        sdrplay_api_RspDuoTunerParamsT.rfDabNotchEnable$VH.set(seg, x);
    }
    public static byte rfDabNotchEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDuoTunerParamsT.rfDabNotchEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rfDabNotchEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDuoTunerParamsT.rfDabNotchEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, SegmentScope scope) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, scope); }
}


