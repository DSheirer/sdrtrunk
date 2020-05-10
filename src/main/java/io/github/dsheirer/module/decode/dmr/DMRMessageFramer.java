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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.dmr.message.DMRBurst;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * DMR Sync Detector and Message Framer.  Includes capability to detect PLL out-of-phase lock errors
 * and issue phase corrections.
 */
public class DMRMessageFramer implements Listener<Dibit>, IDMRBurstDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageFramer.class);

    private DMRBurstDetector mBurstDetector;
    private Listener<IMessage> mMessageListener;

    private int mCurrentSlot = 0; //Slot 0

    private int mInVoiceReadingSlotA = 0;
    private int mInVoiceReadingSlotB = 0;

    private CorrectedBinaryMessage mBinaryMessage;
    private double mBitRate;
    private long mCurrentTime = System.currentTimeMillis();
    private ISyncDetectListener mSyncDetectListener;
    public boolean[] slotSyncMatrix = new boolean[2];

    public DMRMessageFramer(IPhaseLockedLoop phaseLockedLoop, int bitRate)
    {
        mBurstDetector = new DMRBurstDetector(this, phaseLockedLoop);
        mBitRate = bitRate;
    }

    public DMRMessageFramer(int bitRate)
    {
        this(null, bitRate);
    }

    /**
     * Sets the sample rate for the sync detector
     */
    public void setSampleRate(double sampleRate)
    {
        mBurstDetector.setSampleRate(sampleRate);
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
        mMessageListener = messageListener;
    }

    public DMRBurstDetector getDataUnitDetector()
    {
        return mBurstDetector;
    }

    /**
     * Primary method for streaming decoded symbol dibits for message framing.
     *
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        if(slotSyncMatrix[0] || slotSyncMatrix[1]) // at least one slot has synced
        {
            try
            {
                mBinaryMessage.add(dibit.getBit1());
                mBinaryMessage.add(dibit.getBit2());
            }
            catch(BitSetFullException bsfull)
            {
                reset(0);
            }
            catch(NullPointerException ex)
            {
                mBinaryMessage = new CorrectedBinaryMessage(288);
            }
            if(mBinaryMessage.isFull())
            {
                dispatchMessage();
            }
        }
        // if not, we need to detect a burst
        else
        {
            mBurstDetector.receive(dibit);
        }
    }

    private void dispatchMessage()
    {
        DMRSyncPattern currentPattern = DMRBurst.getSyncType(mBinaryMessage);
        if(mMessageListener != null)
        {
            if(mCurrentSlot == 0 && mInVoiceReadingSlotA > 0 || mCurrentSlot == 1 && mInVoiceReadingSlotB > 0)
            {
                int slotCount = 0;
                if(mCurrentSlot == 0)
                {
                    mInVoiceReadingSlotA++;
                    slotCount = mInVoiceReadingSlotA;
                    if(slotCount == 6)
                    {
                        mInVoiceReadingSlotA = 0; //end of voice superframe
                    }
                }
                else
                {
                    mInVoiceReadingSlotB++;

                    slotCount = mInVoiceReadingSlotB;
                    if(slotCount == 6)
                    {
                        mInVoiceReadingSlotB = 0; //end of voice superframe
                    }
                }

                DMRBurst embMsg = DMRMessageFactory.create(DMRSyncPattern.fromValue(-slotCount), mBinaryMessage, mCurrentTime, mCurrentSlot);

                if(!embMsg.isValid())
                {
                    // stop expecting a voice frame in this timeslot
                    if(mCurrentSlot == 0)
                    {
                        mInVoiceReadingSlotA = 0;
                    }
                    else
                    {
                        mInVoiceReadingSlotB = 0;
                    }
                    //lost sync
                    dispatchSyncLoss(0);
                }
                mMessageListener.receive(embMsg);
            }
            else if(currentPattern.isData())
            {
                // data frame
                processDataBurst(mBinaryMessage, currentPattern, 0);
            }
            else if(currentPattern.isVoice())
            {
                // data frame
                processVoiceBurst(mBinaryMessage, currentPattern, 0);
            }
            else
            {
                slotSyncMatrix[mCurrentSlot] = false; // lost sync
                if(currentPattern != DMRSyncPattern.UNKNOWN)
                {
                    System.out.print("SYNC: " + currentPattern.toString());
                }
            }
            mCurrentSlot = (mCurrentSlot == 0 ? 1 : 0);
            mBinaryMessage = new CorrectedBinaryMessage(288); // ready for next message
        }
        else
        {
            reset(0);
        }
    }

    private void reset(int bitsProcessed)
    {
        updateBitsProcessed(bitsProcessed);
        mBinaryMessage = null;
        mCurrentSlot = 0;
        slotSyncMatrix[0] = false;
        slotSyncMatrix[1] = false;
        mBurstDetector.reset();
    }

    /**
     * Primary method for streaming decoded symbol byte arrays.
     *
     * @param buffer to process into a stream of dibits for processing.
     */
    public void receive(ReusableByteBuffer buffer)
    {
        //Updates current timestamp to the timestamp from the incoming buffer
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

    private void processVoiceBurst(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern, int bitErrors)
    {
        if(mMessageListener != null)
        {
            DMRBurst message = DMRMessageFactory.create(pattern, binaryMessage, mCurrentTime, mCurrentSlot);
            mMessageListener.receive(message);
        }

        if(mCurrentSlot == 0)
        { // A is now running
            mInVoiceReadingSlotA = 1;
        }
        else
        {
            mInVoiceReadingSlotB = 1;
        }
    }

    private void processDataBurst(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern, int bitErrors)
    {
        DMRBurst message = DMRMessageFactory.create(pattern, binaryMessage, mCurrentTime, mCurrentSlot);

        if(message != null)
        {
            if(!message.isValid())
            {
                dispatchSyncLoss(288); // CACH(24) + PAYLOAD(108 * 2) + SYNC(48)
                return;
            }
            mMessageListener.receive(message);
            if(mSyncDetectListener != null)
            {
                //mSyncDetectListener.syncDetected(bitErrors);
            }
        }
    }

    @Override
    public void burstDetectedWithSync(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern, int bitErrors)
    {
        slotSyncMatrix[mCurrentSlot] = true; // current slot is synced
        if(pattern == DMRSyncPattern.MOBILE_STATION_VOICE || pattern == DMRSyncPattern.BASE_STATION_VOICE)
        {
            processVoiceBurst(binaryMessage, pattern, bitErrors);
        }
        else if(pattern == DMRSyncPattern.MOBILE_STATION_DATA || pattern == DMRSyncPattern.BASE_STATION_DATA)
        {
            processDataBurst(binaryMessage, pattern, bitErrors);
        }
        mBinaryMessage = new CorrectedBinaryMessage(288);
        mCurrentSlot = (mCurrentSlot == 0 ? 1 : 0);
    }

    @Override
    public void syncLost(int bitsProcessed)
    {
        dispatchSyncLoss(bitsProcessed);
        if(mSyncDetectListener != null)
        {
            mSyncDetectListener.syncLost(bitsProcessed);
        }
    }

    private void dispatchSyncLoss(int bitsProcessed)
    {
        //Updates current timestamp according to the number of bits processed
        updateBitsProcessed(bitsProcessed);
        mCurrentSlot = 0;
        mInVoiceReadingSlotB = 0;
        mInVoiceReadingSlotA = 0;

        if(bitsProcessed > 0 && mMessageListener != null)
        {
            if(bitsProcessed < 64)
            {
                mMessageListener.receive(new StuffBitsMessage(getTimestamp(), bitsProcessed, Protocol.DMR));
            }
            else
            {
                mMessageListener.receive(new SyncLossMessage(getTimestamp(), bitsProcessed, Protocol.DMR));
            }
        }
    }

    public static class MessageListener implements Listener<IMessage>
    {
        private boolean mHasDMRData = false;

        @Override
        public void receive(IMessage message)
        {
            if(message instanceof DMRMessage)
            {
                mLog.info("TS:" + ((DMRMessage)message).getTimeslot() + " " + message.toString());
                mHasDMRData = true;
            }
        }

        public boolean hasData()
        {
            return mHasDMRData;
        }

        public void reset()
        {
            mHasDMRData = false;
        }
    }

    public static void main(String[] args)
    {
//        String file = "/home/denny/SDRTrunk/recordings/20200513_143340_9600BPS_DMR_SaiaNet_Onondaga_Control.bits";
        String path = "/home/denny/SDRTrunk/recordings/";
        String file = path + "20200514_133947_9600BPS_DMR_SaiaNet_Onondaga_LCN_4.bits";

        boolean multi = false;

        if(multi)
        {
            try
            {
                DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path), "*.bits");

                MessageListener listener = new MessageListener();
                stream.forEach(new Consumer<Path>()
                {
                    @Override
                    public void accept(Path path)
                    {
                        boolean hasData = false;
                        DMRMessageFramer messageFramer = new DMRMessageFramer(null, DecoderType.DMR.getProtocol().getBitRate());
                        DMRMessageProcessor messageProcessor = new DMRMessageProcessor();
                        messageFramer.setListener(messageProcessor);
                        messageProcessor.setMessageListener(listener);

                        try(BinaryReader reader = new BinaryReader(path, 200))
                        {
                            while(reader.hasNext())
                            {
                                ReusableByteBuffer buffer = reader.next();
                                messageFramer.receive(buffer);
                            }
                        }
                        catch(Exception ioe)
                        {
                            ioe.printStackTrace();
                        }

                        if(!listener.hasData())
                        {
                            mLog.info("Has Data: " + listener.hasData() + " File:" + path.toString());
//                            try
//                            {
//                                Files.delete(path);
//                            }
//                            catch(IOException ioe)
//                            {
//                                ioe.printStackTrace();
//                            }
                        }

                        listener.reset();

                    }
                });
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
        else
        {
            DMRMessageFramer messageFramer = new DMRMessageFramer(null, DecoderType.DMR.getProtocol().getBitRate());
            DMRMessageProcessor messageProcessor = new DMRMessageProcessor();
            messageFramer.setListener(messageProcessor);
            messageProcessor.setMessageListener(message ->
            {
//                if(message instanceof ShortLCMessage)
//                {
                    mLog.debug("TS" + message.getTimeslot() + " " + message.toString());
//                }
            });

            try(BinaryReader reader = new BinaryReader(Path.of(file), 200))
            {
                while(reader.hasNext())
                {
                    ReusableByteBuffer buffer = reader.next();
                    messageFramer.receive(buffer);
                }
            }
            catch(Exception ioe)
            {
                ioe.printStackTrace();
            }
        }
    }
}
