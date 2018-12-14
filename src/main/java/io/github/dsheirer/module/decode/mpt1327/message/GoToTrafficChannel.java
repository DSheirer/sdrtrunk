/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.module.decode.mpt1327.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.mpt1327.MPTMessageType;
import io.github.dsheirer.module.decode.mpt1327.channel.MPT1327Channel;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327Talkgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Go To Traffic Channel - directs a mobile/group to a traffic channel for a call
 */
public class GoToTrafficChannel extends MPT1327BaseMessage
{
    private static final int MUTE_AUDIO_FLAG = 86;
    private static final int[] CHANNEL_NUMBER = {87, 88, 89, 90, 91, 92, 93, 94, 95, 96};
    private static final int[] FROM_IDENT = {97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
    private static final int[] ALOHA_NUMBER = {110, 111};

    private MPT1327Channel mChannel;
    private MPT1327Talkgroup mFromTalkgroup;
    private List<Identifier> mIdentifiers;

    public GoToTrafficChannel(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public MPTMessageType getMessageType()
    {
        return MPTMessageType.GTC_GO_TO_TRAFFIC_CHANNEL;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(isMuteAudioCall() ? "DATA CALL" : "AUDIO CALL");
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" SITE:").append(getSystemIdentityCode());
        sb.append(" FROM:").append(getFromTalkgroup());
        sb.append(" TO:").append(getToTalkgroup());
        sb.append(" ALOHA NUMBER:").append(getAlohaNumber());
        return sb.toString();
    }

    /**
     * Indicates if the mobile radio(s) should mute audio once at the traffic channel indicating the call is a data
     * call.
     *
     * @return true if a data call (ie mute audio) or false if an audio call (unmute audio)
     */
    public boolean isMuteAudioCall()
    {
        return getMessage().get(MUTE_AUDIO_FLAG);
    }

    /**
     * Indicates if this is a data call
     */
    public boolean isDataCall()
    {
        return isMuteAudioCall();
    }

    /**
     * Indicates if this is an audio call
     */
    public boolean isAudioCall()
    {
        return !isMuteAudioCall();
    }

    /**
     * Traffic channel
     */
    public MPT1327Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = MPT1327Channel.create(getMessage().getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    /**
     * Calling/FROM talkgroup
     */
    public MPT1327Talkgroup getFromTalkgroup()
    {
        if(mFromTalkgroup == null)
        {
            mFromTalkgroup = MPT1327Talkgroup.createFrom(getPrefix(), getMessage().getInt(FROM_IDENT));
        }

        return mFromTalkgroup;
    }

    /**
     * Random access protocol aloha number
     */
    public int getAlohaNumber()
    {
        return getMessage().getInt(ALOHA_NUMBER);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode());
            mIdentifiers.add(getFromTalkgroup());
            mIdentifiers.add(getToTalkgroup());
        }

        return mIdentifiers;
    }
}
