/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.am;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.analog.AnalogDecoderState;
import io.github.dsheirer.protocol.Protocol;

/**
 * AM decoder state
 */
public class AMDecoderState extends AnalogDecoderState
{
    private String mChannelName;
    private Identifier mChannelNameIdentifier;
    private Identifier mTalkgroupIdentifier;

    /**
     * Constructs an instance
     * @param channelName to use for this channel
     * @param decodeConfig with talkgroup identifier
     */
    public AMDecoderState(String channelName, DecodeConfigAM decodeConfig)
    {
        mChannelName = (channelName != null && !channelName.isEmpty()) ? channelName : "AM CHANNEL";
        mChannelNameIdentifier = DecoderLogicalChannelNameIdentifier.create(mChannelName, Protocol.AM);
        mTalkgroupIdentifier = new AMTalkgroup(decodeConfig.getTalkgroup());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.AM;
    }

    @Override
    protected Identifier getChannelNameIdentifier()
    {
        return mChannelNameIdentifier;
    }

    @Override
    protected Identifier getTalkgroupIdentifier()
    {
        return mTalkgroupIdentifier;
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Activity Summary\n");
        sb.append("\tDecoder: AM");
        sb.append("\n\n");
        return sb.toString();
    }

}
