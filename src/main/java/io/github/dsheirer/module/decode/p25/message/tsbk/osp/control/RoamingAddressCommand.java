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
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.StackOperation;

public class RoamingAddressCommand extends TSBKMessage
{
    public static final int[] STACK_OPERATION = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int[] WACN = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107};
    public static final int[] SYSTEM_ID = {108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] TARGET_ID = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IIdentifier mTargetId;

    public RoamingAddressCommand(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(getStackOperation().name());
        sb.append(" WACN:" + getWACN());
        sb.append(" SYSTEM: " + getSystemID());
        sb.append(" TGT ID: " + getTargetID());

        return sb.toString();
    }

    public StackOperation getStackOperation()
    {
        return StackOperation.fromValue(mMessage.getInt(STACK_OPERATION));
    }

    public IIdentifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(mMessage.getInt(WACN));
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
        if(mTargetId == null)
        {
            mTargetId = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ID));
        }

        return mTargetId;
    }
}
