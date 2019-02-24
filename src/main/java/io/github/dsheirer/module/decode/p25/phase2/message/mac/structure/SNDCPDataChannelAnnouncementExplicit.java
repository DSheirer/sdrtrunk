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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.DataServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * SNDCP data channel announcement - explicit format
 */
public class SNDCPDataChannelAnnouncementExplicit extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] SERVICE_OPTIONS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int AUTONOMOUS_ACCESS_FLAG = 16;
    private static final int REQUESTED_ACCESS_FLAG = 17;
    private static final int[] TRANSMIT_FREQUENCY_BAND = {24, 25, 26, 27};
    private static final int[] TRANSMIT_CHANNEL_NUMBER = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] RECEIVE_FREQUENCY_BAND = {40, 41, 42, 43};
    private static final int[] RECEIVE_CHANNEL_NUMBER = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] DATA_ACCESS_CONTROL = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private List<Identifier> mIdentifiers;
    private APCO25Channel mChannel;
    private TalkgroupIdentifier mTargetAddress;
    private DataServiceOptions mServiceOptions;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SNDCPDataChannelAnnouncementExplicit(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" CHAN:").append(getChannel());
        if(isAutonomousAccess() && isRequestedAccess())
        {
            sb.append(" AUTONOMOUS/REQUESTED-ACCESS");
        }
        else if(isAutonomousAccess())
        {
            sb.append(" AUTONOMOUS-ACCESS");
        }
        else if(isRequestedAccess())
        {
            sb.append(" REQUESTED-ACCESS");
        }
        sb.append(" DAC:").append(getDataAccessControl());
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    public boolean isAutonomousAccess()
    {
        return getMessage().get(AUTONOMOUS_ACCESS_FLAG + getOffset());
    }

    public boolean isRequestedAccess()
    {
        return getMessage().get(REQUESTED_ACCESS_FLAG + getOffset());
    }


    /**
     * Voice channel service options
     */
    public DataServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new DataServiceOptions(getMessage().getInt(SERVICE_OPTIONS, getOffset()));
        }

        return mServiceOptions;
    }

    /**
     * Channel
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            if(isExplicitChannel())
            {
                mChannel = APCO25ExplicitChannel.create(getMessage().getInt(TRANSMIT_FREQUENCY_BAND, getOffset()),
                    getMessage().getInt(TRANSMIT_CHANNEL_NUMBER, getOffset()),
                    getMessage().getInt(RECEIVE_FREQUENCY_BAND, getOffset()),
                    getMessage().getInt(RECEIVE_CHANNEL_NUMBER, getOffset()));
            }
            else
            {
                mChannel = APCO25Channel.create(getMessage().getInt(TRANSMIT_FREQUENCY_BAND, getOffset()),
                    getMessage().getInt(TRANSMIT_CHANNEL_NUMBER, getOffset()));
            }
        }

        return mChannel;
    }

    /**
     * Indicates if the channel is an explicit channel, meaning that there are separate downlink
     * and uplink channel numbers included.  When false, the channel number is the same for both
     * uplink and downlink.
     *
     * @return true if this is an explicit channel.
     */
    private boolean isExplicitChannel()
    {
        return getMessage().getInt(RECEIVE_CHANNEL_NUMBER, getOffset()) != 4095;
    }

    public int getDataAccessControl()
    {
        return getMessage().getInt(DATA_ACCESS_CONTROL, getOffset());
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannel());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
