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
 * Unit registration response abbreviated
 */
public class UnitRegistrationResponseAbbreviated extends MacStructure
{
    private static final IntField RESPONSE = IntField.range(18, 19);
    private static final IntField SYSTEM = IntField.length12(OCTET_3_BIT_16 + 4);
    private static final IntField SOURCE_ID = IntField.length24(OCTET_5_BIT_32);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_8_BIT_56);
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public UnitRegistrationResponseAbbreviated(CorrectedBinaryMessage message, int offset)
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
            int wacn = 0; //wacn is not included here
            int system = getInt(SYSTEM);
            int id = getInt(SOURCE_ID);
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
