/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.soft;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase1.P25P1ChannelStatusProcessor;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.P25MessageFactory;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUMessageFactory;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessageFactory;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;

/**
 * Provides message framing for the demodulated dibit stream.  This framer is notified by an external sync detection
 * process using the two syncDetected() methods below to indicate if the NID that follows the sync was correctly error
 * detected and corrected.  When the NID does not pass error correction, we use a PLACEHOLDER data unit ID to allow the
 * uncertain message to assemble and then we'll inspect before and after data unit IDs and the quantity of captured
 * dibits to make a best guess on what the assembled message represents.
 */
public class P25P1SoftMessageFramer implements Listener<Dibit>
{
    private static final double MILLISECONDS_PER_SYMBOL = 1.0 / 4800.0 / 1000.0;
    private Listener<IMessage> mMessageListener;
    private boolean mRunning = false;
    private int mDibitCounter = 0;
    private int mDibitSinceTimestampCounter = 0;
    private int mStatusSymbolDibitCounter = 0;
    private int mTrailingDibitsToSuppress = 0;
    private long mReferenceTimestamp = 0;
    private P25P1MessageAssembler mMessageAssembler;
    private P25P1DataUnitID mPreviousDataUnitID = P25P1DataUnitID.PLACEHOLDER;
    private P25P1ChannelStatusProcessor mChannelStatusProcessor = new P25P1ChannelStatusProcessor();
    private PDUSequence mPDUSequence;

    /**
     * Primary method that receives the demodulated dibit stream from the symbol processor.
     * @param dibit that was demodulated.
     */
    @Override
    public void receive(Dibit dibit)
    {
        mDibitSinceTimestampCounter++;

        if(mTrailingDibitsToSuppress > 0)
        {
            mTrailingDibitsToSuppress--;
            return;
        }

        mDibitCounter ++;

        if(mMessageAssembler != null)
        {
            //Strip out the status symbol dibit after every 70 bits or 35 dibits
            if(mStatusSymbolDibitCounter == 35)
            {
                //Send status dibit to channel status processor to identify ISP or OSP channel
                mChannelStatusProcessor.receive(dibit);
                mStatusSymbolDibitCounter = 0;
                mDibitCounter--;
                return;
            }

            mStatusSymbolDibitCounter++;
            mMessageAssembler.receive(dibit);

            if(mMessageAssembler.isComplete())
            {
                dispatchMessage();
            }
        }
        else if(mDibitCounter >= 4800)
        {
            mDibitCounter -= 4800;
            broadcast(new SyncLossMessage(getTimestamp(), 9600, Protocol.APCO25));
        }
    }

    /**
     * Dispatch the message currently in the message assembler.
     */
    private void dispatchMessage()
    {
        //Note: the message assembler should have a valid DUID on it via the forceCompletion() method.  Capture the
        //current DUID as the previous, before the assembler is nullified.
        mPreviousDataUnitID = mMessageAssembler.getDataUnitID();
        mDibitCounter -= mMessageAssembler.getMessage().currentSize() / 2;

        if(mMessageListener != null)
        {
            switch(mMessageAssembler.getDataUnitID())
            {
                case TRUNKING_SIGNALING_BLOCK_1:
                case TRUNKING_SIGNALING_BLOCK_2:
                case TRUNKING_SIGNALING_BLOCK_3:
                    dispatchTSBK();
                    break;
                case PACKET_DATA_UNIT:
                case PACKET_DATA_UNIT_BLOCK_1:
                case PACKET_DATA_UNIT_BLOCK_2:
                case PACKET_DATA_UNIT_BLOCK_3:
                case PACKET_DATA_UNIT_BLOCK_4:
                case PACKET_DATA_UNIT_BLOCK_5:
                    dispatchPDU();
                    break;
                case TERMINATOR_DATA_UNIT:
                    dispatchTDU();
                    //Increment dibit counter by one since we sniped a sync dibit to eat the trailing status dibit
                    mDibitCounter++;
                    break;
                case TERMINATOR_DATA_UNIT_LINK_CONTROL:
                    dispatchTDULC();
                    break;
                default:
                    dispatchOther();
                    break;
            }
        }
        else
        {
            mMessageAssembler = null;
        }
    }

