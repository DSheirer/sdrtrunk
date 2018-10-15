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
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;

import java.util.ArrayList;
import java.util.List;

public class SecondaryControlChannelBroadcast extends TDULinkControlMessage implements FrequencyBandReceiver
{
    public static final int[] RFSS_ID = {72, 73, 74, 75, 88, 89, 90, 91};
    public static final int[] SITE_ID = {92, 93, 94, 95, 96, 97, 98, 99};
    public static final int[] FREQUENCY_BAND_A = {112, 113, 114, 115};
    public static final int[] CHANNEL_NUMBER_A = {116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139};
    public static final int[] SYSTEM_SERVICE_CLASS_A = {140, 141, 142, 143, 144, 145, 146, 147};
    public static final int[] FREQUENCY_BAND_B = {160, 161, 162, 163};
    public static final int[] CHANNEL_NUMBER_B = {164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187};
    public static final int[] SYSTEM_SERVICE_CLASS_B = {188, 189, 190, 191, 192, 193, 194, 195};

    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannelA;
    private IAPCO25Channel mChannelB;

    public SecondaryControlChannelBroadcast(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.SECONDARY_CONTROL_CHANNEL_BROADCAST.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" SITE:" + getRFSubsystemID() + "-" + getSiteID());

        sb.append(" CHAN A:" + getChannelA());

        sb.append(" " + SystemService.toString(getSystemServiceClassA()));

        sb.append(" CHAN B:" + getChannelB());

        sb.append(" " + SystemService.toString(getSystemServiceClassB()));

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

    public IAPCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_A), mMessage.getInt(CHANNEL_NUMBER_A));
        }

        return mChannelA;
    }

    public IAPCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_B), mMessage.getInt(CHANNEL_NUMBER_B));
        }

        return mChannelB;
    }

    public int getSystemServiceClassA()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_A);
    }

    public int getSystemServiceClassB()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_B);
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannelA());
        channels.add(getChannelB());
        return channels;
    }
}
