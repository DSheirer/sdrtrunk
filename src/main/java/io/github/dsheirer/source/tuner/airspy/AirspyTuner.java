/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.source.tuner.airspy;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController.BoardID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirspyTuner extends Tuner
{
    private final static Logger mLog = LoggerFactory.getLogger(AirspyTuner.class);

    public AirspyTuner(AirspyTunerController controller, UserPreferences userPreferences)
    {
        super("Airspy " + controller.getDeviceInfo().getSerialNumber(), controller, userPreferences);
    }

    public AirspyTunerController getController()
    {
        return (AirspyTunerController)getTunerController();
    }

    @Override
    public String getUniqueID()
    {
        try
        {
            return getController().getDeviceInfo().getSerialNumber();
        }
        catch(Exception e)
        {
            mLog.error("error getting serial number", e);
        }

        return BoardID.AIRSPY.getLabel();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.AIRSPY;
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.AIRSPY_R820T;
    }

    @Override
    public double getSampleSize()
    {
        return 13.0;
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        //4-bytes per sample = 32 bits times 10 MSps = 320,000,000 bits per second
        return 320000000;
    }
}
