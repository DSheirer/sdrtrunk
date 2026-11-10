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

package io.github.dsheirer.source.tuner.limesdr;

import io.github.dsheirer.libusb.LibUsb;
import io.github.dsheirer.libusb.UsbContext;
import io.github.dsheirer.libusb.UsbDeviceHandle;
import io.github.dsheirer.libusb.UsbDeviceInfo;
import io.github.dsheirer.libusb.UsbException;
import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.limesdr.DeviceEntry;
import io.github.dsheirer.limesdr.DeviceInfo;
import io.github.dsheirer.limesdr.Direction;
import io.github.dsheirer.limesdr.LimeSdr;
import io.github.dsheirer.limesdr.LimeSdrDevice;
import io.github.dsheirer.limesdr.LimeSdrException;
import io.github.dsheirer.limesdr.LogLevel;
import io.github.dsheirer.limesdr.Range;
import io.github.dsheirer.limesdr.SampleBuffer;
import io.github.dsheirer.limesdr.SampleFormat;
import io.github.dsheirer.limesdr.StreamConfig;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.UsbDeviceLookup;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LimeSDR tuner controller, implemented with the jLimeSDR library (LimeSuiteNG).
 *
 * The tuner is discovered with LibUsb at a USB bus and port, like all other USB tuners, however LimeSuiteNG owns the USB
 * device and its transfers.  LimeSuiteNG identifies USB devices only by serial number, so the serial number is read
 * from the device at the bus and port.  When it can't be read, such as on Windows where the LimeSDR-USB uses the
 * Cypress driver instead of WinUSB, the device is opened when it is the only LimeSDR of its kind.
 *
 * Only the first receive channel is used.  The LimeSDR-USB has a second receive channel, but it shares the local
 * oscillator and sample rate with the first, so it couldn't be tuned as an independent tuner.
 *
 * The RX chain is calibrated (DC offset and IQ imbalance) when the sample stream starts, and again when the sample rate
 * or antenna changes.  Calibration also adjusts the amplifiers, so the gain is reapplied afterward.
 */
