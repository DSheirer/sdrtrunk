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

package io.github.dsheirer.module.decode.nxdn.layer3.broadcast;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.RestrictionInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ServiceInfo;
import java.util.Collections;
import java.util.List;

/**
 * Service information
 */
public class ServiceInformation extends NXDNLayer3Message
{
    private static final int LOCATION_ID = OCTET_1;
    private static final int SERVICE_INFORMATION = OCTET_4;
    private static final int RESTRICTION_INFORMATION = OCTET_6;
    private LocationID mLocationID;
    private ServiceInfo mServiceInfo;
    private RestrictionInformation mRestrictionInformation;

    /**
     * Constructs an instance
     * @param message content
     * @param timestamp of the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public ServiceInformation(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
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
    public ServiceInfo getServiceInformation()
    {
        if(mServiceInfo == null)
        {
            mServiceInfo = new ServiceInfo(getMessage(), SERVICE_INFORMATION);
        }

        return mServiceInfo;
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
