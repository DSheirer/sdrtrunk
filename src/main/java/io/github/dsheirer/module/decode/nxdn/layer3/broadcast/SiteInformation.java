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
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelStructure;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.RestrictionInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ServiceInformation;
import java.util.ArrayList;
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
    private static final IntField FIRST_CONTROL_CHANNEL = IntField.range(OCTET_15 + 4, OCTET_16 + 5);
    private static final IntField SECOND_CONTROL_CHANNEL = IntField.range(OCTET_16 + 6, OCTET_17 + 7);

    private LocationID mLocationID;
    private ChannelStructure mChannelStructure;
    private ServiceInformation mServiceInformation;
    private RestrictionInformation mRestrictionInformation;
    private ChannelAccessInformation mChannelAccessInformation;

    //TODO: finish this message parsing

    /**
     * Constructs an instance
     * @param message content
     * @param timestamp of the message
     */
    public SiteInformation(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.BROADCAST_SITE_INFORMATION;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SITE INFO ");
        sb.append(getLocationID());
        sb.append(" ADJACENT SITES:").append(getAdjacentSiteAllocation());
        sb.append(" NXDN VERSION:").append(getVersionNumber());
        if(hasFirstControlChannel())
        {
            sb.append(" CONTROL CHAN1:").append(getFirstControlChannel());

            if(hasSecondControlChannel())
            {
                sb.append(" CHAN2:").append(getSecondControlChannel());
            }
        }
        else
        {
            sb.append(" CHANNELS:DFA");
        }
        sb.append(" ").append(getServiceInformation());
        sb.append(" ").append(getRestrictionInformation());
        return sb.toString();
    }

    /**
     * Indicates if this message has a first control channel value.
     */
    public boolean hasFirstControlChannel()
    {
        return getMessage().getInt(FIRST_CONTROL_CHANNEL) > 0;
    }

    /**
     * Indicates if this message has a second control channel value.
     */
    public boolean hasSecondControlChannel()
    {
        return getMessage().getInt(SECOND_CONTROL_CHANNEL) > 0;
    }

    /**
     * First control channel frequency
     * @return frequency (Hertz)
     */
    public long getFirstControlChannel()
    {
        if(getChannelAccessInformation().isChannel())
        {
            return getChannelAccessInformation().getBaseFrequency() +
                    (getChannelAccessInformation().getStepSize() * getMessage().getInt(FIRST_CONTROL_CHANNEL));
        }

        return 0;
    }

    /**
     * Second control channel frequency
     * @return frequency (Hertz)
     */
    public long getSecondControlChannel()
    {
        if(getChannelAccessInformation().isChannel())
        {
            int channel = getMessage().getInt(SECOND_CONTROL_CHANNEL);

            if(channel > 0)
            {
                return getChannelAccessInformation().getBaseFrequency() +
                        (getChannelAccessInformation().getStepSize() * channel);
            }
        }

        return 0;
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
    public ServiceInformation getServiceInformation()
    {
        if(mServiceInformation == null)
        {
            mServiceInformation = new ServiceInformation(getMessage(), SERVICE_INFORMATION);
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
        List<Identifier> identifiers = new ArrayList<>(getLocationID().getIdentifiers());
        return identifiers;
    }
}
