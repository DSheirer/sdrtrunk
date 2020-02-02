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
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataType;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Sync Detector and Message Framer.  Includes capability to detect PLL out-of-phase lock errors
 * and issue phase corrections.
 */
public class DMRMessageFramer implements Listener<Dibit>, IDMRDataUnitDetectListener, IDMRVoiceUnitDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageFramer.class);

    private DMRDataUnitDetector mDataUnitDetector;
    //voiceUnitDetector
    private Listener<Message> mMessageListener;
    private boolean mAssemblingMessage = false;
    private CorrectedBinaryMessage mBinaryMessage;
    private DataType mDataUnitID;
    private double mBitRate;
    private long mCurrentTime = System.currentTimeMillis();
    private ISyncDetectListener mSyncDetectListener;

    public DMRMessageFramer(IPhaseLockedLoop phaseLockedLoop, int bitRate)
    {
        mDataUnitDetector = new DMRDataUnitDetector(this, phaseLockedLoop);
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

    public DMRDataUnitDetector getDataUnitDetector()
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
        if(mAssemblingMessage) // when there is a VOICE
        {
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
                dispatchMessage();
            }
        }
        else
        {
            mDataUnitDetector.receive(dibit);
        }
    }

    private void dispatchMessage()
    {
        if(mMessageListener != null)
        {
            System.out.println("DATA UNIT = " + mDataUnitID.getLabel());
            int slottype = (mBinaryMessage.get(63) ? 8 : 0) | (mBinaryMessage.get(63) ? 4 : 0) | (mBinaryMessage.get(64) ? 2 : 0) | (mBinaryMessage.get(64) ? 1 : 0);
            System.out.println("YOY, GET = " + DataType.fromValue(slottype));
            mDataUnitID = DataType.fromValue(slottype);
            switch(mDataUnitID)
            {

            }
            DMRMessage message = DMRMessageFactory.createDataMessage(mDataUnitID, DMRSyncPattern.BASE_STATION_DATA, getTimestamp(), mBinaryMessage);
            mMessageListener.receive(message);
            reset(mDataUnitID.getMessageLength());
            mBinaryMessage = new CorrectedBinaryMessage(288);
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
        mAssemblingMessage = false;
        mDataUnitID = null;
        mDataUnitDetector.reset();
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
    public void voiceUnitDetected(DataType typeID, int colorcode, int bitErrors) {
        mDataUnitID = typeID;
        mBinaryMessage = new CorrectedBinaryMessage(typeID.getMessageLength());
        mBinaryMessage.incrementCorrectedBitCount(bitErrors);

        mAssemblingMessage = true;
    }

    @Override
    public void dataUnitDetected(DataMessage datamessage, int bitErrors) {
        if(!datamessage.isValid())
        {
            dispatchSyncLoss(288); // CACH(24) + PAYLOAD(108 * 2) + SYNC(48)
            return;
        }
        if(datamessage.getSyncPattern() == DMRSyncPattern.MOBILE_STATION_DATA) {
            System.out.print(datamessage.getSyncPattern().toString() + " >>> TS = NO CC: " +
                    datamessage.getSlotType().getColorCode() + " " +datamessage.getSlotType().getDataType().getLabel() + " <<<\n");
        } else {
            System.out.print(datamessage.getSyncPattern().toString() + " >>> TS = " + datamessage.getCACH().getTimeslot() + ", " +
                    datamessage.getCACH().getInboundChannelAccessType()+" -> CC: " +
                    datamessage.getSlotType().getColorCode() + " " +datamessage.getSlotType().getDataType().getLabel() + " <<<\n");
        }
        mMessageListener.receive(datamessage);
        if(mSyncDetectListener != null)
        {
            //mSyncDetectListener.syncDetected(bitErrors);
        }
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
            if(bitsProcessed < 64)
            {
                mMessageListener.receive(new StuffBitsMessage(getTimestamp(), bitsProcessed, Protocol.APCO25));
            }
            else
            {
                mMessageListener.receive(new SyncLossMessage(getTimestamp(), bitsProcessed, Protocol.APCO25));
            }
        }
    }

}
