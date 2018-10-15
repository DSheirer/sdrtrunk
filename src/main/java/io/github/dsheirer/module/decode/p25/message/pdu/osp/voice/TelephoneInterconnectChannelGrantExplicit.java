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
package io.github.dsheirer.module.decode.p25.message.pdu.osp.voice;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.module.decode.p25.message.IBandIdentifier;
import io.github.dsheirer.module.decode.p25.message.IdentifierReceiver;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TelephoneInterconnectChannelGrantExplicit extends PDUMessage implements IdentifierReceiver
{
    /* Service Options */
    public static final int EMERGENCY_FLAG = 128;
    public static final int ENCRYPTED_CHANNEL_FLAG = 129;
    public static final int DUPLEX_MODE = 130;
    public static final int SESSION_MODE = 131;

    public static final int[] ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105,
        106, 107, 108, 109, 110, 111};
    public static final int[] SERVICE_OPTIONS = {128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] TRANSMIT_IDENTIFIER = {160, 161, 162, 163};
    public static final int[] TRANSMIT_NUMBER = {164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175};
    public static final int[] RECEIVE_IDENTIFIER = {176, 177, 178, 179};
    public static final int[] RECEIVE_NUMBER = {180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191};
    public static final int[] CALL_TIMER = {192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207};
    public static final int[] MULTIPLE_BLOCK_CRC = {224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236,
        237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};

    private SimpleDateFormat mTimeDurationFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    private IBandIdentifier mTransmitIdentifierUpdate;
    private IBandIdentifier mReceiveIdentifierUpdate;

    public TelephoneInterconnectChannelGrantExplicit(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);

	    /* Header block is already error detected/corrected - perform error
         * detection correction on the intermediate and final data blocks */
//        mMessage = CRCP25.correctPDU1(mMessage);
//        mCRC[1] = mMessage.getCRC();
    }



    public String getEventType()
    {
        return Opcode.TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT.getDescription();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }

        sb.append(" ADDR:");
        sb.append(getAddress());

        sb.append(" CALL TIMER:");
        sb.append(mTimeDurationFormat.format(new Date(getCallTimer())));

        sb.append(" CHAN DN:" + getTransmitChannelIdentifier() + "-" + getTransmitChannelNumber());
        sb.append(" " + getDownlinkFrequency());

        sb.append(" CHAN UP:" + getReceiveChannelIdentifier() + "-" + getReceiveChannelNumber());
        sb.append(" " + getUplinkFrequency());

        return sb.toString();
    }

    public boolean isEmergency()
    {
        return getMessage().get(EMERGENCY_FLAG);
    }

    public boolean isEncrypted()
    {
        return getMessage().get(ENCRYPTED_CHANNEL_FLAG);
    }

    public P25Message.DuplexMode getDuplexMode()
    {
        return getMessage().get(DUPLEX_MODE) ? P25Message.DuplexMode.FULL : P25Message.DuplexMode.HALF;
    }

    public P25Message.SessionMode getSessionMode()
    {
        return getMessage().get(SESSION_MODE) ?
            P25Message.SessionMode.CIRCUIT : P25Message.SessionMode.PACKET;
    }

    public String getAddress()
    {
        return getMessage().getHex(ADDRESS, 6);
    }

    /*
     * Call timer in milliseconds
     */
    public long getCallTimer()
    {
        int timer = getMessage().getInt(CALL_TIMER);

        return timer / 100;
    }

    public int getTransmitChannelIdentifier()
    {
        return getMessage().getInt(TRANSMIT_IDENTIFIER);
    }

    public int getTransmitChannelNumber()
    {
        return getMessage().getInt(TRANSMIT_NUMBER);
    }

    public String getTransmitChannel()
    {
        return getTransmitChannelIdentifier() + "-" + getTransmitChannelNumber();
    }

    public int getReceiveChannelIdentifier()
    {
        return getMessage().getInt(RECEIVE_IDENTIFIER);
    }

    public int getReceiveChannelNumber()
    {
        return getMessage().getInt(RECEIVE_NUMBER);
    }

    public String getReceiveChannel()
    {
        return getReceiveChannelIdentifier() + "-" + getReceiveChannelNumber();
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

    public int[] getIdentifiers()
    {
        int[] idens = new int[2];

        idens[0] = getTransmitChannelIdentifier();
        idens[1] = getReceiveChannelIdentifier();

        return idens;
    }
}
