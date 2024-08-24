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

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_TunerParamsT;
import java.lang.foreign.MemorySegment;

/**
 * Tuner Parameters structure (sdrplay_api_TunerParamsT)
 */
public abstract class TunerParameters
{
    private final MemorySegment mTunerParams;
    private final RfFrequency mRfFrequency;
    private final Gain mGain;
    private final DcOffsetTuner mDcOffsetTuner;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public TunerParameters(MemorySegment tunerParams)
    {
        mTunerParams = tunerParams;
        mRfFrequency = new RfFrequency(sdrplay_api_TunerParamsT.rfFreq(tunerParams));
        mGain = new Gain(sdrplay_api_TunerParamsT.gain(tunerParams));
        mDcOffsetTuner = new DcOffsetTuner(sdrplay_api_TunerParamsT.dcOffsetTuner(tunerParams));
    }

    /**
     * Foreign memory segment for this tuner parameters structure
     */
    private MemorySegment getTunerParams()
    {
        return mTunerParams;
    }

    /**
     * IF bandwidth
     */
    public Bandwidth getBandwidth()
    {
        return Bandwidth.fromValue(sdrplay_api_TunerParamsT.bwType(getTunerParams()));
    }

    /**
     * Sets the IF bandwidth
     */
    public void setBandwidth(Bandwidth bandwidth)
    {
        sdrplay_api_TunerParamsT.bwType(getTunerParams(), bandwidth.getValue());
    }

    /**
     * IF mode
     */
    public IfMode getIfMode()
    {
        return IfMode.fromValue(sdrplay_api_TunerParamsT.ifType(getTunerParams()));
    }

    /**
     * Sets IF mode
     */
    public void setIfMode(IfMode ifMode)
    {
        sdrplay_api_TunerParamsT.ifType(getTunerParams(), ifMode.getValue());
    }

    /**
     * LO mode
     */
    public LoMode getLoMode()
    {
        return LoMode.fromValue(sdrplay_api_TunerParamsT.loMode(getTunerParams()));
    }

    /**
     * Sets LO mode
     */
    public void setLoMode(LoMode loMode)
    {
        sdrplay_api_TunerParamsT.loMode(getTunerParams(), loMode.getValue());
    }

    /**
     * Gain settings structure
     */
    public Gain getGain()
    {
        return mGain;
    }

    /**
     * RF frequency structure
     */
    public RfFrequency getRfFrequency()
    {
        return mRfFrequency;
    }

    /**
     * DC offset tuner structure
     */
    public DcOffsetTuner getDcOffsetTuner()
    {
        return mDcOffsetTuner;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuner Parameters\n");
        sb.append("\tBandwidth: ").append(getBandwidth()).append("\n");
        sb.append("\tIF Mode: ").append(getIfMode()).append("\n");
        sb.append("\tLO Mode: ").append(getLoMode()).append("\n");
        sb.append("\tGain: ").append(getGain()).append("\n");
        sb.append("\tRF Frequency: ").append(getRfFrequency()).append("\n");
        sb.append("\tDC Offset Tuner: ").append(getDcOffsetTuner()).append("\n");
        return sb.toString();
    }
}
