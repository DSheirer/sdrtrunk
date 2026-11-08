/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.fcd;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.libusb.LibUsb;
import io.github.dsheirer.libusb.UsbContext;
import io.github.dsheirer.libusb.UsbDeviceHandle;
import io.github.dsheirer.libusb.UsbDeviceInfo;
import io.github.dsheirer.libusb.UsbException;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.adapter.ComplexShortAdapter;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.mixer.ComplexMixer;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.MixerTunerType;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.UsbDeviceLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FCDTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(FCDTunerController.class);
    private static final double USABLE_BANDWIDTH_PERCENT = 1.0;
    private static final int DC_SPIKE_AVOID_HALF_BANDWIDTH = 5000;
    private final static int FCD_INTERFACE = 0x2;
    private final static int FCD_ENDPOINT_IN = 0x82;
    private final static int FCD_ENDPOINT_OUT = 0x2;
    private final static int FCD_PACKET_SIZE = 64;
    private final static int FCD_TRANSFER_TIMEOUT_MS = 500;
    private final static int LIBUSB_ERROR_NO_DEVICE = -4;

    private int mBus;
    private String mPortAddress;
    private UsbContext mUsbContext;
    private UsbDeviceInfo mDevice;
    private volatile UsbDeviceHandle mDeviceHandle;
    private boolean mShutdown = false;

    private FCDConfiguration mConfiguration = new FCDConfiguration();
    protected ComplexMixer mComplexMixer;

    /**
     * Generic FCD tuner controller - contains functionality common across both
     * funcube dongle tuners
     *
     * @param tunerType of FCD
     * @param mixerTDL for the sound card mixer interface
     * @param bus usb
     * @param portAddress usb
     * @param minTunableFrequency of the tuner
     * @param maxTunableFrequency of the tuner
     * @param tunerErrorListener to receive errors from this tuner
     */
    public FCDTunerController(MixerTunerType tunerType, TargetDataLine mixerTDL, int bus, String portAddress,
                              int minTunableFrequency, int maxTunableFrequency, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
        setMinimumFrequency(minTunableFrequency);
        setMaximumFrequency(maxTunableFrequency);
        setMiddleUnusableHalfBandwidth(DC_SPIKE_AVOID_HALF_BANDWIDTH);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENT);

        try
        {
            mFrequencyController.setSampleRate((int) tunerType.getAudioFormat().getSampleRate());
        }
        catch(SourceException se)
        {
            mLog.error("Error setting sample rate to [" + tunerType.getAudioFormat().getSampleRate() + "]", se);
        }

        mComplexMixer = new ComplexMixer(mixerTDL, tunerType.getAudioFormat(), tunerType.getDisplayString(),
                new ComplexShortAdapter());
        mComplexMixer.setBufferSampleCount(getBufferSampleCount());
        mComplexMixer.setBufferListener(mNativeBufferBroadcaster);
    }

    @Override
    public int getBufferSampleCount()
    {
        if(getTunerType() == TunerType.FUNCUBE_DONGLE_PRO)
        {
            return 4096;
        }
        else if(getTunerType() == TunerType.FUNCUBE_DONGLE_PRO_PLUS)
        {
            return 8192;
        }

        throw new IllegalStateException("Unrecognized tuner type: " + getTunerType());
    }

    /**
     * Overrides the super class functionality to auto-start the complex mixer and provide samples to listeners
     */
    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        getLock().lock();

        try
        {
            boolean hasExistingListeners = hasBufferListeners();
            super.addBufferListener(listener);
            if(!hasExistingListeners)
            {
                mComplexMixer.start();
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Overrides the super class functionality to auto-stop the complex mixer and stop sample stream when there are no
     * more registered listeners
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
                ComplexMixer complexMixer = mComplexMixer;

                if(complexMixer != null)
                {
                    complexMixer.stop();
                }
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Initializes the controller by opening the USB device and claiming the HID interface.
     * Invoke this method after constructing this class to setup the controller.
     *
     * @throws SourceException when it cannot open and claim the USB device
     */
    public void start() throws SourceException
    {
        if(mShutdown)
        {
            throw new SourceException("Device cannot be reused once it has been shutdown");
        }

        mUsbContext = UsbDeviceLookup.createContext();

        try
        {
            mDevice = UsbDeviceLookup.find(mUsbContext, mBus, mPortAddress);

            try
            {
                mDeviceHandle = mUsbContext.open(mDevice);
            }
            catch(UsbException e)
            {
                mLog.error("Can't open USB tuner [" + getTunerType() + "] - check driver or Linux udev rules");
                throw new SourceException("Can't open USB tuner - check driver or Linux udev rules - " +
                        LibUsb.errorName(e.code()));
            }

            //Remove a kernel driver if active
            boolean kernelDriverActive;

            try
            {
                kernelDriverActive = mDeviceHandle.isKernelDriverActive(FCD_INTERFACE);
            }
            catch(UsbException e)
            {
                kernelDriverActive = false;
            }

            if(kernelDriverActive)
            {
                try
                {
                    mDeviceHandle.detachKernelDriver(FCD_INTERFACE);
                }
                catch(UsbException e)
                {
                    mLog.error("Unable to detach kernel driver for USB tuner device - bus:" + mBus + " port:" + mPortAddress);
                    throw new SourceException("Can't detach kernel driver");
                }
            }

            //Claim the interface
            try
            {
                mDeviceHandle.claimInterface(FCD_INTERFACE);
            }
            catch(UsbException e)
            {
                if(e.code() == UsbDeviceLookup.ERROR_BUSY)
                {
                    throw new SourceException("USB tuner is in-use by another application");
                }

                throw new SourceException("Can't claim interface on USB tuner - " + LibUsb.errorName(e.code()));
            }
        }
        catch(SourceException se)
        {
            closeUsb();
            throw se;
        }

        deviceStart();
    }

    /**
     * Closes the USB device handle and the libusb context.
     */
    private void closeUsb()
    {
        UsbDeviceHandle handle = mDeviceHandle;
        mDeviceHandle = null;

        if(handle != null)
        {
            try
            {
                handle.close();
            }
            catch(Exception e)
            {
                mLog.error("error while closing device handle", e);
            }
        }

        if(mUsbContext != null)
        {
            mUsbContext.close();
            mUsbContext = null;
        }
    }

    /**
     * Startup actions for the sub-class implementation
     * @throws SourceException if there is an error
     */
    protected abstract void deviceStart() throws SourceException;

    /**
     * Disposes of resources.  Closes the USB device and interface.
     */
    public void stop()
    {
        if(mComplexMixer != null)
        {
            mComplexMixer.stop();
            mComplexMixer = null;
        }

        closeUsb();
        mDevice = null;
        mShutdown = true;
    }

    /**
     * Sample rate of the tuner
     */
    public abstract double getCurrentSampleRate();

    /**
     * Tuner class
     */
    public abstract TunerClass getTunerClass();

    /**
     * Tuner type
     */
    public abstract TunerType getTunerType();

    /**
     * USB address (bus/port)
     */
    public String getUSBAddress()
    {
        UsbDeviceInfo device = mDevice;

        //This is the tuner's unique ID for saved tuner configurations, so the format must not change: the port is the
        //last port number in the path from the root hub, as libusb_get_port_number() reports it.
        if(device != null)
        {
            int port = device.portNumbers().isEmpty() ? 0 : device.portNumbers().getLast();
            return "Bus:" + device.busNumber() + " Port:" + port;
        }

        return "UNKNOWN";
    }

    /**
     * USB Vendor and Product ID
     */
    public String getUSBID()
    {
        UsbDeviceInfo device = mDevice;

        if(device != null)
        {
            return String.format("%04X:%04X", device.vendorId(), device.productId());
        }

        return "UNKNOWN";
    }

    /**
     * USB Port Speed.  The FUNcube Dongle Pro is a full speed (USB 1.1) device.
     */
    public String getUSBSpeed()
    {
        UsbDeviceInfo device = mDevice;

        if(device != null)
        {
            switch(device.speed())
            {
                case LOW:
                    return "1.1 LOW";
                case FULL:
                    return "1.1 FULL";
                case HIGH:
                    return "2.0 HIGH";
                case SUPER:
                    return "3.0 SUPER";
                default:
            }
        }

        return "UNKNOWN";
    }

    /**
     * Set fcd interface mode
     */
    public void setFCDMode(Mode mode)
    {
        ByteBuffer response = null;

        switch(mode)
        {
            case APPLICATION:
                response = send(FCDCommand.BL_QUERY);
                break;
            case BOOTLOADER:
                response = send(FCDCommand.APP_RESET);
                break;
            default:
                break;
        }

        if(response != null)
        {
            mConfiguration.set(response);
        }
        else
        {
            mConfiguration.setModeUnknown();
        }
    }

    /**
     * Sets the actual (uncorrected) device frequency
     */
    public void setTunedFrequency(long frequency) throws SourceException
    {
        getLock().lock();

        try
        {
            send(FCDCommand.APP_SET_FREQUENCY_HZ, frequency);
        }
        catch(Exception e)
        {
            throw new SourceException("Couldn't set FCD Local " +
                    "Oscillator Frequency [" + frequency + "]", e);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Gets the actual (uncorrected) device frequency
     */
    public long getTunedFrequency() throws SourceException
    {
        try
        {
            ByteBuffer buffer = send(FCDCommand.APP_GET_FREQUENCY_HZ);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return (int) (buffer.getInt(2) & 0xFFFFFFFF);
        }
        catch(Exception e)
        {
            throw new SourceException("FCDTunerController - couldn't get LO frequency", e);
        }
    }

    /**
     * Returns the FCD device configuration
     */
    public FCDConfiguration getConfiguration()
    {
        return mConfiguration;
    }

    /**
     * Performs an interrupt write of one 64 byte packet to the OUT endpoint.
     *
     * @param packet to write.  Must be 64 bytes in length.
     * @throws FCDUsbException on error
     */
    private void write(byte[] packet) throws FCDUsbException
    {
        UsbDeviceHandle handle = mDeviceHandle;

        if(handle == null)
        {
            throw new FCDUsbException("device handle is null", LIBUSB_ERROR_NO_DEVICE);
        }

        try
        {
            handle.interruptTransfer(FCD_ENDPOINT_OUT, packet, packet.length, FCD_TRANSFER_TIMEOUT_MS);
        }
        catch(UsbException e)
        {
            throw new FCDUsbException("error writing byte buffer", e.code());
        }
    }

    /**
     * Performs an interrupt write to the OUT endpoint for the FCD command.
     *
     * @param command - no-argument command to write
     * @throws FCDUsbException - on error
     */
    private void write(FCDCommand command) throws FCDUsbException
    {
        byte[] packet = new byte[FCD_PACKET_SIZE];
        packet[0] = command.getCommand();
        packet[1] = (byte) 0x00;
        write(packet);
    }

    /**
     * Performs an interrupt write to the OUT endpoint for the FCD command.
     *
     * @param command - command to write
     * @param argument - value to write with the command
     * @throws FCDUsbException - on error
     */
    private void write(FCDCommand command, long argument) throws FCDUsbException
    {
        byte[] packet = new byte[FCD_PACKET_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        /* The FCD expects little-endian formatted values */
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, command.getCommand());
        buffer.putLong(1, argument);
        write(packet);
    }

    /**
     * Performs an interrupt read of one 64 byte packet from the IN endpoint
     *
     * @return buffer read from FCD
     * @throws FCDUsbException on error
     */
    private ByteBuffer read() throws FCDUsbException
    {
        UsbDeviceHandle handle = mDeviceHandle;

        if(handle == null)
        {
            throw new FCDUsbException("device handle is null", LIBUSB_ERROR_NO_DEVICE);
        }

        byte[] packet = new byte[FCD_PACKET_SIZE];
        int transferred;

        try
        {
            transferred = handle.interruptTransfer(FCD_ENDPOINT_IN, packet, packet.length, FCD_TRANSFER_TIMEOUT_MS);
        }
        catch(UsbException e)
        {
            throw new FCDUsbException("error reading byte buffer", e.code());
        }

        if(transferred != packet.length)
        {
            throw new FCDUsbException("received bytes [" + transferred + "] didn't match expected length [" +
                    packet.length + "]", 0);
        }

        return ByteBuffer.wrap(packet);
    }

    /**
     * Sends the FCD command and argument.  Performs a read to complete the
     * command.
     *
     * @param command - command to send
     * @param argument - command argument to send
     * @throws FCDUsbException - on error
     */
    protected synchronized void send(FCDCommand command, long argument) throws FCDUsbException
    {
        write(command, argument);
        read();
    }

    /**
     * Sends the no-argument FCD command.  Performs a read to complete the
     * command.
     *
     * @param command - command to send
     * @throws FCDUsbException - on error
     */
    protected ByteBuffer send(FCDCommand command) throws FCDUsbException
    {
        write(command);
        return read();
    }

    /**
     * FCD configuration string parsing  class
     */
    public class FCDConfiguration
    {
        private String mConfig;
        private Mode mMode;

        public FCDConfiguration()
        {
            mConfig = null;
            mMode = Mode.UNKNOWN;
        }

        private void setModeUnknown()
        {
            mConfig = null;
            mMode = Mode.UNKNOWN;
        }

        /**
         * Extracts the configuration string from the buffer
         */
        public void set(ByteBuffer buffer)
        {
            if(buffer.capacity() == 64)
            {
                byte[] data = new byte[64];
                for(int x = 0; x < 64; x++)
                {
                    data[x] = buffer.get(x);
                }

                mConfig = new String(data);
                mMode = Mode.getMode(mConfig);
            }
            else
            {
                mConfig = null;
                mMode = Mode.ERROR;
            }
        }

        public Mode getMode()
        {
            return mMode;
        }

        public FCDModel getModel()
        {
            FCDModel retVal = FCDModel.FUNCUBE_UNKNOWN;

            switch(mMode)
            {
                case APPLICATION:
                    retVal = FCDModel.getFCD(mConfig.substring(15, 22));
                    break;
                case BOOTLOADER:
                case UNKNOWN:
                case ERROR:
                    break;
            }

            return retVal;
        }

        public Block getBandBlocking()
        {
            Block retVal = Block.UNKNOWN;

            switch(mMode)
            {
                case APPLICATION:
                    retVal = Block.getBlock(mConfig.substring(23, 33).trim());
                    break;
                case BOOTLOADER:
                case UNKNOWN:
                case ERROR:
                    break;
            }

            return retVal;
        }

        public String getFirmware()
        {
            String retVal = null;

            switch(mMode)
            {
                case APPLICATION:
                    retVal = mConfig.substring(9, 14);
                    break;
                case BOOTLOADER:
                case UNKNOWN:
                case ERROR:
                    break;
            }

            return retVal;
        }

        public String toString()
        {
            return getModel().getLabel();
        }
    }

    public enum Mode
    {
        APPLICATION,
        BOOTLOADER,
        ERROR,
        UNKNOWN;

        public static Mode getMode(String config)
        {
            Mode retVal = UNKNOWN;

            if(config == null)
            {
                retVal = ERROR;
            }
            else
            {
                if(config.length() >= 8)
                {
                    String mode = config.substring(2, 8).trim();

                    if(mode.equalsIgnoreCase("FCDAPP"))
                    {
                        retVal = APPLICATION;
                    }
                    else if(mode.equalsIgnoreCase("FCDBL"))
                    {
                        retVal = BOOTLOADER;
                    }
                }
            }

            return retVal;
        }
    }

    public enum Block
    {
        CELLULAR_BAND_BLOCKED("Blocked"),
        NO_BAND_BLOCK("Unblocked"),
        UNKNOWN("Unknown");

        private String mLabel;

        Block(String label)
        {
            mLabel = label;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public static Block getBlock(String block)
        {
            Block retVal = UNKNOWN;

            if(block.equalsIgnoreCase("No blk"))
            {
                retVal = NO_BAND_BLOCK;
            }
            else if(block.equalsIgnoreCase("Cell blk"))
            {
                retVal = CELLULAR_BAND_BLOCKED;
            }

            return retVal;
        }
    }
}
