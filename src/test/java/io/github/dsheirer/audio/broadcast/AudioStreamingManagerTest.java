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
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.audio.AudioSegment;
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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /**
     * Verifies that broadcast channels are compiled from the CURRENT identifiers: when a TO talkgroup is replaced by a
     * later TO talkgroup, the previous talkgroup's broadcast channel is no longer reported.  This is the source-level
     * fix for the "call streamed to the previous talkgroup's stream, tagged with the current talkgroup" defect - it
     * covers both the stale-seed and missed-terminator triggers, since either way the current TO replaces the prior.
     */
    @Test
    public void getBroadcastChannelsReflectsCurrentTalkgroupNotPrevious()
    {
        AliasList aliasList = new AliasList("test");

        //Previous talkgroup 300 routes to Stream C.
        Alias previousTalkgroup = new Alias("previous-talkgroup");
        previousTalkgroup.addAliasID(new Talkgroup(Protocol.APCO25, TALKGROUP_3));
        previousTalkgroup.addAliasID(new BroadcastChannel("Stream C"));
        aliasList.addAlias(previousTalkgroup);

        //Current talkgroup 200 routes to Stream B.
        Alias currentTalkgroup = new Alias("current-talkgroup");
        currentTalkgroup.addAliasID(new Talkgroup(Protocol.APCO25, TALKGROUP_2));
        currentTalkgroup.addAliasID(new BroadcastChannel("Stream B"));
        aliasList.addAlias(currentTalkgroup);

        AudioSegment audioSegment = new AudioSegment(aliasList, TimeslotMessage.TIMESLOT_0);

        //A previous talkgroup 300 (Stream C) is resident in the segment - e.g. a stale seed or a missed terminator.
        audioSegment.addIdentifier(APCO25Talkgroup.create(TALKGROUP_3));
        assertEquals(Set.of(new BroadcastChannel("Stream C")), audioSegment.getBroadcastChannels(),
                "segment should route to the seeded talkgroup's stream before the current talkgroup arrives");

        //The current talkgroup 200 (Stream B) replaces the previous TO talkgroup.
        audioSegment.addIdentifier(APCO25Talkgroup.create(TALKGROUP_2));

        assertEquals(Set.of(new BroadcastChannel("Stream B")), audioSegment.getBroadcastChannels(),
                "segment must route only to the current talkgroup's stream once the TO identifier is replaced");
        assertFalse(audioSegment.getBroadcastChannels().contains(new BroadcastChannel("Stream C")),
                "previous talkgroup's broadcast channel must not be carried forward under the current talkgroup");
    }

    /**
     * Verifies that both the current TO talkgroup route AND a source-radio (FROM) route are reported together: an
     * operator can attach a broadcast channel to a radio alias ("stream everything unit N transmits"), and that stream
     * is legitimate alongside the current talkgroup's stream.
     */
    @Test
    public void getBroadcastChannelsKeepsCurrentTalkgroupAndSourceRadioRoutes()
    {
        AliasList aliasList = new AliasList("test");

        Alias currentTalkgroup = new Alias("current-talkgroup");
        currentTalkgroup.addAliasID(new Talkgroup(Protocol.APCO25, TALKGROUP_2));
        currentTalkgroup.addAliasID(new BroadcastChannel("Stream B"));
        aliasList.addAlias(currentTalkgroup);

        Alias sourceRadio = new Alias("source-radio");
        sourceRadio.addAliasID(new Radio(Protocol.APCO25, RADIO_1));
        sourceRadio.addAliasID(new BroadcastChannel("Radio Stream"));
        aliasList.addAlias(sourceRadio);

        AudioSegment audioSegment = new AudioSegment(aliasList, TimeslotMessage.TIMESLOT_0);
        audioSegment.addIdentifier(APCO25Talkgroup.create(TALKGROUP_2));
        audioSegment.addIdentifier(APCO25RadioIdentifier.createFrom(RADIO_1));

        assertEquals(Set.of(new BroadcastChannel("Stream B"), new BroadcastChannel("Radio Stream")),
                audioSegment.getBroadcastChannels(),
                "segment must route to both the current talkgroup (TO) and the source radio (FROM) streams");
    }

    /**
     * Verifies that a segment whose current identifiers route to no broadcast channel reports an empty set, so the
     * streaming manager's empty-check skips it rather than streaming to a stale channel.
     */
    @Test
    public void getBroadcastChannelsEmptyWhenNoCurrentIdentifierRoutes()
    {
        AliasList aliasList = new AliasList("test");

        Alias talkgroupNoStream = new Alias("talkgroup-no-stream");
        talkgroupNoStream.addAliasID(new Talkgroup(Protocol.APCO25, TALKGROUP_2));
        aliasList.addAlias(talkgroupNoStream);

        AudioSegment audioSegment = new AudioSegment(aliasList, TimeslotMessage.TIMESLOT_0);
        audioSegment.addIdentifier(APCO25Talkgroup.create(TALKGROUP_2));

        assertTrue(audioSegment.getBroadcastChannels().isEmpty(),
                "segment with no streamable current identifier must report no broadcast channels");
    }

    /**
     * End-to-end check of the non-patch streaming path through the manager: a completed, non-patch talkgroup segment
     * whose current identifier routes to a broadcast channel produces exactly one streaming recording.  Exercises the
     * rewritten {@code !broadcastChannels.isEmpty()} guard and the non-patch branch that the stale-channel defect lives
     * on.
     */
    @Test
    public void testNonPatchTalkgroupStreamsToItsChannel()
    {
        int expectedRecordingsCount = 1;

        CountDownLatch latch = new CountDownLatch(expectedRecordingsCount);
        Listener<AudioRecording> listener = audioRecording -> {
            latch.countDown();
        };

        Settings settings = new Settings();
        AudioStreamingManager manager = new AudioStreamingManager(listener, BroadcastFormat.MP3, settings);
        manager.start();
        manager.receive(getNonPatchAudioSegment());

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

        assertTrue(success, "Stream non-patch talkgroup audio failed to produce [" +
                latch.getCount() + "/" + expectedRecordingsCount + "] streaming recording");
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

    /**
     * Creates a completed, non-patch audio segment for a single talkgroup that routes to a broadcast channel.
     * @return audio segment
     */
    private static AudioSegment getNonPatchAudioSegment()
    {
        AliasList aliasList = new AliasList("test");

        Alias talkgroupAlias = new Alias("talkgroup");
        talkgroupAlias.addAliasID(new Talkgroup(Protocol.APCO25, TALKGROUP_2));
        talkgroupAlias.addAliasID(new BroadcastChannel("Stream B"));
        aliasList.addAlias(talkgroupAlias);

        AudioSegment audioSegment = new AudioSegment(aliasList, TimeslotMessage.TIMESLOT_0);
        ScalarRealOscillator oscillator = new ScalarRealOscillator(1000, 8000);
        for(int x = 0; x < 100; x++)
        {
            audioSegment.addAudio(oscillator.generate(500));
        }
        audioSegment.addIdentifier(APCO25Talkgroup.create(TALKGROUP_2));
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
