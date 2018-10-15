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
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;

public class UnitAuthenticationCommand extends LDU1Message
{
    public static final int[] WACN_ID = {364, 365, 366, 367, 372, 373, 374, 375, 376, 377, 382, 383, 384, 385, 386, 387,
        536, 537, 538, 539};
    public static final int[] SYSTEM_ID = {540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] TARGET_ID = {560, 561, 566, 567, 568, 569, 570, 571, 720, 721, 722, 723, 724, 725, 730,
        731, 732, 733, 734, 735, 740, 741, 742, 743};

    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IIdentifier mTargetAddress;

    public UnitAuthenticationCommand(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" ADDRESS:" + getCompleteTargetAddress());

        return sb.toString();
    }

    public String getCompleteTargetAddress()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getWACN());
        sb.append(":");
        sb.append(getSystemID());
        sb.append(":");
        sb.append(getTargetID());

        return sb.toString();
    }

    public IIdentifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(mMessage.getInt(WACN_ID));
        }

        return mWACN;
    }

    public IIdentifier getSystemID()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(mMessage.getInt(SYSTEM_ID));
        }

        return mSystem;
    }

    public IIdentifier getTargetID()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ID));
        }

        return mTargetAddress;
    }
}
