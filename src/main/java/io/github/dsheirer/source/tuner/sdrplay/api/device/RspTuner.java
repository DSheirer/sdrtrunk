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

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRplay;
import io.github.dsheirer.source.tuner.sdrplay.api.UpdateReason;
import io.github.dsheirer.source.tuner.sdrplay.api.async.AsyncUpdateFuture;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.ControlParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.DeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Bandwidth;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Gain;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.IfMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.LoMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.TunerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract RSP device tuner
 * @param <T> RSP device implementation
 */
public abstract class RspTuner<D extends DeviceParameters, T extends TunerParameters>
{
    private static final Logger mLog = LoggerFactory.getLogger(RspTuner.class);
    private final SDRplay mSDRplay;
    private final Device mDevice;
    private final TunerSelect mTunerSelect;
    private final D mDeviceParameters;
    private final T mTunerParameters;
    private final ControlParameters mControlParameters;

    RspTuner(Device device, SDRplay sdrplay, TunerSelect tunerSelect, D deviceParameters, T tunerParameters,
             ControlParameters controlParameters)
    {
        mDevice = device;
        mSDRplay = sdrplay;
        mTunerSelect = tunerSelect;
        mDeviceParameters = deviceParameters;
        mTunerParameters = tunerParameters;
        mControlParameters = controlParameters;
    }

    /**
     * SDRplay API
     */
    private SDRplay getSDRplay()
    {
        return mSDRplay;
    }

    /**
     * Parent device for this tuner
     */
    private Device getDevice()
    {
        return mDevice;
    }

    /**
     * Selected tuner
     */
    public TunerSelect getTunerSelect()
    {
        return mTunerSelect;
    }

    /**
     * Device parameters
     */
    protected D getDeviceParameters()
    {
        return mDeviceParameters;
    }

    /**
     * Tuner parameters for the selected tuner
     */
    public T getTunerParameters()
    {
        return mTunerParameters;
    }

    /**
     * Control parameters for the selected tuner
     */
    protected ControlParameters getControlParameters()
    {
        return mControlParameters;
    }

    /**
     * Convenience method for notifying the API that parameters for the device have been updated, when the device is
     * initialized.
     *
     * @param updateReasons indicating what has been updated
     * @throws SDRPlayException if there is an error
     */
    protected void update(UpdateReason... updateReasons) throws SDRPlayException
    {
        getDevice().update(getTunerSelect(), updateReasons);
    }

    /**
     * Convenience method for notifying the device that a parameter for this tuner have been updated.
     *
     * Note: the asynchronous device event responses are limited to Frequency, Gain and Sample Rate.  For example,
     * if you update the RSPduo DAB Notch parameter, it will generate a Gain change notification, so we submit
     * an async update for the DAB Notch, but anticipate a Gain update response.
     *
     * @param updateReason indicating what has been updated
     * @param expectedResponse that will be received indicating that the async operation is completed.
     */
    protected AsyncUpdateFuture updateAsync(UpdateReason updateReason, UpdateReason expectedResponse)
    {
        return getDevice().updateAsync(getTunerSelect(), updateReason, expectedResponse);
    }

    /**
     * Current tuner bandwidth
     */
    public Bandwidth getBandwidth()
    {
        return getTunerParameters().getBandwidth();
    }

    /**
     * Sets tuner bandwidth
     * @param bandwidth to apply
     * @throws SDRPlayException if there is an error
     */
    public void setBandwidth(Bandwidth bandwidth) throws SDRPlayException
    {
        getTunerParameters().setBandwidth(bandwidth);
        update(UpdateReason.TUNER_BANDWIDTH_TYPE);
    }

    /**
     * Center frequency for the tuner
     */
    public long getFrequency()
    {
        return (long) getTunerParameters().getRfFrequency().getFrequency();
    }

    /**
     * Requests to set the center frequency for the tuner asynchronously.
     *
     * Note: the API supports the notion of synchronous frequency updates, but in testing I found that you cannot
     * submit a second frequency change update request until a previously submitted frequency change update
     * operation had completed, otherwise it would generate a fail status.  Therefore, this is method is strictly
     * an async operation.
     *
     * @param frequency in Hertz
     * @return future that can be monitored for completion of the set frequency operation.
     */
    public AsyncUpdateFuture setFrequency(long frequency)
    {
        try
        {
            getTunerParameters().getRfFrequency().setFrequency(frequency, false);
            return updateAsync(UpdateReason.TUNER_FREQUENCY_RF, UpdateReason.TUNER_FREQUENCY_RF);
        }
        catch(SDRPlayException se)
        {
            AsyncUpdateFuture future = new AsyncUpdateFuture(getTunerSelect(), UpdateReason.TUNER_FREQUENCY_RF,
                    UpdateReason.TUNER_FREQUENCY_RF);
            future.setError(se);
            return future;
        }
    }

