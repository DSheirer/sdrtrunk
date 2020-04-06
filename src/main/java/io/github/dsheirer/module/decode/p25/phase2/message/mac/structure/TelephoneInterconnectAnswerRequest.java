/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.telephone.APCO25TelephoneNumber;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.Digit;

import java.util.ArrayList;
import java.util.List;

/**
 * Telephone interconnect answer request
 */
public class TelephoneInterconnectAnswerRequest extends MacStructure
{
    private static final int[] DIGIT_1 = {8, 9, 10, 11};
    private static final int[] DIGIT_2 = {12, 13, 14, 15};
    private static final int[] DIGIT_3 = {16, 17, 18, 19};
    private static final int[] DIGIT_4 = {20, 21, 22, 23};
    private static final int[] DIGIT_5 = {24, 25, 26, 27};
    private static final int[] DIGIT_6 = {28, 29, 30, 31};
    private static final int[] DIGIT_7 = {32, 33, 34, 35};
    private static final int[] DIGIT_8 = {36, 37, 38, 39};
    private static final int[] DIGIT_9 = {40, 41, 42, 43};
    private static final int[] DIGIT_10 = {44, 45, 46, 47};
    private static final int[] TARGET_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
        65, 66, 67, 68, 69, 70, 71};

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
            mTargetAddress = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS, getOffset()));
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
            digits.add(getMessage().getInt(DIGIT_1, getOffset()));
            digits.add(getMessage().getInt(DIGIT_2, getOffset()));
            digits.add(getMessage().getInt(DIGIT_3, getOffset()));
            digits.add(getMessage().getInt(DIGIT_4, getOffset()));
            digits.add(getMessage().getInt(DIGIT_5, getOffset()));
            digits.add(getMessage().getInt(DIGIT_6, getOffset()));
            digits.add(getMessage().getInt(DIGIT_7, getOffset()));
            digits.add(getMessage().getInt(DIGIT_8, getOffset()));
            digits.add(getMessage().getInt(DIGIT_9, getOffset()));
            digits.add(getMessage().getInt(DIGIT_10, getOffset()));

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
