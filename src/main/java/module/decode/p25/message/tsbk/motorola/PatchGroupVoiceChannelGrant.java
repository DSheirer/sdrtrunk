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
package module.decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BinaryMessage;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.reference.DataUnitID;

public class PatchGroupVoiceChannelGrant extends MotorolaTSBKMessage implements IdentifierReceiver
{
    /* Service Options */
    public static final int EMERGENCY_FLAG = 80;
    public static final int ENCRYPTED_CHANNEL_FLAG = 81;
    public static final int DUPLEX_MODE = 82;
    public static final int SESSION_MODE = 83;

    public static final int[] PRIORITY = {85, 86, 87};
    public static final int[] IDENTIFIER = {88, 89, 90, 91};
    public static final int[] CHANNEL = {92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] PATCH_GROUP_ADDRESS = {104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116,
        117, 118, 119};
    public static final int[] SOURCE_ADDRESS = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133,
        134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IBandIdentifier mIdentifierUpdate;

    public PatchGroupVoiceChannelGrant(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public boolean isTDMAChannel()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.isTDMA();
        }

        return false;
    }

    @Override
    public String getEventType()
    {
        return MotorolaOpcode.PATCH_GROUP_CHANNEL_GRANT.getLabel();
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

        sb.append(" PATCH GROUP:");
        sb.append(getPatchGroupAddress());
        sb.append(" FROM:");
        sb.append(getSourceAddress());
        sb.append(" PRI:");
        sb.append(getPriority());

        sb.append(" CHAN:");
        sb.append(getChannelIdentifier() + "-" + getChannelNumber());

        sb.append(" DN:" + getDownlinkFrequency());

        sb.append(" UP:" + getUplinkFrequency());

        sb.append(" SESSION MODE:");
        sb.append(getSessionMode().name());

        return sb.toString();
    }

    public String getPatchGroupAddress()
    {
        return mMessage.getHex(PATCH_GROUP_ADDRESS, 4);
    }

    public String getSourceAddress()
    {
        return mMessage.getHex(SOURCE_ADDRESS, 6);
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

    /**
     * 1 = Lowest, 4 = Default, 7 = Highest
     */
    public int getPriority()
    {
        return mMessage.getInt(PRIORITY);
    }

    public int getChannelIdentifier()
    {
        return mMessage.getInt(IDENTIFIER);
    }

    public int getChannelNumber()
    {
        return mMessage.getInt(CHANNEL);
    }

    public String getChannel()
    {
        return getChannelIdentifier() + "-" + getChannelNumber();
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
        identifiers[0] = getChannelIdentifier();

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
