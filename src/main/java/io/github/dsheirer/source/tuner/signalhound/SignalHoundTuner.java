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

package io.github.dsheirer.source.tuner.signalhound;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;

/**
 * Signal Hound tuner: BB60, SM200/SM435 or SP145
 */
public class SignalHoundTuner extends Tuner
{
    //The BB60 and SM200 have 14-bit ADCs.  The samples are floats after the API's corrections and filtering.
    private static final double SAMPLE_SIZE_14_BIT = 14.0;
    //The API transfers 16-bit I and Q samples from the device (32 bits per complex sample)
    private static final int LINK_BITS_PER_SAMPLE = 32;

    /**
     * Constructs an instance
     * @param controller for the Signal Hound device
     * @param tunerErrorListener to listen for errors from this tuner
     * @param channelizerType for the channelizer
     */
    public SignalHoundTuner(SignalHoundTunerController controller, ITunerErrorListener tunerErrorListener,
                            ChannelizerType channelizerType)
    {
        super(controller, tunerErrorListener, channelizerType);
    }

    /**
     * Signal Hound tuner controller
     */
    public SignalHoundTunerController getController()
    {
        return (SignalHoundTunerController)getTunerController();
    }

    @Override
    public String getPreferredName()
    {
        SignalHoundDeviceInformation info = getController().getDeviceInfo();
        String model = info != null ? info.model() : getController().getFamily().toString();
        return "Signal Hound " + model + " " + getUniqueID();
    }

    @Override
    public String getUniqueID()
    {
        return Integer.toString(getController().getDescriptor().serialNumber());
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.SIGNAL_HOUND;
    }

    @Override
    public double getSampleSize()
    {
        return SAMPLE_SIZE_14_BIT;
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        return getController().getFamily().getBaseSampleRate() * LINK_BITS_PER_SAMPLE;
    }
}
