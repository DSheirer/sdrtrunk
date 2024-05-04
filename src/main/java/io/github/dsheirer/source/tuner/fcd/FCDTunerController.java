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
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.adapter.ComplexShortAdapter;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.mixer.ComplexMixer;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.MixerTunerType;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import javax.sound.sampled.TargetDataLine;
import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FCDTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(FCDTunerController.class);
    private static final double USABLE_BANDWIDTH_PERCENT = 1.0;
    private static final int DC_SPIKE_AVOID_HALF_BANDWIDTH = 5000;
    private final static byte FCD_INTERFACE = (byte) 0x2;
    private final static byte FCD_ENDPOINT_IN = (byte) 0x82;
    private final static byte FCD_ENDPOINT_OUT = (byte) 0x2;

    private int mBus;
    private String mPortAddress;
    private Context mDeviceContext = new Context();
    private Device mDevice;
    private DeviceDescriptor mDeviceDescriptor = new DeviceDescriptor();
    private DeviceHandle mDeviceHandle = new DeviceHandle();

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
     * Finds the USB device for this tuner at the specified USB bus and port.
     * @return discovered USB device
     * @throws SourceException if there is an error or the device is not discovered.
     */
    private Device findDevice() throws SourceException
    {
        Device foundDevice = null;

        DeviceList deviceList = new DeviceList();
        int count = LibUsb.getDeviceList(mDeviceContext, deviceList);

        if(count >= 0)
        {
            for(Device device: deviceList)
            {
                int bus = LibUsb.getBusNumber(device);
                int port = LibUsb.getPortNumber(device);

                if(port > 0)
                {
                    String portAddress = TunerManager.getPortAddress(device);

                    if(mBus == bus && mPortAddress != null && mPortAddress.equals(portAddress))
                    {
                        foundDevice = device;
                    }
                    else
                    {
                        LibUsb.unrefDevice(device);
                    }
                }
                else
                {
                    LibUsb.unrefDevice(device);
                }
            }
        }
        else
        {
            throw new SourceException("LibUsb couldn't discover USB device [" + mBus + ":" + mPortAddress +
                    "] from device list" + (count < 0 ? " - error: " + LibUsb.errorName(count) : ""));
        }

        if(foundDevice == null)
        {
            throw new SourceException("LibUsb couldn't find the matching USB device");
        }

        return foundDevice;
    }

    /**
     * Initializes the controller by opening the USB device and claiming the HID interface.
     * Invoke this method after constructing this class to setup the controller.
     *
     * @throws SourceException when it cannot open and claim the USB device
     */
    public void start() throws SourceException
    {
        if(mDeviceContext == null)
        {
            throw new SourceException("Device cannot be reused once it has been shutdown");
        }

        int status = LibUsb.init(mDeviceContext);

        if(status != LibUsb.SUCCESS)
        {
            throw new SourceException("Can't initialize libusb library - " + LibUsb.errorName(status));
        }

        mDevice = findDevice();
        mDeviceDescriptor = new DeviceDescriptor();
        status = LibUsb.getDeviceDescriptor(mDevice, mDeviceDescriptor);

        if(status != LibUsb.SUCCESS)
        {
            mDeviceDescriptor = null;
            throw new SourceException("Can't obtain tuner's device descriptor - " + LibUsb.errorName(status));
        }

        mDeviceHandle = new DeviceHandle();
        status = LibUsb.open(mDevice, mDeviceHandle);

        if(status != LibUsb.SUCCESS)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;

            mLog.error("Can't open USB tuner [" + getTunerType() + "] - check driver or Linux udev rules");
            throw new SourceException("Can't open USB tuner - check driver or Linux udev rules - " + LibUsb.errorName(status));
        }

        //Remove a kernel driver if active
        status = LibUsb.kernelDriverActive(mDeviceHandle, FCD_INTERFACE);

        if(status == 1)
        {
            status = LibUsb.detachKernelDriver(mDeviceHandle, FCD_INTERFACE);

            if(status != LibUsb.SUCCESS)
            {
                mLog.error("Unable to detach kernel driver for USB tuner device - bus:" + mBus + " port:" + mPortAddress);
                mDeviceHandle = null;
                mDeviceDescriptor = null;
                throw new SourceException("Can't detach kernel driver");
            }
        }

        //Claim the interface
        status = LibUsb.claimInterface(mDeviceHandle, FCD_INTERFACE);

        if(status == LibUsb.ERROR_BUSY)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;
            throw new SourceException("USB tuner is in-use by another application");
        }
        else if(status != LibUsb.SUCCESS)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;
            throw new SourceException("Can't claim interface on USB tuner - " + LibUsb.errorName(status));
        }

        try
        {
            deviceStart();
        }
        catch(SourceException se)
        {
            throw se;
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

        if(mDeviceHandle != null)
        {
            try
            {
                LibUsb.close(mDeviceHandle);
            }
            catch(Exception e)
            {
                mLog.error("error while closing device handle", e);
            }
        }

        mDeviceDescriptor = null;
        mDeviceHandle = null;
        mDevice = null;
        LibUsb.exit(mDeviceContext);
        mDeviceContext = null;
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
        if(mDevice != null)
        {
            StringBuilder sb = new StringBuilder();

            sb.append("Bus:");
            int bus = LibUsb.getBusNumber(mDevice);
            sb.append(bus);

            sb.append(" Port:");
            int port = LibUsb.getPortNumber(mDevice);
            sb.append(port);

            return sb.toString();
        }

        return "UNKNOWN";
    }

    /**
     * USB Vendor and Product ID
     */
    public String getUSBID()
    {
        if(mDeviceDescriptor != null)
        {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%04X", (int) (mDeviceDescriptor.idVendor() & 0xFFFF)));
            sb.append(":");
            sb.append(String.format("%04X", (int) (mDeviceDescriptor.idProduct() & 0xFFFF)));

            return sb.toString();
        }

        return "UNKNOWN";
    }

    /**
     * USB Port Speed.  Should be 2.0 for both types of funcube dongles
     */
    public String getUSBSpeed()
    {
        if(mDevice != null)
        {
            int speed = LibUsb.getDeviceSpeed(mDevice);

            switch(speed)
            {
                case 0:
                    return "1.1 LOW";
                case 1:
                    return "1.1 FULL";
                case 2:
                    return "2.0 HIGH";
                case 3:
                    return "3.0 SUPER";
                default:
            }
        }

        return "UNKNOWN";
    }

    /**
     * Set fcd interface mode
     */
    public void setFCDMode(Mode mode) throws UsbException, UsbClaimException
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
     * Performs an interrupt write to the OUT endpoint.
     *
     * @param buffer - direct allocated buffer.  Must be 64 bytes in length.
     * @throws LibUsbException on error
     */
    private void write(ByteBuffer buffer) throws LibUsbException
    {
        if(mDeviceHandle != null)
        {
            IntBuffer transferred = IntBuffer.allocate(1);
            int result = LibUsb.interruptTransfer(mDeviceHandle, FCD_ENDPOINT_OUT, buffer, transferred, 500l);

            if(result != LibUsb.SUCCESS)
            {
                throw new LibUsbException("error writing byte buffer", result);
            }
        }
        else
        {
            throw new LibUsbException("device handle is null", LibUsb.ERROR_NO_DEVICE);
        }
    }

    /**
     * Performs an interrupt write to the OUT endpoint for the FCD command.
     *
     * @param command - no-argument command to write
     * @throws LibUsbException - on error
     */
    private void write(FCDCommand command) throws LibUsbException
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(64);
        buffer.put(0, command.getCommand());
        buffer.put(1, (byte) 0x00);
        write(buffer);
    }

    /**
     * Performs an interrupt write to the OUT endpoint for the FCD command.
     *
     * @param command - command to write
     * @param argument - value to write with the command
     * @throws LibUsbException - on error
     */
    private void write(FCDCommand command, long argument) throws LibUsbException
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(64);
        /* The FCD expects little-endian formatted values */
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, command.getCommand());
        buffer.putLong(1, argument);
        write(buffer);
    }

    /**
     * Performs an interrupt read against the endpoint
     *
     * @return buffer read from FCD
     * @throws LibUsbException on error
     */
    private ByteBuffer read() throws LibUsbException
    {
        if(mDeviceHandle != null)
        {
            ByteBuffer buffer = ByteBuffer.allocateDirect(64);
            IntBuffer transferred = IntBuffer.allocate(1);
            int result = LibUsb.interruptTransfer(mDeviceHandle, FCD_ENDPOINT_IN, buffer, transferred, 500l);

            if(result != LibUsb.SUCCESS)
            {
                throw new LibUsbException("error reading byte buffer", result);
            }
            else if(transferred.get(0) != buffer.capacity())
            {
                throw new LibUsbException("received bytes [" + transferred.get(0) + "] didn't match expected length [" +
                        buffer.capacity() + "]", result);
            }

            return buffer;
        }

        throw new LibUsbException("device handle is null", LibUsb.ERROR_NO_DEVICE);
    }

    /**
     * Sends the FCD command and argument.  Performs a read to complete the
     * command.
     *
     * @param command - command to send
     * @param argument - command argument to send
     * @throws LibUsbException - on error
     */
    protected synchronized void send(FCDCommand command, long argument) throws LibUsbException
    {
        write(command, argument);
        read();
    }

    /**
     * Sends the no-argument FCD command.  Performs a read to complete the
     * command.
     *
     * @param command - command to send
     * @throws LibUsbException - on error
     */
    protected ByteBuffer send(FCDCommand command) throws LibUsbException
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
