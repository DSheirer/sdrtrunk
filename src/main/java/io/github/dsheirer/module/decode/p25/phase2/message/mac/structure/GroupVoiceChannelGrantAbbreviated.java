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
import io.github.dsheirer.module.decode.p25.identifier.channel.P25P2Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel grant - abbreviated format
 */
public class GroupVoiceChannelGrantAbbreviated extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] SERVICE_OPTIONS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] FREQUENCY_BAND = {16, 17, 18, 19};
    private static final int[] CHANNEL_NUMBER = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] GROUP_ADDRESS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SOURCE_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
        65, 66, 67, 68, 69, 70, 71};

    private List<Identifier> mIdentifiers;
    private APCO25Channel mChannel;
    private TalkgroupIdentifier mGroupAddress;
    private TalkgroupIdentifier mSourceAddress;
    private VoiceServiceOptions mServiceOptions;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelGrantAbbreviated(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TO:").append(getGroupAddress());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    /**
     * Voice channel service options
     */
    public VoiceServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS, getOffset()));
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
            mChannel = new APCO25Channel(new P25P2Channel(getMessage().getInt(FREQUENCY_BAND, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER, getOffset())));
        }

        return mChannel;
    }

    /**
     * To Talkgroup
     */
    public TalkgroupIdentifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS, getOffset()));
        }

        return mGroupAddress;
    }

    /**
     * From Radio Unit
     */
    public TalkgroupIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getSourceAddress());
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
