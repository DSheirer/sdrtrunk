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

package io.github.dsheirer.module.decode.nxdn.layer3.broadcast;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import java.util.Collections;
import java.util.List;

/**
 * Control Channel information for a site
 */
public class ControlChannelInformation extends NXDNLayer3Message
{
    private static final int LOCATION_ID = OCTET_1;
    private LocationID mLocationID;
    private static final int CURRENT = OCTET_4;
    private static final int NEW = OCTET_4 + 1;
    private static final int ADD = OCTET_4 + 2;
    private static final int DELETE = OCTET_4 + 3;
    private static final IntField CONTROL_CHANNEL_1 = IntField.length10(OCTET_4 + 6);
    private static final IntField CONTROL_CHANNEL_2 = IntField.length10(OCTET_6 + 6);

    /**
     * Constructs an instance
     * @param message content
     * @param timestamp of the message
     */
    public ControlChannelInformation(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.BROADCAST_SERVICE_INFORMATION;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CONTROL CHANNEL INFO ");
        sb.append(getLocationID());
        return sb.toString();
    }

    /**
     * Location ID field
     */
    public LocationID getLocationID()
    {
        if(mLocationID == null)
        {
            mLocationID = new LocationID(getMessage(), LOCATION_ID);
        }

        return mLocationID;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
