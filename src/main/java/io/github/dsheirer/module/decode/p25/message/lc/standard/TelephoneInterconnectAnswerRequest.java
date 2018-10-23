/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.lc.standard;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.identifier.string.APCO25TelephoneNumber;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.Digit;

import java.util.ArrayList;
import java.util.List;

/**
 * Request to a radio user to answer a telephone call
 */
public class TelephoneInterconnectAnswerRequest extends LinkControlWord
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

    private IIdentifier mTargetAddress;
    private IIdentifier mTelephoneNumber;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public TelephoneInterconnectAnswerRequest(BinaryMessage message)
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

    public IIdentifier getTelephoneNumber()
    {
        if(mTelephoneNumber == null)
        {
            List<Integer> digits = new ArrayList<>();
            digits.add(getMessage().getInt(DIGIT_1));
            digits.add(getMessage().getInt(DIGIT_2));
            digits.add(getMessage().getInt(DIGIT_3));
            digits.add(getMessage().getInt(DIGIT_4));
            digits.add(getMessage().getInt(DIGIT_5));
            digits.add(getMessage().getInt(DIGIT_6));
            digits.add(getMessage().getInt(DIGIT_7));
            digits.add(getMessage().getInt(DIGIT_8));
            digits.add(getMessage().getInt(DIGIT_9));
            digits.add(getMessage().getInt(DIGIT_10));

            mTelephoneNumber = APCO25TelephoneNumber.create(Digit.decode(digits));
        }

        return mTelephoneNumber;
    }

    /**
     * To/From radio identifier communicating with a landline
     */
    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<IIdentifier> getIdentifiers()
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
