/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.channel.DMRTier3Channel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;
import io.github.dsheirer.module.decode.dmr.message.type.AbsoluteChannelParameters;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import io.github.dsheirer.module.decode.dmr.message.type.SystemIdentityCode;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Vote Now Advice
 */
public class VoteNowAdvice extends Announcement implements ITimeslotFrequencyReceiver
{
    //Broadcast Parameters 1: 21-34
    private static final int VOTED_SYSTEM_IDENTITY_CODE_OFFSET = 21;

    //Broadcast Parameters 2: 56-79
    private static final int NETWORK_CONNECTION_STATUS_AVAILABLE_FLAG = 56;
    private static final int ACTIVE_NETWORK_CONNECTION_FLAG = 57;
    private static final int[] CONFIRMED_CHANNEL_PRIORITY = new int[]{58, 59, 60};
    private static final int[] ADJACENT_CHANNEL_PRIORITY = new int[]{61, 62, 63};
    private static final int[] RESERVED = new int[]{64, 65, 66, 67};
    private static final int[] VOTED_CHANNEL_NUMBER = new int[]{68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private SystemIdentityCode mVotedSystemIdentityCode;
    private List<Identifier> mIdentifiers;
    private DMRChannel mChannel;
    private AbsoluteChannelParameters mAbsoluteChannelParameters;

    /**
     * Constructs a single-block CSBK instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public VoteNowAdvice(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Constructs a multi-block MBC instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     * @param multiBlock containing absolute frequency parameters
     */
    public VoteNowAdvice(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                         long timestamp, int timeslot, MBCContinuationBlock multiBlock)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);

        if(multiBlock != null)
        {
            //Timeslot hard-coded to one for control channel
            mAbsoluteChannelParameters = new AbsoluteChannelParameters(multiBlock.getMessage(), 0, 1);
        }
    }

    /**
     * Checks CRC and sets the message valid flag according to the results.
     */
    public void checkCRC()
    {
        if(getSlotType().getDataType() == DataType.MBC_HEADER)
        {
            checkMultiBlockCRC(getAbsoluteChannelParameters());
        }
        else
        {
            //Use the standard CSBK CRC check
            super.checkCRC();
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        sb.append(" VOTED NETWORK:").append(getVotedSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getVotedSystemIdentityCode().getSite());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" THIS ").append(getSystemIdentityCode().getModel());
        sb.append(" NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());

        return sb.toString();
    }

    /**
     * Indicates if the active network connection status is available.
     *
     * See also: isActiveNetworkConnection()
     */
    public boolean hasNetworkConnectionStatus()
    {
        return getMessage().get(NETWORK_CONNECTION_STATUS_AVAILABLE_FLAG);
    }

    /**
     * Indicates if this site has an active network connection
     *
     * See also: hasNetworkConnectionStatus()
     */
    public boolean isActiveNetworkConnection()
    {
        return getMessage().get(ACTIVE_NETWORK_CONNECTION_FLAG);
    }

    /**
     * Confirmed channel priority
     */
    public int getConfirmedChannelPriority()
    {
        return getMessage().getInt(CONFIRMED_CHANNEL_PRIORITY);
    }

    /**
     * Adjacent channel priority
     */
    public int getAdjacentChannelPriority()
    {
        return getMessage().getInt(ADJACENT_CHANNEL_PRIORITY);
    }

    /**
     * Control Channel Number for the voted site
     */
    public int getVotedChannelNumber()
    {
        return getMessage().getInt(VOTED_CHANNEL_NUMBER);
    }

    /**
     * Absolute channel parameters structure
     */
    public AbsoluteChannelParameters getAbsoluteChannelParameters()
    {
        return mAbsoluteChannelParameters;
    }

    /**
     * Indicates if this message has an absolute channel parameters structure
     */
    public boolean hasAbsoluteChannelParameters()
    {
        return getAbsoluteChannelParameters() != null;
    }

    /**
     * DMR Channel and Timeslot for the channel grant
     */
    public DMRChannel getChannel()
    {
        if(hasAbsoluteChannelParameters())
        {
            return mAbsoluteChannelParameters.getChannel();
        }

        if(mChannel == null)
        {
            mChannel = new DMRTier3Channel(getMessage().getInt(VOTED_CHANNEL_NUMBER), 1);
        }

        return mChannel;
    }

    /**
     * Voted Site System Identity Code structure
     */
    public SystemIdentityCode getVotedSystemIdentityCode()
    {
        if(mVotedSystemIdentityCode == null)
        {
            mVotedSystemIdentityCode = new SystemIdentityCode(getMessage(), VOTED_SYSTEM_IDENTITY_CODE_OFFSET, false);
        }

        return mVotedSystemIdentityCode;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode().getNetwork());
            mIdentifiers.add(getSystemIdentityCode().getSite());
        }

        return mIdentifiers;
    }

    /**
     * Logical Slot Number(s) for channels contained in this message
     */
    @Override
    public int[] getLogicalChannelNumbers()
    {
        if(getChannel() != null)
        {
            return getChannel().getLogicalChannelNumbers();
        }

        return new int[0];
    }

    /**
     * Applies the timeslot frequency lookup information to channels contained in this message
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        if(getChannel() != null)
        {
            getChannel().apply(timeslotFrequencies);
        }
    }
}
