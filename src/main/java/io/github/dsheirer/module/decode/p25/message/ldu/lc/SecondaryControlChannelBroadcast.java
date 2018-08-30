/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;

import java.util.ArrayList;
import java.util.List;

public class SecondaryControlChannelBroadcast extends LDU1Message implements FrequencyBandReceiver
{
    public static final int[] RFSS_ID = {364, 365, 366, 367, 372, 373, 374, 375};
    public static final int[] SITE_ID = {376, 377, 382, 383, 384, 385, 386, 387};
    public static final int[] FREQUENCY_BAND_A = {536, 537, 538, 539};
    public static final int[] CHANNEL_NUMBER_A = {540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] SYSTEM_SERVICE_CLASS_A = {560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] FREQUENCY_BAND_B = {720, 721, 722, 723};
    public static final int[] CHANNEL_NUMBER_B = {724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743};
    public static final int[] SYSTEM_SERVICE_CLASS_B = {744, 745, 750, 751, 752, 753, 754, 755};

    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannelA;
    private IAPCO25Channel mChannelB;

    public SecondaryControlChannelBroadcast(LDU1Message message)
    {
        super(message);
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
