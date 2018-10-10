/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.P25MessageFactory;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessageFactory;
import io.github.dsheirer.module.decode.p25.message.pdu.PacketSequence;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessageFactory;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class P25MessageFramer2 implements Listener<Dibit>, IDataUnitDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25MessageFramer2.class);
    private P25DataUnitDetector mDataUnitDetector;
    private Listener<Message> mMessageListener;

    private boolean mAssemblingMessage = false;
    private CorrectedBinaryMessage mBinaryMessage;
    private DataUnitID mDataUnitID;
    private int[] mCorrectedNID;
    private int mNAC;
    private int mStatusSymbolDibitCounter = 0;
    private int mStatusSymbolsDetected;
    private long mCurrentTime = System.currentTimeMillis();
    private double mBitRate;
    private PacketSequence mPacketSequence;
    private int mTrailingDibitsToSuppress = 0;

    private P25MessageFramer2(IPhaseLockedLoop phaseLockedLoop, int bitRate)
    {
        mDataUnitDetector = new P25DataUnitDetector(this, phaseLockedLoop);
        mBitRate = bitRate;
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

    public P25DataUnitDetector getDataUnitDetector()
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
//                if(mDataUnitID == DataUnitID.PACKET_HEADER_DATA_UNIT)
//                {
//                    mLog.debug("Throwing away status symbol: " + dibit.toString());
//                }
                mStatusSymbolDibitCounter = 0;
                mStatusSymbolsDetected++;

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
                mLog.debug("Message full exception - unexpected");
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
                    mPacketSequence = PDUMessageFactory.createPacketSequence(mNAC, mCurrentTime, mBinaryMessage);

                    if(mPacketSequence != null)
                    {
                        if(mPacketSequence.getHeader().getBlocksToFollowCount() > 0)
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

                            mMessageListener.receive(PDUMessageFactory.create(mPacketSequence));
                            reset(mPacketSequence.getBitsProcessedCount());
                            mPacketSequence = null;
                        }
                    }
                    break;
                case PACKET_DATA_UNIT:
                    if(mPacketSequence != null)
                    {
                        if(mPacketSequence.getHeader().isConfirmationRequired())
                        {
                            mPacketSequence.addDataBlock(PDUMessageFactory.createConfirmedDataBlock(mBinaryMessage));
                        }
                        else
                        {
                            mPacketSequence.addDataBlock(PDUMessageFactory.createUnconfirmedDataBlock(mBinaryMessage));
                        }

                        if(mPacketSequence.isComplete())
                        {
                            mMessageListener.receive(PDUMessageFactory.create(mPacketSequence));

                            switch(mPacketSequence.getHeader().getBlocksToFollowCount())
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
                                default:
                                    mLog.debug("*** MORE THAN 3 PDU BLOCKS DETECTED [" +
                                        mPacketSequence.getHeader().getBlocksToFollowCount() +
                                        "] - DETERMINE TRAILING NULL COUNT TO SUPPRESS AND UPDATE CODE");
                                    break;
                            }

                            reset(mPacketSequence.getBitsProcessedCount());
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
                        mLog.error("Received PDU data block with out a preceeding data header");
                        reset(mDataUnitID.getMessageLength());
                    }
                    break;
                case TRUNKING_SIGNALING_BLOCK:
                    TSBKMessage tsbkMessage = TSBKMessageFactory.create(mBinaryMessage, mDataUnitID);
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
                        loadNid(mBinaryMessage, mCorrectedNID);
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
        mPacketSequence = null;
        mBinaryMessage = null;
        mAssemblingMessage = false;
        mDataUnitID = null;
        mNAC = 0;
        mDataUnitDetector.reset();
        mStatusSymbolDibitCounter = 0;

//        mLog.debug("Status Symbols Removed: " + mStatusSymbolsDetected);
        mStatusSymbolsDetected = 0;
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
            mLog.debug("Unrecognized Data Unit Id: " + dataUnitID.name());
            return;
        }

        mDataUnitID = dataUnitID;
        mNAC = nac;
        mCorrectedNID = correctedNid;
        mBinaryMessage = new CorrectedBinaryMessage(dataUnitID.getMessageLength());
        mBinaryMessage.incrementCorrectedBitCount(bitErrors);
        loadNid(mBinaryMessage, correctedNid);
        mAssemblingMessage = true;
        mStatusSymbolDibitCounter = 21;
    }

    /**
     * Loads the nid bits into the (empty) binary message
     *
     * @param correctedBinaryMessage to be loaded, having a length of 63 or greater
     * @param nid corrected by the BCH code
     */
    private void loadNid(CorrectedBinaryMessage correctedBinaryMessage, int[] nid)
    {
        try
        {
            for(int x = 0; x < 63; x++)
            {
                correctedBinaryMessage.add((nid[62 - x] == 1) ? true : false);
            }

            //Load a dummy parity bit to ensure the total nid message length is 64-bits
            correctedBinaryMessage.add(false);
        }
        catch(BitSetFullException bsfe)
        {
            mLog.debug("Unexpected message full error", bsfe);
        }
    }

    @Override
    public void syncLost(int bitsProcessed)
    {
        dispatchSyncLoss(bitsProcessed);
    }

    private void dispatchSyncLoss(int bitsProcessed)
    {
        //Updates current timestamp according to the number of bits procesed
        updateBitsProcessed(bitsProcessed);

        if(bitsProcessed > 0 && mMessageListener != null)
        {
            mMessageListener.receive(new SyncLossMessage(getTimestamp(), bitsProcessed));
        }
    }

    public static void main(String[] args)
    {
        boolean pduOnly = false;

        P25MessageFramer2 messageFramer = new P25MessageFramer2(null, 9600);
        messageFramer.setListener(new Listener<Message>()
        {
            @Override
            public void receive(Message message)
            {
                if(pduOnly)
                {
                    String s = message.toString();

                    if(s.contains(" PDU  "))
                    {
                        mLog.debug(s);
                    }
                }
                else
                {
                    mLog.debug(message.toString());
                }
            }
        });

//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180922_040600_9600BPS_CNYICC_Onondaga Simulcast_LCN 09.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180922_042033_9600BPS_CNYICC_Onondaga Simulcast_LCN 10.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_045614_9600BPS_CNYICC_Onondaga Simulcast_LCN 09.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_045647_9600BPS_CNYICC_Onondaga Simulcast_LCN 10.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_050325_9600BPS_CNYICC_Onondaga Simulcast_LCN 09.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_050403_9600BPS_CNYICC_Onondaga Simulcast_LCN 10.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_050721_9600BPS_CNYICC_Onondaga Simulcast_LCN 10.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_051057_9600BPS_CNYICC_Onondaga Simulcast_LCN 09.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_051808_9600BPS_CNYICC_Onondaga Simulcast_LCN 09_EMERG_AND_PACKETS.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20180923_052519_9600BPS_CNYICC_Onondaga Simulcast_LCN 09.bits");
        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181008_091511_9600BPS_CNYICC_Onondaga Simulcast_LCN 15 Control.bits");
//        Path path = Paths.get("/home/denny/SDRTrunk/recordings/20181008_091650_9600BPS_CNYICC_Onondaga Simulcast_LCN 15 Control.bits");

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
