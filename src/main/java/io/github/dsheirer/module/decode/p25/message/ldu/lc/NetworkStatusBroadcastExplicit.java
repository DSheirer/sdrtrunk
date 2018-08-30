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

import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;

import java.util.ArrayList;
import java.util.List;

public class NetworkStatusBroadcastExplicit extends LDU1Message implements FrequencyBandReceiver
{
    public static final int[] NETWORK_ID = {364, 365, 366, 367, 372, 373, 374, 375, 376, 377, 382, 383, 384, 385, 386,
        387, 536, 537, 538, 539};
    public static final int[] SYSTEM_ID = {540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] DOWNLINK_FREQUENCY_BAND = {560, 561, 566, 567};
    public static final int[] DOWNLINK_CHANNEL_NUMBER = {568, 569, 570, 571, 720, 721, 722, 723, 724, 725, 730, 731};
    public static final int[] UPLINK_FREQUENCY_BAND = {732, 733, 734, 735};
    public static final int[] UPLINK_CHANNEL_NUMBER = {740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    public IAPCO25Channel mChannel;

    public NetworkStatusBroadcastExplicit(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" NETWORK:" + getNetworkID());
        sb.append(" SYS:" + getSystem());
        sb.append(" CHAN:" + getChannel());

        return sb.toString();
    }

    public String getNetworkID()
    {
        return mMessage.getHex(NETWORK_ID, 5);
    }

    public String getSystem()
    {
        return mMessage.getHex(SYSTEM_ID, 3);
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
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
