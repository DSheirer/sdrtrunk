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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.DMRTier3Channel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;
import io.github.dsheirer.module.decode.dmr.message.type.AbsoluteChannelParameters;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Clear Channel (return to control channel)
 */
public class Clear extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] CHANNEL_NUMBER = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int TALKGROUP_FLAG = 31;
    protected static final int[] DESTINATION = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55};
    protected static final int[] SOURCE = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75, 76, 77, 78, 79};

    private DMRChannel mChannel;
    private AbsoluteChannelParameters mAbsoluteChannelParameters;
    private RadioIdentifier mSourceRadio;
    private IntegerIdentifier mDestinationId;
    private List<Identifier> mIdentifiers;

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
    public Clear(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                 long timestamp, int timeslot)
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
     * @param multiBlock continuation containing absolute channel parameters
     */
    public Clear(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                 long timestamp, int timeslot, MBCContinuationBlock multiBlock)
    {
        this(syncPattern, message, cach, slotType, timestamp, timeslot);

        if(multiBlock != null)
        {
            mAbsoluteChannelParameters = new AbsoluteChannelParameters(multiBlock.getMessage(), 0,
                getMoveToTimeslot());
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

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        sb.append(" CLEAR - RETURN TO ").append(getMoveToChannel());
        sb.append(" FM:").append(getSourceRadio());
        sb.append(" TO:").append(getDestinationId());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
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

    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DmrTier3Radio.createFrom(getMessage().getInt(SOURCE));
        }

        return mSourceRadio;
    }

    public IntegerIdentifier getDestinationId()
    {
        if(mDestinationId == null)
        {
            if(getMessage().get(TALKGROUP_FLAG))
            {
                mDestinationId = DMRTalkgroup.create(getMessage().getInt(DESTINATION));
            }
            else
            {
                mDestinationId = DmrTier3Radio.createTo(getMessage().getInt(DESTINATION));
            }
        }

        return mDestinationId;
    }


    /**
     * Timeslot to use for the channel grant
     */
    private int getMoveToTimeslot()
    {
        return 1; //Back to the control channel timeslot
    }

    /**
     * Channel number to move to
     */
    private int getMoveToChannelNumber()
    {
        return getMessage().getInt(CHANNEL_NUMBER);
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
     * DMR Channel and Timeslot to move to
     */
    public DMRChannel getMoveToChannel()
    {
        if(hasAbsoluteChannelParameters())
        {
            return mAbsoluteChannelParameters.getChannel();
        }

        if(mChannel == null)
        {
            mChannel = new DMRTier3Channel(getMoveToChannelNumber(), getMoveToTimeslot());
        }

        return mChannel;
    }

    /**
     * Logical Slot Number(s) for channels contained in this message
     */
    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        if(getMoveToChannel() instanceof DMRLogicalChannel)
        {
            return ((DMRLogicalChannel)getMoveToChannel()).getLSNArray();
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
        if(getMoveToChannel() instanceof DMRLogicalChannel)
        {
            DMRLogicalChannel channel = (DMRLogicalChannel)getMoveToChannel();

            for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
            {
                if(channel.getLogicalSlotNumber() == timeslotFrequency.getNumber())
                {
                    channel.setTimeslotFrequency(timeslotFrequency);
                }
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getMoveToChannel());
            mIdentifiers.add(getSourceRadio());
            mIdentifiers.add(getDestinationId());
        }

        return mIdentifiers;
    }
}
