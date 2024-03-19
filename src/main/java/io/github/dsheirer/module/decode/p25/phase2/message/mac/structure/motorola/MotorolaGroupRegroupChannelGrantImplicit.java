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
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.IP25ChannelGrantDetailProvider;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Motorola Group Regroup Channel Grant Implicit
 */
public class MotorolaGroupRegroupChannelGrantImplicit extends MacStructureVendor
        implements IFrequencyBandReceiver, IP25ChannelGrantDetailProvider, IServiceOptionsProvider
{
    private static final IntField SERVICE_OPTIONS = IntField.length8(OCTET_4_BIT_24);
    private static final IntField FREQUENCY_BAND = IntField.length4(OCTET_5_BIT_32);
    private static final IntField CHANNEL_NUMBER = IntField.length12(OCTET_5_BIT_32 + 4);
    private static final IntField SUPERGROUP_ADDRESS = IntField.length16(OCTET_7_BIT_48);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_9_BIT_64);

    private VoiceServiceOptions mServiceOptions;
    private List<Identifier> mIdentifiers;
    private PatchGroupIdentifier mPatchgroup;
    private RadioIdentifier mSourceAddress;
    private APCO25Channel mChannel;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupChannelGrantImplicit(CorrectedBinaryMessage message, int offset)
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
        if(getServiceOptions().isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        sb.append(" SUPERGROUP:").append(getTargetAddress());
        if(hasSourceAddress())
        {
            sb.append(" SOURCE:").append(getSourceAddress());
        }
        return sb.toString();
    }
    
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getInt(FREQUENCY_BAND), getInt(CHANNEL_NUMBER));
        }
        
        return mChannel;
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
     * Patch group for the channel grant
     */
    public PatchGroupIdentifier getTargetAddress()
    {
        if(mPatchgroup == null)
        {
            mPatchgroup = APCO25PatchGroup.create(getInt(SUPERGROUP_ADDRESS));
        }

        return mPatchgroup;
    }

    /**
     * Talker radio identifier.
     */
    public RadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    /**
     * Indicates if this message has a non-zero radio talker identifier.
     */
    public boolean hasSourceAddress()
    {
        return getInt(SOURCE_ADDRESS) > 0;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());

            if(hasSourceAddress())
            {
                mIdentifiers.add(getSourceAddress());
            }
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
