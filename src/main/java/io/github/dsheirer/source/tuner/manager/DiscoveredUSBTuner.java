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

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A USB tuner that is discovered during the USB device traversal process that has a VID and PID that match a known
 * USB tuner type that is currently supported by the software.
 */
public class DiscoveredUSBTuner extends DiscoveredTuner
{
    private Logger mLog = LoggerFactory.getLogger(DiscoveredUSBTuner.class);
    private TunerClass mTunerClass;
    private int mBus;
    private String mPortAddress;
    private ChannelizerType mChannelizerType;

    /**
     * Constructs an instance
     * @param bus (USB) number
     * @param portAddress (USB)
     * @param channelizerType to use with the tuner
     */
    public DiscoveredUSBTuner(TunerClass tunerClass, int bus, String portAddress, ChannelizerType channelizerType)
    {
        mTunerClass = tunerClass;
        mBus = bus;
        mPortAddress = portAddress;
        mChannelizerType = channelizerType;
    }

    /**
     * Indicates if this USB tuner is plugged into the specified bus and port.
     * @param bus number
     * @param port address
     * @return true if there is a match
     */
    public boolean isAt(int bus, String port)
    {
        return mBus == bus && port != null && port.equalsIgnoreCase(mPortAddress);
    }

    /**
     * Tuner class as determined from the USB VID and PID values
     */
    public TunerClass getTunerClass()
    {
        return mTunerClass;
    }

    /**
     * USB bus number
     */
    public int getBus()
    {
        return mBus;
    }

    /**
     * USB port number
     */
    public String getPortAddress()
    {
        return mPortAddress;
    }

    @Override
    public String getId()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getTunerClass());
        sb.append(" USB Bus:").append(getBus());
        sb.append(" Port:").append(getPortAddress());
        return sb.toString();
    }

    @Override
    public void start()
    {
        if(isAvailable() && !hasTuner())
        {
            try
            {
                mTuner = TunerFactory.getUsbTuner(getTunerClass(), getPortAddress(), getBus(), this, mChannelizerType);
                mTuner.start();
            }
            catch(SourceException se)
            {
                //Set error message to flag this tuner with error status and invoke stop()
                setErrorMessage(se.getMessage());
                mLog.error("Unable to start tuner [" + getTunerClass() + "] - error: " + getErrorMessage());
            }
        }
    }

    @Override
    public String toString()
    {
        return "USB Tuner - " + getId();
    }
}
