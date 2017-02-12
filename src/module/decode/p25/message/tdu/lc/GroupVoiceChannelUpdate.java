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

import java.util.Set;
import java.util.TreeSet;

public class GroupVoiceChannelUpdate extends TDULinkControlMessage implements IdentifierReceiver
{
    public static final int[] IDENTIFIER_A = {72, 73, 74, 75};
    public static final int[] CHANNEL_A = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99};
    public static final int[] GROUP_ADDRESS_A = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139};
    public static final int[] IDENTIFIER_B = {140, 141, 142, 143};
    public static final int[] CHANNEL_B = {144, 145, 146, 147, 160, 161, 162, 163, 164, 165, 166, 167};
    public static final int[] GROUP_ADDRESS_B = {168, 169, 170, 171, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195};

    private IBandIdentifier mIdentifierUpdateA;
    private IBandIdentifier mIdentifierUpdateB;

    public GroupVoiceChannelUpdate(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.GROUP_VOICE_CHANNEL_UPDATE.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" CHAN A:" + getChannelA());

        sb.append(" GRP A:" + getGroupAddressA());

        if(hasChannelB())
        {
            sb.append(" CHAN B:" + getChannelA());

            sb.append(" GRP B:" + getGroupAddressA());
        }

        return sb.toString();
    }

    public int getChannelIdentifierA()
    {
        return mMessage.getInt(IDENTIFIER_A);
    }

    public int getChannelNumberA()
    {
        return mMessage.getInt(CHANNEL_A);
    }

    public String getChannelA()
    {
        return getChannelIdentifierA() + "-" + getChannelNumberA();
    }

    public int getChannelIdentifierB()
    {
        return mMessage.getInt(IDENTIFIER_B);
    }

    public int getChannelNumberB()
    {
        return mMessage.getInt(CHANNEL_B);
    }

    public String getChannelB()
    {
        return getChannelIdentifierB() + "-" + getChannelNumberB();
    }

    public boolean hasChannelB()
    {
        return getChannelNumberB() != 0;
    }

    public String getGroupAddressA()
    {
        return mMessage.getHex(GROUP_ADDRESS_A, 4);
    }

    public String getGroupAddressB()
    {
        return mMessage.getHex(GROUP_ADDRESS_B, 4);
    }

    /**
     * Returns a sorted list of addresses contained in the A and B fields
     *
     * @return
     */
    public Set<String> getAddresses()
    {
        Set<String> addresses = new TreeSet<String>();

        addresses.add(getGroupAddressA());
        addresses.add(getGroupAddressB());

        return addresses;
    }


    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getChannelIdentifierA())
        {
            mIdentifierUpdateA = message;
        }
        if(identifier == getChannelIdentifierB())
        {
            mIdentifierUpdateB = message;
        }
    }

    @Override
    public int[] getIdentifiers()
    {
        if(hasChannelB())
        {
            int[] identifiers = new int[2];
            identifiers[0] = getChannelIdentifierA();
            identifiers[1] = getChannelIdentifierB();

            return identifiers;
        }
        else
        {
            int[] identifiers = new int[1];
            identifiers[0] = getChannelIdentifierA();

            return identifiers;
        }
    }

    public long getDownlinkFrequencyA()
    {
        if(mIdentifierUpdateA != null)
        {
            return mIdentifierUpdateA.getDownlinkFrequency(getChannelNumberA());
        }

        return 0;
    }

    public long getUplinkFrequencyA()
    {
        if(mIdentifierUpdateA != null)
        {
            return mIdentifierUpdateA.getUplinkFrequency(getChannelNumberA());
        }

        return 0;
    }

    public long getDownlinkFrequencyB()
    {
        if(mIdentifierUpdateB != null)
        {
            return mIdentifierUpdateB.getDownlinkFrequency(getChannelNumberB());
        }

        return 0;
    }

    public long getUplinkFrequencyB()
    {
        if(mIdentifierUpdateB != null)
        {
            return mIdentifierUpdateB.getUplinkFrequency(getChannelNumberB());
        }

        return 0;
    }
}
