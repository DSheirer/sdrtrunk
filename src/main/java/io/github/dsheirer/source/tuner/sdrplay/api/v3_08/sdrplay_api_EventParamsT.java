/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;

/**
 * {@snippet lang=c :
 * union {
 *     sdrplay_api_GainCbParamT gainParams;
 *     sdrplay_api_PowerOverloadCbParamT powerOverloadParams;
 *     sdrplay_api_RspDuoModeCbParamT rspDuoModeParams;
 * }
 * }
 */
public class sdrplay_api_EventParamsT {

    sdrplay_api_EventParamsT() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.unionLayout(
        sdrplay_api_GainCbParamT.layout().withName("gainParams"),
        sdrplay_api_PowerOverloadCbParamT.layout().withName("powerOverloadParams"),
        sdrplay_api_RspDuoModeCbParamT.layout().withName("rspDuoModeParams")
    ).withName("$anon$49:9");

    /**
     * The layout of this union
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final GroupLayout gainParams$LAYOUT = (GroupLayout)$LAYOUT.select(groupElement("gainParams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * sdrplay_api_GainCbParamT gainParams
     * }
     */
    public static final GroupLayout gainParams$layout() {
        return gainParams$LAYOUT;
    }

    private static final long gainParams$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * sdrplay_api_GainCbParamT gainParams
     * }
     */
    public static final long gainParams$offset() {
        return gainParams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * sdrplay_api_GainCbParamT gainParams
     * }
     */
    public static MemorySegment gainParams(MemorySegment union) {
        return union.asSlice(gainParams$OFFSET, gainParams$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * sdrplay_api_GainCbParamT gainParams
     * }
     */
    public static void gainParams(MemorySegment union, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, union, gainParams$OFFSET, gainParams$LAYOUT.byteSize());
    }

    private static final GroupLayout powerOverloadParams$LAYOUT = (GroupLayout)$LAYOUT.select(groupElement("powerOverloadParams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * sdrplay_api_PowerOverloadCbParamT powerOverloadParams
     * }
     */
    public static final GroupLayout powerOverloadParams$layout() {
        return powerOverloadParams$LAYOUT;
    }

    private static final long powerOverloadParams$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * sdrplay_api_PowerOverloadCbParamT powerOverloadParams
     * }
     */
    public static final long powerOverloadParams$offset() {
        return powerOverloadParams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * sdrplay_api_PowerOverloadCbParamT powerOverloadParams
     * }
     */
    public static MemorySegment powerOverloadParams(MemorySegment union) {
        return union.asSlice(powerOverloadParams$OFFSET, powerOverloadParams$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * sdrplay_api_PowerOverloadCbParamT powerOverloadParams
     * }
     */
    public static void powerOverloadParams(MemorySegment union, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, union, powerOverloadParams$OFFSET, powerOverloadParams$LAYOUT.byteSize());
    }

    private static final GroupLayout rspDuoModeParams$LAYOUT = (GroupLayout)$LAYOUT.select(groupElement("rspDuoModeParams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * sdrplay_api_RspDuoModeCbParamT rspDuoModeParams
     * }
     */
    public static final GroupLayout rspDuoModeParams$layout() {
        return rspDuoModeParams$LAYOUT;
    }

    private static final long rspDuoModeParams$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * sdrplay_api_RspDuoModeCbParamT rspDuoModeParams
     * }
     */
    public static final long rspDuoModeParams$offset() {
        return rspDuoModeParams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * sdrplay_api_RspDuoModeCbParamT rspDuoModeParams
     * }
     */
    public static MemorySegment rspDuoModeParams(MemorySegment union) {
        return union.asSlice(rspDuoModeParams$OFFSET, rspDuoModeParams$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * sdrplay_api_RspDuoModeCbParamT rspDuoModeParams
     * }
     */
    public static void rspDuoModeParams(MemorySegment union, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, union, rspDuoModeParams$OFFSET, rspDuoModeParams$LAYOUT.byteSize());
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this union
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}

