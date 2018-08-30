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
import io.github.dsheirer.identifier.integer.talkgroup.APCO25AnyTalkgroup;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

public class TelephoneInterconnectVoiceChannelUser extends TDULinkControlMessage
{
    public static final int[] SERVICE_OPTIONS = {92, 93, 94, 95, 96, 97, 98, 99};
    public static final int[] CALL_TIMER = {120, 121, 122, 123, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147};
    public static final int[] ADDRESS = {160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195};

    private ServiceOptions mServiceOptions;
    private IIdentifier mAddress;

    public TelephoneInterconnectVoiceChannelUser(TDULinkControlMessage source)
    {
        super(source);
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
