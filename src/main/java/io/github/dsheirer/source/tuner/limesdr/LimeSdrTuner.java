/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.limesdr;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;

/**
 * LimeSDR tuner
 */
public class LimeSdrTuner extends Tuner
{
    //The USB link carries 12-bit I and Q samples, packed into 3 bytes
    private static final int LINK_BITS_PER_SAMPLE = 24;
    private static final double SAMPLE_SIZE_12_BIT = 12.0;

    /**
     * Constructs an instance
     * @param controller for the LimeSDR
     * @param tunerErrorListener to listen for errors from this tuner
     * @param channelizerType for the channelizer
     */
    public LimeSdrTuner(LimeSdrTunerController controller, ITunerErrorListener tunerErrorListener,
                        ChannelizerType channelizerType)
    {
        super(controller, tunerErrorListener, channelizerType);
    }

    /**
     * LimeSDR tuner controller
     */
    public LimeSdrTunerController getController()
    {
        return (LimeSdrTunerController)getTunerController();
    }

    @Override
    public String getPreferredName()
    {
        LimeSdrDeviceInformation info = getController().getDeviceInfo();
        return (info != null ? info.board() : "LimeSDR") + " " + getUniqueID();
    }

    @Override
    public String getUniqueID()
    {
        return getController().getDeviceInfo().serial();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.LIMESDR;
    }

    @Override
    public double getSampleSize()
    {
        return SAMPLE_SIZE_12_BIT;
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        return LimeSdrSampleRate.RATE_61_44.getRate() * LINK_BITS_PER_SAMPLE;
    }
}
