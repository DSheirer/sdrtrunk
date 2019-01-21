/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.IDataUnitDetectListener;
import io.github.dsheirer.module.decode.p25.P25ChannelStatusProcessor;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.P25MessageFactory;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessageFactory;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.umbtc.UMBTCMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessageFactory;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.protocol.Protocol;
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
public class P25P1MessageFramer implements Listener<Dibit>, IDataUnitDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1MessageFramer.class);

    private P25P1DataUnitDetector mDataUnitDetector;
    private P25ChannelStatusProcessor mChannelStatusProcessor = new P25ChannelStatusProcessor();
    private Listener<Message> mMessageListener;

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

    public P25P1MessageFramer(IPhaseLockedLoop phaseLockedLoop, int bitRate)
    {
        mDataUnitDetector = new P25P1DataUnitDetector(this, phaseLockedLoop);
        mBitRate = bitRate;
    }

    public P25P1MessageFramer(int bitRate)
    {
        this(null, bitRate);
    }

    /**
     * Sets the sample rate for the sync detector
     */
    public void setSampleRate(double sampleRate)
    {
        mDataUnitDetector.setSampleRate(sampleRate);
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
    public void setListener(Listener<Message> messageListener)
    {
        mMessageListener = messageListener;
    }

    public P25P1DataUnitDetector getDataUnitDetector()
    {
        return mDataUnitDetector;
    }

    /**
     * Primary method for streaming decoded symbol dibits for message framing.
     *
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        if(mAssemblingMessage)
        {
            //Strip out the status symbol dibit after every 70 bits or 35 dibits
            if(mStatusSymbolDibitCounter == 35)
            {
                if(mAssemblingMessage)
                {
                    //Send status dibit to channel status processor to identify ISP or OSP channel
                    mChannelStatusProcessor.receive(dibit);
                }
                mStatusSymbolDibitCounter = 0;

                return;
            }

            mStatusSymbolDibitCounter++;

            try
            {
                mBinaryMessage.add(dibit.getBit1());
                mBinaryMessage.add(dibit.getBit2());
            }
            catch(BitSetFullException bsfe)
            {
//                mLog.debug("Message full exception - unexpected");

                //Reset so that we can start over again
                reset(0);
            }

            if(mBinaryMessage.isFull())
            {
                //TDU's have a trailing status symbol that has to be removed -- set flag to true to suppress it.
                if(mDataUnitID.hasTrailingStatusDibit())
                {
                    mTrailingDibitsToSuppress = 1;
                }

                dispatchMessage();
            }
        }
        else
        {
            //Suppress any trailing nulls or status dibits that follow certain DUID sequences
            if(mTrailingDibitsToSuppress > 0)
            {
                mTrailingDibitsToSuppress--;
                updateBitsProcessed(2);
                return;
            }

            mDataUnitDetector.receive(dibit);
        }
    }

    private void dispatchMessage()
    {
        if(mMessageListener != null)
        {
            switch(mDataUnitID)
            {
                case PACKET_HEADER_DATA_UNIT:
                    mPDUSequence = PDUMessageFactory.createPacketSequence(mNAC, mCurrentTime, mBinaryMessage);

                    if(mPDUSequence != null)
                    {
                        if(mPDUSequence.getHeader().isValid() &&
                           mPDUSequence.getHeader().getBlocksToFollowCount() > 0)
                        {
                            //Setup to catch the sequence of data blocks that follow the header
                            mDataUnitID = DataUnitID.PACKET_DATA_UNIT;
                            mBinaryMessage = new CorrectedBinaryMessage(DataUnitID.PACKET_DATA_UNIT.getMessageLength());
                            mAssemblingMessage = true;
                        }
                        else
                        {
                            //Process 44 bits/22 dibits of trailing nulls
                            mTrailingDibitsToSuppress = 22;

                            mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mNAC, getTimestamp()));
                            reset(mPDUSequence.getBitsProcessedCount());
                            mPDUSequence = null;
                        }
                    }
                    break;
                case PACKET_DATA_UNIT:
                    if(mPDUSequence != null)
                    {
                        if(mPDUSequence.getHeader().isConfirmationRequired())
                        {
                            mPDUSequence.addDataBlock(PDUMessageFactory.createConfirmedDataBlock(mBinaryMessage));
                        }
                        else
                        {
                            mPDUSequence.addDataBlock(PDUMessageFactory.createUnconfirmedDataBlock(mBinaryMessage));
                        }

                        if(mPDUSequence.isComplete())
                        {
                            mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mNAC, getTimestamp()));

                            switch(mPDUSequence.getHeader().getBlocksToFollowCount())
                            {
                                case 1:
                                    mTrailingDibitsToSuppress = 29;
                                    break;
                                case 2:
                                    //Process 2 bits or 1 dibit of trailing nulls
                                    mTrailingDibitsToSuppress = 1;
                                    break;
                                case 3:
                                    //Process 16 bits or 8 dibits of trailing nulls
                                    mTrailingDibitsToSuppress = 8;
                                    break;
                                case 4:
                                    //Process 30 bits or 15 dibits of trailing nulls
                                    mTrailingDibitsToSuppress = 15;
                                    break;
                                case 5:
                                    //Process 44 bits or 22 dibits of trailing nulls
                                    mTrailingDibitsToSuppress = 22;
                                    break;
                                default:
//                                    mLog.debug("*** MORE THAN 5 PDU BLOCKS DETECTED [" +
//                                        mPDUSequence.getHeader().getBlocksToFollowCount() +
//                                        "] - DETERMINE TRAILING NULL COUNT TO SUPPRESS AND UPDATE CODE");
                                    break;
                            }

                            reset(mPDUSequence.getBitsProcessedCount());
                        }
                        else
                        {
                            //Setup to catch the next data block
                            mDataUnitID = DataUnitID.PACKET_DATA_UNIT;
                            mBinaryMessage = new CorrectedBinaryMessage(DataUnitID.PACKET_DATA_UNIT.getMessageLength());
                            mAssemblingMessage = true;
                        }
                    }
                    else
                    {
//                        mLog.error("Received PDU data block with out a preceeding data header");
                        reset(mDataUnitID.getMessageLength());
                    }
                    break;
                case TRUNKING_SIGNALING_BLOCK_1:
                case TRUNKING_SIGNALING_BLOCK_2:
                case TRUNKING_SIGNALING_BLOCK_3:
                    TSBKMessage tsbkMessage = TSBKMessageFactory.create(mChannelStatusProcessor.getDirection(),
                        mDataUnitID, mBinaryMessage, mNAC, getTimestamp());

                    mMessageListener.receive(tsbkMessage);

                    if(tsbkMessage.isLastBlock())
                    {
                        reset(mDataUnitID.getMessageLength());
                        mTrailingDibitsToSuppress = 1;
                    }
                    else
                    {
                        updateBitsProcessed(mDataUnitID.getMessageLength());
                        mBinaryMessage = new CorrectedBinaryMessage(mDataUnitID.getMessageLength());
                        if(mDataUnitID == DataUnitID.TRUNKING_SIGNALING_BLOCK_1)
                        {
                            mDataUnitID = DataUnitID.TRUNKING_SIGNALING_BLOCK_2;
                        }
                        else if(mDataUnitID == DataUnitID.TRUNKING_SIGNALING_BLOCK_2)
                        {
                            mDataUnitID = DataUnitID.TRUNKING_SIGNALING_BLOCK_3;
                        }
                    }
                    break;
                default:
                    P25Message message = P25MessageFactory.create(mDataUnitID, mNAC, getTimestamp(), mBinaryMessage);
                    mMessageListener.receive(message);
                    reset(mDataUnitID.getMessageLength());
                    break;
            }
        }
        else
        {
            reset(0);
        }
    }

    private void reset(int bitsProcessed)
    {
        updateBitsProcessed(bitsProcessed);
        mPDUSequence = null;
        mBinaryMessage = null;
        mAssemblingMessage = false;
        mDataUnitID = null;
        mNAC = 0;
        mDataUnitDetector.reset();
        mStatusSymbolDibitCounter = 0;
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

    @Override
    public void dataUnitDetected(DataUnitID dataUnitID, int nac, int bitErrors, int discardedDibits, int[] correctedNid)
    {
        if(discardedDibits > 0)
        {
            dispatchSyncLoss(discardedDibits * 2);
        }

        if(dataUnitID.getMessageLength() < 0)
        {
            dispatchSyncLoss(112); //Sync (48) and Nid (64)
            return;
        }

        if(mSyncDetectListener != null)
        {
            mSyncDetectListener.syncDetected(bitErrors);
        }

        mDataUnitID = dataUnitID;
        mNAC = nac;
        mCorrectedNID = correctedNid;
        mBinaryMessage = new CorrectedBinaryMessage(dataUnitID.getMessageLength());
        mBinaryMessage.incrementCorrectedBitCount(bitErrors);

        mAssemblingMessage = true;
        mStatusSymbolDibitCounter = 21;
    }

    @Override
    public void syncLost(int bitsProcessed)
    {
        dispatchSyncLoss(bitsProcessed);

        if(mSyncDetectListener != null)
        {
            mSyncDetectListener.syncLost();
        }
    }

    private void dispatchSyncLoss(int bitsProcessed)
    {
        //Updates current timestamp according to the number of bits procesed
        updateBitsProcessed(bitsProcessed);

        if(bitsProcessed > 0 && mMessageListener != null)
        {
            mMessageListener.receive(new SyncLossMessage(getTimestamp(), bitsProcessed, Protocol.APCO25));
        }
    }

    public static void main(String[] args)
    {
        boolean pduOnly = false;
        boolean mbtcOnly = false;
        boolean sndcpOnly = false;
        boolean ippacketOnly = false;
        boolean patchOnly = false;

        P25P1MessageFramer messageFramer = new P25P1MessageFramer(null, DecoderType.P25_PHASE1.getProtocol().getBitRate());
        messageFramer.setListener(new Listener<Message>()
        {
            @Override
            public void receive(Message message)
            {
                if(mbtcOnly)
                {
                    if(message instanceof AMBTCMessage || message instanceof UMBTCMessage)
                    {
                        mLog.debug(message.toString());
                    }
                }
                else if(pduOnly)
                {
                    String s = message.toString();

                    if(s.contains(" PDU  "))
                    {
                        mLog.debug(s);
                    }
                }
                else if(sndcpOnly)
                {
                    String s = message.toString();

                    if(s.contains("SNDCP"))
                    {
                        mLog.debug(s);
                    }
                }
                else if(ippacketOnly)
                {
                    String s = message.toString();

                    if(s.contains("IPPKT") || s.contains("SNDCP") || s.contains(" PDU  "))
                    {
                        mLog.debug(s);
                    }
                }
                else if(patchOnly)
                {
                    if(message instanceof PatchGroupVoiceChannelGrant || message instanceof PatchGroupVoiceChannelGrantUpdate)
                    {
                        mLog.debug(message.toString());
                    }
                }
                else
                {
                    String a = message.toString();
                    mLog.debug(a);
                }
            }
        });

//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181102_102339_9600BPS_CNYICC_Onondaga Simulcast_LCN 08.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181103_134948_9600BPS_CNYICC_Oswego Simulcast_LCN 04.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181103_144312_9600BPS_CNYICC_Oswego Simulcast_LCN 04.bits"); //Interesting UDP port 231 packets (oswego LCN 4)
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181103_144429_9600BPS_CNYICC_Onondaga Simulcast_LCN 09.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181103_144437_9600BPS_CNYICC_Onondaga Simulcast_LCN 10.bits");
        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181202_064827_9600BPS_CNYICC_Onondaga Simulcast_LCN 15 Control.bits");



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


        mLog.debug("NIDS Detected: " + messageFramer.getDataUnitDetector().getNIDDetectionCount());
    }
}
