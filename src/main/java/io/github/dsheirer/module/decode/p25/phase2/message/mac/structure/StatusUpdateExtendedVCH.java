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
 * Status update extended VCH
 */
public class StatusUpdateExtendedVCH extends StatusUpdate
{
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_8_BIT_56);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_10_BIT_72 + 4);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_12_BIT_88);

    private List<Identifier> mIdentifiers;
    private APCO25FullyQualifiedRadioIdentifier mSourceSUID;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public StatusUpdateExtendedVCH(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" FM:").append(getSourceSUID());
        sb.append(" UNIT:").append(getUnitStatus());
        sb.append(" USER:").append(getUserStatus());
        return sb.toString();
    }

    /**
     * From Radio Unit
     */
    public APCO25FullyQualifiedRadioIdentifier getSourceSUID()
    {
        if(mSourceSUID == null)
        {
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int id = getInt(SOURCE_SUID_ID);
            //Fully qualified, but not aliased - reuse the ID as the address.
            mSourceSUID = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mSourceSUID;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceSUID());
            mIdentifiers.add(getUnitStatus());
            mIdentifiers.add(getUserStatus());
        }

        return mIdentifiers;
    }
}
