package io.github.dsheirer.source.tuner.airspy;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.usb.USBTransferProcessor;
import io.github.dsheirer.source.tuner.usb.USBTunerController;
import org.apache.commons.io.EndianUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import javax.usb.UsbException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * SDR Trunk
 * Copyright (C) 2015-2018 Dennis Sheirer
 *
 * -----------------------------------------------------------------------------
 * Ported from libairspy at:
 * https://github.com/airspy/host/tree/master/libairspy
 *
 * Copyright (c) 2013, Michael Ossmann <mike@ossmann.com>
 * Copyright (c) 2012, Jared Boone <jared@sharebrained.com>
 * Copyright (c) 2014, Youssef Touil <youssef@airspy.com>
 * Copyright (c) 2014, Benjamin Vernoux <bvernoux@airspy.com>
 * Copyright (c) 2015, Ian Gilmour <ian@sdrsharp.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of AirSpy nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

public class AirspyTunerController extends USBTunerController
{
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
    public static final long FREQUENCY_MIN = 24000000l;
    public static final long FREQUENCY_MAX = 1800000000l;
    public static final long FREQUENCY_DEFAULT = 101100000;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.90;
    public static final AirspySampleRate DEFAULT_SAMPLE_RATE = new AirspySampleRate(0, 10000000, "10.00 MHz");

    private static final long USB_TIMEOUT_MS = 2000l; //milliseconds
    private static final byte USB_INTERFACE = (byte) 0x0;
    private static final int USB_TRANSFER_BUFFER_SIZE = 262144;
    private static final byte USB_REQUEST_IN = (byte) (LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE);
    private static final byte USB_REQUEST_OUT = (byte) (LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE);
    public static final DecimalFormat MHZ_FORMATTER = new DecimalFormat("#.00 MHz");

    private final static Logger mLog = LoggerFactory.getLogger(AirspyTunerController.class);

    private Device mDevice;
    private DeviceHandle mDeviceHandle;

    private AirspySampleConverter mSampleAdapter = new AirspySampleConverter();
    private AirspyDeviceInformation mDeviceInfo;
    private List<AirspySampleRate> mSampleRates = new ArrayList<>();
    private int mSampleRate = 0;
    private USBTransferProcessor mUSBTransferProcessor;

    public AirspyTunerController(Device device) throws SourceException
    {
        super(FREQUENCY_MIN, FREQUENCY_MAX, 0, USABLE_BANDWIDTH_PERCENT);

        mDevice = device;
    }

    @Override
    public int getBufferSampleCount()
    {
        //Each airspy complex sample is 4 bytes, so sample count is buffer size / 4
        return USB_TRANSFER_BUFFER_SIZE / 4;
    }

    public void init() throws SourceException
    {
        mDeviceHandle = new DeviceHandle();

        int result = LibUsb.open(mDevice, mDeviceHandle);

        if(result != LibUsb.SUCCESS)
        {
            if(result == LibUsb.ERROR_ACCESS)
            {
                mLog.error("Unable to access Airspy - insufficient permissions."
                    + "  If you are running a Linux OS, have you installed the "
                    + "airspy rules file in \\etc\\udev\\rules.d ??");
            }

            throw new SourceException("Couldn't open airspy device - " + LibUsb.strError(result));
        }

        try
        {
            claimInterface();
        }
        catch(Exception e)
        {
            throw new SourceException("Airspy Tuner Controller - error while "
                + "setting USB configuration, claiming USB interface or "
                + "reset the kernel mode driver", e);
        }

        try
        {
            setSamplePacking(false);
        }
        catch(LibUsbException | UsbException | UnsupportedOperationException e)
        {
            mLog.info("Sample packing is not supported by airspy firmware");
        }

        try
        {
            setReceiverMode(true);
        }
        catch(Exception e)
        {
            mLog.error("Couldn't enable airspy receiver mode", e);
        }

        setFrequency(FREQUENCY_DEFAULT);

        try
        {
            determineAvailableSampleRates();
        }
        catch(LibUsbException | UsbException e)
        {
            mLog.error("Error identifying available samples rates", e);
        }

        try
        {
            setSampleRate(mSampleRates.get(0));
        }
        catch(IllegalArgumentException | LibUsbException | UsbException e)
        {
            mLog.error("Setting sample rate is not supported by firmware", e);
        }

        String deviceName = "Airspy " + getDeviceInfo().getSerialNumber();

        mUSBTransferProcessor = new USBTransferProcessor(deviceName, mDeviceHandle, mSampleAdapter,
            USB_TRANSFER_BUFFER_SIZE);
    }

