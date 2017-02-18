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
import module.decode.p25.message.tsbk.osp.control.SystemService;
import module.decode.p25.reference.LinkControlOpcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RFSSStatusBroadcastExplicit extends TDULinkControlMessage implements IdentifierReceiver
{
    private final static Logger mLog = LoggerFactory.getLogger(RFSSStatusBroadcastExplicit.class);

    public static final int[] LRA = {72, 73, 74, 75, 88, 89, 90, 91};
    public static final int[] RECEIVE_IDENTIFIER = {92, 93, 94, 95};
    public static final int[] RECEIVE_CHANNEL = {96, 97, 98, 99, 112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] RFSS_ID = {120, 121, 122, 123, 136, 137, 138, 139};
    public static final int[] SITE_ID = {140, 141, 142, 143, 144, 145, 146, 147};
    public static final int[] TRANSMIT_IDENTIFIER = {160, 161, 162, 163};
    public static final int[] TRANSMIT_CHANNEL = {164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187};
    public static final int[] SYSTEM_SERVICE_CLASS = {188, 189, 190, 191, 192, 193, 194, 195};

    private IBandIdentifier mTransmitIdentifierUpdate;
    private IBandIdentifier mReceiveIdentifierUpdate;

    public RFSSStatusBroadcastExplicit(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.RFSS_STATUS_BROADCAST_EXPLICIT.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLocationRegistrationArea());

        sb.append(" SITE:" + getRFSubsystemID() + "-" + getSiteID());

        sb.append(" TRANSMIT:" + getTransmitChannelNumber());

        sb.append(" RECEIVE:" + getReceiveChannelNumber());

        sb.append(" " + SystemService.toString(getSystemServiceClass()));

        sb.append(" " + mMessage.toString());

        return sb.toString();
    }

    public String getLocationRegistrationArea()
    {
        return mMessage.getHex(LRA, 2);
    }

    public String getRFSubsystemID()
    {
        return mMessage.getHex(RFSS_ID, 2);
    }

    public String getSiteID()
    {
        return mMessage.getHex(SITE_ID, 2);
    }

    public int getTransmitIdentifier()
    {
        return mMessage.getInt(TRANSMIT_IDENTIFIER);
    }

    public String getTransmitChannel()
    {
        return getTransmitIdentifier() + "-" + getTransmitChannelNumber();
    }

    public int getTransmitChannelNumber()
    {
        return mMessage.getInt(TRANSMIT_CHANNEL);
    }

    public int getReceiveIdentifier()
    {
        return mMessage.getInt(RECEIVE_IDENTIFIER);
    }

    public String getReceiveChannel()
    {
        return getReceiveIdentifier() + "-" + getReceiveChannelNumber();
    }

    public int getReceiveChannelNumber()
    {
        return mMessage.getInt(RECEIVE_CHANNEL);
    }

    public int getSystemServiceClass()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS);
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
