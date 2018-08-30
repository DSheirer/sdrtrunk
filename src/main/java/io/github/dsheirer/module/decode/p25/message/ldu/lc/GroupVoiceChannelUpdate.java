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
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;

import java.util.ArrayList;
import java.util.List;

public class GroupVoiceChannelUpdate extends LDU1Message implements FrequencyBandReceiver
{
    public static final int[] FREQUENCY_BAND_A = {364, 365, 366, 367};
    public static final int[] CHANNEL_A = {372, 373, 374, 375, 376, 377, 382, 383, 384, 385, 386, 387};
    public static final int[] GROUP_ADDRESS_A = {536, 537, 538, 539, 540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] FREQUENCY_BAND_B = {560, 561, 566, 567};
    public static final int[] CHANNEL_B = {568, 569, 570, 571, 720, 721, 722, 723, 724, 725, 730, 731};
    public static final int[] GROUP_ADDRESS_B = {732, 733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private IAPCO25Channel mChannelA;
    private IAPCO25Channel mChannelB;
    private IIdentifier mTalkgroupA;
    private IIdentifier mTalkgroupB;

    public GroupVoiceChannelUpdate(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" CHAN/GROUP A:").append(getChannelA()).append("/").append(getGroupAddressA());

        if(hasChannelB())
        {
            sb.append(" B:").append(getChannelB()).append("/").append(getGroupAddressB());
        }

        return sb.toString();
    }

    public IAPCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_A), mMessage.getInt(CHANNEL_A));
        }

        return mChannelA;
    }

    public IAPCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND_B), mMessage.getInt(CHANNEL_B));
        }

        return mChannelB;
    }

    public boolean hasChannelB()
    {
        return mMessage.getInt(CHANNEL_B) != 0;
    }

    public IIdentifier getGroupAddressA()
    {
        if(mTalkgroupA == null)
        {
            mTalkgroupA = APCO25ToTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS_A));
        }

        return mTalkgroupA;
    }

    public IIdentifier getGroupAddressB()
    {
        if(mTalkgroupB == null)
        {
            mTalkgroupB = APCO25ToTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS_B));
        }

        return mTalkgroupB;
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
