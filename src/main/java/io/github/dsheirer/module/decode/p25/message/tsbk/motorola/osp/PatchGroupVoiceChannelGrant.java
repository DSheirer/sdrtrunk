/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.message.tsbk.motorola.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

public class PatchGroupVoiceChannelGrant extends OSPMessage implements IFrequencyBandReceiver
{
    public static final int[] SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    public static final int[] FREQUENCY_BAND = {24, 25, 26, 27};
    public static final int[] CHANNEL_NUMBER = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    public static final int[] PATCH_GROUP_ADDRESS = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    public static final int[] SOURCE_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
        74, 75, 76, 77, 78, 79};

    private VoiceServiceOptions mVoiceServiceOptions;
    private APCO25Channel mChannel;
    private Identifier mSourceAddress;
    private PatchGroupIdentifier mPatchGroup;
    private List<Identifier> mIdentifiers;

    public PatchGroupVoiceChannelGrant(DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestlot)
    {
        super(dataUnitID, message, nac, timestlot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(" PATCH GROUP:").append(getPatchGroup());
        sb.append(" FROM:").append(getSourceAddress());
        sb.append(" SERVICE OPTIONS:").append(getVoiceServiceOptions());

        return sb.toString();
    }

    public VoiceServiceOptions getVoiceServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }

    public PatchGroupIdentifier getPatchGroup()
    {
        if(mPatchGroup == null)
        {
            mPatchGroup = APCO25PatchGroup.create(new PatchGroup(APCO25ToTalkgroup.createGroup(getMessage().getInt(PATCH_GROUP_ADDRESS))));
        }

        return mPatchGroup;
    }

    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND), getMessage().getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getPatchGroup());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
