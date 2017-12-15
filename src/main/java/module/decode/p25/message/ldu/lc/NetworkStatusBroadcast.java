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
package module.decode.p25.message.ldu.lc;

import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.ldu.LDU1Message;
import module.decode.p25.reference.LinkControlOpcode;
import module.decode.p25.reference.Service;

public class NetworkStatusBroadcast extends LDU1Message implements IdentifierReceiver
{
    public static final int[] NETWORK_ID = {376, 377, 382, 383, 384, 385, 386, 387, 536, 537, 538, 539, 540, 541, 546,
        547, 548, 549, 550, 551};
    public static final int[] SYSTEM_ID = {556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] IDENTIFIER = {720, 721, 722, 723};
    public static final int[] CHANNEL = {724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743};
    public static final int[] SYSTEM_SERVICE_CLASS = {744, 745, 750, 751, 752, 753, 754, 755};

    private IBandIdentifier mIdentifierUpdate;

    public NetworkStatusBroadcast(LDU1Message source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.NETWORK_STATUS_BROADCAST.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" NETWORK:" + getNetworkID());
        sb.append(" SYS:" + getSystem());
        sb.append(" CHAN:" + getChannel());
        sb.append(" " + Service.getServices(getSystemServiceClass()).toString());

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

    public int getIdentifier()
    {
        return mMessage.getInt(IDENTIFIER);
    }

    public int getChannelNumber()
    {
        return mMessage.getInt(CHANNEL);
    }

    public String getChannel()
    {
        return getIdentifier() + "-" + getChannelNumber();
    }

    public int getSystemServiceClass()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS);
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        mIdentifierUpdate = message;
    }

    @Override
    public int[] getIdentifiers()
    {
        int[] identifiers = new int[1];

        identifiers[0] = getIdentifier();

        return identifiers;
    }

    public long getDownlinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.getDownlinkFrequency(getChannelNumber());
        }

        return 0;
    }

    public long getUplinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.getUplinkFrequency(getChannelNumber());
        }

        return 0;
    }
}
