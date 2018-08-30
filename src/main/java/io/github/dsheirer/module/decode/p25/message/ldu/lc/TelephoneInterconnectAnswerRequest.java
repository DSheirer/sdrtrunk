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
package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.Digit;

import java.util.ArrayList;
import java.util.List;

public class TelephoneInterconnectAnswerRequest extends LDU1Message
{
    public static final int[] DIGIT_1 = {364, 365, 366, 367};
    public static final int[] DIGIT_2 = {372, 373, 374, 375};
    public static final int[] DIGIT_3 = {376, 377, 382, 383};
    public static final int[] DIGIT_4 = {384, 385, 386, 387};
    public static final int[] DIGIT_5 = {536, 537, 538, 539};
    public static final int[] DIGIT_6 = {540, 541, 546, 547};
    public static final int[] DIGIT_7 = {548, 549, 550, 551};
    public static final int[] DIGIT_8 = {556, 557, 558, 559};
    public static final int[] DIGIT_9 = {560, 561, 566, 567};
    public static final int[] DIGIT_10 = {568, 569, 570, 571};
    public static final int[] TARGET_ADDRESS = {720, 721, 722, 723, 724, 725, 730,
        731, 732, 733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private IIdentifier mTargetAddress;

    public TelephoneInterconnectAnswerRequest(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" TO:");
        sb.append(getTargetAddress());

        sb.append(" TEL#:");
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
