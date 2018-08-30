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
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.ExtendedFunction;

public class ExtendedFunctionCommand extends LDU1Message
{
    public static final int[] EXTENDED_FUNCTION = {364, 365, 366, 367, 372, 373, 374, 375, 376, 377, 382, 383, 384, 385, 386, 387};
    public static final int[] ARGUMENT = {536, 537, 538, 539, 540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] TARGET_ADDRESS = {720, 721, 722, 723, 724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private IIdentifier mTarget;

    public ExtendedFunctionCommand(LDU1Message source)
    {
        super(source);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" EXTENDED FUNCTION:" + getExtendedFunction().getLabel());

        sb.append(" ARGUMENT: " + getArgument());

        sb.append(" TGT ADDR: " + getTargetAddress());

        return sb.toString();
    }

    public ExtendedFunction getExtendedFunction()
    {
        return ExtendedFunction.fromValue(mMessage.getInt(EXTENDED_FUNCTION));
    }

    public int getArgument()
    {
        return mMessage.getInt(ARGUMENT);
    }

    public IIdentifier getSourceAddress()
    {
        switch(getExtendedFunction())
        {
            case RADIO_CHECK:
            case RADIO_CHECK_ACK:
            case RADIO_DETACH:
            case RADIO_DETACH_ACK:
            case RADIO_INHIBIT:
            case RADIO_INHIBIT_ACK:
            case RADIO_UNINHIBIT:
            case RADIO_UNINHIBIT_ACK:
                return APCO25FromTalkgroup.createIndividual(getArgument());
            default:
                break;
        }

        return null;
    }

    public IIdentifier getTargetAddress()
    {
        if(mTarget == null)
        {
            mTarget = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTarget;
    }
}
