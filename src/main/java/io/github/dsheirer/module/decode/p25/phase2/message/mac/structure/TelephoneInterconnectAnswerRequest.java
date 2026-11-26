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
import io.github.dsheirer.module.decode.p25.identifier.telephone.APCO25TelephoneNumber;
import io.github.dsheirer.module.decode.p25.reference.Digit;
import java.util.ArrayList;
import java.util.List;

/**
 * Telephone interconnect answer request
 */
public class TelephoneInterconnectAnswerRequest extends MacStructure
{
    private static final IntField DIGIT_1 = IntField.length4(OCTET_2_BIT_8);
    private static final IntField DIGIT_2 = IntField.length4(OCTET_2_BIT_8 + 4);
    private static final IntField DIGIT_3 = IntField.length4(OCTET_3_BIT_16);
    private static final IntField DIGIT_4 = IntField.length4(OCTET_3_BIT_16 + 4);
    private static final IntField DIGIT_5 = IntField.length4(OCTET_4_BIT_24);
    private static final IntField DIGIT_6 = IntField.length4(OCTET_4_BIT_24 + 4);
    private static final IntField DIGIT_7 = IntField.length4(OCTET_5_BIT_32);
    private static final IntField DIGIT_8 = IntField.length4(OCTET_5_BIT_32 + 4);
    private static final IntField DIGIT_9 = IntField.length4(OCTET_6_BIT_40);
    private static final IntField DIGIT_10 = IntField.length4(OCTET_6_BIT_40 + 4);
    private static final IntField TARGET_ADDRESS = IntField.length4(OCTET_7_BIT_48);

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private Identifier mTelephoneNumber;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public TelephoneInterconnectAnswerRequest(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TELEPHONE:").append(getTelephoneNumber());
        return sb.toString();
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * Telephone number
     */
    public Identifier getTelephoneNumber()
    {
        if(mTelephoneNumber == null)
        {
            List<Integer> digits = new ArrayList<>();
            digits.add(getInt(DIGIT_1));
            digits.add(getInt(DIGIT_2));
            digits.add(getInt(DIGIT_3));
            digits.add(getInt(DIGIT_4));
            digits.add(getInt(DIGIT_5));
            digits.add(getInt(DIGIT_6));
            digits.add(getInt(DIGIT_7));
            digits.add(getInt(DIGIT_8));
            digits.add(getInt(DIGIT_9));
            digits.add(getInt(DIGIT_10));
            mTelephoneNumber = APCO25TelephoneNumber.createAny(Digit.decode(digits));
        }

        return mTelephoneNumber;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getTelephoneNumber());
        }

        return mIdentifiers;
    }
}
