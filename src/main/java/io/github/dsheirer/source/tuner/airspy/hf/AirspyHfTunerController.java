/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.airspy.hf;

import io.github.dsheirer.buffer.INativeBufferFactory;
import io.github.dsheirer.buffer.airspy.hf.AirspyHfNativeBufferFactory;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.usb.USBTunerController;
import io.github.dsheirer.util.ByteUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsb;

/**
 * Airspy HF+ and HF Discovery tuner controller.
 */
public class AirspyHfTunerController extends USBTunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(AirspyHfTunerController.class);
    private static final AirspyHfSampleRate DEFAULT_SAMPLE_RATE = new AirspyHfSampleRate(0, 768_000, false);
    private static final long IF_SHIFT_LIF = 0;
    private static final long IF_SHIFT_ZIF = 5_000;
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 500_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 260_000_000;
    private static final byte REQUEST_TYPE_IN = LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE;
    private static final byte REQUEST_TYPE_OUT = LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE;
    private static final int BUFFER_SAMPLE_COUNT = 1024;
    private static final int BUFFER_BYTE_SIZE = BUFFER_SAMPLE_COUNT * 2 * 2; //2x 16-bit samples (4-bytes total) per complex.
    private AirspyHfNativeBufferFactory mNativeBufferFactory = new AirspyHfNativeBufferFactory();
    private List<AirspyHfSampleRate> mAvailableSampleRates;
    private AirspyHfSampleRate mCurrentSampleRate = DEFAULT_SAMPLE_RATE;
    private BoardId mBoardId;
    private String mSerialNumber;
    private Attenuation mAttenuation = Attenuation.A0;
    private boolean mAgcEnabled;
    private boolean mLnaEnabled;
    private long mTunedFrequency;
    private static final int CALIBRATION_MAGIC = 0xA5CA71B0;
    private int mCalibrationRecordMagicNumber;
    private int mCalibrationRecordPPB;
    private int mCalibrationRecordVctcxo;

    /**
     * Constructs an instance
     * @param bus for USB
     * @param portAddress for USB
     * @param tunerErrorListener to receive tuner error notifications.
     */
    public AirspyHfTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(bus, portAddress, tunerErrorListener);
        setMinimumFrequency(MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(MAXIMUM_TUNABLE_FREQUENCY_HZ);
        setUsableBandwidthPercentage(.9); //90% usable after filter rolloff
        setMiddleUnusableHalfBandwidth(3000);
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        super.apply(config);

        if(config instanceof AirspyHfTunerConfiguration airspyConfig)
        {
            int sampleRate = airspyConfig.getSampleRate();

            boolean found = false;
            //The tuner supports several sample rates, but in testing the only rate that seems to function
            //correctly is the 912kHz rate, which should be index 0.
//            for(AirspyHfSampleRate rate: getAvailableSampleRates())
//            {
//                if(rate.getSampleRate() == sampleRate)
//                {
//                    setSampleRate(rate);
//                    found = true;
//                    break;
//                }
//            }

            if(!found)
            {
                setSampleRate(getAvailableSampleRates().get(0));
            }

            try
            {
                setAgc(airspyConfig.isAgc());
            }
            catch(IOException ioe)
            {
                mLog.error("Error setting AGC enabled [" + airspyConfig.isAgc() + "]");
            }

            try
            {
                setLna(airspyConfig.isLna());
            }
            catch(IOException ioe)
            {
                mLog.error("Error setting LNA enabled [" + airspyConfig.isLna() + "]");
            }

            try
            {
                setAttenuation(airspyConfig.getAttenuation());
            }
            catch(IOException ioe)
            {
                mLog.error("Error setting attenuation [" + airspyConfig.getAttenuation() + "]");
            }
        }
    }

    @Override
    public int getBufferSampleCount()
    {
        return 1024;
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mTunedFrequency;
    }

    /**
     * Serial number of the tuner.
     * @return serial number
     */
    public String getSerialNumber()
    {
        return mSerialNumber;
    }

    /**
     * Board identifier
     * @return board ID
     */
    public BoardId getBoardId()
    {
        return mBoardId;
    }

    /**
     * Sets the tuned frequency.
     * @param frequency to set
     * @throws SourceException if there is an error
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        double if_shift = mCurrentSampleRate.isLowIf() ? IF_SHIFT_ZIF : IF_SHIFT_LIF;
        double adjusted_freq_hz = frequency * (1.0e9 + mCalibrationRecordPPB) * 1.0e-9;
        int freq_khz = (int)((adjusted_freq_hz + if_shift) * 1e-3);
        ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        buffer.putInt(freq_khz).flip();

        try
        {
            writeData(Request.SET_FREQUENCY, buffer);
            mTunedFrequency = frequency;
        }
        catch(IOException ioe)
        {
            mLog.info("Error setting tuned frequency to " + frequency, ioe);
            throw new SourceException("Error setting frequency", ioe);
        }

        try
        {
            byte[] deltaBytes = read(Request.GET_FREQUENCY_DELTA, 0, 4);
            int delta = ByteUtil.toInteger(deltaBytes, 0);

            if(delta != 0)
            {
                mLog.warn("Frequency delta after setting tuned frequency [" + frequency + "] is: " + delta);
            }
        }
        catch(IOException ioe)
        {
            mLog.error("Error reading frequency delta from Airspy HF+ tuner", ioe);
        }
    }

    @Override
    public double getCurrentSampleRate() throws SourceException
    {
        loadSampleRates();
        return mCurrentSampleRate.getSampleRate();
    }

    /**
     * Access the current sample rate object.
     * @return current sample rate.
     */
    public AirspyHfSampleRate getCurrentAirspySampleRate()
    {
        return mCurrentSampleRate;
    }

    /**
     * Sets the sample rate for the tuner.
     * @param sampleRate to apply.
     */
    public void setSampleRate(AirspyHfSampleRate sampleRate) throws SourceException
    {
        if(sampleRate != null)
        {
            if(isSupportedSampleRate(sampleRate.getSampleRate()))
            {
                getNativeBufferFactory().setSamplesPerMillisecond((float)sampleRate.getSampleRate() / 1000.0f);

                LibUsb.clearHalt(getDeviceHandle(), USB_BULK_TRANSFER_ENDPOINT);
                int status = writeIndex(Request.SET_SAMPLE_RATE, sampleRate.getIndex());

                if(status != 0)
                {
                    throw new SourceException("Unable to set Airspy HF sample rate to: " + sampleRate);
                }

                mFrequencyController.setSampleRate(sampleRate.getSampleRate());
                getNativeBufferFactory().setSamplesPerMillisecond(sampleRate.getSampleRate() / 1000.0f);
                mCurrentSampleRate = sampleRate;

                //Set the frequency again to adjust for any switch between ZIF & LIF
                setTunedFrequency(mTunedFrequency);
            }
        }
    }

    /**
     * Indicates if the sample rate is supported.
     * @param sampleRate to check
     * @return true if the rate matches one of the available sample rates.
     */
    public boolean isSupportedSampleRate(int sampleRate)
    {
        if(mAvailableSampleRates != null && !mAvailableSampleRates.isEmpty())
        {
            for(AirspyHfSampleRate availableRate: mAvailableSampleRates)
            {
                if(availableRate.getSampleRate() == sampleRate)
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Available/supported sample rates.
     * @return available sample rates
     * @throws IllegalStateException if the device has not yet been started
     */
    public List<AirspyHfSampleRate> getAvailableSampleRates()
    {
        if(mAvailableSampleRates == null || mAvailableSampleRates.isEmpty())
        {
            throw new IllegalStateException("Device must be started before accessing available sample rates");
        }

        return mAvailableSampleRates;
    }

    /**
     * Tuner type.
     * @return for this tuner
     */
    @Override
    public TunerType getTunerType()
    {
        return TunerType.AIRSPY_HF_PLUS;
    }

    /**
     * Native buffer factory for creating native buffers from USB transfer buffers
     * @return factory
     */
    @Override
    protected INativeBufferFactory getNativeBufferFactory()
    {
        return mNativeBufferFactory;
    }

    /**
     * Byte size of USB transfer buffers
     */
    @Override
    protected int getTransferBufferSize()
    {
        return BUFFER_BYTE_SIZE;
    }

    /**
     * Implements additional device-specific start operations.
     * @throws SourceException if there is an issue
     */
    @Override
    protected void deviceStart() throws SourceException
    {
        int status = LibUsb.setInterfaceAltSetting(getDeviceHandle(), 0, 1);

        if(status != LibUsb.SUCCESS)
        {
            throw new SourceException("Can't set Airspy HF interface 0 alternate setting 1 - " + LibUsb.errorName(status));
        }

        loadBoardIdAndSerialNumber();
        loadSampleRates();

        try
        {
            loadDeviceConfig();
        }
        catch(IOException ioe)
        {
            mLog.info("Error getting device configuration and calibration info", ioe);
        }
    }

    /**
     * Implements additional device-specific stop operations.
     * @throws SourceException if there is an issue
     */
    @Override
    protected void deviceStop()
    {
        try
        {
            setReceiverMode(false);
        }
        catch(Exception e)
        {
            mLog.error("Error setting Airspy HF tuner receiver mode to false for device stop.");
        }
    }

    /**
     * Preparation operations for starting sample stream.
     */
    @Override
    protected void prepareStreaming()
    {
        try
        {
            setReceiverMode(false);
        }
        catch(SourceException ioe)
        {
            mLog.error("Error setting Airspy HF tuner receiver mode to false to reset before we start streaming.");
            setErrorMessage("Unable to set receiver mode off");
            return;
        }

        LibUsb.clearHalt(getDeviceHandle(), USB_BULK_TRANSFER_ENDPOINT);

        try
        {
            setReceiverMode(true);
        }
        catch(SourceException ioe)
        {
            mLog.error("Error setting Airspy HF tuner receiver mode to true to start streaming.");
            setErrorMessage("Unable to set receiver mode on");
        }
    }

    /**
     * Cleanup operations for shutting down sample stream.
     */
    @Override
    protected void streamingCleanup()
    {
        try
        {
            setReceiverMode(false);
        }
        catch(SourceException ioe)
        {
            mLog.error("Error setting Airspy HF tuner receiver mode to false to stop streaming.");
        }
    }

    /**
     * LibUsb control transfer to request data from the tuner.
     * @param request to submit
     * @param count parameter associated with the request
     * @param bufferLength is the byte size of the buffer to receive the read data.
     * @return byte array containing the response value
     * @throws IOException if there is an error or the request cannot be completed.
     */
    private byte[] read(Request request, int count, int bufferLength) throws IOException
    {
        //Allocate a native/direct byte buffer outside of the JVM
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferLength);
        int status = LibUsb.controlTransfer(getDeviceHandle(), REQUEST_TYPE_IN, request.getRequest(), (short)0,
                (short)count, buffer, 0);

        if(status < 0)
        {
            throw new IOException("Unable to complete read request [" + request.name() + "] - libusb status [" + status +
                    "] - " + LibUsb.errorName(status));
        }

        byte[] result = new byte[bufferLength];
        buffer.get(result);
        return result;
    }

    /**
     * LibUsb control transfer to write data to the tuner with a value of zero and an index of zero.
     * @param request to submit
     * @param dataBuffer to write
     * @return direct byte buffer containing the value(s) to write in little-endian format
     * @throws IOException if there is an error or the request cannot be completed.
     */
    private void writeData(Request request, ByteBuffer dataBuffer) throws IOException
    {
        if(!dataBuffer.isDirect())
        {
            throw new IllegalArgumentException("Cannot write - must use a direct/native byte buffer");
        }

        int status = LibUsb.controlTransfer(getDeviceHandle(), REQUEST_TYPE_OUT, request.getRequest(), (short)0, (short)0,
                dataBuffer, 0);

        if(status < 0)
        {
            throw new IOException("Unable to complete write request [" + request.name() + "] - libusb status [" +
                    status + "] - " + LibUsb.errorName(status));
        }
    }

    /**
     * Writes the request with the specified value using an index of zero.
     * @param request type
     * @param value value to write
     * @return status code of the write operation.
     */
    private int writeValue(Request request, short value)
    {
        return LibUsb.controlTransfer(getDeviceHandle(), REQUEST_TYPE_OUT, request.getRequest(), value, (short)0,
                ByteBuffer.allocateDirect(0), 0);
    }

    /**
     * Writes the request with the specified index with a value of zero.
     * @param request type
     * @param index to write
     * @return status code of the write operation.
     */
    private int writeIndex(Request request, short index)
    {
        return LibUsb.controlTransfer(getDeviceHandle(), REQUEST_TYPE_OUT, request.getRequest(), (short)0, index,
                ByteBuffer.allocateDirect(0), 0);
    }

    /**
     * Sets the receiver mode to start/stop the sample stream
     * @param running true to start and false to stop.
     * @throws SourceException if there is an error
     */
    private void setReceiverMode(boolean running) throws SourceException
    {
        int status = writeValue(Request.RECEIVER_MODE, running ? (short)1 : (short)0);

        if(status != LibUsb.SUCCESS)
        {
            throw new SourceException("Unable to set receiver mode started to " + running + " - status [" + status +
                    "] " + LibUsb.errorName(status));
        }
   }

    /**
     * Reads the board ID and serial number from the device.
     * @throws IOException if there is an issue
     */
   private void loadBoardIdAndSerialNumber()
   {
       try
       {
           byte[] bytes = read(Request.GET_SERIAL_NUMBER_BOARD_ID, 0, 20);

           int boardId = ByteUtil.toInteger(bytes, 0);
           mBoardId = BoardId.fromValue(boardId);

           int serial1 = ByteUtil.toInteger(bytes, 4);
           int serial2 = ByteUtil.toInteger(bytes, 8);
           int serial3 = ByteUtil.toInteger(bytes, 12);
           int serial4 = ByteUtil.toInteger(bytes, 16);

           StringBuilder sb = new StringBuilder();
           sb.append(Integer.toHexString(serial1));
           sb.append(Integer.toHexString(serial2));
           sb.append(Integer.toHexString(serial3));
           sb.append(Integer.toHexString(serial4));
           mSerialNumber = sb.toString().toUpperCase();
       }
       catch(IOException ioe)
       {
            mLog.error("Error reading board ID and serial number from device", ioe);
            mBoardId = BoardId.UNKNOWN;
            mSerialNumber = "UNKNOWN";
       }
   }

    /**
     * Loads the sample rates and LIF/ZIF modes from the tuner's firmware or uses the default sample rate of 768 kHz.
     */
    private void loadSampleRates()
    {
        if(mAvailableSampleRates == null)
        {
            mAvailableSampleRates = new ArrayList<>();

            try
            {
                //Read the first 4 bytes to get the count of sample rates.
                byte[] bytes = read(Request.GET_SAMPLE_RATES, 0, 4);

                int count = ByteUtil.toInteger(bytes, 0);

                if(count > 0)
                {
                    //Read the count (again) plus the number of 4-byte integer values.
                    bytes = read(Request.GET_SAMPLE_RATES, count, count * 4);

                    //Read the architectures array.
                    byte[] architectures = read(Request.GET_SAMPLE_RATE_ARCHITECTURES, count, count);

                    for(int x = 0; x < count; x++)
                    {
                        int sampleRate = ByteUtil.toInteger(bytes, (x * 4));
                        boolean lowIf = architectures[x] == (byte)0x1;
                        mAvailableSampleRates.add(new AirspyHfSampleRate(x, sampleRate, lowIf));
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("Error reading sample rates from Airspy HF tuner", e);
            }

            //If we can't read sample rates from tuner, use the default sample rate.
            if(mAvailableSampleRates.isEmpty())
            {
                mAvailableSampleRates.add(DEFAULT_SAMPLE_RATE);
            }
        }
    }

    /**
     * Indicates if the Automatic Gain Control (AGC) is enabled.
     * @return enabled state, true or false.
     */
    public boolean getAgc()
    {
        return mAgcEnabled;
    }

    /**
     * Sets the Automatic Gain Control (AGC)
     * @param enabled true to turn on AGC or false to turn off AGC
     * @throws IOException if there is an error
     */
    public void setAgc(boolean enabled) throws IOException
    {
        int status = writeValue(Request.SET_HF_AGC, enabled ? (short)1 : (short)0);

        if(status != 0)
        {
            throw new IOException("Unable to set AGC enabled [" + enabled + "] - status code: " + status);
        }

        mAgcEnabled = enabled;
    }

    /**
     * Indicates if the Low Noise Amplifier (LNA) is enabled.
     * @return enabled state of the LNA
     */
    public boolean getLna()
    {
        return mLnaEnabled;
    }

    /**
     * Sets the Low Noise Amplifier (LNA)
     * @param enabled true to turn on or false to turn off
     * @throws IOException if there is an error
     */
    public void setLna(boolean enabled) throws IOException
    {
        int status = writeValue(Request.SET_HF_LNA, enabled ? (short)1 : (short)0);

        if(status != 0)
        {
            throw new IOException("Unable to set LNA enabled [" + enabled + "] - status code: " + status);
        }

        mLnaEnabled = enabled;
    }

    /**
     * Current attenuation value.
     */
    public Attenuation getAttenuation()
    {
        return mAttenuation;
    }

    /**
     * Sets the attenuation value.
     * @param attenuation value to set: 0 - 8.  Increases attenuation (ie decrease gain) by 6dB with each step.
     * @throws IOException if there is an error
     */
    public void setAttenuation(Attenuation attenuation) throws IOException
    {
        int status = writeValue(Request.SET_HF_ATT, attenuation.getValue());

        if(status != 0)
        {
            throw new IOException("Unable to set attenuation [" + attenuation + "] - status code: " + status);
        }
        mAttenuation = attenuation;
    }

    /**
     * Reads the device configuration from the tuner
     * @return byte array with configuration contents.
     */
    private void loadDeviceConfig() throws IOException
    {
        byte[] bytes = read(Request.CONFIG_READ, 0, 256);
        IntBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        mCalibrationRecordMagicNumber = buffer.get(0);
        mCalibrationRecordPPB = buffer.get(1);
        mCalibrationRecordVctcxo = buffer.get(2);
    }
}
