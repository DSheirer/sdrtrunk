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

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

/**
 * {@snippet :
 * struct {
 *     unsigned char rfNotchEnable;
 *     unsigned char rfDabNotchEnable;
 * };
 * }
 */
public class sdrplay_api_Rsp1aParamsT {

    static final StructLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_CHAR$LAYOUT.withName("rfNotchEnable"),
        Constants$root.C_CHAR$LAYOUT.withName("rfDabNotchEnable")
    );
    public static MemoryLayout $LAYOUT() {
        return sdrplay_api_Rsp1aParamsT.$struct$LAYOUT;
    }
    static final VarHandle rfNotchEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rfNotchEnable"));
    public static VarHandle rfNotchEnable$VH() {
        return sdrplay_api_Rsp1aParamsT.rfNotchEnable$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char rfNotchEnable;
     * }
     */
    public static byte rfNotchEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_Rsp1aParamsT.rfNotchEnable$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char rfNotchEnable;
     * }
     */
    public static void rfNotchEnable$set(MemorySegment seg, byte x) {
        sdrplay_api_Rsp1aParamsT.rfNotchEnable$VH.set(seg, x);
    }
    public static byte rfNotchEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_Rsp1aParamsT.rfNotchEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rfNotchEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_Rsp1aParamsT.rfNotchEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rfDabNotchEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rfDabNotchEnable"));
    public static VarHandle rfDabNotchEnable$VH() {
        return sdrplay_api_Rsp1aParamsT.rfDabNotchEnable$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char rfDabNotchEnable;
     * }
     */
    public static byte rfDabNotchEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_Rsp1aParamsT.rfDabNotchEnable$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char rfDabNotchEnable;
     * }
     */
    public static void rfDabNotchEnable$set(MemorySegment seg, byte x) {
        sdrplay_api_Rsp1aParamsT.rfDabNotchEnable$VH.set(seg, x);
    }
    public static byte rfDabNotchEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_Rsp1aParamsT.rfDabNotchEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rfDabNotchEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_Rsp1aParamsT.rfDabNotchEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, SegmentScope scope) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, scope); }
}


