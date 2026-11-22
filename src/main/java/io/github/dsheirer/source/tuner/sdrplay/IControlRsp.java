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

package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.sdrplay.DeviceEventListener;
import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.SampleListener;
import io.github.dsheirer.sdrplay.Tuner;

/**
 * Control interface for base RSP device
 */
public interface IControlRsp
{
    /**
     * Device selection mode for this RSP.  All RSP devices use single tuner 1, except the RSPduo.
     */
    DeviceSelectionMode getDeviceSelectionMode();

    /**
     * Device information (model and serial number) for this RSP, as discovered from the API.
     * @return device info
     */
    DeviceInfo getDeviceInfo();

    /**
     * Indicates if the device is started (selected from the API) and available for control.
     */
    boolean hasDevice();

    /**
     * Process an RSP device removal.  Closes the device and releases it from the API.  Note: this method must not
     * be invoked from an SDRplay API callback (stream or event listener) thread.
     */
    void deviceRemoved();

    /**
     * Selected tuner for this RSP: tuner A for all RSP devices, except the RSPduo.
     * @return selected tuner.
     */
    Tuner getSdrplayTuner();

    /**
     * Starts the device: selects the device from the API for exclusive use.
     * @throws RspException if the device is not available or can't be selected
     */
    void start() throws RspException;

    /**
     * Stops the device: stops streaming and releases the device back to the API.
     * @throws RspException if there is an error
     */
    void stop() throws RspException;

    /**
     * Registers listeners to receive device events and streaming samples when startStream() is invoked.
     * @param deviceEventListener to receive device events
     * @param sampleListener to receive sample stream.
     */
    void register(DeviceEventListener deviceEventListener, SampleListener sampleListener);

    /**
     * Starts the sample stream and delivers the samples to the registered listener.
     */
    void startStream();

    /**
     * Stops the sample stream.
     */
    void stopStream();

    /**
     * Process a power overload detected event from the API.  The jsdrplay library acknowledges the event to the API.
     * @param tuner where the power overload is happening.
     */
    void processPowerOverload(Tuner tuner);

    /**
     * Current center tune frequency
     * @return frequency in Hertz
     */
    long getTunedFrequency() throws RspException;

    /**
     * Sets the center tune frequency
     * @param frequency in Hertz
     */
    void setTunedFrequency(long frequency) throws RspException;

    /**
     * Current sample rate enumeration value
     * @return sample rate enumeration.
     */
    RspSampleRate getSampleRateEnumeration();

    /**
     * Sets the sample rate.
     * @param rspSampleRate enumeration value.
     */
    void setSampleRate(RspSampleRate rspSampleRate) throws RspException;

    /**
     * Current sample rate value.
     * @return sample rate in Hertz
     */
    double getCurrentSampleRate();

    /**
     * Set the lna state and gain reduction dB values separately.
     * @param lna state (varies by tuner and frequency range)
     * @param basebandGainReduction setting (20-59)
     * @throws RspException if the device is not started or the API rejects the setting.
     */
    void setGain(int lna, int basebandGainReduction) throws RspException;

    /**
     * Current gain setting as reported by the device.
     * @return gain value in decibels
     */
    float getCurrentGain() throws RspException;

    /**
     * Current LNA gain index value
     * @return LNA gain value.
     */
    int getLNA();

    /**
     * Maximum LNA setting index value.
     * @return max LNA that varies by tuner type.
     */
    int getMaximumLNASetting();

    /**
     * Current gain reduction value.
     * @return gain reduction value.
     */
    int getBasebandGainReduction();

    /**
     * Current IF AGC mode setting.
     * @return IF agc mode, or null if the device is not started.
     */
    RspAgcMode getAgcMode();

    /**
     * Sets the IF AGC mode
     * @param mode to set.
     */
    void setAgcMode(RspAgcMode mode) throws RspException;

    /**
     * Registers a listener to receive notifications of gain overload and frequency changes.
     * @param listener to register
     */
    void setGainOverloadListener(ITunerStatusListener listener);
}
