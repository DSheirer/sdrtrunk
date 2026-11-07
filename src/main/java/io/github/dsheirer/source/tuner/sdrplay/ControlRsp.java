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

import io.github.dsheirer.sdrplay.DeviceEvent;
import io.github.dsheirer.sdrplay.DeviceEventListener;
import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.IfMode;
import io.github.dsheirer.sdrplay.LoMode;
import io.github.dsheirer.sdrplay.RspDevice;
import io.github.dsheirer.sdrplay.RxChannel;
import io.github.dsheirer.sdrplay.SDRplay;
import io.github.dsheirer.sdrplay.SDRplayError;
import io.github.dsheirer.sdrplay.SDRplayException;
import io.github.dsheirer.sdrplay.SampleListener;
import io.github.dsheirer.sdrplay.Tuner;
import io.github.dsheirer.util.ThreadPool;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base RSP control implementation using the jsdrplay library.
 *
 * The device is selected from the SDRplay API when start() is invoked and released when stop() is invoked.  Each
 * control opens its own jsdrplay session, which shares the single process-wide connection to the API service.
 *
 * @param <T> jsdrplay device type for the RSP model
 */
public abstract class ControlRsp<T extends RspDevice> implements IControlRsp
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlRsp.class);
    private final DeviceInfo mDeviceInfo;
    private final Class<T> mDeviceClass;
    private SDRplay mSession;
    private volatile T mDevice;
    protected RspSampleRate mSampleRate = RspSampleRate.RATE_8_000;
    protected int mLNA;
    protected int mBasebandGainReduction;
    private DeviceEventListener mDeviceEventListener;
    private SampleListener mSampleListener;
    protected WeakReference<ITunerStatusListener> mGainOverloadReference;

    //Streaming control lock and boolean status indicator.  Access to the boolean indicator is protected by the lock.
    protected ReentrantLock mStreamingLock = new ReentrantLock();
    protected boolean mStreaming = false;

    /**
     * Constructs an instance
     * @param deviceInfo for the device, obtained from the API
     * @param deviceClass jsdrplay device class that the API returns for this RSP model
     */
    public ControlRsp(DeviceInfo deviceInfo, Class<T> deviceClass)
    {
        mDeviceInfo = deviceInfo;
        mDeviceClass = deviceClass;
    }

    @Override
    public DeviceInfo getDeviceInfo()
    {
        return mDeviceInfo;
    }

    @Override
    public Tuner getSdrplayTuner()
    {
        return Tuner.A;
    }

    @Override
    public DeviceSelectionMode getDeviceSelectionMode()
    {
        //Default device selection mode for all tuners except the RSPduo
        return DeviceSelectionMode.SINGLE_TUNER_1;
    }

    /**
     * Selects the device from the API.  Subclasses override this to select the RSPduo in a specific mode.
     * @param session to select from
     * @param deviceInfo current device info from the API
     * @return selected device
     * @throws SDRplayException if the API can't select the device
     */
    protected RspDevice select(SDRplay session, DeviceInfo deviceInfo) throws SDRplayException
    {
        return session.select(deviceInfo);
    }

    /**
     * IF mode to use for the selected tuner.
     */
    protected IfMode getIfMode()
    {
        return IfMode.ZERO;
    }

    @Override
    public void start() throws RspException
    {
        if(hasDevice())
        {
            return;
        }

        SDRplay session = null;

        try
        {
            session = SDRplay.open();
            RspDevice device = select(session, getCurrentDeviceInfo(session));

            if(!mDeviceClass.isInstance(device))
            {
                device.close();
                throw new RspException("Unexpected SDRplay device class [" + device.getClass().getSimpleName() +
                    "] for " + mDeviceInfo);
            }

            //Enable automatic DC and I/Q correction and setup IF and LO mode
            RxChannel channel = device.channel();
            channel.setDcOffsetCorrection(true);
            channel.setIqCorrection(true);
            channel.setIfMode(getIfMode());
            channel.setLoMode(LoMode.AUTO);

            mSession = session;
            mDevice = mDeviceClass.cast(device);
        }
        catch(SDRplayException | RuntimeException e)
        {
            //Closing the session also closes any device selected through it
            closeQuietly(session);
            throw new RspException("Unable to select " + mDeviceInfo + " - " + e.getMessage(), e);
        }
        catch(RspException re)
        {
            closeQuietly(session);
            throw re;
        }
    }

    /**
     * Finds the current device info from the API for this device.  The API lists only devices (or RSPduo tuners)
     * that are not in use and the RSPduo modes that are currently available.
     * @param session to query
     * @return current device info
     * @throws RspException if the device is not available
     */
    private DeviceInfo getCurrentDeviceInfo(SDRplay session) throws SDRplayException, RspException
    {
        for(DeviceInfo deviceInfo: session.devices())
        {
            if(deviceInfo.serialNumber().equals(mDeviceInfo.serialNumber()))
            {
                return deviceInfo;
            }
        }

        throw new RspException("Device is not available - it may be in use by another application");
    }

    @Override
    public void stop() throws RspException
    {
        stopStream();

        T device = mDevice;
        SDRplay session = mSession;
        mDevice = null;
        mSession = null;

        try
        {
            if(device != null)
            {
                device.close();
            }
        }
        catch(SDRplayException | RuntimeException e)
        {
            throw new RspException("Error releasing " + mDeviceInfo, e);
        }
        finally
        {
            closeQuietly(session);
        }
    }

    /**
     * Closes the session, ignoring and logging any errors.
     */
    private void closeQuietly(SDRplay session)
    {
        if(session != null)
        {
            try
            {
                session.close();
            }
            catch(Exception e)
            {
                mLog.warn("Error closing SDRplay API session - " + e.getMessage());
            }
        }
    }

    @Override
    public void startStream()
    {
        mStreamingLock.lock();

        try
        {
            if(!mStreaming)
            {
                getDevice().start(mSampleListener, mDeviceEventListener);
                mStreaming = true;
            }
        }
        catch(RspException re)
        {
            mLog.error("Unable to start RSP device sample stream - device not started");
        }
        catch(SDRplayException se)
        {
            if(se.error() == SDRplayError.FAIL)
            {
                mLog.error("Unable to start RSP streaming - device may have been unplugged - removing.");

                if(mDeviceEventListener != null)
                {
                    mDeviceEventListener.onEvent(new DeviceEvent.DeviceRemoved(getSdrplayTuner()));
                }
            }
            else
            {
                mLog.error("Unable to initialize/start streaming for RSP device", se);
            }
        }
        catch(RuntimeException re)
        {
            mLog.error("Unable to initialize/start streaming for RSP device", re);
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
            T device = mDevice;

            if(mStreaming && device != null && !device.isClosed())
            {
                device.stop();
            }
        }
        catch(SDRplayException | RuntimeException e)
        {
            mLog.error("Unable to uninitialize/stop streaming for RSP device - " + e.getMessage());
        }
        finally
        {
            mStreaming = false;
            mStreamingLock.unlock();
        }
    }

    /**
     * Started device.
     * @return device
     * @throws RspException if the device is not started or was closed.
     */
    protected T getDevice() throws RspException
    {
        T device = mDevice;

        if(device == null || device.isClosed())
        {
            throw new RspException("RSP device is not started");
        }

        return device;
    }

    /**
     * RSP device was unplugged from USB bus.  Close the device to release it from the API.
     */
    @Override
    public void deviceRemoved()
    {
        try
        {
            stop();
        }
        catch(RspException re)
        {
            mLog.info("Error releasing removed RSP device " + mDeviceInfo + " - " + re.getMessage());
        }
    }

    @Override
    public boolean hasDevice()
    {
        T device = mDevice;
        return device != null && !device.isClosed();
    }

    /**
     * Command that is applied to the device.
     */
    @FunctionalInterface
    protected interface DeviceCommand<D>
    {
        void apply(D device) throws SDRplayException;
    }

    /**
     * Query that is applied to the device.
     */
    @FunctionalInterface
    protected interface DeviceQuery<D, R>
    {
        R get(D device) throws SDRplayException;
    }

    /**
     * Applies the command to the started device, converting library and API errors to RspException.
     * @param description of the command for error messages
     * @param command to apply
     * @throws RspException if the device is not started or the command fails
     */
    protected void command(String description, DeviceCommand<T> command) throws RspException
    {
        T device = getDevice();

        try
        {
            command.apply(device);
        }
        catch(SDRplayException | RuntimeException e)
        {
            throw new RspException("Unable to " + description + " - " + e.getMessage(), e);
        }
    }

    /**
     * Applies the query to the started device, converting library and API errors to RspException.
     * @param description of the query for error messages
     * @param query to apply
     * @return query result
     * @throws RspException if the device is not started or the query fails
     */
    protected <R> R query(String description, DeviceQuery<T,R> query) throws RspException
    {
        T device = getDevice();

        try
        {
            return query.get(device);
        }
        catch(SDRplayException | RuntimeException e)
        {
            throw new RspException("Unable to " + description + " - " + e.getMessage(), e);
        }
    }

    /**
     * Sets the sample rate
     * @param sampleRate enumeration value.
     * @throws RspException if the device is not started or the API rejects the sample rate
     */
    @Override
    public void setSampleRate(RspSampleRate sampleRate) throws RspException
    {
        command("set sample rate " + sampleRate, device -> {
            device.channel().setBandwidth(sampleRate.getBandwidth());
            device.setSampleRate(sampleRate.getSampleRate());
            device.channel().setDecimation(sampleRate.getDecimation(), true);
        });

        mSampleRate = sampleRate;
    }

    @Override
    public void setGain(int lna, int gr) throws RspException
    {
        int lnaLimited = Math.max(lna, 0);
        lnaLimited = Math.min(lnaLimited, getMaximumLNASetting());

        int grLimited = Math.max(gr, 20);
        grLimited = Math.min(grLimited, 59);

        if(lna != lnaLimited || gr != grLimited)
        {
            mLog.warn("Setting gain - adjusted LNA from [" + lna + "] to [" + lnaLimited + "] and GR from [" + gr + "] to [" + grLimited + "]");
        }

        final int lnaState = lnaLimited;
        final int gainReduction = grLimited;
        command("set gain", device -> device.channel().setGain(gainReduction, lnaState));
        mLNA = lnaLimited;
        mBasebandGainReduction = grLimited;
    }

    @Override
    public float getCurrentGain() throws RspException
    {
        return query("get current gain", device -> device.channel().gainValues().current());
    }

    /**
     * Registers the listener to receive gain overload notifications.
     * @param listener to register
     */
    @Override
    public void setGainOverloadListener(ITunerStatusListener listener)
    {
        if(mGainOverloadReference != null && mGainOverloadReference.get() != null)
        {
            mGainOverloadReference.clear();
        }

        mGainOverloadReference = new WeakReference<>(listener);
    }

    /**
     * Optional weakly referenced tuner status listener.
     * @return listener or null.
     */
    protected ITunerStatusListener getTunerStatusListener()
    {
        return mGainOverloadReference != null ? mGainOverloadReference.get() : null;
    }

    @Override
    public void processPowerOverload(Tuner tuner)
    {
        ITunerStatusListener listener = getTunerStatusListener();

        if(listener != null)
        {
            ThreadPool.CACHED.submit(() -> listener.notifyGainOverload(tuner));
        }
    }

    @Override
    public long getTunedFrequency() throws RspException
    {
        return query("get frequency", device -> (long)device.channel().frequency());
    }

    @Override
    public void setTunedFrequency(long frequency) throws RspException
    {
        command("set frequency " + frequency, device -> device.channel().setFrequency(frequency));

        //Notify an optional weakly referenced, registered listener that the frequency changed.
        ITunerStatusListener listener = getTunerStatusListener();

        if(listener != null)
        {
            ThreadPool.CACHED.submit(listener::frequencyUpdated);
        }
    }

    /**
     * Current sample rate setting
     */
    @Override
    public RspSampleRate getSampleRateEnumeration()
    {
        return mSampleRate;
    }

    /**
     * Current sample rate
     */
    @Override
    public double getCurrentSampleRate()
    {
        return getSampleRateEnumeration().getEffectiveSampleRate();
    }

    /**
     * Current LNA gain setting
     * @return LNA gain
     */
    @Override
    public int getLNA()
    {
        return mLNA;
    }

    /**
     * Current Baseband gain reduction setting
     * @return baseband gain reduction
     */
    @Override
    public int getBasebandGainReduction()
    {
        return mBasebandGainReduction;
    }

    /**
     * Current IF AGC mode setting.
     * @return AGC mode or null if the device is not started.
     */
    @Override
    public RspAgcMode getAgcMode()
    {
        try
        {
            return query("get AGC mode", device -> RspAgcMode.fromAgcMode(device.channel().agcMode()));
        }
        catch(RspException re)
        {
            return null;
        }
    }

    /**
     * Sets the IF AGC mode
     * @param mode to set.
     */
    @Override
    public void setAgcMode(RspAgcMode mode) throws RspException
    {
        command("set AGC mode " + mode, device -> device.channel().setAgc(mode.getAgcMode()));
    }

    /**
     * Registers listeners to receive device events and sample streams.
     * @param deviceEventListener to receive device events
     * @param sampleListener to receive sample stream.
     */
    @Override
    public void register(DeviceEventListener deviceEventListener, SampleListener sampleListener)
    {
        mDeviceEventListener = deviceEventListener;
        mSampleListener = sampleListener;
    }
}
