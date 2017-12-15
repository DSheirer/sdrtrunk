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

public class GroupVoiceChannelUpdateExplicit extends LDU1Message implements IdentifierReceiver
{
    public static final int EMERGENCY_FLAG = 376;
    public static final int ENCRYPTED_CHANNEL_FLAG = 377;
    public static final int DUPLEX_MODE = 382;
    public static final int SESSION_MODE = 383;
    public static final int[] GROUP_ADDRESS = {536, 537, 538, 539, 540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] TRANSMIT_IDENTIFIER = {560, 561, 566, 567};
    public static final int[] TRANSMIT_CHANNEL = {568, 569, 570, 571, 720, 721, 722, 723, 724, 725, 730, 731};
    public static final int[] RECEIVE_IDENTIFIER = {732, 733, 734, 735};
    public static final int[] RECEIVE_CHANNEL = {740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private IBandIdentifier mTransmitIdentifierUpdate;
    private IBandIdentifier mReceiveIdentifierUpdate;

    public GroupVoiceChannelUpdateExplicit(LDU1Message message)
    {
        super(message);
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
