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
package module.decode.p25.message.tdu.lc;

import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.reference.LinkControlOpcode;

public class NetworkStatusBroadcastExplicit extends TDULinkControlMessage implements IdentifierReceiver
{
    public static final int[] WACN = {72, 73, 74, 75, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 112, 113, 114, 115};
    public static final int[] SYSTEM = {116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139};
    public static final int[] TRANSMIT_IDENTIFIER = {140, 141, 142, 143};
    public static final int[] TRANSMIT_CHANNEL = {144, 145, 146, 147, 160, 161, 162, 163, 164, 165, 166, 167};
    public static final int[] RECEIVE_IDENTIFIER = {168, 169, 170, 171};
    public static final int[] RECEIVE_CHANNEL = {184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195};

    private IBandIdentifier mTransmitIdentifierUpdate;
    private IBandIdentifier mReceiveIdentifierUpdate;

    public NetworkStatusBroadcastExplicit(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.NETWORK_STATUS_BROADCAST_EXPLICIT.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" NETWORK:" + getWACN());
        sb.append(" SYS:" + getSystem());
        sb.append(" TRANSMIT:" + getTransmitChannel());
        sb.append(" RECEIVE:" + getReceiveChannel());

        return sb.toString();
    }

    public String getWACN()
    {
        return mMessage.getHex(WACN, 5);
    }

    public String getSystem()
    {
        return mMessage.getHex(SYSTEM, 3);
    }

    public int getTransmitIdentifier()
    {
        return mMessage.getInt(TRANSMIT_IDENTIFIER);
    }

    public int getTransmitChannelNumber()
    {
        return mMessage.getInt(TRANSMIT_CHANNEL);
    }

    public String getTransmitChannel()
    {
        return getTransmitIdentifier() + "-" + getTransmitChannelNumber();
    }

    public int getReceiveIdentifier()
    {
        return mMessage.getInt(RECEIVE_IDENTIFIER);
    }

    public int getReceiveChannelNumber()
    {
        return mMessage.getInt(RECEIVE_CHANNEL);
    }

    public String getReceiveChannel()
    {
        return getReceiveIdentifier() + "-" + getReceiveChannelNumber();
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getTransmitIdentifier())
        {
            mTransmitIdentifierUpdate = message;
        }
        if(identifier == getReceiveIdentifier())
        {
            mReceiveIdentifierUpdate = message;
        }
    }

    @Override
    public int[] getIdentifiers()
    {
        int[] identifiers = new int[2];

        identifiers[0] = getTransmitIdentifier();
        identifiers[1] = getReceiveIdentifier();

        return identifiers;
    }

    public long getDownlinkFrequency()
    {
        if(mTransmitIdentifierUpdate != null)
        {
            return mTransmitIdentifierUpdate.getDownlinkFrequency(getTransmitChannelNumber());
        }

        return 0;
    }

    public long getUplinkFrequency()
    {
        if(mReceiveIdentifierUpdate != null)
        {
            return mReceiveIdentifierUpdate.getUplinkFrequency(getReceiveChannelNumber());
        }

        return 0;
    }
}
