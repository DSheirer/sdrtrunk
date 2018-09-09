/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2017 Dennis Sheirer
 *
 *     Ported from libhackrf at:
 *     https://github.com/mossmann/hackrf/tree/master/host/libhackrf
 *
 *	   Copyright (c) 2012, Jared Boone <jared@sharebrained.com>
 *     Copyright (c) 2013, Benjamin Vernoux <titanmkd@gmail.com>
 *     copyright (c) 2013, Michael Ossmann <mike@ossmann.com>
 *
 *     All rights reserved.
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions are 
 *     met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright 
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *     Neither the name of Great Scott Gadgets nor the names of its contributors 
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 *     TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
 *     PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT 
 *     HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 *     SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 *     TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 *     PROFITS; OR BUSINESS INTERRUPTION)
 *     HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 *     STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *     ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *     POSSIBILITY OF SUCH DAMAGE.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.source.tuner.hackrf;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.usb.USBTransferProcessor;
import io.github.dsheirer.source.tuner.usb.USBTunerController;
import io.github.dsheirer.source.tuner.usb.converter.ByteSampleConverter;
import io.github.dsheirer.source.tuner.usb.converter.NativeBufferConverter;
import org.apache.commons.io.EndianUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import javax.usb.UsbException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class HackRFTunerController extends USBTunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(HackRFTunerController.class);

    public final static long USB_TIMEOUT_US = 1000000l; //uSeconds
    public static final byte USB_ENDPOINT = (byte)0x81;
    public static final byte USB_INTERFACE = (byte)0x0;
    public static final int USB_TRANSFER_BUFFER_SIZE = 262144;

    public static final byte REQUEST_TYPE_IN = (byte)(LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE);
    public static final byte REQUEST_TYPE_OUT = (byte)(LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE);

    public static final long MIN_FREQUENCY = 10000000l;
    public static final long MAX_FREQUENCY = 6000000000l;
    public static final long DEFAULT_FREQUENCY = 101100000;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 5000;

    private NativeBufferConverter mNativeBufferConverter = new ByteSampleConverter();
    private USBTransferProcessor mUSBTransferProcessor;

    private HackRFSampleRate mSampleRate = HackRFSampleRate.RATE2_016MHZ;
    private boolean mAmplifierEnabled = false;

    private Device mDevice;
    private DeviceDescriptor mDeviceDescriptor;
    private DeviceHandle mDeviceHandle;

    public HackRFTunerController(Device device, DeviceDescriptor descriptor) throws SourceException
    {
        super(MIN_FREQUENCY, MAX_FREQUENCY, DC_SPIKE_AVOID_BUFFER, USABLE_BANDWIDTH_PERCENT);

        mDevice = device;
        mDeviceDescriptor = descriptor;
    }

    @Override
    public int getBufferSampleCount()
    {
        return USB_TRANSFER_BUFFER_SIZE / 2;
    }

    public void init() throws SourceException
    {
        mDeviceHandle = new DeviceHandle();

        int result = LibUsb.open(mDevice, mDeviceHandle);

        if(result != 0)
        {
            if(result == LibUsb.ERROR_ACCESS)
            {
                mLog.error("Unable to access HackRF - insufficient permissions.  "
                    + "If you are running a Linux OS, have you installed the "
                    + "hackRF rules file in \\etc\\udev\\rules.d ??");
            }

            throw new SourceException("Couldn't open hackrf device - " +
                LibUsb.strError(result));
        }

        try
        {
            claimInterface();

            setMode(Mode.RECEIVE);

            setFrequency(DEFAULT_FREQUENCY);
        }
        catch(Exception e)
        {
            throw new SourceException("HackRF Tuner Controller - couldn't "
                + "claim USB interface or get endpoint or pipe", e);
        }

        String name;

        try
        {
            name = "HackRF " + getSerial().getSerialNumber();
        }
        catch(UsbException ue)
        {
            //Do nothing, we couldn't determine the serial number
            name = "HackRF - Unidentified Serial";
        }

        mUSBTransferProcessor = new USBTransferProcessor(name, mDeviceHandle, mNativeBufferConverter, USB_TRANSFER_BUFFER_SIZE);
    }

    @Override
    public void dispose()
    {
        if(mDeviceHandle != null)
        {
            mLog.info("Releasing HackRF Tuner");
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

    /**
     * HackRF board identifier/type
     */
    public BoardID getBoardID() throws UsbException
    {
        int id = readByte(Request.BOARD_ID_READ, (byte)0, (byte)0, false);

        return BoardID.lookup(id);
    }

    /**
     * HackRF firmware version string
     */
    public String getFirmwareVersion() throws UsbException
    {
        ByteBuffer buffer = readArray(Request.VERSION_STRING_READ, 0, 0, 255);

        byte[] data = new byte[255];

        buffer.get(data);

        return new String(data);
    }

    /**
     * HackRF part id number and serial number
     */
    public Serial getSerial() throws UsbException
    {
        ByteBuffer buffer = readArray(Request.BOARD_PARTID_SERIALNO_READ, 0, 0, 24);

        return new Serial(buffer);
    }

    /**
     * Sets the HackRF transceiver mode
     */
    public void setMode(Mode mode) throws UsbException
    {
        write(Request.SET_TRANSCEIVER_MODE, mode.getNumber(), 0);
    }

    /**
     * Sets the HackRF baseband filter
     */
    public void setBasebandFilter(BasebandFilter filter) throws UsbException
    {
        write(Request.BASEBAND_FILTER_BANDWIDTH_SET,
            filter.getLowValue(),
            filter.getHighValue());
    }

    /**
     * Enables (true) or disables (false) the amplifier
     */
    public void setAmplifierEnabled(boolean enabled) throws UsbException
    {
        write(Request.AMP_ENABLE, (enabled ? 1 : 0), 0);

        mAmplifierEnabled = enabled;
    }

    public boolean getAmplifier()
    {
        return mAmplifierEnabled;
    }

    /**
     * Sets the IF LNA Gain
     */
    public void setLNAGain(HackRFLNAGain gain) throws UsbException
    {
        int result = readByte(Request.SET_LNA_GAIN, 0, gain.getValue(), true);

        if(result != 1)
        {
            throw new UsbException("couldn't set lna gain to " + gain);
        }
    }

    /**
     * Sets the Baseband VGA Gain
     */
    public void setVGAGain(HackRFVGAGain gain) throws UsbException
    {
        int result = readByte(Request.SET_VGA_GAIN, 0, gain.getValue(), true);

        if(result != 1)
        {
            throw new UsbException("couldn't set vga gain to " + gain);
        }
    }

    /**
     * Not implemented
     */
    public long getTunedFrequency() throws SourceException
    {
        return mFrequencyController.getTunedFrequency();
    }

    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int mhz = (int)(frequency / 1E6);
        int hz = (int)(frequency - (mhz * 1E6));

        buffer.putInt(mhz);
        buffer.putInt(hz);

        buffer.rewind();

        try
        {
            write(Request.SET_FREQUENCY, 0, 0, buffer);
        }
        catch(UsbException e)
        {
            mLog.error("error setting frequency [" + frequency + "]", e);

            throw new SourceException("error setting frequency [" +
                frequency + "]", e);
        }
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mSampleRate.getRate();
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof HackRFTunerConfiguration)
        {
            HackRFTunerConfiguration hackRFConfig =
                (HackRFTunerConfiguration)config;

            try
            {
                setSampleRate(hackRFConfig.getSampleRate());
                setFrequencyCorrection(hackRFConfig.getFrequencyCorrection());
                setAmplifierEnabled(hackRFConfig.getAmplifierEnabled());
                setLNAGain(hackRFConfig.getLNAGain());
                setVGAGain(hackRFConfig.getVGAGain());
                setFrequency(getFrequency());
            }
            catch(UsbException e)
            {
                throw new SourceException("Error while applying tuner "
                    + "configuration", e);
            }

            try
            {
                setFrequency(hackRFConfig.getFrequency());
            }
            catch(SourceException se)
            {
                //Do nothing, we couldn't set the frequency
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid tuner configuration "
                + "type [" + config.getClass() + "]");
        }
    }

    public ByteBuffer readArray(Request request,
                                int value,
                                int index,
                                int length) throws UsbException
    {
        if(mDeviceHandle != null)
        {
            ByteBuffer buffer = ByteBuffer.allocateDirect(length);

            int transferred = LibUsb.controlTransfer(mDeviceHandle,
                REQUEST_TYPE_IN,
                request.getRequestNumber(),
                (short)value,
                (short)index,
                buffer,
                USB_TIMEOUT_US);

            if(transferred < 0)
            {
                throw new LibUsbException("read error", transferred);
            }

            return buffer;
        }
        else
        {
            throw new LibUsbException("device handle is null",
                LibUsb.ERROR_NO_DEVICE);
        }
    }

    public int read(Request request, int value, int index, int length)
        throws UsbException
    {
        if(!(length == 1 || length == 2 || length == 4))
        {
            throw new IllegalArgumentException("invalid length [" + length +
                "] must be: byte=1, short=2, int=4 to read a primitive");
        }

        ByteBuffer buffer = readArray(request, value, index, length);

        byte[] data = new byte[buffer.capacity()];

        buffer.get(data);

        switch(data.length)
        {
            case 1:
                return data[0];
            case 2:
                return EndianUtils.readSwappedShort(data, 0);
            case 4:
                return EndianUtils.readSwappedInteger(data, 0);
            default:
                throw new UsbException("read() primitive returned an "
                    + "unrecognized byte array " + Arrays.toString(data));
        }
    }

    public int readByte(Request request, int value, int index, boolean signed)
        throws UsbException
    {
        ByteBuffer buffer = readArray(request, value, index, 1);

        if(signed)
        {
            return (int)(buffer.get());
        }
        else
        {
            return (int)(buffer.get() & 0xFF);
        }
    }

    public void write(Request request,
                      int value,
                      int index,
                      ByteBuffer buffer) throws UsbException
    {
        if(mDeviceHandle != null)
        {
            int transferred = LibUsb.controlTransfer(mDeviceHandle,
                REQUEST_TYPE_OUT,
                request.getRequestNumber(),
                (short)value,
                (short)index,
                buffer,
                USB_TIMEOUT_US);

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

    /**
     * Sends a request that doesn't have a data payload
     */
    public void write(Request request,
                      int value,
                      int index) throws UsbException
    {
        write(request, value, index, ByteBuffer.allocateDirect(0));
    }

    /**
     * Sample Rate
     *
     * Note: the libhackrf set sample rate method is designed to allow fractional
     * sample rates.  However, since we're only using integral sample rates, we
     * simply invoke the setSampleRateManual method directly.
     */
    public void setSampleRate(HackRFSampleRate rate) throws UsbException, SourceException
    {
        setSampleRateManual(rate.getRate(), 1);

        mFrequencyController.setSampleRate(rate.getRate());

        setBasebandFilter(rate.getFilter());

        mSampleRate = rate;
    }

    public void setSampleRateManual(int frequency, int divider)
        throws UsbException
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(frequency);
        buffer.putInt(divider);

        write(Request.SET_SAMPLE_RATE, 0, 0, buffer);
    }

    public double getSampleRate()
    {
        return mSampleRate.getRate();
    }

    public enum Request
    {
        SET_TRANSCEIVER_MODE(1),
        MAX2837_TRANSCEIVER_WRITE(2),
        MAX2837_TRANSCEIVER_READ(3),
        SI5351C_CLOCK_GENERATOR_WRITE(4),
        SI5351C_CLOCK_GENERATOR_READ(5),
        SET_SAMPLE_RATE(6),
        BASEBAND_FILTER_BANDWIDTH_SET(7),
        RFFC5071_MIXER_WRITE(8),
        RFFC5071_MIXER_READ(9),
        SPIFLASH_ERASE(10),
        SPIFLASH_WRITE(11),
        SPIFLASH_READ(12),
        BOARD_ID_READ(14),
        VERSION_STRING_READ(15),
        SET_FREQUENCY(16),
        AMP_ENABLE(17),
        BOARD_PARTID_SERIALNO_READ(18),
        SET_LNA_GAIN(19),
        SET_VGA_GAIN(20),
        SET_TXVGA_GAIN(21),
        ANTENNA_ENABLE(23),
        SET_FREQUENCY_EXPLICIT(24);

        private byte mRequestNumber;

        private Request(int number)
        {
            mRequestNumber = (byte)number;
        }

        public byte getRequestNumber()
        {
            return mRequestNumber;
        }
    }

    public enum HackRFSampleRate
    {
        RATE2_016MHZ(2016000, "2.016 MHz", BasebandFilter.F3_50),
        RATE3_024MHZ(3024000, "3.024 MHz", BasebandFilter.F5_00),
        RATE4_464MHZ(4464000, "4.464 MHz", BasebandFilter.F6_00),
        RATE5_376MHZ(5376000, "5.376 MHz", BasebandFilter.F7_00),
        RATE7_488MHZ(7488000, "7.488 MHz", BasebandFilter.F9_00),
        RATE10_080MHZ(10080000, "10.080 MHz", BasebandFilter.F12_00),
        RATE12_000MHZ(12000000, "12.000 MHz", BasebandFilter.F14_00),
        RATE13_440MHZ(13440000, "13.440 MHz", BasebandFilter.F15_00),
        RATE14_976MHZ(14976000, "14.976 MHz", BasebandFilter.F20_00),
        RATE19_968MHZ(19968000, "19.968 MHz", BasebandFilter.F24_00);

        private int mRate;
        private String mLabel;
        private BasebandFilter mFilter;

        private HackRFSampleRate(int rate,
                                 String label,
                                 BasebandFilter filter)
        {
            mRate = rate;
            mLabel = label;
            mFilter = filter;
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

        public BasebandFilter getFilter()
        {
            return mFilter;
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

        private BasebandFilter(int bandwidth, String label)
        {
            mBandwidth = bandwidth;
            mLabel = label;
        }

        public int getBandwidth()
        {
            return mBandwidth;
        }

        public int getHighValue()
        {
            return mBandwidth >> 16;
        }

        public int getLowValue()
        {
            return mBandwidth & 0xFFFF;
        }

        public String getLabel()
        {
            return mLabel;
        }
    }

    public enum BoardID
    {
        JELLYBEAN(0x00, "HackRF Jelly Bean"),
        JAWBREAKER(0x01, "HackRF Jaw Breaker"),
        HACKRF_ONE(0x02, "HackRF One"),
        INVALID(0xFF, "HackRF Unknown Board");

        private byte mIDNumber;
        private String mLabel;

        private BoardID(int number, String label)
        {
            mIDNumber = (byte)number;
            mLabel = label;
        }

        public String toString()
        {
            return mLabel;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public byte getNumber()
        {
            return mIDNumber;
        }

        public static BoardID lookup(int value)
        {
            switch(value)
            {
                case 0:
                    return JELLYBEAN;
                case 1:
                    return JAWBREAKER;
                case 2:
                    return HACKRF_ONE;
                default:
                    return INVALID;
            }
        }
    }

    public enum Mode
    {
        OFF(0, "Off"),
        RECEIVE(1, "Receive"),
        TRANSMIT(2, "Transmit"),
        SS(3, "SS");

        private byte mNumber;
        private String mLabel;

        private Mode(int number, String label)
        {
            mNumber = (byte)number;
            mLabel = label;
        }

        public byte getNumber()
        {
            return mNumber;
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

    public enum HackRFLNAGain
    {
        GAIN_0(0),
        GAIN_8(8),
        GAIN_16(16),
        GAIN_24(24),
        GAIN_32(32),
        GAIN_40(40);

        private int mValue;

        private HackRFLNAGain(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }

        public String toString()
        {
            return String.valueOf(mValue) + " dB";
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

        private HackRFVGAGain(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }

        public String toString()
        {
            return String.valueOf(mValue) + " dB";
        }
    }

    /**
     * HackRF part id and serial number parsing class
     */
    public class Serial
    {
        private byte[] mData;

        public Serial(ByteBuffer buffer)
        {
            mData = new byte[buffer.capacity()];

            buffer.get(mData);
        }

        public String getPartID()
        {
            int part0 = EndianUtils.readSwappedInteger(mData, 0);
            int part1 = EndianUtils.readSwappedInteger(mData, 4);

            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%08X", part0));
            sb.append("-");
            sb.append(String.format("%08X", part1));

            return sb.toString();
        }

        public String getSerialNumber()
        {
            int serial0 = EndianUtils.readSwappedInteger(mData, 8);
            int serial1 = EndianUtils.readSwappedInteger(mData, 12);
            int serial2 = EndianUtils.readSwappedInteger(mData, 16);
            int serial3 = EndianUtils.readSwappedInteger(mData, 20);

            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%08X", serial0));
            sb.append("-");
            sb.append(String.format("%08X", serial1));
            sb.append("-");
            sb.append(String.format("%08X", serial2));
            sb.append("-");
            sb.append(String.format("%08X", serial3));

            return sb.toString();
        }
    }
}