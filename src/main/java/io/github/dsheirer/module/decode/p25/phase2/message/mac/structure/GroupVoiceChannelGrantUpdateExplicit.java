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
import io.github.dsheirer.module.decode.p25.identifier.channel.P25P2ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel grant update - explicit channel format
 */
public class GroupVoiceChannelGrantUpdateExplicit extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] SERVICE_OPTIONS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] TRANSMIT_FREQUENCY_BAND = {16, 17, 18, 19};
    private static final int[] TRANSMIT_CHANNEL_NUMBER = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] RECEIVE_FREQUENCY_BAND = {32, 33, 34, 35};
    private static final int[] RECEIVE_CHANNEL_NUMBER = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] GROUP_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};

    private List<Identifier> mIdentifiers;
    private VoiceServiceOptions mVoiceServiceOptions;
    private TalkgroupIdentifier mGroupAddress;
    private APCO25Channel mChannel;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelGrantUpdateExplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" GROUP:").append(getGroupAddress());
        sb.append(" CHAN:").append(getChannel());
        return sb.toString();
    }

    public VoiceServiceOptions getVoiceServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS, getOffset()));
        }

        return mVoiceServiceOptions;
    }

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = new APCO25ExplicitChannel(new P25P2ExplicitChannel(getMessage().getInt(TRANSMIT_FREQUENCY_BAND, getOffset()),
                getMessage().getInt(TRANSMIT_CHANNEL_NUMBER, getOffset()),
                getMessage().getInt(RECEIVE_FREQUENCY_BAND, getOffset()),
                getMessage().getInt(RECEIVE_CHANNEL_NUMBER, getOffset())));
        }

        return mChannel;
    }

    /**
     * Talkgroup channel A
     */
    public TalkgroupIdentifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS, getOffset()));
        }

        return mGroupAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannel());
            mIdentifiers.add(getGroupAddress());
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
