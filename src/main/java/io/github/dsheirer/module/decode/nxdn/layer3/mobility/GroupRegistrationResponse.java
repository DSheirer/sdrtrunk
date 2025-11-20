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

package io.github.dsheirer.module.decode.nxdn.layer3.mobility;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CauseMM;
import java.util.List;

/**
 * Group registration request
 */
public class GroupRegistrationResponse extends GroupRegistration
{
    private static final IntField CAUSE = IntField.length8(OCTET_6);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public GroupRegistrationResponse(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        if(getGroupRegistrationOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }
        sb.append("GROUP REGISTRATION RESPONSE:").append(getCause());
        sb.append(" RADIO:").append(getRadio());
        sb.append(" TALKGROUP:").append(getGroup());
        return sb.toString();
    }


    @Override
    protected int getLocationIdOffset()
    {
        return OCTET_7 + 5;
    }

    /**
     * Response cause for the request
     */
    public CauseMM getCause()
    {
        return CauseMM.fromValue(getMessage().getInt(CAUSE));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getRadio(), getGroup());
    }
}
