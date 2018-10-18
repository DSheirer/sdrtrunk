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
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.status.APCO25Status;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.message.tsbk2.Opcode;

public class StatusUpdate extends TSBKMessage
{
    public static final int[] USER_STATUS = {72, 73, 74, 75, 88, 89, 90, 91};
    public static final int[] UNIT_STATUS = {92, 93, 94, 95, 96, 97, 98, 99};
    public static final int[] TARGET_ADDRESS = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147};
    public static final int[] SOURCE_ADDRESS = {160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187, 188, 189, 190, 190, 192, 193, 194, 195};

    private IIdentifier mUserStatus;
    private IIdentifier mUnitStatus;
    private IIdentifier mTargetAddress;
    private IIdentifier mSourceAddress;

    public StatusUpdate(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getEventType()
    {
        return Opcode.OSP_STATUS_UPDATE.toString();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" STATUS USER:" + getUserStatus());
        sb.append(" UNIT:" + getUnitStatus());
        sb.append(" SRC ADDR: " + getSourceAddress());
        sb.append(" TGT ADDR: " + getTargetAddress());

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

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }
}
