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
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public class SecondaryControlChannelBroadcast extends TSBKMessage implements FrequencyBandReceiver
{
    public static final int[] RFSS_ID = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int[] SITE_ID = {88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] FREQUENCY_BAND_1 = {96, 97, 98, 99};
    public static final int[] CHANNEL_NUMBER_1 = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SYSTEM_SERVICE_CLASS_1 = {112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] FREQUENCY_BAND_2 = {120, 121, 122, 123};
    public static final int[] CHANNEL_NUMBER_2 = {124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SYSTEM_SERVICE_CLASS_2 = {136, 137, 138, 139, 140, 141, 142, 143};

    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannel1;
    private IAPCO25Channel mChannel2;

    public SecondaryControlChannelBroadcast(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" SITE:" + getRFSS() + "-" + getSiteID());
        sb.append(" CHAN1:" + getChannel1());
        sb.append(" SVC1:" + SystemService.toString(getSystemServiceClass1()));

        if(hasChannel2())
        {
            sb.append(" CHAN2:" + getChannel2());
            sb.append(" SVC2:" + SystemService.toString(getSystemServiceClass2()));
        }

        return sb.toString();
    }

    public IIdentifier getRFSS()
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

    public IAPCO25Channel getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_1), mMessage.getInt(CHANNEL_NUMBER_1));
        }

        return mChannel1;
    }

    public int getSystemServiceClass1()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_1);
    }

    public IAPCO25Channel getChannel2()
    {
        if(mChannel2 == null)
        {
            mChannel2 = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_2), mMessage.getInt(CHANNEL_NUMBER_2));
        }

        return mChannel2;
    }

    public int getSystemServiceClass2()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_2);
    }

    public boolean hasChannel2()
    {
        return getSystemServiceClass2() != 0;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel1());

        if(hasChannel2())
        {
            channels.add(getChannel2());
        }

        return channels;
    }
}
