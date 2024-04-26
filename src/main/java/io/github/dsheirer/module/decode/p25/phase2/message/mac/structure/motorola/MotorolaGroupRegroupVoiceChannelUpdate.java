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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Motorola Group Regroup Voice Channel Update
 * <p>
 * Indicates supergroup call activity on another (traffic) channel.
 */
public class MotorolaGroupRegroupVoiceChannelUpdate extends MacStructureVendor implements IFrequencyBandReceiver, IServiceOptionsProvider
{
    private static final IntField SERVICE_OPTIONS = IntField.length8(OCTET_3_BIT_16);
    private static final IntField SUPERGROUP = IntField.length16(OCTET_4_BIT_24);
    private static final IntField FREQUENCY_BAND = IntField.length4(OCTET_6_BIT_40);
    private static final IntField CHANNEL_NUMBER = IntField.length12(OCTET_6_BIT_40 + 4);

    private VoiceServiceOptions mServiceOptions;
    private List<Identifier> mIdentifiers;
    private PatchGroupIdentifier mPatchgroup;
    private APCO25Channel mChannel;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupVoiceChannelUpdate(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA GROUP REGROUP VOICE CHANNEL UPDATE");
        if(getServiceOptions().isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        sb.append(" SUPERGROUP:").append(getPatchgroup());
        sb.append(" IS ACTIVE ON CHANNEL:").append(getChannel());
        return sb.toString();
    }

    /**
     * Service options for the referenced call.
     */
    public VoiceServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new VoiceServiceOptions(getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    /**
     * Talkgroup active on this channel/timeslot.
     */
    public PatchGroupIdentifier getPatchgroup()
    {
        if(mPatchgroup == null)
        {
            mPatchgroup = APCO25PatchGroup.create(getInt(SUPERGROUP));
        }

        return mPatchgroup;
    }

    /**
     * Current channel where this call is taking place.
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getInt(FREQUENCY_BAND), getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getPatchgroup());
        }

        return mIdentifiers;
    }

    /**
     * Implements the IFrequencyBandReceiver interface to expose the channel to be enriched with frequency band info.
     */
    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
