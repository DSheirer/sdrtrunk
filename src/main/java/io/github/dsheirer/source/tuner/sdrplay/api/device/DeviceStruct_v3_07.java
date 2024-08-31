/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DeviceT;
import java.lang.foreign.MemorySegment;

/**
 * sdrplay_api_DeviceT structure parser for API version 3.07
 */
public class DeviceStruct_v3_07 implements IDeviceStruct
{
    private final MemorySegment mDeviceMemorySegment;
    private final DeviceType mDeviceType;
    private final String mSerialNumber;

    /**
     * Constructs an instance
     * @param deviceMemorySegment of foreign memory
     */
    public DeviceStruct_v3_07(MemorySegment deviceMemorySegment)
    {
        mDeviceMemorySegment = deviceMemorySegment;
        mDeviceType = DeviceType.fromValue(0xFF & sdrplay_api_DeviceT.hwVer(mDeviceMemorySegment));
        MemorySegment serialSegment = sdrplay_api_DeviceT.SerNo(mDeviceMemorySegment);
        mSerialNumber = serialSegment.getString(0);
    }

    @Override public MemorySegment getDeviceMemorySegment()
    {
        return mDeviceMemorySegment;
    }

    @Override public String getSerialNumber()
    {
        return mSerialNumber;
    }

    @Override public DeviceType getDeviceType()
    {
        return mDeviceType;
    }

    @Override public TunerSelect getTunerSelect()
    {
        return TunerSelect.fromValue(sdrplay_api_DeviceT.tuner(getDeviceMemorySegment()));
    }

    @Override public RspDuoMode getRspDuoMode()
    {
        return RspDuoMode.fromValue(sdrplay_api_DeviceT.rspDuoMode(getDeviceMemorySegment()));
    }

    @Override
    public void setRspDuoMode(RspDuoMode mode)
    {
        sdrplay_api_DeviceT.rspDuoMode(getDeviceMemorySegment(), mode.getValue());
    }

    @Override public boolean isValid()
    {
        //Always returns true for version 3.07
        return true;
    }

    @Override public double getRspDuoSampleFrequency()
    {
        return sdrplay_api_DeviceT.rspDuoSampleFreq(getDeviceMemorySegment());
    }

    @Override
    public void setRspDuoSampleFrequency(double frequency)
    {
        sdrplay_api_DeviceT.rspDuoSampleFreq(getDeviceMemorySegment(), frequency);
    }

    @Override public MemorySegment getDeviceHandle()
    {
        return sdrplay_api_DeviceT.dev(getDeviceMemorySegment());
    }
}
