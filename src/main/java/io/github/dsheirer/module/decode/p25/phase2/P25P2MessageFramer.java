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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.audio.P25P2CallSequenceRecorder;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * P25 Sync Detector and Message Framer.  Includes capability to detect PLL out-of-phase lock errors
 * and issue phase corrections.
 */
public class P25P2MessageFramer implements Listener<Dibit>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2MessageFramer.class);

    private P25P2SuperFrameDetector mSuperFrameDetector;
    private boolean mAssemblingMessage = false;
    private CorrectedBinaryMessage mBinaryMessage;
    private DataUnitID mDataUnitID;
    private PDUSequence mPDUSequence;
    private int[] mCorrectedNID;
    private int mNAC;
    private int mStatusSymbolDibitCounter = 0;
    private int mTrailingDibitsToSuppress = 0;
    private double mBitRate;
    private long mCurrentTime = System.currentTimeMillis();
    private ISyncDetectListener mSyncDetectListener;

    public P25P2MessageFramer(IPhaseLockedLoop phaseLockedLoop, int bitRate)
    {
        mSuperFrameDetector = new P25P2SuperFrameDetector(phaseLockedLoop);
        mBitRate = bitRate;
    }

    public P25P2MessageFramer(int bitRate)
    {
        this(null, bitRate);
    }

    /**
     * Sets the sample rate for the sync detector
     */
    public void setSampleRate(double sampleRate)
    {
        mSuperFrameDetector.setSampleRate(sampleRate);
    }

    /**
     * Registers a sync detect listener to be notified each time a sync pattern and NID are detected.
     */
    public void setSyncDetectListener(ISyncDetectListener syncDetectListener)
    {
        mSyncDetectListener = syncDetectListener;
    }

    /**
     * Current timestamp or timestamp of incoming message buffers that is continuously updated to as
     * close as possible to the bits processed for the expected baud rate.
     *
     * @return
     */
    private long getTimestamp()
    {
        return mCurrentTime;
    }

    /**
     * Sets the current time.  This should be invoked by an incoming message buffer stream.
     *
     * @param currentTime
     */
    public void setCurrentTime(long currentTime)
    {
        mCurrentTime = currentTime;
    }

    /**
     * Updates the current timestamp based on the number of bits processed versus the bit rate per second
     * in order to keep an accurate running timestamp to use for timestamped message creation.
     *
     * @param bitsProcessed thus far
     */
    private void updateBitsProcessed(int bitsProcessed)
    {
        if(bitsProcessed > 0)
        {
            mCurrentTime += (long)((double)bitsProcessed / mBitRate * 1000.0);
        }
    }

    /**
     * Registers the listener for messages produced by this message framer
     *
     * @param messageListener to receive framed and decoded messages
     */
    public void setListener(Listener<IMessage> messageListener)
    {
        mSuperFrameDetector.setListener(messageListener);
    }

    public P25P2SuperFrameDetector getSuperFrameDetector()
    {
        return mSuperFrameDetector;
    }

    /**
     * Primary method for streaming decoded symbol dibits for message framing.
     *
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        mSuperFrameDetector.receive(dibit);
    }

    private void reset(int bitsProcessed)
    {
        updateBitsProcessed(bitsProcessed);
        mPDUSequence = null;
        mBinaryMessage = null;
        mAssemblingMessage = false;
        mDataUnitID = null;
        mNAC = 0;
        mSuperFrameDetector.reset();
        mStatusSymbolDibitCounter = 0;
    }

    /**
     * Primary method for streaming decoded symbol byte arrays.
     *
     * @param buffer to process into a stream of dibits for processing.
     */
    public void receive(ReusableByteBuffer buffer)
    {
        //TODO: set timestamp in super frame detector
        setCurrentTime(buffer.getTimestamp());

        for(byte value : buffer.getBytes())
        {
            for(int x = 0; x <= 3; x++)
            {
                receive(Dibit.parse(value, x));
            }
        }

        buffer.decrementUserCount();
    }

    public static void main(String[] args)
    {
        P25P2MessageFramer messageFramer = new P25P2MessageFramer(null, DecoderType.P25_PHASE1.getProtocol().getBitRate());
        P25P2MessageProcessor messageProcessor = new P25P2MessageProcessor();
        P25P2CallSequenceRecorder frameRecorder = new P25P2CallSequenceRecorder(new UserPreferences(), 154250000);
        messageFramer.setListener(messageProcessor);
        messageProcessor.setMessageListener(new Listener<IMessage>()
        {
            @Override
            public void receive(IMessage message)
            {
                frameRecorder.receive(message);
                mLog.debug(message.toString());
            }
        });


        Path path = Paths.get("/media/denny/500G1EXT4/RadioRecordings/APCO25P2/DFW Airport Encrypted/20190224_101332_12000BPS_APCO25PHASE2_DFWAirport_Site_857_3875_baseband_20181213_223136.bits");
//        Path path = Paths.get("/media/denny/500G1EXT4/RadioRecordings/APCO25P2/CNYICC/20190323_042605_12000BPS_APCO25PHASE2_CNYICC_ROME_154_250_1.bits");
//        Path path = Paths.get("/media/denny/500G1EXT4/RadioRecordings/APCO25P2/CNYICC/20190323_042806_12000BPS_APCO25PHASE2_CNYICC_ROME_154_250_3.bits");
//        Path path = Paths.get("/media/denny/500G1EXT4/RadioRecordings/APCO25P2/CNYICC/20190323_042830_12000BPS_APCO25PHASE2_CNYICC_ROME_154_250_4.bits");
//        Path path = Paths.get("/media/denny/500G1EXT4/RadioRecordings/APCO25P2/CNYICC/20190323_042853_12000BPS_APCO25PHASE2_CNYICC_ROME_154_250_5.bits");
//        Path path = Paths.get("/media/denny/500G1EXT4/RadioRecordings/APCO25P2/CNYICC/20190323_042917_12000BPS_APCO25PHASE2_CNYICC_ROME_154_250_6.bits");
//        Path path = Paths.get("/media/denny/500G1EXT4/RadioRecordings/APCO25P2/CNYICC/20190323_042938_12000BPS_APCO25PHASE2_CNYICC_ROME_154_250_7.bits");


        try(BinaryReader reader = new BinaryReader(path, 200))
        {
            while(reader.hasNext())
            {
                messageFramer.receive(reader.next());
            }
        }
        catch(Exception ioe)
        {
            ioe.printStackTrace();
        }

        frameRecorder.stop();
    }
}
