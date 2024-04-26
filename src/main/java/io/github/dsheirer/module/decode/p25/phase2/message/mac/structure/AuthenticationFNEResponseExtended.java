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
 * Authentication FNE response extended
 */
public class AuthenticationFNEResponseExtended extends MacStructure
{
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_3_BIT_16);
    private static final IntField TARGET_SUID_WACN = IntField.length20(OCTET_6_BIT_40);
    private static final IntField TARGET_SUID_SYSTEM = IntField.length12(OCTET_8_BIT_56 + 4);
    private static final IntField TARGET_SUID_ID = IntField.length24(OCTET_10_BIT_72);
    private static final IntField RES2_1 = IntField.length8(OCTET_13_BIT_96);
    private static final IntField RES2_2 = IntField.length8(OCTET_14_BIT_104);
    private static final IntField RES2_3 = IntField.length8(OCTET_15_BIT_112);
    private static final IntField RES2_4 = IntField.length8(OCTET_16_BIT_120);
    private List<Identifier> mIdentifiers;
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AuthenticationFNEResponseExtended(CorrectedBinaryMessage message, int offset)
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

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            int address = getInt(TARGET_ADDRESS);
            int wacn = getInt(TARGET_SUID_WACN);
            int system = getInt(TARGET_SUID_SYSTEM);
            int id = getInt(TARGET_SUID_ID);
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
