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

package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamActivityModel;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.dsp.oscillator.ScalarRealOscillator;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.message.TimeslotMessage;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automated testing for the AudioStreamingManager that includes for testing streaming a patch group audio segment as
 * an individual stream aliased against the patch group, or broken up into the set of patched talkgroups and streamed
 * according to the aliases for each patched talkgroup.
 */
public class AudioStreamingManagerTest
{
    private static final int TALKGROUP_1 = 100;
    private static final int TALKGROUP_2 = 200;
    private static final int TALKGROUP_3 = 300;
    private static final int RADIO_1 = 9999;

    @Test
    public void testPatchGroupStreamingAsPatchGroup()
    {
        int expectedRecordingsCount = 1;

        //We use a countdown latch to count the number of expected audio recordings produced.
        CountDownLatch latch = new CountDownLatch(expectedRecordingsCount);
        Listener<AudioRecording> listener = audioRecording -> {
            latch.countDown();
        };

        Settings settings = new Settings();
        settings.getCallManagementSettings().setPatchGroupStreamingOption(PatchGroupStreamingOption.PATCH_GROUP);
        AudioStreamingManager manager = new AudioStreamingManager(listener, BroadcastFormat.MP3, settings);
        manager.start();
        manager.receive(getAudioSegment());

        boolean success = false;

        try
        {
            success = latch.await(5, TimeUnit.SECONDS);
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        cleanupStreamingDirectory(settings.getDirectorySettings().getDirectoryStreaming());

        assertTrue(success, "Stream patch group audio as PATCHED GROUP failed to produce [" +
                latch.getCount() + "/" + expectedRecordingsCount + "] streaming recordings");
    }

    @Test
    public void testPatchGroupStreamingAsIndividualGroups()
    {
        int expectedRecordingsCount = 2;

        //We use a countdown latch to count the number of expected audio recordings produced.  In this case, we expect
        //two audio recordings, one for stream B and one for stream C associated with the two patched talkgroups.
        CountDownLatch latch = new CountDownLatch(expectedRecordingsCount);
        Listener<AudioRecording> listener = audioRecording -> {
            latch.countDown();
        };

        Settings settings = new Settings();
        settings.getCallManagementSettings().setPatchGroupStreamingOption(PatchGroupStreamingOption.TALKGROUPS);
        AudioStreamingManager manager = new AudioStreamingManager(listener, BroadcastFormat.MP3, settings);
        manager.start();
        manager.receive(getAudioSegment());

        boolean success = false;

        try
        {
            success = latch.await(5, TimeUnit.SECONDS);
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        cleanupStreamingDirectory(settings.getDirectorySettings().getDirectoryStreaming());

        assertTrue(success, "Stream patch group audio as INDIVIDUAL TALKGROUPS failed to produce [" +
                latch.getCount() + "/" + expectedRecordingsCount + "] streaming recordings");
    }

    @Test
    public void testStreamActivityForPatchGroup()
    {
        StreamActivityModel model = new StreamActivityModel();
        Settings settings = new Settings();
        settings.getCallManagementSettings().setPatchGroupStreamingOption(PatchGroupStreamingOption.PATCH_GROUP);
        List<AudioRecording> recordings = process(settings, model, getAudioSegment(), 1);

        //Streamed as the patch group: the aliases of the patch group and of the patched talkgroups select streams
        Map<String,StreamActivity> byStream = model.getActivities().stream()
            .collect(Collectors.toMap(StreamActivity::getStreamName, Function.identity()));
        assertEquals(Set.of("Stream A", "Stream B", "Stream C"), byStream.keySet());
        StreamActivity activity = byStream.get("Stream A");
        assertEquals(StreamStatus.PENDING, activity.getStatus());
        assertEquals(1, activity.getSelectingAliases().size());
        assertEquals("patch", activity.getSelectingAliases().getFirst().alias());
        assertTrue(activity.getSelectingAliases().getFirst().identifier().startsWith("To "),
            activity.getSelectingAliases().getFirst().identifier());
        assertEquals("talkgroup1", byStream.get("Stream B").getSelectingAliases().getFirst().alias());
        assertEquals("talkgroup2", byStream.get("Stream C").getSelectingAliases().getFirst().alias());
        assertTrue(activity.getDuration() > 0);
        assertEquals(activity, recordings.getFirst().getStreamActivity("Stream A"));
    }

    @Test
    public void testStreamActivityForPatchedTalkgroups()
    {
        StreamActivityModel model = new StreamActivityModel();
        Settings settings = new Settings();
        settings.getCallManagementSettings().setPatchGroupStreamingOption(PatchGroupStreamingOption.TALKGROUPS);
        process(settings, model, getAudioSegment(), 2);

        Map<String,StreamActivity> byStream = model.getActivities().stream()
            .collect(Collectors.toMap(StreamActivity::getStreamName, Function.identity()));
        assertEquals(3, byStream.size(), byStream.toString());

        StreamActivity b = byStream.get("Stream B");
        assertEquals(StreamStatus.PENDING, b.getStatus());
        assertEquals("talkgroup1", b.getSelectingAliases().getFirst().alias());
        assertEquals("Patched " + b.getTo(), b.getSelectingAliases().getFirst().identifier());

        StreamActivity c = byStream.get("Stream C");
        assertEquals("talkgroup2", c.getSelectingAliases().getFirst().alias());

        //The patch group alias streams to A, but patch calls are streamed as the patched talkgroups
        StreamActivity a = byStream.get("Stream A");
        assertEquals(StreamStatus.SUPPRESSED, a.getStatus());
        assertEquals("patch", a.getSelectingAliases().getFirst().alias());
        assertNotNull(a.getDetail());
    }

    @Test
    public void testStreamActivityForSuppressedDuplicate() throws InterruptedException
    {
        StreamActivityModel model = new StreamActivityModel();
        Settings settings = new Settings();
        settings.getCallManagementSettings().setDuplicateStreamingSuppressionEnabled(true);
        settings.getCallManagementSettings().setPatchGroupStreamingOption(PatchGroupStreamingOption.PATCH_GROUP);
        AudioSegment audioSegment = getAudioSegment();
        audioSegment.setDuplicate(true);
        List<AudioRecording> recordings = process(settings, model, audioSegment, 0);

        assertTrue(recordings.isEmpty());
        List<StreamActivity> activities = model.getActivities();
        assertEquals(3, activities.size());

        for(StreamActivity activity: activities)
        {
            assertEquals(StreamStatus.SUPPRESSED, activity.getStatus());
            assertTrue(activity.getDetail().startsWith("Duplicate call"), activity.getDetail());
        }
    }

    /**
     * Processes the audio segment and waits for the expected number of recordings (or a short time when none).
     */
    private List<AudioRecording> process(Settings settings, StreamActivityModel model, AudioSegment audioSegment,
                                         int expectedRecordings)
    {
        List<AudioRecording> recordings = new java.util.concurrent.CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(Math.max(1, expectedRecordings));
        AudioStreamingManager manager = new AudioStreamingManager(recording -> {
            recordings.add(recording);
            latch.countDown();
        }, BroadcastFormat.MP3, settings, model);
        manager.start();
        manager.receive(audioSegment);

        try
        {
            boolean done = latch.await(expectedRecordings > 0 ? 5 : 1, TimeUnit.SECONDS);
            assertEquals(expectedRecordings > 0, done, "recordings: " + recordings.size());
            //Let the manager finish adding the rows of the segment after the last recording
            Thread.sleep(300);
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            manager.stop();
            cleanupStreamingDirectory(settings.getDirectorySettings().getDirectoryStreaming());
        }

        assertEquals(expectedRecordings, recordings.size());
        return recordings;
    }

    /**
     * Cleanup any generated streaming recordings.
     * @param streamingDirectory
     */
    private void cleanupStreamingDirectory(Path streamingDirectory)
    {
        if(Files.exists(streamingDirectory))
        {
            try(Stream<Path> fileStream = Files.list(streamingDirectory))
            {
                fileStream.forEach(path -> {
                    try
                    {
                        Files.delete(path);
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                });
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates an audio segment with audio using the supplied alias list.
     * @return audio segment
     */
    private static AudioSegment getAudioSegment()
    {
        AliasList aliasList = getAliasList();

        AudioSegment audioSegment = new AudioSegment(aliasList, TimeslotMessage.TIMESLOT_0);
        ScalarRealOscillator oscillator = new ScalarRealOscillator(1000, 8000);
        for(int x = 0; x < 100; x++)
        {
            audioSegment.addAudio(oscillator.generate(500));
        }
        audioSegment.addIdentifier(getPatchGroup());
        audioSegment.addIdentifier(getRadio());
        audioSegment.completeProperty().set(true);
        return audioSegment;
    }

    private static AliasList getAliasList()
    {
        AliasList aliasList = new AliasList("test");

        Alias patchAlias = new Alias("patch");
        patchAlias.addAliasID(new Talkgroup(Protocol.APCO25, 100));
        patchAlias.addAliasID(new BroadcastChannel("Stream A"));
        aliasList.addAlias(patchAlias);

        Alias talkgroupAlias1 = new Alias("talkgroup1");
        talkgroupAlias1.addAliasID(new Talkgroup(Protocol.APCO25, 200));
        talkgroupAlias1.addAliasID(new BroadcastChannel("Stream B"));
        aliasList.addAlias(talkgroupAlias1);

        Alias talkgroupAlias2 = new Alias("talkgroup2");
        talkgroupAlias2.addAliasID(new Talkgroup(Protocol.APCO25, 300));
        talkgroupAlias2.addAliasID(new BroadcastChannel("Stream C"));
        aliasList.addAlias(talkgroupAlias2);

        return aliasList;
    }

    /**
     * Creates a patch group
     * @return p25 patch group
     */
    private static APCO25PatchGroup getPatchGroup()
    {
        TalkgroupIdentifier talkgroup1 = APCO25Talkgroup.create(TALKGROUP_1);
        TalkgroupIdentifier talkgroup2 = APCO25Talkgroup.create(TALKGROUP_2);
        TalkgroupIdentifier talkgroup3 = APCO25Talkgroup.create(TALKGROUP_3);

        PatchGroup pg = new PatchGroup(talkgroup1);
        pg.addPatchedTalkgroup(talkgroup2);
        pg.addPatchedTalkgroup(talkgroup3);
        return APCO25PatchGroup.create(pg);
    }

    /**
     * Creates a source radio identifier.
     * @return radio
     */
    private static RadioIdentifier getRadio()
    {
        return APCO25RadioIdentifier.createFrom(RADIO_1);
    }
}
