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
package io.github.dsheirer.module.decode.p25.message.pdu.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Response;

public class UnitRegistrationResponseExtended extends PDUMessage
{
    public static final int[] ASSIGNED_SOURCE_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102,
        103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] WACN = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
        160, 161, 162, 163};
    public static final int[] SYSTEM_ID = {164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175};
    public static final int[] SOURCE_ID = {176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190,
        191, 192, 193, 194, 195, 196, 197, 198, 199};
    public static final int[] SOURCE_ADDRESS = {200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213,
        214, 215, 216, 217, 218, 219, 220, 221, 222, 223};
    public static final int[] UNIT_REGISTRATION_RESPONSE_VALUE = {230, 231};
    public static final int[] MULTIPLE_BLOCK_CRC = {320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332,
        333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351};

    private IIdentifier mAssignedSourceAddress;
    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IIdentifier mSourceId;
    private IIdentifier mSourceAddress;

    public UnitRegistrationResponseExtended(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);

        /* Header block is already error detected/corrected - perform error
         * detection correction on the intermediate and final data blocks */
        mMessage = CRCP25.correctPDU1(mMessage);
        mCRC[1] = mMessage.getCRC();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append("REGISTRATION:");
        sb.append(getResponse().name());

        sb.append(" SRC ID:");
        sb.append(getSourceID());

        sb.append(" ADDR:");
        sb.append(getSourceAddress());

        sb.append(" WACN:");
        sb.append(getWACN());

        sb.append(" SYS:");
        sb.append(getSystemID());

        return sb.toString();
    }

    public IIdentifier getAssignedSourceAddress()
    {
        if(mAssignedSourceAddress == null)
        {
            mAssignedSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(ASSIGNED_SOURCE_ADDRESS));
        }

        return mAssignedSourceAddress;
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

    public IIdentifier getSourceID()
    {
        if(mSourceId == null)
        {
            mSourceId = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ID));
        }

        return mSourceId;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    public Response getResponse()
    {
        return Response.fromValue(mMessage.getInt(UNIT_REGISTRATION_RESPONSE_VALUE));
    }
}
