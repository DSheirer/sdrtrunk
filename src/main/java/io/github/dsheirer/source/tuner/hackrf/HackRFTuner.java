/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.hackrf;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.BoardID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HackRF Tuner
 */
public class HackRFTuner extends Tuner
{
    private final static Logger mLog = LoggerFactory.getLogger(HackRFTuner.class);

    /**
     * Constructs an instance
     * @param controller for the HackRF
     * @param tunerErrorListener to listen for errors from this tuner
     * @param channelizerType for the channelizer
     */
    public HackRFTuner(HackRFTunerController controller, ITunerErrorListener tunerErrorListener,
                       ChannelizerType channelizerType)
    {
        super(controller, tunerErrorListener, channelizerType);
    }

    @Override
    public String getPreferredName()
    {
        try
        {
            HackRFTunerController.Serial serial = getController().getSerial();
            return "HackRF " + getController().getTunerType() + " " + serial.getSerialNumber();
        }
        catch(Exception e)
        {
            //No - op
        }

        return "HackRF " + getController().getTunerType();
    }

    public HackRFTunerController getController()
    {
        return (HackRFTunerController)getTunerController();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.HACKRF;
    }

    @Override
    public double getSampleSize()
    {
        return 11.0;
    }

    @Override
    public String getUniqueID()
    {
        try
        {
            return getTunerClass().toString() + " " + getController().getSerial().getSerialNumber();
        }
        catch(Exception e)
        {
            mLog.error("error gettting serial number", e);
        }

        return BoardID.HACKRF_ONE.getLabel();
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        //16 bits per sample * 20 MSPS
        return 320000000;
    }
}