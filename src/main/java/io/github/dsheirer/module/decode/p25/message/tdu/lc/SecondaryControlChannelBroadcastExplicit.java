/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;

import java.util.ArrayList;
import java.util.List;

public class SecondaryControlChannelBroadcastExplicit extends TDULinkControlMessage implements FrequencyBandReceiver
{
    public static final int[] RFSS_ID = {72, 73, 74, 75, 88, 89, 90, 91};
    public static final int[] SITE_ID = {92, 93, 94, 95, 96, 97, 98, 99};
    public static final int[] DOWNLINK_FREQUENCY_BAND = {112, 113, 114, 115};
    public static final int[] DOWNLINK_CHANNEL_NUMBER = {116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139};
    public static final int[] UPLINK_FREQUENCY_BAND = {140, 141, 142, 143};
    public static final int[] UPLINK_CHANNEL_NUMBER = {144, 145, 146, 147, 160, 161, 162, 163, 164, 165, 166, 167};
    public static final int[] SYSTEM_SERVICE_CLASS = {168, 169, 170, 171, 184, 185, 186, 187};

    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannel;

    public SecondaryControlChannelBroadcastExplicit(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" SITE:" + getRFSubsystemID() + "-" + getSiteID());

        sb.append(" CHAN:" + getChannel());

        sb.append(" " + SystemService.toString(getSystemServiceClass()));

        return sb.toString();
    }

    public IIdentifier getRFSubsystemID()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(mMessage.getInt(RFSS_ID));
        }

        return mRFSS;
    }

    public IIdentifier getSiteID()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(mMessage.getInt(SITE_ID));
        }

        return mSite;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(mMessage.getInt(DOWNLINK_FREQUENCY_BAND),
                mMessage.getInt(DOWNLINK_CHANNEL_NUMBER), mMessage.getInt(UPLINK_FREQUENCY_BAND),
                mMessage.getInt(UPLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public int getSystemServiceClass()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS);
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
