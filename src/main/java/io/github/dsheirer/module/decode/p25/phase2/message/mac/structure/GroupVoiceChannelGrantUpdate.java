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
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel user - abbreviated format
 */
public class GroupVoiceChannelGrantUpdate extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] FREQUENCY_BAND_A = {8, 9, 10, 11};
    private static final int[] CHANNEL_NUMBER_A = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] GROUP_ADDRESS_A = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] FREQUENCY_BAND_B = {40, 41, 42, 43};
    private static final int[] CHANNEL_NUMBER_B = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] GROUP_ADDRESS_B = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private List<Identifier> mIdentifiers;
    private TalkgroupIdentifier mGroupAddressA;
    private APCO25Channel mChannelA;
    private TalkgroupIdentifier mGroupAddressB;
    private APCO25Channel mChannelB;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelGrantUpdate(CorrectedBinaryMessage message, int offset)
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
        sb.append(" GROUP A:").append(getGroupAddressA());
        sb.append(" CHAN-A:").append(getChannelA());
        sb.append(" GROUP B:").append(getGroupAddressB());
        sb.append(" CHAN-B:").append(getChannelB());
        return sb.toString();
    }

    public APCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_A, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER_A, getOffset()));
        }

        return mChannelA;
    }

    public APCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_B, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER_B, getOffset()));
        }

        return mChannelB;
    }


    /**
     * Talkgroup channel A
     */
    public TalkgroupIdentifier getGroupAddressA()
    {
        if(mGroupAddressA == null)
        {
            mGroupAddressA = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_A));
        }

        return mGroupAddressA;
    }

    /**
     * Talkgroup channel B
     */
    public TalkgroupIdentifier getGroupAddressB()
    {
        if(mGroupAddressB == null)
        {
            mGroupAddressB = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_B));
        }

        return mGroupAddressB;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannelA());
            mIdentifiers.add(getChannelB());
            mIdentifiers.add(getGroupAddressA());
            mIdentifiers.add(getGroupAddressB());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannelA());
        channels.add(getChannelB());
        return channels;
    }
}
