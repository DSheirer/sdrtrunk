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
package module.decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BinaryMessage;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import module.decode.p25.reference.P25NetworkCallsign;

public class NetworkStatusBroadcast extends TSBKMessage implements IdentifierReceiver
{
    public static final int[] LOCATION_REGISTRATION_AREA = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int[] WACN = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107};
    public static final int[] SYSTEM_ID = {108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] IDENTIFIER = {120, 121, 122, 123};
    public static final int[] CHANNEL = {124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SYSTEM_SERVICE_CLASS = {136, 137, 138, 139, 140, 141, 142, 143};

    private IBandIdentifier mIdentifierUpdate;

    public NetworkStatusBroadcast(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getEventType()
    {
        return Opcode.NETWORK_STATUS_BROADCAST.getDescription();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLocationRegistrationArea());

        sb.append(" WACN:" + getWACN());

        sb.append(" SYS:" + getSystemID());

        sb.append(" CTRL CHAN:" + getIdentifier() + "-" + getChannel());

        sb.append(" DN:" + getDownlinkFrequency());

        sb.append(" UP:" + getUplinkFrequency());

        sb.append(" SYS SVC CLASS:" +
            SystemService.toString(getSystemServiceClass()));

        return sb.toString();
    }

    public String getLocationRegistrationArea()
    {
        return mMessage.getHex(LOCATION_REGISTRATION_AREA, 2);
    }

    public String getWACN()
    {
        return mMessage.getHex(WACN, 5);
    }

    public String getSystemID()
    {
        return mMessage.getHex(SYSTEM_ID, 3);
    }

    public String getNetworkCallsign()
    {
        return P25NetworkCallsign.getCallsign(mMessage.getInt(WACN),
            mMessage.getInt(SYSTEM_ID));
    }

    public int getIdentifier()
    {
        return mMessage.getInt(IDENTIFIER);
    }

    public int getChannel()
    {
        return mMessage.getInt(CHANNEL);
    }

    public int getSystemServiceClass()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS);
    }

    public long getDownlinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.getDownlinkFrequency(getChannel());
        }

        return 0;
    }

    public long getUplinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.getUplinkFrequency(getChannel());
        }

        return 0;
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        /* we're only expecting 1 identifier, so use whatever is received */
        mIdentifierUpdate = message;
    }

    public int[] getIdentifiers()
    {
        int[] idens = new int[1];

        idens[0] = getIdentifier();

        return idens;
    }
}
