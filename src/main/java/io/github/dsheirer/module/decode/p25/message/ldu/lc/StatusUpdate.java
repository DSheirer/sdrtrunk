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
import io.github.dsheirer.identifier.integer.status.APCO25Status;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;

public class StatusUpdate extends LDU1Message
{
    public static final int[] USER_STATUS = {364, 365, 366, 367, 372, 373, 374, 375};
    public static final int[] UNIT_STATUS = {376, 377, 382, 383, 384, 385, 386, 387};
    public static final int[] TARGET_ADDRESS = {536, 537, 538, 539, 540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] SOURCE_ADDRESS = {720, 721, 722, 723, 724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private IIdentifier mUserStatus;
    private IIdentifier mUnitStatus;
    private IIdentifier mSourceAddress;
    private IIdentifier mTargetAddress;

    public StatusUpdate(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" STATUS USER:" + getUserStatus());
        sb.append(" UNIT:" + getUnitStatus());
        sb.append(" FROM:" + getSourceAddress());
        sb.append(" TO:" + getTargetAddress());

        return sb.toString();
    }

    public IIdentifier getUserStatus()
    {
        if(mUserStatus == null)
        {
            mUserStatus = APCO25Status.createUserStatus(mMessage.getInt(USER_STATUS));
        }

        return mUserStatus;
    }

    public IIdentifier getUnitStatus()
    {
        if(mUnitStatus == null)
        {
            mUnitStatus = APCO25Status.createUnitStatus(mMessage.getInt(UNIT_STATUS));
        }

        return mUnitStatus;
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }
}
