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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.StatusCallOption;
import java.util.List;

/**
 * Radio status inquiry
 */
public class StatusInquiryRequest extends Call
{
    private static final IntField LOCATION_ID_OPTION = IntField.length5(OCTET_7);
    private static final int OFFSET_LOCATION_ID = OCTET_7 + 5;
    private StatusCallOption mCallOption;
    private LocationID mLocationID;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public StatusInquiryRequest(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_STATUS_INQUIRY_REQUEST;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        sb.append(getCallType()).append(" STATUS INQUIRY");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());

        if(getCallControlOption().hasLocationId())
        {
            sb.append(" ").append(getLocationIDOption()).append(" ").append(getLocationID());
        }

        sb.append(getCallOption());

        return sb.toString();
    }

    @Override
    public CallOption getCallOption()
    {
        if(mCallOption == null)
        {
            mCallOption = new StatusCallOption(getMessage().getInt(CALL_OPTION));
        }

        return mCallOption;
    }

    /**
     * Location ID option
     */
    public LocationIDOption getLocationIDOption()
    {
        return LocationIDOption.fromValue(getMessage().getInt(LOCATION_ID_OPTION));
    }

    /**
     * Location ID with network category and system code.
     */
    public LocationID getLocationID()
    {
        if(mLocationID == null)
        {
            mLocationID = new LocationID(getMessage(), OFFSET_LOCATION_ID, true);
        }

        return mLocationID;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination());
    }
}
