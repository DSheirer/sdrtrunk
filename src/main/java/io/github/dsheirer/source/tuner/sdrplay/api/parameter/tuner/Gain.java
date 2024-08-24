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
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_GainT;
import java.lang.foreign.MemorySegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gain structure (sdrplay_api_GainT)
 */
public class Gain
{
    private static final Logger mLog = LoggerFactory.getLogger(Gain.class);

    private final MemorySegment mMemorySegment;
    private final GainValues mGainValues;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public Gain(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
        mGainValues = new GainValues(sdrplay_api_GainT.gainVals(memorySegment));
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * Current gain reduction
     * @return gain dB
     */
    public int getGainReductionDb()
    {
        return sdrplay_api_GainT.gRdB(getMemorySegment());
    }

    /**
     * Sets the gain reduction value
     * @param gainReductionDb
     */
    public void setGainReductionDb(int gainReductionDb)
    {
        sdrplay_api_GainT.gRdB(getMemorySegment(), gainReductionDb);
    }

    /**
     * Low Noise Amplifier (LNA) state/value
     */
    public int getLNA()
    {
        return sdrplay_api_GainT.LNAstate(getMemorySegment());
    }

    /**
     * Sets the Low Noise Amplifier (LNA) state
     */
    public void setLNA(int lna)
    {
        sdrplay_api_GainT.LNAstate(getMemorySegment(), (byte)lna);
    }

    /**
     * Specifies if LNA and gain reduction changes should be applied synchronously
     */
    public void setSynchronousUpdate(boolean syncUpdate)
    {
        sdrplay_api_GainT.syncUpdate(getMemorySegment(), Flag.of(syncUpdate));
    }

    /**
     * Minimum gain reduction mode
     */
    public MinimumGainReductionMode getMinimumGainReductionMode()
    {
        return MinimumGainReductionMode.fromValue(sdrplay_api_GainT.minGr(getMemorySegment()));
    }

    /**
     * Sets the minimum gain reduction mode
     */
    public void setMinimumGainReductionMode(MinimumGainReductionMode minimumGainReductionMode)
    {
        sdrplay_api_GainT.minGr(getMemorySegment(), minimumGainReductionMode.getValue());
    }

    /**
     * Gain values structure
     */
    public GainValues getGainValues()
    {
        return mGainValues;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Reduction:").append(getGainReductionDb()).append("dB");
        sb.append(" LNA:").append(getLNA());
        sb.append(" Min Gain Reduction Mode:").append(getMinimumGainReductionMode());
        sb.append(" Gain Values:").append(getGainValues());
        return sb.toString();
    }
}
