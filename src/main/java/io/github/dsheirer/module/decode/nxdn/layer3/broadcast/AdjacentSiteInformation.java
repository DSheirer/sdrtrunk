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
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import java.util.List;
import java.util.Map;

/**
 * Adjacent (ie neighbor) site information
 */
public class AdjacentSiteInformation extends NXDNLayer3Message implements IChannelInformationReceiver
{
    private static final int LOCATION_ID_1 = OCTET_1;
    private static final IntField NEIGHBOR_ID_1 = IntField.length4(OCTET_4 + 2);

    //Channel Mode Fields
    private static final IntField CHANNEL_1 = IntField.length10(OCTET_4 + 6);
    private static final int LOCATION_ID_2 = OCTET_6;
    private static final IntField NEIGHBOR_ID_2 = IntField.length4(OCTET_9 + 2);
    private static final IntField CHANNEL_2 = IntField.length10(OCTET_9 + 6);
    private static final int LOCATION_ID_3 = OCTET_11;
    private static final IntField NEIGHBOR_ID_3 = IntField.length4(OCTET_14 + 2);
    private static final IntField CHANNEL_3 = IntField.length10(OCTET_14 + 6);
    private static final int LOCATION_ID_4 = OCTET_16;
    private static final IntField NEIGHBOR_ID_4 = IntField.length4(OCTET_19 + 2);
    private static final IntField CHANNEL_4 = IntField.length10(OCTET_19 + 6);

    //DFA Mode Fields
    private static final IntField DFA_BANDWIDTH_1 = IntField.length2(OCTET_4 + 6);
    private static final IntField DFA_CHANNEL_1 = IntField.length16(OCTET_5);
    private static final int DFA_LOCATION_ID_2 = OCTET_7;
    private static final IntField DFA_NEIGHBOR_ID_2 = IntField.length4(OCTET_10 + 2);
    private static final IntField DFA_BANDWIDTH_2 = IntField.length2(OCTET_10 + 6);
    private static final IntField DFA_CHANNEL_2 = IntField.length16(OCTET_11);
    private static final int DFA_LOCATION_ID_3 = OCTET_13;
    private static final IntField DFA_NEIGHBOR_ID_3 = IntField.length4(OCTET_16 + 2);
    private static final IntField DFA_BANDWIDTH_3 = IntField.length2(OCTET_16 + 6);
    private static final IntField DFA_CHANNEL_3 = IntField.length16(OCTET_17);

    private LocationID mLocationID1;
    private LocationID mLocationID2;
    private LocationID mLocationID3;
    private LocationID mLocationID4;

    private NXDNChannel mChannel1;
    private NXDNChannel mChannel2;
    private NXDNChannel mChannel3;
    private NXDNChannel mChannel4;

    private int mNeighborId2;
    private int mNeighborId3;
    private int mNeighborId4;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public AdjacentSiteInformation(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("ADJACENT SITE ").append(getNeighborId1()).append(" [").append(getLocation1());
        if(hasChannel1())
        {
            sb.append(" CHAN:").append(getChannel1());

            if(hasChannel2())
            {
                sb.append("] SITE ").append(getNeighborId2()).append(" [").append(getLocation2());
                sb.append(" CHAN:").append(getChannel2());

                if(hasChannel3())
                {
                    sb.append("] SITE ").append(getNeighborId3()).append(" [").append(getLocation3());
                    sb.append(" CHAN:").append(getChannel3());

                    if(hasChannel4())
                    {
                        sb.append("] SITE ").append(getNeighborId4()).append(" [").append(getLocation4());
                        sb.append(" CHAN:").append(getChannel4());
                    }
                    else
                    {
                        sb.append("]");
                    }
                }
                else
                {
                    sb.append("]");
                }
            }
            else
            {
                sb.append("]");
            }
        }

        return sb.toString();
    }