public class LimeSdrTunerController extends TunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(LimeSdrTunerController.class);
    private static final Logger LIME_SUITE_LOG = LoggerFactory.getLogger("LimeSuiteNG");
    private static final AtomicBoolean LOG_HANDLER_INSTALLED = new AtomicBoolean();
    //The LMS7002M range.  The device reports its own range when it's started.
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 100_000L;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 3_800_000_000L;
    public static final int GAIN_MINIMUM = 0;
    public static final int GAIN_MAXIMUM = 73;
    public static final int GAIN_DEFAULT = 40;
    //Wideband LNA port.  LimeSuiteNG selects LNAL after initialization.
    public static final String ANTENNA_DEFAULT = "LNAW";
    private static final long FREQUENCY_DEFAULT = 101_100_000L;
    private static final int CHANNEL = 0;
    //12-bit samples in 16-bit values, which is the USB link format, so LimeSuiteNG only unpacks them.
    private static final SampleFormat SAMPLE_FORMAT = SampleFormat.I12;
    private static final float SAMPLE_SCALE = 1.0f / 2048.0f;
    //The samples are offset by -0.5 LSB, which appears as a DC spike at the center of the spectrum.  Measured on a
    //LimeSDR-USB at -0.48 to -0.50 LSB on both I and Q, independent of frequency, sample rate and calibration.
    private static final float SAMPLE_OFFSET = 0.5f;
    private static final int BUFFER_SAMPLE_COUNT = 65536;
    //Sample FIFO between the LimeSuiteNG receive loop and the stream thread, in seconds.  This is the LimeSuiteNG
    //default, which the 1M sample default of jLimeSDR (26 ms at 40 MHz) replaces.
    private static final double FIFO_SECONDS = 0.25;
    //Requests the largest USB transfers (jLimeSDR 25.1.0.1 and later), which keeps about 8.7 ms of transfers in
    //flight at 40 MHz instead of 1.1 ms, so that the LimeSuiteNG receive thread, which doesn't get realtime priority
    //without the rtprio limit on Linux, loses fewer packets when the system is busy.
    private static final float THROUGHPUT_VS_LATENCY = 1.0f;
    //Analog low-pass filter bandwidth relative to the sample rate.  At 1.0, the noise floor is 2.4 dB down at 90% of
    //the band, and at 1.2 it is 1.8 dB down.  The LMS7002M decimation filters prevent aliasing from the wider filter.
    private static final double LPF_BANDWIDTH_RATIO = 1.2;
    private static final double USABLE_BANDWIDTH_PERCENT = 0.85;
    //The USB link carries 3 bytes per sample (12-bit I and Q).  USB 2.0 sustains about 32 MBytes per second.
    private static final double USB2_MAXIMUM_SAMPLE_RATE = 32_000_000.0 / 3.0;
    private static final long STREAM_CHECK_INTERVAL_SECONDS = 1;

    private final int mBus;
    private final String mPortAddress;
    private LimeSdrDevice mDevice;
    private LimeSdrDeviceInformation mDeviceInfo;
    private boolean mStreaming;
    private ScheduledFuture<?> mStreamCheck;
    private boolean mCalibrated;
    private List<LimeSdrSampleRate> mSampleRates = List.of();
    private LimeSdrSampleRate mSampleRate;
    private List<String> mAntennaPorts = List.of();
    private List<String> mAntennas = List.of();
    private String mAntenna;
    private Range mLpfRange;
    private long mMinimumTunableFrequency = MINIMUM_TUNABLE_FREQUENCY_HZ;
    private long mMaximumTunableFrequency = MAXIMUM_TUNABLE_FREQUENCY_HZ;
    private int mGain = GAIN_DEFAULT;
    private volatile float mSamplesPerMillisecond = 0.0f;
    //Reused by the stream thread for each buffer of samples from the library
    private short[] mReceiveBuffer = new short[0];
    //Hardware timestamp (sample count) expected for the next buffer, or -1 when the stream has just started.  A larger
    //timestamp means that samples were dropped, either on the USB link or because the stream FIFO overflowed.
    private long mExpectedTimestamp = -1;
    private final AtomicLong mDroppedSamples = new AtomicLong();
    //Calibrating while streaming makes small timestamp gaps that aren't dropped samples, so gaps aren't counted until
    //shortly after a calibration (System.nanoTime)
    private volatile long mIgnoreGapsUntil = Long.MIN_VALUE;
    private static final long CALIBRATION_GAP_GRACE_NANOSECONDS = TimeUnit.MILLISECONDS.toNanos(250);

    /**
     * Constructs an instance
     * @param bus usb
     * @param portAddress usb
     * @param tunerErrorListener to receive error notifications from this controller
     */
    public LimeSdrTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
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
        return TunerType.LIMESDR;
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
            installLogHandler();
            UsbIdentity identity = readUsbIdentity();
            DeviceEntry entry = findDevice(identity, LimeSdr.listDevices());
            mDevice = LimeSdr.open(entry);
            mDevice.init();
            mDevice.enableChannel(Direction.RX, CHANNEL, true);
            mSampleRate = null;
            mCalibrated = false;

            DeviceInfo info = mDevice.deviceInfo();
            mDeviceInfo = new LimeSdrDeviceInformation(info.serialNumberHex(), info.deviceName(),
                    info.firmwareVersion(), info.hardwareVersion(), info.gatewareVersion(), entry.media(),
                    LimeSdr.libraryVersion());

            Range frequencies = mDevice.frequencyRange(Direction.RX);
            mMinimumTunableFrequency = (long)frequencies.min();
            mMaximumTunableFrequency = (long)frequencies.max();
            setFrequencyExtents(mMinimumTunableFrequency, mMaximumTunableFrequency);

            Range rates = mDevice.sampleRateRange(Direction.RX);
            mSampleRates = LimeSdrSampleRate.getRates(rates.min(), getMaximumSampleRate(rates, entry.media()));

            if(mSampleRates.isEmpty())
            {
                throw new SourceException("LimeSDR sample rate range [" + rates + "] doesn't include any usable rates");
            }

            mLpfRange = mDevice.lpfBandwidthRange(Direction.RX);
            mAntennaPorts = mDevice.antennas(Direction.RX, CHANNEL);
            mAntennas = getSelectableAntennas(mAntennaPorts);

            if(mAntennas.isEmpty())
            {
                throw new SourceException("LimeSDR doesn't report any receive antenna ports " + mAntennaPorts);
            }

            setSampleRate(LimeSdrSampleRate.getClosest(mSampleRates, LimeSdrSampleRate.DEFAULT.getRate()));
            setFrequency(FREQUENCY_DEFAULT);
            setAntenna(getDefaultAntenna(mAntennas));
            setGain(GAIN_DEFAULT);
            mLog.info("LimeSDR [" + mDeviceInfo.board() + "] serial [" + mDeviceInfo.serial() + "] started on [" +
                    mDeviceInfo.connection() + "] - LimeSuiteNG " + mDeviceInfo.library());
        }
        catch(LimeSdrException | RuntimeException | LinkageError e)
        {
            closeDevice(mDevice);
            mDevice = null;
            throw new SourceException("Can't start LimeSDR tuner - " + e.getMessage(), e);
        }
        catch(SourceException se)
        {
            closeDevice(mDevice);
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
        LimeSdrDevice device;

        getLock().lock();

        try
        {
            device = mDevice;
            mDevice = null;
            clearStream();
        }
        finally
        {
            getLock().unlock();
        }

        mNativeBufferBroadcaster.clear();

        //Closing the device also stops the sample stream
        closeDevice(device);
    }

    /**
     * Closes the device.
     * @param device to close, or null
     */
    private static void closeDevice(LimeSdrDevice device)
    {
        if(device != null)
        {
            try
            {
                device.close();
            }
            catch(LimeSdrException | RuntimeException e)
            {
                mLog.error("Error closing LimeSDR device", e);
            }
        }
    }

    /**
     * Sends LimeSuiteNG log messages to the application log.  Errors are also included in the message of the exception
     * from the failed call.
     */
    private static void installLogHandler()
    {
        if(LOG_HANDLER_INSTALLED.compareAndSet(false, true))
        {
            LimeSdr.setLogHandler(LimeSdrTunerController::log);
        }
    }

    /**
     * Logs a LimeSuiteNG message.  Its informational messages, such as calibration progress, are logged at debug level.
     * Verbose and debug messages are discarded, since some are logged from the LimeSuiteNG receive loop for each lost
     * packet, and writing them to the log stalls the receive loop, which loses more packets.  Lost samples are detected
     * from the sample timestamps instead and reported by the stream check.
     */
    static void log(LogLevel level, String message)
    {
        switch(level)
        {
            case CRITICAL, ERROR -> LIME_SUITE_LOG.error(message);
            case WARNING -> LIME_SUITE_LOG.warn(message);
            case INFO -> LIME_SUITE_LOG.debug(message);
            default -> {}
        }
    }

    /**
     * Number of samples in the stream FIFO for the sample rate.
     */
    static int getFifoSize(LimeSdrSampleRate rate)
    {
        return Math.max(StreamConfig.DEFAULT_FIFO_SIZE, (int)(rate.getRate() * FIFO_SECONDS));
    }

    /**
     * Highest sample rate supported by the hardware and the USB connection.
     * @param range of sample rates reported by the device
     * @param connection type, such as USB3.0 or USB2.0
     * @return maximum sample rate
     */
    static double getMaximumSampleRate(Range range, String connection)
    {
        if(connection != null && connection.toUpperCase(Locale.ROOT).startsWith("USB2"))
        {
            return Math.min(range.max(), USB2_MAXIMUM_SAMPLE_RATE);
        }

        return range.max();
    }

    /**
     * Receive antenna ports that a user can select, which excludes the 'None' port and the internal loopback ports.
     * @param ports reported by the device, where each port's index is its position in the list.
     * @return port names
     */
    static List<String> getSelectableAntennas(List<String> ports)
    {
        List<String> antennas = new ArrayList<>();

        for(String port : ports)
        {
            String name = port.toUpperCase(Locale.ROOT);

            if(!name.equals("NONE") && !name.startsWith("LB"))
            {
                antennas.add(port);
            }
        }

        return antennas;
    }

    /**
     * Default antenna port: the wideband LNA when the device has one, otherwise the first port.
     */
    static String getDefaultAntenna(List<String> antennas)
    {
        for(String antenna : antennas)
        {
            if(antenna.equalsIgnoreCase(ANTENNA_DEFAULT))
            {
                return antenna;
            }
        }

        return antennas.getFirst();
    }

    /**
     * Identifies the USB device at this controller's bus and port.
     * @param vendor id
     * @param product id
     * @param serial number, or null when it can't be read
     */
    record UsbIdentity(int vendor, int product, String serial) {}

    /**
     * Finds the LimeSuiteNG entry for the USB device.  The entry address is the USB vendor and product ID, so the device
     * is matched by serial number, or when the serial number isn't available, by being the only device of its kind.
     * @param identity of the USB device
     * @param entries from LimeSuiteNG
     * @return matching entry
     * @throws SourceException if there isn't exactly one match
     */
    static DeviceEntry findDevice(UsbIdentity identity, List<DeviceEntry> entries) throws SourceException
    {
        List<DeviceEntry> candidates = new ArrayList<>();

        for(DeviceEntry entry : entries)
        {
            if(isUsbDevice(entry, identity.vendor(), identity.product()))
            {
                if(identity.serial() != null && entry.hasSerialNumber(identity.serial()))
                {
                    return entry;
                }

                candidates.add(entry);
            }
        }

        if(identity.serial() == null && candidates.size() == 1)
        {
            return candidates.getFirst();
        }

        if(candidates.isEmpty())
        {
            throw new SourceException("LimeSDR library can't access the USB device - if using linux, check the udev " +
                    "rules.  If using windows, install the LimeSDR-USB (Cypress) or LimeSDR Mini (FTDI D3XX) driver.");
        }

        if(identity.serial() == null)
        {
            throw new SourceException("Can't read the LimeSDR USB serial number to choose between " +
                    candidates.size() + " LimeSDR devices");
        }

        throw new SourceException("LimeSDR library can't access the device with serial number [" + identity.serial() +
                "] - it may be in use by another application");
    }

    /**
     * Indicates if the LimeSuiteNG entry is a USB device with the vendor and product ID.
     */
    private static boolean isUsbDevice(DeviceEntry entry, int vendor, int product)
    {
        String[] ids = entry.address().split(":");

        if(ids.length != 2 || !entry.media().toUpperCase(Locale.ROOT).startsWith("USB"))
        {
            return false;
        }

        try
        {
            return Integer.parseInt(ids[0].trim(), 16) == vendor && Integer.parseInt(ids[1].trim(), 16) == product;
        }
        catch(NumberFormatException e)
        {
            return false;
        }
    }

    /**
     * Reads the vendor ID, product ID and serial number of the USB device at this controller's bus and port.  The device
     * is closed again before returning so that LimeSuiteNG can open it.
     */
    private UsbIdentity readUsbIdentity() throws SourceException
    {
        try(UsbContext usb = UsbDeviceLookup.createContext())
        {
            UsbDeviceInfo device = UsbDeviceLookup.find(usb, mBus, mPortAddress);
            String serial = null;

            if(device.serialNumberIndex() != 0)
            {
                try(UsbDeviceHandle handle = usb.open(device))
                {
                    //LimeSuiteNG aborts the application when it opens a device that another application is using,
                    //since the open throws a C++ exception through its C API.  Check that the device is free first.
                    try
                    {
                        handle.claimInterface(0);
                        handle.releaseInterface(0);
                    }
                    catch(UsbException e)
                    {
                        if(e.code() == UsbDeviceLookup.ERROR_BUSY)
                        {
                            throw new SourceException("LimeSDR is in use by another application");
                        }
                    }

                    serial = handle.readString(device.serialNumberIndex());
                    serial = serial != null && !serial.isBlank() ? serial : null;
                }
                catch(UsbException e)
                {
                    //Expected on Windows, where libusb can't open a device that uses the Cypress or FTDI driver
                    mLog.debug("Can't open LimeSDR to read its USB serial number - " + LibUsb.errorName(e.code()));
                }
            }

            return new UsbIdentity(device.vendorId(), device.productId(), serial);
        }
    }

    @Override
    public void apply(TunerConfiguration tunerConfiguration) throws SourceException
    {
        if(!(tunerConfiguration instanceof LimeSdrTunerConfiguration config))
        {
            throw new IllegalArgumentException("Invalid tuner config:" + tunerConfiguration.getClass());
        }

        //Saved limits can be wider than this device, since the defaults are for the LimeSDR-USB.
        config.setMinimumFrequency(Math.max(config.getMinimumFrequency(), mMinimumTunableFrequency));
        config.setMaximumFrequency(Math.min(config.getMaximumFrequency() > 0 ? config.getMaximumFrequency() :
                mMaximumTunableFrequency, mMaximumTunableFrequency));

        LimeSdrSampleRate rate = LimeSdrSampleRate.getClosest(mSampleRates, config.getSampleRate());
        setSampleRate(rate);
        config.setSampleRate(rate.getRate());

        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(config);

        String antenna = mAntennas.contains(config.getAntenna()) ? config.getAntenna() : getDefaultAntenna(mAntennas);
        setAntenna(antenna);
        config.setAntenna(antenna);

        int gain = Math.clamp(config.getGain(), GAIN_MINIMUM, GAIN_MAXIMUM);
        setGain(gain);
        config.setGain(gain);
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
                mDevice.setFrequency(Direction.RX, CHANNEL, frequency);
            }
        }
        catch(LimeSdrException e)
        {
            throw new SourceException("Can't tune LimeSDR to [" + frequency + "] - " + e.getMessage(), e);
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
    public List<LimeSdrSampleRate> getSampleRates()
    {
        return mSampleRates;
    }

    /**
     * Current sample rate.
     */
    public LimeSdrSampleRate getLimeSdrSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets the sample rate and the matching analog filter bandwidth.
     * @param rate to set
     * @throws SourceException if the sample rate is locked or can't be set
     */
    public void setSampleRate(LimeSdrSampleRate rate) throws SourceException
    {
        getLock().lock();

        try
        {
            if(rate != mSampleRate && mDevice != null)
            {
                if(isLockedSampleRate())
                {
                    throw new SourceException("Can't change LimeSDR sample rate while channels are allocated");
                }

                //The stream is restarted because LimeSuiteNG sizes it for the sample rate when it starts.  Raising the
                //rate of a running stream limits it to about 38 MHz on a LimeSDR-USB.
                boolean streaming = mStreaming;

                try
                {
                    if(streaming)
                    {
                        clearStream();
                        mDevice.stopRx();
                    }

                    mDevice.setSampleRate(rate.getRate(), 0);
                    mDevice.setLpfBandwidth(Direction.RX, CHANNEL, getLpfBandwidth(rate));
                    mSampleRate = rate;
                    mSamplesPerMillisecond = rate.getRate() / 1000.0f;
                    mFrequencyController.setSampleRate(rate.getRate());
                }
                catch(LimeSdrException e)
                {
                    throw new SourceException("Can't set LimeSDR sample rate to [" + rate + "] - " + e.getMessage(), e);
                }
                finally
                {
                    //Calibrated when the stream starts
                    mCalibrated = false;

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
     * Analog low-pass filter bandwidth for the sample rate, within the filter's range.
     */
    private double getLpfBandwidth(LimeSdrSampleRate rate)
    {
        return Math.clamp(rate.getRate() * LPF_BANDWIDTH_RATIO, mLpfRange.min(), mLpfRange.max());
    }

    /**
     * Receive antenna ports that can be selected.
     */
    public List<String> getAntennas()
    {
        return mAntennas;
    }

    /**
     * Current receive antenna port.
     */
    public String getAntenna()
    {
        return mAntenna;
    }

    /**
     * Selects the receive antenna port.
     * @param antenna name from {@link #getAntennas()}
     * @throws SourceException if the port can't be selected
     */
    public void setAntenna(String antenna) throws SourceException
    {
        getLock().lock();

        try
        {
            if(mDevice != null && !antenna.equals(mAntenna))
            {
                int index = mAntennaPorts.indexOf(antenna);

                if(index < 0 || !mAntennas.contains(antenna))
                {
                    throw new SourceException("LimeSDR doesn't have antenna port [" + antenna + "]");
                }

                mDevice.setAntenna(Direction.RX, CHANNEL, index);
                mAntenna = antenna;
                invalidateCalibration();
            }
        }
        catch(LimeSdrException e)
        {
            throw new SourceException("Can't set LimeSDR antenna to [" + antenna + "] - " + e.getMessage(), e);
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
     * Current gain in dB.
     */
    public int getGain()
    {
        return mGain;
    }

    /**
     * Sets the combined receive gain, which LimeSuiteNG distributes across the LNA, TIA and PGA amplifiers.
     * @param gain in dB, 0 to 73
     * @throws SourceException if the gain can't be set
     */
    public void setGain(int gain) throws SourceException
    {
        getLock().lock();

        try
        {
            mGain = gain;

            if(mDevice != null)
            {
                mDevice.setGain(Direction.RX, CHANNEL, gain);
            }
        }
        catch(LimeSdrException | IllegalArgumentException e)
        {
            throw new SourceException("Can't set LimeSDR gain to [" + gain + "] - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Marks the calibration as out of date, and calibrates now when the sample stream is running.  Otherwise, the
     * calibration is deferred until the stream starts, so that start-up and configuration changes calibrate once.
     */
    private void invalidateCalibration()
    {
        mCalibrated = false;

        if(mStreaming)
        {
            calibrate();
        }
    }

    /**
     * Calibrates the receive chain for the current frequency, sample rate and antenna, and reapplies the gain which
     * the calibration changes.  A failed calibration is logged, since the tuner is still usable without it.
     */
    private void calibrate()
    {
        if(mDevice == null || mSampleRate == null)
        {
            return;
        }

        mIgnoreGapsUntil = Long.MAX_VALUE;

        try
        {
            mDevice.calibrate(Direction.RX, CHANNEL, getLpfBandwidth(mSampleRate));
        }
        catch(LimeSdrException e)
        {
            mLog.warn("LimeSDR calibration failed - " + e.getMessage());
        }
        finally
        {
            mIgnoreGapsUntil = System.nanoTime() + CALIBRATION_GAP_GRACE_NANOSECONDS;
        }

        //Avoid repeating a calibration that failed
        mCalibrated = true;

        try
        {
            mDevice.setGain(Direction.RX, CHANNEL, mGain);
        }
        catch(LimeSdrException e)
        {
            mLog.error("Can't restore LimeSDR gain after calibration - " + e.getMessage());
        }
    }

    /**
     * Device information, available once the tuner has been started.
     */
    public LimeSdrDeviceInformation getDeviceInfo()
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

                if(!mStreaming)
                {
                    startStream();
                }
            }
        }
        catch(SourceException se)
        {
            mLog.error("Error starting streaming on LimeSDR tuner", se);
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

            if(!hasBufferListeners() && mStreaming && mDevice != null)
            {
                clearStream();

                try
                {
                    mDevice.stopRx();
                }
                catch(LimeSdrException | RuntimeException e)
                {
                    mLog.error("Error stopping LimeSDR sample stream", e);
                }
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Starts the sample stream, calibrating first if the settings have changed.
     */
    private void startStream() throws SourceException
    {
        if(!mCalibrated)
        {
            calibrate();
        }

        try
        {
            StreamConfig defaults = StreamConfig.of(CHANNEL, SAMPLE_FORMAT);
            StreamConfig config = new StreamConfig(CHANNEL, SAMPLE_FORMAT, getFifoSize(mSampleRate),
                    defaults.linkFormat(), false, THROUGHPUT_VS_LATENCY);
            mExpectedTimestamp = -1;
            mDroppedSamples.set(0);
            mDevice.startRx(config, BUFFER_SAMPLE_COUNT, this::receive);
            mStreaming = true;
            mStreamCheck = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkStream, STREAM_CHECK_INTERVAL_SECONDS,
                    STREAM_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
        catch(LimeSdrException | RuntimeException e)
        {
            throw new SourceException("Can't start LimeSDR sample stream - " + e.getMessage(), e);
        }
    }

    /**
     * Clears the sample stream state so that it's no longer monitored.  The caller is responsible for stopping it.
     */
    private void clearStream()
    {
        if(mStreamCheck != null)
        {
            mStreamCheck.cancel(false);
            mStreamCheck = null;
        }

        mStreaming = false;
    }

    /**
     * Detects a stream that ended by itself, such as when the device is unplugged, and flags the tuner.
     */
    private void checkStream()
    {
        String message = null;

        getLock().lock();

        try
        {
            if(mStreaming && mDevice != null && !mDevice.isReceiving())
            {
                Throwable failure = mDevice.receiveFailure();
                message = "LimeSDR sample stream stopped - " + (failure != null ? failure.getMessage() : "unknown error");
                clearStream();
            }
        }
        finally
        {
            getLock().unlock();
        }

        long dropped = mDroppedSamples.getAndSet(0);
        float samplesPerMillisecond = mSamplesPerMillisecond;

        if(dropped > 0 && samplesPerMillisecond > 0)
        {
            mLog.warn("LimeSDR dropped [" + dropped + "] samples (" + Math.round(dropped / samplesPerMillisecond) +
                    " ms) - the computer or USB connection isn't keeping up with the sample rate");
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
            countDroppedSamples(buffer);
            int length = buffer.sampleCount() * 2;

            if(mReceiveBuffer.length != length)
            {
                mReceiveBuffer = new short[length];
            }

            buffer.copyShorts(mReceiveBuffer);
            broadcast(new FloatNativeBuffer(scale(mReceiveBuffer), System.currentTimeMillis(), mSamplesPerMillisecond));
        }
        catch(RuntimeException e)
        {
            mLog.error("Error dispatching LimeSDR sample buffer", e);
        }

        return true;
    }

    /**
     * Counts the samples missing before this buffer, from its hardware timestamp.  Called on the stream thread.
     */
    private void countDroppedSamples(SampleBuffer buffer)
    {
        long gap = getTimestampGap(mExpectedTimestamp, buffer.timestamp());

        if(gap > 0 && System.nanoTime() - mIgnoreGapsUntil > 0)
        {
            mDroppedSamples.addAndGet(gap);
        }

        mExpectedTimestamp = buffer.timestamp() + buffer.sampleCount();
    }

    /**
     * Number of samples missing between the expected and actual timestamp of a buffer.
     * @param expected timestamp, or -1 for the first buffer of a stream
     * @param actual timestamp
     * @return missing samples, or zero when there's no gap or the timestamps restarted
     */
    static long getTimestampGap(long expected, long actual)
    {
        return expected >= 0 && actual > expected ? actual - expected : 0;
    }

    /**
     * Scales 12-bit samples to the range -1 to 1 and removes the -0.5 LSB offset of the ADC.
     * @param samples interleaved complex samples
     * @return scaled interleaved complex samples
     */
    static float[] scale(short[] samples)
    {
        float[] scaled = new float[samples.length];

        for(int x = 0; x < samples.length; x++)
        {
            scaled[x] = (samples[x] + SAMPLE_OFFSET) * SAMPLE_SCALE;
        }

        return scaled;
    }
}
