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
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Regroup Channel Update
 */
public class MotorolaGroupRegroupChannelGrantUpdate extends MacStructureVendor implements IFrequencyBandReceiver
{
    private static final IntField FREQUENCY_BAND_A = IntField.length4(OCTET_4_BIT_24);
    private static final IntField CHANNEL_NUMBER_A = IntField.length12(OCTET_4_BIT_24 + 4);
    private static final IntField SUPERGROUP_ADDRESS_A = IntField.length16(OCTET_6_BIT_40);
    private static final IntField FREQUENCY_BAND_B = IntField.length4(OCTET_8_BIT_56);
    private static final IntField CHANNEL_NUMBER_B = IntField.length12(OCTET_8_BIT_56 + 4);
    private static final IntField SUPERGROUP_ADDRESS_B = IntField.length16(OCTET_10_BIT_72);

    private List<Identifier> mIdentifiers;
    private PatchGroupIdentifier mPatchgroupA;
    private PatchGroupIdentifier mPatchgroupB;
    private APCO25Channel mChannelA;
    private APCO25Channel mChannelB;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupChannelGrantUpdate(CorrectedBinaryMessage message, int offset)
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
        sb.append(" SUPERGROUP A:").append(getPatchgroupA());
        sb.append(" CHAN A:").append(getChannelA());

        if(hasPatchgroupB())
        {
            sb.append(" SUPERGROUP B:").append(getPatchgroupB());
            sb.append(" CHAN B:").append(getChannelB());
        }
        return sb.toString();
    }
    
    public APCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getInt(FREQUENCY_BAND_A), getInt(CHANNEL_NUMBER_A));
        }
        
        return mChannelA;
    }

    public APCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getInt(FREQUENCY_BAND_B), getInt(CHANNEL_NUMBER_B));
        }

        return mChannelB;
    }

    /**
     * Patch group A
     */
    public PatchGroupIdentifier getPatchgroupA()
    {
        if(mPatchgroupA == null)
        {
            mPatchgroupA = APCO25PatchGroup.create(getInt(SUPERGROUP_ADDRESS_A));
        }

        return mPatchgroupA;
    }

    /**
     * Patch group B
     */
    public PatchGroupIdentifier getPatchgroupB()
    {
        if(mPatchgroupB == null)
        {
            mPatchgroupB = APCO25PatchGroup.create(getInt(SUPERGROUP_ADDRESS_B));
        }

        return mPatchgroupB;
    }

    /**
     * Indicates if this message has a second (B) patchgroup that is being reported.
     * @return
     */
    public boolean hasPatchgroupB()
    {
        int patchgroupB = getInt(SUPERGROUP_ADDRESS_B);
        return patchgroupB > 0 && patchgroupB != getInt(SUPERGROUP_ADDRESS_A);
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getPatchgroupA());
            mIdentifiers.add(getChannelA());

            if(hasPatchgroupB())
            {
                mIdentifiers.add(getPatchgroupB());
                mIdentifiers.add(getChannelB());
            }
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannelA());

        if(hasPatchgroupB())
        {
            channels.add(getChannelB());
        }

        return channels;
    }
}
