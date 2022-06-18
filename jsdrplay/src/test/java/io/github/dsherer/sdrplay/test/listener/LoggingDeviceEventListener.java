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

package io.github.dsherer.sdrplay.test.listener;

import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.callback.IDeviceEventListener;
import com.github.dsheirer.sdrplay.device.Device;
import com.github.dsheirer.sdrplay.device.TunerSelect;
import com.github.dsheirer.sdrplay.parameter.event.EventType;
import com.github.dsheirer.sdrplay.parameter.event.GainCallbackParameters;
import com.github.dsheirer.sdrplay.parameter.event.PowerOverloadCallbackParameters;
import com.github.dsheirer.sdrplay.parameter.event.RspDuoModeCallbackParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging device event listener for debug or testing.
 */
public class LoggingDeviceEventListener implements IDeviceEventListener
{
    private static final Logger mLog = LoggerFactory.getLogger(LoggingDeviceEventListener.class);
    private String mLabel;
    private Device mDevice;

    /**
     * Constructs an instance
     * @param label prefix for logging.
     */
    public LoggingDeviceEventListener(String label, Device device)
    {
        mLabel = label;
        mDevice = device;
    }

    @Override
    public void processEvent(EventType eventType, TunerSelect tunerSelect)
    {
        mLog.info(mLabel + " - Unrecognized Event: " + eventType + " Tuner:" + tunerSelect);
    }

    @Override
    public void processGainChange(TunerSelect tunerSelect, GainCallbackParameters gainCallbackParameters)
    {
        mLog.info(mLabel + " - Gain Change - Tuner: " + tunerSelect + " " + gainCallbackParameters.toString());
    }

    @Override
    public void processPowerOverload(TunerSelect tunerSelect, PowerOverloadCallbackParameters parameters)
    {
        mLog.info(mLabel + " - Power Overload - Tuner: " + tunerSelect + " - acknowledging");

        try
        {
            mDevice.acknowledgePowerOverload(tunerSelect);
        }
        catch(SDRPlayException se)
        {
            mLog.error("Unable to acknowledge power overload for tuner(s): " + tunerSelect + ": " + se.getLocalizedMessage());
        }
    }

    @Override
    public void processRspDuoModeChange(TunerSelect tunerSelect, RspDuoModeCallbackParameters parameters)
    {
        mLog.info(mLabel + " - RSPduo Mode Change - Tuner: " + tunerSelect + " Event:" + parameters.getRspDuoModeEvent());
    }

    @Override
    public void processDeviceRemoval(TunerSelect tunerSelect)
    {
        mLog.info(mLabel + " - Device Removed");
    }
}