    /**
     * Sets the LNA state and Baseband Gain Reduction values.
     * @param lna state (varies by tuner and frequency range)
     * @param basebandGainReduction in range 20-59 dB
     * @throws SDRPlayException
     */
    public void setGain(int lna, int basebandGainReduction) throws SDRPlayException
    {
        getTunerParameters().getGain().setLNA(lna);
        getTunerParameters().getGain().setGainReductionDb(basebandGainReduction);
        update(UpdateReason.TUNER_GAIN_REDUCTION);
    }

    /**
     * Sets the automatic gain contral (AGC) mode
     *
     * @param mode to set
     * @throws SDRPlayException if there is an error
     */
    public void setAGC(AgcMode mode) throws SDRPlayException
    {
        getControlParameters().getAgc().setAgcMode(mode);
        update(UpdateReason.CONTROL_AGC);
    }

    /**
     * AGC Mode
     */
    public AgcMode getAGC()
    {
        return getControlParameters().getAgc().getAgcMode();
    }

    /**
     * Gain for the tuner.
     */
    public Gain getGain()
    {
        return getTunerParameters().getGain();
    }

    /**
     * Enables or disables DC correction
     *
     * @param enable true or false
     * @throws SDRPlayException if there is an error
     */
    public void setDCCorrection(boolean enable) throws SDRPlayException
    {
        getControlParameters().getDcOffset().setDC(enable);
        update(UpdateReason.CONTROL_DC_OFFSET_IQ_IMBALANCE);
    }

    /**
     * Enables or disables IQ imbalance correction
     *
     * @param enable true or false
     * @throws SDRPlayException if there is an error
     */
    public void setIQCorrection(boolean enable) throws SDRPlayException
    {
        getControlParameters().getDcOffset().setIQ(enable);
        update(UpdateReason.CONTROL_DC_OFFSET_IQ_IMBALANCE);
    }

    /**
     * Sets or updates the parts per million (ppm) frequency correction
     *
     * @param ppm value
     * @throws SDRPlayException
     */
    public AsyncUpdateFuture setPPM(double ppm) throws SDRPlayException
    {
        getDeviceParameters().setPPM(ppm);

        //Note: async ppm updates effect both frequency and sample rate, so we expect the completion of the
        //operation will be first FREQUENCY and then SAMPLE RATE, so we watch for sample rate.
        return updateAsync(UpdateReason.DEVICE_PPM, UpdateReason.DEVICE_SAMPLE_RATE);
    }

    /**
     * Parts per million (PPM) frequency correction value
     *
     * @return ppm value
     */
    public double getPPM()
    {
        return getDeviceParameters().getPPM();
    }

    /**
     * Perform synchronous update
     *
     * @param sampleNumber value
     * @param period value
     * @throws SDRPlayException if there is an error
     */
    public void setSynchronousUpdate(int sampleNumber, int period) throws SDRPlayException
    {
        getDeviceParameters().getSynchronousUpdate().set(sampleNumber, period);
        update(UpdateReason.DEVICE_SYNC_UPDATE);
    }

    /**
     * Resets device functions to default
     *
     * @param frequency to reset
     * @param sampleRate to reset
     * @param gain to reset
     * @throws SDRPlayException if there is an error
     */
    public void reset(boolean frequency, boolean sampleRate, boolean gain) throws SDRPlayException
    {
        getDeviceParameters().getResetFlags().resetGain(gain);
        getDeviceParameters().getResetFlags().resetFrequency(frequency);
        getDeviceParameters().getResetFlags().resetSampleRate(sampleRate);
        update(UpdateReason.DEVICE_RESET_FLAGS);
    }

    /**
     * Resets the frequency.
     *
     * @param frequency true to reset
     * @throws SDRPlayException if there is an error
     */
    public void resetFrequency(boolean frequency) throws SDRPlayException
    {
        reset(frequency, false, false);
    }

    /**
     * Resets the sample rate.
     *
     * @param sampleRate true to reset
     * @throws SDRPlayException if there is an error
     */
    public void resetSampleRate(boolean sampleRate) throws SDRPlayException
    {
        reset(false, sampleRate, false);
    }

    /**
     * Resets the gain (reduction).
     *
     * @param gain true to reset
     * @throws SDRPlayException if there is an error
     */
    public void resetGain(boolean gain) throws SDRPlayException
    {
        reset(false, false, gain);
    }

    /**
     * IF mode (type)
     */
    public IfMode getIfMode()
    {
        return getTunerParameters().getIfMode();
    }

    /**
     * Sets the IF mode (type)
     * @param mode to set
     * @throws SDRPlayException if there is an error
     */
    public void setIfMode(IfMode mode) throws SDRPlayException
    {
        getTunerParameters().setIfMode(mode);
        update(UpdateReason.TUNER_IF_TYPE);
    }

    /**
     * Local Oscillator (LO) mode
     * @return mode
     */
    public LoMode getLoMode()
    {
        return getTunerParameters().getLoMode();
    }

    /**
     * Sets the Local Oscillator (LO) mode
     * @param mode to set
     * @throws SDRPlayException if there is an error
     */
    public void setLoMode(LoMode mode) throws SDRPlayException
    {
        getTunerParameters().setLoMode(mode);
        update(UpdateReason.TUNER_LO_MODE);
    }
}
