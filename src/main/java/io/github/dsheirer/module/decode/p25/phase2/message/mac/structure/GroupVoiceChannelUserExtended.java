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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel user - extended format
 */
public class GroupVoiceChannelUserExtended extends MacStructureGroupVoiceService
{
    private static final IntField SOURCE_ADDRESS = IntField.range(32, 55);
    private static final IntField SOURCE_SUID_WACN = IntField.range(56, 75);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.range(76, 87);
    private static final IntField SOURCE_SUID_ID = IntField.range(88, 111);

    private List<Identifier> mIdentifiers;
    private APCO25FullyQualifiedRadioIdentifier mSource;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelUserExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" FM:").append(getSource());
        sb.append(" TO:").append(getGroupAddress());
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    /**
     * From Radio Source
     */
    public APCO25FullyQualifiedRadioIdentifier getSource()
    {
        if(mSource == null)
        {
            int address = getInt(SOURCE_ADDRESS);
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int id = getInt(SOURCE_SUID_ID);
            mSource = APCO25FullyQualifiedRadioIdentifier.createFrom(address, wacn, system, id);
        }

        return mSource;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getSource());
        }

        return mIdentifiers;
    }
}
