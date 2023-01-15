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

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.VarHandle;
public class sdrplay_api_DecimationT {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_CHAR$LAYOUT.withName("enable"),
        Constants$root.C_CHAR$LAYOUT.withName("decimationFactor"),
        Constants$root.C_CHAR$LAYOUT.withName("wideBandSignal")
    );
    public static MemoryLayout $LAYOUT() {
        return sdrplay_api_DecimationT.$struct$LAYOUT;
    }
    static final VarHandle enable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("enable"));
    public static VarHandle enable$VH() {
        return sdrplay_api_DecimationT.enable$VH;
    }
    public static byte enable$get(MemorySegment seg) {
        return (byte)sdrplay_api_DecimationT.enable$VH.get(seg);
    }
    public static void enable$set( MemorySegment seg, byte x) {
        sdrplay_api_DecimationT.enable$VH.set(seg, x);
    }
    public static byte enable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_DecimationT.enable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void enable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_DecimationT.enable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle decimationFactor$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("decimationFactor"));
    public static VarHandle decimationFactor$VH() {
        return sdrplay_api_DecimationT.decimationFactor$VH;
    }
    public static byte decimationFactor$get(MemorySegment seg) {
        return (byte)sdrplay_api_DecimationT.decimationFactor$VH.get(seg);
    }
    public static void decimationFactor$set( MemorySegment seg, byte x) {
        sdrplay_api_DecimationT.decimationFactor$VH.set(seg, x);
    }
    public static byte decimationFactor$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_DecimationT.decimationFactor$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void decimationFactor$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_DecimationT.decimationFactor$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle wideBandSignal$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("wideBandSignal"));
    public static VarHandle wideBandSignal$VH() {
        return sdrplay_api_DecimationT.wideBandSignal$VH;
    }
    public static byte wideBandSignal$get(MemorySegment seg) {
        return (byte)sdrplay_api_DecimationT.wideBandSignal$VH.get(seg);
    }
    public static void wideBandSignal$set( MemorySegment seg, byte x) {
        sdrplay_api_DecimationT.wideBandSignal$VH.set(seg, x);
    }
    public static byte wideBandSignal$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_DecimationT.wideBandSignal$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void wideBandSignal$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_DecimationT.wideBandSignal$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


