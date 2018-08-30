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
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class StatusQueryExtended extends PDUMessage
{
    public static final int[] TARGET_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SOURCE_WACN = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 160, 161, 162, 163};
    public static final int[] SOURCE_SYSTEM_ID = {164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175};
    public static final int[] SOURCE_ID = {176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199};
    public static final int[] MULTIPLE_BLOCK_CRC = {224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};

    private IIdentifier mTargetAddress;
    private IIdentifier mSourceAddress;
    private IIdentifier mWACN;
    private IIdentifier mSystem;

    public StatusQueryExtended(BinaryMessage message, DataUnitID duid, AliasList aliasList)
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

        sb.append(" TO:");
        sb.append(getTargetAddress());

        sb.append(" FROM:");
        sb.append(getSourceAddress());

        sb.append(" WACN:");
        sb.append(getSourceWACN());

        sb.append(" SYS:");
        sb.append(getSourceSystemID());

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

    public IIdentifier getSourceWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(mMessage.getInt(SOURCE_WACN));
        }

        return mWACN;
    }

    public IIdentifier getSourceSystemID()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(mMessage.getInt(SOURCE_SYSTEM_ID));
        }

        return mSystem;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ID));
        }

        return mSourceAddress;
    }
}
