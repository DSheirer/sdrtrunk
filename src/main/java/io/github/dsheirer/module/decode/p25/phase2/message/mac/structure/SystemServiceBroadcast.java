/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.reference.Service;
import java.util.Collections;
import java.util.List;

/**
 * System services broadcast
 */
public class SystemServiceBroadcast extends MacStructure
{
    private static final IntField TWUID_VALIDITY = IntField.length8(OCTET_2_BIT_8);
    private static final IntField AVAILABLE_SERVICES = IntField.length24(OCTET_3_BIT_16);
    private static final IntField SUPPORTED_SERVICES = IntField.length24(OCTET_6_BIT_40);
    private static final IntField REQUEST_PRIORITY_LEVEL = IntField.length8(OCTET_9_BIT_64);
    private List<Service> mAvailableServices;
    private List<Service> mSupportedServices;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SystemServiceBroadcast(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" AVAILABLE SERVICES ").append(getAvailableServices());
        sb.append(" SUPPORTED SERVICES ").append(getSupportedServices());
        sb.append(" ").append(getTemporaryWUIDValidity());
        sb.append(" MIN REQUEST PRI SUPPORTED:").append(getMinimumSupportedRequestPriorityLevel());
        return sb.toString();
    }

    /**
     * Indicates the lowest service request priority which is processed at the site at this time.  This priority
     * is reflected in the service options priority level specified in the (radio) request.
     */
    public int getMinimumSupportedRequestPriorityLevel()
    {
        return getInt(REQUEST_PRIORITY_LEVEL);
    }

    /**
     * Registered roaming radio temporarily assigned WUID validity duration description.
     */
    public String getTemporaryWUIDValidity()
    {
        int duration = getInt(TWUID_VALIDITY);

        if(duration > 0)
        {
            int hours = 4 + (duration / 2);
            String minutes = (duration % 2 == 1) ? ":30" : ":00";
            return "ROAMING WUIDS VALID FOR:" + hours + minutes + " HOURS";
        }
        else
        {
            return "ROAMING WUIDS VALID FOR: NO EXPIRY";
        }
    }

    public List<Service> getAvailableServices()
    {
        if(mAvailableServices == null)
        {
            mAvailableServices = Service.getServices(getInt(AVAILABLE_SERVICES));
        }

        return mAvailableServices;
    }

    public List<Service> getSupportedServices()
    {
        if(mSupportedServices == null)
        {
            mSupportedServices = Service.getServices(getInt(SUPPORTED_SERVICES));
        }

        return mSupportedServices;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
