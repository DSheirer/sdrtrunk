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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DcOffsetTunerT;
import java.lang.foreign.MemorySegment;

/**
 * DC Offset Tuner structure (sdrplay_api_DcOffsetTunerT)
 */
public class DcOffsetTuner
{
    private final MemorySegment mMemorySegment;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public DcOffsetTuner(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * DC calibration mode.
     * @return
     */
    public int getDcCal()
    {
        return sdrplay_api_DcOffsetTunerT.dcCal(getMemorySegment());
    }

    public void setDcCal(int dcCal)
    {
        sdrplay_api_DcOffsetTunerT.dcCal(getMemorySegment(), (byte)dcCal);
    }

    public boolean isSpeedUp()
    {
        return Flag.evaluate(sdrplay_api_DcOffsetTunerT.speedUp(getMemorySegment()));
    }

    public void setSpeedUp(boolean speedUp)
    {
        sdrplay_api_DcOffsetTunerT.speedUp(getMemorySegment(), Flag.of(speedUp));
    }

    public int getTrackTime()
    {
        return sdrplay_api_DcOffsetTunerT.trackTime(getMemorySegment());
    }

    public void setTrackTime(int trackTime)
    {
        sdrplay_api_DcOffsetTunerT.trackTime(getMemorySegment(), trackTime);
    }

    public int getRefreshRateTime()
    {
        return sdrplay_api_DcOffsetTunerT.refreshRateTime(getMemorySegment());
    }

    public void setRefreshRateTime(int refreshRateTime)
    {
        sdrplay_api_DcOffsetTunerT.refreshRateTime(getMemorySegment(), refreshRateTime);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DC Cal:").append(getDcCal()).append(" Speed Up:").append(isSpeedUp());
        sb.append(" Track Time:").append(getTrackTime()).append(" Refresh Rate Time:").append(getRefreshRateTime());
        return sb.toString();
    }
}
