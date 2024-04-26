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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication FNE response abbreviated
 */
public class AuthenticationFNEResponseAbbreviated extends MacStructure
{
    private static final IntField RES2_1 = IntField.length8(OCTET_3_BIT_16);
    private static final IntField RES2_2 = IntField.length8(OCTET_4_BIT_24);
    private static final IntField RES2_3 = IntField.length8(OCTET_5_BIT_32);
    private static final IntField RES2_4 = IntField.length8(OCTET_6_BIT_40);
    private static final IntField TARGET_ID = IntField.length24(OCTET_7_BIT_48);
    private List<Identifier> mIdentifiers;
    private Identifier mTargetId;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AuthenticationFNEResponseAbbreviated(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TO:").append(getTargetId());
        sb.append(" RESPONSE:").append(getResponse());
        return sb.toString();
    }

    public String getResponse()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(formatOctetAsHex(getInt(RES2_1)));
        sb.append(formatOctetAsHex(getInt(RES2_2)));
        sb.append(formatOctetAsHex(getInt(RES2_3)));
        sb.append(formatOctetAsHex(getInt(RES2_4)));
        return sb.toString();
    }

    public Identifier getTargetId()
    {
        if(mTargetId == null)
        {
            mTargetId = APCO25RadioIdentifier.createTo(getInt(TARGET_ID));
        }

        return mTargetId;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetId());
        }

        return mIdentifiers;
    }
}
