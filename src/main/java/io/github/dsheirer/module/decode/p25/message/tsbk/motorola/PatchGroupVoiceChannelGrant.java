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
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

import java.util.ArrayList;
import java.util.List;

public class PatchGroupVoiceChannelGrant extends MotorolaTSBKMessage implements FrequencyBandReceiver
{
    public static final int[] SERVICE_OPTIONS = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int[] IDENTIFIER = {88, 89, 90, 91};
    public static final int[] CHANNEL = {92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] PATCH_GROUP_ADDRESS = {104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] SOURCE_ADDRESS = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private ServiceOptions mServiceOptions;
    private IAPCO25Channel mChannel;
    private IIdentifier mSourceAddress;
    private IIdentifier mPatchGroupAddress;

    public PatchGroupVoiceChannelGrant(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(" ").append(getServiceOptions());
        sb.append(" PATCH GROUP:");
        sb.append(getPatchGroupAddress());
        sb.append(" FROM:");
        sb.append(getSourceAddress());

        return sb.toString();
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(mMessage.getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    public IIdentifier getPatchGroupAddress()
    {
        if(mPatchGroupAddress == null)
        {
            mPatchGroupAddress = APCO25ToTalkgroup.createGroup(mMessage.getInt(PATCH_GROUP_ADDRESS));
        }

        return mPatchGroupAddress;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(mMessage.getInt(IDENTIFIER), mMessage.getInt(CHANNEL));
        }

        return mChannel;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
