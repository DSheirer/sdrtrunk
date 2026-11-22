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
package io.github.dsheirer.module.decode.am;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.source.SourceEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the source event requests that the squelch controls use to get the current squelch settings.
 */
class AMDecoderSquelchRequestTest
{
    private static List<SourceEvent> events(DecodeConfigAM config, SourceEvent request)
    {
        AMDecoder decoder = new AMDecoder(config);
        List<SourceEvent> events = new ArrayList<>();
        decoder.setSourceEventListener(events::add);
        decoder.getSourceEventListener().receive(request);
        return events;
    }

    @Test
    void currentThresholdRequestIsAnswered()
    {
        DecodeConfigAM config = new DecodeConfigAM();
        config.setSquelchThreshold(-70);
        List<SourceEvent> events = events(config, SourceEvent.requestCurrentSquelchThreshold());
        assertEquals(1, events.size());
        assertEquals(SourceEvent.Event.NOTIFICATION_SQUELCH_THRESHOLD, events.getFirst().getEvent());
        assertEquals(-70.0, events.getFirst().getValue().doubleValue(), 0.01);
    }

    @Test
    void currentAutoTrackRequestIsAnswered()
    {
        DecodeConfigAM config = new DecodeConfigAM();
        config.setSquelchAutoTrack(false);
        List<SourceEvent> events = events(config, SourceEvent.requestCurrentSquelchAutoTrack());
        assertEquals(1, events.size());
        assertEquals(SourceEvent.Event.NOTIFICATION_SQUELCH_AUTO_TRACK, events.getFirst().getEvent());
        assertEquals(0, events.getFirst().getValue().intValue());
    }
}
