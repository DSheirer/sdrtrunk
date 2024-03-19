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
import io.github.dsheirer.module.decode.p25.reference.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit registration response extended
 */
public class UnitRegistrationResponseExtended extends MacStructure
{
    private static final IntField RESPONSE = IntField.range(22, 23);
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_4_BIT_24);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_6_BIT_40 + 4);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_8_BIT_56);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_11_BIT_80);
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public UnitRegistrationResponseExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" REGISTRATION ").append(getResponse());

        return sb.toString();
    }

    public Response getResponse()
    {
        return Response.fromValue(getInt(RESPONSE));
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            int address = getInt(SOURCE_ADDRESS);
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int id = getInt(SOURCE_SUID_ID);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(address, wacn, system, id);
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
