/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CauseVD;
import io.github.dsheirer.module.decode.nxdn.layer3.type.SystemID;
import java.util.List;

/**
 * Voice call response (VCALL_RESP) or Type-D call response (CALL_RESP).
 *
 * Note: the Type-D call response (NXDN TS 1-E, Figure 6.4-21) has no cause or cipher fields.  Instead, octets 7-9
 * optionally carry a System ID Option and System ID when the CC Option indicates a multi trunked system.
 */
public class VoiceCallResponse extends VoiceCallWithOptionalLocation
{
    private static final IntField CAUSE_VD = IntField.length8(OCTET_7);
    private static final int LOCATION_ID_OFFSET = OCTET_8;
    private static final int TYPE_D_MASK_MULTI_TRUNKED_SYSTEM = 0x10; //CC Option T-Type flag
    private static final IntField TYPE_D_SYSTEM_ID_RELATED_UNIT = IntField.length2(OCTET_7);
    private static final int TYPE_D_SYSTEM_ID_OFFSET = OCTET_7 + 5;
    private static final int TYPE_D_RELATED_UNIT_SOURCE = 2;
    private static final int TYPE_D_RELATED_UNIT_DESTINATION = 1;
    private SystemID mTypeDSystemID;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public VoiceCallResponse(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    protected int getLocationOffset()
    {
        return LOCATION_ID_OFFSET;
    }

    @Override
    public String toString()
    {
        if(isTypeD())
        {
            return toStringTypeD();
        }

        StringBuilder sb = getMessageBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        if(isTypeD())
        {
            sb.append("TYPE-D ");
        }

        CauseVD cause = getCause();

        if(cause == CauseVD.ACCEPTED_NORMAL || cause == CauseVD.UNKNOWN)
        {
            sb.append(getCallType()).append(" CALL RESPONSE");
        }
        else
        {
            sb.append(getCallType()).append(" CALL RESPONSE FAIL:").append(cause);
        }
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getEncryptionKeyIdentifier());
        sb.append(" ").append(getCallOption());

        return sb.toString();
    }

    /**
     * Type-D call response formatted as a string.
     */
    private String toStringTypeD()
    {
        StringBuilder sb = getMessageBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY ");
        }

        sb.append("TYPE-D ").append(getCallType()).append(" CALL RESPONSE");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());

        if(hasTypeDSystemID())
        {
            sb.append(" MULTI-SYSTEM ").append(getTypeDSystemID());

            if(isTypeDSystemIDForSource())
            {
                sb.append(" (SOURCE)");
            }
            else if(isTypeDSystemIDForDestination())
            {
                sb.append(" (DESTINATION)");
            }
        }

        sb.append(" FREE REPEATER:").append(getFreeRepeater());
        sb.append(" ").append(getCallOption());

        return sb.toString();
    }

    /**
     * Type-D free repeater number.
     */
    public int getFreeRepeater()
    {
        return getMessage().getInt(CALL_OPTION);
    }

    /**
     * Indicates if this Type-D call response carries the optional System ID, which is present for a multi trunked
     * system.
     */
    public boolean hasTypeDSystemID()
    {
        return isTypeD() && (getCallControlOption().value() & TYPE_D_MASK_MULTI_TRUNKED_SYSTEM) ==
                TYPE_D_MASK_MULTI_TRUNKED_SYSTEM;
    }

    /**
     * Optional Type-D System ID.
     * @return system ID or null if the message doesn't carry a system ID.
     */
    public SystemID getTypeDSystemID()
    {
        if(mTypeDSystemID == null && hasTypeDSystemID())
        {
            mTypeDSystemID = new SystemID(getMessage(), TYPE_D_SYSTEM_ID_OFFSET);
        }

        return mTypeDSystemID;
    }

    /**
     * Indicates if the Type-D System ID relates to the source unit.
     */
    public boolean isTypeDSystemIDForSource()
    {
        return hasTypeDSystemID() && getMessage().getInt(TYPE_D_SYSTEM_ID_RELATED_UNIT) == TYPE_D_RELATED_UNIT_SOURCE;
    }

    /**
     * Indicates if the Type-D System ID relates to the destination unit.
     */
    public boolean isTypeDSystemIDForDestination()
    {
        return hasTypeDSystemID() &&
                getMessage().getInt(TYPE_D_SYSTEM_ID_RELATED_UNIT) == TYPE_D_RELATED_UNIT_DESTINATION;
    }

    /**
     * Amplifying cause for the response.
     * @return cause, or UNKNOWN for a Type-D call response which doesn't carry a cause.
     */
    public CauseVD getCause()
    {
        if(isTypeD())
        {
            return CauseVD.UNKNOWN;
        }

        return CauseVD.fromValue(getMessage().getInt(CAUSE_VD));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(isTypeD())
        {
            //Type-D call response doesn't carry an encryption key
            return List.of(getSource(), getDestination());
        }

        return List.of(getSource(), getDestination(), getEncryptionKeyIdentifier());
    }
}
