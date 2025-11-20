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
import io.github.dsheirer.module.decode.nxdn.layer3.type.SubscriberType;
import java.util.List;

/**
 * Registration request
 */
public class RegistrationRequest extends Registration
{
    private static final IntField SUBSCRIBER_TYPE = IntField.length16(OCTET_8);
    private static final IntField VERSION = IntField.length8(OCTET_10);
    private SubscriberType mSubscriberType;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type
     * @param ran value
     * @param lich info
     */
    public RegistrationRequest(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        if(getRegistrationOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }
        sb.append("REGISTRATION REQUEST FROM RADIO:").append(getRadio());
        sb.append(" TALKGROUP:").append(getGroup());
        if(getRegistrationOption().isPriorityStation())
        {
            sb.append(" PRIORITY STATION");
        }

        sb.append(" ").append(getSubscriberType());
        sb.append(" NXDN VERSION:").append(getVersion());
        return sb.toString();
    }

    /**
     * Subscriber type
     */
    public SubscriberType getSubscriberType()
    {
        if(mSubscriberType == null)
        {
            mSubscriberType = new SubscriberType(getMessage().getInt(SUBSCRIBER_TYPE));
        }

        return mSubscriberType;
    }

    /**
     * NXDN protocol version
     */
    public int getVersion()
    {
        return getMessage().getInt(VERSION);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getRadio(), getGroup());
    }
}
