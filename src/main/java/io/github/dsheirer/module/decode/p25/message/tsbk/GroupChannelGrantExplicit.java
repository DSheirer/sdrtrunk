/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public abstract class GroupChannelGrantExplicit extends ChannelGrant
{
    public static final int[] DOWNLINK_FREQUENCY_BAND = {96, 97, 98, 99};
    public static final int[] DOWNLINK_CHANNEL_NUMBER = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] UPLINK_FREQUENCY_BAND = {112, 113, 114, 115};
    public static final int[] UPLINK_CHANNEL_NUMBER = {116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127};
    public static final int[] GROUP_ADDRESS = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IAPCO25Channel mChannel;
    private IIdentifier mGroupAddress;

    public GroupChannelGrantExplicit(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public boolean isTDMAChannel()
    {
        return getChannel().isTDMAChannel();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }

        sb.append(" CHAN:").append(getChannel());
        sb.append(" GRP:");
        sb.append(getGroupAddress());

        return sb.toString();
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(mMessage.getInt(DOWNLINK_FREQUENCY_BAND),
                mMessage.getInt(DOWNLINK_CHANNEL_NUMBER), mMessage.getInt(UPLINK_FREQUENCY_BAND),
                mMessage.getInt(UPLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public IIdentifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25FromTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
