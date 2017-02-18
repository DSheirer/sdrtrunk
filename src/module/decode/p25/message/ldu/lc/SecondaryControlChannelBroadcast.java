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
import module.decode.p25.message.tsbk.osp.control.SystemService;
import module.decode.p25.reference.LinkControlOpcode;

public class SecondaryControlChannelBroadcast extends LDU1Message implements IdentifierReceiver
{
    public static final int[] RFSS_ID = {364, 365, 366, 367, 372, 373, 374, 375};
    public static final int[] SITE_ID = {376, 377, 382, 383, 384, 385, 386, 387};
    public static final int[] IDENTIFIER_A = {536, 537, 538, 539};
    public static final int[] CHANNEL_A = {540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] SYSTEM_SERVICE_CLASS_A = {560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] IDENTIFIER_B = {720, 721, 722, 723};
    public static final int[] CHANNEL_B = {724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743};
    public static final int[] SYSTEM_SERVICE_CLASS_B = {744, 745, 750, 751, 752, 753, 754, 755};

    private IBandIdentifier mIdentifierProviderA;
    private IBandIdentifier mIdentifierProviderB;

    public SecondaryControlChannelBroadcast(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.SECONDARY_CONTROL_CHANNEL_BROADCAST.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" SITE:" + getRFSubsystemID() + "-" + getSiteID());

        sb.append(" CHAN A:" + getChannelA());

        sb.append(" " + SystemService.toString(getSystemServiceClassA()));

        sb.append(" CHAN B:" + getChannelB());

        sb.append(" " + SystemService.toString(getSystemServiceClassB()));

        return sb.toString();
    }

    public String getRFSubsystemID()
    {
        return mMessage.getHex(RFSS_ID, 2);
    }

    public String getSiteID()
    {
        return mMessage.getHex(SITE_ID, 2);
    }

    public int getIdentifierA()
    {
        return mMessage.getInt(IDENTIFIER_A);
    }

    public int getChannelA()
    {
        return mMessage.getInt(CHANNEL_A);
    }

    public String getChannelNumberA()
    {
        return getIdentifierA() + "-" + getChannelA();
    }

    public int getIdentifierB()
    {
        return mMessage.getInt(IDENTIFIER_B);
    }

    public int getChannelB()
    {
        return mMessage.getInt(CHANNEL_B);
    }

    public String getChannelNumberB()
    {
        return getIdentifierB() + "-" + getChannelB();
    }

    public int getSystemServiceClassA()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_A);
    }

    public int getSystemServiceClassB()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS_B);
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        if(identifier == getIdentifierA())
        {
            mIdentifierProviderA = message;
        }
        if(identifier == getIdentifierB())
        {
            mIdentifierProviderB = message;
        }
    }

    @Override
    public int[] getIdentifiers()
    {
        int[] identifiers = new int[2];

        identifiers[0] = getIdentifierA();
        identifiers[1] = getIdentifierB();

        return identifiers;
    }

    public long getDownlinkFrequencyA()
    {
        if(mIdentifierProviderA != null)
        {
            return mIdentifierProviderA.getDownlinkFrequency(getChannelA());
        }

        return 0;
    }

    public long getUplinkFrequencyA()
    {
        if(mIdentifierProviderA != null)
        {
            return mIdentifierProviderA.getUplinkFrequency(getChannelA());
        }

        return 0;
    }

    public long getDownlinkFrequencyB()
    {
        if(mIdentifierProviderB != null)
        {
            return mIdentifierProviderB.getDownlinkFrequency(getChannelB());
        }

        return 0;
    }

    public long getUplinkFrequencyB()
    {
        if(mIdentifierProviderB != null)
        {
            return mIdentifierProviderB.getUplinkFrequency(getChannelB());
        }

        return 0;
    }
}
