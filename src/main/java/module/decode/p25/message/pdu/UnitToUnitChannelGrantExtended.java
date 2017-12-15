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
package module.decode.p25.message.pdu;

import alias.AliasList;
import bits.BinaryMessage;
import edac.CRCP25;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.reference.DataUnitID;

public abstract class UnitToUnitChannelGrantExtended extends PDUMessage implements IdentifierReceiver
{
    /* Service Options */
    public static final int EMERGENCY_FLAG = 128;
    public static final int ENCRYPTED_CHANNEL_FLAG = 129;
    public static final int DUPLEX_MODE = 130;
    public static final int SESSION_MODE = 131;

    public static final int[] SOURCE_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104,
        105, 106, 107, 108, 109, 110, 111};
    public static final int[] SERVICE_OPTIONS = {128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SOURCE_WACN = {160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174,
        175, 176, 177, 178, 179};
    public static final int[] SOURCE_SYSTEM_ID = {180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191};
    public static final int[] SOURCE_ID = {192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206,
        207, 208, 209, 210, 211, 212, 213, 214, 215};
    public static final int[] TARGET_ADDRESS = {216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229,
        230, 231, 232, 233, 234, 235, 236, 237, 238, 239};
    public static final int[] TRANSMIT_IDENTIFIER = {240, 241, 242, 243};
    public static final int[] TRANSMIT_NUMBER = {244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};
    public static final int[] RECEIVE_IDENTIFIER = {256, 257, 258, 259};
    public static final int[] RECEIVE_NUMBER = {260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271};
    public static final int[] MULTIPLE_BLOCK_CRC = {320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332,
        333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351};

    private IBandIdentifier mTransmitIdentifierUpdate;
    private IBandIdentifier mReceiveIdentifierUpdate;

    public UnitToUnitChannelGrantExtended(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);

	    /* Header block is already error detected/corrected - perform error
         * detection correction on the intermediate and final data blocks */
        mMessage = CRCP25.correctPDU2(mMessage);
        mCRC[1] = mMessage.getCRC();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }

        sb.append(" FROM ADDR:");
        sb.append(getSourceAddress());

        sb.append(" ID:");
        sb.append(getSourceID());

        sb.append(" TO:");
        sb.append(getTargetAddress());

        sb.append(" WACN:");
        sb.append(getSourceWACN());

        sb.append(" SYS:");
        sb.append(getSourceSystemID());

        sb.append(" CHAN DN:" + getTransmitChannelIdentifier() + "-" + getTransmitChannelNumber());
        sb.append(" " + getDownlinkFrequency());

        sb.append(" CHAN UP:" + getReceiveChannelIdentifier() + "-" + getReceiveChannelNumber());
        sb.append(" " + getUplinkFrequency());

        return sb.toString();
    }

    public boolean isEmergency()
    {
        return mMessage.get(EMERGENCY_FLAG);
    }

    public boolean isEncrypted()
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

    public String getSourceAddress()
    {
        return mMessage.getHex(SOURCE_ADDRESS, 6);
    }

    public String getSourceID()
    {
        return mMessage.getHex(SOURCE_ID, 6);
    }

    public String getSourceWACN()
    {
        return mMessage.getHex(SOURCE_WACN, 5);
    }

    public String getSourceSystemID()
    {
        return mMessage.getHex(SOURCE_SYSTEM_ID, 3);
    }

    public String getTargetAddress()
    {
        return mMessage.getHex(TARGET_ADDRESS, 6);
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
