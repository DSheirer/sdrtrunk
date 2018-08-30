/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public abstract class GroupMultiChannelGrant extends ChannelGrant implements FrequencyBandReceiver
{
    public static final int[] FREQUENCY_BAND_1 = {80, 81, 82, 83};
    public static final int[] CHANNEL_NUMBER_1 = {84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] GROUP_ADDRESS_1 = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] FREQUENCY_BAND_2 = {112, 113, 114, 115};
    public static final int[] CHANNEL_NUMBER_2 = {116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127};
    public static final int[] GROUP_ADDRESS_2 = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IAPCO25Channel mChannel1;
    private IIdentifier mGroupAddress1;

    private IAPCO25Channel mChannel2;
    private IIdentifier mGroupAddress2;

    public GroupMultiChannelGrant(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" CHAN1:").append(getChannel1());
        sb.append(" GRP1:").append(getGroupAddress1());

        if(hasChannelNumber2())
        {
            sb.append(" CHAN2:").append(getChannel2());
            sb.append(" GRP2:").append(getGroupAddress2());
        }

        return sb.toString();
    }

    public IAPCO25Channel getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_1), mMessage.getInt(CHANNEL_NUMBER_1));
        }

        return mChannel1;
    }

    public IAPCO25Channel getChannel2()
    {
        if(mChannel2 == null)
        {
            mChannel2 = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_2), mMessage.getInt(CHANNEL_NUMBER_2));
        }

        return mChannel2;
    }

    public IIdentifier getGroupAddress1()
    {
        if(mGroupAddress1 == null)
        {
            mGroupAddress1 = APCO25FromTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS_1));
        }

        return mGroupAddress1;
    }

    public IIdentifier getGroupAddress2()
    {
        if(mGroupAddress2 == null)
        {
            mGroupAddress2 = APCO25FromTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS_2));
        }

        return mGroupAddress2;
    }

    public boolean hasChannelNumber2()
    {
        int channelNumber2 = mMessage.getInt(CHANNEL_NUMBER_2);
        return channelNumber2 != 0 && channelNumber2 != mMessage.getInt(CHANNEL_NUMBER_1);
    }

    public boolean isTDMAChannel1()
    {
        return getChannel1().isTDMAChannel();
    }

    public boolean isTDMAChannel2()
    {
        return getChannel2().isTDMAChannel();
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel1());
        channels.add(getChannel2());
        return channels;
    }
}