    @Override
    public void dispose()
    {
        if(mDeviceHandle != null)
        {
            mLog.info("Releasing Airspy Tuner - " + getDeviceInfo().getSerialNumber());
            try
            {
                LibUsb.close(mDeviceHandle);
            }
            catch(Exception e)
            {
                mLog.error("error while closing device handle", e);
            }

            mDeviceHandle = null;
        }
    }

    @Override
    protected USBTransferProcessor getUSBTransferProcessor()
    {
        return mUSBTransferProcessor;
    }


    /**
     * Claims the USB interface.  If another application currently has
     * the interface claimed, the USB_FORCE_CLAIM_INTERFACE setting
     * will dictate if the interface is forcibly claimed from the other
     * application
     */
    private void claimInterface() throws SourceException
    {
        if(mDeviceHandle != null)
        {
            int result = LibUsb.kernelDriverActive(mDeviceHandle, USB_INTERFACE);

            if(result == 1)
            {

                result = LibUsb.detachKernelDriver(mDeviceHandle, USB_INTERFACE);

                if(result != LibUsb.SUCCESS)
                {
                    mLog.error("failed attempt to detach kernel driver [" +
                        LibUsb.errorName(result) + "]");

                    throw new SourceException("couldn't detach kernel driver "
                        + "from device");
                }
            }

            result = LibUsb.setConfiguration(mDeviceHandle, 1);

            if(result != LibUsb.SUCCESS)
            {
                throw new SourceException("couldn't set USB configuration 1 [" +
                    LibUsb.errorName(result) + "]");
            }

            result = LibUsb.claimInterface(mDeviceHandle, USB_INTERFACE);

            if(result != LibUsb.SUCCESS)
            {
                throw new SourceException("couldn't claim usb interface [" +
                    LibUsb.errorName(result) + "]");
            }
        }
        else
        {
            throw new SourceException("couldn't claim usb interface - no "
                + "device handle");
        }
    }

