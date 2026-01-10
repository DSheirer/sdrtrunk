/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.nxdn.layer3.broadcast;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannel;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelDFA;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelLookup;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Bandwidth;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelNotification;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import java.util.List;
import java.util.Map;

/**
 * Control Channel information for a site
 */
public class ControlChannelInformation extends NXDNLayer3Message implements IChannelInformationReceiver
{
    private static final int LOCATION_ID = OCTET_1;
    private LocationID mLocationID;
    private static final IntField FLAGS = IntField.length4(OCTET_4);

    //Channel Access = Channel Mode Fields
    private static final IntField CONTROL_CHANNEL_1 = IntField.length10(OCTET_4 + 6);
    private static final IntField CONTROL_CHANNEL_2 = IntField.length10(OCTET_6 + 6);

    //Channel Access = DFA Fields
    private static final int HAS_CHANNEL_1 = OCTET_4 + 4;
    private static final int HAS_CHANNEL_2 = OCTET_4 + 5;
    private static final IntField BANDWIDTH_1 = IntField.length2(OCTET_4 + 6);
    private static final IntField CONTROL_CHANNEL_1_OFN = IntField.length16(OCTET_5);
    private static final IntField CONTROL_CHANNEL_1_IFN = IntField.length16(OCTET_7);
    private static final IntField BANDWIDTH_2 = IntField.length2(OCTET_9 + 6);
    private static final IntField CONTROL_CHANNEL_2_OFN = IntField.length16(OCTET_10);
    private static final IntField CONTROL_CHANNEL_2_IFN = IntField.length16(OCTET_12);
    private NXDNChannel mChannel1;
    private NXDNChannel mChannel2;

    /**
     * Constructs an instance
     * @param message content
     * @param timestamp of the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public ControlChannelInformation(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append(getLocationID());
        sb.append(" ").append(getFlags()).append(" CONTROL");

        if(hasChannel1())
        {
            sb.append(" 1 ").append(getChannel1());
        }
        else
        {
            sb.append(" 1:NOT YET CONFIGURED");
        }

        if(hasChannel2())
        {
            sb.append(" 2 ").append(getChannel2());
        }

        return sb.toString();
    }

    /**
     * Indicates if this message has a channel 1 value.
     */
    public boolean hasChannel1()
    {
        return mChannel1 != null;
    }

    /**
     * Indicates if this message has a channel 2 value.
     */
    public boolean hasChannel2()
    {
        return mChannel2 != null;
    }

    /**
     * Channel 1
     * @return null if channel access info hasn't been received, or the channel 1 instance.
     */
    public NXDNChannel getChannel1()
    {
        if(mChannel1 == null)
        {
            return new NXDNChannelLookup(0);
        }

        return mChannel1;
    }

    /**
     * Channel 2
     * @return null if channel access info hasn't been received, message doesn't have a channel 2, or the channel 2 instance.
     */
    public NXDNChannel getChannel2()
    {
        if(mChannel2 == null)
        {
            return new NXDNChannelLookup(0);
        }

        return mChannel2;
    }

    @Override
    public void receive(ChannelAccessInformation channelAccessInformation, Map<Integer, ChannelFrequency> channelFrequencyMap)
    {
        if(channelAccessInformation != null)
        {
            if(channelAccessInformation.isDFA())
            {
                if(getMessage().get(HAS_CHANNEL_1))
                {
                    int ofn = getMessage().getInt(CONTROL_CHANNEL_1_OFN);
                    int ifn = getMessage().getInt(CONTROL_CHANNEL_1_IFN);
                    Bandwidth bandwidth = Bandwidth.fromValue(getMessage().getInt(BANDWIDTH_1));
                    mChannel1 = new NXDNChannelDFA(ofn, ifn, bandwidth);
                    mChannel1.receive(channelAccessInformation, channelFrequencyMap);
                }

                if(getMessage().get(HAS_CHANNEL_2))
                {
                    int ofn = getMessage().getInt(CONTROL_CHANNEL_2_OFN);
                    int ifn = getMessage().getInt(CONTROL_CHANNEL_2_IFN);
                    Bandwidth bandwidth = Bandwidth.fromValue(getMessage().getInt(BANDWIDTH_2));
                    mChannel2 = new NXDNChannelDFA(ofn, ifn, bandwidth);
                    mChannel2.receive(channelAccessInformation, channelFrequencyMap);
                }
            }
            else //Channel mode
            {
                int channel1 = getMessage().getInt(CONTROL_CHANNEL_1);
                mChannel1 = new NXDNChannelLookup(channel1);
                mChannel1.receive(channelAccessInformation, channelFrequencyMap);
                int channel2 = getMessage().getInt(CONTROL_CHANNEL_2);

                if(channel2 > 0)
                {
                    mChannel2 = new NXDNChannelLookup(channel2);
                    mChannel2.receive(channelAccessInformation, channelFrequencyMap);
                }
            }
        }
    }

    /**
     * Location ID field
     */
    public LocationID getLocationID()
    {
        if(mLocationID == null)
        {
            mLocationID = new LocationID(getMessage(), LOCATION_ID);
        }

        return mLocationID;
    }

    /**
     * Flags that amplify the meaning of the control channel information.
     */
    public ChannelNotification getFlags()
    {
        return ChannelNotification.fromValue(getMessage().getInt(FLAGS));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return getLocationID().getIdentifiers();
    }
}
