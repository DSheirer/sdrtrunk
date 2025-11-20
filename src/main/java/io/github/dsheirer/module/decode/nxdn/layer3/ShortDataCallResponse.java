/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CauseSS;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDPartial;
import java.util.List;

/**
 * Short data call request header for simultaneous data call request (FACCH1)
 */
public class ShortDataCallResponse extends DataCall
{
    private static final IntField LOCATION_ID_OPTION = IntField.length5(OCTET_10);
    private static final int OFFSET_LOCATION_ID_PARTIAL = OCTET_10 + 5;
    private static final IntField CAUSE = IntField.length8(OCTET_7);

    private LocationIDPartial mLocationIDPartial;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public ShortDataCallResponse(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_SHORT_DATA_CALL_RESPONSE;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SHORT DATA CALL RESPONSE:").append(getCause());
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getCallOption());

        if(getCallControlOption().hasLocationId())
        {
            sb.append(" ").append(getLocationIDOption()).append(" LOCATION:").append(getLocationIDPartial());
        }

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

    /**
     * Option that qualifies the location ID partial field.
     */
    public LocationIDOption getLocationIDOption()
    {
        return LocationIDOption.fromValue(getMessage().getInt(LOCATION_ID_OPTION));
    }

    /**
     * Location ID field.
     */
    public LocationIDPartial getLocationIDPartial()
    {
        if(mLocationIDPartial == null)
        {
            mLocationIDPartial = new LocationIDPartial(getMessage(), OFFSET_LOCATION_ID_PARTIAL);
        }

        return mLocationIDPartial;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination(), getEncryptionKeyIdentifier());
    }
}
