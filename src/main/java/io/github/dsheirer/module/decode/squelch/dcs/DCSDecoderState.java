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

package io.github.dsheirer.module.decode.squelch.dcs;

import io.github.dsheirer.module.decode.event.ActivitySummary;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the decoder state from the stream of decoded DCS code messages.
 */
public class DCSDecoderState extends DecoderState
{
    private Map<DCSCode,Integer> mDcsCodeCountsMap = new ConcurrentHashMap<>();
    private DCSCode mCurrentCode = DCSCode.UNKNOWN;

    /**
     * Constructs an instance
     */
    public DCSDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DCS;
    }


    @Override
    public void receive(IMessage message)
    {
        if(message instanceof DCSMessage dcsMessage)
        {
            DCSCode code = dcsMessage.getDCSCode();

            //Status messages (e.g. code lost) carry no code: don't count them, otherwise a null key ends up in the
            //counts map and the activity summary throws a NullPointerException.
            if(code != null)
            {
                mCurrentCode = code;
                mDcsCodeCountsMap.merge(code, 1, Integer::sum);
            }

            getIdentifierCollection().update(dcsMessage.getIdentifiers());
        }
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET:
                resetState();
                break;
            default:
                break;
        }
    }

    @Override
    public String getActivitySummary()
    {
        ActivitySummary summary = new ActivitySummary("Digital Coded Squelch (DCS)");
        summary.section("Detected Codes");
        ActivitySummary.Table table = summary.table("Code", "Count", "Current");

        for(Map.Entry<DCSCode,Integer> entry: mDcsCodeCountsMap.entrySet())
        {
            table.row(entry.getKey(), entry.getValue(), entry.getKey().equals(mCurrentCode) ? "Yes" : null);
        }

        return summary.toString();
    }

    @Override
    public void init()
    {
    }
}
