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

package io.github.dsheirer.source.tuner.hydrasdr;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.hydrasdr.DecimationMode;
import io.github.dsheirer.hydrasdr.GainType;
import io.github.dsheirer.hydrasdr.HydraSdr;
import io.github.dsheirer.hydrasdr.HydraSdrDevice;
import io.github.dsheirer.hydrasdr.HydraSdrException;
import io.github.dsheirer.hydrasdr.SampleBuffer;
import io.github.dsheirer.hydrasdr.SampleType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;

/**
 * HydraSDR tuner controller, implemented with the jHydraSDR library (libhydrasdr).
 *
 * The tuner is discovered with LibUsb at a USB bus and port, like all other USB tuners, however libhydrasdr owns the
 * USB device, its transfers, and the conversion of real samples to complex samples.
 */
public class HydraSdrTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(HydraSdrTunerController.class);
    public static final Gain LINEARITY_GAIN_DEFAULT = Gain.LINEARITY_14;
    public static final Gain SENSITIVITY_GAIN_DEFAULT = Gain.SENSITIVITY_10;
    public static final int GAIN_MIN = 1;
    public static final int GAIN_MAX = 22;
    public static final int LNA_GAIN_MIN = 0;
    public static final int LNA_GAIN_MAX = 14;
    public static final int LNA_GAIN_DEFAULT = 7;
    public static final int MIXER_GAIN_MIN = 0;
    public static final int MIXER_GAIN_MAX = 15;
    public static final int MIXER_GAIN_DEFAULT = 9;
    public static final int IF_GAIN_MIN = 0;
    public static final int IF_GAIN_MAX = 15;
    public static final int IF_GAIN_DEFAULT = 9;
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 24000000l;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 1800000000l;
    public static final long FREQUENCY_DEFAULT = 101100000;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.90;
    public static final DecimalFormat MHZ_FORMATTER = new DecimalFormat("#.00 MHz");
    public static final HydraSdrSampleRate DEFAULT_SAMPLE_RATE = new HydraSdrSampleRate(0, 10000000, "10.00 MHz");
    //Lower rates from libhydrasdr don't divide into whole 25 kHz polyphase channels, and the lowest give none at all.
    private static final long MINIMUM_SAMPLE_RATE = 1250000;
    //Power-of-2 sample count of an undecimated buffer, used until buffers arrive from the device.  libhydrasdr divides
    //this by the software decimation factor, which is also a power of 2.
    private static final int UNDECIMATED_BUFFER_SAMPLE_COUNT = 65536;
    //Complex samples from libhydrasdr are half the amplitude (-6 dB) of the analytic signal for a real input.  Restoring
    //this keeps spectrum levels and squelch thresholds consistent with the other tuners.
    private static final float SAMPLE_SCALE = 2.0f;
    private final int mBus;
    private final String mPortAddress;
    private HydraSdrDevice mDevice;
    private HydraSdrDeviceInformation mDeviceInfo;
    private List<HydraSdrSampleRate> mSampleRates = new ArrayList<>();
    private int mSampleRate = 0;
    private volatile float mSamplesPerMillisecond = 0.0f;
    private volatile int mBufferSampleCount = UNDECIMATED_BUFFER_SAMPLE_COUNT;
    private boolean mStreaming = false;

    /**
     * Constructs an instance
     * @param bus usb
     * @param portAddress usb
     * @param tunerErrorListener to receive error notifications from this controller
     */
    public HydraSdrTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
        setMinimumFrequency(MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(MAXIMUM_TUNABLE_FREQUENCY_HZ);
        setMiddleUnusableHalfBandwidth(0);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENT);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.HYDRASDR_R828D;
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
            mDevice = HydraSdr.open(serialNumber);
            mDevice.setSampleType(SampleType.FLOAT32_IQ);
            mDevice.setDecimationMode(DecimationMode.HIGH_DEFINITION);
            mDeviceInfo = new HydraSdrDeviceInformation(mDevice.deviceInfo());
            mSampleRates.clear();

            for(long rate : mDevice.sampleRates())
            {
                if(rate >= MINIMUM_SAMPLE_RATE)
                {
                    mSampleRates.add(new HydraSdrSampleRate(mSampleRates.size(), (int)rate, formatSampleRate((int)rate)));
                }
            }

            if(mSampleRates.isEmpty())
            {
                mSampleRates.add(DEFAULT_SAMPLE_RATE);
            }

            setFrequency(FREQUENCY_DEFAULT);
            setSampleRate(mSampleRates.get(0));
        }
        catch(HydraSdrException | SourceException | LinkageError e)
        {
            closeDevice(mDevice);
            mDevice = null;
            throw new SourceException("Can't start HydraSDR tuner - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void stop()
    {
        HydraSdrDevice device;

        getLock().lock();

        try
        {
            device = mDevice;
            mDevice = null;
            mStreaming = false;
        }
        finally
        {
            getLock().unlock();
        }

        mNativeBufferBroadcaster.clear();
        closeDevice(device);
    }

    /**
     * Closes the device, which also stops streaming and turns off the bias-T.
     * @param device to close, or null
     */
    private static void closeDevice(HydraSdrDevice device)
    {
        if(device != null)
        {
            try
            {
                device.close();
            }
            catch(HydraSdrException | RuntimeException e)
            {
                mLog.error("Error closing HydraSDR device", e);
            }
        }
    }

    /**
     * Reads the serial number of the device at this controller's USB bus and port, since libhydrasdr opens a specific
     * device by serial number.  The device is closed again before returning so that libhydrasdr can open it.
     * @return serial number
     * @throws SourceException if the device isn't found or can't be accessed.
     */
    private long readSerialNumber() throws SourceException
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
                        return readSerialNumber(device);
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

    /**
     * Reads the serial number from the device's USB serial string, formatted as: HYDRASDR SN:0123456789ABCDEF
     * @param device to read
     * @return serial number
     * @throws SourceException if the device can't be opened or the serial string isn't valid.
     */
    private static long readSerialNumber(Device device) throws SourceException
    {
        DeviceDescriptor descriptor = new DeviceDescriptor();
        int status = LibUsb.getDeviceDescriptor(device, descriptor);

        if(status != LibUsb.SUCCESS)
        {
            throw new SourceException("Can't obtain tuner's device descriptor - " + LibUsb.errorName(status));
        }

        DeviceHandle handle = new DeviceHandle();
        status = LibUsb.open(device, handle);

        if(status == LibUsb.ERROR_ACCESS)
        {
            mLog.error("Access to USB tuner denied - (windows) reinstall zadig driver or (linux) blacklist driver and/or check udev rules");
            throw new SourceException("access denied - if using linux, blacklist the default driver and/or install udev rules");
        }
        else if(status != LibUsb.SUCCESS)
        {
            mLog.error("Can't open USB tuner - check driver or Linux udev rules");
            throw new SourceException("Can't open USB tuner - reinstall driver? - " + LibUsb.errorName(status));
        }

        String serial;

        try
        {
            serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
        }
        finally
        {
            LibUsb.close(handle);
        }

        try
        {
            return Long.parseUnsignedLong(serial.substring(serial.lastIndexOf(':') + 1), 16);
        }
        catch(NullPointerException | NumberFormatException e)
        {
            throw new SourceException("Can't read HydraSDR serial number from USB serial string [" + serial + "]", e);
        }
    }

    /**
     * Operation that requires the device.
     */
    private interface DeviceOperation
    {
        void execute(HydraSdrDevice device) throws HydraSdrException;
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
        catch(HydraSdrException e)
        {
            throw new SourceException(e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Receives converted samples on the libhydrasdr thread and dispatches them to the buffer listeners.  Exceptions
     * are contained here since an exception reaching the library would stop the sample stream.
     * @param buffer from the library, which is only valid until this method returns.
     * @return true to continue streaming
     */
    private boolean receive(SampleBuffer buffer)
    {
        try
        {
            mBufferSampleCount = buffer.sampleCount();
            float[] samples = buffer.copyFloats();

            for(int x = 0; x < samples.length; x++)
            {
                samples[x] *= SAMPLE_SCALE;
            }

            broadcast(new FloatNativeBuffer(samples, System.currentTimeMillis(), mSamplesPerMillisecond));
        }
        catch(RuntimeException e)
        {
            mLog.error("Error dispatching HydraSDR sample buffer", e);
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
            execute(HydraSdrDevice::stopRx);
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
            mLog.error("Error starting streaming on HydraSDR tuner", se);
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
            mLog.error("Error stopping streaming on HydraSDR tuner", se);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void apply(TunerConfiguration tunerConfiguration) throws SourceException
    {
        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(tunerConfiguration);

        if(tunerConfiguration instanceof HydraSdrTunerConfiguration config)
        {
            HydraSdrSampleRate rate = getSampleRate(config.getSampleRate());
            setSampleRate(rate != null ? rate : mSampleRates.isEmpty() ? DEFAULT_SAMPLE_RATE : mSampleRates.get(0));

            try
            {
                setFrequency(config.getFrequency());
                setFrequencyCorrection(config.getFrequencyCorrection());

                setIFGain(config.getIFGain());
                setMixerGain(config.getMixerGain());
                setLNAGain(config.getLNAGain());
                setMixerAGC(config.isMixerAGC());
                setLNAAGC(config.isLNAAGC());
                setBiasT(config.isBiasT());

                //Set the gain mode last, so custom values are already set, and linearity and sensitivity modes will
                //automatically override the custom values.
                setGain(config.getGain());
            }
            catch(Exception e)
            {
                throw new SourceException("Couldn't apply settings from HydraSDR config", e);
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid tuner config:" + tunerConfiguration.getClass());
        }
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mFrequencyController.getTunedFrequency();
    }

    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        if(MINIMUM_TUNABLE_FREQUENCY_HZ <= frequency && frequency <= MAXIMUM_TUNABLE_FREQUENCY_HZ)
        {
            execute(device -> device.setFrequency(frequency));
        }
        else
        {
            throw new SourceException("Frequency [" + frequency + "] outside " + "of tunable range " + MINIMUM_TUNABLE_FREQUENCY_HZ + "-" + MAXIMUM_TUNABLE_FREQUENCY_HZ);
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets the sample rate.  The sample stream is stopped for the change, because libhydrasdr replaces the sample
     * converter that its streaming thread is using.
     *
     * @param rate to set
     * @throws SourceException if the rate can't be set
     */
    public void setSampleRate(HydraSdrSampleRate rate) throws SourceException
    {
        getLock().lock();

        try
        {
            mSamplesPerMillisecond = (float)rate.getRate() / 1000.0f;

            if(rate.getRate() != mSampleRate)
            {
                boolean streaming = mStreaming;
                stopStreaming();

                try
                {
                    execute(device ->
                    {
                        device.setSampleRate(rate.getRate());
                        mBufferSampleCount = UNDECIMATED_BUFFER_SAMPLE_COUNT /
                                (int)device.deviceInfo().current().decimationFactor();
                    });
                    mSampleRate = rate.getRate();
                    mFrequencyController.setSampleRate(mSampleRate);

                    // Update the Usable Bandwidth Percentage, as the usable range varies with the sample rate.
                    switch(mSampleRate)
                    {
                        default:
                        case 10000000:
                            super.setUsableBandwidthPercentage(0.90);
                            break;
                        case 6000000:
                            super.setUsableBandwidthPercentage(0.83);
                            break;
                        case 3000000:
                            super.setUsableBandwidthPercentage(0.66);
                            break;
                        case 2500000:
                            super.setUsableBandwidthPercentage(0.90);
                            break;
                    }
                }
                finally
                {
                    if(streaming)
                    {
                        startStreaming();
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
     * Returns a list of sample rates supported by the device
     */
    public List<HydraSdrSampleRate> getSampleRates()
    {
        return mSampleRates;
    }

    /**
     * HydraSDR sample rate object that matches the current sample rate setting.
     */
    public HydraSdrSampleRate getHydraSdrSampleRate()
    {
        return getSampleRate(mSampleRate);
    }

    /**
     * HydraSDR sample rate object that matches the specified rate in hertz, or
     * null if there are no available sample rates for the tuner that match the
     * argument value.
     */
    public HydraSdrSampleRate getSampleRate(int rate)
    {
        for(HydraSdrSampleRate sampleRate : mSampleRates)
        {
            if(sampleRate.getRate() == rate)
            {
                return sampleRate;
            }
        }

        //We should never get to here ...
        return null;
    }

    /**
     * Enables/disables the mixer automatic gain setting
     *
     * @param enabled true to enable
     * @throws SourceException on error
     */
    public void setMixerAGC(boolean enabled) throws SourceException
    {
        execute(device -> device.setGain(GainType.MIXER_AGC, enabled ? 1 : 0));
    }

    /**
     * Enables/disables the low noise amplifier automatic gain setting
     *
     * @param enabled true to enable
     * @throws SourceException on error
     */
    public void setLNAAGC(boolean enabled) throws SourceException
    {
        execute(device -> device.setGain(GainType.LNA_AGC, enabled ? 1 : 0));
    }

    public void setGain(Gain gain) throws SourceException
    {
        if(gain != Gain.CUSTOM)
        {
            setMixerAGC(false);
            setLNAAGC(false);
            setLNAGain(gain.getLNA());
            setMixerGain(gain.getMixer());
            setIFGain(gain.getIF());
        }
    }

    /**
     * Sets LNA gain
     *
     * @param gain - value within range of LNA_GAIN_MIN to LNA_GAIN_MAX
     * @throws SourceException          on error
     * @throws IllegalArgumentException if gain value is invalid
     */
    public void setLNAGain(int gain) throws SourceException, IllegalArgumentException
    {
        if(LNA_GAIN_MIN <= gain && gain <= LNA_GAIN_MAX)
        {
            execute(device -> device.setGain(GainType.LNA, gain));
        }
        else
        {
            throw new IllegalArgumentException("LNA gain value [" + gain + "] is outside value range: " +
                    LNA_GAIN_MIN + "-" + LNA_GAIN_MAX);
        }
    }

    /**
     * Sets Mixer gain
     *
     * @param gain - value within range of MIXER_GAIN_MIN to MIXER_GAIN_MAX
     * @throws SourceException          on error
     * @throws IllegalArgumentException if gain value is invalid
     */
    public void setMixerGain(int gain) throws SourceException, IllegalArgumentException
    {
        if(MIXER_GAIN_MIN <= gain && gain <= MIXER_GAIN_MAX)
        {
            execute(device -> device.setGain(GainType.MIXER, gain));
        }
        else
        {
            throw new IllegalArgumentException("Mixer gain value [" + gain +
                    "] is outside value range: " + MIXER_GAIN_MIN + "-" + MIXER_GAIN_MAX);
        }
    }

    /**
     * Sets IF (VGA) gain
     *
     * @param gain - value within range of IF_GAIN_MIN to IF_GAIN_MAX
     * @throws SourceException          on error
     * @throws IllegalArgumentException if gain value is invalid
     */
    public void setIFGain(int gain) throws SourceException, IllegalArgumentException
    {
        if(IF_GAIN_MIN <= gain && gain <= IF_GAIN_MAX)
        {
            execute(device -> device.setGain(GainType.VGA, gain));
        }
        else
        {
            throw new IllegalArgumentException("VGA gain value [" + gain +
                    "] is outside value range: " + IF_GAIN_MIN + "-" + IF_GAIN_MAX);
        }
    }

    /**
     * Enables/disables the Bias-T (RF bias tee) power output for active antennas
     *
     * @param enabled true to enable Bias-T, false to disable
     * @throws SourceException on error
     */
    public void setBiasT(boolean enabled) throws SourceException
    {
        execute(device -> device.setBiasTee(enabled));
    }

    /**
     * Formats the rate in hertz for display as megahertz
     */
    private static String formatSampleRate(int rate)
    {
        return MHZ_FORMATTER.format((double) rate / 1E6d);
    }

    /**
     * Device information, available once the tuner has been started.
     */
    public HydraSdrDeviceInformation getDeviceInfo()
    {
        return mDeviceInfo;
    }

    public enum GainMode
    {
        LINEARITY,
        SENSITIVITY,
        CUSTOM;
    }

    public enum Gain
    {
        LINEARITY_1(1, 4, 0, 0),
        LINEARITY_2(2, 5, 0, 0),
        LINEARITY_3(3, 6, 1, 0),
        LINEARITY_4(4, 7, 1, 0),
        LINEARITY_5(5, 8, 1, 0),
        LINEARITY_6(6, 9, 1, 0),
        LINEARITY_7(7, 10, 2, 0),
        LINEARITY_8(8, 10, 2, 1),
        LINEARITY_9(9, 10, 0, 3),
        LINEARITY_10(10, 10, 0, 5),
        LINEARITY_11(11, 10, 1, 6),
        LINEARITY_12(12, 10, 0, 8),
        LINEARITY_13(13, 10, 0, 9),
        LINEARITY_14(14, 10, 5, 8),
        LINEARITY_15(15, 10, 6, 9),
        LINEARITY_16(16, 11, 6, 9),
        LINEARITY_17(17, 11, 7, 10),
        LINEARITY_18(18, 11, 8, 12),
        LINEARITY_19(19, 11, 9, 13),
        LINEARITY_20(20, 11, 11, 14),
        LINEARITY_21(21, 12, 12, 14),
        LINEARITY_22(22, 13, 12, 14),
        SENSITIVITY_1(1, 4, 0, 0),
        SENSITIVITY_2(2, 4, 0, 1),
        SENSITIVITY_3(3, 4, 0, 2),
        SENSITIVITY_4(4, 4, 0, 3),
        SENSITIVITY_5(5, 4, 1, 5),
        SENSITIVITY_6(6, 4, 2, 6),
        SENSITIVITY_7(7, 4, 2, 7),
        SENSITIVITY_8(8, 4, 3, 8),
        SENSITIVITY_9(9, 4, 4, 9),
        SENSITIVITY_10(10, 5, 4, 9),
        SENSITIVITY_11(11, 5, 4, 12),
        SENSITIVITY_12(12, 5, 7, 12),
        SENSITIVITY_13(13, 5, 8, 13),
        SENSITIVITY_14(14, 5, 9, 14),
        SENSITIVITY_15(15, 6, 9, 14),
        SENSITIVITY_16(16, 7, 10, 14),
        SENSITIVITY_17(17, 8, 10, 14),
        SENSITIVITY_18(18, 9, 11, 14),
        SENSITIVITY_19(19, 10, 12, 14),
        SENSITIVITY_20(20, 11, 12, 14),
        SENSITIVITY_21(21, 12, 12, 14),
        SENSITIVITY_22(22, 13, 12, 14),
        CUSTOM(1, 0, 0, 0);

        private int mValue;
        private int mIF;
        private int mMixer;
        private int mLNA;

        private Gain(int value, int ifGain, int mixer, int lna)
        {
            mValue = value;
            mIF = ifGain;
            mMixer = mixer;
            mLNA = lna;
        }

        public int getValue()
        {
            return mValue;
        }

        public int getIF()
        {
            return mIF;
        }

        public int getMixer()
        {
            return mMixer;
        }

        public int getLNA()
        {
            return mLNA;
        }

        public static Gain getGain(GainMode mode, int value)
        {
            assert (GAIN_MIN <= value && value <= GAIN_MAX);

            switch(mode)
            {
                case LINEARITY:
                    for(Gain gain : getLinearityGains())
                    {
                        if(gain.getValue() == value)
                        {
                            return gain;
                        }
                    }
                    return LINEARITY_GAIN_DEFAULT;
                case SENSITIVITY:
                    for(Gain gain : getSensitivityGains())
                    {
                        if(gain.getValue() == value)
                        {
                            return gain;
                        }
                    }
                    return SENSITIVITY_GAIN_DEFAULT;
                case CUSTOM:
                default:
                    return Gain.CUSTOM;
            }
        }

        public GainMode getGainMode()
        {
            if(getLinearityGains().contains(this))
            {
                return GainMode.LINEARITY;
            }
            else if(getSensitivityGains().contains(this))
            {
                return GainMode.SENSITIVITY;
            }

            return GainMode.CUSTOM;
        }

        public static EnumSet<Gain> getLinearityGains()
        {
            return EnumSet.range(LINEARITY_1, LINEARITY_22);
        }

        public static EnumSet<Gain> getSensitivityGains()
        {
            return EnumSet.range(SENSITIVITY_1, SENSITIVITY_22);
        }
    }
}
