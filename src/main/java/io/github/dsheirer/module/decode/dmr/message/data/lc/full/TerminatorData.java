/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Terminator Data Full Link Control Message
 *
 * See: TS 102 361-3 Paragraph 7.1.1.1 Terminator Data Link Control PDU
 */
public class TerminatorData extends FullLCMessage
{
    private static final int[] DESTINATION_LLID = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SOURCE_LLID = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
        56, 57, 58, 59, 60, 61, 62, 63};
    private static final int GROUP_INDIVIDUAL_FLAG = 64;
    private static final int RESPONSE_REQUESTED = 65;
    private static final int FULL_MESSAGE_FLAG = 66;
    private static final int RESYNC_FLAG = 68;
    private static final int[] SEND_SEQUENCE_NUMBER = new int[]{69, 70, 71};

    private RadioIdentifier mSourceLLID;
    private IntegerIdentifier mDestinationLLID;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     * @param timestamp
     * @param timeslot
     */
    public TerminatorData(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC ERROR " + getMessage().getCorrectedBitCount() + "] ");
        }

        sb.append("FM:").append(getSourceLLID());
        sb.append(" TO:").append(getDestinationLLID());
        sb.append(isFullMessage() ? " COMPLETE" : " FRAGMENT");
        sb.append(" SEQUENCE:").append(getSendSequenceNumber());
        if(isResync())
        {
            sb.append( " RESYNC");
        }
        if(isResponseRequested())
        {
            sb.append(" ACK:YES");
        }

        return sb.toString();
    }

    /**
     * Destination or TO LLID identifier
     */
    public IntegerIdentifier getDestinationLLID()
    {
        if(mDestinationLLID == null)
        {
            if(isGroupDestination())
            {
                mDestinationLLID = DMRTalkgroup.create(getMessage().getInt(DESTINATION_LLID));
            }
            else
            {
                mDestinationLLID = DMRRadio.createTo(getMessage().getInt(DESTINATION_LLID));
            }
        }

        return mDestinationLLID;
    }

    /**
     * Indicates if the destination LLID is a talkgroup (true) or radio (false).
     */
    public boolean isGroupDestination()
    {
        return getMessage().get(GROUP_INDIVIDUAL_FLAG);
    }

    /**
     * Source or FROM LLID identifier
     */
    public RadioIdentifier getSourceLLID()
    {
        if(mSourceLLID == null)
        {
            mSourceLLID = DMRRadio.createFrom(getMessage().getInt(SOURCE_LLID));
        }

        return mSourceLLID;
    }

    /**
     * Indicates if a response is requested
     */
    public boolean isResponseRequested()
    {
        return getMessage().get(RESPONSE_REQUESTED);
    }

    /**
     * Indicates if the transmitted data is a full message (or partial).
     */
    public boolean isFullMessage()
    {
        return getMessage().get(FULL_MESSAGE_FLAG);
    }

    /**
     * Indicates if the transmitted message is asserting a resync for a sequence of partial fragments.
     */
    public boolean isResync()
    {
        return getMessage().get(RESYNC_FLAG);
    }

    /**
     * Send sequence number for a sequence of partial fragments
     */
    public int getSendSequenceNumber()
    {
        return getMessage().getInt(SEND_SEQUENCE_NUMBER);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSourceLLID());
            mIdentifiers.add(getDestinationLLID());
        }

        return mIdentifiers;
    }
}
