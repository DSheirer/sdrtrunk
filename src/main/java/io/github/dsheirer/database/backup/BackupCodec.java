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
package io.github.dsheirer.database.backup;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.dsheirer.database.IdentifierRecord;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.CallState;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.database.call.RecordingState;
import io.github.dsheirer.database.event.EventRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON form of the call and event records in a backup file.  Field names match the database column names, null
 * values are omitted and unknown fields are ignored when reading, so that a newer backup format version can add fields.
 */
public final class BackupCodec
{
    private BackupCodec()
    {
    }

    public static void write(JsonWriter writer, CallRecord call) throws IOException
    {
        writer.beginObject();
        writeIdentifierFields(writer, call);
        writer.name("call_state").value(call.getCallState().name());
        string(writer, "talker_alias", call.getTalkerAlias());
        writer.name("encrypted").value(call.isEncrypted());
        writer.name("duplicate_call").value(call.isDuplicate());
        writer.name("priority").value(call.getPriority());
        writer.name("alias_record").value(call.isAliasRecord());
        string(writer, "record_format", call.getRecordFormat());
        string(writer, "file_path", call.getFilePath());
        writer.name("file_size").value(call.getFileSize());
        writer.name("recording_state").value(call.getRecordingState().name());
        writer.endObject();
    }

    public static void write(JsonWriter writer, EventRecord event) throws IOException
    {
        writer.beginObject();
        writeIdentifierFields(writer, event);
        writer.name("duration_ms").value(event.getDuration());
        writer.name("event_type").value(event.getEventTypeValue().name());
        string(writer, "channel_descriptor", event.getChannelDescriptor());
        string(writer, "talker_alias", event.getTalkerAlias());
        string(writer, "details", event.getDetails());
        writer.endObject();
    }

    private static void writeIdentifierFields(JsonWriter writer, IdentifierRecord record) throws IOException
    {
        writer.name("start_time").value(record.getStartTime());
        writer.name("end_time").value(record.getEndTime());
        string(writer, "protocol", record.getProtocol());
        string(writer, "system_name", record.getSystem());
        string(writer, "site_name", record.getSite());
        string(writer, "channel_name", record.getChannel());
        writer.name("frequency").value(record.getFrequency());
        writer.name("timeslot").value(record.getTimeslot());
        string(writer, "to_id", record.getToId());
        number(writer, "to_value", record.getToValue());
        string(writer, "to_form", record.getToForm());
        string(writer, "to_fq", record.getToFullyQualified());
        string(writer, "from_id", record.getFromId());
        number(writer, "from_value", record.getFromValue());
        string(writer, "from_form", record.getFromForm());
        string(writer, "from_fq", record.getFromFullyQualified());
        string(writer, "alias_list", record.getAliasList());

        if(!record.getPatchedTalkgroups().isEmpty())
        {
            writer.name("patched_talkgroups").beginArray();

            for(PatchedTalkgroup talkgroup: record.getPatchedTalkgroups())
            {
                writer.beginObject();
                writer.name("talkgroup").value(talkgroup.value());
                string(writer, "fq", talkgroup.fullyQualified());
                writer.endObject();
            }

            writer.endArray();
        }
    }

    private static void string(JsonWriter writer, String name, String value) throws IOException
    {
        if(value != null)
        {
            writer.name(name).value(value);
        }
    }

    private static void number(JsonWriter writer, String name, Long value) throws IOException
    {
        if(value != null)
        {
            writer.name(name).value(value);
        }
    }

