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
package io.github.dsheirer.source.tuner.rtl;

import io.github.dsheirer.libusb.UsbContext;
import io.github.dsheirer.libusb.UsbDeviceHandle;
import io.github.dsheirer.libusb.UsbDeviceInfo;
import io.github.dsheirer.libusb.UsbException;
import io.github.dsheirer.buffer.ByteNativeBufferFactory;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.rtlsdr.DeviceInfo;
import io.github.dsheirer.rtlsdr.RtlSdr;
import io.github.dsheirer.rtlsdr.RtlSdrDevice;
import io.github.dsheirer.rtlsdr.RtlSdrException;
import io.github.dsheirer.rtlsdr.SampleBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.UsbDeviceLookup;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0012.FC0012TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0013.FC0013TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc2580.FC2580TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xTunerConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTL-2832 tuner controller, implemented with the jRTL library (librtlsdr from rtl-sdr-blog).
 *
 * The tuner is discovered with LibUsb at a USB bus and port, like all other USB tuners, however librtlsdr owns the
 * USB device, its transfers, and the embedded tuner chip.  librtlsdr opens devices by index, so the controller
 * finds the index of the device at this controller's bus and port by its USB serial string.
 */
public class RTL2832TunerController extends TunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(RTL2832TunerController.class);
    public static final SampleRate DEFAULT_SAMPLE_RATE = SampleRate.RATE_2_400MHZ;
    public static final long FREQUENCY_DEFAULT = 101_100_000;
    //Bytes per USB transfer, each containing 32,768 complex samples of interleaved unsigned 8-bit I/Q
    private static final int USB_TRANSFER_BUFFER_SIZE = 65_536;
    private static final int USB_TRANSFER_BUFFER_COUNT = 16;
    private static final int EEPROM_SIZE = 256;
    private static final int LIBUSB_ERROR_PIPE = -9;
    private static final int CONTROL_ATTEMPTS = 3;
    //Total IF gain values supported by the E4000 IF gain stages 1-6, in dB
    private static final int[][] E4K_IF_STAGE_GAINS = {{-3, 6}, {0, 3, 6, 9}, {0, 3, 6, 9}, {0, 1, 2},
            {3, 6, 9, 12, 15}, {3, 6, 9, 12, 15}};
    public static final int E4K_IF_GAIN_MINIMUM = 3;
    /**
     * The IF gain stages total up to 56 dB, but the E4000 IF output saturates at about 40 dB of total IF gain, even
     * with the minimum LNA gain.  Above that, the output level stays flat and then falls, by about 20 dB with all stages
     * at maximum, so the IF gain is limited to 42 dB.
     */
    public static final int E4K_IF_GAIN_MAXIMUM = 42;

    private final int mBus;
    private final String mPortAddress;
    private final ByteNativeBufferFactory mNativeBufferFactory = new ByteNativeBufferFactory();
    private RtlSdrDevice mDevice;
    private Descriptor mDescriptor;
    private TunerType mTunerType = TunerType.UNKNOWN;
    private int mUsbSerialNumberIndex = 0;
    private SampleRate mSampleRate = DEFAULT_SAMPLE_RATE;
    private int[] mTunerGains = new int[0];
    private boolean mBiasTEnabled = false;
    private boolean mStreaming = false;

    /**
     * Constructs an instance
     * @param bus usb
     * @param portAddress usb
     * @param tunerErrorListener to receive error notifications from this controller
     */
    public RTL2832TunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
    }

    @Override
    public int getBufferSampleCount()
    {
        return USB_TRANSFER_BUFFER_SIZE / 2;
    }

    @Override
    public void start() throws SourceException
    {
        int index = findDeviceIndex();

        getLock().lock();

        try
        {
            mDevice = RtlSdr.open(index);
            mDescriptor = readDescriptor(mDevice);
            mTunerType = getTunerType(mDevice.tunerType());

            switch(mTunerType)
            {
                case ELONICS_E4000:
                    configureTunerLimits(E4KTunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ,
                            E4KTunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ,
                            E4KTunerConfiguration.DC_SPIKE_AVOID_BUFFER, E4KTunerConfiguration.USABLE_BANDWIDTH_PERCENT);
                    break;
                case FCI_FC2580:
                    configureTunerLimits(FC2580TunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ,
                            FC2580TunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ,
                            FC2580TunerConfiguration.DC_SPIKE_AVOID_BUFFER, FC2580TunerConfiguration.USABLE_BANDWIDTH_PERCENT);
                    break;
                case FITIPOWER_FC0012:
                    configureTunerLimits(FC0012TunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ,
                            FC0012TunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ,
                            FC0012TunerConfiguration.DC_SPIKE_AVOID_BUFFER, FC0012TunerConfiguration.USABLE_BANDWIDTH_PERCENT);
                    break;
                case FITIPOWER_FC0013:
                    configureTunerLimits(FC0013TunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ,
                            FC0013TunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ,
                            FC0013TunerConfiguration.DC_SPIKE_AVOID_BUFFER, FC0013TunerConfiguration.USABLE_BANDWIDTH_PERCENT);
                    break;
                case RAFAELMICRO_R820T:
                case RAFAELMICRO_R828D:
                    configureTunerLimits(R8xTunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ,
                            R8xTunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ,
                            R8xTunerConfiguration.DC_SPIKE_AVOID_BUFFER, R8xTunerConfiguration.USABLE_BANDWIDTH_PERCENT);
                    break;
                default:
                    throw new SourceException("Unsupported RTL-2832 embedded tuner type: " + mDevice.tunerType());
            }

            mTunerGains = mDevice.tunerGains();

            //Frequency correction is applied by the frequency controller and the channelizer, not by librtlsdr
            mDevice.setFrequencyCorrection(0);
            //Tune before setting the sample rate, otherwise librtlsdr retunes to 0 Hz when it sets the sample rate
            setFrequency(FREQUENCY_DEFAULT);
            setSampleRate(DEFAULT_SAMPLE_RATE);
        }
        catch(RtlSdrException | SourceException | RuntimeException | LinkageError e)
        {
            closeDevice(mDevice);
            mDevice = null;
            mTunerType = TunerType.UNKNOWN;
            throw new SourceException("Can't start RTL-2832 tuner - " + e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void stop()
    {
        RtlSdrDevice device;

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
     * Applies the frequency limits and usable bandwidth for the embedded tuner type.
     */
    private void configureTunerLimits(long minimum, long maximum, int dcSpikeHalfBandwidth, double usableBandwidth)
    {
        setMinimumFrequency(minimum);
        setMaximumFrequency(maximum);
        setMiddleUnusableHalfBandwidth(dcSpikeHalfBandwidth);
        setUsableBandwidthPercentage(usableBandwidth);
    }

    /**
     * Closes the device, which also stops streaming.  The bias-T is turned off first, since librtlsdr leaves the
     * GPIO state unchanged when the device is closed.
     * @param device to close, or null
     */
    private void closeDevice(RtlSdrDevice device)
    {
        if(device != null)
        {
            try
            {
                device.stopRx();
                device.setBiasTee(false);
            }
            catch(RtlSdrException | RuntimeException e)
            {
                mLog.error("Error turning off RTL-2832 bias-T", e);
            }

            try
            {
                device.close();
            }
            catch(RtlSdrException | RuntimeException e)
            {
                mLog.error("Error closing RTL-2832 device", e);
            }
        }
    }

    /**
     * Maps the librtlsdr tuner type to the sdrtrunk tuner type.
     */
    private static TunerType getTunerType(io.github.dsheirer.rtlsdr.Tuner tuner)
    {
        return switch(tuner)
        {
            case E4000 -> TunerType.ELONICS_E4000;
            case FC0012 -> TunerType.FITIPOWER_FC0012;
            case FC0013 -> TunerType.FITIPOWER_FC0013;
            case FC2580 -> TunerType.FCI_FC2580;
            case R820T -> TunerType.RAFAELMICRO_R820T;
            case R828D -> TunerType.RAFAELMICRO_R828D;
            default -> TunerType.UNKNOWN;
        };
    }

    /**
     * Reads the 256-byte EEPROM descriptor.
     * @return descriptor or null if the EEPROM can't be read
     */
    private static Descriptor readDescriptor(RtlSdrDevice device)
    {
        try
        {
            return new Descriptor(device.readEeprom(0, EEPROM_SIZE));
        }
        catch(RtlSdrException | RuntimeException e)
        {
            mLog.error("Error reading the RTL-2832 EEPROM device descriptor", e);
        }

        return null;
    }

    /**
     * Finds the librtlsdr index of the device at this controller's USB bus and port.
     *
     * librtlsdr enumerates devices in the order of the libusb device list and doesn't expose the bus or port of a
     * device, so the device is matched by its USB serial string.  When more than one device has the same serial
     * string (some dongles share a serial of 00000001), the device is matched by its position among the devices
     * with that serial.  A device without a readable serial is matched by its position among all RTL-2832 devices.
     *
     * @return device index for librtlsdr
     * @throws SourceException if the device isn't found or can't be accessed.
     */
    private int findDeviceIndex() throws SourceException
    {
        UsbIdentity identity = readUsbIdentity();
        List<DeviceInfo> devices = RtlSdr.devices();

        if(devices.isEmpty())
        {
            throw new SourceException("librtlsdr didn't find any RTL-2832 devices");
        }

        if(identity.serial() != null && !identity.serial().isEmpty())
        {
            List<DeviceInfo> matches = devices.stream().filter(d -> identity.serial().equals(d.usbStrings().serial()))
                    .toList();

            if(identity.serialPosition() < matches.size())
            {
                return matches.get(identity.serialPosition()).index();
            }
        }

        if(identity.position() < devices.size())
        {
            return devices.get(identity.position()).index();
        }

        throw new SourceException("Couldn't find the librtlsdr device for USB bus [" + mBus + "] port [" +
                mPortAddress + "]");
    }

    /**
     * USB identity of this controller's device: its serial string, its position among the RTL-2832 devices with the
     * same serial string, and its position among all RTL-2832 devices, in libusb device list order.
     */
    private record UsbIdentity(String serial, int serialPosition, int position) {}

    /**
     * Reads the USB identity of the device at this controller's USB bus and port.  Devices are closed again before
     * returning so that librtlsdr can open them.
     */
    private UsbIdentity readUsbIdentity() throws SourceException
    {
        try(UsbContext usb = UsbDeviceLookup.createContext())
        {
            //RTL-2832 devices that precede this controller's device in the device list
            List<UsbDeviceInfo> earlierDevices = new ArrayList<>();

            for(UsbDeviceInfo device : UsbDeviceLookup.devices(usb))
            {
                if(TunerClass.lookup((short)device.vendorId(), (short)device.productId()) != TunerClass.RTL2832)
                {
                    continue;
                }

                if(UsbDeviceLookup.isAt(device, mBus, mPortAddress))
                {
                    mUsbSerialNumberIndex = device.serialNumberIndex();
                    String serial = readSerial(usb, device, true);
                    int serialPosition = 0;

                    if(serial != null)
                    {
                        for(UsbDeviceInfo earlierDevice : earlierDevices)
                        {
                            if(serial.equals(readSerial(usb, earlierDevice, false)))
                            {
                                serialPosition++;
                            }
                        }
                    }

                    return new UsbIdentity(serial, serialPosition, earlierDevices.size());
                }

                earlierDevices.add(device);
            }
        }

        throw new SourceException("Couldn't find USB device at bus [" + mBus + "] port [" + mPortAddress + "]");
    }

    /**
     * Reads the device's USB serial string.
     * @param usb context
     * @param device to read
     * @param required true to throw an exception when the device can't be opened, false to return null
     * @return serial or null if the device doesn't have a serial string
     * @throws SourceException if the device is required and it can't be opened.
     */
    private static String readSerial(UsbContext usb, UsbDeviceInfo device, boolean required) throws SourceException
    {
        if(!required)
        {
            if(device.serialNumberIndex() == 0)
            {
                return null;
            }

            try(UsbDeviceHandle handle = usb.open(device))
            {
                return handle.readString(device.serialNumberIndex());
            }
            catch(UsbException e)
            {
                return null;
            }
        }

        return UsbDeviceLookup.readSerialNumber(usb, device);
    }

    /**
     * Operation that requires the device.
     */
    private interface DeviceOperation
    {
        void execute(RtlSdrDevice device) throws RtlSdrException;
    }

    /**
     * Executes the operation under the controller lock so that the device can't be closed while it is in use.  The
     * operation is ignored when the device isn't open, which allows for calls that arrive during tuner shutdown.
     *
     * While the device is streaming, the RTL2832 stalls the first vendor OUT control transfer that follows an idle
     * period of more than about 100-300 ms (LIBUSB_ERROR_PIPE).  The librtlsdr bundled with jRTL 1.4.0.1 and later
     * reads a register before those writes, which prevents the stall.  Operations that still fail with a pipe error
     * are retried as a backstop, since the librtlsdr control functions set the complete state on each call.
     *
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
                for(int attempt = 1; ; attempt++)
                {
                    try
                    {
                        operation.execute(mDevice);
                        return;
                    }
                    catch(RtlSdrException e)
                    {
                        if(e.code() != LIBUSB_ERROR_PIPE || attempt >= CONTROL_ATTEMPTS)
                        {
                            throw e;
                        }
                    }
                }
            }
        }
        catch(RtlSdrException | RuntimeException e)
        {
            throw new SourceException(e.getMessage(), e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Receives samples on the librtlsdr thread and dispatches them to the buffer listeners.  Exceptions are
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
            mLog.error("Error dispatching RTL-2832 sample buffer", e);
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
            execute(device -> device.startRx(this::receive, USB_TRANSFER_BUFFER_COUNT, USB_TRANSFER_BUFFER_SIZE));
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
            execute(RtlSdrDevice::stopRx);
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
            mLog.error("Error starting streaming on RTL-2832 tuner", se);
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
            mLog.error("Error stopping streaming on RTL-2832 tuner", se);
        }
        finally
        {
            getLock().unlock();
        }
    }

    @Override
    public void apply(TunerConfiguration tunerConfig) throws SourceException
    {
        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(tunerConfig);

        if(tunerConfig instanceof RTL2832TunerConfiguration rtl)
        {
            setSampleRate(rtl.getSampleRate());
            setBiasT(rtl.isBiasT());
            setGain(rtl.isAGC(), rtl.getGain());

            if(rtl instanceof E4KTunerConfiguration e4k)
            {
                setE4KIFGain(e4k.getIFGainDb());
            }
        }
    }

    /**
     * Sets the tuned frequency
     * @param frequency in Hertz
     * @throws SourceException if there is an error
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        execute(device -> device.setCenterFrequency(frequency));
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mFrequencyController.getTunedFrequency();
    }

    /**
     * Sets the enabled state of the bias-t
     * @param enabled true to turn-on the bias-t or false to turn-off the bias-t.
     */
    public void setBiasT(boolean enabled)
    {
        try
        {
            execute(device -> device.setBiasTee(enabled));
            mBiasTEnabled = enabled;
        }
        catch(SourceException se)
        {
            mLog.error("Error setting RTL-2832 bias-T to " + (enabled ? "on" : "off"), se);
        }
    }

    /**
     * Indicates if the bias-t is enabled.
     * @return true if enabled.
     */
    public boolean isBiasT()
    {
        return mBiasTEnabled;
    }

    /**
     * Gains supported by the embedded tuner, in tenths of a dB from lowest to highest.  Available once the tuner has
     * been started.
     */
    public int[] getTunerGains()
    {
        return mTunerGains;
    }

    /**
     * Supported tuner gain that is closest to the requested gain.
     * @param gain in tenths of a dB
     * @return closest supported gain, or the requested gain if the tuner doesn't report its gains.
     */
    public int getClosestTunerGain(int gain)
    {
        int closest = gain;
        int distance = Integer.MAX_VALUE;

        for(int supported : mTunerGains)
        {
            if(Math.abs(supported - gain) < distance)
            {
                closest = supported;
                distance = Math.abs(supported - gain);
            }
        }

        return closest;
    }

    /**
     * Sets the tuner gain mode and manual gain.
     * @param agc true for the tuner's automatic gain control, false for manual gain
     * @param gain in tenths of a dB, applied when AGC is off.  The gain is rounded to the closest supported gain,
     * since the librtlsdr E4000 driver rejects gains that aren't in its gain table.
     * @throws SourceException if the gain can't be set
     */
    public void setGain(boolean agc, int gain) throws SourceException
    {
        if(!hasGainControl())
        {
            return;
        }

        boolean tunerAgc = agc && hasTunerAGC();
        int supported = getClosestTunerGain(gain);

        execute(device -> {
            device.setTunerGainMode(!tunerAgc);

            if(!tunerAgc)
            {
                device.setTunerGain(supported);
            }
        });
    }

    /**
     * Indicates if the embedded tuner supports manual gain control.  The librtlsdr FC2580 driver doesn't, and the
     * tuner always uses its internal AGC.
     */
    public boolean hasGainControl()
    {
        return mTunerGains != null && mTunerGains.length > 1;
    }

    /**
     * Indicates if the embedded tuner supports switching to its automatic gain control.  The librtlsdr FC0012 and
     * FC2580 drivers ignore the gain mode, so the FC0012 always uses manual gain.
     */
    public boolean hasTunerAGC()
    {
        return hasGainControl() && mTunerType != TunerType.FITIPOWER_FC0012;
    }

    /**
     * Sets the E4000 total IF gain, distributed across the six IF gain stages.  Ignored for other tuner types.
     * @param gain in dB
     * @throws SourceException if the gain can't be set
     */
    public void setE4KIFGain(int gain) throws SourceException
    {
        if(mTunerType == TunerType.ELONICS_E4000)
        {
            int[] stages = getE4KIFStageGains(Math.clamp(gain, E4K_IF_GAIN_MINIMUM, E4K_IF_GAIN_MAXIMUM));

            execute(device -> {
                for(int stage = 0; stage < stages.length; stage++)
                {
                    device.setTunerIfGain(stage + 1, stages[stage] * 10);
                }
            });
        }
    }

    /**
     * Distributes a total E4000 IF gain across the six IF gain stages, choosing the combination closest to the
     * requested gain and, among equally close combinations, the one with the most gain in the earliest stages for the
     * best noise figure.
     * @param gain total in dB
     * @return gain for stages 1-6 in dB
     */
    public static int[] getE4KIFStageGains(int gain)
    {
        int[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        int[] indices = new int[E4K_IF_STAGE_GAINS.length];

        while(true)
        {
            int[] candidate = new int[indices.length];
            int total = 0;

            for(int stage = 0; stage < indices.length; stage++)
            {
                candidate[stage] = E4K_IF_STAGE_GAINS[stage][indices[stage]];
                total += candidate[stage];
            }

            int distance = Math.abs(total - gain);

            if(distance < bestDistance || (distance == bestDistance && Arrays.compare(candidate, best) > 0))
            {
                best = candidate;
                bestDistance = distance;
            }

            //Advance to the next combination
            int stage = indices.length - 1;

            while(stage >= 0 && ++indices[stage] == E4K_IF_STAGE_GAINS[stage].length)
            {
                indices[stage] = 0;
                stage--;
            }

            if(stage < 0)
            {
                return best;
            }
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mSampleRate.getRate();
    }

    /**
     * Sets the sampling rate of the RTL2832.  librtlsdr also sets the tuner's bandwidth for the sample rate.
     * @param sampleRate to apply
     * @throws SourceException if the sample rate can't be set
     */
    public void setSampleRate(SampleRate sampleRate) throws SourceException
    {
        getLock().lock();

        try
        {
            execute(device -> device.setSampleRate(sampleRate.getRate()));
            mSampleRate = sampleRate;
            mFrequencyController.setSampleRate(sampleRate.getRate());

            //Updates native buffer factory so that sample buffers can be accurately timestamped
            mNativeBufferFactory.setSamplesPerMillisecond(sampleRate.getRate() / 1000.0f);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Descriptor contains all identifiers and labels parsed from the EEPROM.
     *
     * May return null if unable to get 256 byte eeprom descriptor from tuner or if the descriptor doesn't begin with
     * byte values of 0x28 and 0x32 meaning it is a valid (and can be parsed) RTL2832 descriptor
     */
    public Descriptor getDescriptor()
    {
        if(mDescriptor != null && mDescriptor.isValid())
        {
            return mDescriptor;
        }

        return null;
    }

    /**
     * Provides a unique identifier to use in distinctly identifying this tuner from among other tuners of the same
     * type, so that we can fetch a tuner configuration from the settings manager for this specific tuner.
     *
     * @return serial number of the device
     */
    public String getUniqueID()
    {
        if(mDescriptor != null)
        {
            if(mDescriptor.hasSerial())
            {
                if(mTunerType != TunerType.UNKNOWN)
                {
                    return TunerClass.RTL2832 + "/" + getTunerType().getLabel() + " " + mDescriptor.getSerial();
                }
                else
                {
                    return TunerClass.RTL2832 + " " + mDescriptor.getSerial();
                }
            }
            else
            {
                return "RTL-2832 USB Bus:" + mBus + " Port:" + mPortAddress;
            }
        }
        else
        {
            return TunerClass.RTL2832 + " " + mUsbSerialNumberIndex;
        }
    }

    /**
     * Identifies the embedded tuner type
     */
    @Override
    public TunerType getTunerType()
    {
        return mTunerType;
    }

    /**
     * RTL2832 sample rates supported by librtlsdr: 225,001 to 300,000 and 900,001 to 3,200,000 Hz.
     */
    public enum SampleRate
    {
        /* Note: sample rates below 1.0MHz are subject to aliasing */
        RATE_0_230MHZ(230400, "0.230 MHz"),
        RATE_0_240MHZ(240000, "0.240 MHz"),
        RATE_0_256MHZ(256000, "0.256 MHz"),
        RATE_0_288MHZ(288000, "0.288 MHz"),
        RATE_0_300MHZ(300000, "0.300 MHz"),
        RATE_0_960MHZ(960000, "0.960 MHz"),
        RATE_1_024MHZ(1024000, "1.024 MHz"),
        RATE_1_200MHZ(1200000, "1.200 MHz"),
        RATE_1_440MHZ(1440000, "1.440 MHz"),
        RATE_1_600MHZ(1600000, "1.600 MHz"),
        RATE_1_800MHZ(1800000, "1.800 MHz"),
        RATE_1_920MHZ(1920000, "1.920 MHz"),
        RATE_2_048MHZ(2048000, "2.048 MHz"),
        RATE_2_304MHZ(2304000, "2.304 MHz"),
        RATE_2_400MHZ(2400000, "2.400 MHz"),
        RATE_2_560MHZ(2560000, "2.560 MHz"),
        RATE_2_880MHZ(2880000, "2.880 MHz");

        private int mRate;
        private String mLabel;

        SampleRate(int rate, String label)
        {
            mRate = rate;
            mLabel = label;
        }

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
    }

    /**
     * RTL2832 EEPROM byte array descriptor parsing class.  The 256-byte EEPROM contents are as follows (in hex):
     *
     * 00/01 - 2832 Signature
     * 03/02 - 0BDA Vendor ID
     * 05/04 - 2832 Product ID
     * 06 - A5 (has serial id?)
     * 07 - 16 (bit field - bit 0 = remote wakeup, bit 1 = IR enabled
     * 08 - 02 or 12
     * 10/09 0310 ETX(0x03) plus label length (includes length and ETX bytes)
     * 12/11 First UTF-16 character
     * 14/13 Second UTF-16 character ...
     *
     * Label 1: vendor
     * Label 2: product
     * Label 3: serial
     * Label 4,5 ... (user defined)
     */
    public static class Descriptor
    {
        private final byte[] mData;
        private final List<String> mLabels = new ArrayList<>();

        public Descriptor(byte[] data)
        {
            mData = Arrays.copyOf(data, EEPROM_SIZE);
            getLabels();
        }

        public boolean isValid()
        {
            return mData[0] != (byte)0x0 && mData[1] != (byte)0x0;
        }

        public String getVendorID()
        {
            int id = Integer.rotateLeft((0xFF & mData[3]), 8) | (0xFF & mData[2]);
            return String.format("%04X", id);
        }

        public String getVendorLabel()
        {
            return getLabel(0);
        }

        public String getProductID()
        {
            int id = Integer.rotateLeft((0xFF & mData[5]), 8) | (0xFF & mData[4]);
            return String.format("%04X", id);
        }

        public String getProductLabel()
        {
            return getLabel(1);
        }

        public boolean hasSerial()
        {
            return mData[6] == (byte) 0xA5;
        }

        public String getSerial()
        {
            return getLabel(2);
        }

        public boolean remoteWakeupEnabled()
        {
            byte mask = (byte) 0x01;
            return (mData[7] & mask) == mask;
        }

        public boolean irEnabled()
        {
            byte mask = (byte) 0x02;
            return (mData[7] & mask) == mask;
        }

        private String getLabel(int index)
        {
            return index < mLabels.size() ? mLabels.get(index) : "";
        }

        private void getLabels()
        {
            mLabels.clear();
            int start = 0x09;
            while(start < EEPROM_SIZE)
            {
                start = parseLabel(start);
            }
        }

        private int parseLabel(int start)
        {
            /* Validate length and check second byte for ETX (0x03) */
            if(start > 254 || mData[start + 1] != (byte) 0x03)
            {
                return EEPROM_SIZE;
            }

            /* Get label length, including the length and ETX bytes */
            int length = 0xFF & mData[start];

            if(length < 2 || start + length > 255)
            {
                return EEPROM_SIZE;
            }

            /* Translate the bytes as UTF-16 Little Endian and store the label */
            byte[] data = Arrays.copyOfRange(mData, start + 2, start + length);
            mLabels.add(new String(data, StandardCharsets.UTF_16LE));
            return start + length;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("RTL-2832 EEPROM Descriptor\n");
            sb.append("Vendor: ").append(getVendorID()).append(" [").append(getVendorLabel()).append("]\n");
            sb.append("Product: ").append(getProductID()).append(" [").append(getProductLabel()).append("]\n");
            sb.append("Serial: ");
            if(hasSerial())
            {
                sb.append("yes [").append(getSerial()).append("]\n");
            }
            else
            {
                sb.append("no\n");
            }
            sb.append("Remote Wakeup Enabled: ").append(remoteWakeupEnabled() ? "yes" : "no").append("\n");
            sb.append("IR Enabled: ").append(irEnabled() ? "yes" : "no").append("\n");

            if(mLabels.size() > 3)
            {
                sb.append("Additional Labels: ");

                for(int x = 3; x < mLabels.size(); x++)
                {
                    sb.append(" [").append(mLabels.get(x)).append("]");
                }

                sb.append("\n");
            }

            return sb.toString();
        }
    }
}
