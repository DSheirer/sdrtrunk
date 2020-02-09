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
package io.github.dsheirer.source.tuner.rtl;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTL2832Tuner extends Tuner
{
    private final static Logger mLog = LoggerFactory.getLogger(RTL2832Tuner.class);

    private TunerClass mTunerClass;

    public RTL2832Tuner(TunerClass tunerClass, RTL2832TunerController controller, UserPreferences userPreferences)
        throws SourceException
    {
        super(tunerClass.getVendorDeviceLabel() + "/" + controller.getTunerType().getLabel() + " " +
            controller.getUniqueID(), controller, userPreferences);

        mTunerClass = tunerClass;
    }

    public RTL2832TunerController getController()
    {
        return (RTL2832TunerController)getTunerController();
    }

    @Override
    public String getUniqueID()
    {
        return getController().getUniqueID();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return mTunerClass;
    }

    @Override
    public TunerType getTunerType()
    {
        return getController().getTunerType();
    }

    @Override
    public double getSampleSize()
    {
        //Note: although sample size is 8, we set it to 11 to align with the
        //actual noise floor.
        return 11.0;
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        //16 bits per sample * 2.4 MSPS
        return 38400000;
    }
}