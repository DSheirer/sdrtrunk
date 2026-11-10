/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.buffer.ByteNativeBufferFactory;
import io.github.dsheirer.buffer.INativeBufferFactory;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.usb.USBTunerController;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;

/**
 * RTL-2832 tuner controller implementation.
 */
public class RTL2832TunerController extends USBTunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(RTL2832TunerController.class);

    public static final int TWO_TO_22_POWER = 4194304;
    public static final int USB_TRANSFER_BUFFER_SIZE = 65_536;
    public static final byte CONTROL_ENDPOINT_IN = (byte) (LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_VENDOR);
    public static final byte CONTROL_ENDPOINT_OUT = (byte) (LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_VENDOR);
    public static final long TIMEOUT_US = 1000000l; //uSeconds
    public static final byte REQUEST_ZERO = (byte) 0;
    public static final byte EEPROM_ADDRESS = (byte) 0xA0;
    public static final byte[] FIR_FILTER_COEFFICIENTS = {(byte) 0xCA, (byte) 0xDC, (byte) 0xD7, (byte) 0xD8,
            (byte) 0xE0, (byte) 0xF2, (byte) 0x0E, (byte) 0x35, (byte) 0x06, (byte) 0x50, (byte) 0x9C, (byte) 0x0D,
            (byte) 0x71, (byte) 0x11, (byte) 0x14, (byte) 0x71, (byte) 0x74, (byte) 0x19, (byte) 0x41, (byte) 0xA5};
    public static final SampleRate DEFAULT_SAMPLE_RATE = SampleRate.RATE_2_400MHZ;
    private SampleRate mSampleRate = DEFAULT_SAMPLE_RATE;
    private INativeBufferFactory mNativeBufferFactory = new ByteNativeBufferFactory();
    private int mOscillatorFrequency = 28800000; //28.8 MHz
    private Descriptor mDescriptor;
    private EmbeddedTuner mEmbeddedTuner;
    private long mTunedFrequency = 0;
    private boolean mBiasTEnabled = false;

    /**
     * Abstract tuner controller device.  Use the static gettype() method
     * to determine the tuner type, and construct the corresponding child
     * tuner controller class for that tuner type.
     */
    public RTL2832TunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(bus, portAddress, tunerErrorListener);
    }

    @Override
    protected INativeBufferFactory getNativeBufferFactory()
    {
        return mNativeBufferFactory;
    }

    @Override
    protected int getTransferBufferSize()
    {
        return USB_TRANSFER_BUFFER_SIZE;
    }

    @Override
    public int getBufferSampleCount()
    {
        return getTransferBufferSize() / 2;
    }

    /**
     * The embedded tuner used with this RTL2832 USB tuner
     */
    public EmbeddedTuner getEmbeddedTuner()
    {
        return mEmbeddedTuner;
    }

    public boolean hasEmbeddedTuner()
    {
        return mEmbeddedTuner != null;
    }

    /**
     * Sets the tuned frequency in the embedded tuner
     * @param frequency in Hertz
     * @throws SourceException if there is an error
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        getEmbeddedTuner().setTunedFrequency(frequency);
        mTunedFrequency = frequency;
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mTunedFrequency;
    }

    @Override
    public void apply(TunerConfiguration tunerConfig) throws SourceException
    {
        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(tunerConfig);

        if(tunerConfig instanceof RTL2832TunerConfiguration rtl)
        {
            setSampleRate(rtl.getSampleRate());
            getEmbeddedTuner().apply(rtl);
        }
    }

    /**
     * Executes additional device startup actions after being commanded by the parent USB startup actions.
     * @throws SourceException if there is an error/issue making this device is unusable
     */
    @Override
    protected void deviceStart() throws SourceException
    {
        //Perform dummy write to see if device needs reset
        boolean resetRequired = false;

        try
        {
            writeRegister(Block.USB, Address.USB_SYSCTL, 0x09, 1);
        }
        catch(LibUsbException lue)
        {
            if(lue.getErrorCode() < 0)
            {
                resetRequired = true;
            }
            else
            {
                mLog.error("Error performing test write to RTL2832 device - " + LibUsb.errorName(lue.getErrorCode()));
            }
        }

        if(resetRequired)
        {
            int status = LibUsb.resetDevice(getDeviceHandle());
            mLog.info("Resetting device - status: " + LibUsb.errorName(status));

            try
            {
                writeRegister(Block.USB, Address.USB_SYSCTL, 0x09, 1);
            }
            catch(LibUsbException lue)
            {
                mLog.error("Couldnt' reset RTL2832 device - setting error");
                throw new SourceException("unable to reset USB device - " + LibUsb.errorName(status));
            }
        }


        byte[] eeprom = null;

        try
        {
            /* Read the contents of the 256-byte EEPROM */
            eeprom = readEEPROM((short) 0, 256);
        }
        catch(Exception e)
        {
            mLog.error("error while reading the EEPROM device descriptor", e);
        }

        try
        {
            mDescriptor = new Descriptor(eeprom);

            if(eeprom == null)
            {
                mLog.error("eeprom byte array was null - constructed empty descriptor object");
            }
        }
        catch(Exception e)
        {
            mLog.error("error while constructing device descriptor using descriptor byte array " +
                (eeprom == null ? "[null]" : Arrays.toString(eeprom)), e);
        }

        initBaseband();

        TunerType tunerType = identifyTunerType();

        if(tunerType == TunerType.UNKNOWN)
        {
            throw new SourceException("Unrecognized RTL-2832 embedded tuner type: " + tunerType);
        }

        mEmbeddedTuner = TunerFactory.getRtlEmbeddedTuner(tunerType, new ControllerAdapter(this));

        try
        {
            enableI2CRepeater(true);
            getEmbeddedTuner().initTuner();
            enableI2CRepeater(false);
        }
        catch(UsbException ue)
        {
            throw new SourceException("Unable to initialize " + tunerType, ue);
        }

        setMinimumFrequency(mEmbeddedTuner.getMinimumFrequencySupported());
        setMaximumFrequency(mEmbeddedTuner.getMaximumFrequencySupported());
        setMiddleUnusableHalfBandwidth(mEmbeddedTuner.getDcSpikeHalfBandwidth());
        setUsableBandwidthPercentage(mEmbeddedTuner.getUsableBandwidthPercent());

        try
        {
            setSampleRate(DEFAULT_SAMPLE_RATE);
        }
        catch(Exception e)
        {
            throw new SourceException("RTL2832 Tuner Controller - couldn't set default sample rate", e);
        }
    }

    @Override
    protected void deviceStop()
    {
        try
        {
            deinitBaseband();
        }
        catch(Exception e)
        {
//            mLog.error("Error on device stop", e);
            //No-op
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
     * Sets the enabled state of the bias-t
     * @param enabled true to turn-on the bias-t or false to turn-off the bias-t.
     */
    public void setBiasT(boolean enabled)
    {
        setGPIOOutput((byte)0x01);
        setGPIOBit((byte)0x01, enabled);
        mBiasTEnabled = enabled;
    }

    /**
     * Indicates if the bias-t is enabled.
     * @return true if enabled.
     */
    public boolean isBiasT()
    {
        return mBiasTEnabled;
    }

    private void setIFFrequency(int frequency) throws LibUsbException
    {
        long ifFrequency = ((long) TWO_TO_22_POWER * (long) frequency) / (long) mOscillatorFrequency * -1;

        /* Write byte 2 (high) */
        writeDemodRegister(Page.ONE, (short) 0x19, (short) (Long.rotateRight(ifFrequency, 16) & 0x3F), 1);

        /* Write byte 1 (middle) */
        writeDemodRegister(Page.ONE, (short) 0x1A, (short) (Long.rotateRight(ifFrequency, 8) & 0xFF), 1);

        /* Write byte 0 (low) */
        writeDemodRegister(Page.ONE, (short) 0x1B, (short) (ifFrequency & 0xFF), 1);
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
                if(hasEmbeddedTuner())
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
            int serial = (0xFF & getDeviceDescriptor().iSerialNumber());
            return TunerClass.RTL2832 + " " + serial;
        }
    }

    /**
     * Sets the filters for the sample rate
     * @param sampleRate to setup
     * @throws SourceException if there is an error
     */
    public void setSampleRateFilters(int sampleRate) throws SourceException
    {
        getEmbeddedTuner().setSampleRateFilters(sampleRate);
    }

    /**
     * Identifies the embedded tuner type
     */
    public TunerType getTunerType()
    {
        if(getEmbeddedTuner() != null)
        {
            return getEmbeddedTuner().getTunerType();
        }

        return TunerType.UNKNOWN;
    }

    /**
     * Identifies the embedded tuner type
     * @return tuner type
     * @throws SourceException if there is an error
     */
    private TunerType identifyTunerType() throws SourceException
    {
        TunerType tunerClass = TunerType.UNKNOWN;

        try
        {
            enableI2CRepeater(true);

            boolean controlI2CRepeater = false;

            /* Test for each tuner type until we find the correct one */
            if(isTuner(TunerTypeCheck.E4K, controlI2CRepeater))
            {
                tunerClass = TunerType.ELONICS_E4000;
            }
            else if(isTuner(TunerTypeCheck.R820T, controlI2CRepeater))
            {
                tunerClass = TunerType.RAFAELMICRO_R820T;
            }
            else if(isTuner(TunerTypeCheck.FC0013, controlI2CRepeater))
            {
                tunerClass = TunerType.FITIPOWER_FC0013;
            }
            else if(isTuner(TunerTypeCheck.R828D, controlI2CRepeater))
            {
                tunerClass = TunerType.RAFAELMICRO_R828D;
            }
            else if(isTuner(TunerTypeCheck.FC2580, controlI2CRepeater))
            {
                tunerClass = TunerType.FCI_FC2580;
            }
            else if(isTuner(TunerTypeCheck.FC0012, controlI2CRepeater))
            {
                tunerClass = TunerType.FITIPOWER_FC0012;
            }

            enableI2CRepeater(false);
        }
        catch(Exception e)
        {
            mLog.error("error while determining tuner type", e);
        }

        return tunerClass;
    }

    @Override
    protected void prepareStreaming()
    {
        resetUSBBuffer();
    }

    /**
     * Resets the USB buffer
     * @throws LibUsbException if there is an error
     */
    public void resetUSBBuffer() throws LibUsbException
    {
        writeRegister(Block.USB, Address.USB_EPA_CTL, 0x1002, 2);
        writeRegister(Block.USB, Address.USB_EPA_CTL, 0x0000, 2);
    }

    /**
     * Initializes the RF baseband system
     * @throws LibUsbException if there is an error
     */
    public void initBaseband() throws LibUsbException
    {
        /* Initialize USB */
        writeRegister(Block.USB, Address.USB_SYSCTL, 0x09, 1);
        writeRegister(Block.USB, Address.USB_EPA_MAXPKT, 0x0002, 2);
        writeRegister(Block.USB, Address.USB_EPA_CTL, 0x1002, 2);

        /* Power on demod */
        writeRegister(Block.SYS, Address.DEMOD_CTL_1, 0x22, 1);
        writeRegister(Block.SYS, Address.DEMOD_CTL, 0xE8, 1);

        /* Reset demod */
        writeDemodRegister(Page.ONE, (short) 0x01, 0x14, 1); //Bit 3 = soft reset
        writeDemodRegister(Page.ONE, (short) 0x01, 0x10, 1);

        /* Disable spectrum inversion and adjacent channel rejection */
        writeDemodRegister(Page.ONE, (short) 0x15, 0x00, 1);
        writeDemodRegister(Page.ONE, (short) 0x16, 0x0000, 2);

        /* Clear DDC shift and IF frequency registers */
        writeDemodRegister(Page.ONE, (short) 0x16, 0x00, 1);
        writeDemodRegister(Page.ONE, (short) 0x17, 0x00, 1);
        writeDemodRegister(Page.ONE, (short) 0x18, 0x00, 1);
        writeDemodRegister(Page.ONE, (short) 0x19, 0x00, 1);
        writeDemodRegister(Page.ONE, (short) 0x1A, 0x00, 1);
        writeDemodRegister(Page.ONE, (short) 0x1B, 0x00, 1);

        /* Set FIR coefficients */
        for(int x = 0; x < FIR_FILTER_COEFFICIENTS.length; x++)
        {
            writeDemodRegister(Page.ONE, (short) (0x1C + x), FIR_FILTER_COEFFICIENTS[x], 1);
        }

        /* Enable SDR mode, disable DAGC (bit 5) */
        writeDemodRegister(Page.ZERO, (short) 0x19, 0x05, 1);

        /* Init FSM state-holding register */
        writeDemodRegister(Page.ONE, (short) 0x93, 0xF0, 1);
        writeDemodRegister(Page.ONE, (short) 0x94, 0x0F, 1);

        /* Disable AGC (en_dagc, bit 0) (seems to have no effect) */
        writeDemodRegister(Page.ONE, (short) 0x11, 0x00, 1);

        /* Disable RF and IF AGC loop */
        writeDemodRegister(Page.ONE, (short) 0x04, 0x00, 1);

        /* Disable PID filter */
        writeDemodRegister(Page.ZERO, (short) 0x61, 0x60, 1);

        /* opt_adc_iq = 0, default ADC_I/ADC_Q datapath */
        writeDemodRegister(Page.ZERO, (short) 0x06, 0x80, 1);

        /* Enable Zero-if mode (en_bbin bit),
         *        DC cancellation (en_dc_est),
         *        IQ estimation/compensation (en_iq_comp, en_iq_est) */
        writeDemodRegister(Page.ONE, (short) 0xB1, 0x1B, 1);

        /* Disable 4.096 MHz clock output on pin TP_CK0 */
        writeDemodRegister(Page.ZERO, (short) 0x0D, 0x83, 1);
    }

    /**
     * Turns off the RF baseband system
     * @throws IllegalArgumentException never
     * @throws UsbDisconnectedException of the device is no longer accessible
     */
    private void deinitBaseband() throws IllegalArgumentException, UsbDisconnectedException
    {
        writeRegister(Block.SYS, Address.DEMOD_CTL, 0x20, 1);
    }

    /**
     * Sets the General Purpose Input/Output (GPIO) register bit
     *
     * @param bitMask - bit mask with one for targeted register bits and zero for the non-targeted register bits
     * @param enabled - true to set the bit and false to clear the bit
     * @throws UsbDisconnectedException - if the tuner device is disconnected
     * @throws UsbException             - if there is a USB error while communicating with the device
     */
    private void setGPIOBit(byte bitMask, boolean enabled) throws LibUsbException
    {
        //Get current register value
        int value = readRegister(Block.SYS, Address.GPO, 1);

        //Update the masked bits
        if(enabled)
        {
            value |= bitMask;
        }
        else
        {
            value &= ~bitMask;
        }

        //Write the change back to the device
        writeRegister(Block.SYS, Address.GPO, value, 1);
    }

    /**
     * Enables GPIO Output
     *
     * @param bitMask - mask containing one bit value in targeted bit field(s)
     * @throws UsbDisconnectedException
     * @throws UsbException
     */
    private void setGPIOOutput(byte bitMask) throws LibUsbException
    {
        //Get current register value
        int value = readRegister(Block.SYS, Address.GPD, 1);

        //Mask the value and rewrite it
        writeRegister(Block.SYS, Address.GPO, value & ~bitMask, 1);

        //Get current register value
        value = readRegister(Block.SYS, Address.GPOE, 1);

        //Mask the value and rewrite it
        writeRegister(Block.SYS, Address.GPOE, value | bitMask, 1);
    }

    /**
     * Enables or disables the I2C repeater
     * @param enabled to turn I2C on or off
     * @throws LibUsbException if there is an error
     */
    private void enableI2CRepeater(boolean enabled) throws LibUsbException
    {
        short address = 1;
        int value = (enabled ? 0x18 : 0x10);

        boolean isEnabledAlready = isI2CRepeaterEnabled();
        if (isEnabledAlready != enabled) {
           // mLog.info("trying to " + (enabled ? "enable" : "disable") + " i2c repeater, which is currently " + (isEnabledAlready ? "enabled" : "disabled"));
            writeDemodRegister(Page.ONE, address, value, 1);
        }
    }

    /**
     * Indicates if the I2C repeater is currently enabled or turned on.
     */
    private boolean isI2CRepeaterEnabled()
    {
        return readDemodRegister(Page.ONE, (short) 0x1, 1) == 0x18;
    }

    /**
     * Reads the I2C register
     * @param i2CAddress address of the register
     * @param i2CRegister register
     * @param controlI2CRepeater set to true to have this method turn repeater on/off for this invocation or false if
     * the repeater has already been enabled.
     * @return register value
     * @throws LibUsbException if there is an error
     */
    private int readI2CRegister(byte i2CAddress, byte i2CRegister, boolean controlI2CRepeater) throws LibUsbException
    {
        short address = (short) (i2CAddress & 0xFF);
        ByteBuffer buffer = ByteBuffer.allocateDirect(1);
        buffer.put(i2CRegister);
        buffer.rewind();
        ByteBuffer data = ByteBuffer.allocateDirect(1);

        if(controlI2CRepeater)
        {
            enableI2CRepeater(true);
            write(address, Block.I2C, buffer);
            read(address, Block.I2C, data);
            enableI2CRepeater(false);
        }
        else
        {
            write(address, Block.I2C, buffer);
            read(address, Block.I2C, data);
        }

        return (data.get() & 0xFF);
    }

    /**
     * Writes to the I2C register
     * @param i2CAddress of the register
     * @param i2CRegister register
     * @param value to write
     * @param controlI2CRepeater set to true to have this method turn repeater on/off for this invocation or false if
     * the repeater has already been enabled.
     * @throws LibUsbException if there is an error
     */
    private void writeI2CRegister(byte i2CAddress, byte i2CRegister, byte value, boolean controlI2CRepeater)
            throws LibUsbException
    {

        short address = (short) (i2CAddress & 0xFF);
        ByteBuffer buffer = ByteBuffer.allocateDirect(2);
        buffer.put(i2CRegister);
        buffer.put(value);

        buffer.rewind();

        if(controlI2CRepeater)
        {
            enableI2CRepeater(true);
            write(address, Block.I2C, buffer);
            enableI2CRepeater(false);
        }
        else
        {
            write(address, Block.I2C, buffer);
        }
    }

    /**
     * Writes to the demod register
     * @param page of register
     * @param address of resister
     * @param value to write
     * @param length of value in bytes
     * @throws LibUsbException on error
     */
    private void writeDemodRegister(Page page, short address, int value, int length) throws LibUsbException
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(length);
        buffer.order(ByteOrder.BIG_ENDIAN);

        if(length == 1)
        {
            buffer.put((byte) (value & 0xFF));
        }
        else if(length == 2)
        {
            buffer.putShort((short) (value & 0xFFFF));
        }
        else
        {
            throw new IllegalArgumentException("Cannot write value greater than 16 bits to the register - length [" +
                    length + "]");
        }

        short index = (short) (0x10 | page.getPage());
        short newAddress = (short) (address << 8 | 0x20);
        write(newAddress, index, buffer);
        readDemodRegister(Page.TEN, (short) 1, length);
    }

    /**
     * Reads the demod register
     * @param page of register
     * @param address of register
     * @param length of value to read in bytes
     * @return value read
     * @throws LibUsbException on error
     */
    private int readDemodRegister(Page page, short address, int length) throws LibUsbException
    {
        short index = page.getPage();
        short newAddress = (short) ((address << 8) | 0x20);

        ByteBuffer buffer = ByteBuffer.allocateDirect(length);

        read(newAddress, index, buffer);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        if(length == 2)
        {
            return (buffer.getShort() & 0xFFFF);
        }
        else
        {
            return (buffer.get() & 0xFF);
        }
    }

    /**
     * Writes a value to the register
     * @param block of register
     * @param address of register
     * @param value to write
     * @param length of value in bytes
     * @throws LibUsbException on error
     */
    private void writeRegister(Block block, Address address, int value, int length) throws LibUsbException
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(length);
        buffer.order(ByteOrder.BIG_ENDIAN);

        if(length == 1)
        {
            buffer.put((byte) (value & 0xFF));
        }
        else if(length == 2)
        {
            buffer.putShort((short) value);
        }
        else
        {
            throw new IllegalArgumentException("Cannot write value greater than 16 bits to the register - length [" +
                    length + "]");
        }

        buffer.rewind();
        write(address.getAddress(), block, buffer);
    }

    /**
     * Reads a value from a register
     * @param block of register
     * @param address of register
     * @param length of value to read
     * @return value read
     * @throws LibUsbException on error
     */
    private int readRegister(Block block, Address address, int length) throws LibUsbException
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(2);
        read(address.getAddress(), block, buffer);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        if(length == 2)
        {
            return (buffer.getShort() & 0xFFFF);
        }
        else
        {
            return (buffer.get() & 0xFF);
        }
    }

    /**
     * Writes a value
     * @param address of register
     * @param block of register
     * @param buffer containing value to write
     * @throws LibUsbException on error
     */
    private void write(short address, Block block, ByteBuffer buffer) throws LibUsbException
    {
        write(address, block.getWriteIndex(), buffer);
    }

    /**
     * Performs a control type write
     * @param value to write
     * @param index to write value to
     * @param buffer contents to write
     * @throws LibUsbException if there is an error
     */
    private void write(short value, short index, ByteBuffer buffer) throws LibUsbException
    {
        if(hasDeviceHandle())
        {
            int transferred = LibUsb.controlTransfer(getDeviceHandle(), CONTROL_ENDPOINT_OUT, REQUEST_ZERO, value,
                    index, buffer, TIMEOUT_US);

            if(transferred < 0)
            {
                throw new LibUsbException("error writing byte buffer", transferred);
            }
            else if(transferred != buffer.capacity())
            {
                throw new LibUsbException("transferred bytes [" + transferred + "] is not what was expected [" +
                    buffer.capacity() + "]", transferred);
            }
        }
        else
        {
            throw new LibUsbException("device handle is null", LibUsb.ERROR_NO_DEVICE);
        }
    }

    /**
     * Performs a control type read
     * @param address to read
     * @param index to read
     * @param buffer to store read content
     */
    private void read(short address, short index, ByteBuffer buffer) throws LibUsbException
    {
        if(isRunning())
        {
            int transferred = LibUsb.controlTransfer(getDeviceHandle(), CONTROL_ENDPOINT_IN, REQUEST_ZERO, address,
                    index, buffer, TIMEOUT_US);

            if(transferred < 0)
            {
                throw new LibUsbException("read error", transferred);
            }
            else if(transferred != buffer.capacity())
            {
                throw new LibUsbException("transferred bytes [" + transferred + "] is not what was expected [" +
                        buffer.capacity() + "]", transferred);
            }
        }
    }

    /**
     * Reads byte array from index at the address.
     *
     * @return big-endian byte array (needs to be swapped to be usable)
     */
    private void read(short address, Block block, ByteBuffer buffer) throws LibUsbException
    {
        read(address, block.getReadIndex(), buffer);
    }

    /**
     * Tests if the specified tuner type is contained in the usb tuner device.
     *
     * @param type - tuner type to test for
     * @param controlI2CRepeater - indicates if the method should control the I2C repeater independently
     * @return - true if the device is the specified tuner type
     */
    private boolean isTuner(TunerTypeCheck type, boolean controlI2CRepeater)
    {
        try
        {
            if(type == TunerTypeCheck.FC0012 || type == TunerTypeCheck.FC2580)
            {
                /* Initialize the GPIOs */
                setGPIOOutput((byte) 0x20);

                /* Reset tuner before probing */
                setGPIOBit((byte) 0x20, true);
                setGPIOBit((byte) 0x20, false);
            }

            int value = readI2CRegister(type.getI2CAddress(), type.getCheckAddress(), controlI2CRepeater);

            if(type == TunerTypeCheck.FC2580)
            {
                return ((value & 0x7F) == (type.getCheckValue() & 0xFF));
            }
            else
            {
                return (value == (type.getCheckValue() & 0xFF));
            }
        }
        catch(LibUsbException e)
        {
            //Do nothing ... it's not the specified tuner
        }

        return false;
    }

    public double getCurrentSampleRate()
    {
        return mSampleRate.getRate();
    }

    public int getSampleRateFromTuner() throws SourceException
    {
        int returnRate = DEFAULT_SAMPLE_RATE.getRate();

        getLock().lock();

        try
        {
            int high = readDemodRegister(Page.ONE, (short) 0x9F, 2);
            int low = readDemodRegister(Page.ONE, (short) 0xA1, 2);
            int ratio = Integer.rotateLeft(high, 16) | low;
            int rate = (mOscillatorFrequency * TWO_TO_22_POWER / ratio);

            SampleRate sampleRate = SampleRate.getClosest(rate);

            /* If we're not currently set to this rate, set it as the current rate */
            if(sampleRate.getRate() != rate)
            {
                setSampleRate(sampleRate);
                returnRate = sampleRate.getRate();
            }
        }
        catch(Exception e)
        {
            throw new SourceException("RTL2832 Tuner Controller - cannot get current sample rate", e);
        }
        finally
        {
            getLock().unlock();
        }

        return returnRate;
    }

    /**
     * Sets the sampling rate of the RTL2832
     * @param sampleRate to apply
     * @throws SourceException
     */
    public void setSampleRate(SampleRate sampleRate) throws SourceException
    {
        getLock().lock();

        try
        {
            /* Write high-order 16-bits of sample rate ratio to demod register */
            writeDemodRegister(Page.ONE, (short) 0x9F, sampleRate.getRatioHighBits(), 2);

            /* Write low-order 16-bits of sample rate ratio to demod register.
             * Note: none of the defined rates have a low order ratio value, so we
             * simply write a zero to the register */
            writeDemodRegister(Page.ONE, (short) 0xA1, 0, 2);

            /* Set sample rate correction to 0 */
            setSampleRateFrequencyCorrection(0);

            /* Reset the demod for the changes to take effect */
            writeDemodRegister(Page.ONE, (short) 0x01, 0x14, 1);
            writeDemodRegister(Page.ONE, (short) 0x01, 0x10, 1);

            /* Apply any tuner specific sample rate filter settings */
            setSampleRateFilters(sampleRate.getRate());
            mSampleRate = sampleRate;
            mFrequencyController.setSampleRate(sampleRate.getRate());

            //Updates native buffer factory so that sample buffers can be accurately timestamped
            getNativeBufferFactory().setSamplesPerMillisecond(sampleRate.getRate() / 1000.0f);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Determines the size of USB transfer buffers according to the sample rate
     */
    private int getUSBTransferBufferSize(double sampleRate)
    {
        return USB_TRANSFER_BUFFER_SIZE;
    }

    public void setSampleRateFrequencyCorrection(int ppm) throws SourceException
    {
        int offset = -ppm * TWO_TO_22_POWER / 1_000_000;
        writeDemodRegister(Page.ONE, (short) 0x3F, (offset & 0xFF), 1);
        writeDemodRegister(Page.ONE, (short) 0x3E, (Integer.rotateRight(offset, 8) & 0xFF), 1);
        /* Test to retune controller to apply frequency correction */
        try
        {
            mFrequencyController.setFrequency(mFrequencyController.getFrequency());
        }
        catch(Exception e)
        {
            throw new SourceException("Couldn't set sample rate frequency correction", e);
        }
    }

    public int getSampleRateFrequencyCorrection() throws UsbException
    {
        int high = readDemodRegister(Page.ONE, (short) 0x3E, 1);
        int low = readDemodRegister(Page.ONE, (short) 0x3F, 1);
        return (Integer.rotateLeft(high, 8) | low);
    }

    /**
     * Returns contents of the 256-byte EEPROM.  The contents are as follows:
     *
     * 256-byte EEPROM (in hex):
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
    public byte[] readEEPROM(short offset, int length) throws IllegalArgumentException
    {
        if(offset + length > 256)
        {
            throw new IllegalArgumentException("cannot read more than 256 bytes from EEPROM - requested read to byte [" +
                    (offset + length) + "]");
        }

        byte[] data = new byte[length];
        ByteBuffer buffer = ByteBuffer.allocateDirect(1);

        try
        {
            /* Tell the RTL-2832 to address the EEPROM */
            writeRegister(Block.I2C, Address.EEPROM, (byte) offset, 1);
        }
        catch(LibUsbException e)
        {
            mLog.error("usb error while attempting to set read address to EEPROM register, prior to reading the EEPROM " +
                    "device descriptor", e);
        }

        for(int x = 0; x < length; x++)
        {
            try
            {
                read(EEPROM_ADDRESS, Block.I2C, buffer);
                data[x] = buffer.get();
                buffer.rewind();
            }
            catch(Exception e)
            {
                mLog.error("error while reading eeprom byte [" + x + "/" + length + "] aborting eeprom read and " +
                        "returning partially filled descriptor byte array", e);
                x = length;
            }
        }

        return data;
    }

    /**
     * Writes a single byte to the 256-byte EEPROM using the specified offset.
     *
     * Note: introduce a 5 millisecond delay between each successive write to
     * the EEPROM or subsequent writes may fail.
     */
    public void writeEEPROMByte(byte offset, byte value) throws IllegalArgumentException, UsbDisconnectedException
    {
        if(offset < 0 || offset > 255)
        {
            throw new IllegalArgumentException("RTL2832 Tuner Controller - EEPROM offset must be within range of 0 - 255");
        }

        int offsetAndValue = Integer.rotateLeft((0xFF & offset), 8) | (0xFF & value);
        writeRegister(Block.I2C, Address.EEPROM, offsetAndValue, 2);
    }

    public enum Address
    {
        USB_SYSCTL(0x2000),
        USB_CTRL(0x2010),
        USB_STAT(0x2014),
        USB_EPA_CFG(0x2144),
        USB_EPA_CTL(0x2148),
        USB_EPA_MAXPKT(0x2158),
        USB_EPA_MAXPKT_2(0x215A),
        USB_EPA_FIFO_CFG(0x2160),
        DEMOD_CTL(0x3000),
        GPO(0x3001),
        GPI(0x3002),
        GPOE(0x3003),
        GPD(0x3004),
        SYSINTE(0x3005),
        SYSINTS(0x3006),
        GP_CFG0(0x3007),
        GP_CFG1(0x3008),
        SYSINTE_1(0x3009),
        SYSINTS_1(0x300A),
        DEMOD_CTL_1(0x300B),
        IR_SUSPEND(0x300C),
        EEPROM(0xA0);

        private int mAddress;

        Address(int address)
        {
            mAddress = address;
        }

        public short getAddress()
        {
            return (short) mAddress;
        }
    }

    public enum Page
    {
        ZERO(0x0),
        ONE(0x1),
        TEN(0xA);

        private int mPage;

        Page(int page)
        {
            mPage = page;
        }

        public byte getPage()
        {
            return (byte) (mPage & 0xFF);
        }
    }

    public enum SampleMode
    {
        QUADRATURE, DIRECT;
    }

    public enum Block
    {
        DEMOD(0),
        USB(1),
        SYS(2),
        TUN(3),
        ROM(4),
        IR(5),
        I2C(6); //I2C controller

        private int mValue;

        Block(int value)
        {
            mValue = value;
        }

        public int getValue()
        {
            return mValue;
        }

        /**
         * Returns the value left shifted 8 bits
         */
        public short getReadIndex()
        {
            return (short) Integer.rotateLeft(mValue, 8);
        }

        public short getWriteIndex()
        {
            return (short) (getReadIndex() | 0x10);
        }
    }

    /**
     * Sample rates supported by the RTL-2832.
     *
     * Formula to calculate the ratio value:
     *
     * ratio = ( ( crystal_frequency * 2^22 ) / sample_rate ) & ~3
     *
     * Default crystal_frequency is 28,800,000
     *
     * This produces a 32-bit value that has to be set in 2 x 16-bit registers.
     * Place the high 16-bit value in ratioMSB and the low 16-bit value in
     * ratioLSB.  Use integer for these values to avoid sign-extension issues.
     *
     * Mask the value with 0xFFFF when setting the register.
     */
    public enum SampleRate
    {
        /* Note: sample rates below 1.0MHz are subject to aliasing */
        RATE_0_230MHZ(0x1F40, 230400, "0.230 MHz"),
        RATE_0_240MHZ(0x1E00, 240000, "0.240 MHz"),
        RATE_0_256MHZ(0x1C20, 256000, "0.256 MHz"),
        RATE_0_288MHZ(0x1900, 288000, "0.288 MHz"),
        RATE_0_300MHZ(0x1800, 300000, "0.300 MHz"),
        RATE_0_960MHZ(0x0780, 960000, "0.960 MHz"),
        RATE_1_024MHZ(0x0708, 1024000, "1.024 MHz"),
        RATE_1_200MHZ(0x0600, 1200000, "1.200 MHz"),
        RATE_1_440MHZ(0x0500, 1440000, "1.440 MHz"),
        RATE_1_600MHZ(0x0480, 1600000, "1.600 MHz"),
        RATE_1_800MHZ(0x0400, 1800000, "1.800 MHz"),
        RATE_1_920MHZ(0x03C0, 1920000, "1.920 MHz"),
        RATE_2_048MHZ(0x0384, 2048000, "2.048 MHz"),
        RATE_2_304MHZ(0x0320, 2304000, "2.304 MHz"),
        RATE_2_400MHZ(0x0300, 2400000, "2.400 MHz"),
        RATE_2_560MHZ(0x02D0, 2560000, "2.560 MHz"),
        RATE_2_880MHZ(0x0280, 2880000, "2.880 MHz");

        private int mRatioHigh;
        private int mRate;
        private String mLabel;

        SampleRate(int ratioHigh, int rate, String label)
        {
            mRatioHigh = ratioHigh;
            mRate = rate;
            mLabel = label;
        }

        public int getRatioHighBits()
        {
            return mRatioHigh;
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

        /**
         * Returns the sample rate that is equal to the argument or the next
         * higher sample rate
         *
         * @param sampleRate
         * @return
         */
        public static SampleRate getClosest(int sampleRate)
        {
            for(SampleRate rate : values())
            {
                if(rate.getRate() >= sampleRate)
                {
                    return rate;
                }
            }

            return DEFAULT_SAMPLE_RATE;
        }
    }

    public enum TunerTypeCheck
    {
        E4K(0xC8, 0x02, 0x40),
        FC0012(0xC6, 0x00, 0xA1),
        FC0013(0xC6, 0x00, 0xA3),
        FC2580(0xAC, 0x01, 0x56),
        R820T(0x34, 0x00, 0x69),
        R828D(0x74, 0x00, 0x69);

        private int mI2CAddress;
        private int mCheckAddress;
        private int mCheckValue;

        TunerTypeCheck(int i2c, int address, int value)
        {
            mI2CAddress = i2c;
            mCheckAddress = address;
            mCheckValue = value;
        }

        public byte getI2CAddress()
        {
            return (byte) mI2CAddress;
        }

        public byte getCheckAddress()
        {
            return (byte) mCheckAddress;
        }

        public byte getCheckValue()
        {
            return (byte) mCheckValue;
        }
    }

    /**
     * RTL2832 EEPROM byte array descriptor parsing class
     */
    public class Descriptor
    {
        private byte[] mData;
        private ArrayList<String> mLabels = new ArrayList<String>();

        public Descriptor(byte[] data)
        {
            if(data != null)
            {
                mData = data;
            }
            else
            {
                data = new byte[256];
            }

            getLabels();
        }

        /**
         * Indicates if this is a rtl-sdr.com V4 R828D dongle with notch filtering.
         */
        public boolean isRtlSdrV4()
        {
            return "RTLSDRBlog".equals(getVendorLabel()) && "Blog V4".equals(getProductLabel());
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
            return mLabels.get(0);
        }

        public String getProductID()
        {
            int id = Integer.rotateLeft((0xFF & mData[5]), 8) | (0xFF & mData[4]);
            return String.format("%04X", id);
        }

        public String getProductLabel()
        {
            return mLabels.get(1);
        }

        public boolean hasSerial()
        {
            return mData[6] == (byte) 0xA5;
        }

        public String getSerial()
        {
            return mLabels.get(2);
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

        private void getLabels()
        {
            mLabels.clear();
            int start = 0x09;
            while(start < 256)
            {
                start = getLabel(start);
            }
        }

        private int getLabel(int start)
        {
            /* Validate length and check second byte for ETX (0x03) */
            if(start > 254 || mData[start + 1] != (byte) 0x03)
            {
                return 256;
            }

            /* Get label length, including the length and ETX bytes */
            int length = 0xFF & mData[start];

            if(start + length > 255)
            {
                return 256;
            }

            /* Get the label bytes */
            byte[] data = Arrays.copyOfRange(mData, start + 2, start + length);

            /* Translate the bytes as UTF-16 Little Endian and store the label */
            String label = new String(data, Charset.forName("UTF-16LE"));
            mLabels.add(label);
            return start + length;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("RTL-2832 EEPROM Descriptor\n");

            sb.append("Vendor: ");
            sb.append(getVendorID());
            sb.append(" [");
            sb.append(getVendorLabel());
            sb.append("]\n");

            sb.append("Product: ");
            sb.append(getProductID());
            sb.append(" [");
            sb.append(getProductLabel());
            sb.append("]\n");

            sb.append("Serial: ");
            if(hasSerial())
            {
                sb.append("yes [");
                sb.append(getSerial());
                sb.append("]\n");
            }
            else
            {
                sb.append("no\n");
            }

            sb.append("Remote Wakeup Enabled: ");
            sb.append((remoteWakeupEnabled() ? "yes" : "no"));
            sb.append("\n");

            sb.append("IR Enabled: ");
            sb.append((irEnabled() ? "yes" : "no"));
            sb.append("\n");

            if(mLabels.size() > 3)
            {
                sb.append("Additional Labels: ");

                for(int x = 3; x < mLabels.size(); x++)
                {
                    sb.append(" [");
                    sb.append(mLabels.get(x));
                    sb.append("\n");
                }
            }

            return sb.toString();
        }
    }

    /**
     * Adapter that provides access to the private methods of this RTL2832 USB tuner device's USB and other interfaces.
     *
     * Note: most of the methods exposed by this tuner are private in the tuner controller class by design to prevent
     * the user from incorrectly accessing them.  This private adapter exposes these as public methods that the various
     * RTL2832 tuner implementations need access to.
     */
    public class ControllerAdapter
    {
        private RTL2832TunerController mController;

        /**
         * Constructs an instance
         * @param controller for this adapter
         */
        public ControllerAdapter(RTL2832TunerController controller)
        {
            mController = controller;
        }

        /**
         * Indicates if the tuner controller is running and that the Device Handle is active.
         */
        public boolean isRunning()
        {
            return mController.isRunning();
        }

        /**
         * Indicates if this is a rtl-sdr.com V4 dongle with support for notch filtering.
         * @return true if this is a V4 R828D tuner.
         */
        public boolean isV4Dongle()
        {
            return mController.getDescriptor().isRtlSdrV4();
        }

        /**
         * Device handle for the tuner controller.
         * @return
         */
        public DeviceHandle getDeviceHandle()
        {
            return mController.getDeviceHandle();
        }

        /**
         * Thread lock for controlling access to configuration changes that require multiple USB control message
         * sequences.
         *
         * @return lock.
         */
        public ReentrantLock getLock()
        {
            return mController.getLock();
        }

        /**
         * Enables the I2C repeater
         */
        public void enableI2CRepeater()
        {
            mController.enableI2CRepeater(true);
        }

        /**
         * Disables the I2C repeater
         */
        public void disableI2CRepeater()
        {
            mController.enableI2CRepeater(false);
        }

        /**
         * Indicates if the I2C repeater is enabled
         */
        public boolean isI2CRepeaterEnabled()
        {
            return mController.isI2CRepeaterEnabled();
        }

        /**
         * Writes to the Demod registers
         * @param page value
         * @param address value
         * @param value to write
         * @param length of the value in bytes
         * @throws LibUsbException if there is an error
         */
        public void writeDemodRegister(Page page, short address, int value, int length) throws LibUsbException
        {
            mController.writeDemodRegister(page, address, value, length);
        }

        /**
         * Writes to the I2C register
         * @param i2CAddress address
         * @param i2CRegister register
         * @param value to write
         * @param controlI2CRepeater true to turn on/off the I2C repeater, or false if it's already been turned on.
         * @throws LibUsbException if there is an error
         */
        public void writeI2CRegister(byte i2CAddress, byte i2CRegister, byte value, boolean controlI2CRepeater)
                throws LibUsbException
        {
            mController.writeI2CRegister(i2CAddress, i2CRegister, value, controlI2CRepeater);
        }

        /**
         * Reads the I2C register
         * @param i2CAddress address
         * @param i2CRegister register
         * @param controlI2CRepeater to control the I2C repeater
         * @return value
         * @throws LibUsbException if there is an error
         */
        public int readI2CRegister(byte i2CAddress, byte i2CRegister, boolean controlI2CRepeater) throws LibUsbException
        {
            return mController.readI2CRegister(i2CAddress, i2CRegister, controlI2CRepeater);
        }

        /**
         * Reads a value
         * @param address to read
         * @param block at the address
         * @param buffer to read the value into
         * @throws LibUsbException
         */
        public void read(short address, Block block, ByteBuffer buffer) throws LibUsbException
        {
            mController.read(address, block, buffer);
        }

        /**
         * Sets the intermediate frequency (IF) for the digitizer
         */
        public void setIFFrequency(int frequency)
        {
            mController.setIFFrequency(frequency);
        }
    }
}