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
package io.github.dsheirer.module.decode.p25.message.tsbk.motorola;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public class PatchGroupVoiceChannelGrantUpdate extends MotorolaTSBKMessage implements FrequencyBandReceiver
{
    public static final int[] IDENTIFIER_1 = {80, 81, 82, 83};
    public static final int[] CHANNEL_1 = {84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] PATCH_GROUP_ADDRESS_1 = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] IDENTIFIER_2 = {112, 113, 114, 115};
    public static final int[] CHANNEL_2 = {116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127};
    public static final int[] PATCH_GROUP_ADDRESS_2 = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IIdentifier mPatchGroupAddress1;
    private IIdentifier mPatchGroupAddress2;
    private IAPCO25Channel mChannel1;
    private IAPCO25Channel mChannel2;

    public PatchGroupVoiceChannelGrantUpdate(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());


        sb.append(" PATCH GRP1:");
        sb.append(getPatchGroupAddress1());
        sb.append(" GRP2:");
        sb.append(getPatchGroupAddress2());

        sb.append(" CHAN 1:");
        sb.append(getChannel1());

        sb.append(" CHAN 2:");
        sb.append(getChannel2());

        return sb.toString();
    }

    public IIdentifier getPatchGroupAddress1()
    {
        if(mPatchGroupAddress1 == null)
        {
            mPatchGroupAddress1 = APCO25ToTalkgroup.createGroup(mMessage.getInt(PATCH_GROUP_ADDRESS_1));
        }

        return mPatchGroupAddress1;
    }

    public IIdentifier getPatchGroupAddress2()
    {
        if(mPatchGroupAddress2 == null)
        {
            mPatchGroupAddress2 = APCO25ToTalkgroup.createGroup(mMessage.getInt(PATCH_GROUP_ADDRESS_2));
        }

        return mPatchGroupAddress2;
    }

    public IAPCO25Channel getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = APCO25Channel.create(mMessage.getInt(IDENTIFIER_1), mMessage.getInt(CHANNEL_1));
        }

        return mChannel1;
    }

    public IAPCO25Channel getChannel2()
    {
        if(mChannel2 == null)
        {
            mChannel2 = APCO25Channel.create(mMessage.getInt(IDENTIFIER_2), mMessage.getInt(CHANNEL_2));
        }

        return mChannel2;
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
