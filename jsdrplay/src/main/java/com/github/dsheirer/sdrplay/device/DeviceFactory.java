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

package com.github.dsheirer.sdrplay.device;

import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.SDRplay;
import com.github.dsheirer.sdrplay.Version;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_DeviceT;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_h;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory methods for creating new RSP Device instances
 */
public class DeviceFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(DeviceFactory.class);

    /**
     * Creates a foreign memory segment for a DeviceT array, appropriate for the specified version.
     * @param version value
     * @param segmentAllocator to allocate the foreign memory
     * @return devices array
     */
    public static MemorySegment createDeviceArray(Version version, SegmentAllocator segmentAllocator)
    {
        if(version.gte(Version.V3_08))
        {
            return com.github.dsheirer.sdrplay.api.v3_08.sdrplay_api_DeviceT.allocateArray(sdrplay_api_h.SDRPLAY_MAX_DEVICES(), segmentAllocator);
        }
        else if(version == Version.V3_07)
        {
            return sdrplay_api_DeviceT.allocateArray(sdrplay_api_h.SDRPLAY_MAX_DEVICES(), segmentAllocator);
        }

        throw new IllegalArgumentException("Unrecognized version: " + version);
    }

    /**
     * Parses device information from a list of device structures
     * @param deviceStructs representing devices to parse
     * @return a list of device infos
     */
    public static List<DeviceInfo> parseDeviceInfos(List<IDeviceStruct> deviceStructs)
    {
        List<DeviceInfo> deviceInfos = new ArrayList<>();

        for(IDeviceStruct deviceStruct: deviceStructs)
        {
            deviceInfos.add(new DeviceInfo(deviceStruct));
        }

        return deviceInfos;
    }

    /**
     * Parses device information from a memory segment containing an array of device structures.
     * @param version of the API
     * @param devicesArray memory segment
     * @param count of device structures in the devicesArray.
     * @return a list of device infos
     * @throws exception if version is unrecognized or unsupported
     */
    public static List<IDeviceStruct> parseDeviceStructs(Version version, MemorySegment devicesArray, int count) throws SDRPlayException
    {
        List<IDeviceStruct> deviceStructs = new ArrayList<>();

        if(version.gte(Version.V3_08))
        {
            devicesArray.elements(com.github.dsheirer.sdrplay.api.v3_08.sdrplay_api_DeviceT.$LAYOUT())
                    .limit(count).forEach(memorySegment ->
                            deviceStructs.add(DeviceFactory.createDeviceStruct(version, memorySegment)));
        }
        else if(version == Version.V3_07)
        {
            devicesArray.elements(sdrplay_api_DeviceT.$LAYOUT()).limit(count).forEach(memorySegment ->
            {
                deviceStructs.add(DeviceFactory.createDeviceStruct(version, memorySegment));
            });
        }
        else
        {
            throw new SDRPlayException("Unrecognized version: " + version);
        }

        return deviceStructs;
    }

    /**
     * Creates an SDRplay device from the foreign memory Device instance.
     * @param sdrPlay for device callback support
     * @param deviceMemorySegment instance for the device
     * @return correctly typed device
     */
    public static Device createDevice(SDRplay sdrPlay, IDeviceStruct deviceStruct)
    {
        switch(deviceStruct.getDeviceType())
        {
            case RSP1 -> {
                return new Rsp1Device(sdrPlay, deviceStruct);
            }
            case RSP1A -> {
                return new Rsp1aDevice(sdrPlay, deviceStruct);
            }
            case RSP2 -> {
                return new Rsp2Device(sdrPlay, deviceStruct);
            }
            case RSPduo -> {
                return new RspDuoDevice(sdrPlay, deviceStruct);
            }
            case RSPdx -> {
                return new RspDxDevice(sdrPlay, deviceStruct);
            }
            default -> {
                return new UnknownDevice(sdrPlay, deviceStruct);
            }
        }
    }

    /**
     * Creates a device structure parser for the specified API version
     * @param version to create
     * @param deviceMemorySegment for the device
     * @return device structure
     */
    private static IDeviceStruct createDeviceStruct(Version version, MemorySegment deviceMemorySegment)
    {
        if(version == Version.V3_07)
        {
            return new DeviceStruct_v3_07(deviceMemorySegment);
        }
        else if(version.gte(Version.V3_08))
        {
            return new DeviceStruct_v3_08(deviceMemorySegment);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }
    }
}
