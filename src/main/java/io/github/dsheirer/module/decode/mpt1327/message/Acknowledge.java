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
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327Talkgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * ACK Acknowledgement Message.
 */
public class Acknowledge extends MPT1327BaseMessage
{
    private static final int[] ACKNOWLEDGE_FUNCTION = {91, 92, 93};
    private static final int[] IDENT_FROM = {94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106};
    private static final int FUNCTION_QUALIFIER = 107;
    private static final int[] ALOHA_NUMBER = {108, 109, 110, 111};

    private MPT1327Talkgroup mFromIdentifier;
    private List<Identifier> mIdentifiers;

    public Acknowledge(CorrectedBinaryMessage message, long timestamp)
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
        sb.append(getFunctionDescription());
        sb.append(" TO:").append(getToTalkgroup());
        sb.append(" FROM:").append(getFromIdentifier());
        sb.append(" SITE:").append(getSystemIdentityCode());
        return sb.toString();
    }

    public MPT1327Talkgroup getFromIdentifier()
    {
        if(mFromIdentifier == null)
        {
            mFromIdentifier = MPT1327Talkgroup.createFrom(getPrefix(), getFromIdent());
        }

        return mFromIdentifier;
    }

    /**
     * Ident value for the from talkgroup
     */
    public int getFromIdent()
    {
        return getMessage().getInt(IDENT_FROM);
    }

    /**
     * Aloha number.
     */
    public int getAlohaNumber()
    {
        return getMessage().getInt(ALOHA_NUMBER);
    }

    /**
     * Indicates the type of aloha for this message
     */
    public AcknowledgeFunction getAcknowledgeFunction()
    {
        return getAcknowledgeFunction(getMessage());
    }

    /**
     * Status lookup of the acknowledge function for a given message.
     */
    public static AcknowledgeFunction getAcknowledgeFunction(CorrectedBinaryMessage message)
    {
        return AcknowledgeFunction.fromValue(message.getInt(ACKNOWLEDGE_FUNCTION));
    }

    /**
     * Function qualifier flag.  When unqualified (ie true) it indicates that additional data blocks will
     * follow the address block.
     */
    public boolean isUnQualifiedFunction()
    {
        return isUnQualifiedFunction(getMessage());
    }

    /**
     * Function qualifier flag.  When unqualified (ie true) it indicates that additional data blocks will
     * follow the address block.
     */
    public static boolean isUnQualifiedFunction(CorrectedBinaryMessage message)
    {
        return !message.get(FUNCTION_QUALIFIER);
    }

    private String getFunctionDescription()
    {
        switch(getAcknowledgeFunction())
        {
            case ACK_ACKNOWLEDGE:
            default:
                return "ACKNOWLEDGE (ACK)";
            case ACKB_ACKNOWLEDGE_CALL_BACK:
                return "ACKNOWLEDGE - CALL BACK (ACKB)";
            case ACKE_ACKNOWLEDGE_EMERGENCY_CALL:
                return "ACKNOWLEDGE EMERGENCY CALL (ACKE)";
            case ACKI_INTERIM_ACKNOWLEDGE_MORE_TO_FOLLOW:
                return "ACKNOWLEDGE - MORE TO FOLLOW (ACKI)";
            case ACKQ_ACKNOWLEDGE_CALL_QUEUED:
                return "ACKNOWLEDGE - CALL QUEUED (ACKQ)";
            case ACKT_ACKNOWLEDGE_TRY_ON_GIVEN_ADDRESS:
                return "ACKNOWLEDGE - TRY ON GIVEN ADDRESS (ACKT)";
            case ACKV_ACKNOWLEDGE_CALLED_UNIT_UNAVAILABLE:
                return "ACKNOWLEDGE - CALLED UNIT UNAVAILABLE (ACKV)";
            case ACKX_ACKNOWLEDGE_MESSAGE_REJECTED:
                return "ACKNOWLEDGE - MESSAGE REJECTED (ACKX)";
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode());
            mIdentifiers.add(getFromIdentifier());
            mIdentifiers.add(getToTalkgroup());
        }

        return mIdentifiers;
    }

    public enum AcknowledgeFunction
    {
        ACK_ACKNOWLEDGE,
        ACKI_INTERIM_ACKNOWLEDGE_MORE_TO_FOLLOW,
        ACKQ_ACKNOWLEDGE_CALL_QUEUED,
        ACKX_ACKNOWLEDGE_MESSAGE_REJECTED,
        ACKV_ACKNOWLEDGE_CALLED_UNIT_UNAVAILABLE,
        ACKE_ACKNOWLEDGE_EMERGENCY_CALL,
        ACKT_ACKNOWLEDGE_TRY_ON_GIVEN_ADDRESS,
        ACKB_ACKNOWLEDGE_CALL_BACK;

        public static AcknowledgeFunction fromValue(int value)
        {
            switch(value)
            {
                case 0:
                default:
                    return ACK_ACKNOWLEDGE;
                case 1:
                    return ACKI_INTERIM_ACKNOWLEDGE_MORE_TO_FOLLOW;
                case 2:
                    return ACKQ_ACKNOWLEDGE_CALL_QUEUED;
                case 3:
                    return ACKX_ACKNOWLEDGE_MESSAGE_REJECTED;
                case 4:
                    return ACKV_ACKNOWLEDGE_CALLED_UNIT_UNAVAILABLE;
                case 5:
                    return ACKE_ACKNOWLEDGE_EMERGENCY_CALL;
                case 6:
                    return ACKT_ACKNOWLEDGE_TRY_ON_GIVEN_ADDRESS;
                case 7:
                    return ACKB_ACKNOWLEDGE_CALL_BACK;
            }
        }
    }
}
