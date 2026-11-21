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
package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.edacs.identifier.EDACSTalkgroup;
import io.github.dsheirer.module.decode.edacs.provoice.ProVoiceHeaderMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * The voice channel checks the talkgroup of each ProVoice header against the talkgroup of the
 * call.
 */
public class EDACSHeaderTalkgroupTest
{
    private static EDACSDecoderState voiceChannel(int talkgroup)
    {
        EDACSDecoderState state = new EDACSDecoderState(Channel.ChannelType.TRAFFIC, null, new DecodeConfigEDACS());
        state.getIdentifierCollection().update(EDACSTalkgroup.createTo(talkgroup));
        return state;
    }

    @Test
    void headersForTheCallsTalkgroupAreCounted()
    {
        EDACSDecoderState state = voiceChannel(273);
        state.receive(new ProVoiceHeaderMessage(273, 0));
        state.receive(new ProVoiceHeaderMessage(273, 1));
        assertArrayEquals(new int[]{2, 2, 0}, state.getHeaderCounts());
    }

    @Test
    void aHeaderForAnotherTalkgroupIsNoted()
    {
        EDACSDecoderState state = voiceChannel(273);
        state.receive(new ProVoiceHeaderMessage(273, 0));
        state.receive(new ProVoiceHeaderMessage(280, 1));
        state.receive(new ProVoiceHeaderMessage(280, 2));
        assertArrayEquals(new int[]{3, 1, 1}, state.getHeaderCounts());
    }

    @Test
    void theControlChannelAndACallWithoutATalkgroupIgnoreHeaders()
    {
        EDACSDecoderState control = new EDACSDecoderState(Channel.ChannelType.STANDARD, null, new DecodeConfigEDACS());
        control.getIdentifierCollection().update(EDACSTalkgroup.createTo(273));
        control.receive(new ProVoiceHeaderMessage(280, 0));
        assertArrayEquals(new int[]{0, 0, 0}, control.getHeaderCounts());

        EDACSDecoderState unknown = new EDACSDecoderState(Channel.ChannelType.TRAFFIC, null, new DecodeConfigEDACS());
        unknown.receive(new ProVoiceHeaderMessage(280, 0));
        assertArrayEquals(new int[]{0, 0, 0}, unknown.getHeaderCounts());
    }

    /**
     * Test: a heartbeat that arrives before the voice channel has started does not end the call (Cambridge
     * 2026-11-14/15: 328 of 12,189 calls were lost this way, logged as "voice channel open 0.0 s").
     */
    @Test
    void heartbeatBeforeStartDoesNotEndTheCall()
    {
        EDACSDecoderState state = voiceChannel(273);
        java.util.List<io.github.dsheirer.channel.state.DecoderStateEvent> events = new java.util.ArrayList<>();
        state.setDecoderStateListener(events::add);
        state.getHeartbeatListener().receive(new io.github.dsheirer.source.heartbeat.Heartbeat());
        state.getHeartbeatListener().receive(new io.github.dsheirer.source.heartbeat.Heartbeat());
        org.junit.jupiter.api.Assertions.assertEquals(0, events.size(), "nothing before start - in particular no END");

        state.start();
        state.getHeartbeatListener().receive(new io.github.dsheirer.source.heartbeat.Heartbeat());
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        org.junit.jupiter.api.Assertions.assertEquals(io.github.dsheirer.channel.state.DecoderStateEvent.Event.START,
            events.get(0).getEvent());
    }
}