    private void dispatchTDU()
    {
        CorrectedBinaryMessage cbm = mMessageAssembler.getMessage();
        P25P1Message message = P25MessageFactory.create(mMessageAssembler.getDataUnitID(), mMessageAssembler.getNAC(),
                getTimestamp(), cbm);

        if(message != null)
        {
            mMessageListener.receive(message);
        }
        else
        {
            SyncLossMessage slm = new SyncLossMessage(getTimestamp(), cbm.currentSize(), Protocol.APCO25);
            mMessageListener.receive(slm);
        }

        mMessageAssembler = null;
        mTrailingDibitsToSuppress = 1;
    }

    private void dispatchTDULC()
    {
        CorrectedBinaryMessage cbm = mMessageAssembler.getMessage();
        P25P1Message message = P25MessageFactory.create(mMessageAssembler.getDataUnitID(), mMessageAssembler.getNAC(),
                getTimestamp(), cbm);

        if(message != null)
        {
            mMessageListener.receive(message);
        }
        else
        {
            SyncLossMessage slm = new SyncLossMessage(getTimestamp(), cbm.currentSize(), Protocol.APCO25);
            mMessageListener.receive(slm);
        }

        mMessageAssembler = null;

        mTrailingDibitsToSuppress = 11;
    }

    /**
     * Dispatches the message currently in the message assembler when the DUID is not PDU or TSBK.
     */
    private void dispatchOther()
    {
        CorrectedBinaryMessage cbm = mMessageAssembler.getMessage();
        P25P1Message message = P25MessageFactory.create(mMessageAssembler.getDataUnitID(), mMessageAssembler.getNAC(),
                getTimestamp(), cbm);

        if(message != null)
        {
            mMessageListener.receive(message);
        }
        else
        {
            SyncLossMessage slm = new SyncLossMessage(getTimestamp(), cbm.currentSize(), Protocol.APCO25);
            mMessageListener.receive(slm);
        }

        mMessageAssembler = null;
    }

    /**
     * Dispatches the message currently in the message assembler when the DUID is TSBK1, TSBK2, or TSBK3.
     */
    private void dispatchTSBK()
    {
        int syncLossBits = 0;

        CorrectedBinaryMessage message = mMessageAssembler.getMessage();
        int size = message.currentSize();

        switch(mMessageAssembler.getDataUnitID())
        {
            case TRUNKING_SIGNALING_BLOCK_1:

                TSBKMessage tsbk1 = TSBKMessageFactory.create(mChannelStatusProcessor.getDirection(), mMessageAssembler.getDataUnitID(), message, mMessageAssembler.getNAC(), getTimestamp());

                if(tsbk1 != null)
                {
                    mMessageListener.receive(tsbk1);

                    if(tsbk1.isLastBlock())
                    {
                        mMessageAssembler = null;

                        //Setup to ignore trailing null dibits if we only captured the 196 bits of the message
                        if(size == 196)
                        {
                            mTrailingDibitsToSuppress = 21;
                        }

                        return;
                    }
                    else if(size == 196) //We captured TSBK1, setup to capture TSBK2
                    {
                        mMessageAssembler.reconfigure(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_2, 196);
                        return;
                    }
                }
                else
                {
                    syncLossBits += 196;
                }

                //Check for TSBK2 in the message
                if(size >= 392)
                {
                    CorrectedBinaryMessage message2 = message.getSubMessage(196, 392);
                    TSBKMessage tsbk2 = TSBKMessageFactory.create(mChannelStatusProcessor.getDirection(),
                        P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_2, message2, mMessageAssembler.getNAC(), getTimestamp());

                    if(tsbk2 != null)
                    {
                        mMessageListener.receive(tsbk2);

                        if(tsbk2.isLastBlock())
                        {
                            mMessageAssembler = null;

                            //Setup to ignore trailing null dibits if we only captured the 392 bits of TSBK1 & TSBK2
                            if(size == 392)
                            {
                                mTrailingDibitsToSuppress = 28;
                            }

                            dispatchSyncLoss(syncLossBits);
                            return;
                        }
                        else if(size == 392) //We captured TSBK1 & TSBK2, setup to capture TSBK3
                        {
                            mMessageAssembler.reconfigure(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_3, 196);
                            dispatchSyncLoss(syncLossBits);
                            return;
                        }
                    }
                    else
                    {
                        syncLossBits += 196;
                    }
                }

                //Check for TSBK3 in the message
                if(size >= 588)
                {
                    CorrectedBinaryMessage message3 = message.getSubMessage(392, 588);
                    TSBKMessage tsbk3 = TSBKMessageFactory.create(mChannelStatusProcessor.getDirection(),
                        P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_3, message3, mMessageAssembler.getNAC(), getTimestamp());

                    if(tsbk3 != null)
                    {
                        mMessageListener.receive(tsbk3);
                        mMessageAssembler = null;
                    }
                    else
                    {
                        syncLossBits += 196;
                    }
                }

                dispatchSyncLoss(syncLossBits);
                break;
        }
    }

