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
package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Lra;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.IAdjacentSite;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;

import java.util.ArrayList;
import java.util.List;

public class AdjacentSiteStatusBroadcastExplicit extends LDU1Message implements FrequencyBandReceiver, IAdjacentSite
{
    public static final int[] LRA = {364, 365, 366, 367, 372, 373, 374, 375};
    public static final int[] DOWNLINK_FREQUENCY_BAND = {376, 377, 382, 383};
    public static final int[] DOWNLINK_CHANNEL_NUMBER = {384, 385, 386, 387, 536, 537, 538, 539, 540, 541, 546, 547};
    public static final int[] RFSS_ID = {548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] SITE_ID = {560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] UPLINK_FREQUENCY_BAND = {720, 721, 722, 723};
    public static final int[] UPLINK_CHANNEL_NUMBER = {724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743};

    private IAPCO25Channel mChannel;
    private IIdentifier mSite;
    private IIdentifier mRfss;
    private IIdentifier mLra;

    public AdjacentSiteStatusBroadcastExplicit(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLRAId());

        sb.append(" SITE:" + getRFSSId() + "-" + getSiteID());

        sb.append(" CHAN:" + getChannel());

        return sb.toString();
    }

    public String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getSystemID());
        sb.append(":");
        sb.append(getRFSSId());
        sb.append(":");
        sb.append(getSiteID());

        return sb.toString();
    }

    public IIdentifier getLRAId()
    {
        if(mLra == null)
        {
            mLra = APCO25Lra.create(mMessage.getInt(LRA));
        }

        return mLra;
    }

    public IIdentifier getRFSSId()
    {
        if(mRfss == null)
        {
            mRfss = APCO25Rfss.create(mMessage.getInt(RFSS_ID));
        }

        return mRfss;
    }

    public IIdentifier getSystemID()
    {
        return null;
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

    @Override
    public String getSystemServiceClass()
    {
        return "[]";
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
