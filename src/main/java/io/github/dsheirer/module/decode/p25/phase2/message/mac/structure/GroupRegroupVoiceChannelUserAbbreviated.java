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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Group Regroup Voice Channel User Abbreviated
 */
public class GroupRegroupVoiceChannelUserAbbreviated extends MacStructure implements IServiceOptionsProvider
{
    private static final IntField SUPERGROUP = IntField.length16(OCTET_3_BIT_16);
    private static final IntField RADIO = IntField.length24(OCTET_5_BIT_32);
    private List<Identifier> mIdentifiers;
    private PatchGroupIdentifier mPatchgroup;
    private RadioIdentifier mRadio;
    private ServiceOptions mServiceOptions = new VoiceServiceOptions(0);

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupRegroupVoiceChannelUserAbbreviated(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("GROUP REGROUP VOICE CHANNEL USER ABBREVIATED");
        sb.append(" TALKGROUP:").append(getPatchgroup());
        sb.append(" TALKER RADIO:").append(getRadio());
        return sb.toString();
    }

    @Override
    public ServiceOptions getServiceOptions()
    {
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
     * Indicates if this message has a non-zero patch group identifier.
     */
    public boolean hasPatchgroup()
    {
        return getInt(SUPERGROUP) > 0;
    }

    /**
     * Talker radio identifier.
     */
    public RadioIdentifier getRadio()
    {
        if(mRadio == null)
        {
            mRadio = APCO25RadioIdentifier.createFrom(getInt(RADIO));
        }

        return mRadio;
    }

    /**
     * Indicates if this message has a non-zero radio talker identifier.
     */
    public boolean hasRadio()
    {
        return getInt(RADIO) > 0;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasPatchgroup())
            {
                mIdentifiers.add(getPatchgroup());
            }

            if(hasRadio())
            {
                mIdentifiers.add(getRadio());
            }
        }

        return mIdentifiers;
    }
}
