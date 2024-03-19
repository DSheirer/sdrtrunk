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
 * Unit-to-Unit voice channel user - extended
 */
public class UnitToUnitVoiceChannelUserExtended extends MacStructureUnitVoiceService
{
    private static final IntField SOURCE_ADDRESS = IntField.range(40, 63);
    private static final IntField SOURCE_SUID_WACN = IntField.range(64, 83);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.range(84, 95);
    private static final IntField SOURCE_SUID_ID = IntField.range(96, 119);

    private List<Identifier> mIdentifiers;
    private APCO25FullyQualifiedRadioIdentifier mSource;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public UnitToUnitVoiceChannelUserExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    /**
     * From Radio Unit using a fully qualified radio identifier and a local address (persona).
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
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSource());
        }

        return mIdentifiers;
    }
}
