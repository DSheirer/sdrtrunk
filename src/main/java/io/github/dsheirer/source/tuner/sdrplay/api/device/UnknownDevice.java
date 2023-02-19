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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRplay;

/**
 * Unknown or Unrecognized SDRplay Device
 */
public class UnknownDevice extends Device
{
    /**
     * Constructs an Unknown SDRPlay device from the foreign memory segment
     *
     * @param sdrPlay api instance that created this device
     * @param deviceStruct parser
     */
    UnknownDevice(SDRplay sdrPlay, IDeviceStruct deviceStruct)
    {
        super(sdrPlay, deviceStruct);
    }

    @Override
    public RspTuner getTuner() throws SDRPlayException
    {
        throw new SDRPlayException("Unrecognized device type.  Cannot construct tuner");
    }
}
