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
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Lra;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.IAdjacentSite;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;

import java.util.ArrayList;
import java.util.List;

public class AdjacentSiteStatusBroadcast extends LDU1Message implements FrequencyBandReceiver, IAdjacentSite
{
    public static final int[] LRA = {364, 365, 366, 367, 372, 373, 374, 375};
    public static final int[] SYSTEM_ID = {384, 385, 386, 387, 536, 537, 538, 539, 540, 541, 546, 547};
    public static final int[] RFSS_ID = {548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] SITE_ID = {560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] FREQUENCY_BAND = {720, 721, 722, 723};
    public static final int[] CHANNEL = {724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743};
    public static final int[] SYSTEM_SERVICE_CLASS = {744, 745, 750, 751, 752, 753, 754, 755};

    private IIdentifier mSystem;
    private IIdentifier mSite;
    private IIdentifier mRFSS;
    private IIdentifier mLRA;
    private IAPCO25Channel mChannel;

    public AdjacentSiteStatusBroadcast(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLRAId());

        sb.append(" SYS:" + getSystemID());

        sb.append(" SITE:" + getRFSSId() + "-" + getSiteID());

        sb.append(" CHAN:" + getChannel());

        sb.append(" " + getSystemServiceClass());

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

    /**
     * Location Registration Area
     */
    public IIdentifier getLRAId()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(mMessage.getInt(LRA));
        }

        return mLRA;
    }

    /**
     * System identifier
     */
    public IIdentifier getSystemID()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(mMessage.getInt(SYSTEM_ID));
        }

        return mSystem;
    }

    public IIdentifier getRFSSId()
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

    public int getFrequencyBand()
    {
        return mMessage.getInt(FREQUENCY_BAND);
    }

    @Override
    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getFrequencyBand(), getChannelNumber());
        }

        return mChannel;
    }

    public int getChannelNumber()
    {
        return mMessage.getInt(CHANNEL);
    }

    public String getSystemServiceClass()
    {
        return SystemService.toString(mMessage.getInt(SYSTEM_SERVICE_CLASS));
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
