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

package io.github.dsheirer.source.tuner.airspy.hf;

import io.github.dsheirer.airspyhf.AirspyHf;
import io.github.dsheirer.airspyhf.AirspyHfDevice;
import io.github.dsheirer.airspyhf.AirspyHfException;
import io.github.dsheirer.airspyhf.BoardInfo;
import io.github.dsheirer.airspyhf.SampleBuffer;
import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.UsbDeviceLookup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Airspy HF+ Dual Port, HF+ Discovery and Ranger tuner controller, implemented with the jAirspyHF library
 * (libairspyhf).
 *
 * The tuner is discovered with LibUsb at a USB bus and port, like all other USB tuners, however libairspyhf owns the
 * USB device and its transfers.  The library also corrects the IQ imbalance of the zero IF sample rates and shifts the
 * signal so that the tuned frequency is at the center of the spectrum, without a DC spike, for every sample rate.
 */
public class AirspyHfTunerController extends TunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(AirspyHfTunerController.class);
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 500_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 260_000_000;
    private static final long FREQUENCY_DEFAULT = 10_000_000;
    private static final AirspyHfSampleRate DEFAULT_SAMPLE_RATE = new AirspyHfSampleRate(0, 912_000, false);
    //Sample count of each buffer from libairspyhf, used until the device is opened.
    private static final int DEFAULT_BUFFER_SAMPLE_COUNT = 4096;
    private final int mBus;
    private final String mPortAddress;
    private AirspyHfDevice mDevice;
    private BoardId mBoardId = BoardId.UNKNOWN;
    private String mSerialNumber = "UNKNOWN";
    private String mFirmwareVersion;
    private List<AirspyHfSampleRate> mAvailableSampleRates = Collections.emptyList();
    private AirspyHfSampleRate mCurrentSampleRate = DEFAULT_SAMPLE_RATE;
    private Attenuation mAttenuation = Attenuation.A0;
    private boolean mAgcEnabled;
    private boolean mLnaEnabled;
    private boolean mBiasTSupported;
    private boolean mBiasTEnabled;
    private volatile float mSamplesPerMillisecond = DEFAULT_SAMPLE_RATE.getSampleRate() / 1000.0f;
    private volatile int mBufferSampleCount = DEFAULT_BUFFER_SAMPLE_COUNT;
    private boolean mStreaming = false;

    /**
     * Constructs an instance
     * @param bus for USB
     * @param portAddress for USB
     * @param tunerErrorListener to receive tuner error notifications.
     */
    public AirspyHfTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
        setMinimumFrequency(MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(MAXIMUM_TUNABLE_FREQUENCY_HZ);
        setUsableBandwidthPercentage(.9); //90% usable after filter rolloff
        //libairspyhf offsets the LO from the tuned frequency and removes the IQ imbalance, so the center is usable.
        setMiddleUnusableHalfBandwidth(0);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.AIRSPY_HF_PLUS;
    }

    @Override
    public int getBufferSampleCount()
    {
        return mBufferSampleCount;
    }

    @Override
    public void start() throws SourceException
    {
        long serialNumber = readSerialNumber();

        getLock().lock();

        try
        {
            mBiasTSupported = false;
            mBiasTEnabled = false;
            mDevice = AirspyHf.open(serialNumber);

            BoardInfo boardInfo = mDevice.boardInfo();
            mBoardId = BoardId.fromValue((int)boardInfo.partId());
            mSerialNumber = String.format("%016X", boardInfo.serialNumber());
            mFirmwareVersion = mDevice.firmwareVersion();
            mBufferSampleCount = mDevice.outputSize();

            List<AirspyHfSampleRate> sampleRates = new ArrayList<>();
            long[] rates = mDevice.sampleRates();

            for(int x = 0; x < rates.length; x++)
            {
                mDevice.setSampleRate(rates[x]);
                sampleRates.add(new AirspyHfSampleRate(x, (int)rates[x], mDevice.isLowIf()));
            }

            if(sampleRates.isEmpty())
            {
                sampleRates.add(DEFAULT_SAMPLE_RATE);
            }

            mAvailableSampleRates = Collections.unmodifiableList(sampleRates);
            mBiasTSupported = detectBiasTSupport(mDevice);
            mBiasTEnabled = false;

            //Force the sample rate to be applied, since the rate discovery leaves the device at the last rate.
            mCurrentSampleRate = null;
            setFrequency(FREQUENCY_DEFAULT);
            setSampleRate(mAvailableSampleRates.get(0));
        }
        catch(AirspyHfException | SourceException | RuntimeException | LinkageError e)
        {
            closeDevice(mDevice);
            mDevice = null;
            throw new SourceException("Can't start Airspy HF tuner - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void stop()
    {
        AirspyHfDevice device;
        boolean biasTEnabled;

        getLock().lock();

        try
        {
            device = mDevice;
            mDevice = null;
            mStreaming = false;
            biasTEnabled = mBiasTEnabled;
            mBiasTEnabled = false;
        }
        finally
        {
            getLock().unlock();
        }

        mNativeBufferBroadcaster.clear();

        if(device != null && biasTEnabled)
        {
            try
            {
                device.setBiasTee(0);
            }
            catch(AirspyHfException | RuntimeException e)
            {
                mLog.warn("Error turning off Airspy HF bias-T before closing the device - " + e.getMessage());
            }
        }

        closeDevice(device);
    }

    /**
     * Detects if the device firmware supports a bias-T.  Firmware without bias-T support rejects the bias-T commands.
     * Some firmware (HF+ Discovery R5.0.1) rejects the bias-T count request, so this falls back to turning the bias-T
     * off, which is a safe probe since the bias-T should be off at startup anyway.
     * @param device to probe
     * @return true if the device accepts bias-T commands
     */
    private static boolean detectBiasTSupport(AirspyHfDevice device)
    {
        try
        {
            if(device.biasTeeCount() > 0)
            {
                device.setBiasTee(0);
                return true;
            }

            return false;
        }
        catch(AirspyHfException | RuntimeException e)
        {
            //Bias-T count not supported - fall through and probe with a bias-T off command.
        }

        try
        {
            device.setBiasTee(0);
            return true;
        }
        catch(AirspyHfException | RuntimeException e)
        {
            mLog.debug("Airspy HF firmware doesn't support bias-T - " + e.getMessage());
            return false;
        }
    }

    /**
     * Closes the device, which also stops streaming.
     * @param device to close, or null
     */
    private static void closeDevice(AirspyHfDevice device)
    {
        if(device != null)
        {
            try
            {
                device.close();
            }
            catch(AirspyHfException | RuntimeException e)
            {
                mLog.error("Error closing Airspy HF device", e);
            }
        }
    }

    /**
     * Reads the serial number of the device at this controller's USB bus and port, since libairspyhf opens a specific
     * device by serial number.  The USB serial string is formatted as: AIRSPYHF SN:0123456789ABCDEF
     * @return serial number
     * @throws SourceException if the device isn't found or can't be accessed.
     */
    private long readSerialNumber() throws SourceException
    {
        String serial = UsbDeviceLookup.readSerialNumber(mBus, mPortAddress);

        try
        {
            return Long.parseUnsignedLong(serial.substring(serial.lastIndexOf(':') + 1).trim(), 16);
        }
        catch(NullPointerException | NumberFormatException e)
        {
            throw new SourceException("Can't read Airspy HF serial number from USB serial string [" + serial + "]", e);
        }
    }

    /**
     * Operation that requires the device.
     */
    private interface DeviceOperation
    {
        void execute(AirspyHfDevice device) throws AirspyHfException;
    }

    /**
     * Executes the operation under the controller lock so that the device can't be closed while it is in use.  The
     * operation is ignored when the device isn't open, which allows for calls that arrive during tuner shutdown.
     * @param operation to execute
     * @throws SourceException if the operation fails
     */
    private void execute(DeviceOperation operation) throws SourceException
    {
        getLock().lock();

        try
        {
            if(mDevice != null)
            {
                operation.execute(mDevice);
            }
        }
        catch(AirspyHfException e)
        {
            throw new SourceException(e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Receives samples on the libairspyhf consumer thread and dispatches them to the buffer listeners.  Exceptions
     * are contained here since an exception reaching the library would stop the sample stream.
     * @param buffer from the library, which is only valid until this method returns.
     * @return true to continue streaming
     */
    private boolean receive(SampleBuffer buffer)
    {
        try
        {
            mBufferSampleCount = buffer.sampleCount();
            broadcast(new FloatNativeBuffer(buffer.copyFloats(), System.currentTimeMillis(), mSamplesPerMillisecond));
        }
        catch(RuntimeException e)
        {
            mLog.error("Error dispatching Airspy HF sample buffer", e);
        }

        return true;
    }

    /**
     * Starts the sample stream if it's not already started.
     */
    private void startStreaming() throws SourceException
    {
        if(!mStreaming)
        {
            execute(device -> device.startRx(this::receive));
            mStreaming = mDevice != null;
        }
    }

    /**
     * Stops the sample stream if it's started.
     */
    private void stopStreaming() throws SourceException
    {
        if(mStreaming)
        {
            mStreaming = false;
            execute(AirspyHfDevice::stopRx);
        }
    }

    /**
     * Adds the IQ buffer listener and automatically starts streaming, if not already started.
     */
    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        getLock().lock();

        try
        {
            if(mDevice != null)
            {
                super.addBufferListener(listener);
                startStreaming();
            }
        }
        catch(SourceException se)
        {
            mLog.error("Error starting streaming on Airspy HF tuner", se);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Removes the IQ buffer listener and stops streaming if there are no more listeners.
     */
    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        getLock().lock();

        try
        {
            super.removeBufferListener(listener);

            if(!hasBufferListeners())
            {
                stopStreaming();
            }
        }
        catch(SourceException se)
        {
            mLog.error("Error stopping streaming on Airspy HF tuner", se);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(config);

        if(config instanceof AirspyHfTunerConfiguration airspyConfig)
        {
            AirspyHfSampleRate sampleRate = getSampleRate(airspyConfig.getSampleRate());
            setSampleRate(sampleRate != null ? sampleRate : getAvailableSampleRates().get(0));

            try
            {
                setLna(airspyConfig.isLna());
                setAttenuation(airspyConfig.getAttenuation());
                setAgc(airspyConfig.isAgc());

                if(isBiasTSupported())
                {
                    setBiasT(airspyConfig.isBiasT());
                }
            }
            catch(SourceException se)
            {
                throw new SourceException("Couldn't apply settings from Airspy HF config", se);
            }
        }
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mFrequencyController.getTunedFrequency();
    }

    /**
     * Sets the tuned frequency.  libairspyhf applies the device's frequency calibration and the IF shift.
     * @param frequency to set
     * @throws SourceException if there is an error
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        if(MINIMUM_TUNABLE_FREQUENCY_HZ <= frequency && frequency <= MAXIMUM_TUNABLE_FREQUENCY_HZ)
        {
            execute(device -> device.setFrequency(frequency));
        }
        else
        {
            throw new SourceException("Frequency [" + frequency + "] outside of tunable range " +
                    MINIMUM_TUNABLE_FREQUENCY_HZ + "-" + MAXIMUM_TUNABLE_FREQUENCY_HZ);
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mCurrentSampleRate != null ? mCurrentSampleRate.getSampleRate() : 0;
    }

    /**
     * Access the current sample rate object.
     * @return current sample rate.
     */
    public AirspyHfSampleRate getCurrentAirspySampleRate()
    {
        return mCurrentSampleRate;
    }

    /**
     * Sets the sample rate for the tuner.  The sample stream is stopped for the change so that the library's consumer
     * thread doesn't process samples while the rate, filter gain and IF mode are changing.
     * @param sampleRate to apply.
     * @throws SourceException if the rate can't be set
     */
    public void setSampleRate(AirspyHfSampleRate sampleRate) throws SourceException
    {
        if(sampleRate == null || sampleRate.equals(mCurrentSampleRate))
        {
            return;
        }

        if(!isSupportedSampleRate(sampleRate.getSampleRate()))
        {
            throw new SourceException("Unsupported Airspy HF sample rate: " + sampleRate);
        }

        getLock().lock();

        try
        {
            boolean streaming = mStreaming;
            stopStreaming();

            try
            {
                execute(device -> device.setSampleRate(sampleRate.getSampleRate()));
                mSamplesPerMillisecond = sampleRate.getSampleRate() / 1000.0f;
                mCurrentSampleRate = sampleRate;
                mFrequencyController.setSampleRate(sampleRate.getSampleRate());
            }
            finally
            {
                if(streaming)
                {
                    startStreaming();
                }
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Indicates if the sample rate is supported.
     * @param sampleRate to check
     * @return true if the rate matches one of the available sample rates.
     */
    public boolean isSupportedSampleRate(int sampleRate)
    {
        return getSampleRate(sampleRate) != null;
    }

    /**
     * Sample rate object that matches the rate in hertz.
     * @param sampleRate in hertz
     * @return matching sample rate or null
     */
    private AirspyHfSampleRate getSampleRate(int sampleRate)
    {
        for(AirspyHfSampleRate availableRate: mAvailableSampleRates)
        {
            if(availableRate.getSampleRate() == sampleRate)
            {
                return availableRate;
            }
        }

        return null;
    }

    /**
     * Available/supported sample rates.
     * @return available sample rates
     * @throws IllegalStateException if the device has not yet been started
     */
    public List<AirspyHfSampleRate> getAvailableSampleRates()
    {
        if(mAvailableSampleRates.isEmpty())
        {
            throw new IllegalStateException("Device must be started before accessing available sample rates");
        }

        return mAvailableSampleRates;
    }

    /**
     * Serial number of the tuner.
     * @return serial number
     */
    public String getSerialNumber()
    {
        return mSerialNumber;
    }

    /**
     * Board identifier
     * @return board ID
     */
    public BoardId getBoardId()
    {
        return mBoardId;
    }

    /**
     * Firmware version string, available once the tuner is started.
     * @return firmware version or null
     */
    public String getFirmwareVersion()
    {
        return mFirmwareVersion;
    }

    /**
     * Indicates if the Automatic Gain Control (AGC) is enabled.
     * @return enabled state, true or false.
     */
    public boolean getAgc()
    {
        return mAgcEnabled;
    }

    /**
     * Sets the Automatic Gain Control (AGC).  The attenuation setting applies when the AGC is off.
     * @param enabled true to turn on AGC or false to turn off AGC
     * @throws SourceException if there is an error
     */
    public void setAgc(boolean enabled) throws SourceException
    {
        execute(device -> device.setHfAgc(enabled));
        mAgcEnabled = enabled;
    }

    /**
     * Indicates if the Low Noise Amplifier (LNA) is enabled.
     * @return enabled state of the LNA
     */
    public boolean getLna()
    {
        return mLnaEnabled;
    }

    /**
     * Sets the Low Noise Amplifier (LNA)
     * @param enabled true to turn on or false to turn off
     * @throws SourceException if there is an error
     */
    public void setLna(boolean enabled) throws SourceException
    {
        execute(device -> device.setHfLna(enabled));
        mLnaEnabled = enabled;
    }

    /**
     * Current attenuation value.
     */
    public Attenuation getAttenuation()
    {
        return mAttenuation;
    }

    /**
     * Sets the attenuation value, which applies when the AGC is off.
     * @param attenuation value to set: 0 - 8.  Increases attenuation (ie decrease gain) by 6dB with each step.
     * @throws SourceException if there is an error
     */
    public void setAttenuation(Attenuation attenuation) throws SourceException
    {
        execute(device -> device.setAttenuationStep(attenuation.getValue()));
        mAttenuation = attenuation;
    }

    /**
     * Indicates if the device firmware supports a bias-T, available once the tuner is started.
     * @return true if supported
     */
    public boolean isBiasTSupported()
    {
        return mBiasTSupported;
    }

    /**
     * Indicates if the Bias-T power output is enabled.
     * @return enabled state
     */
    public boolean getBiasT()
    {
        return mBiasTEnabled;
    }

    /**
     * Enables/disables the Bias-T (RF bias tee) power output for active antennas
     * @param enabled true to enable Bias-T, false to disable
     * @throws SourceException if the device doesn't support a bias-T or there is an error
     */
    public void setBiasT(boolean enabled) throws SourceException
    {
        if(!mBiasTSupported)
        {
            throw new SourceException("Airspy HF firmware doesn't support bias-T");
        }

        execute(device -> device.setBiasTee(enabled ? 1 : 0));
        mBiasTEnabled = enabled;
    }
}