    /**
     * Dispatches a sync loss message to account for lost bits.
     * @param bitCount that was lost.
     */
    private void dispatchSyncLoss(int bitCount)
    {
        if(mMessageListener != null && bitCount > 0)
        {
            mMessageListener.receive(new SyncLossMessage(getTimestamp(), bitCount, Protocol.APCO25));
        }
    }

    /**
     * Dispatches the message currently in the message assembler when the DUID is PDU or PDU1
     */
    private void dispatchPDU()
    {
        System.out.println("Dispatching PDU ... Message Length: " + mMessageAssembler.getMessage().currentSize() +
                " DUID:" + mMessageAssembler.getDataUnitID());

        switch(mMessageAssembler.getDataUnitID())
        {
            case PACKET_DATA_UNIT:
                PDUHeader header = PDUMessageFactory.createHeader(mMessageAssembler.getMessage());

                if(header != null)
                {
                    mPDUSequence = new PDUSequence(header, getTimestamp(), mMessageAssembler.getNAC());

                    if(mPDUSequence.getHeader().isValid() && mPDUSequence.getHeader().getBlocksToFollowCount() > 0)
                    {
                        //Setup to catch the sequence of data blocks that follow the header
                        mMessageAssembler.reconfigure(P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_1, 196);
                    }
                    else
                    {
                        //Process 44 bits/22 dibits of trailing nulls
                        mTrailingDibitsToSuppress = 23;
                        mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mMessageAssembler.getNAC(),
                                getTimestamp()));
                        mPDUSequence = null;
                        mMessageAssembler = null;
                    }
                }
                else
                {
                    System.out.println(" << PDU HEADER FAIL >>");
                }
                break;
            case PACKET_DATA_UNIT_BLOCK_1:
                if(mPDUSequence != null)
                {
                    if(mPDUSequence.getHeader().isConfirmationRequired())
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createConfirmedDataBlock(mMessageAssembler.getMessage()));
                    }
                    else
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createUnconfirmedDataBlock(mMessageAssembler.getMessage()));
                    }

                    if(mPDUSequence.isComplete())
                    {
                        mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mMessageAssembler.getNAC(),
                                getTimestamp()));
                        mMessageAssembler = null;
                        mTrailingDibitsToSuppress = 30;
                    }
                    else
                    {
                        //Setup to catch the next data block
                        mMessageAssembler.reconfigure(P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_2, 196);
                    }
                }
                break;
            case PACKET_DATA_UNIT_BLOCK_2:
                if(mPDUSequence != null)
                {
                    if(mPDUSequence.getHeader().isConfirmationRequired())
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createConfirmedDataBlock(mMessageAssembler.getMessage()));
                    }
                    else
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createUnconfirmedDataBlock(mMessageAssembler.getMessage()));
                    }

                    if(mPDUSequence.isComplete())
                    {
                        mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mMessageAssembler.getNAC(),
                                getTimestamp()));
                        mMessageAssembler = null;

