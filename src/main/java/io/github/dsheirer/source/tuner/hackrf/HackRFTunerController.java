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
package io.github.dsheirer.source.tuner.hackrf;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.SignedByteNativeBufferFactory;
import io.github.dsheirer.hackrf.BoardId;
import io.github.dsheirer.hackrf.BoardInfo;
import io.github.dsheirer.hackrf.HackRf;
import io.github.dsheirer.hackrf.HackRfDevice;
import io.github.dsheirer.hackrf.HackRfException;
import io.github.dsheirer.hackrf.SampleBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.UsbDeviceLookup;
import java.util.EnumSet;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HackRF One, HackRF Pro, Jawbreaker and rad1o tuner controller, implemented with the jHackRF library (libhackrf).
 *
 * The tuner is discovered with LibUsb at a USB bus and port, like all other USB tuners, however libhackrf owns the
 * USB device and its transfers.
 */
public class HackRFTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(HackRFTunerController.class);

    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 10000000l;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 6000000000l;
    public static final long DEFAULT_FREQUENCY = 101100000;
    public static final double USABLE_BANDWIDTH = 0.90;
    public static final int DC_HALF_BANDWIDTH = 5000;
    //Sample count of each buffer from libhackrf (262,144 bytes), used until the device is opened.
    private static final int DEFAULT_BUFFER_SAMPLE_COUNT = 131072;

    private final int mBus;
    private final String mPortAddress;
    private final SignedByteNativeBufferFactory mNativeBufferFactory = new SignedByteNativeBufferFactory();
    private HackRfDevice mDevice;
    private TunerType mTunerType = TunerType.HACKRF_ONE;
    private String mBoardName;
    private String mSerialNumber;
    private String mPartId;
    private String mFirmwareVersion;
    private HackRFSampleRate mSampleRate = HackRFSampleRate.RATE_5_0;
    private boolean mAmplifierEnabled = false;
    private boolean mBiasTEnabled = false;
    private volatile int mBufferSampleCount = DEFAULT_BUFFER_SAMPLE_COUNT;
    private boolean mStreaming = false;

    /**
     * Constructs an instance
     * @param bus usb
     * @param portAddress usb
     * @param tunerErrorListener to receive tuner error notifications.
     */
    public HackRFTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
        setMinimumFrequency(MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(MAXIMUM_TUNABLE_FREQUENCY_HZ);
        setMiddleUnusableHalfBandwidth(DC_HALF_BANDWIDTH);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH);
    }

    /**
     * Tuner type for this HackRF, available once the tuner is started.
     */
    @Override
    public TunerType getTunerType()
    {
        return mTunerType;
    }

    @Override
    public int getBufferSampleCount()
    {
        return mBufferSampleCount;
    }

    @Override
    public void start() throws SourceException
    {
        String serialNumber = readSerialNumber();

        getLock().lock();

        try
        {
            mDevice = open(serialNumber);

            BoardId boardId = mDevice.boardId();
            mBoardName = boardId.name();
            mTunerType = getTunerType(boardId);

            BoardInfo boardInfo = mDevice.boardInfo();
            mSerialNumber = formatSerialNumber(boardInfo.serialNumber());
            mPartId = String.format(Locale.ROOT, "%08X-%08X", boardInfo.partId1(), boardInfo.partId2());
            mFirmwareVersion = mDevice.firmwareVersion();
            mBufferSampleCount = mDevice.transferBufferSize() / 2;

            //The device keeps its previous settings, so force the default sample rate to be applied.
            mSampleRate = null;
            setSampleRate(HackRFSampleRate.RATE_5_0);
            setFrequency(DEFAULT_FREQUENCY);
        }
        catch(HackRfException | SourceException | RuntimeException | LinkageError e)
        {
            closeDevice(mDevice);
            mDevice = null;
            throw new SourceException("Can't start HackRF tuner - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void stop()
    {
        HackRfDevice device;

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
     * Opens the device by serial number, or when the device doesn't report a USB serial number, opens it when it is
     * the only HackRF device connected.
     * @param serialNumber from the USB serial string descriptor, or null.
     * @return opened device
     */
    private static HackRfDevice open(String serialNumber) throws HackRfException, SourceException
    {
        if(serialNumber != null)
        {
            return HackRf.open(serialNumber);
        }

        if(HackRf.listDevices().size() == 1)
        {
            return HackRf.open(0);
        }

        throw new SourceException("HackRF doesn't report a USB serial number and can't be distinguished from the " +
                "other HackRF devices - update the HackRF firmware");
    }

    /**
     * Closes the device, which also stops streaming.
     * @param device to close, or null
     */
    private static void closeDevice(HackRfDevice device)
    {
        if(device != null)
        {
            try
            {
                device.setAntennaEnable(false);
            }
            catch(HackRfException | RuntimeException e)
            {
                mLog.error("Error disabling HackRF Bias-T antenna power", e);
            }

            try
            {
                device.close();
            }
            catch(HackRfException | RuntimeException e)
            {
                mLog.error("Error closing HackRF device", e);
            }
        }
    }

    /**
     * Maps the board identifier to a tuner type.
     */
    private static TunerType getTunerType(BoardId boardId)
    {
        return switch(boardId.code())
        {
            case 1 -> TunerType.HACKRF_JAWBREAKER;
            case 3 -> TunerType.HACKRF_RAD1O;
            case BoardId.PRALINE -> TunerType.HACKRF_PRO;
            default -> TunerType.HACKRF_ONE;
        };
    }

    /**
     * Formats the 32 hex digit serial number as four dash-separated groups, as sdrtrunk has always identified HackRF
     * tuners, so that existing tuner configurations continue to match.
     */
    private static String formatSerialNumber(String serialNumber)
    {
        String serial = serialNumber.toUpperCase(Locale.ROOT);

        if(serial.length() != 32)
        {
            return serial;
        }

        return serial.substring(0, 8) + "-" + serial.substring(8, 16) + "-" + serial.substring(16, 24) + "-" +
                serial.substring(24, 32);
    }

    /**
     * Reads the USB serial number of the device at this controller's USB bus and port, since libhackrf opens a
     * specific device by serial number.
     * @return serial number or null if the device doesn't report a serial number.
     * @throws SourceException if the device isn't found or can't be accessed.
     */
    private String readSerialNumber() throws SourceException
    {
        String serial = UsbDeviceLookup.readSerialNumber(mBus, mPortAddress);

        //libhackrf compares, at most, the last 32 characters of the serial string
        if(serial != null && serial.length() > 32)
        {
            serial = serial.substring(serial.length() - 32);
        }

        return serial == null || serial.isEmpty() ? null : serial;
    }

    /**
     * Operation that requires the device.
     */
    private interface DeviceOperation
    {
        void execute(HackRfDevice device) throws HackRfException;
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
        catch(HackRfException | IllegalArgumentException | IllegalStateException e)
        {
            throw new SourceException(e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Receives samples on the libusb event thread and dispatches them to the buffer listeners.  Exceptions are
     * contained here since an exception reaching the library would stop the sample stream.
     * @param buffer from the library, which is only valid until this method returns.
     * @return true to continue streaming
     */
    private boolean receive(SampleBuffer buffer)
    {
        try
        {
            //The buffer factory copies the samples from native memory
            broadcast(mNativeBufferFactory.getBuffer(buffer.samples().asByteBuffer(), System.currentTimeMillis()));
        }
        catch(RuntimeException e)
        {
            mLog.error("Error dispatching HackRF sample buffer", e);
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

            //The firmware turns off antenna power when the transceiver returns to idle, so restore it
            if(mBiasTEnabled)
            {
                execute(device -> device.setAntennaEnable(true));
            }
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
            execute(HackRfDevice::stopRx);
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
            mLog.error("Error starting streaming on HackRF tuner", se);
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
            mLog.error("Error stopping streaming on HackRF tuner", se);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * HackRF board name, available once the tuner is started.
     * @return board name or null
     */
    public String getBoardName()
    {
        return mBoardName;
    }

    /**
     * HackRF firmware version string, available once the tuner is started.
     * @return firmware version or null
     */
    public String getFirmwareVersion()
    {
        return mFirmwareVersion;
    }

    /**
     * HackRF serial number, formatted as four dash-separated groups of 8 hex digits, available once the tuner is
     * started.
     * @return serial number or null
     */
    public String getSerialNumber()
    {
        return mSerialNumber;
    }

    /**
     * HackRF microcontroller part ID, available once the tuner is started.
     * @return part ID or null
     */
    public String getPartId()
    {
        return mPartId;
    }

    /**
     * Sets the HackRF baseband filter
     */
    public void setBasebandFilter(BasebandFilter filter) throws SourceException
    {
        execute(device -> device.setBasebandFilterBandwidth(filter.getBandwidth()));
    }

    /**
     * Enables (true) or disables (false) the amplifier
     */
    public void setAmplifierEnabled(boolean enabled) throws SourceException
    {
        execute(device -> device.setAmpEnable(enabled));
        mAmplifierEnabled = enabled;
    }

    public boolean getAmplifier()
    {
        return mAmplifierEnabled;
    }

    /**
     * Enables (true) or disables (false) the Bias-T power on the antenna port.
     */
    public void setBiasTEnabled(boolean enabled) throws SourceException
    {
        execute(device -> device.setAntennaEnable(enabled));
        mBiasTEnabled = enabled;
    }

    public boolean isBiasTEnabled()
    {
        return mBiasTEnabled;
    }

    /**
     * Sets the IF LNA Gain
     */
    public void setLNAGain(HackRFLNAGain gain) throws SourceException
    {
        execute(device -> device.setLnaGain(gain.getValue()));
    }

    /**
     * Sets the Baseband VGA Gain
     */
    public void setVGAGain(HackRFVGAGain gain) throws SourceException
    {
        execute(device -> device.setVgaGain(gain.getValue()));
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
            throw new SourceException("Frequency [" + frequency + "] outside of tunable range " +
                    MINIMUM_TUNABLE_FREQUENCY_HZ + "-" + MAXIMUM_TUNABLE_FREQUENCY_HZ);
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mSampleRate != null ? mSampleRate.getRate() : 0;
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(config);

        if(config instanceof HackRFTunerConfiguration hackRFConfig)
        {
            //Convert legacy sample rate setting to new sample rates
            if(!hackRFConfig.getSampleRate().isValidSampleRate())
            {
                mLog.warn("Changing legacy HackRF Sample Rates Setting [" + hackRFConfig.getSampleRate().name() +
                        "] to current valid setting");
                hackRFConfig.setSampleRate(HackRFSampleRate.RATE_5_0);
            }

            try
            {
                setSampleRate(hackRFConfig.getSampleRate());
                setAmplifierEnabled(hackRFConfig.getAmplifierEnabled());
                setBiasTEnabled(hackRFConfig.isBiasT());
                setLNAGain(hackRFConfig.getLNAGain());
                setVGAGain(hackRFConfig.getVGAGain());
            }
            catch(SourceException e)
            {
                throw new SourceException("Error while applying tuner configuration", e);
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid tuner configuration type [" + config.getClass() + "]");
        }
    }

    /**
     * Sets the sample rate and the baseband filter for the sample rate.  Setting the sample rate resets the
     * baseband filter, so the filter is set afterward.
     */
    public void setSampleRate(HackRFSampleRate rate) throws SourceException
    {
        if(rate == null || !rate.isValidSampleRate())
        {
            rate = HackRFSampleRate.RATE_5_0;
        }

        if(rate == mSampleRate)
        {
            return;
        }

        HackRFSampleRate sampleRate = rate;

        getLock().lock();

        try
        {
            execute(device -> {
                device.setSampleRateManual(sampleRate.getRate(), 1);
                device.setBasebandFilterBandwidth(sampleRate.getFilter().getBandwidth());
            });
            mSampleRate = sampleRate;
            mNativeBufferFactory.setSamplesPerMillisecond(sampleRate.getRate() / 1000.0f);
            mFrequencyController.setSampleRate(sampleRate.getRate());
        }
        finally
        {
            getLock().unlock();
        }
    }

    public enum HackRFSampleRate
    {
		//Changes to enumerate correct rates
        RATE_1_75(1750000, "1.750 MHz", BasebandFilter.F1_75),
        RATE_2_5(2500000, "2.500 MHz", BasebandFilter.F2_50),
        RATE_3_5(3500000, "3.500 MHz", BasebandFilter.F3_50),
        RATE_5_0(5000000, "5.000 MHz", BasebandFilter.F5_00),
		RATE_5_5(5500000, "5.500 MHz", BasebandFilter.F5_50),
        RATE_6_0(6000000, "6.000 MHz", BasebandFilter.F6_00),
        RATE_7_0(7000000, "7.000 MHz", BasebandFilter.F7_00),
        RATE_8_0(8000000, "8.000 MHz", BasebandFilter.F8_00),
        RATE_9_0(9000000, "9.000 MHz", BasebandFilter.F9_00),
        RATE_10_0(10000000, "10.000 MHz", BasebandFilter.F10_00),
        RATE_12_0(12000000, "12.000 MHz", BasebandFilter.F12_00),
        RATE_14_0(14000000, "14.000 MHz", BasebandFilter.F14_00),
        RATE_15_0(15000000, "15.000 MHz", BasebandFilter.F15_00),
        RATE_20_0(20000000, "20.000 MHz", BasebandFilter.F20_00),

        //These sample rates are deprecated.  They're maintained here for backward compatibility with user settings
        RATE_2_3(2300000, "2.300 MHz", BasebandFilter.F1_75),
        RATE_3_3(3300000, "3.300 MHz", BasebandFilter.F2_50),
        RATE_4_6(4600000, "4.600 MHz", BasebandFilter.F3_50),
        RATE_6_6(6600000, "6.600 MHz", BasebandFilter.F5_00),
        RATE_7_3(7300000, "7.300 MHz", BasebandFilter.F5_50),
        RATE_9_3(9300000, "9.300 MHz", BasebandFilter.F7_00),
        RATE_10_6(10600000, "10.600 MHz", BasebandFilter.F8_00),
        RATE_13_3(13300000, "13.300 MHz", BasebandFilter.F10_00),
        RATE_16_0(16000000, "16.000 MHz", BasebandFilter.F12_00),
        RATE_18_6(18600000, "18.600 MHz", BasebandFilter.F14_00),
        RATE2_016MHZ(2016000, "*2.016 MHz", BasebandFilter.F3_50),
        RATE3_024MHZ(3024000, "*3.024 MHz", BasebandFilter.F5_00),
        RATE4_464MHZ(4464000, "*4.464 MHz", BasebandFilter.F6_00),
        RATE5_376MHZ(5376000, "*5.376 MHz", BasebandFilter.F7_00),
        RATE7_488MHZ(7488000, "*7.488 MHz", BasebandFilter.F9_00),
        RATE10_080MHZ(10080000, "*10.080 MHz", BasebandFilter.F12_00),
        RATE12_000MHZ(12000000, "*12.000 MHz", BasebandFilter.F14_00),
        RATE13_440MHZ(13440000, "*13.440 MHz", BasebandFilter.F15_00),
        RATE14_976MHZ(14976000, "*14.976 MHz", BasebandFilter.F20_00),
        RATE19_968MHZ(19968000, "*19.968 MHz", BasebandFilter.F24_00);
        private int mRate;
        private String mLabel;
        private BasebandFilter mFilter;

        HackRFSampleRate(int rate, String label, BasebandFilter filter)
        {
            mRate = rate;
            mLabel = label;
            mFilter = filter;
        }

        public static EnumSet<HackRFSampleRate> VALID_SAMPLE_RATES = EnumSet.range(RATE_1_75, RATE_20_0);

        public int getRate()
        {
            return mRate;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public BasebandFilter getFilter()
        {
            return mFilter;
        }

        public boolean isValidSampleRate()
        {
            return VALID_SAMPLE_RATES.contains(this);
        }
    }

    public enum BasebandFilter
    {
        FAUTO(0, "AUTO"),
        F1_75(1750000, "1.75 MHz"),
        F2_50(2500000, "2.50 MHz"),
        F3_50(3500000, "3.50 MHz"),
        F5_00(5000000, "5.00 MHz"),
        F5_50(5500000, "5.50 MHz"),
        F6_00(6000000, "6.00 MHz"),
        F7_00(7000000, "7.00 MHz"),
        F8_00(8000000, "8.00 MHz"),
        F9_00(9000000, "9.00 MHz"),
        F10_00(10000000, "10.00 MHz"),
        F12_00(12000000, "12.00 MHz"),
        F14_00(14000000, "14.00 MHz"),
        F15_00(15000000, "15.00 MHz"),
        F20_00(20000000, "20.00 MHz"),
        F24_00(24000000, "24.00 MHz"),
        F28_00(28000000, "28.00 MHz");

        private int mBandwidth;
        private String mLabel;

        BasebandFilter(int bandwidth, String label)
        {
            mBandwidth = bandwidth;
            mLabel = label;
        }

        public int getBandwidth()
        {
            return mBandwidth;
        }

        public String getLabel()
        {
            return mLabel;
        }
    }

    public enum HackRFLNAGain
    {
        GAIN_0(0),
        GAIN_8(8),
        GAIN_16(16),
        GAIN_24(24),
        GAIN_32(32),
        GAIN_40(40);

        private int mValue;

        HackRFLNAGain(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }

        public String toString()
        {
            return mValue + " dB";
        }
    }

    /**
     * Receive (baseband) VGA Gain values
     */
    public enum HackRFVGAGain
    {
        GAIN_0(0),
        GAIN_2(2),
        GAIN_4(4),
        GAIN_6(6),
        GAIN_8(8),
        GAIN_10(10),
        GAIN_12(12),
        GAIN_14(14),
        GAIN_16(16),
        GAIN_18(18),
        GAIN_20(20),
        GAIN_22(22),
        GAIN_23(24),
        GAIN_26(26),
        GAIN_28(28),
        GAIN_30(30),
        GAIN_32(32),
        GAIN_34(34),
        GAIN_36(36),
        GAIN_38(38),
        GAIN_40(40),
        GAIN_42(42),
        GAIN_44(44),
        GAIN_46(46),
        GAIN_48(48),
        GAIN_50(50),
        GAIN_52(52),
        GAIN_54(54),
        GAIN_56(56),
        GAIN_58(58),
        GAIN_60(60),
        GAIN_62(62);

        private int mValue;

        HackRFVGAGain(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }

        public String toString()
        {
            return mValue + " dB";
        }
    }
}
