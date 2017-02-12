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
import module.decode.p25.message.IAdjacentSite;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;

public class AdjacentStatusBroadcast extends TSBKMessage implements IdentifierReceiver, IAdjacentSite
{
    public static final int[] LOCATION_REGISTRATION_AREA = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int CONVENTIONAL_CHANNEL_FLAG = 88;
    public static final int SITE_FAILURE_CONDITION_FLAG = 89;
    public static final int VALID_FLAG = 90;
    public static final int ACTIVE_NETWORK_CONNECTION_FLAG = 91;
    public static final int[] SYSTEM_ID = {92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] RFSS_ID = {104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SITE_ID = {112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] IDENTIFIER = {120, 121, 122, 123};
    public static final int[] CHANNEL = {124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SYSTEM_SERVICE_CLASS = {136, 137, 138, 139, 140, 141, 142, 143};

    private IBandIdentifier mIdentifierUpdate;

    public AdjacentStatusBroadcast(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getEventType()
    {
        return Opcode.ADJACENT_STATUS_BROADCAST.getDescription();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLRA());

        sb.append(" SYS:" + getSystemID());

        sb.append(" SITE:" + getRFSS() + "-" + getSiteID());

        sb.append(" CHAN:" + getIdentifier() + "-" + getChannel());

        sb.append(" DN:" + getDownlinkFrequency());

        sb.append(" UP:" + getUplinkFrequency());

        if(isConventionalChannel())
        {
            sb.append(" CONVENTIONAL");
        }

        if(isSiteFailureCondition())
        {
            sb.append(" FAILURE-CONDITION");
        }

        if(isValidSiteInformation())
        {
            sb.append(" VALID-INFO");
        }

        if(hasActiveNetworkConnection())
        {
            sb.append(" ACTIVE-NETWORK-CONN");
        }

        sb.append(getSystemServiceClass());

        return sb.toString();
    }

    @Override
    public String getDownlinkChannel()
    {
        return getIdentifier() + "-" + getChannel();
    }

    @Override
    public String getUplinkChannel()
    {
        return getIdentifier() + "-" + getChannel();
    }

    public String getLRA()
    {
        return mMessage.getHex(LOCATION_REGISTRATION_AREA, 2);
    }

    public boolean isConventionalChannel()
    {
        return mMessage.get(CONVENTIONAL_CHANNEL_FLAG);
    }

    public boolean isSiteFailureCondition()
    {
        return mMessage.get(SITE_FAILURE_CONDITION_FLAG);
    }

    public boolean isValidSiteInformation()
    {
        return mMessage.get(VALID_FLAG);
    }

    public boolean hasActiveNetworkConnection()
    {
        return mMessage.get(ACTIVE_NETWORK_CONNECTION_FLAG);
    }

    public String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getSystemID());
        sb.append(":");
        sb.append(getRFSS());
        sb.append(":");
        sb.append(getSiteID());

        return sb.toString();
    }

    public String getSystemID()
    {
        return mMessage.getHex(SYSTEM_ID, 3);
    }

    public String getRFSS()
    {
        return mMessage.getHex(RFSS_ID, 2);
    }

    public String getSiteID()
    {
        return mMessage.getHex(SITE_ID, 2);
    }

    public int getChannel()
    {
        return mMessage.getInt(CHANNEL);
    }

    public int getIdentifier()
    {
        return mMessage.getInt(IDENTIFIER);
    }

    public String getSystemServiceClass()
    {
        return SystemService.toString(mMessage.getInt(SYSTEM_SERVICE_CLASS));
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
