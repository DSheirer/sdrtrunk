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
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;

public class UnitAuthenticationCommand extends TDULinkControlMessage
{
    public static final int[] WACN_ID = {72, 73, 74, 75, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 112, 113, 114, 115};
    public static final int[] SYSTEM_ID = {116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139};
    /* ICD says this is a source id, but this is a command messages, so this must be a target id */
    public static final int[] TARGET_ID = {140, 141, 142, 143, 144, 145, 146, 147, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187};

    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IIdentifier mTargetAddress;

    public UnitAuthenticationCommand(TDULinkControlMessage source)
    {
        super(source);
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
