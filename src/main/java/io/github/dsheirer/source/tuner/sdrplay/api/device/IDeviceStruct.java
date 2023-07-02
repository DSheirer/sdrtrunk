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

import java.lang.foreign.MemorySegment;

/**
 * Interface for parsing and accessing the fields of the sdrplay_api_DeviceT structure across
 * various versions.
 */
public interface IDeviceStruct
{
    /**
     * Foreign memory segment for this device structure
     */
    MemorySegment getDeviceMemorySegment();

    /**
     * Serial number of the device
     */
    String getSerialNumber();

    /**
     * Indicates the device type, or model of RSP
     */
    DeviceType getDeviceType();

    /**
     * Indicates single, dual, or master/slave mode for the tuner.
     */
    TunerSelect getTunerSelect();

    /**
     * RSPduo mode
     */
    RspDuoMode getRspDuoMode();

    /**
     * Sets the RSPduo mode
     * @param mode to set
     */
    void setRspDuoMode(RspDuoMode mode);

    /**
     * Indicates if the device is valid and ready for use (V3.08 and later)
     */
    boolean isValid();

    /**
     * Sample frequency/rate for the RSPduo when in master/slave mode
     */
    double getRspDuoSampleFrequency();

    /**
     * Sets the sample frequency/rate for the RSPduo when in master (only) mode.
     */
    void setRspDuoSampleFrequency(double frequency);

    /**
     * Device handle.  Note this is only available if the device has been selected.
     */
    MemorySegment getDeviceHandle();
}
