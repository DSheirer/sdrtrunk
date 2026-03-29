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
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNSite;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.RestrictionInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ServiceInfo;
import io.github.dsheirer.module.decode.nxdn.layer3.type.SystemID;
import java.util.Collections;
import java.util.List;

/**
 * Service information for Type-D system
 */
public class ServiceInformationTypeD extends NXDNLayer3Message
{
    private static final int SYSTEM_ID = OCTET_1 + 5;
    private static final IntField SITE_CODE = IntField.length8(OCTET_4);
    private static final IntField SERVICE_INFORMATION = IntField.length16(OCTET_5);
    private static final int RESTRICTION_INFORMATION = OCTET_7;
    private NXDNSite mSite;
    private SystemID mSystemID;
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
    public ServiceInformationTypeD(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append(getSystemID());
        sb.append(" SITE:").append(getSite());
        sb.append(" ").append(getServiceInformation());
        sb.append(" ").append(getRestrictionInformation());
        return sb.toString();
    }

    /**
     * Site information
     */
    public NXDNSite getSite()
    {
        if(mSite == null)
        {
            mSite = NXDNSite.create(getMessage().getInt(SITE_CODE));
        }

        return mSite;
    }

    /**
     * Location ID field
     */
    public SystemID getSystemID()
    {
        if(mSystemID == null)
        {
            mSystemID = new SystemID(getMessage(), SYSTEM_ID);
        }

        return mSystemID;
    }

    /**
     * Service information field.
     */
    public ServiceInfo getServiceInformation()
    {
        if(mServiceInfo == null)
        {
            mServiceInfo = new ServiceInfo(getMessage().getInt(SERVICE_INFORMATION));
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
