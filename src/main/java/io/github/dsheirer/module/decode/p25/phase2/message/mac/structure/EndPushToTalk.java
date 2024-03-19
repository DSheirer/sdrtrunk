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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Call end.
 *
 * Similar to a P25 Phase 1 Terminator Data Unit (TDU).
 */
public class EndPushToTalk extends MacStructure
{
    private static final int SYSTEM_CONTROLLER = 16777215;
    private static final IntField COLOR_CODE = IntField.length12(OCTET_2_BIT_8 + 4);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_14_BIT_104);
    private static final IntField GROUP_ADDRESS = IntField.length16(OCTET_17_BIT_128);

    private Identifier mColorCode;
    private Identifier mSourceAddress;
    private Identifier mGroupAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     */
    public EndPushToTalk(CorrectedBinaryMessage message)
    {
        super(message, 0);
    }

    @Override
    public MacOpcode getOpcode()
    {
        return MacOpcode.END_PUSH_TO_TALK;
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FM:").append(getSourceAddress());
        sb.append(" TO:").append(getGroupAddress());
        sb.append(" NAC:").append(getNAC());

        return sb.toString();
    }

    /**
     * NAC or Color Code
     */
    public Identifier getNAC()
    {
        if(mColorCode == null)
        {
            mColorCode = APCO25Nac.create(getInt(COLOR_CODE));
        }

        return mColorCode;
    }

    /**
     * Calling (source) radio identifier
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
     * Called (destination) group identifier
     */
    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.create(getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
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
            mIdentifiers.add(getSourceAddress());
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getNAC());
        }

        return mIdentifiers;
    }
}
