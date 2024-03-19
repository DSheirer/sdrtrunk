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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.telephone.APCO25TelephoneNumber;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.Digit;
import java.util.ArrayList;
import java.util.List;

/**
 * Request to a radio user to answer a telephone call
 */
public class LCTelephoneInterconnectAnswerRequest extends LinkControlWord
{
    private static final IntField DIGIT_1 = IntField.length4(OCTET_1_BIT_8);
    private static final IntField DIGIT_2 = IntField.length4(OCTET_1_BIT_8 + 4);
    private static final IntField DIGIT_3 = IntField.length4(OCTET_2_BIT_16);
    private static final IntField DIGIT_4 = IntField.length4(OCTET_2_BIT_16 + 4);
    private static final IntField DIGIT_5 = IntField.length4(OCTET_3_BIT_24);
    private static final IntField DIGIT_6 = IntField.length4(OCTET_3_BIT_24 + 4);
    private static final IntField DIGIT_7 = IntField.length4(OCTET_4_BIT_32);
    private static final IntField DIGIT_8 = IntField.length4(OCTET_4_BIT_32 + 4);
    private static final IntField DIGIT_9 = IntField.length4(OCTET_5_BIT_40);
    private static final IntField DIGIT_10 = IntField.length4(OCTET_5_BIT_40 + 4);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_6_BIT_48);

    private Identifier mTargetAddress;
    private Identifier mTelephoneNumber;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCTelephoneInterconnectAnswerRequest(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" TELEPHONE:").append(getTelephoneNumber());

        return sb.toString();
    }

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

            mTelephoneNumber = APCO25TelephoneNumber.createFrom(Digit.decode(digits));
        }

        return mTelephoneNumber;
    }

    /**
     * To/From radio identifier communicating with a landline
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
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTelephoneNumber());
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
