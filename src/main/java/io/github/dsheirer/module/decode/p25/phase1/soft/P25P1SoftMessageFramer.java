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

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;

public class P25P1SoftMessageFramer implements Listener<Dibit>
{
    private static final double MILLISECONDS_PER_SYMBOL = 1.0 / 4800.0 / 1000.0;
    private Listener<IMessage> mMessageListener;
    private boolean mRunning = false;
    private int mDibitCounter = 0;
    private int mDibitSinceTimestampCounter = 0;
    private long mReferenceTimestamp = 0;
    private P25P1MessageAssembler mMessageAssembler;
    private P25P1DataUnitID mPreviousDataUnitID = P25P1DataUnitID.PLACEHOLDER;

    @Override
    public void receive(Dibit dibit)
    {
        mDibitSinceTimestampCounter++;

        if(mMessageAssembler != null)
        {
            mMessageAssembler.receive(dibit);

            if(mMessageAssembler.isComplete())
            {
                complete(mMessageAssembler);
                mMessageAssembler = null;
            }
        }
        else
        {
            mDibitCounter++;

            if(mDibitCounter >= 4800)
            {
                mDibitCounter -= 4800;
                broadcast(new SyncLossMessage(getTimestamp(), 9600, Protocol.APCO25));
            }
        }
    }

    /**
     * Process the completed message assembler.
     * @param assembler to process.
     */
    public void complete(P25P1MessageAssembler assembler)
    {
        if(assembler.getDataUnitID() == P25P1DataUnitID.PLACEHOLDER)
        {
//            System.out.println(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ HERE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~``");
            assembler.forceCompletion(mPreviousDataUnitID);
            //TODO: increment the dibit counter to add back in the bits from this
        }

        mPreviousDataUnitID = assembler.getDataUnitID();

//        System.out.println("  Processing completed assembler: " + assembler + " Object: " + assembler.hashCode());
    }

    /**
     * Broadcasts the assembled message to the registered listener.
     * @param message to broadcast - ignored if there is no registered listener.
     */
    private void broadcast(IMessage message)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * External trigger that a sync pattern is detected and the next arriving dibit is the start of the message that
     * contains that sync.
     *
     * @param nac value decoded from the NID or the previously decoded NAC value.
     * @param dataUnitID that was decoded from the NID or assumed/best guess based on previous messaging.
     * @param validNID indicates if the NID passed error detection and correction.
     */
    public void syncDetected(int nac, P25P1DataUnitID dataUnitID, boolean validNID)
    {
        mDibitCounter -= 116; //Sync(48) + NID(64) + Status(2) + First Dibit(2) Already Received

        if(mDibitCounter > 0)
        {
            broadcast(new SyncLossMessage(getTimestamp(), mDibitCounter * 2, Protocol.APCO25));
            mDibitCounter = 0;
        }

        //If there are residual message assemblers, force them to be completed
        if(mMessageAssembler != null)
        {
            mMessageAssembler.forceCompletion(mPreviousDataUnitID);
            complete(mMessageAssembler);
        }

        //If our data unit ID is not a valid value, use the placeholder until we can determine how many symbols elapsed
        //and from there make an educated guess.
        if(!dataUnitID.isValidPrimaryDUID())
        {
            dataUnitID = P25P1DataUnitID.PLACEHOLDER;
        }

        if(dataUnitID == P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1)
        {
            dataUnitID = P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_3;
        }

        mMessageAssembler = new P25P1MessageAssembler(nac, dataUnitID, validNID);

        StringBuilder debugSB = new StringBuilder();
        debugSB.append("  --> Message Framer Sync Detect - NAC: ").append(nac);
        debugSB.append(" Data Unit: ").append(dataUnitID);
        debugSB.append(" Valid NID: ").append(validNID);
//        System.out.println(debugSB);
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
