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

package io.github.dsheirer.audio.broadcast.activity;

import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallBroadcaster;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallConfiguration;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerBroadcaster;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.identifier.IdentifierCollection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stream activity rows: status updates, final status, row limit and the pending replays of a recording.
 */
public class StreamActivityModelTest
{
    private static StreamActivity.Builder call()
    {
        return new StreamActivity.Builder().callKey(7).to("1234").from("5678")
            .selectingAliases(List.of(new StreamActivity.SelectingAlias("Fire", "To 1234")));
    }

    @Test
    public void testFinalStatusIsKept()
    {
        StreamActivityModel model = new StreamActivityModel();
        List<StreamActivity> updates = new ArrayList<>();
        model.addListener(updates::add);

        StreamActivity activity = model.add(call(), "Feed", null, StreamStatus.PENDING, null);
        model.update(activity, StreamStatus.UPLOADING, null);
        model.update(activity, StreamStatus.DUPLICATE, "skipped");
        model.update(activity, StreamStatus.NOT_SENT, "stream stopped");
        model.update(activity, StreamStatus.DUPLICATE, "skipped");

        assertEquals(StreamStatus.DUPLICATE, activity.getStatus());
        assertEquals("skipped", activity.getDetail());
        assertEquals(3, updates.size(), "add, uploading and duplicate");
        assertEquals("Fire (To 1234)", activity.getSelectingAliasText());
    }

    @Test
    public void testRowLimit()
    {
        StreamActivityModel model = new StreamActivityModel(3);
        List<StreamActivity> added = new ArrayList<>();

        for(int x = 0; x < 5; x++)
        {
            added.add(model.add(call(), "Feed " + x, null, StreamStatus.PENDING, null));
        }

        assertEquals(added.subList(2, 5), model.getActivities());
    }

    @Test
    public void testRecordingStatusByBroadcaster()
    {
        StreamActivityModel model = new StreamActivityModel();
        BroadcastifyCallConfiguration broadcastifyConfig = new BroadcastifyCallConfiguration();
        broadcastifyConfig.setName("Broadcastify");
        BroadcastifyCallBroadcaster broadcastify = new BroadcastifyCallBroadcaster(broadcastifyConfig,
            InputAudioFormat.SR_8000, MP3Setting.CBR_16, null);
        RdioScannerConfiguration rdioConfig = new RdioScannerConfiguration();
        rdioConfig.setName("Rdio");
        RdioScannerBroadcaster rdio = new RdioScannerBroadcaster(rdioConfig, InputAudioFormat.SR_8000,
            MP3Setting.CBR_16, null);

        AudioRecording recording = new AudioRecording(Path.of("unused.mp3"),
            Set.of(new BroadcastChannel("Broadcastify"), new BroadcastChannel("Rdio"),
                new BroadcastChannel("Missing")), new IdentifierCollection(), System.currentTimeMillis(), 1000);
        Map<String,StreamActivity> rows = new LinkedHashMap<>();

        for(String name: List.of("Broadcastify", "Rdio", "Missing"))
        {
            rows.put(name, model.add(call(), name, null, StreamStatus.PENDING, null));
        }

        recording.setStreamActivities(model, rows);
        recording.addBroadcaster(broadcastify, "Broadcastify");
        recording.addBroadcaster(rdio, "Rdio");
        recording.setStatus("Missing", StreamStatus.NOT_SENT, "No stream named 'Missing'");

        assertEquals(BroadcastServerType.BROADCASTIFY_CALL, rows.get("Broadcastify").getServerType());
        assertNull(rows.get("Missing").getServerType());
        assertEquals(StreamStatus.NOT_SENT, rows.get("Missing").getStatus());
        assertTrue(recording.hasPendingReplays());

        recording.setStatus(broadcastify, StreamStatus.UPLOADING, null);
        recording.setStatus(broadcastify, StreamStatus.SENT, null);
        //A late report from the same broadcaster doesn't release the recording for the other broadcaster
        recording.setStatus(broadcastify, StreamStatus.ERROR, "late");
        recording.release(broadcastify);

        assertEquals(StreamStatus.SENT, rows.get("Broadcastify").getStatus());
        assertEquals(StreamStatus.PENDING, rows.get("Rdio").getStatus());
        assertTrue(recording.hasPendingReplays(), "Rdio still holds the recording");

        recording.setStatus(rdio, StreamStatus.DUPLICATE, "duplicate");
        assertFalse(recording.hasPendingReplays());
        assertEquals(StreamStatus.DUPLICATE, rows.get("Rdio").getStatus());
    }
}
