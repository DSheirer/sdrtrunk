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
package io.github.dsheirer.source.tuner.airspy.hf;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Airspy HF+Tuner
 */
public class AirspyHfTuner extends Tuner
{
    private final static Logger mLog = LoggerFactory.getLogger(AirspyHfTuner.class);

    /**
     * Constructs an instance
     * @param controller for the Airspy HF
     * @param tunerErrorListener to listen for errors from this tuner
     * @param channelizerType for the channelizer
     */
    public AirspyHfTuner(AirspyHfTunerController controller, ITunerErrorListener tunerErrorListener,
                         ChannelizerType channelizerType)
    {
        super(controller, tunerErrorListener, channelizerType);
    }

    @Override
    public String getPreferredName()
    {
//        return "Airspy HF+ " + getController().getDeviceInfo().getSerialNumber();
        return "Airspy HF+";
    }

    /**
     * Airspy tuner controller
     */
    public AirspyHfTunerController getController()
    {
        return (AirspyHfTunerController)getTunerController();
    }

    @Override
    public String getUniqueID()
    {
        return "Unique ID";
//        try
//        {
//            return getController().getDeviceInfo().getSerialNumber();
//        }
//        catch(Exception e)
//        {
//            mLog.error("error getting serial number", e);
//        }
//
//        return BoardID.AIRSPY.getLabel();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.AIRSPY_HF;
    }

    @Override
    public double getSampleSize()
    {
        return 18.0;
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        //4-bytes per sample = 32 bits times 912 kSps = 29,184,000 bits per second
        return 29_184_000;
    }
}
