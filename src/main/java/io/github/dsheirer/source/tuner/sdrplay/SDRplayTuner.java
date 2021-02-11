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
package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SDRplayTuner extends Tuner
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRplayTuner.class);

    public SDRplayTuner(SDRplayTunerController controller, UserPreferences userPreferences) throws SourceException
    {
        super("SDRplay", controller, userPreferences);
        setName("SDRplay:"+controller.getModel());
    }

    public SDRplayTunerController getController()
    {
        return (SDRplayTunerController)getTunerController();
    }

    @Override
    public TunerClass getTunerClass()
    {
    	TunerClass retval = TunerClass.UNKNOWN;
    	switch(getController().getModel()) {
    	case ("RSP1"):
    				retval = TunerClass.SDRPLAY_RSP1;
    				break;
    	case ("RSP1A"):
    				retval = TunerClass.SDRPLAY_RSP1A;
    				break;
    	case ("RSPDX"):
    				retval = TunerClass.SDRPLAY_RSPDX;
    				break;
    	case ("RSP2"):
    				retval = TunerClass.SDRPLAY_RSP2;
    				break;
    	case ("RSPDUO"):
    				retval =  TunerClass.SDRPLAY_RSPDUO;
    				break;
    	}
    	return retval;
    }

    @Override
    public TunerType getTunerType()
    {
       	TunerType retval = TunerType.UNKNOWN;
    	switch(getController().getModel()) {
    	case ("RSP1"):
    				retval = TunerType.SDRPLAY_RSP1;
    				break;
    	case ("RSP1A"):
    				retval = TunerType.SDRPLAY_RSP1A;
    				break;
    	case ("RSPDX"):
    				retval = TunerType.SDRPLAY_RSPDX;
    				break;
    	case ("RSP2"):
    				retval = TunerType.SDRPLAY_RSP2;
    				break;
    	case ("RSPDUO"):
    				retval =  TunerType.SDRPLAY_RSPDUO;
    				break;
    	}
    	return retval;
    }

    @Override
    public double getSampleSize()
    {
        // TODO
        return 12.0;
    }

    @Override
    public String getUniqueID()
    {
        return getController().getSerial();
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        //12 bits per sample * 8.064 MSPS
        return 96768000;  // This is sort of a rough guess;
    }
}