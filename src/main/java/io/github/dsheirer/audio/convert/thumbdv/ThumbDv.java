/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.audio.convert.thumbdv;

import com.fazecast.jSerialComm.SerialPort;
import io.github.dsheirer.audio.convert.thumbdv.message.AmbeMessage;
import io.github.dsheirer.audio.convert.thumbdv.message.AmbeMessageFactory;
import io.github.dsheirer.audio.convert.thumbdv.message.request.AmbeRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.DecodeSpeechRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.EncodeSpeechRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.ResetRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.SetVocoderParametersRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.response.AmbeResponse;
import io.github.dsheirer.gui.SDRTrunk;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Northwest Digital Radio (NWDR) ThumbDv dongle.
 *
 * Note: linux users must allow access to the serial port:
 *
 *  sudo usermod -a -G uucp username
 *  sudo usermod -a -G dialout username
 *  sudo usermod -a -G lock username
 *  sudo usermod -a -G tty username
 */
public class ThumbDv implements AutoCloseable
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRTrunk.class);
    private static final String PORT_DESCRIPTION = "USB-to-Serial Port (ftdi_sio)";
    private static final int BAUD_RATE = 460800;
    private static final byte PACKET_START = (byte)0x61;

    private SerialPort mSerialPort;
    private ScheduledFuture mSerialPortReaderHandle;

    public ThumbDv()
    {
    }

    public void close() throws IOException
    {
        if(mSerialPortReaderHandle != null)
        {
            mSerialPortReaderHandle.cancel(true);
            mSerialPortReaderHandle = null;
        }

        if(mSerialPort != null)
        {
            mSerialPort.closePort();
        }
    }

    public void start() throws IOException
    {
        SerialPort[] ports = SerialPort.getCommPorts();

        for(SerialPort port: ports)
        {
            if(port.getDescriptivePortName().contentEquals(PORT_DESCRIPTION))
            {
                mLog.debug("ThumbDv serial port found - initializing for event-based message processing");
                mSerialPort = port;
                mSerialPort.setBaudRate(BAUD_RATE);
                mSerialPort.openPort(0, 1000, 1000);

                if(!mSerialPort.isOpen())
                {
                    mLog.warn("Could not open serial port: " + mSerialPort.getSystemPortName());
                    throw new IOException("Could not open serial port:" + mSerialPort.getSystemPortName());
                }

                Runnable r = new SerialPortReader(mSerialPort.getInputStream());
                mSerialPortReaderHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(r, 100, 10, TimeUnit.MILLISECONDS);

                logSerialPort();

                return;
            }
        }

        throw new IOException("ThumbDV serial port not found");
    }

    public void send(byte[] message) throws IOException
    {
        if(mSerialPort == null)
        {
            throw new IOException("ThumbDv must be started before use");
        }

        int bytesWritten = mSerialPort.writeBytes(message, message.length);

        if(bytesWritten < 0)
        {
            throw new IOException("Unable to write message:" + Arrays.toString(message));
        }
        else if(bytesWritten != message.length)
        {
            throw new IOException("Unable to write message:" + Arrays.toString(message) + " Bytes Written:" + bytesWritten);
        }
    }

    /**
     * Sends the AMBE request message
     * @param request message
     * @throws IOException if there is an error
     */
    public void send(AmbeRequest request) throws IOException
    {
        send(request.getData());
    }

    private void receive(byte[] bytes)
    {
        AmbeMessage message = AmbeMessageFactory.getMessage(bytes);
        mLog.debug("RECEIVED:" + message.toString());
    }

    private void logSerialPort()
    {
        if(mSerialPort != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("\nPort:\t\t\t").append(mSerialPort.getSystemPortName()).append("\n");
            sb.append("Name:\t\t\t").append(mSerialPort.getDescriptivePortName()).append("\n");
            sb.append("Baud Rate:\t\t").append(mSerialPort.getBaudRate()).append("\n");
            sb.append("Data Bits:\t\t").append(mSerialPort.getNumDataBits()).append("\n");
            sb.append("Parity Bits:\t").append(mSerialPort.getParity()).append("\n");
            sb.append("Stop Bits:\t\t").append(mSerialPort.getNumStopBits()).append("\n");
            sb.append("Flow Control:\t").append(mSerialPort.getFlowControlSettings()).append("\n");
            sb.append("Is Open:\t\t").append(mSerialPort.isOpen()).append("\n");

            mLog.info(sb.toString());
        }
    }

    public class SerialPortReader implements Runnable
    {
        private InputStream mInputStream;

        public SerialPortReader(InputStream inputStream)
        {
            mInputStream = inputStream;
        }

        @Override
        public void run()
        {
            if(mInputStream != null)
            {
                try
                {
                    while(mInputStream.available() > 0)
                    {
                        byte[] buffer = new byte[400];

                        int bytesRead = mInputStream.readNBytes(buffer, 0, 1);

                        if(bytesRead == 1 && buffer[0] == PACKET_START)
                        {
                            while(mInputStream.available() < 2)
                            {
                                try
                                {
                                    Thread.sleep(1);
                                }
                                catch(InterruptedException ie)
                                {
                                }
                            }

                            bytesRead = mInputStream.readNBytes(buffer, 1, 2);

                            if(bytesRead == 2)
                            {
                                int length = (0xFF & buffer[1]) << 8;
                                length += (0xFF & buffer[2]);

                                if(0 < length && length < 400)
                                {
                                    length++; //Add a byte for the type byte

                                    while(mInputStream.available() < length)
                                    {
                                        try
                                        {
                                            Thread.sleep(1);
                                        }
                                        catch(InterruptedException ie)
                                        {
                                        }
                                    }

                                    bytesRead = mInputStream.readNBytes(buffer, 3, length);

                                    if(bytesRead == length)
                                    {
                                        receive(Arrays.copyOf(buffer, length + 3));
                                    }
                                    else
                                    {
                                        mLog.debug("Expected [" + length + "] but received [" + bytesRead + "] bytes - " + Arrays.toString(buffer));
                                    }
                                }
                                else
                                {
                                    mLog.error("Received packet with unexpected length: " + length);
                                    //Don't process the buffer ... let the reader read and inspect 1 byte at a time
                                    //to regain sync on the packet start byte
                                }
                            }
                            else
                            {
                                mLog.debug("Expected [2] but received [" + bytesRead + "] -- this shouldn't happen");
                            }
                        }
                        else
                        {
                            mLog.debug("Unrecognized byte: " + Arrays.toString(buffer));
                        }
                    }
                }
                catch(IOException ioe)
                {
                    mLog.error("Error while reading ThumbDv serial port", ioe);
                }
            }
        }
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting");

//        String[] frames = {"B9E881526173002A6B", "954BE6500310B00777", "F6948E13324A0F4AB7"};
//        String[] frames = {"954BE6500310B00777", "A478E4520114C02767", "FDC5825757247CC3AF", "EEC4A07775103C87AB", "DDE7805756155CE7FB"};
//        String[] frames = {"ACAA40200044408080"}; //silence frame

        String[] frames = {"0E46122323067C60F8", "0E469433C1067CF1BC", "0E46122B23067C60F8", "0E67162BE08874E2B4",
            "0E46163BE1067CF1BC", "0E46122B23067C60F8", "0A06163BE00A5C303E", "0E46122B23067C60F8", "0E46163BE1847CE1FC",
            "0E46122B23067C60F8"};

        List<byte[]> frameData = new ArrayList<>();

        for(String frame: frames)
        {
            byte[] bytes = new byte[frame.length() / 2];
            for(int x = 0; x < frame.length(); x += 2)
            {
                String hex = frame.substring(x, x + 2);
                bytes[x / 2] = (byte)(0xFF & Integer.parseInt(hex, 16));
            }

            frameData.add(bytes);
        }

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                mLog.debug("Starting thumb dv thread(s)");

                try(ThumbDv thumbDv = new ThumbDv())
                {
                    thumbDv.start();

                    mLog.debug("Resetting ...");
                    thumbDv.send(new ResetRequest());

                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch(InterruptedException ie)
                    {

                    }

//                    thumbDv.send(new ProductIdRequest());
//                    thumbDv.send(new VersionRequest());

//                    SetVocoderParametersRequest r = new SetVocoderParametersRequest(0x0431, 0x0754, 0x2400, 0x0000, 0x0000, 0x6F48);  //DMR
                    SetVocoderParametersRequest r = new SetVocoderParametersRequest(0x0130, 0x0763, 0x4000, 0x0000, 0x0000, 0x0048);  //DSTAR
                    thumbDv.send(r);
//                    thumbDv.send(new SetVocoderRequest(VocoderRate.RATE_33));
//                    thumbDv.send(new SetVocoderRequest(VocoderRate.RATE_39));

//                    thumbDv.send(new SetPacketModeRequest());
//                    thumbDv.send(new GetConfigRequest());


//                    thumbDv.send(new ResetRequest());
//
//                    try
//                    {
//                        Thread.sleep(5000);
//                    }
//                    catch(InterruptedException ie)
//                    {
//
//                    }

//                    thumbDv.send(new SetChannelFormatRequest());
//                    thumbDv.send(new SetSpeechFormatRequest());
//                    thumbDv.send(new InitializeCodecRequest(InitializeOption.ENCODER_AND_DECODER));

                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch(InterruptedException ie)
                    {

                    }

                    for(int x = 0; x < 10; x++)
                    {
                        mLog.debug("Sending PCM Audio Frame");
                        thumbDv.send(new EncodeSpeechRequest(new short[160]));

                        try
                        {
                            Thread.sleep(40);
                        }
                        catch(InterruptedException ie)
                        {

                        }
                    }

                    for(byte[] frame: frameData)
                    {
//                        DecodeSpeechRequest request = new DecodeSpeechRequest(frame, VocoderRate.RATE_33);
                        DecodeSpeechRequest request = new DecodeSpeechRequest(frame);
                        mLog.debug("Sending Decode Request Message: " + Arrays.toString(request.getData()) + " " + AmbeResponse.toHex(request.getData()));
                        thumbDv.send(request);
                        try
                        {
                            Thread.sleep(40);
                        }
                        catch(InterruptedException ie)
                        {

                        }
                    }


                    while(true);
                }
                catch(IOException ioe)
                {
                    mLog.error("Error", ioe);
                }
            }
        };

        ThreadPool.SCHEDULED.schedule(r, 1, TimeUnit.SECONDS);

        while(true);
    }
}
