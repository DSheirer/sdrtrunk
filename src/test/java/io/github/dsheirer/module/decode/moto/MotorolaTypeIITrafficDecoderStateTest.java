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

package io.github.dsheirer.module.decode.moto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.moto.identifier.MotorolaTypeIIRadioIdentifier;
import io.github.dsheirer.module.decode.moto.identifier.MotorolaTypeIITalkgroup;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that an analog traffic channel keeps the identifiers of the grant that started it.
 */
class MotorolaTypeIITrafficDecoderStateTest
{
    private static final Identifier TALKGROUP = new MotorolaTypeIITalkgroup(33200);
    private static final Identifier RADIO = new MotorolaTypeIIRadioIdentifier(55631, Role.FROM);

    private static MotorolaTypeIITrafficDecoderState stateWithGrantIdentifiers(List<IDecodeEvent> events)
    {
        MotorolaTypeIITrafficDecoderState state = new MotorolaTypeIITrafficDecoderState("T-Test", new DecodeConfigNBFM());
        state.addDecodeEventListener(events::add);

        //The channel processing manager loads the grant identifiers this way before the channel starts.
        state.getConfigurationIdentifierListener().receive(new IdentifierUpdateNotification(TALKGROUP,
            IdentifierUpdateNotification.Operation.ADD, 0));
        state.getConfigurationIdentifierListener().receive(new IdentifierUpdateNotification(RADIO,
            IdentifierUpdateNotification.Operation.ADD, 0));
        return state;
    }

    private static void assertGrantIdentifiers(IDecodeEvent event)
    {
        assertEquals(List.of(TALKGROUP), event.getIdentifierCollection().getIdentifiers(Role.TO));
        assertEquals(List.of(RADIO), event.getIdentifierCollection().getIdentifiers(Role.FROM));
    }

    @Test
    void callEventHasGrantIdentifiersWhenSquelchOpensAfterIdle()
    {
        List<IDecodeEvent> events = new ArrayList<>();
        MotorolaTypeIITrafficDecoderState state = stateWithGrantIdentifiers(events);

        //Squelch is closed when the channel starts.
        state.receiveDecoderStateEvent(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
        state.receiveDecoderStateEvent(new DecoderStateEvent(this, Event.START, State.CALL));

        assertEquals(1, events.size());
        assertGrantIdentifiers(events.get(0));
    }

    @Test
    void callEventHasGrantIdentifiersAfterSquelchFlutter()
    {
        List<IDecodeEvent> events = new ArrayList<>();
        MotorolaTypeIITrafficDecoderState state = stateWithGrantIdentifiers(events);

        state.receiveDecoderStateEvent(new DecoderStateEvent(this, Event.START, State.CALL));
        state.receiveDecoderStateEvent(new DecoderStateEvent(this, Event.END, State.CALL));
        events.clear();
        state.receiveDecoderStateEvent(new DecoderStateEvent(this, Event.START, State.CALL));

        assertEquals(1, events.size());
        assertGrantIdentifiers(events.get(0));
        assertEquals(List.of(TALKGROUP), state.getIdentifierCollection().getIdentifiers(Role.TO));
    }
}
