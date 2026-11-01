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

package io.github.dsheirer.source.tuner.bladerf;

import io.github.dsheirer.bladerf.BladeRf;
import io.github.dsheirer.bladerf.BladeRfDevice;
import io.github.dsheirer.bladerf.BladeRfError;
import io.github.dsheirer.bladerf.BladeRfException;
import io.github.dsheirer.bladerf.BladeRfStream;
import io.github.dsheirer.bladerf.Channel;
import io.github.dsheirer.bladerf.ChannelLayout;
import io.github.dsheirer.bladerf.DeviceInfo;
import io.github.dsheirer.bladerf.DeviceSpeed;
import io.github.dsheirer.bladerf.Feature;
import io.github.dsheirer.bladerf.GainMode;
import io.github.dsheirer.bladerf.GainModeInfo;
import io.github.dsheirer.bladerf.Range;
import io.github.dsheirer.bladerf.SampleBuffer;
import io.github.dsheirer.bladerf.SampleFormat;
import io.github.dsheirer.bladerf.StreamConfig;
import io.github.dsheirer.bladerf.Version;
import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.SignedByteNativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;

/**
 * bladeRF tuner controller, implemented with the jBladeRF library (libbladeRF).
 *
 * The tuner is discovered with LibUsb at a USB bus and port, like all other USB tuners, however libbladeRF owns the USB
 * device and its transfers.
 *
 * Only the first receive channel (RX1 connector) is used.  The bladeRF 2.0 has a second receive channel, but it shares
 * the local oscillator and sample rate with the first, so it couldn't be tuned as an independent tuner.
 *
 * Sample rates above 61.44 MHz use the AD9361 oversample feature with 8-bit samples.  These are only offered with FPGA
 * version 0.15.0 or later, because starting an 8-bit sample stream on an older FPGA crashes in libbladeRF.
 *
 * The device is reopened when changing between oversampled and normal sample rates.  Disabling the oversample feature
 * doesn't fully restore the RFIC.  The device reports the correct settings, but streams at the wrong rate or times out.
 */
