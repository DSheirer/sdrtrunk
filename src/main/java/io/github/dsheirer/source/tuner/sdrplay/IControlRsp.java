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

package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.source.tuner.sdrplay.api.DeviceSelectionMode;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IDeviceEventListener;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IStreamListener;
import io.github.dsheirer.source.tuner.sdrplay.api.device.Device;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;

/**
 * Control interface for base RSP device
 */
public interface IControlRsp
{
    DeviceSelectionMode getDeviceSelectionMode();

    /**
     * Device descriptor for this RSP
     * @return device descriptor
     */
    Device getDevice();

    /**
     * Process an RSP device removal.  Device memory resources should be nullified and the software should not attempt
     * any further interaction with the device via the API.
     */
    void deviceRemoved();

    /**
     * Selected tuner for this RSP.
     * @return selected tuner.
     */
    TunerSelect getTunerSelect();

    /**
     * Starts the device
     */
    void start() throws SDRPlayException;

    /**
     * Stops the device
     */
    void stop() throws SDRPlayException;

    /**
     * Registers listeners to receive device events and streaming samples when startStream() is invoked.
     * @param deviceEventListener to receive device events
     * @param streamListener to receive sample stream and related parameters.
     */
    void resister(IDeviceEventListener deviceEventListener, IStreamListener streamListener);

    /**
     * Starts the sample stream and delivers the samples to the registered listener.
     */
    void startStream();

    /**
     * Stops the sample stream.
     */
    void stopStream();

    /**
     * Callback to acknowledge a power overload event.
     * @param tunerSelect identifies the tuner where the power overload is happening.
     */
    void acknowledgePowerOverload(TunerSelect tunerSelect) throws SDRPlayException;

    /**
     * Current center tune frequency
     * @return frequency in Hertz
     */
    long getTunedFrequency() throws SDRPlayException;

    /**
     * Sets the center tune frequency
     * @param frequency in Hertz
     */
    void setTunedFrequency(long frequency) throws SDRPlayException;

    /**
     * Current sample rate enumeration value
     * @return sample rate enumeration.
     */
    RspSampleRate getSampleRateEnumeration();

    /**
     * Sets the sample rate.
     * @param rspSampleRate enumeration value.
     */
    void setSampleRate(RspSampleRate rspSampleRate) throws SDRPlayException;

    /**
     * Current sample rate value.
     * @return sample rate in Hertz
     */
    double getCurrentSampleRate();

    /**
     * Sets the gain index value.
     * @param gain index value (0 - 28)
     */
    void setGain(int gain) throws SDRPlayException;

    /**
     * Current gain index value
     * @return gain index value (0 - 28)
     */
    int getGain();

    /**
     * Current IF AGC mode setting.
     * @return IF agc mode
     */
    AgcMode getAgcMode();

    /**
     * Sets the IF AGC mode
     * @param mode to set.
     */
    void setAgcMode(AgcMode mode) throws SDRPlayException;

    /**
     * Registers a listener to receive notifications of gain overload.
     * @param listener to register
     */
    void setGainOverloadListener(IGainOverloadListener listener);
}
