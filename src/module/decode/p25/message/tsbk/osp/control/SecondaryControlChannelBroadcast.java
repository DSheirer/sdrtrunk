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
package module.decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BinaryMessage;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;

public class SecondaryControlChannelBroadcast extends TSBKMessage implements IdentifierReceiver, Comparable<SecondaryControlChannelBroadcast>
{
    public static final int[] RFSS_ID = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int[] SITE_ID = {88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] IDENTIFIER_1 = {96, 97, 98, 99};
    public static final int[] CHANNEL_1 = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SYSTEM_SERVICE_CLASS_1 = {112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] IDENTIFIER_2 = {120, 121, 122, 123};
    public static final int[] CHANNEL_2 = {124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SYSTEM_SERVICE_CLASS_2 = {136, 137, 138, 139, 140, 141, 142, 143};

    private IBandIdentifier mIdentifierUpdate1;
    private IBandIdentifier mIdentifierUpdate2;

    public SecondaryControlChannelBroadcast(BinaryMessage message,
                                            DataUnitID duid,
                                            AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getEventType()
    {
        return Opcode.SECONDARY_CONTROL_CHANNEL_BROADCAST.getDescription();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" SITE:" + getRFSS() + "-" + getSiteID());

        sb.append(" CHAN1:" + getIdentifier1() + "-" + getChannel1());

        sb.append(" DN1:" + getDownlinkFrequency1());

        sb.append(" UP1:" + getUplinkFrequency1());

        sb.append(" SVC1:" + SystemService.toString(getSystemServiceClass1()));

        if(hasChannel2())
        {
            sb.append(" CHAN2:" + getIdentifier2() + "-" + getChannel2());

            sb.append(" DN2:" + getDownlinkFrequency2());

            sb.append(" UP2:" + getUplinkFrequency2());

            sb.append(" SVC2:" + SystemService.toString(getSystemServiceClass2()));
        }

        return sb.toString();
    }

    public String getRFSS()
    {
        return mMessage.getHex(RFSS_ID, 2);
    }

    public String getSiteID()
    {
        return mMessage.getHex(SITE_ID, 2);
    }

    public int getIdentifier1()
    {
        return mMessage.getInt(IDENTIFIER_1);
    }

    public int getChannel1()
    {
        return mMessage.getInt(CHANNEL_1);
    }

    public int getSystemServiceClass1()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_1);
    }

    public int getIdentifier2()
    {
        return mMessage.getInt(IDENTIFIER_2);
    }

    public int getChannel2()
    {
        return mMessage.getInt(CHANNEL_2);
    }

    public int getSystemServiceClass2()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_2);
    }

    public boolean hasChannel2()
    {
        return getSystemServiceClass2() != 0;
    }


    public long getDownlinkFrequency1()
    {
        if(mIdentifierUpdate1 != null)
        {
            return mIdentifierUpdate1.getDownlinkFrequency(getChannel1());
        }

        return 0;
    }

    public long getUplinkFrequency1()
    {
        if(mIdentifierUpdate1 != null)
        {
            return mIdentifierUpdate1.getUplinkFrequency(getChannel1());
        }

        return 0;
    }

    public long getDownlinkFrequency2()
    {
        if(mIdentifierUpdate2 != null)
        {
            return mIdentifierUpdate2.getDownlinkFrequency(getChannel2());
        }

        return 0;
    }

    public long getUplinkFrequency2()
    {
        if(mIdentifierUpdate2 != null)
        {
            return mIdentifierUpdate2.getUplinkFrequency(getChannel2());
        }

        return 0;
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getIdentifier1())
        {
            mIdentifierUpdate1 = message;
        }

        if(identifier == getIdentifier2())
        {
            mIdentifierUpdate2 = message;
        }
    }

    public int[] getIdentifiers()
    {
        int[] idens;

        if(hasChannel2())
        {
            idens = new int[2];

            idens[0] = getIdentifier1();
            idens[1] = getIdentifier2();
        }
        else
        {
            idens = new int[1];

            idens[0] = getIdentifier1();
        }

        return idens;
    }

    @Override
    public int compareTo(SecondaryControlChannelBroadcast other)
    {
        if(other.getRFSS().contentEquals(getRFSS()) &&
            other.getSiteID().contentEquals(getSiteID()) &&
            other.getIdentifier1() == getIdentifier1() &&
            other.getChannel1() == getChannel1())
        {
            return 0;
        }
        else if(other.getIdentifier1() < getIdentifier1())
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof SecondaryControlChannelBroadcast))
        {
            return false;
        }

        SecondaryControlChannelBroadcast that = (SecondaryControlChannelBroadcast)o;

        if(getRFSS() != that.getRFSS())
        {
            return false;
        }
        if(getSiteID() != that.getSiteID())
        {
            return false;
        }
        if(getIdentifier1() != that.getIdentifier1())
        {
            return false;
        }

        return getChannel1() == that.getChannel1();
    }

    @Override
    public int hashCode()
    {
        int result = getRFSS() != null ? getRFSS().hashCode() : 0;
        result = 31 * result + getSiteID() != null ? getSiteID().hashCode() : 0;
        result = 31 * result + getIdentifier1();
        result = 31 * result + getChannel1();
        return result;
    }
}
