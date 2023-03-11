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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.source.tuner.sdrplay.ControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.IGainOverloadListener;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.api.DeviceSelectionMode;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.Status;
import io.github.dsheirer.source.tuner.sdrplay.api.UpdateReason;
import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoDevice;
import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoTuner;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.ControlParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.IfMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.LoMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.TunerParameters;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base control wrapper for an RSPduo operating in either single-tuner or master or slave mode.
 */
public abstract class ControlRspDuo<T extends RspDuoTuner> extends ControlRsp<RspDuoDevice> implements IControlRspDuo
{
    private final Logger mLog = LoggerFactory.getLogger(ControlRspDuo.class);

    /**
     * Constructs an instance
     *
     * @param device for the device, obtained from the API
     */
    public ControlRspDuo(RspDuoDevice device)
    {
        super(device);
    }

    /**
     * Access the tuner
     * @return tuner
     * @throws SDRPlayException if the device is not started/selected
     */
    protected abstract T getTuner() throws SDRPlayException;

    /**
     * Device selection mode - tuner 1 or tuner 2
     * @return mode
     */
    public abstract DeviceSelectionMode getDeviceSelectionMode();

    /**
     * Control parameters for either tuner 1 or tuner 2
     * @return control parameters
     */
    protected abstract ControlParameters getControlParameters();

    /**
     * Tuner parameters for either tuner 1 or tuner 2
     */
    protected abstract TunerParameters getTunerParameters();

    @Override
    public void start() throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().setRspDuoMode(getDeviceSelectionMode().getRspDuoMode());
            getDevice().setTunerSelect(getDeviceSelectionMode().getTunerSelect());

            //In master mode, we have to set the sample rate here, before we select the device
            if(getDeviceSelectionMode().isMasterMode())
            {
                getDevice().setRspDuoSampleFrequency(8_000_000);
            }

            getDevice().select();

            //Enable automatic DC and I/Q correction
            getControlParameters().getDcOffset().setDC(true);
            getControlParameters().getDcOffset().setIQ(true);

            //Setup LO Mode
            getTunerParameters().setLoMode(LoMode.AUTO);

            //Setup IF mode. In master/slave dual-tuner mode use IF_2048, otherwise use IF_ZERO
            if(getDeviceSelectionMode().isSingleTunerMode())
            {
                getTunerParameters().setIfMode(IfMode.IF_ZERO);
            }
            else
            {
                getTunerParameters().setIfMode(IfMode.IF_2048);
            }
        }
        else
        {
            throw new SDRPlayException("Unable to start - no device");
        }
    }

    @Override
    public void startStream()
    {
        mStreamingLock.lock();

        try
        {
            if(hasDevice() && !mStreaming)
            {
                if(getDeviceSelectionMode().isSlaveMode())
                {
                    getDevice().initStreamB(getDeviceEventListener(), getStreamListener());
                }
                else
                {
                    getDevice().initStreamA(getDeviceEventListener(), getStreamListener());
                }

                mStreaming = true;
            }
        }
        catch(SDRPlayException se)
        {
            if(se.getStatus() == Status.FAIL)
            {
                if(getDeviceEventListener() != null)
                {
                    getDeviceEventListener().processDeviceRemoval(getTunerSelect());
                }
            }
            else
            {
                mLog.error("Unable to initialize/start streaming for RSPduo", se);
            }
        }
        finally
        {
            mStreamingLock.unlock();
        }
    }

    @Override
    public void stopStream()
    {
        mStreamingLock.lock();

        try
        {
            if(hasDevice() && mStreaming)
            {
                getDevice().uninitialize();
                mStreaming = false;
            }
        }
        catch(Exception e)
        {
            mLog.error("Error while stopping RSPduo stream", e);
        }
        finally
        {
            mStreamingLock.unlock();
        }
    }

    /**
     * Sets the sample rate
     * @param sampleRate enumeration value.
     * @throws SDRPlayException if the device is not started/initialized
     */
    @Override
    public void setSampleRate(RspSampleRate sampleRate) throws SDRPlayException
    {
        if(hasDevice())
        {
            mSampleRate = sampleRate;

            getDevice().getTuner().setBandwidth(sampleRate.getBandwidth());
            getDevice().getCompositeParameters().getDeviceParameters().getSamplingFrequency()
                    .setSampleRate(sampleRate.getSampleRate());
            //Only send an update if we're in single-tuner mode ... not sure why we don't have to for dual-tuner mode.
            if(getDeviceSelectionMode() != DeviceSelectionMode.MASTER_TUNER_1)
            {
                getDevice().update(getTunerSelect(), UpdateReason.DEVICE_SAMPLE_RATE);
            }

            getControlParameters().getDecimation().setWideBandSignal(true);
            getDevice().setDecimation(sampleRate.getDecimation());
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    /**
     * Sets the gain index
     * @param gain index value (0 - 28)
     * @throws SDRPlayException for errors setting gain value.
     */
    @Override
    public void setGain(int gain) throws SDRPlayException
    {
        validateGain(gain);

        if(gain != mGain)
        {
            mGain = gain;
            getTuner().setGain(mGain);
        }
    }

    @Override
    public void acknowledgePowerOverload(TunerSelect tunerSelect) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().acknowledgePowerOverload(tunerSelect);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }

        //Notify an optional weakly referenced, registered listener that gain overload has been acknowledged.
        if(mGainOverloadReference != null)
        {
            IGainOverloadListener listener = mGainOverloadReference.get();

            if(listener != null)
            {
                ThreadPool.CACHED.submit(() -> listener.notifyGainOverload(tunerSelect));
            }
        }
    }

    @Override
    public long getTunedFrequency() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getTuner().getFrequency();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setTunedFrequency(long frequency) throws SDRPlayException
    {
        if(hasDevice())
        {
            getTuner().setFrequency(frequency);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public boolean isRfDabNotch() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getTuner().isRfDabNotch();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setRfDabNotch(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getTuner().setRfDabNotch(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public boolean isRfNotch() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getTuner().isRFNotch();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setRfNotch(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getTuner().setRfNotch(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public boolean isExternalReferenceOutput() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getTuner().isExternalReferenceOutput();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setExternalReferenceOutput(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getTuner().setExternalReferenceOutput(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }
}
