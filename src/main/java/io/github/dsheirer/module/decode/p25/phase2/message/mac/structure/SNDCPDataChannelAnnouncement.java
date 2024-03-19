/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import java.util.Collections;
import java.util.List;

/**
 * SNDCP data channel announcement
 */
public class SNDCPDataChannelAnnouncement extends MacStructureDataService implements IFrequencyBandReceiver
{
    private static final int AUTONOMOUS_ACCESS_FLAG = 16;
    private static final int REQUESTED_ACCESS_FLAG = 17;
    private static final IntField TRANSMIT_FREQUENCY_BAND = IntField.length4(OCTET_4_BIT_24);
    private static final IntField TRANSMIT_CHANNEL_NUMBER = IntField.length12(OCTET_4_BIT_24 + 4);
    private static final IntField RECEIVE_FREQUENCY_BAND = IntField.length4(OCTET_6_BIT_40);
    private static final IntField RECEIVE_CHANNEL_NUMBER = IntField.length12(OCTET_6_BIT_40 + 4);
    private static final IntField DATA_ACCESS_CONTROL = IntField.length16(OCTET_8_BIT_56);
    private List<Identifier> mIdentifiers;
    private APCO25Channel mChannel;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SNDCPDataChannelAnnouncement(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" CHAN:").append(getChannel());
        if(isAutonomousAccess() && isRequestedAccess())
        {
            sb.append(" AUTONOMOUS/REQUESTED-ACCESS");
        }
        else if(isAutonomousAccess())
        {
            sb.append(" AUTONOMOUS-ACCESS");
        }
        else if(isRequestedAccess())
        {
            sb.append(" REQUESTED-ACCESS");
        }
        sb.append(" DAC:").append(getDataAccessControl());
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    public boolean isAutonomousAccess()
    {
        return getMessage().get(AUTONOMOUS_ACCESS_FLAG + getOffset());
    }

    public boolean isRequestedAccess()
    {
        return getMessage().get(REQUESTED_ACCESS_FLAG + getOffset());
    }

    /**
     * Channel
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            if(isExplicitChannel())
            {
                mChannel = APCO25ExplicitChannel.create(getInt(TRANSMIT_FREQUENCY_BAND), getInt(TRANSMIT_CHANNEL_NUMBER),
                    getInt(RECEIVE_FREQUENCY_BAND), getInt(RECEIVE_CHANNEL_NUMBER));
            }
            else
            {
                mChannel = APCO25Channel.create(getInt(TRANSMIT_FREQUENCY_BAND), getInt(TRANSMIT_CHANNEL_NUMBER));
            }
        }

        return mChannel;
    }

    /**
     * Indicates if the channel is an explicit channel, meaning that there are separate downlink
     * and uplink channel numbers included.  When false, the channel number is the same for both
     * uplink and downlink.
     *
     * @return true if this is an explicit channel.
     */
    private boolean isExplicitChannel()
    {
        return getInt(RECEIVE_CHANNEL_NUMBER) != 4095;
    }

    public int getDataAccessControl()
    {
        return getInt(DATA_ACCESS_CONTROL);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = Collections.singletonList(getChannel());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