    @Override
    public void receive(ChannelAccessInformation channelAccessInformation, Map<Integer, ChannelFrequency> channelFrequencyMap)
    {
        if(channelAccessInformation != null)
        {
            if(channelAccessInformation.isChannel()) //Channel Mode
            {
                mChannel1 = new NXDNChannelLookup(getMessage().getInt(CHANNEL_1));
                mChannel1.receive(channelAccessInformation, channelFrequencyMap);

                mNeighborId2 = getMessage().getInt(NEIGHBOR_ID_2);

                if(mNeighborId2 > 0)
                {
                    mLocationID2 = new LocationID(getMessage(), LOCATION_ID_2);
                    mChannel2 = new NXDNChannelLookup(getMessage().getInt(CHANNEL_2));
                    mChannel2.receive(channelAccessInformation, channelFrequencyMap);

                    mNeighborId3 = getMessage().getInt(NEIGHBOR_ID_3);

                    if(mNeighborId3 > 0)
                    {
                        mLocationID3 = new LocationID(getMessage(), LOCATION_ID_3);
                        mChannel3 = new NXDNChannelLookup(getMessage().getInt(CHANNEL_3));
                        mChannel3.receive(channelAccessInformation, channelFrequencyMap);

                        mNeighborId4 = getMessage().getInt(NEIGHBOR_ID_4);

                        if(mNeighborId4 > 0)
                        {
                            mLocationID4 = new LocationID(getMessage(), LOCATION_ID_4);
                            mChannel4 = new NXDNChannelLookup(getMessage().getInt(CHANNEL_4));
                            mChannel4.receive(channelAccessInformation, channelFrequencyMap);
                        }
                    }
                }
            }
            else //DFA Mode
            {
                mChannel1 = new NXDNChannelDFA(getMessage().getInt(DFA_CHANNEL_1), 0,
                        Bandwidth.fromValue(getMessage().getInt(DFA_BANDWIDTH_1)));
                mChannel1.receive(channelAccessInformation, channelFrequencyMap);

                mNeighborId2 = getMessage().getInt(DFA_NEIGHBOR_ID_2);

                if(mNeighborId2 > 0)
                {
                    mLocationID2 = new LocationID(getMessage(), DFA_LOCATION_ID_2);
                    mChannel2 = new NXDNChannelDFA(getMessage().getInt(DFA_CHANNEL_2), 0,
                            Bandwidth.fromValue(getMessage().getInt(DFA_BANDWIDTH_2)));
                    mChannel2.receive(channelAccessInformation, channelFrequencyMap);

                    mNeighborId3 = getMessage().getInt(DFA_NEIGHBOR_ID_3);

                    if(mNeighborId3 > 0)
                    {
                        mLocationID3 = new LocationID(getMessage(), DFA_LOCATION_ID_3);
                        mChannel3 = new NXDNChannelDFA(getMessage().getInt(DFA_CHANNEL_3), 0,
                                Bandwidth.fromValue(getMessage().getInt(DFA_BANDWIDTH_3)));
                        mChannel3.receive(channelAccessInformation, channelFrequencyMap);
                    }
                }
            }
        }
    }

    public Neighbor getNeighbor1()
    {
        if(hasChannel1())
        {
            return new Neighbor(getNeighborId1(), getLocation1(), getChannel1());
        }

        return null;
    }

    public Neighbor getNeighbor2()
    {
        if(hasChannel2())
        {
            return new Neighbor(getNeighborId2(), getLocation2(), getChannel2());
        }

        return null;
    }

    public Neighbor getNeighbor3()
    {
        if(hasChannel3())
        {
            return new Neighbor(getNeighborId3(), getLocation3(), getChannel3());
        }

        return null;
    }

    public Neighbor getNeighbor4()
    {
        if(hasChannel4())
        {
            return new Neighbor(getNeighborId4(), getLocation4(), getChannel4());
        }

        return null;
    }

    /**
     * Neighbor ID for neighbor 1
     */
    public int getNeighborId1()
    {
        return getMessage().getInt(NEIGHBOR_ID_1);
    }

    /**
     * Neighbor ID for neighbor 2
     */
    public int getNeighborId2()
    {
        return mNeighborId2;
    }

    /**
     * Neighbor ID for neighbor 3
     */
    public int getNeighborId3()
    {
        return mNeighborId3;
    }

    /**
     * Neighbor ID for neighbor 4
     */
    public int getNeighborId4()
    {
        return mNeighborId4;
    }

    /**
     * Location 1
     * @return location
     */
    public LocationID getLocation1()
    {
        if(mLocationID1 == null)
        {
            mLocationID1 = new LocationID(getMessage(), LOCATION_ID_1);
        }

        return mLocationID1;
    }

    /**
     * Location 2
     * @return location or null
     */
    public LocationID getLocation2()
    {
        return mLocationID2;
    }

    /**
     * Location 3
     * @return location or null
     */
    public LocationID getLocation3()
    {
        return mLocationID3;
    }

    /**
     * Location 4
     * @return location or null
     */
    public LocationID getLocation4()
    {
        return mLocationID4;
    }

    /**
     * Indicates if the channel is available.  Note: channel info isn't available until the channel access info is set.
     */
    public boolean hasChannel1()
    {
        return mChannel1 != null;
    }

    /**
     * Channel 1
     * @return channel or null.
     */
    public NXDNChannel getChannel1()
    {
        return mChannel1;
    }

    /**
     * Channel 2
     * @return channel or null.
     */
    public NXDNChannel getChannel2()
    {
        return mChannel2;
    }

    /**
     * Channel 3
     * @return channel or null.
     */
    public NXDNChannel getChannel3()
    {
        return mChannel3;
    }

    /**
     * Channel 4
     * @return channel or null.
     */
    public NXDNChannel getChannel4()
    {
        return mChannel4;
    }

    /**
     * Indicates if the channel is available.  Note: channel info isn't available until the channel access info is set.
     */
    public boolean hasChannel2()
    {
        return mChannel2 != null;
    }

    /**
     * Indicates if the channel is available.  Note: channel info isn't available until the channel access info is set.
     */
    public boolean hasChannel3()
    {
        return mChannel3 != null;
    }

    /**
     * Indicates if the channel is available.  Note: channel info isn't available until the channel access info is set.
     */
    public boolean hasChannel4()
    {
        return mChannel4 != null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