//                        mTrailingDibitsToSuppress = 29;
                    }
                    else
                    {
                        //Setup to catch the next data block
                        mMessageAssembler.reconfigure(P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_3, 196);
                    }
                }
                break;
            case PACKET_DATA_UNIT_BLOCK_3:
                if(mPDUSequence != null)
                {
                    if(mPDUSequence.getHeader().isConfirmationRequired())
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createConfirmedDataBlock(mMessageAssembler.getMessage()));
                    }
                    else
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createUnconfirmedDataBlock(mMessageAssembler.getMessage()));
                    }

                    if(mPDUSequence.isComplete())
                    {
                        mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mMessageAssembler.getNAC(),
                                getTimestamp()));
                        mMessageAssembler = null;
                        mTrailingDibitsToSuppress = 9;
                    }
                    else
                    {
                        //Setup to catch the next data block
                        mMessageAssembler.reconfigure(P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_4, 196);
                    }
                }
                break;
            case PACKET_DATA_UNIT_BLOCK_4:
                if(mPDUSequence != null)
                {
                    if(mPDUSequence.getHeader().isConfirmationRequired())
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createConfirmedDataBlock(mMessageAssembler.getMessage()));
                    }
                    else
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createUnconfirmedDataBlock(mMessageAssembler.getMessage()));
                    }

                    if(mPDUSequence.isComplete())
                    {
                        mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mMessageAssembler.getNAC(),
                                getTimestamp()));
                        mMessageAssembler = null;

                        //                        mTrailingDibitsToSuppress = 29;
                    }
                    else
                    {
                        //Setup to catch the next data block
                        mMessageAssembler.reconfigure(P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_5, 196);
                    }
                }
                break;
            case PACKET_DATA_UNIT_BLOCK_5:
                if(mPDUSequence != null)
                {
                    if(mPDUSequence.getHeader().isConfirmationRequired())
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createConfirmedDataBlock(mMessageAssembler.getMessage()));
                    }
                    else
                    {
                        mPDUSequence.addDataBlock(PDUMessageFactory
                                .createUnconfirmedDataBlock(mMessageAssembler.getMessage()));
                    }

                    if(mPDUSequence.isComplete())
                    {
                        mMessageListener.receive(PDUMessageFactory.create(mPDUSequence, mMessageAssembler.getNAC(),
                                getTimestamp()));

                        //                        mTrailingDibitsToSuppress = 29;
                    }

                    mMessageAssembler = null;
                }
                break;
        }
    }

    private void reset()
    {
        mPDUSequence = null;
        mStatusSymbolDibitCounter = 0;
    }

    /**
     * Broadcasts the assembled message to the registered listener.
     * @param message to broadcast - ignored if there is no registered listener.
     */
    private void broadcast(IMessage message)
    {
        if(mMessageListener != null && message != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Externally provided trigger that a sync pattern is detected and the next arriving dibit is the start of the
     * message that contains that sync.  This method is triggered when the NID following the sync was correctly verified
     * via the BCH error correction and the NAC/DUID values are assumed to be correct.
     *
     * @param nac value decoded from the NID.
     * @param dataUnitID decoded from the NID
     */
    public void syncDetected(int nac, P25P1DataUnitID dataUnitID)
    {
        //Set the status symbol counter to 21 to account for NID dibits processed thus far.
        mStatusSymbolDibitCounter = 21;
        mDibitCounter -= 56; //Sync(48) + NID(64) = 56 dibits

        //If there is a message assembler (still) active, force it to complete
        if(mMessageAssembler != null)
        {
            mMessageAssembler.forceCompletion(mPreviousDataUnitID, dataUnitID);
            dispatchMessage();
        }

        if(mDibitCounter < 0)
        {
            mDibitCounter = 0;
        }

        if(mDibitCounter > 0)
        {
            dispatchSyncLoss(mDibitCounter * 2);
            mDibitCounter = 0;
        }

        mMessageAssembler = new P25P1MessageAssembler(nac, dataUnitID);

        StringBuilder debugSB = new StringBuilder();
        debugSB.append("  --> Message Framer Sync Detect - NAC: ").append(nac);
        debugSB.append(" Data Unit: ").append(dataUnitID);
        debugSB.append(" DIBIT Counter: ").append(mDibitCounter);
//        System.out.println(debugSB);
    }

    /**
     * Indicates a valid sync was detected, but the NID didn't pass BCH error correction and therefore we're not certain
     * of what DUID value to use.  In this case, we'll use the PLACEHOLDER DUID for the current DUID which has a message
     * length that is long enough to capture any DUID.  Then, once the next sync is detected, we can inspect both the
     * previous DUID, the next DUID, and the quantity of dibits captured for the unknown DUID that falls in between to
     * make a best guess and process the message accordingly.  If the subsequent sync detect also has an invalid NID
     * error correction, then we might abort guessing on the message that is closing out, unless the dibit count
     * definitively correlates to a specific DUID message length.
     * @param nac from previously decoded NID sync detections.
     */
    public void syncDetectedInvalidNID(int nac)
    {
        //Set the status symbol counter to 21 to account for NID dibits processed thus far.
        mStatusSymbolDibitCounter = 21;
        mDibitCounter -= 56; //Sync(48) + NID(64) = 56 dibits

        if(mDibitCounter < 0)
        {
            mDibitCounter = 0;
        }

        if(mDibitCounter > 0)
        {
            dispatchSyncLoss(mDibitCounter * 2);
            mDibitCounter = 0;
        }

        //If there is a message assembler (still) active, force it to complete
        if(mMessageAssembler != null)
        {
            mMessageAssembler.forceCompletion(mPreviousDataUnitID, P25P1DataUnitID.PLACEHOLDER);
            dispatchMessage();
        }

        System.out.println("Message Assembly Started with PLACEHOLDER");
        mMessageAssembler = new P25P1MessageAssembler(nac, P25P1DataUnitID.PLACEHOLDER);




        //TODO: the below commented code should be married up to the forceCompletion on an assembler that is using the
        // PLACEHOLDER duid value.
//        switch(mPreviousDUID)
//        {
//            case HEADER_DATA_UNIT:
//                if(!duid.isValidPrimaryDUID() && mPreviousMessageSymbolLength == 396) //Length of an HDU in symbols
//                {
//                    //This might be a stretch, but let's err on the side of voice.
//                    mPreviousDUID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
//                    System.out.println("  (@-@) Corrected DUID from [" + duid.name() + "] to [" + mPreviousDUID.name() + "]  *****************");
//                }
//                else
//                {
//                    mSyncLock = duid.isValidPrimaryDUID();
//                    mPreviousDUID = duid;
//                }
//                break;
//            case LOGICAL_LINK_DATA_UNIT_1:
//                if(mPreviousMessageSymbolLength >= 845)
//                {
//                    //There should always be an LDU2 following an LDU1
//                    mPreviousDUID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2;
//                    System.out.println("  (@-@) Corrected DUID from [" + duid.name() + "] to [" + mPreviousDUID.name() + "]  *****************");
//                }
//                else
//                {
//                    mSyncLock = duid.isValidPrimaryDUID();
//                    mPreviousDUID = duid;
//                }
//                break;
//            case LOGICAL_LINK_DATA_UNIT_2:
//                if(!duid.isValidPrimaryDUID() && mPreviousMessageSymbolLength >= 845)
//                {
//                    //Should be either an LDU1 or TDU ... set it to LDU1 and if not, message framer will revert it to TDU.
//                    mPreviousDUID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
//                    mSyncLock = true;
//                    System.out.println("  (@-@) Corrected DUID from [" + duid.name() + "] to [" + mPreviousDUID.name() + "]  *****************");
//                }
//                else
//                {
//                    mSyncLock = duid.isValidPrimaryDUID();
//                    mPreviousDUID = duid;
//                }
//                break;
//            case TERMINATOR_DATA_UNIT:
//                if(!duid.isValidPrimaryDUID() && mPreviousMessageSymbolLength == 72)
//                {
//                    //If the previous message was a TDU and 72 symbols long, there's a good chance this is also a TDU
//                    mPreviousDUID = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
//                    mSyncLock = true;
//                    System.out.println("  (@-@) Corrected DUID from [" + duid.name() + "] to [" + mPreviousDUID.name() + "]  *****************");
//                }
//                else
//                {
//                    mSyncLock = duid.isValidPrimaryDUID();
//                    mPreviousDUID = duid;
//                }
//                break;
//            case TRUNKING_SIGNALING_BLOCK_1:
//                System.out.println("  TSBK2/3 Detected - Continuing");
//                //Do nothing -
//                break;
//            default:
//                if(!duid.isValidPrimaryDUID() && mPreviousMessageSymbolLength == 72)
//                {
//                    //If the previous message was 72 symbols long (ie a TDU), there's a good chance this is also a TDU
//                    mPreviousDUID = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
//                    System.out.println("  (@-@) Corrected DUID from [" + duid.name() + "] to [" + mPreviousDUID.name() + "]  *****************");
//                }
//                else
//                {
//                    //                        System.out.println("  No Correction - DUID [" + duid.name() + "] Previous [" + mPreviousDUID.name() + "]  *****************");
//                    mPreviousDUID = duid;
//                }
//                mSyncLock = false;
//                break;
//        }


    }

    /**
     * Starts this framer dispatching messages
     */
    public void start()
    {
        mRunning = true;
    }

    /**
     * Stops this framer from dispatching messages
     */
    public void stop()
    {
        mRunning = false;
    }

    /**
     * Sets the listener to receive framed DMR messages.
     * @param listener for messages.
     */
    public void setListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Sets or updates the current dibit stream time from an incoming sample buffer.
     * @param time to use as a reference timestamp.
     */
    public void setTimestamp(long time)
    {
        mReferenceTimestamp = time;
        mDibitSinceTimestampCounter = 0;
    }

    /**
     * Calculates the timestamp accurate to the currently received dibit.
     * @return timestamp in milliseconds.
     */
    private long getTimestamp()
    {
        if(mReferenceTimestamp > 0)
        {
            return mReferenceTimestamp + (long)(1000.0 * mDibitSinceTimestampCounter / 4800);
        }
        else
        {
            mDibitSinceTimestampCounter = 0;
            return System.currentTimeMillis();
        }
    }
}
