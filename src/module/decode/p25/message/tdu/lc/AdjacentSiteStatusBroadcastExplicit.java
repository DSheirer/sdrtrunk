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

import module.decode.p25.message.IAdjacentSite;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.reference.LinkControlOpcode;

public class AdjacentSiteStatusBroadcastExplicit extends TDULinkControlMessage implements IdentifierReceiver, IAdjacentSite
{
    public static final int[] LRA = {72, 73, 74, 75, 88, 89, 90, 91};
    public static final int[] TRANSMIT_IDENTIFIER = {92, 93, 94, 95};
    public static final int[] TRANSMIT_CHANNEL = {96, 97, 98, 99, 112, 113, 114, 115,
        116, 117, 118, 119};
    public static final int[] RFSS_ID = {120, 121, 122, 123, 136, 137, 138, 139};
    public static final int[] SITE_ID = {140, 141, 142, 143, 144, 145, 146, 147};

    public static final int[] RECEIVE_IDENTIFIER = {160, 161, 162, 163};
    public static final int[] RECEIVE_CHANNEL = {164, 165, 166, 167, 168, 169, 170,
        171, 184, 185, 186, 187};

    private IBandIdentifier mTransmitBand;
    private IBandIdentifier mReceiveBand;

    public AdjacentSiteStatusBroadcastExplicit(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLRA());

        sb.append(" SITE:" + getRFSS() + "-" + getSiteID());

        sb.append(" DNLINK:" + getDownlinkChannel());

        sb.append(" UPLINK:" + getUplinkChannel());

        return sb.toString();
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

    public String getLRA()
    {
        return mMessage.getHex(LRA, 2);
    }

    public String getRFSS()
    {
        return mMessage.getHex(RFSS_ID, 2);
    }

    public String getSystemID()
    {
        return "***";
    }

    public String getSiteID()
    {
        return mMessage.getHex(SITE_ID, 2);
    }

    @Override
    public String getSystemServiceClass()
    {
        return "[]";
    }

    public int getTransmitIdentifier()
    {
        return mMessage.getInt(TRANSMIT_IDENTIFIER);
    }

    public int getTransmitChannel()
    {
        return mMessage.getInt(TRANSMIT_CHANNEL);
    }

    public String getDownlinkChannel()
    {
        return getTransmitIdentifier() + "-" + getTransmitChannel();
    }

    public int getReceiveIdentifier()
    {
        return mMessage.getInt(RECEIVE_IDENTIFIER);
    }

    public int getReceiveChannel()
    {
        return mMessage.getInt(RECEIVE_CHANNEL);
    }

    public String getUplinkChannel()
    {
        return getReceiveIdentifier() + "-" + getReceiveChannel();
    }

    public long getDownlinkFrequency()
    {
        if(mTransmitBand != null)
        {
            return mTransmitBand.getDownlinkFrequency(getTransmitChannel());
        }

        return 0;
    }

    public long getUplinkFrequency()
    {
        if(mReceiveBand != null)
        {
            return mReceiveBand.getUplinkFrequency(getReceiveChannel());
        }

        return 0;
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getTransmitIdentifier())
        {
            mTransmitBand = message;
        }

        if(identifier == getReceiveIdentifier())
        {
            mReceiveBand = message;
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
}
