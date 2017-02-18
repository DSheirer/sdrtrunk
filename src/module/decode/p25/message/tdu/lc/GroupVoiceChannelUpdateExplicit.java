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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupVoiceChannelUpdateExplicit extends TDULinkControlMessage
    implements IdentifierReceiver
{
    private final static Logger mLog =
        LoggerFactory.getLogger(GroupVoiceChannelUpdateExplicit.class);

    public static final int EMERGENCY_FLAG = 92;
    public static final int ENCRYPTED_CHANNEL_FLAG = 93;
    public static final int DUPLEX_MODE = 94;
    public static final int SESSION_MODE = 95;
    public static final int[] GROUP_ADDRESS = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139};
    public static final int[] TRANSMIT_IDENTIFIER = {140, 141, 142, 143};
    public static final int[] TRANSMIT_CHANNEL = {144, 145, 146, 147, 160, 161, 162, 163, 164, 165, 166, 167};
    public static final int[] RECEIVE_IDENTIFIER = {168, 169, 170, 171};
    public static final int[] RECEIVE_CHANNEL = {184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199};

    private IBandIdentifier mTransmitIdentifierUpdate;
    private IBandIdentifier mReceiveIdentifierUpdate;

    public GroupVoiceChannelUpdateExplicit(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }
        if(isEncryptedChannel())
        {
            sb.append(" ENCRYPTED CHANNEL");
        }


        return sb.toString();
    }

    public boolean isEmergency()
    {
        return mMessage.get(EMERGENCY_FLAG);
    }

    public boolean isEncryptedChannel()
    {
        return mMessage.get(ENCRYPTED_CHANNEL_FLAG);
    }

    public DuplexMode getDuplexMode()
    {
        return mMessage.get(DUPLEX_MODE) ? DuplexMode.FULL : DuplexMode.HALF;
    }

    public SessionMode getSessionMode()
    {
        return mMessage.get(SESSION_MODE) ?
            SessionMode.CIRCUIT : SessionMode.PACKET;
    }

    public int getTransmitChannelIdentifier()
    {
        return mMessage.getInt(TRANSMIT_IDENTIFIER);
    }

    public int getTransmitChannelNumber()
    {
        return mMessage.getInt(TRANSMIT_CHANNEL);
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
        return mMessage.getInt(RECEIVE_CHANNEL);
    }

    public String getReceiveChannel()
    {
        return getReceiveChannelIdentifier() + "-" + getReceiveChannelNumber();
    }

    public String getGroupAddress()
    {
        return mMessage.getHex(GROUP_ADDRESS, 4);
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getTransmitChannelIdentifier())
        {
            mTransmitIdentifierUpdate = message;
        }
        if(identifier == getReceiveChannelIdentifier())
        {
            mReceiveIdentifierUpdate = message;
        }
    }

    @Override
    public int[] getIdentifiers()
    {
        int[] identifiers = new int[2];

        identifiers[0] = getTransmitChannelIdentifier();
        identifiers[1] = getReceiveChannelIdentifier();

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
