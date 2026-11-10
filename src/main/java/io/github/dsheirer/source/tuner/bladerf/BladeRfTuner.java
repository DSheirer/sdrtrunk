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

package io.github.dsheirer.source.tuner.bladerf;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;

/**
 * bladeRF tuner
 */
public class BladeRfTuner extends Tuner
{
    private static final int BITS_PER_SAMPLE = 32;
    private static final double SAMPLE_SIZE_12_BIT = 12.0;
    private static final double SAMPLE_SIZE_8_BIT = 8.0;

    /**
     * Constructs an instance
     * @param controller for the bladeRF
     * @param tunerErrorListener to listen for errors from this tuner
     * @param channelizerType for the channelizer
     */
    public BladeRfTuner(BladeRfTunerController controller, ITunerErrorListener tunerErrorListener,
                        ChannelizerType channelizerType)
    {
        super(controller, tunerErrorListener, channelizerType);
    }

    /**
     * bladeRF tuner controller
     */
    public BladeRfTunerController getController()
    {
        return (BladeRfTunerController)getTunerController();
    }

    @Override
    public String getPreferredName()
    {
        return "bladeRF " + getUniqueID();
    }

    @Override
    public String getUniqueID()
    {
        return getController().getDeviceInfo().serial();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.BLADERF;
    }

    @Override
    public double getSampleSize()
    {
        BladeRfSampleRate rate = getController().getBladeRfSampleRate();
        return rate != null && rate.isOversampled() ? SAMPLE_SIZE_8_BIT : SAMPLE_SIZE_12_BIT;
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        return BladeRfSampleRate.RATE_61_44.getRate() * BITS_PER_SAMPLE;
    }
}
