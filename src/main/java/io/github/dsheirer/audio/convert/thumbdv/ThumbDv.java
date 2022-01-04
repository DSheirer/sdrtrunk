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

package io.github.dsheirer.audio.convert.thumbdv;

import com.fazecast.jSerialComm.SerialPort;
import io.github.dsheirer.audio.convert.thumbdv.message.AmbeMessage;
import io.github.dsheirer.audio.convert.thumbdv.message.AmbeMessageFactory;
import io.github.dsheirer.audio.convert.thumbdv.message.VocoderRate;
import io.github.dsheirer.audio.convert.thumbdv.message.request.AmbeRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.DecodeSpeechRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.EncodeSpeechRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.ResetRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.SetVocoderParametersRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.request.SetVocoderRequest;
import io.github.dsheirer.audio.convert.thumbdv.message.response.DecodeSpeechResponse;
import io.github.dsheirer.audio.convert.thumbdv.message.response.ReadyResponse;
import io.github.dsheirer.audio.convert.thumbdv.message.response.SetVocoderParameterResponse;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Northwest Digital Radio (NWDR) ThumbDv dongle.
 *
 * Note: linux users must allow access to the serial port:
 *
 * sudo usermod -a -G uucp username
 * sudo usermod -a -G dialout username
 * sudo usermod -a -G lock username
 * sudo usermod -a -G tty username
 */
public class ThumbDv implements AutoCloseable
{
    private final static Logger mLog = LoggerFactory.getLogger(ThumbDv.class);
    private static final String PORT_DESCRIPTION = "USB-to-Serial Port (ftdi_sio)";
    private static final String PORT_DESCRIPTION_FRAGMENT = "USB Serial Port";
    private static final int BAUD_RATE = 460800;
    private static final byte PACKET_START = (byte) 0x61;

    public enum AudioProtocol
    {
        DMR,
        DSTAR,
        NXDN,
        P25_PHASE2,
    }

    private SerialPort mSerialPort;
    private ScheduledFuture mSerialPortReaderHandle;
    private ScheduledFuture mAudioDecodeProcessorHandle;
    private LinkedTransferQueue<DecodeSpeechRequest> mDecodeSpeechRequests = new LinkedTransferQueue<>();
    private AudioProtocol mAudioProtocol;
    private Listener<float[]> mAudioBufferListener;
    private boolean mStarted;

    public ThumbDv(AudioProtocol audioProtocol, Listener<float[]> listener)
    {
        mAudioProtocol = audioProtocol;
        mAudioBufferListener = listener;
    }

