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
package module.decode.p25.message.tsbk;

import alias.AliasList;
import bits.BinaryMessage;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.reference.DataUnitID;

public abstract class GroupMultiChannelGrant extends ChannelGrant implements IdentifierReceiver
{
    public static final int[] CHANNEL_IDENTIFIER_1 = {80, 81, 82, 83};
    public static final int[] CHANNEL_NUMBER_1 = {84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] GROUP_ADDRESS_1 = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] CHANNEL_IDENTIFIER_2 = {112, 113, 114, 115};
    public static final int[] CHANNEL_NUMBER_2 = {116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127};
    public static final int[] GROUP_ADDRESS_2 = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IBandIdentifier mIdentifierUpdate1;
    private IBandIdentifier mIdentifierUpdate2;

    public GroupMultiChannelGrant(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" CHAN1:");
        sb.append(getChannelIdentifier1() + "/" + getChannelNumber1());

        sb.append(" GRP1:");
        sb.append(getGroupAddress1());

        if(hasChannelNumber2())
        {
            sb.append(" CHAN2:");
            sb.append(getChannelIdentifier2() + "/" + getChannelNumber2());

            sb.append(" GRP2:");
            sb.append(getGroupAddress2());
        }

        return sb.toString();
    }

    public int getChannelIdentifier1()
    {
        return mMessage.getInt(CHANNEL_IDENTIFIER_1);
    }

    public int getChannelNumber1()
    {
        return mMessage.getInt(CHANNEL_NUMBER_1);
    }

    public String getChannel1()
    {
        return getChannelIdentifier1() + "-" + getChannelNumber1();
    }

    public String getGroupAddress1()
    {
        return mMessage.getHex(GROUP_ADDRESS_1, 4);
    }

    public int getChannelIdentifier2()
    {
        return mMessage.getInt(CHANNEL_IDENTIFIER_2);
    }

    public int getChannelNumber2()
    {
        return mMessage.getInt(CHANNEL_NUMBER_2);
    }

    public String getChannel2()
    {
        return getChannelIdentifier2() + "-" + getChannelNumber2();
    }

    public String getGroupAddress2()
    {
        return mMessage.getHex(GROUP_ADDRESS_2, 4);
    }

    public boolean hasChannelNumber2()
    {
        return mMessage.getInt(CHANNEL_NUMBER_2) !=
            mMessage.getInt(CHANNEL_NUMBER_1);
    }

    @Override
    public String getFromID()
    {
        return getGroupAddress1();
    }

    @Override
    public String getToID()
    {
        return getGroupAddress2();
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getChannelIdentifier1())
        {
            mIdentifierUpdate1 = message;
        }

        if(hasChannelNumber2() && identifier == getChannelIdentifier2())
        {
            mIdentifierUpdate2 = message;
        }
    }

    @Override
    public int[] getIdentifiers()
    {
        int[] identifiers;

        if(hasChannelNumber2())
        {
            identifiers = new int[2];
            identifiers[0] = getChannelIdentifier1();
            identifiers[1] = getChannelIdentifier2();
        }
        else
        {
            identifiers = new int[1];
            identifiers[0] = getChannelIdentifier1();
        }

        return identifiers;
    }

    public long getDownlinkFrequency1()
    {
        if(mIdentifierUpdate1 != null)
        {
            return mIdentifierUpdate1.getDownlinkFrequency(getChannelNumber1());
        }

        return 0;
    }

    public boolean isTDMAChannel1()
    {
        if(mIdentifierUpdate1 != null)
        {
            return mIdentifierUpdate1.isTDMA();
        }

        return false;
    }

    public long getUplinkFrequency1()
    {
        if(mIdentifierUpdate1 != null)
        {
            return mIdentifierUpdate1.getUplinkFrequency(getChannelNumber1());
        }

        return 0;
    }

    public long getDownlinkFrequency2()
    {
        if(mIdentifierUpdate2 != null)
        {
            return mIdentifierUpdate2.getDownlinkFrequency(getChannelNumber2());
        }

        return 0;
    }

    public boolean isTDMAChannel2()
    {
        if(mIdentifierUpdate2 != null)
        {
            return mIdentifierUpdate2.isTDMA();
        }

        return false;
    }

    public long getUplinkFrequency2()
    {
        if(mIdentifierUpdate2 != null)
        {
            return mIdentifierUpdate2.getUplinkFrequency(getChannelNumber2());
        }

        return 0;
    }

}
