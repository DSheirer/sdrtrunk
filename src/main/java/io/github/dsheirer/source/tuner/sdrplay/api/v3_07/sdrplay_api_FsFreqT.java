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
public class sdrplay_api_FsFreqT {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_DOUBLE$LAYOUT.withName("fsHz"),
        Constants$root.C_CHAR$LAYOUT.withName("syncUpdate"),
        Constants$root.C_CHAR$LAYOUT.withName("reCal"),
        MemoryLayout.paddingLayout(48)
    );
    public static MemoryLayout $LAYOUT() {
        return sdrplay_api_FsFreqT.$struct$LAYOUT;
    }
    static final VarHandle fsHz$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fsHz"));
    public static VarHandle fsHz$VH() {
        return sdrplay_api_FsFreqT.fsHz$VH;
    }
    public static double fsHz$get(MemorySegment seg) {
        return (double)sdrplay_api_FsFreqT.fsHz$VH.get(seg);
    }
    public static void fsHz$set( MemorySegment seg, double x) {
        sdrplay_api_FsFreqT.fsHz$VH.set(seg, x);
    }
    public static double fsHz$get(MemorySegment seg, long index) {
        return (double)sdrplay_api_FsFreqT.fsHz$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void fsHz$set(MemorySegment seg, long index, double x) {
        sdrplay_api_FsFreqT.fsHz$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle syncUpdate$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("syncUpdate"));
    public static VarHandle syncUpdate$VH() {
        return sdrplay_api_FsFreqT.syncUpdate$VH;
    }
    public static byte syncUpdate$get(MemorySegment seg) {
        return (byte)sdrplay_api_FsFreqT.syncUpdate$VH.get(seg);
    }
    public static void syncUpdate$set( MemorySegment seg, byte x) {
        sdrplay_api_FsFreqT.syncUpdate$VH.set(seg, x);
    }
    public static byte syncUpdate$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_FsFreqT.syncUpdate$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void syncUpdate$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_FsFreqT.syncUpdate$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle reCal$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("reCal"));
    public static VarHandle reCal$VH() {
        return sdrplay_api_FsFreqT.reCal$VH;
    }
    public static byte reCal$get(MemorySegment seg) {
        return (byte)sdrplay_api_FsFreqT.reCal$VH.get(seg);
    }
    public static void reCal$set( MemorySegment seg, byte x) {
        sdrplay_api_FsFreqT.reCal$VH.set(seg, x);
    }
    public static byte reCal$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_FsFreqT.reCal$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void reCal$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_FsFreqT.reCal$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


