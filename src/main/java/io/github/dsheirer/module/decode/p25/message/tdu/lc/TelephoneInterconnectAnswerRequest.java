/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.reference.Digit;

import java.util.ArrayList;
import java.util.List;

public class TelephoneInterconnectAnswerRequest extends TDULinkControlMessage
{
    public static final int[] DIGIT_1 = {72, 73, 74, 75};
    public static final int[] DIGIT_2 = {88, 89, 90, 91};
    public static final int[] DIGIT_3 = {92, 93, 94, 95};
    public static final int[] DIGIT_4 = {96, 97, 98, 99};
    public static final int[] DIGIT_5 = {112, 113, 114, 115};
    public static final int[] DIGIT_6 = {116, 117, 118, 119};
    public static final int[] DIGIT_7 = {120, 121, 122, 123};
    public static final int[] DIGIT_8 = {136, 137, 138, 139};
    public static final int[] DIGIT_9 = {140, 141, 142, 143};
    public static final int[] DIGIT_10 = {144, 145, 146, 147};
    public static final int[] TARGET_ADDRESS = {160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195};

    private IIdentifier mTargetAddress;

    public TelephoneInterconnectAnswerRequest(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" TO:");
        sb.append(getTargetAddress());

        sb.append(" TEL:");
        sb.append(getTelephoneNumber());

        return sb.toString();
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }


    public String getTelephoneNumber()
    {
        List<Integer> digits = new ArrayList<Integer>();

        digits.add(mMessage.getInt(DIGIT_1));
        digits.add(mMessage.getInt(DIGIT_2));
        digits.add(mMessage.getInt(DIGIT_3));
        digits.add(mMessage.getInt(DIGIT_4));
        digits.add(mMessage.getInt(DIGIT_5));
        digits.add(mMessage.getInt(DIGIT_6));
        digits.add(mMessage.getInt(DIGIT_7));
        digits.add(mMessage.getInt(DIGIT_8));
        digits.add(mMessage.getInt(DIGIT_9));
        digits.add(mMessage.getInt(DIGIT_10));

        return Digit.decode(digits);
    }

}
