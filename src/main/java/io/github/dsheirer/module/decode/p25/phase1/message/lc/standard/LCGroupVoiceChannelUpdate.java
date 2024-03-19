/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel update
 */
public class LCGroupVoiceChannelUpdate extends LinkControlWord implements IFrequencyBandReceiver
{
    private static final IntField FREQUENCY_BAND_A = IntField.length4(OCTET_1_BIT_8);
    private static final IntField CHANNEL_A = IntField.length12(OCTET_1_BIT_8 + 4);
    private static final IntField GROUP_ADDRESS_A = IntField.length16(OCTET_3_BIT_24);
    private static final IntField FREQUENCY_BAND_B = IntField.length4(OCTET_5_BIT_40);
    private static final IntField CHANNEL_B = IntField.length12(OCTET_5_BIT_40 + 4);
    private static final IntField GROUP_ADDRESS_B = IntField.length16(OCTET_7_BIT_56);

    private APCO25Channel mChannelA;
    private APCO25Channel mChannelB;
    private Identifier mTalkgroupA;
    private Identifier mTalkgroupB;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     */
    public LCGroupVoiceChannelUpdate(CorrectedBinaryMessage message)
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

    public APCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getInt(FREQUENCY_BAND_A), getInt(CHANNEL_A));
        }

        return mChannelA;
    }

    public APCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getInt(FREQUENCY_BAND_B), getInt(CHANNEL_B));
        }

        return mChannelB;
    }

    public boolean hasChannelB()
    {
        return getInt(CHANNEL_B) != 0 && getInt(GROUP_ADDRESS_A) != getInt(GROUP_ADDRESS_B);
    }

    public Identifier getGroupAddressA()
    {
        if(mTalkgroupA == null)
        {
            mTalkgroupA = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_A));
        }

        return mTalkgroupA;
    }

    public Identifier getGroupAddressB()
    {
        if(mTalkgroupB == null)
        {
            mTalkgroupB = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_B));
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
