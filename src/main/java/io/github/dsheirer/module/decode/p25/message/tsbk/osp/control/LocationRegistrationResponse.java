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
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Response;

public class LocationRegistrationResponse extends TSBKMessage
{
    public static final int[] REGISTRATION_RESPONSE = {86, 87};
    public static final int[] GROUP_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] RFSS_ID = {104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SITE_ID = {112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] TARGET_ADDRESS = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IIdentifier mGroupAddress;
    private IIdentifier mTargetAddress;
    private IIdentifier mRFSS;
    private IIdentifier mSite;

    public LocationRegistrationResponse(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" REGISTRATION:" + getResponse().name());
        sb.append(" GROUP:" + getGroupAddress());
        sb.append(" RFSS: " + getRFSSID());
        sb.append(" SITE: " + getSiteID());
        sb.append(" TGT ADDR: " + getTargetAddress());

        return sb.toString();
    }

    public Response getResponse()
    {
        return Response.fromValue(mMessage.getInt(REGISTRATION_RESPONSE));
    }

    public IIdentifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25ToTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    public IIdentifier getRFSSID()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(mMessage.getInt(RFSS_ID));
        }

        return mRFSS;
    }

    public IIdentifier getSiteID()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(mMessage.getInt(SITE_ID));
        }

        return mSite;
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
