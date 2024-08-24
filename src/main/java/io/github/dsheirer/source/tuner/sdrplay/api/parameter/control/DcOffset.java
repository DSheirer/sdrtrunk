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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.control;

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DcOffsetT;
import java.lang.foreign.MemorySegment;

/**
 * DC Offset structure (sdrplay_api_DCOffsetT)
 */
public class DcOffset
{
    private final MemorySegment mDCOffset;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public DcOffset(MemorySegment dCOffset)
    {
        mDCOffset = dCOffset;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getDCOffset()
    {
        return mDCOffset;
    }

    /**
     * Indicates if DC correction is enabled.
     */
    public boolean isDC()
    {
        return Flag.evaluate(sdrplay_api_DcOffsetT.DCenable(getDCOffset()));
    }

    /**
     * Enable or disable DC correction
     */
    public void setDC(boolean enable)
    {
        sdrplay_api_DcOffsetT.DCenable(getDCOffset(), Flag.of(enable));
    }

    /**
     * Indicates if IQ correction is enabled
     */
    public boolean isIQ()
    {
        return Flag.evaluate(sdrplay_api_DcOffsetT.IQenable(getDCOffset()));
    }

    /**
     * Enable or disable IQ correction
     */
    public void setIQ(boolean enable)
    {
        sdrplay_api_DcOffsetT.IQenable(getDCOffset(), Flag.of(enable));
    }
}
