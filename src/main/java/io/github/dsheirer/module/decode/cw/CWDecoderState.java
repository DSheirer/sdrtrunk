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

package io.github.dsheirer.module.decode.cw;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.ActivitySummary;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Morse code (CW) station ID decoder state.  Each decoded station ID is broadcast as a STATION_ID decode event with
 * the call sign as the FROM identifier.  This decoder state does not broadcast channel state changes: the NBFM decoder
 * (noise squelch) controls the channel state.
 */
public class CWDecoderState extends DecoderState
{
    private final Map<String,StationActivity> mStations = new LinkedHashMap<>();

    /**
     * Constructs an instance
     */
    public CWDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.CW_ID;
    }

    @Override
    public void init()
    {
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof CWMessage cw)
        {
            synchronized(mStations)
            {
                mStations.computeIfAbsent(cw.getCallSign(), StationActivity::new).update(cw);
            }

            getIdentifierCollection().update(cw.getIdentifiers());

            broadcast(DecodeEvent.builder(DecodeEventType.STATION_ID, cw.getTimestamp())
                .channel(getCurrentChannel())
                .details("CW ID: " + cw.getText() + " (" + cw.getToneFrequency() + " Hz, " +
                    cw.getWordsPerMinute() + " WPM)")
                .identifiers(getIdentifierCollection().copyOf())
                .protocol(Protocol.CW)
                .build());

            getIdentifierCollection().remove(IdentifierClass.USER);
        }
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getEvent() == DecoderStateEvent.Event.REQUEST_RESET)
        {
            resetState();
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        resetState();
    }

    @Override
    public String getActivitySummary()
    {
        ActivitySummary summary = new ActivitySummary("CW Station ID");

        synchronized(mStations)
        {
            if(mStations.isEmpty())
            {
                summary.field("Station IDs", ActivitySummary.NONE);
            }
            else
            {
                summary.section("Station IDs");
                ActivitySummary.Table table = summary.table("Call Sign", "Count", "Tone", "Speed", "Last Text");

                for(StationActivity station: mStations.values())
                {
                    table.row(station.mCallSign, station.mCount, station.mToneFrequency + " Hz",
                        station.mWordsPerMinute + " WPM", station.mLastText);
                }
            }
        }

        return summary.toString();
    }

    /**
     * Activity for a station call sign
     */
    private static class StationActivity
    {
        private final String mCallSign;
        private int mCount;
        private int mToneFrequency;
        private int mWordsPerMinute;
        private String mLastText;

        StationActivity(String callSign)
        {
            mCallSign = callSign;
        }

        void update(CWMessage message)
        {
            mCount++;
            mToneFrequency = message.getToneFrequency();
            mWordsPerMinute = message.getWordsPerMinute();
            mLastText = message.getText();
        }
    }
}
