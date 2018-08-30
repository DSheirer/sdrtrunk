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
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.data;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class SNDCPDataChannelGrant extends SNDCPData implements FrequencyBandReceiver
{
    public static final int[] TRANSMIT_IDENTIFIER = {88, 89, 90, 91};
    public static final int[] TRANSMIT_NUMBER = {92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] RECEIVE_IDENTIFIER = {104, 105, 106, 107};
    public static final int[] RECEIVE_NUMBER = {108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] TARGET_ADDRESS = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133,
        134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IFrequencyBand mIdentifierUpdateTransmit;
    private IFrequencyBand mIdentifierUpdateReceive;

    public SNDCPDataChannelGrant(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public boolean isTDMAChannel()
    {
        if(mIdentifierUpdateTransmit != null)
        {
            return mIdentifierUpdateTransmit.isTDMA();
        }

        return false;
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }

        sb.append(" UPLINK:");
        sb.append(getTransmitChannelIdentifier() + "-" + getTransmitChannelNumber());

        sb.append(" " + getUplinkFrequency());

        sb.append(" DOWNLINK:");
        sb.append(getReceiveChannelIdentifier() + "-" + getReceiveChannelNumber());

        sb.append(" " + getDownlinkFrequency());

        sb.append(" TGT:");
        sb.append(getTargetAddress());

        return sb.toString();
    }

    public int getTransmitChannelIdentifier()
    {
        return mMessage.getInt(TRANSMIT_IDENTIFIER);
    }

    public int getTransmitChannelNumber()
    {
        return mMessage.getInt(TRANSMIT_NUMBER);
    }

    public String getTransmitChannel()
    {
        return getTransmitChannelIdentifier() + "-" + getTransmitChannelNumber();
    }

    public int getReceiveChannelIdentifier()
    {
        return mMessage.getInt(RECEIVE_IDENTIFIER);
    }

    public int getReceiveChannelNumber()
    {
        return mMessage.getInt(RECEIVE_NUMBER);
    }

    public String getReceiveChannel()
    {
        return getReceiveChannelIdentifier() + "-" + getReceiveChannelNumber();
    }

    public String getTargetAddress()
    {
        return mMessage.getHex(TARGET_ADDRESS, 6);
    }

    @Override
    public void setIdentifierMessage(int identifier, IFrequencyBand message)
    {
        if(identifier == getTransmitChannelIdentifier())
        {
            mIdentifierUpdateTransmit = message;
        }
        else if(identifier == getReceiveChannelIdentifier())
        {
            mIdentifierUpdateReceive = message;
        }
        else
        {
            throw new IllegalArgumentException("unexpected channel band "
                + "identifier [" + identifier + "]");
        }
    }

    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        int[] identifiers = new int[2];

        identifiers[0] = getTransmitChannelIdentifier();
        identifiers[1] = getReceiveChannelIdentifier();

        return identifiers;
    }

    public long getDownlinkFrequency()
    {
        if(mIdentifierUpdateTransmit != null)
        {
            return mIdentifierUpdateTransmit.getDownlinkFrequency(getTransmitChannelNumber());
        }

        return 0;
    }

    public long getUplinkFrequency()
    {
        if(mIdentifierUpdateReceive != null)
        {
            return mIdentifierUpdateReceive.getUplinkFrequency(getReceiveChannelNumber());
        }

        return 0;
    }
}
