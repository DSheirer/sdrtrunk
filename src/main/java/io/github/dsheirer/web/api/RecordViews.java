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

package io.github.dsheirer.web.api;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.database.IdentifierRecord;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.gui.calls.CallAliasResolver;
import java.util.List;

/**
 * JSON representations of calls and decode events.  Timestamps are ISO-8601 UTC strings, with epoch milliseconds
 * alongside the start time for client arithmetic.  Aliases are looked up when the view is created, so stored calls
 * show the current alias names.
 */
public final class RecordViews
{
    private RecordViews()
    {
    }

    /**
     * Talkgroup, patch group or radio identifier
     * @param formatted as sdrtrunk displays it, using the talkgroup format settings
     * @param value numeric value, or null
     * @param form TALKGROUP, PATCH_GROUP, RADIO or another identifier form
     * @param fullyQualified address for a roaming identifier, or null
     * @param alias current alias name, or null
     */
    public record IdentifierView(String formatted, Long value, String form, String fullyQualified, String alias)
    {
        static IdentifierView of(String formatted, Long value, String form, String fullyQualified, Alias alias)
        {
            if(formatted == null && value == null)
            {
                return null;
            }

            return new IdentifierView(formatted, value, form, fullyQualified, alias != null ? alias.getName() : null);
        }
    }

    /**
     * A talkgroup patched into a patch group call
     */
    public record PatchedTalkgroupView(long value, String fullyQualified) {}

    /**
     * A call.  Live calls from the calls topic have no id until they are stored, but always have a callKey that is
     * unique while the application runs.
     * @param id database id, or null when not stored yet
     * @param callKey id of the live call while the application runs
     * @param state ACTIVE, COMPLETE or INTERRUPTED
     * @param type GROUP, PATCH, PRIVATE, TELEPHONE or UNKNOWN
     * @param recording NONE, RECORDING, RECORDED, DELETED or FAILED
     * @param audioAvailable true when a completed recording can be played
     */
    public record CallView(Long id, long callKey, String state, String type, String start, long startMs, String end,
                           long durationMs, String protocol, String system, String site, String channel,
                           long frequency, int timeslot, IdentifierView to, IdentifierView from,
                           List<PatchedTalkgroupView> patchedTalkgroups, String aliasList, String talkerAlias,
                           boolean encrypted, boolean duplicate, int priority, String recording,
                           boolean audioAvailable)
    {
        public static CallView of(CallRecord call, Long id, CallAliasResolver aliases)
        {
            boolean active = call.isActive();
            return new CallView(id != null ? id : (call.getId() > 0 ? call.getId() : null), call.getCallKey(),
                call.getCallState().name(), call.getCallType().name(), ApiSupport.iso(call.getStartTime()),
                call.getStartTime(), active || call.getEndTime() <= 0 ? null : ApiSupport.iso(call.getEndTime()),
                call.getDuration(),
                call.getProtocol(), call.getSystem(), call.getSite(), call.getChannel(), call.getFrequency(),
                call.getTimeslot(), toView(call, aliases), fromView(call, aliases), patched(call), call.getAliasList(),
                call.getTalkerAlias(), call.isEncrypted(), call.isDuplicate(), call.getPriority(),
                call.getRecordingState().name(), call.isPlayable());
        }
    }

    /**
     * A decode event
     * @param id database id, or null when not stored yet
     * @param eventKey id of the live event while the application runs
     * @param type decode event type, for example CALL_GROUP, REGISTER, GPS
     * @param category VOICE_CALL, ENCRYPTED_CALL, DATA, COMMAND, REGISTRATION or OTHER
     */
    public record EventView(Long id, long eventKey, String type, String category, String start, long startMs,
                            String end, long durationMs, String protocol, String system, String site, String channel,
                            String channelDescriptor, long frequency, int timeslot, IdentifierView to,
                            IdentifierView from, List<PatchedTalkgroupView> patchedTalkgroups, String aliasList,
                            String talkerAlias, String details)
    {
        public static EventView of(EventRecord event, Long id, CallAliasResolver aliases)
        {
            return new EventView(id != null ? id : (event.getId() > 0 ? event.getId() : null), event.getEventKey(),
                event.getEventType(), event.getCategory().name(), ApiSupport.iso(event.getStartTime()),
                event.getStartTime(), event.getEndTime() > 0 ? ApiSupport.iso(event.getEndTime()) : null,
                event.getDuration(), event.getProtocol(), event.getSystem(), event.getSite(), event.getChannel(),
                event.getChannelDescriptor(), event.getFrequency(), event.getTimeslot(), toView(event, aliases),
                fromView(event, aliases), patched(event), event.getAliasList(), event.getTalkerAlias(),
                event.getDetails());
        }
    }

    private static IdentifierView toView(IdentifierRecord record, CallAliasResolver aliases)
    {
        return IdentifierView.of(record.getToId(), record.getToValue(), record.getToForm(),
            record.getToFullyQualified(), aliases != null ? aliases.getToAlias(record) : null);
    }

    private static IdentifierView fromView(IdentifierRecord record, CallAliasResolver aliases)
    {
        return IdentifierView.of(record.getFromId(), record.getFromValue(), record.getFromForm(),
            record.getFromFullyQualified(), aliases != null ? aliases.getFromAlias(record) : null);
    }

    private static List<PatchedTalkgroupView> patched(IdentifierRecord record)
    {
        List<PatchedTalkgroup> patched = record.getPatchedTalkgroups();
        return patched.isEmpty() ? List.of() : patched.stream()
            .map(talkgroup -> new PatchedTalkgroupView(talkgroup.value(), talkgroup.fullyQualified())).toList();
    }
}
