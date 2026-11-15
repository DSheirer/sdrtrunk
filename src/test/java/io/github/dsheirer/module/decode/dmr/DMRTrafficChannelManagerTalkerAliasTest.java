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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.alias.DmrTalkerAliasIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.channel.DMRAbsoluteChannel;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that DMR channel grant events show the stored talker alias of the radio, learned from its earlier calls.
 */
public class DMRTrafficChannelManagerTalkerAliasTest
{
    private static String getAlias(IDecodeEvent event)
    {
        Identifier alias = event.getIdentifierCollection().getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS,
            Role.FROM);
        return alias != null ? alias.getValue().toString() : null;
    }

    private static MutableIdentifierCollection grant(int radio, int talkgroup)
    {
        MutableIdentifierCollection mic = new MutableIdentifierCollection();
        mic.update(DMRTalkgroup.create(talkgroup));
        mic.update(DMRRadio.createFrom(radio));
        return mic;
    }

    @Test
    public void channelGrantShowsTheStoredTalkerAlias()
    {
        DMRTrafficChannelManager manager = new DMRTrafficChannelManager(new Channel("Control"));
        List<IDecodeEvent> events = new ArrayList<>();
        manager.addDecodeEventListener(event -> {
            if(!events.contains(event))
            {
                events.add(event);
            }
        });
        manager.getTalkerAliasManager().update((RadioIdentifier)DMRRadio.createFrom(100),
            DmrTalkerAliasIdentifier.create("ENGINE 7"));

        //Radio 100 has a stored alias, radio 200 doesn't
        manager.processChannelGrant(new DMRAbsoluteChannel(1, 1, 0, 0), grant(100, 10),
            Opcode.STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT, 1000, false);
        manager.processChannelGrant(new DMRAbsoluteChannel(2, 1, 0, 0), grant(200, 20),
            Opcode.STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT, 1000, false);

        assertEquals(2, events.size());
        assertEquals("ENGINE 7", getAlias(events.get(0)));
        assertNull(getAlias(events.get(1)));

        //Repeated grants for the same call update the event instead of starting a new one
        manager.processChannelGrant(new DMRAbsoluteChannel(1, 1, 0, 0), grant(100, 10),
            Opcode.STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT, 1500, false);
        assertEquals(2, events.size());
        assertFalse(events.get(0).getDuration() < 0);
    }
}
