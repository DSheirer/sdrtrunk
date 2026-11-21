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
package io.github.dsheirer.event;

import io.github.dsheirer.database.IdentifierRecord;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Event manager: one event key per decode event, snapshots when the event changes (with or without a re-broadcast),
 * and the record fields extracted from the event.
 */
public class EventManagerTest
{
    private EventManager mEventManager;
    private final List<EventRecord> mSnapshots = new ArrayList<>();

    @BeforeEach
    public void setup()
    {
        mEventManager = new EventManager(new Settings().getTalkgroupFormatSettings(), null);
        mEventManager.addEventListener(mSnapshots::add);
    }

    @AfterEach
    public void teardown()
    {
        mEventManager.stop();
    }

    private static DecodeEvent groupCall(long start, int talkgroup, int radio)
    {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(APCO25Talkgroup.create(talkgroup));
        identifiers.add(APCO25RadioIdentifier.createFrom(radio));
        identifiers.add(new AliasListConfigurationIdentifier("Metro"));
        identifiers.add(new SystemConfigurationIdentifier("Metro System"));
        return DecodeEvent.builder(DecodeEventType.CALL_GROUP, start)
            .identifiers(new IdentifierCollection(identifiers))
            .protocol(Protocol.APCO25)
            .details("Phase 1")
            .build();
    }

    @Test
    public void eventChangesAreSnapshotsOfOneEvent()
    {
        DecodeEvent event = groupCall(1000, 100, 5551212);
        mEventManager.receive(event);
        mEventManager.process();
        assertEquals(1, mSnapshots.size());

        EventRecord first = mSnapshots.getFirst();
        assertEquals("CALL_GROUP", first.getEventType());
        assertEquals("APCO25", first.getProtocol());
        assertEquals(100L, first.getToValue());
        assertEquals(IdentifierRecord.FORM_TALKGROUP, first.getToForm());
        assertEquals(5551212L, first.getFromValue());
        assertTrue(first.isFromRadio());
        assertEquals("Metro", first.getAliasList());
        assertEquals("Metro System", first.getSystem());
        assertEquals("Phase 1", first.getDetails());
        assertEquals(-1, first.getTimeslot());

        //No change, no snapshot
        mEventManager.process();
        assertEquals(1, mSnapshots.size());

        //The decoder updates the event without broadcasting it again
        event.end(4000);
        mEventManager.process();
        assertEquals(2, mSnapshots.size());
        assertEquals(3000, mSnapshots.get(1).getDuration());
        assertEquals(first.getEventKey(), mSnapshots.get(1).getEventKey(), "same event key");

        //A re-broadcast of the same event doesn't create a new event
        mEventManager.receive(event);
        mEventManager.process();
        assertEquals(2, mSnapshots.size());
        assertEquals(1, mEventManager.getActiveEventCount());

        mEventManager.receive(groupCall(5000, 200, 777));
        mEventManager.process();
        assertEquals(3, mSnapshots.size());
        assertNotEquals(first.getEventKey(), mSnapshots.get(2).getEventKey(), "different event, different key");
    }

    @Test
    public void patchGroupMembersAreCaptured()
    {
        PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(500));
        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(100));
        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(101));
        DecodeEvent event = DecodeEvent.builder(DecodeEventType.CALL_PATCH_GROUP, 1000)
            .identifiers(new IdentifierCollection(List.of(APCO25PatchGroup.create(patchGroup))))
            .protocol(Protocol.APCO25)
            .build();

        mEventManager.receive(event);
        mEventManager.process();

        EventRecord record = mSnapshots.getFirst();
        assertEquals(IdentifierRecord.FORM_PATCH_GROUP, record.getToForm());
        assertEquals(500L, record.getToValue());
        assertEquals(List.of(new PatchedTalkgroup(100, null), new PatchedTalkgroup(101, null)),
            record.getPatchedTalkgroups());
    }
}
