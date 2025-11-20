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
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import java.util.List;

/**
 * Trunking controller request to remotely control an SU radio, identified by ESN
 */
public class RemoteControlRequestWithESN extends RemoteControl
{
    private static final IntField AUTHENTICATION_PARAMETER = IntField.length8(OCTET_3);
    private static final LongField AUTHENTICATION_VALUE = LongField.range(OCTET_7, OCTET_14 - 1);
    private static final int LOCATION_ID_OFFSET = OCTET_14;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public RemoteControlRequestWithESN(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
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
        StringBuilder sb = getMessageBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        sb.append("REMOTE CONTROL REQUEST WITH ESN ").append(getControlCommand());
        sb.append(" TO:").append(getDestination());
        sb.append(" AUTHENTICATION PARAMETER:").append(getAuthenticationParameter());
        sb.append(" VALUE:").append(getAuthenticationValue());
        return sb.toString();
    }

    /**
     * Authentication parameter
     */
    public String getAuthenticationParameter()
    {
        return getMessage().getHex(AUTHENTICATION_PARAMETER);
    }

    /**
     * Authentication value.
     */
    public String getAuthenticationValue()
    {
        return getMessage().getHex(AUTHENTICATION_VALUE);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getDestination());
    }
}
