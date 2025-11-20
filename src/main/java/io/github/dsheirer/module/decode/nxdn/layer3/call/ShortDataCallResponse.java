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
import io.github.dsheirer.module.decode.nxdn.layer3.type.CauseSS;
import java.util.List;

/**
 * Short data call request header for simultaneous data call request (FACCH1)
 */
public class ShortDataCallResponse extends DataCallWithOptionalLocation
{
    private static final int LOCATION_ID_OFFSET = OCTET_10;
    private static final IntField CAUSE = IntField.length8(OCTET_7);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public ShortDataCallResponse(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
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
        sb.append("SHORT DATA CALL RESPONSE:").append(getCause());
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getCallOption());

        if(getEncryptionKeyIdentifier().isEncrypted())
        {
            sb.append(" ").append(getEncryptionKeyIdentifier());
        }

        return sb.toString();
    }

    /**
     * Response cause.
     */
    public CauseSS getCause()
    {
        return CauseSS.fromValue(getMessage().getInt(CAUSE));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination(), getEncryptionKeyIdentifier());
    }
}
