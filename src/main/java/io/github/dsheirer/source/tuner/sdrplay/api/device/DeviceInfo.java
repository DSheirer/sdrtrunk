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

import io.github.dsheirer.source.tuner.sdrplay.api.DeviceSelectionMode;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * RSP device information to support device selection.
 */
public class DeviceInfo
{
    private DeviceSelectionMode mDeviceSelectionMode = DeviceSelectionMode.SINGLE_TUNER_1;
    private DeviceType mDeviceType;
    private String mSerialNumber;

    /**
     * Constructs an instance
     * @param deviceType of the RSP device
     * @param serialNumber for the device
     */
    public DeviceInfo(DeviceType deviceType, String serialNumber)
    {
        Validate.notNull(deviceType, "Device type cannot be null");
        Validate.notNull(serialNumber, "Device serial number cannot be null");

        mDeviceType = deviceType;
        mSerialNumber = serialNumber;
    }

    /**
     * Alternate constructor where we get the parameters from a device structure.
     * @param deviceStruct containing device type and serial number values.
     */
    public DeviceInfo(IDeviceStruct deviceStruct)
    {
        this(deviceStruct.getDeviceType(), deviceStruct.getSerialNumber());
    }

    /**
     * Device type for this RSP device
     * @return type
     */
    public DeviceType getDeviceType()
    {
        return mDeviceType;
    }

    /**
     * Serial number for this RSP device
     * @return serial number
     */
    public String getSerialNumber()
    {
        return mSerialNumber;
    }

    /**
     * Device selection mode for this RSP device
     * @return device selection mode where single tuner 1 is default
     */
    public DeviceSelectionMode getDeviceSelectionMode()
    {
        return mDeviceSelectionMode;
    }

    /**
     * Sets the device selection mode.  This should be left as default of single tuner 1 unless this device is an RSPduo.
     * @param deviceSelectionMode to use for this device.
     */
    public void setDeviceSelectionMode(DeviceSelectionMode deviceSelectionMode)
    {
        Validate.notNull(deviceSelectionMode, "Device selection mode cannot be null");
        mDeviceSelectionMode = deviceSelectionMode;
    }

    /**
     * Clones this instance
     * @return deep copy.
     */
    public DeviceInfo clone()
    {
        return new DeviceInfo(mDeviceType, mSerialNumber);
    }

    /**
     * Indicates if this device type and serial number matches the details of the device structure.
     * @param deviceStruct to compare
     * @return true if the device type and serial number match.
     */
    public boolean matches(IDeviceStruct deviceStruct)
    {
        return deviceStruct != null && getDeviceType().equals(deviceStruct.getDeviceType()) && getSerialNumber().equals(deviceStruct.getSerialNumber());
    }

    @Override
    public String toString()
    {
        return "Device: " + getDeviceType() + " Serial Number: " + getSerialNumber() + " Selection Mode: " + getDeviceSelectionMode();
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }
        DeviceInfo that = (DeviceInfo) o;
        return mDeviceSelectionMode == that.mDeviceSelectionMode && mDeviceType == that.mDeviceType && mSerialNumber.equals(that.mSerialNumber);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mDeviceSelectionMode, mDeviceType, mSerialNumber);
    }
}
