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
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.DenyReason;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

public class DenyResponse extends TSBKMessage
{
    public static final int ADDITIONAL_INFORMATION_FLAG = 80;
    public static final int[] SERVICE_TYPE = {82, 83, 84, 85, 86, 87};
    public static final int[] REASON_CODE = {88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] CALL_OPTIONS = {96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] SOURCE_GROUP = {104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SOURCE_ADDRESS = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
        111};
    public static final int[] TARGET_ADDRESS = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133,
        134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IIdentifier mSourceAddress;
    private IIdentifier mTargetAddress;

    public DenyResponse(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" TGT ADDR: " + getTargetAddress());

        sb.append(" REASON:" + getReason().name());

        return sb.toString();
    }

    public DenyReason getReason()
    {
        int code = mMessage.getInt(REASON_CODE);

        return DenyReason.fromCode(code);
    }

    public boolean hasAdditionalInformation()
    {
        return mMessage.get(ADDITIONAL_INFORMATION_FLAG);
    }

    public Opcode getServiceType()
    {
        return Opcode.fromValue(mMessage.getInt(SERVICE_TYPE));
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            switch(getServiceType())
            {
                case GROUP_DATA_CHANNEL_GRANT:
                case GROUP_VOICE_CHANNEL_GRANT:
                    mSourceAddress = APCO25FromTalkgroup.createGroup(mMessage.getInt(SOURCE_GROUP));
                    break;
                case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                case INDIVIDUAL_DATA_CHANNEL_GRANT:
                case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                    mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
                    break;
                default:
                    break;
            }
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
