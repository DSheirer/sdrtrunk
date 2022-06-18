// Generated by jextract

package com.github.dsheirer.sdrplay.api.v3_08;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.VarHandle;
public class sdrplay_api_RspDxParamsT {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_CHAR$LAYOUT.withName("hdrEnable"),
        Constants$root.C_CHAR$LAYOUT.withName("biasTEnable"),
        MemoryLayout.paddingLayout(16),
        Constants$root.C_LONG$LAYOUT.withName("antennaSel"),
        Constants$root.C_CHAR$LAYOUT.withName("rfNotchEnable"),
        Constants$root.C_CHAR$LAYOUT.withName("rfDabNotchEnable"),
        MemoryLayout.paddingLayout(16)
    );
    public static MemoryLayout $LAYOUT() {
        return sdrplay_api_RspDxParamsT.$struct$LAYOUT;
    }
    static final VarHandle hdrEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("hdrEnable"));
    public static VarHandle hdrEnable$VH() {
        return sdrplay_api_RspDxParamsT.hdrEnable$VH;
    }
    public static byte hdrEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDxParamsT.hdrEnable$VH.get(seg);
    }
    public static void hdrEnable$set( MemorySegment seg, byte x) {
        sdrplay_api_RspDxParamsT.hdrEnable$VH.set(seg, x);
    }
    public static byte hdrEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDxParamsT.hdrEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void hdrEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDxParamsT.hdrEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle biasTEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("biasTEnable"));
    public static VarHandle biasTEnable$VH() {
        return sdrplay_api_RspDxParamsT.biasTEnable$VH;
    }
    public static byte biasTEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDxParamsT.biasTEnable$VH.get(seg);
    }
    public static void biasTEnable$set( MemorySegment seg, byte x) {
        sdrplay_api_RspDxParamsT.biasTEnable$VH.set(seg, x);
    }
    public static byte biasTEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDxParamsT.biasTEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void biasTEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDxParamsT.biasTEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle antennaSel$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("antennaSel"));
    public static VarHandle antennaSel$VH() {
        return sdrplay_api_RspDxParamsT.antennaSel$VH;
    }
    public static int antennaSel$get(MemorySegment seg) {
        return (int)sdrplay_api_RspDxParamsT.antennaSel$VH.get(seg);
    }
    public static void antennaSel$set( MemorySegment seg, int x) {
        sdrplay_api_RspDxParamsT.antennaSel$VH.set(seg, x);
    }
    public static int antennaSel$get(MemorySegment seg, long index) {
        return (int)sdrplay_api_RspDxParamsT.antennaSel$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void antennaSel$set(MemorySegment seg, long index, int x) {
        sdrplay_api_RspDxParamsT.antennaSel$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rfNotchEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rfNotchEnable"));
    public static VarHandle rfNotchEnable$VH() {
        return sdrplay_api_RspDxParamsT.rfNotchEnable$VH;
    }
    public static byte rfNotchEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDxParamsT.rfNotchEnable$VH.get(seg);
    }
    public static void rfNotchEnable$set( MemorySegment seg, byte x) {
        sdrplay_api_RspDxParamsT.rfNotchEnable$VH.set(seg, x);
    }
    public static byte rfNotchEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDxParamsT.rfNotchEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rfNotchEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDxParamsT.rfNotchEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle rfDabNotchEnable$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("rfDabNotchEnable"));
    public static VarHandle rfDabNotchEnable$VH() {
        return sdrplay_api_RspDxParamsT.rfDabNotchEnable$VH;
    }
    public static byte rfDabNotchEnable$get(MemorySegment seg) {
        return (byte)sdrplay_api_RspDxParamsT.rfDabNotchEnable$VH.get(seg);
    }
    public static void rfDabNotchEnable$set( MemorySegment seg, byte x) {
        sdrplay_api_RspDxParamsT.rfDabNotchEnable$VH.set(seg, x);
    }
    public static byte rfDabNotchEnable$get(MemorySegment seg, long index) {
        return (byte)sdrplay_api_RspDxParamsT.rfDabNotchEnable$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void rfDabNotchEnable$set(MemorySegment seg, long index, byte x) {
        sdrplay_api_RspDxParamsT.rfDabNotchEnable$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


