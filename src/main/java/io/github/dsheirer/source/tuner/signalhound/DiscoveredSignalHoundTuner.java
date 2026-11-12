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
import io.github.dsheirer.signalhound.DeviceDescriptor;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;

/**
 * Signal Hound tuner discovered from the device list of a Signal Hound API.
 */
public class DiscoveredSignalHoundTuner extends DiscoveredTuner
{
    private final DeviceDescriptor mDescriptor;
    private final ChannelizerType mChannelizerType;

    /**
     * Constructs an instance
     * @param descriptor of the device, from the Signal Hound API device list
     * @param channelizerType to use for the tuner
     */
    public DiscoveredSignalHoundTuner(DeviceDescriptor descriptor, ChannelizerType channelizerType)
    {
        mDescriptor = descriptor;
        mChannelizerType = channelizerType;
    }

    /**
     * Descriptor of the device
     */
    public DeviceDescriptor getDescriptor()
    {
        return mDescriptor;
    }

    /**
     * Device family
     */
    public SignalHoundFamily getFamily()
    {
        return SignalHoundFamily.of(mDescriptor.api());
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.SIGNAL_HOUND;
    }

    /**
     * Constructs and starts the tuner
     */
    @Override
    public void start()
    {
        if(isAvailable() && !hasTuner())
        {
            mTuner = TunerFactory.getSignalHoundTuner(mDescriptor, mChannelizerType, this);

            try
            {
                mTuner.start();
            }
            catch(Exception se)
            {
                setErrorMessage("Error starting tuner [" + getId() + "] - " + se.getMessage());
                mTuner = null;
            }
        }
    }

    /**
     * Tuner ID, which identifies the tuner configuration.  The family is used instead of the model, since the APIs
     * don't always report the model before the device is opened, and the ID must be the same each time.
     */
    @Override
    public String getId()
    {
        return getId(mDescriptor);
    }

    /**
     * Tuner ID of a device.
     */
    public static String getId(DeviceDescriptor descriptor)
    {
        return "Signal Hound " + SignalHoundFamily.of(descriptor.api()) + " SER#" + descriptor.serialNumber();
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
