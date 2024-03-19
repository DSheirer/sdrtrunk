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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel grant update.
 */
public class GroupVoiceChannelGrantUpdate extends OSPMessage implements IFrequencyBandReceiver
{
    private static final int[] FREQUENCY_BAND_A = {16, 17, 18, 19};
    private static final int[] CHANNEL_NUMBER_A = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] GROUP_ADDRESS_A = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] FREQUENCY_BAND_B = {48, 49, 50, 51};
    private static final int[] CHANNEL_NUMBER_B = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] GROUP_ADDRESS_B = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private APCO25Channel mChannelA;
    private Identifier mGroupAddressA;
    private APCO25Channel mChannelB;
    private Identifier mGroupAddressB;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public GroupVoiceChannelGrantUpdate(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" GROUP A:").append(getGroupAddressA());
        sb.append(" CHAN A:").append(getChannelA());
        if(hasGroupB())
        {
            sb.append(" GROUP B:").append(getGroupAddressB());
            sb.append(" CHAN B:").append(getChannelB());
        }
        return sb.toString();
    }

    public APCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_A),
                    getMessage().getInt(CHANNEL_NUMBER_A));
        }

        return mChannelA;
    }

    public Identifier getGroupAddressA()
    {
        if(mGroupAddressA == null)
        {
            mGroupAddressA = APCO25Talkgroup.create(getMessage().getInt(GROUP_ADDRESS_A));
        }

        return mGroupAddressA;
    }

    public boolean hasGroupB()
    {
        return (getMessage().getInt(GROUP_ADDRESS_A) != getMessage().getInt(GROUP_ADDRESS_B)) &&
            getMessage().getInt(GROUP_ADDRESS_B) != 0;
    }

    public APCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_B),
                    getMessage().getInt(CHANNEL_NUMBER_B));
        }

        return mChannelB;
    }

    public Identifier getGroupAddressB()
    {
        if(mGroupAddressB == null)
        {
            mGroupAddressB = APCO25Talkgroup.create(getMessage().getInt(GROUP_ADDRESS_B));
        }

        return mGroupAddressB;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGroupAddressA());
            if(hasGroupB())
            {
                mIdentifiers.add(getGroupAddressB());
            }
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannelA());

        if(hasGroupB())
        {
            channels.add(getChannelB());
        }

        return channels;
    }
}
