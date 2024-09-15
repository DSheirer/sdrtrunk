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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Regroup Talker Alias - 17-bytes long
 */
public class MotorolaGroupRegroupTalkerAlias extends MacStructureVendor
{
    private static final IntField TALKGROUP = IntField.length16(24);
    private static final IntField UNKNOWN = IntField.length32(40);
    private static final IntField SOURCE_SUID_WACN = IntField.length20(72);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(92);
    private static final IntField SOURCE_SUID_UNIT = IntField.length24(104);
    private List<Identifier> mIdentifiers;
    private TalkgroupIdentifier mTalkgroup;
    private APCO25FullyQualifiedRadioIdentifier mRadio;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupTalkerAlias(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA GROUP REGROUP TALKER ALIAS TALKGROUP:").append(getTalkgroup());
        sb.append(" SOURCE SUID RADIO:").append(getRadio());
        sb.append(" UNK:").append(Integer.toHexString(getInt(UNKNOWN)).toUpperCase());
        sb.append(" MSG:").append(getMessage().get(getOffset(), getMessage().length()).toHexString());
        return sb.toString();
    }

    /**
     * Talkgroup identifier
     */
    public TalkgroupIdentifier getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = APCO25Talkgroup.create(getMessage().getInt(TALKGROUP, getOffset()));
        }

        return mTalkgroup;
    }

    public APCO25FullyQualifiedRadioIdentifier getRadio()
    {
        if(mRadio == null)
        {
            int wacn = getMessage().getInt(SOURCE_SUID_WACN, getOffset());
            int system = getMessage().getInt(SOURCE_SUID_SYSTEM, getOffset());
            int unit = getMessage().getInt(SOURCE_SUID_UNIT, getOffset());
            mRadio = APCO25FullyQualifiedRadioIdentifier.createFrom(unit, wacn, system, unit);
        }

        return mRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getRadio());
        }

        return mIdentifiers;
    }
}
