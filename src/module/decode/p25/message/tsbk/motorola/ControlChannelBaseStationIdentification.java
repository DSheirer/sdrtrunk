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

public class ControlChannelBaseStationIdentification extends MotorolaTSBKMessage implements IdentifierReceiver
{
    public static final int[] CHARACTER_1 = {80, 81, 82, 83, 84, 85};
    public static final int[] CHARACTER_2 = {86, 87, 88, 89, 90, 91};
    public static final int[] CHARACTER_3 = {92, 93, 94, 95, 96, 97};
    public static final int[] CHARACTER_4 = {98, 99, 100, 101, 102, 103};
    public static final int[] CHARACTER_5 = {104, 105, 106, 107, 108, 109};
    public static final int[] CHARACTER_6 = {110, 111, 112, 113, 114, 115};
    public static final int[] CHARACTER_7 = {116, 117, 118, 119, 120, 121};
    public static final int[] CHARACTER_8 = {122, 123, 124, 125, 126, 127};

    public static final int[] IDENTIFIER = {128, 129, 130, 131};
    public static final int[] CHANNEL = {132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IBandIdentifier mIdentifierUpdate;

    public ControlChannelBaseStationIdentification(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getEventType()
    {
        return MotorolaOpcode.CONTROL_CHANNEL_ID.getDescription();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" CHAN:" + getIdentifier() + "-" + getChannel());

        sb.append(" CWID:" + getCWID());

        sb.append(" DN:" + getDownlinkFrequency());

        sb.append(" UP:" + getUplinkFrequency());

        return sb.toString();
    }


    public String getCWID()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getCharacter(CHARACTER_1));
        sb.append(getCharacter(CHARACTER_2));
        sb.append(getCharacter(CHARACTER_3));
        sb.append(getCharacter(CHARACTER_4));
        sb.append(getCharacter(CHARACTER_5));
        sb.append(getCharacter(CHARACTER_6));
        sb.append(getCharacter(CHARACTER_7));
        sb.append(getCharacter(CHARACTER_8));

        return sb.toString();
    }

    private String getCharacter(int[] field)
    {
        int value = mMessage.getInt(field);

        if(value != 0)
        {
            return String.valueOf((char)(value + 43));
        }

        return null;
    }

    public int getIdentifier()
    {
        return mMessage.getInt(IDENTIFIER);
    }

    public int getChannel()
    {
        return mMessage.getInt(CHANNEL);
    }

    public long getUplinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            mIdentifierUpdate.getUplinkFrequency(getChannel());
        }

        return 0;
    }

    public long getDownlinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            mIdentifierUpdate.getDownlinkFrequency(getChannel());
        }

        return 0;
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

        identifiers[0] = getIdentifier();

        return identifiers;
    }
}
