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

package io.github.dsheirer.source.tuner.signalhound;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.signalhound.DeviceDescriptor;
import io.github.dsheirer.signalhound.Diagnostics;
import io.github.dsheirer.signalhound.IqBlock;
import io.github.dsheirer.signalhound.IqBuffer;
import io.github.dsheirer.signalhound.IqConfig;
import io.github.dsheirer.signalhound.IqDataType;
import io.github.dsheirer.signalhound.IqStreamInfo;
import io.github.dsheirer.signalhound.SignalHound;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundDevice;
import io.github.dsheirer.signalhound.SignalHoundException;
import io.github.dsheirer.signalhound.bb.Bb60Device;
import io.github.dsheirer.signalhound.sm.SmDevice;
import io.github.dsheirer.signalhound.sp.SpDevice;
import io.github.dsheirer.signalhound.sp.SpModel;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.util.ThreadPool;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signal Hound tuner controller for the BB60, SM200/SM435 and SP145 families, implemented with the jSignalHound library.
 *
 * The Signal Hound APIs are polling interfaces: the API buffers about half a second of samples and the application
 * reads them in blocks.  A dedicated reader thread reads the blocks and dispatches them to the buffer listeners.
 *
 * The APIs can't change the center frequency, sample rate or front-end settings of a running I/Q stream, so each of
 * these changes reconfigures (aborts and restarts) the stream.  The reader thread waits while the stream is being
 * reconfigured.
 *
 * Samples are read as 32-bit floats, which the API corrects for amplitude flatness and I/Q imbalance and scales to
 * mW (power in dBm = 10 * log10(I*I + Q*Q)).  They are scaled so that a signal at the reference level is at full scale
 * (1.0), so that the reference level acts as the gain control: lowering it increases the gain.
 */
