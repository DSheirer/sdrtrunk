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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannel;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelDFA;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelLookup;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.IChannelInformationReceiver;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Bandwidth;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallTimer;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import java.util.List;
import java.util.Map;

/**
 * Data call assignment
 */
public class DataCallAssignment extends DataCallWithOptionalLocation implements IChannelInformationReceiver
{
    private static final IntField CALL_TIMER = IntField.length6(OCTET_7);
    private static final IntField CHANNEL_NUMBER = IntField.length10(OCTET_7 + 6);
    private static final IntField BANDWIDTH = IntField.length2(OCTET_7 + 6);
    private static final IntField OFN = IntField.length16(OCTET_8);
    private static final IntField IFN = IntField.length16(OCTET_10);
    private static final int LOCATION_ID_OFFSET = OCTET_12;
    private NXDNChannel mChannel;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public DataCallAssignment(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    protected int getLocationOffset()
    {
        return LOCATION_ID_OFFSET;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        sb.append(getCallType()).append(" DATA CALL ASSIGNMENT");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getEncryptionKeyIdentifier());
        sb.append(getCallOption());
        if(hasChannel())
        {
            sb.append(" CHANNEL:").append(getChannel());
        }
        else
        {
            sb.append(" CHANNEL: NOT CONFIGURED");
        }
        sb.append(" TIMER:").append(getCallTimer());
        return sb.toString();
    }

    @Override
    public void receive(ChannelAccessInformation channelAccessInformation, Map<Integer, ChannelFrequency> channelFrequencyMap)
    {
        if(mChannel == null)
        {
            if(channelAccessInformation.isChannel())
            {
                mChannel = new NXDNChannelLookup(getMessage().getInt(CHANNEL_NUMBER));
            }
            else
            {
                mChannel = new NXDNChannelDFA(getMessage().getInt(OFN), getMessage().getInt(IFN),
                        Bandwidth.fromValue(getMessage().getInt(BANDWIDTH)));
            }
        }

        mChannel.receive(channelAccessInformation, channelFrequencyMap);
    }

    /**
     * Indicates if the channel information is configured.
     */
    public boolean hasChannel()
    {
        return getChannel() != null;
    }

    /**
     * Channel for the call
     */
    public NXDNChannel getChannel()
    {
        return mChannel;
    }

    /**
     * Call timer.
     */
    public CallTimer getCallTimer()
    {
        return CallTimer.fromValue(getMessage().getInt(CALL_TIMER));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
