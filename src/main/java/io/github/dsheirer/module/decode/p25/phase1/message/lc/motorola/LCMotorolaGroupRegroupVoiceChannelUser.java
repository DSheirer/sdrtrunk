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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.VoiceLinkControlMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Regroup Voice Channel User.
 */
public class LCMotorolaGroupRegroupVoiceChannelUser extends VoiceLinkControlMessage
{
    private static final IntField SUPERGROUP_ADDRESS = IntField.length16(OCTET_4_BIT_32);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_6_BIT_48);

    private APCO25PatchGroup mSupergroupAddress;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    public LCMotorolaGroupRegroupVoiceChannelUser(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA GROUP REGROUP VOICE CHANNEL USER FM:").append(getSourceAddress());
        sb.append(" TO:").append(getSupergroupAddress());
        sb.append(" ").append(getServiceOptions());

        return sb.toString();
    }

    /**
     * Supergroup (ie Patch group) address
     */
    public APCO25PatchGroup getSupergroupAddress()
    {
        if(mSupergroupAddress == null)
        {
            PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(getInt(SUPERGROUP_ADDRESS)));
            mSupergroupAddress = APCO25PatchGroup.create(patchGroup);
        }

        return mSupergroupAddress;
    }

    /**
     * Source address
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSupergroupAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
