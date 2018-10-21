/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sub-Network Dependent Convergence Protocol (SNDCP) Data Channel Announcement - Explicit
 */
public class SNDCPDataChannelAnnouncementExplicit extends OSPMessage implements FrequencyBandReceiver
{
    private static final int[] SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int AUTONOMOUS_ACCESS_FLAG = 24;
    private static final int REQUESTED_ACCESS_FLAG = 25;
    private static final int[] DOWNLINK_FREQUENCY_BAND = {32, 33, 34, 35};
    private static final int[] DOWNLINK_CHANNEL_NUMBER = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] UPLINK_FREQUENCY_BAND = {48, 49, 50, 51};
    private static final int[] UPLINK_CHANNEL_NUMBER = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] DATA_ACCESS_CONTROL = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private ServiceOptions mServiceOptions;
    private IAPCO25Channel mChannel;
    private List<IAPCO25Channel> mChannels;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public SNDCPDataChannelAnnouncementExplicit(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
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
        sb.append(" SERVICE OPTIONS:").append(getServiceOptions());
        return sb.toString();
    }

    public boolean isAutonomousAccess()
    {
        return getMessage().get(AUTONOMOUS_ACCESS_FLAG);
    }

    public boolean isRequestedAccess()
    {
        return getMessage().get(REQUESTED_ACCESS_FLAG);
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            if(isExplicitChannel())
            {
                mChannel = APCO25ExplicitChannel.create(getMessage().getInt(DOWNLINK_FREQUENCY_BAND),
                    getMessage().getInt(DOWNLINK_CHANNEL_NUMBER), getMessage().getInt(UPLINK_FREQUENCY_BAND),
                    getMessage().getInt(UPLINK_CHANNEL_NUMBER));
            }
            else
            {
                mChannel = APCO25Channel.create(getMessage().getInt(DOWNLINK_FREQUENCY_BAND),
                    getMessage().getInt(DOWNLINK_CHANNEL_NUMBER));
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
        return getMessage().getInt(UPLINK_CHANNEL_NUMBER) != 4095;
    }

    public int getDataAccessControl()
    {
        return getMessage().getInt(DATA_ACCESS_CONTROL);
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        if(mChannels == null)
        {
            mChannels = new ArrayList<>();
            mChannels.add(getChannel());
        }

        return mChannels;
    }
}
