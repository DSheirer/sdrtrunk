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
import io.github.dsheirer.identifier.integer.talkgroup.APCO25AnyTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

public class TelephoneInterconnectVoiceChannelUser extends LDU1Message
{
    public static final int[] SERVICE_OPTIONS = {376, 377, 382, 383, 384, 385, 386, 387};
    public static final int[] CALL_TIMER = {548, 549, 550, 551, 556, 557, 558, 559,
        560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] ADDRESS = {720, 721, 722, 723, 724, 725, 730, 731, 732,
        733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private ServiceOptions mServiceOptions;
    private IIdentifier mAddress;

    public TelephoneInterconnectVoiceChannelUser(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(" ").append(getServiceOptions());
        sb.append(" TIMER:" + getCallTimer() + " SECS");
        sb.append(" ADDR:" + getAddress());

        return sb.toString();
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(mMessage.getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    public IIdentifier getAddress()
    {
        if(mAddress == null)
        {
            mAddress = APCO25AnyTalkgroup.create(mMessage.getInt(ADDRESS));
        }

        return mAddress;
    }

    /**
     * Call timer in seconds
     */
    public int getCallTimer()
    {
        int units = mMessage.getInt(CALL_TIMER);

        return (int)(units / 10);
    }

}
