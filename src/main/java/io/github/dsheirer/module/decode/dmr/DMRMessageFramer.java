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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.edac.BPTC_17_12_3;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.LCSS;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.lc.ShortLCMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceAMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceEMBMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DMR Sync Detector and Message Framer.  Includes capability to detect PLL out-of-phase lock errors
 * and issue phase corrections.
 */
public class DMRMessageFramer implements Listener<Dibit>, IDMRBurstDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageFramer.class);

    private DMRBurstDetector mBurstDetector;
    private Listener<Message> mMessageListener;

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
    public void setListener(Listener<Message> messageListener)
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
            catch(NullPointerException ex) {
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
        DMRSyncPattern currentPattern = DMRMessage.getSyncType(mBinaryMessage);
        if(mMessageListener != null)
        {
            if(mCurrentSlot == 0 && mInVoiceReadingSlotA > 0 || mCurrentSlot == 1 && mInVoiceReadingSlotB > 0) {
                System.out.print("[TS-x" + (mCurrentSlot)+"] __ VOICE >>> ");
                int slotCount = 0;
                if(mCurrentSlot == 0) {
                    mInVoiceReadingSlotA ++;
                    slotCount = mInVoiceReadingSlotA;
                    if(slotCount == 6) {
                        mInVoiceReadingSlotA = 0; //end of voice superframe
                    }
                } else {
                    mInVoiceReadingSlotB ++;

                    slotCount = mInVoiceReadingSlotB;
                    if(slotCount == 6) {
                        mInVoiceReadingSlotB = 0; //end of voice superframe
                    }
                }
                System.out.print("VOICE FRAME [" + (char)('A' + slotCount - 1) + "] ");
                VoiceMessage embMsg = DMRMessageFactory.createVoiceMessage(DMRSyncPattern.fromValue( - slotCount),
                        mBinaryMessage, mCurrentTime, mCurrentSlot);
                if(!embMsg.isValid()) {
                    // stop expecting a voice frame in this timeslot
                    if(mCurrentSlot == 0) {
                        mInVoiceReadingSlotA = 0;
                    } else {
                        mInVoiceReadingSlotB = 0;
                    }
                    //lost sync
                    dispatchSyncLoss(0);
                }
                mMessageListener.receive(embMsg);
            } else if(currentPattern.isData()) {
                // data frame
                processDataBurst(mBinaryMessage, currentPattern, 0);
            } else if(currentPattern.isVoice()) {
                // data frame
                processVoiceBurst(mBinaryMessage, currentPattern, 0);
            } else {
                slotSyncMatrix[mCurrentSlot] = false; // lost sync
                System.out.print("[TS-x" + (mCurrentSlot == 0 ? "A": "B") + "] No Activity ");
                if(currentPattern != DMRSyncPattern.UNKNOWN) {
                    System.out.print("SYNC: " + currentPattern.toString());
                }
                System.out.print("\n"); // at "+getTimestamp()+"
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


    void processVoiceBurst(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern, int bitErrors) {
        if(mMessageListener != null)
        {
            VoiceMessage voiceMessage = DMRMessageFactory.createVoiceMessage(pattern, binaryMessage, mCurrentTime, mCurrentSlot);
            mMessageListener.receive(voiceMessage);
            System.out.print("[TS-x" + (mCurrentSlot) +"] " + voiceMessage.getSyncPattern().toString() + " >>> VOICE FRAME [A] =========<<<<<<\n");
        }
        if(mCurrentSlot == 0) { // A is now running
            mInVoiceReadingSlotA = 1;
        } else {
            mInVoiceReadingSlotB = 1;
        }
    }
    private ShortLCMessage shortlc;

    void processDataBurst(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern, int bitErrors) {
        SlotType sl = new SlotType(binaryMessage);
        DataMessage datamessage = DMRMessageFactory.createDataMessage(sl.getDataType(),pattern,
                binaryMessage,mCurrentTime, mCurrentSlot);
        if(!datamessage.isValid())
        {
            dispatchSyncLoss(288); // CACH(24) + PAYLOAD(108 * 2) + SYNC(48)
            return;
        }
        if(datamessage.getSyncPattern() == DMRSyncPattern.MOBILE_STATION_DATA) {
            System.out.print("[TS-x"  + (mCurrentSlot == 0 ? 'A' : 'B') + "] ");
        } else {
            CACH cach = datamessage.getCACH();
            System.out.print("[TS-" + cach.getTimeslot() + (mCurrentSlot == 0 ? 'A' : 'B')+ "] ");
            try {
                if(cach.getLCSS() == LCSS.FIRST_FRAGMENT) {
                    shortlc = new ShortLCMessage();
                    shortlc.appendMsg(cach.getPayload());
                } else if(cach.getLCSS() == LCSS.CONTINUATION_FRAGMENT && (shortlc!=null)) {
                    shortlc.appendMsg(cach.getPayload());
                } else if(cach.getLCSS() == LCSS.LAST_FRAGMENT && (shortlc!=null)) {
                    shortlc.appendMsg(cach.getPayload());
                    shortlc.finalizeMessage();
                    mMessageListener.receive(shortlc);
                    System.out.print(shortlc.toString());
                    shortlc = null;
                }
            } catch (BitSetFullException e) {
                shortlc = null;
                e.printStackTrace();
            }
            //System.out.print("LCSS: " + cach.getLCSS().toString() + ": ");
        }

        System.out.print(datamessage.getSyncPattern().toString() + ", CC: " +
                datamessage.getSlotType().getColorCode() + " >>> ");
        String messageText = datamessage.toString();
        if(messageText != null) {
            System.out.print(datamessage.toString() + ">>> ");
        } else {
            System.out.print("[" + datamessage.getSlotType().getDataType().getLabel() + "] Not Parsed >>> ");
        }
        if(datamessage.getSyncPattern() == DMRSyncPattern.BASE_STATION_DATA) {
            System.out.print(" InBoundChannel: " + datamessage.getCACH().getInboundChannelAccessType());
        }
        System.out.print("\n");
        mMessageListener.receive(datamessage);
        if(mSyncDetectListener != null)
        {
            //mSyncDetectListener.syncDetected(bitErrors);
        }
    }

    @Override
    public void burstDetectedWithSync(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern, int bitErrors) {
        slotSyncMatrix[mCurrentSlot] = true; // current slot is synced
        System.out.println("Sync Detected: " + mCurrentSlot + ", at: " + getTimestamp());
        if(pattern == DMRSyncPattern.MOBILE_STATION_VOICE || pattern == DMRSyncPattern.BASE_STATION_VOICE) {
            processVoiceBurst(binaryMessage, pattern, bitErrors);
        } else if(pattern == DMRSyncPattern.MOBILE_STATION_DATA || pattern == DMRSyncPattern.BASE_STATION_DATA) {
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

}