    public static String getTransferStatus(int status)
    {
        switch(status)
        {
            case 0:
                return "TRANSFER COMPLETED (0)";
            case 1:
                return "TRANSFER ERROR (1)";
            case 2:
                return "TRANSFER TIMED OUT (2)";
            case 3:
                return "TRANSFER CANCELLED (3)";
            case 4:
                return "TRANSFER STALL (4)";
            case 5:
                return "TRANSFER NO DEVICE (5)";
            case 6:
                return "TRANSFER OVERFLOW (6)";
            default:
                return "UNKNOWN TRANSFER STATUS (" + status + ")";
        }
    }


    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof AirspyTunerConfiguration)
        {
            AirspyTunerConfiguration airspy = (AirspyTunerConfiguration) config;

            int sampleRate = airspy.getSampleRate();

            AirspySampleRate rate = getSampleRate(sampleRate);

            if(rate == null)
            {
                if(!mSampleRates.isEmpty())
                {
                    rate = mSampleRates.get(0);
                }
                else
                {
                    rate = DEFAULT_SAMPLE_RATE;
                }
            }

            try
            {
                setSampleRate(rate);
            }
            catch(UsbException e)
            {
                throw new SourceException("Couldn't set sample rate [" + rate.toString() + "]", e);
            }

            try
            {
                setIFGain(airspy.getIFGain());
                setMixerGain(airspy.getMixerGain());
                setLNAGain(airspy.getLNAGain());

                setMixerAGC(airspy.isMixerAGC());
                setLNAAGC(airspy.isLNAAGC());

                //Set the gain mode last, so custom values are already set, and
                //linearity and sensitivity modes will automatically override
                //the custom values.
                setGain(airspy.getGain());

                setFrequencyCorrection(airspy.getFrequencyCorrection());
            }
            catch(Exception e)
            {
                throw new SourceException("Couldn't apply gain settings from airspy config", e);
            }

            try
            {
                setFrequency(airspy.getFrequency());
            }
            catch(SourceException se)
            {
                //Do nothing, we couldn't set the frequency
            }

        }
        else
        {
            throw new IllegalArgumentException("Invalid tuner config:" +
                config.getClass());
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
        if(FREQUENCY_MIN <= frequency && frequency <= FREQUENCY_MAX)
        {
            ByteBuffer buffer = ByteBuffer.allocateDirect(4);

            buffer.order(ByteOrder.LITTLE_ENDIAN);

            buffer.putInt((int) frequency);

            buffer.rewind();

            try
            {
                write(Command.SET_FREQUENCY, 0, 0, buffer);
            }
            catch(UsbException e)
            {
                mLog.error("error setting frequency [" + frequency + "]", e);

                throw new SourceException("error setting frequency [" +
                    frequency + "]", e);
            }
        }
        else
        {
            throw new SourceException("Frequency [" + frequency + "] outside "
                + "of tunable range " + FREQUENCY_MIN + "-" + FREQUENCY_MAX);
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets the sample rate to the rate specified by the index value in the
     * available sample rates map
     *
     * @param rate to a sample rate in the available samples rates map.
     * @throws IllegalArgumentException if index is not a valid rate index
     * @throws LibUsbException          if there was a read error or if this operation
     *                                  is not supported by the current firmware
     * @throws UsbException             if there was a USB error
     */
    public void setSampleRate(AirspySampleRate rate) throws LibUsbException, UsbException, SourceException
    {
        if(rate.getRate() != mSampleRate)
        {
            int result = readByte(Command.SET_SAMPLE_RATE, 0, rate.getIndex(), true);

            if(result != 1)
            {
                throw new UsbException("Error setting sample rate [" + rate + "] rate - return value [" + result + "]");
            }
            else
            {
                mSampleRate = rate.getRate();
                mFrequencyController.setSampleRate(mSampleRate);
            }
        }
    }

    /**
     * Returns a list of sample rates supported by the firmware version
     */
    public List<AirspySampleRate> getSampleRates()
    {
        return mSampleRates;
    }

    /**
     * Airspy sample rate object that matches the current sample rate setting.
     */
    public AirspySampleRate getAirspySampleRate()
    {
        return getSampleRate(mSampleRate);
    }

    /**
     * Airspy sample rate object that matches the specified rate in hertz, or
     * null if there are no available sample rates for the tuner that match the
     * argument value.
     */
    public AirspySampleRate getSampleRate(int rate)
    {
        for(AirspySampleRate sampleRate : mSampleRates)
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
     * Enables/Disables sample packing to allow two 12-bit samples to be packed
     * into 3 bytes (enabled) or 4 bytes (disabled).
     *
     * @param enabled
     * @throws UsbException if sample packing is not supported by the current
     *                      device firmware or if there were usb communication issues
     */
    public void setSamplePacking(boolean enabled)
        throws LibUsbException, UsbException
    {
        int result = readByte(Command.SET_PACKING, 0, (enabled ? 1 : 0), true);

        if(result != 1)
        {
            throw new UsbException("Couldnt set sample packing enabled: " + enabled);
        }

        /* If we didn't throw an exception above, then update the sample adapter
         * to process samples accordingly */
        mSampleAdapter.setSamplePacking(enabled);
    }

    /**
     * Enables/disables the mixer automatic gain setting
     *
     * @param enabled
     * @throws LibUsbException on unsuccessful read operation
     * @throws UsbException    on USB error
     */
    public void setMixerAGC(boolean enabled)
        throws LibUsbException, UsbException
    {
        int result = readByte(Command.SET_MIXER_AGC, 0, (enabled ? 1 : 0), true);

        if(result != LibUsb.SUCCESS)
        {
            throw new UsbException("Couldnt set mixer AGC enabled: " + enabled);
        }
    }

    /**
     * Enables/disables the low noise amplifier automatic gain setting
     *
     * @param enabled
     * @throws LibUsbException on unsuccessful read operation
     * @throws UsbException    on USB error
     */
    public void setLNAAGC(boolean enabled) throws LibUsbException, UsbException
    {
        int result = readByte(Command.SET_LNA_AGC, 0, (enabled ? 1 : 0), true);

        if(result != LibUsb.SUCCESS)
        {
            throw new UsbException("Couldnt set LNA AGC enabled: " + enabled);
        }
    }

    public void setGain(Gain gain) throws UsbException
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
     * @throws LibUsbException          on error in java USB wrapper
     * @throws UsbException             on error in USB transfer
     * @throws IllegalArgumentException if gain value is invalid
     */
    public void setLNAGain(int gain)
        throws LibUsbException, UsbException, IllegalArgumentException
    {
        if(LNA_GAIN_MIN <= gain && gain <= LNA_GAIN_MAX)
        {
            int result = readByte(Command.SET_LNA_GAIN, 0, gain, true);

            if(result != LibUsb.SUCCESS)
            {
                throw new UsbException("Couldnt set LNA gain to: " + gain);
            }
        }
        else
        {
            throw new IllegalArgumentException("LNA gain value [" + gain +
                "] is outside value range: " + LNA_GAIN_MIN + "-" + LNA_GAIN_MAX);
        }
    }

    /**
     * Sets Mixer gain
     *
     * @param gain - value within range of MIXER_GAIN_MIN to MIXER_GAIN_MAX
     * @throws LibUsbException          on error in java USB wrapper
     * @throws UsbException             on error in USB transfer
     * @throws IllegalArgumentException if gain value is invalid
     */
    public void setMixerGain(int gain)
        throws LibUsbException, UsbException, IllegalArgumentException
    {
        if(MIXER_GAIN_MIN <= gain && gain <= MIXER_GAIN_MAX)
        {
            int result = readByte(Command.SET_MIXER_GAIN, 0, gain, true);

            if(result != LibUsb.SUCCESS)
            {
                throw new UsbException("Couldnt set mixer gain to: " + gain);
            }
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
     * @param gain - value within range of VGA_GAIN_MIN to VGA_GAIN_MAX
     * @throws LibUsbException          on error in java USB wrapper
     * @throws UsbException             on error in USB transfer
     * @throws IllegalArgumentException if gain value is invalid
     */
    public void setIFGain(int gain)
        throws LibUsbException, UsbException, IllegalArgumentException
    {
        if(IF_GAIN_MIN <= gain && gain <= IF_GAIN_MAX)
        {
            int result = readByte(Command.SET_VGA_GAIN, 0, gain, true);

            if(result != LibUsb.SUCCESS)
            {
                throw new UsbException("Couldnt set VGA gain to: " + gain);
            }
        }
        else
        {
            throw new IllegalArgumentException("VGA gain value [" + gain +
                "] is outside value range: " + IF_GAIN_MIN + "-" + IF_GAIN_MAX);
        }
    }

    public void setReceiverMode(boolean enabled) throws LibUsbException, UsbException
    {
        //Empty buffer to throw away
        ByteBuffer buffer = ByteBuffer.allocateDirect(0);

        write(Command.RECEIVER_MODE, (enabled ? 1 : 0), 0, buffer);
    }

    /**
     * Queries the device for available sample rates.  Will always provide at
     * least the default 10 MHz sample rate.
     */
    private void determineAvailableSampleRates() throws LibUsbException, UsbException
    {
        mSampleRates.clear();

        //Get a count of available sample rates.  If we get an exception, then
        //we're using an older firmware revision and only the default 10 MHz
        //rate is supported
        try
        {
            byte[] rawCount = readArray(Command.GET_SAMPLE_RATES, 0, 0, 4);

            if(rawCount != null)
            {
                int count = EndianUtils.readSwappedInteger(rawCount, 0);

                byte[] rawRates = readArray(Command.GET_SAMPLE_RATES, 0,
                    count, (count * 4));

                for(int x = 0; x < count; x++)
                {
                    int rate = EndianUtils.readSwappedInteger(rawRates, (x * 4));
                    mSampleRates.add(new AirspySampleRate(x, rate, formatSampleRate(rate)));
                }
            }
        }
        catch(LibUsbException e)
        {
            //Press on, nothing else to do here ..
        }

        if(mSampleRates.isEmpty())
        {
            mSampleRates.add(DEFAULT_SAMPLE_RATE);
        }
    }

    /**
     * Formats the rate in hertz for display as megahertz
     */
    private static String formatSampleRate(int rate)
    {
        return MHZ_FORMATTER.format((double) rate / 1E6d);
    }

    /**
     * Device information
     */
    public AirspyDeviceInformation getDeviceInfo()
    {
        //Lazy initialization
        if(mDeviceInfo == null)
        {
            readDeviceInfo();
        }

        return mDeviceInfo;
    }

    /**
     * Reads version information from the device and populates the info object
     */
    private void readDeviceInfo()
    {
        if(mDeviceInfo == null)
        {
            mDeviceInfo = new AirspyDeviceInformation();
        }

        /* Board ID */
        try
        {
            int boardID = readByte(Command.BOARD_ID_READ, 0, 0, true);

            mDeviceInfo.setBoardID(boardID);
        }
        catch(LibUsbException | UsbException e)
        {
            mLog.error("Error reading airspy board ID", e);
        }

        /* Version String */
        try
        {
            //NOTE: libairspy is internally reading 127 bytes, however airspy_info
            //script is telling it to read 255 bytes ... things that make you go hmmmm
            byte[] version = readArray(Command.VERSION_STRING_READ, 0, 0, 127);

            mDeviceInfo.setVersion(version);
        }
        catch(LibUsbException | UsbException e)
        {
            mLog.error("Error reading airspy version string", e);
        }

        /* Part ID and Serial Number */
        try
        {
            //Read 6 x 32-bit integers = 24 bytes
            byte[] serial = readArray(
                Command.BOARD_PART_ID_SERIAL_NUMBER_READ, 0, 0, 24);

            mDeviceInfo.setPartAndSerialNumber(serial);
        }
        catch(LibUsbException | UsbException e)
        {
            mLog.error("Error reading airspy version string", e);
        }
    }

    /**
     * Reads a single byte value from the device.
     *
     * @param command - airspy command
     * @param value - value field for usb setup packet
     * @param index - index field for usb setup packet
     * @return - byte value as an integer
     * @throws LibUsbException if the operation is unsuccesful
     * @throws UsbException    on any usb errors
     */
    private int readByte(Command command, int value, int index, boolean signed)
        throws LibUsbException, UsbException
    {
        if(mDeviceHandle != null)
        {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1);

            int transferred = LibUsb.controlTransfer(mDeviceHandle,
                USB_REQUEST_IN,
                command.getValue(),
                (short) value,
                (short) index,
                buffer,
                USB_TIMEOUT_MS);

            if(transferred < 0)
            {
                throw new LibUsbException("read error", transferred);
            }

            byte result = buffer.get(0);

            if(signed)
            {
                return (result & 0xFF);
            }
            else
            {
                return result;
            }
        }
        else
        {
            throw new LibUsbException("device handle is null",
                LibUsb.ERROR_NO_DEVICE);
        }
    }

    /**
     * Reads a multi-byte value from the device
     *
     * @param command - airspy command
     * @param value - usb packet value
     * @param index - usb packet index
     * @param length - number of bytes to read
     * @return - bytes read from the device
     * @throws LibUsbException if quantity of bytes read doesn't equal the
     *                         requested number of bytes
     * @throws UsbException    on error communicating with the device
     */
    private byte[] readArray(Command command, int value, int index, int length)
        throws LibUsbException, UsbException
    {
        if(mDeviceHandle != null)
        {
            ByteBuffer buffer = ByteBuffer.allocateDirect(length);

            int transferred = LibUsb.controlTransfer(mDeviceHandle,
                USB_REQUEST_IN,
                command.getValue(),
                (short) value,
                (short) index,
                buffer,
                USB_TIMEOUT_MS);

            if(transferred < 0)
            {
                throw new LibUsbException("read error", transferred);
            }

            byte[] results = new byte[transferred];

            buffer.get(results);

            return results;
        }
        else
        {
            throw new LibUsbException("device handle is null",
                LibUsb.ERROR_NO_DEVICE);
        }
    }

    /**
     * Writes the buffer contents to the device
     *
     * @param command - airspy command
     * @param value - usb packet value
     * @param index - usb packet index
     * @param buffer - data to write to the device
     * @throws UsbException on error
     */
    public void write(Command command, int value, int index, ByteBuffer buffer)
        throws UsbException
    {
        if(mDeviceHandle != null)
        {
            int transferred = LibUsb.controlTransfer(mDeviceHandle,
                USB_REQUEST_OUT,
                command.getValue(),
                (short) value,
                (short) index,
                buffer,
                USB_TIMEOUT_MS);

            if(transferred < 0)
            {
                throw new LibUsbException("error writing byte buffer",
                    transferred);
            }
            else if(transferred != buffer.capacity())
            {
                throw new LibUsbException("transferred bytes [" +
                    transferred + "] is not what was expected [" +
                    buffer.capacity() + "]", transferred);
            }
        }
        else
        {
            throw new LibUsbException("device handle is null",
                LibUsb.ERROR_NO_DEVICE);
        }
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

        public static GainMode getGainMode(Gain gain)
        {
            if(gain == CUSTOM)
            {
                return GainMode.CUSTOM;
            }
            else if(getLinearityGains().contains(gain))
            {
                return GainMode.LINEARITY;
            }
            else if(getSensitivityGains().contains(gain))
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


    /**
     * Airspy Board Identifier
     */
    public enum BoardID
    {
        AIRSPY(0, "Airspy"),
        UNKNOWN(-1, "Unknown");

        private int mValue;
        private String mLabel;

        private BoardID(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public int getValue()
        {
            return mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public static BoardID fromValue(int value)
        {
            if(value == 0)
            {
                return AIRSPY;
            }

            return UNKNOWN;
        }
    }

    /**
     * Airspy Commands
     */
    public enum Command
    {
        INVALID(0),
        RECEIVER_MODE(1),
        SI5351C_WRITE(2),
        SI5351C_READ(3),
        R820T_WRITE(4),
        R820T_READ(5),
        SPIFLASH_ERASE(6),
        SPIFLASH_WRITE(7),
        SPIFLASH_READ(8),
        BOARD_ID_READ(9),
        VERSION_STRING_READ(10),
        BOARD_PART_ID_SERIAL_NUMBER_READ(11),
        SET_SAMPLE_RATE(12),
        SET_FREQUENCY(13),
        SET_LNA_GAIN(14),
        SET_MIXER_GAIN(15),
        SET_VGA_GAIN(16),
        SET_LNA_AGC(17),
        SET_MIXER_AGC(18),
        MS_VENDOR_COMMAND(19),
        SET_RF_BIAS_COMMAND(20),
        GPIO_WRITE(21),
        GPIO_READ(22),
        GPIO_DIR__WRITE(23),
        GPIO_DIR_READ(24),
        GET_SAMPLE_RATES(25),
        SET_PACKING(26);

        private int mValue;

        private Command(int value)
        {
            mValue = value;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public static Command fromValue(int value)
        {
            if(0 <= value && value <= 25)
            {
                return Command.values()[value];
            }

            return INVALID;
        }
    }

    public enum ReceiverMode
    {
        OFF(0),
        ON(1);

        private int mValue;

        private ReceiverMode(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }
    }

    /**
     * General Purpose Input/Output Ports (accessible on the airspy board)
     */
    public enum GPIOPort
    {
        PORT_0(0),
        PORT_1(1),
        PORT_2(2),
        PORT_3(3),
        PORT_4(4),
        PORT_5(5),
        PORT_6(6),
        PORT_7(7);

        private int mValue;

        private GPIOPort(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }
    }

    /**
     * General Purpose Input/Output Pins (accessible on the airspy board)
     */
    public enum GPIOPin
    {
        PIN_0(0),
        PIN_1(1),
        PIN_2(2),
        PIN_3(3),
        PIN_4(4),
        PIN_5(5),
        PIN_6(6),
        PIN_7(7),
        PIN_8(8),
        PIN_9(9),
        PIN_10(10),
        PIN_11(11),
        PIN_12(12),
        PIN_13(13),
        PIN_14(14),
        PIN_15(15),
        PIN_16(16),
        PIN_17(17),
        PIN_18(18),
        PIN_19(19),
        PIN_20(20),
        PIN_21(21),
        PIN_22(22),
        PIN_23(23),
        PIN_24(24),
        PIN_25(25),
        PIN_26(26),
        PIN_27(27),
        PIN_28(28),
        PIN_29(29),
        PIN_30(30),
        PIN_31(31);

        private int mValue;

        private GPIOPin(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }
    }
}