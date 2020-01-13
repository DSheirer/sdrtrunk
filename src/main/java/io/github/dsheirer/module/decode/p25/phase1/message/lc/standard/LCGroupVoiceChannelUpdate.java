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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Update detailing other users/channels that are active on the network.
 */
public class LCGroupVoiceChannelUpdate extends LinkControlWord implements IFrequencyBandReceiver
{
    public static final int[] FREQUENCY_BAND_A = {8, 9, 10, 11};
    public static final int[] CHANNEL_A = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    public static final int[] GROUP_ADDRESS_A = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    public static final int[] FREQUENCY_BAND_B = {40, 41, 42, 43};
    public static final int[] CHANNEL_B = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    public static final int[] GROUP_ADDRESS_B = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private IChannelDescriptor mChannelA;
    private IChannelDescriptor mChannelB;
    private Identifier mTalkgroupA;
    private Identifier mTalkgroupB;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     */
    public LCGroupVoiceChannelUpdate(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TALKGROUP A:").append(getGroupAddressA());
        sb.append(" CHAN A:").append(getChannelA());

        if(hasChannelB())
        {
            sb.append(" TALKGROUP B:").append(getGroupAddressB());
            sb.append(" CHAN B:").append(getChannelB());
        }

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGroupAddressA());

            if(hasChannelB())
            {
                mIdentifiers.add(getGroupAddressB());
            }
        }

        return mIdentifiers;
    }

    public IChannelDescriptor getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_A), getMessage().getInt(CHANNEL_A));
        }

        return mChannelA;
    }

    public IChannelDescriptor getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_B), getMessage().getInt(CHANNEL_B));
        }

        return mChannelB;
    }

    public boolean hasChannelB()
    {
        return getMessage().getInt(CHANNEL_B) != 0 && getMessage().getInt(GROUP_ADDRESS_A) != getMessage().getInt(GROUP_ADDRESS_B);
    }

    public Identifier getGroupAddressA()
    {
        if(mTalkgroupA == null)
        {
            mTalkgroupA = APCO25Talkgroup.create(getMessage().getInt(GROUP_ADDRESS_A));
        }

        return mTalkgroupA;
    }

    public Identifier getGroupAddressB()
    {
        if(mTalkgroupB == null)
        {
            mTalkgroupB = APCO25Talkgroup.create(getMessage().getInt(GROUP_ADDRESS_B));
        }

        return mTalkgroupB;
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
