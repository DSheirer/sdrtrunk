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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.RestrictionInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ServiceInformation;
import java.util.Collections;
import java.util.List;

/**
 * Service information
 */
public class ServiceInfo extends NXDNLayer3Message
{
    private static final int LOCATION_ID = OCTET_1;
    private static final int SERVICE_INFORMATION = OCTET_4;
    private static final int RESTRICTION_INFORMATION = OCTET_6;
    private LocationID mLocationID;
    private ServiceInformation mServiceInformation;
    private RestrictionInformation mRestrictionInformation;

    /**
     * Constructs an instance
     * @param message content
     * @param timestamp of the message
     */
    public ServiceInfo(CorrectedBinaryMessage message, long timestamp)
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
        sb.append("SERVICE INFO ");
        sb.append(getLocationID());
        sb.append(" ").append(getServiceInformation());
        sb.append(" ").append(getRestrictionInformation());
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

    /**
     * Service information field.
     */
    public io.github.dsheirer.module.decode.nxdn.layer3.type.ServiceInformation getServiceInformation()
    {
        if(mServiceInformation == null)
        {
            mServiceInformation = new io.github.dsheirer.module.decode.nxdn.layer3.type.ServiceInformation(getMessage(), SERVICE_INFORMATION);
        }

        return mServiceInformation;
    }

    /**
     * Restriction information field.
     */
    public RestrictionInformation getRestrictionInformation()
    {
        if(mRestrictionInformation == null)
        {
            mRestrictionInformation = new RestrictionInformation(getMessage(), RESTRICTION_INFORMATION);
        }

        return mRestrictionInformation;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
