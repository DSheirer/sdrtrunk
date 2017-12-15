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
package module.decode.p25.message.pdu.osp.control;

import alias.AliasList;
import bits.BinaryMessage;
import edac.CRCP25;
import module.decode.p25.message.IAdjacentSite;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.pdu.PDUMessage;
import module.decode.p25.message.tsbk.osp.control.SystemService;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;

public class AdjacentStatusBroadcastExtended extends PDUMessage implements IdentifierReceiver, IAdjacentSite
{
    public static final int[] LRA = {88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] SYSTEM_ID = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] RF_SUBSYSTEM_ID = {128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SITE_ID = {136, 137, 138, 139, 140, 141, 142, 143};
    public static final int[] TRANSMIT_IDENTIFIER = {160, 161, 162, 163};
    public static final int[] TRANSMIT_CHANNEL = {164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175};
    public static final int[] RECEIVE_IDENTIFIER = {176, 177, 178, 179};
    public static final int[] RECEIVE_CHANNEL = {180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191};
    public static final int[] SYSTEM_SERVICE_CLASS = {192, 193, 194, 195, 196, 197, 198, 199};
    public static final int[] MULTIPLE_BLOCK_CRC = {224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236,
        237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};

    private IBandIdentifier mTransmitIdentifierProvider;
    private IBandIdentifier mReceiveIdentifierProvider;

    public AdjacentStatusBroadcastExtended(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);

	    /* Header block is already error detected/corrected - perform error
         * detection correction on the intermediate and final data blocks */
        mMessage = CRCP25.correctPDU1(mMessage);
        mCRC[1] = mMessage.getCRC();
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

        sb.append(" RFSS:" + getRFSS());

        sb.append(" SITE:" + getSiteID());

        sb.append(" CTRL CHAN:" + getTransmitIdentifier() + "-" + getTransmitChannel());

        sb.append(" DN:" + getDownlinkFrequency());

        sb.append(" " + getReceiveIdentifier() + "-" + getReceiveChannel());

        sb.append(" UP:" + getUplinkFrequency());

        sb.append(" SYS SVC CLASS:" + getSystemServiceClass());

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

    public String getSystemID()
    {
        return mMessage.getHex(SYSTEM_ID, 3);
    }

    public String getSiteID()
    {
        return mMessage.getHex(SITE_ID, 2);
    }

    public int getTransmitIdentifier()
    {
        return mMessage.getInt(TRANSMIT_IDENTIFIER);
    }

    public int getTransmitChannel()
    {
        return mMessage.getInt(TRANSMIT_CHANNEL);
    }

    public int getReceiveIdentifier()
    {
        return mMessage.getInt(RECEIVE_IDENTIFIER);
    }

    public int getReceiveChannel()
    {
        return mMessage.getInt(RECEIVE_CHANNEL);
    }

    public long getDownlinkFrequency()
    {
        if(mTransmitIdentifierProvider != null)
        {
            return mTransmitIdentifierProvider.getDownlinkFrequency(getTransmitChannel());
        }

        return 0;
    }

    public long getUplinkFrequency()
    {
        if(mReceiveIdentifierProvider != null)
        {
            return mReceiveIdentifierProvider.getUplinkFrequency(getReceiveChannel());
        }

        return 0;
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getTransmitIdentifier())
        {
            mTransmitIdentifierProvider = message;
        }

        if(identifier == getReceiveIdentifier())
        {
            mReceiveIdentifierProvider = message;
        }
    }

    public int[] getIdentifiers()
    {
        int[] idens = new int[2];

        idens[0] = getTransmitIdentifier();
        idens[1] = getReceiveIdentifier();

        return idens;
    }

    @Override
    public String getRFSS()
    {
        return mMessage.getHex(RF_SUBSYSTEM_ID, 2);
    }

    @Override
    public String getLRA()
    {
        return mMessage.getHex(LRA, 2);
    }

    @Override
    public String getSystemServiceClass()
    {
        return SystemService.toString(mMessage.getInt(SYSTEM_SERVICE_CLASS));
    }

    @Override
    public String getDownlinkChannel()
    {
        return getTransmitIdentifier() + "-" + getTransmitChannel();
    }

    @Override
    public String getUplinkChannel()
    {
        return getReceiveIdentifier() + "-" + getReceiveChannel();
    }
}
