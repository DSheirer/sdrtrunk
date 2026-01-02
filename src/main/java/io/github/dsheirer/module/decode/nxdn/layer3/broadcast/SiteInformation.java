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
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannel;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelLookup;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelStructure;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.RestrictionInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ServiceInfo;
import java.util.List;

/**
 * Site information
 */
public class SiteInformation extends NXDNLayer3Message
{
    private static final int LOCATION_ID = OCTET_1;
    private static final int CHANNEL_STRUCTURE = OCTET_4;
    private static final int SERVICE_INFORMATION = OCTET_6;
    private static final int RESTRICTION_INFORMATION = OCTET_8;
    private static final int CHANNEL_ACCESS_INFORMATION = OCTET_11;
    private static final IntField VERSION_NUMBER = IntField.length8(OCTET_14);
    private static final IntField ADJACENT_SITE_ALLOCATION = IntField.length4(OCTET_15);
    private static final IntField CONTROL_CHANNEL_1 = IntField.range(OCTET_15 + 4, OCTET_16 + 5);
    private static final IntField CONTROL_CHANNEL_2 = IntField.range(OCTET_16 + 6, OCTET_17 + 7);

    private LocationID mLocationID;
    private ChannelStructure mChannelStructure;
    private ServiceInfo mServiceInfo;
    private RestrictionInformation mRestrictionInformation;
    private ChannelAccessInformation mChannelAccessInformation;
    private NXDNChannel mChannel1;
    private NXDNChannel mChannel2;

    /**
     * Constructs an instance
     * @param message content
     * @param timestamp of the message
     * @param type of message
     */
    public SiteInformation(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type)
    {
        super(message, timestamp, type);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getLocationID());
        if(getChannelAccessInformation().isChannel() && hasChannel1())
        {
            sb.append(" CONTROL CHANNEL1:").append(getChannel1());

            if(hasChannel2())
            {
                sb.append(" CONTROL CHANNEL2:").append(getChannel2());
            }
        }
        else
        {
            sb.append(" USING DFA CHANNELS");
        }
        sb.append(" ADJACENT SITES:").append(getAdjacentSiteAllocation());
        sb.append(" NXDN VER:").append(getVersionNumber());
        sb.append(" ").append(getServiceInformation());
        sb.append(" ").append(getRestrictionInformation());
        return sb.toString();
    }

    /**
     * Control channel 1
     * @return channel instance or null if the site is configured for DFA
     */
    public NXDNChannel getChannel1()
    {
        if(mChannel1 == null && getChannelAccessInformation().isChannel())
        {
            mChannel1 = new NXDNChannelLookup(getMessage().getInt(CONTROL_CHANNEL_1));
        }

        return mChannel1;
    }

    /**
     * Indicates if this message has a first control channel value.
     */
    public boolean hasChannel1()
    {
        return getChannel1() != null;
    }

    /**
     * Control channel 2
     * @return channel instance or null if the site is configured for DFA
     */
    public NXDNChannel getChannel2()
    {
        if(mChannel2 == null && getChannelAccessInformation().isChannel())
        {
            mChannel2 = new NXDNChannelLookup(getMessage().getInt(CONTROL_CHANNEL_2));
        }

        return mChannel2;
    }

    /**
     * Indicates if this message has a first control channel value.
     */
    public boolean hasChannel2()
    {
        return getChannel1() != null;
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
     * Channel structure field.
     */
    public ChannelStructure getChannelStructure()
    {
        if(mChannelStructure == null)
        {
            mChannelStructure = new ChannelStructure(getMessage(), CHANNEL_STRUCTURE);
        }

        return mChannelStructure;
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

    /**
     * Channel access information field.
     */
    public ChannelAccessInformation getChannelAccessInformation()
    {
        if(mChannelAccessInformation == null)
        {
            mChannelAccessInformation = new ChannelAccessInformation(getMessage(), CHANNEL_ACCESS_INFORMATION);
        }

        return mChannelAccessInformation;
    }

    /**
     * NXDN version number.  Note: version numbers are not further defined.
     */
    public int getVersionNumber()
    {
        return getMessage().getInt(VERSION_NUMBER);
    }

    /**
     * Number of adjacent trunking sites.
     */
    public int getAdjacentSiteAllocation()
    {
        return getMessage().getInt(ADJACENT_SITE_ALLOCATION);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return getLocationID().getIdentifiers();
    }
}
