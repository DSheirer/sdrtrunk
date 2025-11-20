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
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNFullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Status;
import io.github.dsheirer.module.decode.nxdn.layer3.type.StatusCallOption;
import java.util.List;

/**
 * SU Radio reports current status.
 */
public class StatusRequest extends Call
{
    private static final IntField STATUS = IntField.length8(OCTET_8);
    private static final IntField LOCATION_ID_OPTION = IntField.length5(OCTET_9);
    private static final int OFFSET_LOCATION_ID = OCTET_9 + 5;
    private CallOption mCallOption;
    private LocationID mLocationID;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     */
    public StatusRequest(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type)
    {
        super(message, timestamp, type);
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

        sb.append(getCallType()).append(" STATUS:").append(getStatus()).append(":").append(getStatusValue());
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getCallOption());
        return sb.toString();
    }

    @Override
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null && getCallControlOption().hasLocationId() && getLocationIDOption().isSource())
        {
            mSourceIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                    getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return super.getSource();
    }

    @Override
    public IntegerIdentifier getDestination()
    {
        if(mDestinationIdentifier == null && getCallControlOption().hasLocationId() && getLocationIDOption().isDestination())
        {
            mDestinationIdentifier = NXDNFullyQualifiedRadioIdentifier.createTo(getLocationID().getSystem().getValue(),
                    getMessage().getInt(IDENTIFIER_OCTET_5));
        }

        return super.getDestination();
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
     * Status value.
     */
    public int getStatusValue()
    {
        return getMessage().getInt(STATUS);
    }

    /**
     * Status
     */
    public Status getStatus()
    {
        return Status.fromValue(getStatusValue());
    }

    /**
     * Location ID Option
     */
    public LocationIDOption getLocationIDOption()
    {
        return LocationIDOption.fromValue(getMessage().getInt(LOCATION_ID_OPTION));
    }

    /**
     * Location ID Partial
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
