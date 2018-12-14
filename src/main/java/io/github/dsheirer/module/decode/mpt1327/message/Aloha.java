/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.module.decode.mpt1327.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.mpt1327.MPTMessageType;
import io.github.dsheirer.module.decode.mpt1327.channel.MPT1327Channel;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327Talkgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * ALH Aloha (Invitation) Message.
 */
public class Aloha extends MPT1327BaseMessage
{
    private static final int[] ALOHA_TYPE = {91,92,93};
    private static final int[] PARTIAL_CHANNEL_NUMBER = {95, 96, 97, 98};

    private List<Identifier> mIdentifiers;

    public Aloha(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public MPTMessageType getMessageType()
    {
        return MPTMessageType.ALH_ALOHA;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ALOHA TO:").append(getToTalkgroup());
        sb.append(" SITE:").append(getSystemIdentityCode());
        sb.append(" ACCEPTING:").append(getDescription());
        return sb.toString();
    }

    /**
     * Partial channel number for the control channel, containing the least significant 4 bits only.
     */
    public int getPartialChannelNumber()
    {
        return getMessage().getInt(PARTIAL_CHANNEL_NUMBER);
    }

    /**
     * Indicates the type of aloha for this message
     */
    public AlohaType getAlohaType()
    {
        return AlohaType.fromValue(getMessage().getInt(ALOHA_TYPE));
    }

    private String getDescription()
    {
        switch(getAlohaType())
        {
            case ALH_SINGLE_CODEWORD_MESSAGES_INVITED:
                return "ALL MESSAGES";
            case ALHD_MESSAGES_INVITED_EXCEPT_RQS:
                return "ALL MESSAGES EXCEPT SIMPLE REQUESTS (RQS)";
            case ALHE_EMERGENCY_REQUESTS_ONLY:
                return "EMERGENCY REQUESTS ONLY (RQE)";
            case ALHF_FALL_BACK_MODE:
                return "FALLBACK MODE";
            case ALHR_REGISTRATION_OR_EMERGENCY_REQUESTS_ONLY:
                return "REGISTRATION OR EMERGENCY REQUESTS ONLY";
            case ALHS_MESSAGES_INVITED_EXCEPT_RQD:
                return "ALL MESSAGES EXCEPT STATUS (RQD)";
            case ALHX_MESSAGES_INVITED_EXCEPT_RQR:
                return "ALL MESSAGES EXCEPT REGISTRATION (RQR)";
            case RESERVED:
            default:
                return "RESERVED MODE";
        }
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode());
            mIdentifiers.add(getToTalkgroup());
        }

        return mIdentifiers;
    }

    public enum AlohaType
    {
        ALH_SINGLE_CODEWORD_MESSAGES_INVITED,
        ALHD_MESSAGES_INVITED_EXCEPT_RQS,
        ALHE_EMERGENCY_REQUESTS_ONLY,
        ALHF_FALL_BACK_MODE,
        ALHR_REGISTRATION_OR_EMERGENCY_REQUESTS_ONLY,
        ALHS_MESSAGES_INVITED_EXCEPT_RQD,
        ALHX_MESSAGES_INVITED_EXCEPT_RQR,
        RESERVED;

        public static AlohaType fromValue(int value)
        {
            switch(value)
            {
                case 0:
                    return ALH_SINGLE_CODEWORD_MESSAGES_INVITED;
                case 1:
                    return ALHS_MESSAGES_INVITED_EXCEPT_RQD;
                case 2:
                    return ALHD_MESSAGES_INVITED_EXCEPT_RQS;
                case 3:
                    return ALHE_EMERGENCY_REQUESTS_ONLY;
                case 5:
                    return ALHX_MESSAGES_INVITED_EXCEPT_RQR;
                case 6:
                    return ALHF_FALL_BACK_MODE;
                case 7:
                default:
                    return RESERVED;
            }
        }
    }
}