public class SignalHoundTunerController extends TunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(SignalHoundTunerController.class);
    //Widest range of all of the families.  The device's own range is applied when it's started.
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 9_000L;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 44_200_000_000L;
    public static final int AUTOMATIC = -1;
    public static final int REFERENCE_LEVEL_MINIMUM = -60;
    public static final int REFERENCE_LEVEL_MAXIMUM = 20;
    public static final int REFERENCE_LEVEL_DEFAULT = (int)IqConfig.DEFAULT_REFERENCE_LEVEL;
    private static final long FREQUENCY_DEFAULT = 101_100_000L;
    //The filter bandwidth requested from the API, relative to the sample rate.  The API reduces it to the maximum
    //bandwidth for the decimation, which is reported when the stream starts.
    private static final double REQUESTED_BANDWIDTH_RATIO = 0.9;
    private static final double USABLE_BANDWIDTH_PERCENT_DEFAULT = 0.8;
    private static final double USABLE_BANDWIDTH_PERCENT_MINIMUM = 0.5;
    //Each read is about 10 ms of samples, as a power of two
    private static final double BUFFER_SECONDS = 0.01;
    private static final int BUFFER_SAMPLES_MINIMUM = 8192;
    private static final int BUFFER_SAMPLES_MAXIMUM = 524288;
    private static final long READER_JOIN_TIMEOUT_MS = 2000;
    private static final long CONFIGURE_WAIT_MS = 250;
    private static final long STATUS_LOG_INTERVAL_NANOSECONDS = TimeUnit.SECONDS.toNanos(10);

    private final DeviceDescriptor mDescriptor;
    private final DeviceOpener mDeviceOpener;
    private final SignalHoundFamily mFamily;
    private SignalHoundDeviceInformation mDeviceInfo;
    private List<SignalHoundSampleRate> mSampleRates = List.of();
    private SignalHoundSampleRate mSampleRate;
    private long mMinimumTunableFrequency = MINIMUM_TUNABLE_FREQUENCY_HZ;
    private long mMaximumTunableFrequency = MAXIMUM_TUNABLE_FREQUENCY_HZ;
    private long mTunedFrequency = FREQUENCY_DEFAULT;
    private int mReferenceLevel = REFERENCE_LEVEL_DEFAULT;
    private int mAttenuation = AUTOMATIC;
    private int mGain = AUTOMATIC;
    private boolean mPreselector = false;
    private boolean mPreselectorSupported = false;
    //Reader thread and streaming state, guarded by the controller lock (getLock())
    private Thread mReaderThread;
    private boolean mStreaming;
    private volatile boolean mReading;

    //Lock for device calls and the I/Q stream state, which the reader thread holds while it reads each block.  It is
    //fair so that a reconfiguration isn't starved by the reader thread, which takes it again after each block.  The
    //controller lock is always taken before this lock, and the reader thread never takes the controller lock.
    private final ReentrantLock mDeviceLock = new ReentrantLock(true);
    private final Condition mStreamConfigured = mDeviceLock.newCondition();
    //Guarded by mDeviceLock
    private SignalHoundDevice mDevice;
    private boolean mIqStreaming;
    private int mBufferSamples = BUFFER_SAMPLES_MINIMUM;
    private float mSampleScale = 1.0f;
    private float mSamplesPerMillisecond;

    /**
     * Constructs an instance
     * @param descriptor of the device, from the Signal Hound API device list
     * @param tunerErrorListener to receive error notifications from this controller
     */
    public SignalHoundTunerController(DeviceDescriptor descriptor, ITunerErrorListener tunerErrorListener)
    {
        this(descriptor, tunerErrorListener, SignalHoundTunerController::openDevice);
    }

    /**
     * Constructs an instance that opens the device with an opener, for testing.
     * @param descriptor of the device
     * @param tunerErrorListener to receive error notifications from this controller
     * @param deviceOpener that opens the device
     */
    SignalHoundTunerController(DeviceDescriptor descriptor, ITunerErrorListener tunerErrorListener,
                               DeviceOpener deviceOpener)
    {
        super(tunerErrorListener);
        mDescriptor = descriptor;
        mDeviceOpener = deviceOpener;
        mFamily = SignalHoundFamily.of(descriptor.api());
        setMinimumFrequency(mFamily.getMinimumFrequency());
        setMaximumFrequency(mFamily.getMaximumFrequency());
        setMiddleUnusableHalfBandwidth(0);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENT_DEFAULT);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.SIGNAL_HOUND;
    }

    /**
     * Device family
     */
    public SignalHoundFamily getFamily()
    {
        return mFamily;
    }

    /**
     * Descriptor of the device from the Signal Hound API device list.
     */
    public DeviceDescriptor getDescriptor()
    {
        return mDescriptor;
    }

    @Override
    public int getBufferSampleCount()
    {
        return getBufferSampleCount(getSampleRate());
    }

    /**
     * Number of samples to read in each block: about 10 ms of samples, as a power of two.
     * @param sampleRate in Hertz
     */
    static int getBufferSampleCount(double sampleRate)
    {
        int samples = (int)Math.min(Integer.MAX_VALUE, sampleRate * BUFFER_SECONDS);
        return Math.clamp(Integer.highestOneBit(Math.max(samples, 1)), BUFFER_SAMPLES_MINIMUM, BUFFER_SAMPLES_MAXIMUM);
    }

    /**
     * Scale that puts a signal at the reference level at full scale, for samples in units of the square root of mW.
     * @param referenceLevel in dBm
     */
    static float getSampleScale(double referenceLevel)
    {
        return (float)Math.pow(10.0, -referenceLevel / 20.0);
    }

    /**
     * Usable bandwidth percentage from the bandwidth that the API reports for the stream.
     */
    static double getUsableBandwidthPercent(IqStreamInfo info)
    {
        if(info.sampleRate() <= 0 || info.bandwidth() <= 0)
        {
            return USABLE_BANDWIDTH_PERCENT_DEFAULT;
        }

        return Math.clamp(info.bandwidth() / info.sampleRate(), USABLE_BANDWIDTH_PERCENT_MINIMUM,
                REQUESTED_BANDWIDTH_RATIO);
    }

    @Override
    public void start() throws SourceException
    {
        getLock().lock();

        try
        {
            SignalHoundApi api = mDescriptor.api();
            SignalHoundDevice device = mDeviceOpener.open(mDescriptor);

            mDeviceLock.lock();

            try
            {
                mDevice = device;
            }
            finally
            {
                mDeviceLock.unlock();
            }

            String firmware;

            try
            {
                firmware = device.firmwareVersion();
            }
            catch(SignalHoundException | RuntimeException e)
            {
                firmware = "unknown";
                mLog.warn("Can't read Signal Hound firmware version - " + e.getMessage());
            }

            mDeviceInfo = new SignalHoundDeviceInformation(device.model(), device.serialNumber(), firmware,
                    api.libraryName(), getLibraryVersion(api), api.location().orElse("unknown"));

            mPreselectorSupported = device instanceof SmDevice ||
                    (device instanceof SpDevice sp && sp.spModel() == SpModel.SP145B);

            mMinimumTunableFrequency = mFamily.getMinimumFrequency();
            mMaximumTunableFrequency = mFamily.getMaximumFrequency(device.model());
            setFrequencyExtents(mMinimumTunableFrequency, mMaximumTunableFrequency);

            mSampleRates = SignalHoundSampleRate.getRates(mFamily);
            mSampleRate = null;
            setSampleRate(SignalHoundSampleRate.getClosest(mSampleRates, SignalHoundSampleRate.DEFAULT_RATE));
            setFrequency(FREQUENCY_DEFAULT);

            mLog.info("Signal Hound [" + mDeviceInfo.model() + "] serial [" + mDeviceInfo.serial() + "] firmware [" +
                    firmware + "] started - " + api.libraryName() + " " + mDeviceInfo.libraryVersion());
        }
        catch(SignalHoundException | RuntimeException | LinkageError e)
        {
            closeDevice();
            throw new SourceException("Can't start Signal Hound tuner - " + e.getMessage(), e);
        }
        catch(SourceException se)
        {
            closeDevice();
            throw se;
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Opens a device with the Signal Hound API.
     * @throws SourceException if the API library isn't installed
     */
    private static SignalHoundDevice openDevice(DeviceDescriptor descriptor) throws SignalHoundException, SourceException
    {
        SignalHoundApi api = descriptor.api();

        if(!api.isAvailable())
        {
            throw new SourceException("Signal Hound " + api.libraryName() + " library is not installed");
        }

        return SignalHound.open(descriptor);
    }

    /**
     * Version of the API library, or unknown when the library isn't loaded.
     */
    private static String getLibraryVersion(SignalHoundApi api)
    {
        try
        {
            return api.version();
        }
        catch(LinkageError e)
        {
            return "unknown";
        }
    }

    /**
     * Opens the Signal Hound device.
     */
    @FunctionalInterface
    interface DeviceOpener
    {
        SignalHoundDevice open(DeviceDescriptor descriptor) throws SignalHoundException, SourceException;
    }

    @Override
    public void stop()
    {
        getLock().lock();

        try
        {
            stopStream();
            closeDevice();
        }
        finally
        {
            getLock().unlock();
        }

        mNativeBufferBroadcaster.clear();
    }

    /**
     * Closes the device, which also stops any measurement.  Call while holding the controller lock, after the reader
     * thread has stopped.
     */
    private void closeDevice()
    {
        SignalHoundDevice device;

        mDeviceLock.lock();

        try
        {
            device = mDevice;
            mDevice = null;
            mIqStreaming = false;
        }
        finally
        {
            mDeviceLock.unlock();
        }

        if(device != null)
        {
            try
            {
                device.close();
            }
            catch(RuntimeException e)
            {
                mLog.error("Error closing Signal Hound device", e);
            }
        }
    }

    @Override
    public void apply(TunerConfiguration tunerConfiguration) throws SourceException
    {
        if(!(tunerConfiguration instanceof SignalHoundTunerConfiguration config))
        {
            throw new IllegalArgumentException("Invalid tuner config:" + tunerConfiguration.getClass());
        }

        //Saved limits can be wider than this device, since the defaults cover all of the families
        config.setMinimumFrequency(Math.max(config.getMinimumFrequency(), mMinimumTunableFrequency));
        config.setMaximumFrequency(Math.min(config.getMaximumFrequency() > 0 ? config.getMaximumFrequency() :
                mMaximumTunableFrequency, mMaximumTunableFrequency));

        SignalHoundSampleRate rate = SignalHoundSampleRate.getClosest(mSampleRates, config.getSampleRate());
        setSampleRate(rate);
        config.setSampleRate(rate.rate());

        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(config);

        int referenceLevel = Math.clamp(config.getReferenceLevel(), REFERENCE_LEVEL_MINIMUM, REFERENCE_LEVEL_MAXIMUM);
        setReferenceLevel(referenceLevel);
        config.setReferenceLevel(referenceLevel);

        int attenuation = getValidSetting(config.getAttenuation(), mFamily.getMaximumAttenuation());
        setAttenuation(attenuation);
        config.setAttenuation(attenuation);

        int gain = mFamily.hasGain() ? getValidSetting(config.getGain(), mFamily.getMaximumGain()) : AUTOMATIC;
        setGain(gain);
        config.setGain(gain);

        boolean preselector = mPreselectorSupported && config.isPreselector();
        setPreselector(preselector);
        config.setPreselector(preselector);
    }

    /**
     * Setting value when it's automatic or within 0 and the maximum, otherwise automatic.
     */
    static int getValidSetting(int value, int maximum)
    {
        return value >= 0 && value <= maximum ? value : AUTOMATIC;
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mTunedFrequency;
    }

    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        getLock().lock();

        try
        {
            long previous = mTunedFrequency;
            mTunedFrequency = frequency;

            if(frequency != previous)
            {
                reconfigure("tune to [" + frequency + "]");
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mSampleRate != null ? mSampleRate.rate() : 0;
    }

    /**
     * Sample rates of the device family.
     */
    public List<SignalHoundSampleRate> getSampleRates()
    {
        return mSampleRates;
    }

    /**
     * Current sample rate, or null before the tuner is started.
     */
    public SignalHoundSampleRate getSignalHoundSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets the sample rate.
     * @param rate to set
     * @throws SourceException if the sample rate is locked or can't be set
     */
    public void setSampleRate(SignalHoundSampleRate rate) throws SourceException
    {
        getLock().lock();

        try
        {
            if(!rate.equals(mSampleRate))
            {
                if(isLockedSampleRate())
                {
                    throw new SourceException("Can't change Signal Hound sample rate while channels are allocated");
                }

                mSampleRate = rate;
                reconfigure("set sample rate to [" + rate + "]");
                mFrequencyController.setSampleRate(rate.rate());
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Lowest frequency that the device can tune.
     */
    public long getMinimumTunableFrequency()
    {
        return mMinimumTunableFrequency;
    }

    /**
     * Highest frequency that the device can tune.
     */
    public long getMaximumTunableFrequency()
    {
        return mMaximumTunableFrequency;
    }

    /**
     * Reference level in dBm.
     */
    public int getReferenceLevel()
    {
        return mReferenceLevel;
    }

    /**
     * Sets the reference level, which is the strongest expected input level.  When the attenuation (and the gain for
     * the BB60) is automatic, the API chooses them from the reference level.
     * @param referenceLevel in dBm, -60 to 20
     * @throws SourceException if the stream can't be reconfigured
     */
    public void setReferenceLevel(int referenceLevel) throws SourceException
    {
        if(referenceLevel < REFERENCE_LEVEL_MINIMUM || referenceLevel > REFERENCE_LEVEL_MAXIMUM)
        {
            throw new IllegalArgumentException("Reference level must be " + REFERENCE_LEVEL_MINIMUM + " to " +
                    REFERENCE_LEVEL_MAXIMUM + " dBm: " + referenceLevel);
        }

        getLock().lock();

        try
        {
            if(referenceLevel != mReferenceLevel)
            {
                mReferenceLevel = referenceLevel;
                reconfigure("set reference level to [" + referenceLevel + " dBm]");
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Attenuator setting, or {@link #AUTOMATIC}.
     */
    public int getAttenuation()
    {
        return mAttenuation;
    }

    /**
     * Sets the attenuator.
     * @param attenuation 0 to the family's maximum, in steps of the family's attenuation step, or {@link #AUTOMATIC}
     * @throws SourceException if the stream can't be reconfigured
     */
    public void setAttenuation(int attenuation) throws SourceException
    {
        if(getValidSetting(attenuation, mFamily.getMaximumAttenuation()) != attenuation)
        {
            throw new IllegalArgumentException("Invalid " + mFamily + " attenuation setting: " + attenuation);
        }

        getLock().lock();

        try
        {
            if(attenuation != mAttenuation)
            {
                mAttenuation = attenuation;
                reconfigure("set attenuation to [" + attenuation + "]");
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Gain setting (BB60 only), or {@link #AUTOMATIC}.
     */
    public int getGain()
    {
        return mGain;
    }

    /**
     * Sets the gain of a BB60.  The BB60 uses the manual gain and attenuation only when both are set, otherwise it
     * chooses both from the reference level.
     * @param gain 0 to 3, or {@link #AUTOMATIC}
     * @throws SourceException if the stream can't be reconfigured
     */
    public void setGain(int gain) throws SourceException
    {
        if(gain != AUTOMATIC && (!mFamily.hasGain() || getValidSetting(gain, mFamily.getMaximumGain()) != gain))
        {
            throw new IllegalArgumentException("Invalid " + mFamily + " gain setting: " + gain);
        }

        getLock().lock();

        try
        {
            if(gain != mGain)
            {
                mGain = gain;
                reconfigure("set gain to [" + gain + "]");
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Indicates if the device has an RF preselector: the SM200/SM435, and the SP145B.
     */
    public boolean isPreselectorSupported()
    {
        return mPreselectorSupported;
    }

    /**
     * Indicates if the RF preselector is enabled.
     */
    public boolean isPreselector()
    {
        return mPreselector;
    }

    /**
     * Enables or disables the RF preselector, which filters the input up to 645 MHz (SM200/SM435) or 520 MHz (SP145B).
     * @param enabled true to enable
     * @throws SourceException if the device doesn't have a preselector or the stream can't be reconfigured
     */
    public void setPreselector(boolean enabled) throws SourceException
    {
        getLock().lock();

        try
        {
            if(enabled != mPreselector)
            {
                if(enabled && !mPreselectorSupported)
                {
                    throw new SourceException("This Signal Hound device doesn't have an RF preselector");
                }

                mPreselector = enabled;
                reconfigure((enabled ? "enable" : "disable") + " preselector");
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Device information, available once the tuner has been started.
     */
    public SignalHoundDeviceInformation getDeviceInfo()
    {
        return mDeviceInfo;
    }

    /**
     * Reads the device's temperature, voltage and current sensors, for display.
     * @return readings, or null if they can't be read
     */
    public Diagnostics getDiagnostics()
    {
        mDeviceLock.lock();

        try
        {
            return mDevice != null ? mDevice.diagnostics() : null;
        }
        catch(SignalHoundException | RuntimeException e)
        {
            mLog.warn("Can't read Signal Hound diagnostics - " + e.getMessage());
            return null;
        }
        finally
        {
            mDeviceLock.unlock();
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
            if(hasDevice())
            {
                super.addBufferListener(listener);

                if(!mStreaming)
                {
                    startStream();
                }
            }
        }
        catch(SourceException se)
        {
            mLog.error("Error starting streaming on Signal Hound tuner", se);
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
                stopStream();
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Indicates if the device is open.
     */
    private boolean hasDevice()
    {
        mDeviceLock.lock();

        try
        {
            return mDevice != null;
        }
        finally
        {
            mDeviceLock.unlock();
        }
    }

    /**
     * Configures and starts the I/Q stream and the reader thread.  Call while holding the controller lock.
     */
    private void startStream() throws SourceException
    {
        configureStream();
        mStreaming = true;
        mReading = true;
        mReaderThread = new Thread(this::read, "sdrtrunk signal hound " + mDescriptor.serialNumber());
        mReaderThread.setDaemon(true);
        mReaderThread.start();
    }

    /**
     * Stops the reader thread and the I/Q stream.  Call while holding the controller lock.
     */
    private void stopStream()
    {
        mStreaming = false;
        mReading = false;
        Thread reader = mReaderThread;
        mReaderThread = null;

        mDeviceLock.lock();

        try
        {
            mStreamConfigured.signalAll();
        }
        finally
        {
            mDeviceLock.unlock();
        }

        if(reader != null && reader != Thread.currentThread())
        {
            try
            {
                //The reader finishes the block that it's reading, about 10 ms of samples
                reader.join(READER_JOIN_TIMEOUT_MS);

                if(reader.isAlive())
                {
                    mLog.warn("Signal Hound reader thread didn't stop within " + READER_JOIN_TIMEOUT_MS + " ms");
                }
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        mDeviceLock.lock();

        try
        {
            if(mDevice != null && mIqStreaming)
            {
                mIqStreaming = false;
                mDevice.stop();
            }
        }
        catch(SignalHoundException | RuntimeException e)
        {
            mLog.error("Error stopping Signal Hound I/Q stream - " + e.getMessage());
        }
        finally
        {
            mDeviceLock.unlock();
        }
    }

    /**
     * Reconfigures the I/Q stream after a settings change, when it's running.  Otherwise the settings are applied when
     * the stream starts.  Call while holding the controller lock.
     * @param change description for the error message
     * @throws SourceException if the stream can't be reconfigured, which leaves it stopped
     */
    private void reconfigure(String change) throws SourceException
    {
        if(mStreaming)
        {
            try
            {
                configureStream();
            }
            catch(SourceException se)
            {
                throw new SourceException("Can't " + change + " - " + se.getMessage(), se);
            }
        }
    }

    /**
     * Configures and starts the I/Q stream with the current settings, stopping it first when it's running.  Call while
     * holding the controller lock.
     * @throws SourceException if the device isn't open or rejects the settings, which leaves the stream stopped
     */
    private void configureStream() throws SourceException
    {
        if(mSampleRate == null)
        {
            throw new SourceException("Signal Hound tuner isn't started");
        }

        IqConfig config = new IqConfig(mTunedFrequency, mSampleRate.decimation(),
                mSampleRate.rate() * REQUESTED_BANDWIDTH_RATIO, IqDataType.FLOAT32, mReferenceLevel);
        IqStreamInfo info;

        mDeviceLock.lock();

        try
        {
            if(mDevice == null)
            {
                throw new SourceException("Signal Hound device isn't open");
            }

            mIqStreaming = false;
            applyFrontEnd(mDevice);
            info = mDevice.startIq(config);
            mSampleScale = getSampleScale(mReferenceLevel);
            mBufferSamples = getBufferSampleCount(info.sampleRate());
            mSamplesPerMillisecond = (float)(info.sampleRate() / 1000.0);
            mIqStreaming = true;
            mStreamConfigured.signalAll();
        }
        catch(SignalHoundException | RuntimeException e)
        {
            throw new SourceException("Signal Hound rejected I/Q stream settings " + config + " - " + e.getMessage(), e);
        }
        finally
        {
            mDeviceLock.unlock();
        }

        if(Math.abs(info.sampleRate() - mSampleRate.rate()) > 1.0)
        {
            mLog.warn("Signal Hound I/Q sample rate is [" + info.sampleRate() + "] instead of [" +
                    mSampleRate.rate() + "]");
        }

        setUsableBandwidthPercentage(getUsableBandwidthPercent(info));
    }

    /**
     * Applies the attenuation, gain and preselector settings, which the device uses when the stream is configured.
     * Call while holding the device lock.
     */
    private void applyFrontEnd(SignalHoundDevice device) throws SignalHoundException
    {
        switch(device)
        {
            case Bb60Device bb -> bb.setGainAttenuation(mGain, mAttenuation);
            case SmDevice sm ->
            {
                sm.setAttenuation(mAttenuation);
                sm.setPreselector(mPreselector);
            }
            case SpDevice sp ->
            {
                sp.setAttenuation(mAttenuation);

                if(mPreselectorSupported)
                {
                    try
                    {
                        sp.setPreselector(mPreselector);
                    }
                    catch(UnsupportedOperationException e)
                    {
                        //The Linux x86-64 and macOS builds of sp_api don't have the preselector functions
                        mLog.warn("Signal Hound sp_api library doesn't support the SP145B preselector on this platform");
                        mPreselectorSupported = false;
                        mPreselector = false;
                    }
                }
            }
            default -> {}
        }
    }

    /**
     * Reads blocks of samples on the reader thread and dispatches them to the buffer listeners, until the stream is
     * stopped or a read fails.
     */
    private void read()
    {
        IqBuffer buffer = null;
        String failure = null;
        long lostBlocks = 0;
        long warnings = 0;
        String lastWarning = null;
        long lastReport = System.nanoTime();

        try
        {
            while(mReading)
            {
                float[] samples;
                float scale;
                float samplesPerMillisecond;
                IqBlock block;

                mDeviceLock.lock();

                try
                {
                    while(mReading && (!mIqStreaming || mDevice == null))
                    {
                        mStreamConfigured.await(CONFIGURE_WAIT_MS, TimeUnit.MILLISECONDS);
                    }

                    if(!mReading)
                    {
                        break;
                    }

                    if(buffer == null || buffer.capacity() != mBufferSamples)
                    {
                        if(buffer != null)
                        {
                            buffer.close();
                        }

                        buffer = IqBuffer.allocate(IqDataType.FLOAT32, mBufferSamples);
                    }

                    block = mDevice.readIq(buffer, false);
                    samples = new float[buffer.capacity() * 2];
                    buffer.copyFloats(samples);
                    scale = mSampleScale;
                    samplesPerMillisecond = mSamplesPerMillisecond;
                }
                finally
                {
                    mDeviceLock.unlock();
                }

                scale(samples, scale);

                if(block.sampleLoss())
                {
                    lostBlocks++;
                }

                if(block.warning() != null)
                {
                    warnings++;
                    lastWarning = block.warning();
                }

                long now = System.nanoTime();

                if(now - lastReport > STATUS_LOG_INTERVAL_NANOSECONDS)
                {
                    if(lostBlocks > 0)
                    {
                        mLog.warn("Signal Hound dropped samples before [" + lostBlocks + "] sample blocks - the " +
                                "computer isn't keeping up with the sample rate");
                    }

                    if(warnings > 0)
                    {
                        mLog.warn("Signal Hound reported [" + warnings + "] warnings, most recently [" + lastWarning +
                                "] - an ADC overflow means that the reference level is too low for the input signals");
                    }

                    lostBlocks = 0;
                    warnings = 0;
                    lastReport = now;
                }

                long timestamp = block.timestampNanos() > 0 ? block.timestampNanos() / 1_000_000L :
                        System.currentTimeMillis();
                broadcast(new FloatNativeBuffer(samples, timestamp, samplesPerMillisecond));
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch(SignalHoundException | RuntimeException e)
        {
            //A read that fails because the stream is being stopped isn't an error
            if(mReading)
            {
                failure = e.getMessage();
            }
        }
        finally
        {
            if(buffer != null)
            {
                buffer.close();
            }
        }

        if(failure != null)
        {
            mReading = false;
            String message = "Signal Hound sample stream stopped - " + failure;
            mLog.error(message);
            //Stopping the tuner joins this thread, so it's done on another thread
            ThreadPool.CACHED.execute(() -> setErrorMessage(message));
        }
    }

    /**
     * Scales the samples in place.
     * @param samples interleaved complex samples
     * @param scale to apply
     */
    static void scale(float[] samples, float scale)
    {
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] *= scale;
        }
    }
}