    /**
     * Reads a call record object
     */
    public static CallRecord readCall(JsonReader reader) throws IOException
    {
        CallRecord call = new CallRecord();
        reader.beginObject();

        while(reader.hasNext())
        {
            String name = reader.nextName();

            if(reader.peek() == JsonToken.NULL)
            {
                reader.nextNull();
                continue;
            }

            if(readIdentifierField(reader, name, call))
            {
                continue;
            }

            switch(name)
            {
                case "call_state" -> call.setCallState(CallState.fromValue(reader.nextString()));
                case "talker_alias" -> call.setTalkerAlias(reader.nextString());
                case "encrypted" -> call.setEncrypted(reader.nextBoolean());
                case "duplicate_call" -> call.setDuplicate(reader.nextBoolean());
                case "priority" -> call.setPriority(reader.nextInt());
                case "alias_record" -> call.setAliasRecord(reader.nextBoolean());
                case "record_format" -> call.setRecordFormat(reader.nextString());
                case "file_path" -> call.setFilePath(reader.nextString());
                case "file_size" -> call.setFileSize(reader.nextLong());
                case "recording_state" -> call.setRecordingState(RecordingState.fromValue(reader.nextString()));
                default -> reader.skipValue();
            }
        }

        reader.endObject();
        return call;
    }

    /**
     * Reads an event record object
     */
    public static EventRecord readEvent(JsonReader reader) throws IOException
    {
        EventRecord event = new EventRecord();
        reader.beginObject();

        while(reader.hasNext())
        {
            String name = reader.nextName();

            if(reader.peek() == JsonToken.NULL)
            {
                reader.nextNull();
                continue;
            }

            if(readIdentifierField(reader, name, event))
            {
                continue;
            }

            switch(name)
            {
                case "duration_ms" -> event.setDuration(reader.nextLong());
                case "event_type" -> event.setEventType(reader.nextString());
                case "channel_descriptor" -> event.setChannelDescriptor(reader.nextString());
                case "talker_alias" -> event.setTalkerAlias(reader.nextString());
                case "details" -> event.setDetails(reader.nextString());
                default -> reader.skipValue();
            }
        }

        reader.endObject();
        return event;
    }

    /**
     * Reads the value of a common identifier record field.
     * @return true if the field is a common field and its value was read
     */
    private static boolean readIdentifierField(JsonReader reader, String name, IdentifierRecord record)
        throws IOException
    {
        switch(name)
        {
            case "start_time" -> record.setStartTime(reader.nextLong());
            case "end_time" -> record.setEndTime(reader.nextLong());
            case "protocol" -> record.setProtocol(reader.nextString());
            case "system_name" -> record.setSystem(reader.nextString());
            case "site_name" -> record.setSite(reader.nextString());
            case "channel_name" -> record.setChannel(reader.nextString());
            case "frequency" -> record.setFrequency(reader.nextLong());
            case "timeslot" -> record.setTimeslot(reader.nextInt());
            case "to_id" -> record.setToId(reader.nextString());
            case "to_value" -> record.setToValue(reader.nextLong());
            case "to_form" -> record.setToForm(reader.nextString());
            case "to_fq" -> record.setToFullyQualified(reader.nextString());
            case "from_id" -> record.setFromId(reader.nextString());
            case "from_value" -> record.setFromValue(reader.nextLong());
            case "from_form" -> record.setFromForm(reader.nextString());
            case "from_fq" -> record.setFromFullyQualified(reader.nextString());
            case "alias_list" -> record.setAliasList(reader.nextString());
            case "patched_talkgroups" -> record.setPatchedTalkgroups(readPatchedTalkgroups(reader));
            default ->
            {
                return false;
            }
        }

        return true;
    }

    private static List<PatchedTalkgroup> readPatchedTalkgroups(JsonReader reader) throws IOException
    {
        List<PatchedTalkgroup> talkgroups = new ArrayList<>();
        reader.beginArray();

        while(reader.hasNext())
        {
            long value = 0;
            String fq = null;
            reader.beginObject();

            while(reader.hasNext())
            {
                String name = reader.nextName();

                if(reader.peek() == JsonToken.NULL)
                {
                    reader.nextNull();
                }
                else if(name.equals("talkgroup"))
                {
                    value = reader.nextLong();
                }
                else if(name.equals("fq"))
                {
                    fq = reader.nextString();
                }
                else
                {
                    reader.skipValue();
                }
            }

            reader.endObject();
            talkgroups.add(new PatchedTalkgroup(value, fq));
        }

        reader.endArray();
        return talkgroups;
    }
}
