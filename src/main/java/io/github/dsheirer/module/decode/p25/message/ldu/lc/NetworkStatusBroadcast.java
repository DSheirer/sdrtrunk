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

import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.Service;

import java.util.ArrayList;
import java.util.List;

public class NetworkStatusBroadcast extends LDU1Message implements FrequencyBandReceiver
{
    public static final int[] WACN_ID = {376, 377, 382, 383, 384, 385, 386, 387, 536, 537, 538, 539, 540, 541, 546,
        547, 548, 549, 550, 551};
    public static final int[] SYSTEM_ID = {556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] FREQUENCY_BAND = {720, 721, 722, 723};
    public static final int[] CHANNEL_NUMBER = {724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743};
    public static final int[] SYSTEM_SERVICE_CLASS = {744, 745, 750, 751, 752, 753, 754, 755};

    private IAPCO25Channel mChannel;

    public NetworkStatusBroadcast(LDU1Message source)
    {
        super(source);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" WACN:" + getWACN());
        sb.append(" SYS:" + getSystem());
        sb.append(" CHAN:" + getChannel());
        sb.append(" " + Service.getServices(getSystemServiceClass()).toString());

        return sb.toString();
    }

    public int getWACN()
    {
        return mMessage.getInt(WACN_ID);
    }

    public int getSystem()
    {
        return mMessage.getInt(SYSTEM_ID);
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND), mMessage.getInt(CHANNEL_NUMBER));
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