    /**
     * Enqueues the audio code frame for decoding.  Decoded PCM speech packet will be sent to the registered
     * audio packet listener.
     *
     * @param codecFrame
     */
    public void decode(byte[] codecFrame)
    {
        if(!mStarted || mSerialPort == null)
        {
            throw new IllegalStateException("Must invoke start() method and thumbdv serial device must be available");
        }

        mDecodeSpeechRequests.offer(new DecodeSpeechRequest(codecFrame));
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
        if(mSerialPort == null)
        {
            SerialPort[] ports = SerialPort.getCommPorts();

            for(SerialPort port : ports)
            {
                mLog.debug("Serial Port Name:" + port.getDescriptivePortName());
                if(port.getDescriptivePortName().contentEquals(PORT_DESCRIPTION) ||
                        port.getDescriptivePortName().contains(PORT_DESCRIPTION_FRAGMENT))
                {
                    mSerialPort = port;
                    mSerialPort.setBaudRate(BAUD_RATE);
                    mSerialPort.openPort(0, 5000, 10000);

                    if(mSerialPort.isOpen())
                    {
                        mLog.info("Resetting ThumbDv Device");
                        send(new ResetRequest());

                        mLog.info("Creating Serial Port Reader");
                        final Runnable r = new SerialPortReader(mSerialPort.getInputStream());
                        mLog.info("Starting Serial Port Reader");
                        mSerialPortReaderHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(r, 0,
                                5, TimeUnit.MILLISECONDS);

                        mStarted = true;
                        mLog.info("Startup complete - awaiting reset response");
                    }
                    else
                    {
                        mLog.warn("Could not open serial port: " + mSerialPort.getSystemPortName());
                        throw new IOException("Could not open serial port:" + mSerialPort.getSystemPortName());
                    }

                    break;
                }
            }
        }

        if(mSerialPort == null)
        {
            throw new IOException("ThumbDV serial port not found");
        }


        return;
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
     *
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

        if(message instanceof DecodeSpeechResponse && mAudioBufferListener != null)
        {
            float[] samples = ((DecodeSpeechResponse)message).getSamples();
            mAudioBufferListener.receive(samples);
        }
        else if(message instanceof ReadyResponse && mAudioDecodeProcessorHandle == null)
        {
            if(mAudioDecodeProcessorHandle == null)
            {
                try
                {
                    switch(mAudioProtocol)
                    {
                        case DSTAR:
                            send(new SetVocoderRequest(VocoderRate.RATE_33));
                            send(new SetVocoderParametersRequest(0x0130, 0x0763, 0x4000, 0x0000, 0x0000, 0x0048));
                            break;
                        case DMR:
                        case NXDN:
                        case P25_PHASE2:
                            send(new SetVocoderRequest(VocoderRate.RATE_33));
                            send(new SetVocoderParametersRequest(0x0431, 0x0754, 0x2400, 0x0000, 0x0000, 0x6F48));
                            break;
                        default:
                            throw new IllegalStateException("Unrecognized audio protocol:" + mAudioProtocol);
                    }
                }
                catch(IOException ioe)
                {
                    mLog.error("Error setting audio protocol vocoder parameters");
                }

                mLog.info("ThumbDv Reset Complete");
            }
            else
            {
                mLog.info("ThumbDv Reset Detected");
            }
        }
        else if(message instanceof SetVocoderParameterResponse)
        {
            if(((SetVocoderParameterResponse)message).isSuccessful())
            {
                mLog.info("Audio vocoder parameters configured for " + mAudioProtocol);
                //Start the audio frame decode processor
                mAudioDecodeProcessorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(new AudioDecodeProcessor(), 0,
                        10, TimeUnit.MILLISECONDS);
            }
        }
        else if(message != null)
        {
            mLog.debug("RECEIVED:" + message.toString());
        }
    }

    /**
     * Logs the current settings of the serial port.  Note: you must invoke start() before this method so that
     * the serial port can be discovered and opened.
     */
    public void logSerialPort()
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
        else
        {
            mLog.info("No serial port found");
        }
    }

    /**
     * Processes the audio frame decode queue
     */
    public class AudioDecodeProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                DecodeSpeechRequest request = mDecodeSpeechRequests.poll();

                if(request != null)
                {
                    try
                    {
                        send(request);
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error decoding audio frame", ioe);
                    }

//                    try
//                    {
//                        //Force a 20ms sleep after submitting an audio frame to avoid overflowing the thumbdv
//                        Thread.sleep(20);
//                    }
//                    catch(InterruptedException ie)
//                    {
//                        //Do nothing
//                    }
                }
            }
            catch(Throwable t)
            {
                mLog.error("Error", t);
            }
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
            try
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
            catch(Throwable t)
            {
                mLog.error("Error", t);
            }
        }
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting");

        String silence = "BEDDEA821EFD660C08";

        String[] frames = {"0E46122323067C60F8", "0E469433C1067CF1BC", "0E46122B23067C60F8", "0E67162BE08874E2B4",
                "0E46163BE1067CF1BC", "0E46122B23067C60F8", "0A06163BE00A5C303E", "0E46122B23067C60F8", "0E46163BE1847CE1FC",
                "0E46122B23067C60F8"};

        List<byte[]> frameData = new ArrayList<>();

        for(String frame : frames)
        {
            byte[] bytes = new byte[frame.length() / 2];
            for(int x = 0; x < frame.length(); x += 2)
            {
                String hex = frame.substring(x, x + 2);
                bytes[x / 2] = (byte) (0xFF & Integer.parseInt(hex, 16));
            }

            frameData.add(bytes);
        }

        mLog.debug("Starting thumb dv thread(s)");

        final Listener<float[]> listener = packet -> {
            mLog.info("Got an audio packet!");
        };

        try(ThumbDv thumbDv = new ThumbDv(AudioProtocol.P25_PHASE2, listener))
        {
            thumbDv.start();

            Thread.sleep(6000);

            for(int x = 0; x < 20; x++)
            {
                thumbDv.send(new EncodeSpeechRequest(new short[160]));
                Thread.sleep(20);
            }
//            for(byte[] frame : frameData)
//            {
//                thumbDv.decode(frame);
//            }

            while(true);
        }
        catch(IOException ioe)
        {
            mLog.error("Error", ioe);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
