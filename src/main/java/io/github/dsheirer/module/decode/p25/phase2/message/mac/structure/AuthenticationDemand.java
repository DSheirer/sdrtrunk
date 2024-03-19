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
 * Authentication demand
 */
public class AuthenticationDemand extends MacStructureMultiFragment
{
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_4_BIT_24);
    private static final IntField TARGET_SUID_WACN = IntField.length20(OCTET_7_BIT_48);
    private static final IntField TARGET_SUID_SYSTEM = IntField.length12(OCTET_9_BIT_64 + 4);
    private static final IntField TARGET_SUID_ID = IntField.length24(OCTET_11_BIT_80);
    private static final IntField RS_1 = IntField.length8(OCTET_14_BIT_104);
    private static final IntField RS_2 = IntField.length8(OCTET_15_BIT_112);
    private static final IntField RS_3 = IntField.length8(OCTET_16_BIT_120);
    private static final IntField RS_4 = IntField.length8(OCTET_17_BIT_128);
    private static final IntField RS_5 = IntField.length8(OCTET_18_BIT_136);
    private static final IntField FRAGMENT_0_RS_6 = IntField.length8(OCTET_3_BIT_16);
    private static final IntField FRAGMENT_0_RS_7 = IntField.length8(OCTET_4_BIT_24);
    private static final IntField FRAGMENT_0_RS_8 = IntField.length8(OCTET_5_BIT_32);
    private static final IntField FRAGMENT_0_RS_9 = IntField.length8(OCTET_6_BIT_40);
    private static final IntField FRAGMENT_0_RS_10 = IntField.length8(OCTET_7_BIT_48);
    private static final IntField FRAGMENT_0_RAND1_1 = IntField.length8(OCTET_8_BIT_56);
    private static final IntField FRAGMENT_0_RAND1_2 = IntField.length8(OCTET_9_BIT_64);
    private static final IntField FRAGMENT_0_RAND1_3 = IntField.length8(OCTET_10_BIT_72);
    private static final IntField FRAGMENT_0_RAND1_4 = IntField.length8(OCTET_11_BIT_80);
    private static final IntField FRAGMENT_0_RAND1_5 = IntField.length8(OCTET_12_BIT_88);
    private List<Identifier> mIdentifiers;
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AuthenticationDemand(CorrectedBinaryMessage message, int offset)
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
        String challenge = getChallenge();
        if(challenge != null)
        {
            sb.append(" CHALLENGE:").append(challenge);
        }
        sb.append(" RANDOM SEED:").append(getRandomSeed());
        return sb.toString();
    }

    public String getChallenge()
    {
        if(hasFragment(0))
        {
            StringBuilder sb = new StringBuilder();
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RAND1_1)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RAND1_2)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RAND1_3)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RAND1_4)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RAND1_5)));
            return sb.toString();
        }

        return null;
    }

    public String getRandomSeed()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(formatOctetAsHex(getInt(RS_1)));
        sb.append(formatOctetAsHex(getInt(RS_2)));
        sb.append(formatOctetAsHex(getInt(RS_3)));
        sb.append(formatOctetAsHex(getInt(RS_4)));
        sb.append(formatOctetAsHex(getInt(RS_5)));

        if(hasFragment(0))
        {
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RS_6)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RS_7)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RS_8)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RS_9)));
            sb.append(formatOctetAsHex(getFragment(0).getInt(FRAGMENT_0_RS_10)));
        }

        return sb.toString();
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            int address = getInt(TARGET_ADDRESS);
            int wacn = getInt(TARGET_SUID_WACN);
            int system = getInt(TARGET_SUID_SYSTEM);
            int id = getInt((TARGET_SUID_ID));

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