public class BladeRfTunerController extends TunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(BladeRfTunerController.class);
    //The bladeRF 2.0 range.  The device reports its own range when it's started.
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 70_000_000L;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 6_000_000_000L;
    public static final GainMode GAIN_MODE_DEFAULT = GainMode.MANUAL;
    public static final int GAIN_DEFAULT = 30;
    private static final long FREQUENCY_DEFAULT = 101_100_000L;
    private static final Channel CHANNEL = Channel.RX0;
    private static final float SAMPLE_SCALE = 1.0f / 2048.0f;
    private static final SampleFormat SAMPLE_FORMAT = SampleFormat.SC16_Q11;
    private static final SampleFormat OVERSAMPLE_SAMPLE_FORMAT = SampleFormat.SC8_Q7;
    private static final int OVERSAMPLE_MINIMUM_FPGA_MAJOR = 0;
    private static final int OVERSAMPLE_MINIMUM_FPGA_MINOR = 15;
    private static final int BUFFER_SAMPLE_COUNT = 65536;
    private static final StreamConfig STREAM_CONFIG = new StreamConfig(32, BUFFER_SAMPLE_COUNT, 16);
    private static final StreamConfig OVERSAMPLE_STREAM_CONFIG = new StreamConfig(64, BUFFER_SAMPLE_COUNT, 32);
    private static final long USB2_MAXIMUM_BYTES_PER_SECOND = 32_000_000L;
    private static final double MAXIMUM_USABLE_BANDWIDTH_PERCENT = 0.90;
    //Measured with a carrier swept across the band at 122.88 MHz.  The analog filter is 3 dB down at about +/- 33 MHz.
    private static final double OVERSAMPLE_USABLE_BANDWIDTH_HZ = 66_000_000.0;
    private static final long STREAM_CHECK_INTERVAL_SECONDS = 1;

    private final int mBus;
    private final String mPortAddress;
    private BladeRfDevice mDevice;
    private BladeRfDeviceInformation mDeviceInfo;
    private BladeRfStream mStream;
    private ScheduledFuture<?> mStreamCheck;
    private List<BladeRfSampleRate> mSampleRates = List.of();
    private BladeRfSampleRate mSampleRate;
    private boolean mOversampled;
    private List<GainModeInfo> mGainModes = List.of();
    private Range mGainRange;
    private long mMinimumTunableFrequency = MINIMUM_TUNABLE_FREQUENCY_HZ;
    private long mMaximumTunableFrequency = MAXIMUM_TUNABLE_FREQUENCY_HZ;
    private GainMode mGainMode = GAIN_MODE_DEFAULT;
    private int mGain = GAIN_DEFAULT;
    private boolean mBiasTSupported;
    private boolean mBiasT;
    private volatile float mSamplesPerMillisecond = 0.0f;

    /**
     * Constructs an instance
     * @param bus usb
     * @param portAddress usb
     * @param tunerErrorListener to receive error notifications from this controller
     */
    public BladeRfTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
        setMinimumFrequency(MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(MAXIMUM_TUNABLE_FREQUENCY_HZ);
        setMiddleUnusableHalfBandwidth(0);
        setUsableBandwidthPercentage(MAXIMUM_USABLE_BANDWIDTH_PERCENT);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.BLADERF;
    }

    @Override
    public int getBufferSampleCount()
    {
        return BUFFER_SAMPLE_COUNT;
    }

    @Override
    public void start() throws SourceException
    {
        getLock().lock();

        try
        {
            mDevice = BladeRf.open(findDevice());
            mSampleRate = null;
            mOversampled = false;

            if(!mDevice.isFpgaConfigured())
            {
                throw new SourceException("bladeRF FPGA isn't loaded - use bladeRF-cli to flash an FPGA image to the device");
            }

            mBiasTSupported = isBiasTSupported(mDevice);
            mDeviceInfo = new BladeRfDeviceInformation(mDevice.serial(), mDevice.boardName(),
                    mDevice.firmwareVersion().toString(), mDevice.fpgaVersion().toString(), mDevice.speed().name());
            mSampleRates = BladeRfSampleRate.getRates(getMaximumSampleRate(mDevice));
            Range frequencies = mDevice.frequencyRange(CHANNEL);
            mMinimumTunableFrequency = frequencies.min();
            mMaximumTunableFrequency = frequencies.max();
            setFrequencyExtents(mMinimumTunableFrequency, mMaximumTunableFrequency);
            mGainModes = mDevice.gainModes(CHANNEL);

            setSampleRate(BladeRfSampleRate.getClosest(mSampleRates, BladeRfSampleRate.DEFAULT.getRate()));
            setFrequency(FREQUENCY_DEFAULT);
            setGainMode(GAIN_MODE_DEFAULT);
            setGain(GAIN_DEFAULT);
            mGainRange = mDevice.gainRange(CHANNEL);
        }
        catch(BladeRfException | LinkageError e)
        {
            closeDevice(mDevice, false);
            mDevice = null;
            throw new SourceException("Can't start bladeRF tuner - " + e.getMessage(), e);
        }
        catch(SourceException se)
        {
            closeDevice(mDevice, false);
            mDevice = null;
            throw se;
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void stop()
    {
        BladeRfDevice device;
        BladeRfStream stream;
        boolean biasT;

        getLock().lock();

        try
        {
            device = mDevice;
            mDevice = null;
            stream = clearStream();
            biasT = mBiasT;
            mBiasT = false;
        }
        finally
        {
            getLock().unlock();
        }

        mNativeBufferBroadcaster.clear();

        if(stream != null)
        {
            stream.stop();
        }

        closeDevice(device, biasT);
    }

    /**
     * Closes the device.  The bias-T stays on after the device is closed, so it has to be turned off first.
     * @param device to close, or null
     * @param biasT true if the bias-T is on
     */
    private static void closeDevice(BladeRfDevice device, boolean biasT)
    {
        if(device != null)
        {
            try
            {
                if(biasT)
                {
                    device.setBiasTee(CHANNEL, false);
                }
            }
            catch(BladeRfException | RuntimeException e)
            {
                mLog.error("Error turning off bladeRF bias-T", e);
            }

            try
            {
                device.close();
            }
            catch(RuntimeException e)
            {
                mLog.error("Error closing bladeRF device", e);
            }
        }
    }

    /**
     * Probes for bias-T support, since only the bladeRF 2.0 has a bias-T.
     */
    private static boolean isBiasTSupported(BladeRfDevice device) throws BladeRfException
    {
        try
        {
            device.biasTee(CHANNEL);
            return true;
        }
        catch(BladeRfException e)
        {
            if(e.error() == BladeRfError.UNSUPPORTED)
            {
                return false;
            }

            throw e;
        }
    }

    /**
     * Highest sample rate supported by the hardware and the USB connection.
     */
    private static long getMaximumSampleRate(BladeRfDevice device) throws BladeRfException
    {
        long maximum = device.sampleRateRange(CHANNEL).max();

        if(device.speed() != DeviceSpeed.SUPER)
        {
            return Math.min(maximum, USB2_MAXIMUM_BYTES_PER_SECOND / SAMPLE_FORMAT.bytesPerSample());
        }

        if(isOversampleSupported(device.fpgaVersion()))
        {
            maximum = Math.max(maximum, getOversampleMaximumSampleRate(device));
        }
        else
        {
            mLog.info("bladeRF sample rates limited to [" + maximum + "] Hz - FPGA version [" + device.fpgaVersion() +
                    "] doesn't support 8-bit samples.  Upgrade to FPGA version 0.15.0 or later for higher sample rates.");
        }

        return maximum;
    }

    /**
     * Indicates if the FPGA version supports the 8-bit samples that are required for the oversample feature.
     * @param fpga version
     * @return true if supported
     */
    static boolean isOversampleSupported(Version fpga)
    {
        return fpga.major() > OVERSAMPLE_MINIMUM_FPGA_MAJOR ||
                (fpga.major() == OVERSAMPLE_MINIMUM_FPGA_MAJOR && fpga.minor() >= OVERSAMPLE_MINIMUM_FPGA_MINOR);
    }

    /**
     * Usable portion of an oversampled rate, which is limited by the analog filter that stays in the signal path.
     * @param rate that is oversampled
     * @return usable bandwidth percentage
     */
    static double getOversampleUsableBandwidthPercentage(BladeRfSampleRate rate)
    {
        return Math.min(MAXIMUM_USABLE_BANDWIDTH_PERCENT, OVERSAMPLE_USABLE_BANDWIDTH_HZ / rate.getRate());
    }

    /**
     * Highest sample rate that the device reports while the oversample feature is enabled.
     * @return maximum rate, or zero if the device doesn't have the oversample feature, such as the bladeRF 1.0.
     */
    private static long getOversampleMaximumSampleRate(BladeRfDevice device) throws BladeRfException
    {
        try
        {
            device.enableFeature(Feature.OVERSAMPLE, true);
        }
        catch(BladeRfException e)
        {
            mLog.info("bladeRF oversample feature isn't available - " + e.getMessage());
            return 0;
        }

        try
        {
            return device.sampleRateRange(CHANNEL).max();
        }
        finally
        {
            device.enableFeature(Feature.DEFAULT, true);
        }
    }

    /**
     * Finds the libbladeRF description of the device at this controller's USB bus and port.
     */
    private DeviceInfo findDevice() throws SourceException, BladeRfException
    {
        int address = readUsbAddress();

        for(DeviceInfo info : BladeRf.listDevices())
        {
            if(info.usbBus() == mBus && info.usbAddress() == address)
            {
                return info;
            }
        }

        throw new SourceException("bladeRF library can't access the USB device at bus [" + mBus + "] port [" +
                mPortAddress + "] - if using linux, check the udev rules.  If using windows, install the WinUSB driver.");
    }

    /**
     * Reads the current USB address of the device at this controller's USB bus and port.  The address changes each time
     * that the device is plugged in.
     */
    private int readUsbAddress() throws SourceException
    {
        Context context = new Context();
        int status = LibUsb.init(context);

        if(status != LibUsb.SUCCESS)
        {
            throw new SourceException("Can't initialize libusb library - " + LibUsb.errorName(status));
        }

        try
        {
            DeviceList deviceList = new DeviceList();
            int count = LibUsb.getDeviceList(context, deviceList);

            if(count < 0)
            {
                throw new SourceException("Can't get the USB device list - " + LibUsb.errorName(count));
            }

            try
            {
                for(Device device : deviceList)
                {
                    if(LibUsb.getBusNumber(device) == mBus && LibUsb.getPortNumber(device) > 0 &&
                        mPortAddress != null && mPortAddress.equals(TunerManager.getPortAddress(device)))
                    {
                        return LibUsb.getDeviceAddress(device);
                    }
                }
            }
            finally
            {
                LibUsb.freeDeviceList(deviceList, true);
            }
        }
        finally
        {
            LibUsb.exit(context);
        }

        throw new SourceException("Couldn't find USB device at bus [" + mBus + "] port [" + mPortAddress + "]");
    }

    @Override
    public void apply(TunerConfiguration tunerConfiguration) throws SourceException
    {
        if(!(tunerConfiguration instanceof BladeRfTunerConfiguration config))
        {
            throw new IllegalArgumentException("Invalid tuner config:" + tunerConfiguration.getClass());
        }

        //Saved limits can be wider than this device, since the defaults are for the bladeRF 2.0.
        config.setMinimumFrequency(Math.max(config.getMinimumFrequency(), mMinimumTunableFrequency));
        config.setMaximumFrequency(Math.min(config.getMaximumFrequency() > 0 ? config.getMaximumFrequency() :
                mMaximumTunableFrequency, mMaximumTunableFrequency));

        BladeRfSampleRate rate = BladeRfSampleRate.getClosest(mSampleRates, config.getSampleRate());
        setSampleRate(rate);
        config.setSampleRate(rate.getRate());

        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(config);
        setBiasT(config.isBiasT());
        setGain(config.getGain());
        setGainMode(isSupported(config.getGainMode()) ? config.getGainMode() : GAIN_MODE_DEFAULT);
    }

    private boolean isSupported(GainMode mode)
    {
        return mGainModes.stream().anyMatch(info -> info.mode() == mode);
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mFrequencyController.getTunedFrequency();
    }

    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        getLock().lock();

        try
        {
            if(mDevice != null)
            {
                mDevice.setFrequency(CHANNEL, frequency);
            }
        }
        catch(BladeRfException e)
        {
            throw new SourceException("Can't tune bladeRF to [" + frequency + "] - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mSampleRate != null ? mSampleRate.getRate() : 0;
    }

    /**
     * Sample rates supported by the device across its current USB connection.
     */
    public List<BladeRfSampleRate> getSampleRates()
    {
        return mSampleRates;
    }

    /**
     * Current sample rate.
     */
    public BladeRfSampleRate getBladeRfSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets the sample rate and the matching analog filter bandwidth.  The sample stream is stopped for the change.
     * @param rate to set
     * @throws SourceException if the sample rate is locked or can't be set
     */
    public void setSampleRate(BladeRfSampleRate rate) throws SourceException
    {
        getLock().lock();

        try
        {
            if(rate != mSampleRate && mDevice != null)
            {
                if(isLockedSampleRate())
                {
                    throw new SourceException("Can't change bladeRF sample rate while channels are allocated");
                }

                boolean streaming = mStream != null;
                BladeRfStream stream = clearStream();

                if(stream != null)
                {
                    stream.stop();
                }

                try
                {
                    if(rate.isOversampled() != mOversampled)
                    {
                        reopenDevice();

                        if(rate.isOversampled())
                        {
                            //Bandwidth has to be set first since it can't be reliably set with oversample enabled.
                            mDevice.setBandwidth(CHANNEL, mDevice.bandwidthRange(CHANNEL).max());
                            mDevice.enableFeature(Feature.OVERSAMPLE, true);
                            mOversampled = true;
                        }
                    }

                    mDevice.setSampleRate(CHANNEL, rate.getRate());

                    if(rate.isOversampled())
                    {
                        setUsableBandwidthPercentage(getOversampleUsableBandwidthPercentage(rate));
                    }
                    else
                    {
                        long requested = (long)(rate.getRate() * MAXIMUM_USABLE_BANDWIDTH_PERCENT);
                        long bandwidth = mDevice.setBandwidth(CHANNEL, requested);
                        setUsableBandwidthPercentage(Math.min(MAXIMUM_USABLE_BANDWIDTH_PERCENT,
                                (double)bandwidth / rate.getRate()));
                    }

                    mSampleRate = rate;
                    mSamplesPerMillisecond = rate.getRate() / 1000.0f;
                    mFrequencyController.setSampleRate(rate.getRate());
                }
                catch(BladeRfException e)
                {
                    throw new SourceException("Can't set bladeRF sample rate to [" + rate + "] - " + e.getMessage(), e);
                }
                finally
                {
                    if(streaming && mDevice != null)
                    {
                        startStream();
                    }
                }
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Closes and reopens the device and restores the settings that were reset, except for the sample rate and bandwidth
     * which the caller sets.  The sample stream must be stopped.
     */
    private void reopenDevice() throws SourceException, BladeRfException
    {
        long frequency = mFrequencyController.getTunedFrequency();
        BladeRfDevice device = mDevice;
        mDevice = null;
        mOversampled = false;
        closeDevice(device, false);

        mDevice = BladeRf.open(findDevice());
        mDevice.setFrequency(CHANNEL, frequency);
        mDevice.setGainMode(CHANNEL, mGainMode);

        if(mGainMode == GainMode.MANUAL)
        {
            mDevice.setGain(CHANNEL, mGain);
        }

        if(mBiasT)
        {
            mDevice.setBiasTee(CHANNEL, true);
        }
    }

    /**
     * Gain modes supported by the device.
     */
    public List<GainModeInfo> getGainModes()
    {
        return mGainModes;
    }

    /**
     * Range of gain values, available once the tuner has been started.
     */
    public Range getGainRange()
    {
        return mGainRange;
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
     * Sets the gain mode.
     * @param mode to set
     * @throws SourceException if the mode can't be set
     */
    public void setGainMode(GainMode mode) throws SourceException
    {
        getLock().lock();

        try
        {
            if(mDevice != null)
            {
                mDevice.setGainMode(CHANNEL, mode);

                if(mode == GainMode.MANUAL)
                {
                    mDevice.setGain(CHANNEL, mGain);
                }
            }

            mGainMode = mode;
        }
        catch(BladeRfException e)
        {
            throw new SourceException("Can't set bladeRF gain mode to [" + mode + "] - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Sets the gain, which is used when the gain mode is manual.
     * @param gain in dB
     * @throws SourceException if the gain can't be set
     */
    public void setGain(int gain) throws SourceException
    {
        getLock().lock();

        try
        {
            mGain = gain;

            if(mDevice != null && mGainMode == GainMode.MANUAL)
            {
                mDevice.setGain(CHANNEL, gain);
            }
        }
        catch(BladeRfException e)
        {
            throw new SourceException("Can't set bladeRF gain to [" + gain + "] - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Indicates if the device has a bias-T.
     */
    public boolean isBiasTSupported()
    {
        return mBiasTSupported;
    }

    /**
     * Turns the bias-T on or off.  On the bladeRF 2.0 this switches both receive connectors.
     * @param enabled true to turn on
     * @throws SourceException if the bias-T can't be set
     */
    public void setBiasT(boolean enabled) throws SourceException
    {
        getLock().lock();

        try
        {
            if(mDevice != null && mBiasTSupported)
            {
                mDevice.setBiasTee(CHANNEL, enabled);
                mBiasT = enabled;
            }
        }
        catch(BladeRfException e)
        {
            throw new SourceException("Can't set bladeRF bias-T - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Device information, available once the tuner has been started.
     */
    public BladeRfDeviceInformation getDeviceInfo()
    {
        return mDeviceInfo;
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

                if(mStream == null)
                {
                    startStream();
                }
            }
        }
        catch(SourceException se)
        {
            mLog.error("Error starting streaming on bladeRF tuner", se);
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
                BladeRfStream stream = clearStream();

                if(stream != null)
                {
                    stream.stop();
                }
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Starts the sample stream.
     */
    private void startStream() throws SourceException
    {
        try
        {
            mStream = mOversampled ?
                    mDevice.startRx(ChannelLayout.RX_X1, OVERSAMPLE_SAMPLE_FORMAT, OVERSAMPLE_STREAM_CONFIG, this::receive) :
                    mDevice.startRx(ChannelLayout.RX_X1, SAMPLE_FORMAT, STREAM_CONFIG, this::receive);
            mStreamCheck = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkStream, STREAM_CHECK_INTERVAL_SECONDS,
                    STREAM_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
        catch(BladeRfException | RuntimeException e)
        {
            throw new SourceException("Can't start bladeRF sample stream - " + e.getMessage(), e);
        }
    }

    /**
     * Clears the sample stream so that it's no longer monitored.  The caller is responsible for stopping it.
     * @return stream or null if there is no stream
     */
    private BladeRfStream clearStream()
    {
        if(mStreamCheck != null)
        {
            mStreamCheck.cancel(false);
            mStreamCheck = null;
        }

        BladeRfStream stream = mStream;
        mStream = null;
        return stream;
    }

    /**
     * Detects a stream that ended by itself, such as when the device has an error, and flags the tuner.
     */
    private void checkStream()
    {
        String message = null;

        getLock().lock();

        try
        {
            if(mStream != null && !mStream.isRunning())
            {
                message = "bladeRF sample stream stopped - " + mStream.failure();
                clearStream();
            }
        }
        finally
        {
            getLock().unlock();
        }

        //Outside of the lock because this stops the tuner
        if(message != null)
        {
            mLog.error(message);
            setErrorMessage(message);
        }
    }

    /**
     * Receives samples on the stream thread and dispatches them to the buffer listeners.  Exceptions are contained here
     * since an exception reaching the library stops the sample stream.
     * @param buffer from the library, which is only valid until this method returns.
     * @return true to continue streaming
     */
    private boolean receive(SampleBuffer buffer)
    {
        try
        {
            long timestamp = System.currentTimeMillis();

            if(buffer.format() == OVERSAMPLE_SAMPLE_FORMAT)
            {
                broadcast(new SignedByteNativeBuffer(buffer.copyBytes(), timestamp, 0.0f, 0.0f, mSamplesPerMillisecond));
            }
            else
            {
                broadcast(new FloatNativeBuffer(scale(buffer.copyShorts()), timestamp, mSamplesPerMillisecond));
            }
        }
        catch(RuntimeException e)
        {
            mLog.error("Error dispatching bladeRF sample buffer", e);
        }

        return true;
    }

    /**
     * Scales 12-bit samples to the range -1 to 1.
     * @param samples interleaved complex samples
     * @return scaled interleaved complex samples
     */
    static float[] scale(short[] samples)
    {
        float[] scaled = new float[samples.length];

        for(int x = 0; x < samples.length; x++)
        {
            scaled[x] = samples[x] * SAMPLE_SCALE;
        }

        return scaled;
    }
}
