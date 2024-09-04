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

package io.github.dsheirer.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.preference.duplicate.ICallManagementProvider;
import io.github.dsheirer.preference.duplicate.TestCallManagementProvider;
import io.github.dsheirer.sample.Listener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the duplicate call detector.
 */
public class DuplicateCallDetectionTest
{
    /**
     * Test: same source radio to two different talkgroups on the same site where one call is encrytped and the other
     * call is not.
     *
     * Success Criteria: neither call gets flagged as duplicate.
     */
    @Test
    void sameCallOnDifferentSites()
    {
        AliasList aliasList = new AliasList("test");
        AudioSegment audioSegment1 = new AudioSegment(aliasList, 1);
        audioSegment1.addIdentifier(SystemConfigurationIdentifier.create("Test System"));
        audioSegment1.addIdentifier(SiteConfigurationIdentifier.create("Test Site 1"));
        audioSegment1.addIdentifier(APCO25Talkgroup.create(1));
        audioSegment1.addIdentifier(APCO25RadioIdentifier.createFrom(2));
        audioSegment1.addAudio(new float[2]);

        AudioSegment audioSegment2 = new AudioSegment(aliasList, 2);
        audioSegment2.addIdentifier(SystemConfigurationIdentifier.create("Test System"));
        audioSegment2.addIdentifier(SiteConfigurationIdentifier.create("Test Site 2"));
        audioSegment2.addIdentifier(APCO25Talkgroup.create(1));
        audioSegment2.addIdentifier(APCO25RadioIdentifier.createFrom(2));
        audioSegment1.addAudio(new float[2]);

        boolean testByTalkgroup = true;
        boolean testByRadio = false;

        ICallManagementProvider provider = new TestCallManagementProvider(testByTalkgroup, testByRadio);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener<AudioSegment> callback = audioSegment -> countDownLatch.countDown();

        DuplicateCallDetector duplicateCallDetector = new DuplicateCallDetector(provider);
        duplicateCallDetector.setDuplicateCallDetectionListener(callback);

        duplicateCallDetector.receive(audioSegment1);
        duplicateCallDetector.receive(audioSegment2);

        try
        {
            //Wait up to 100 ms, but the duplicate detector should fire within 25 ms.
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }

        audioSegment1.completeProperty().set(true);
        audioSegment2.completeProperty().set(true);

        //Test that at least one of the audio segments was marked as duplicate.
        assertTrue(audioSegment1.isDuplicate() || audioSegment2.isDuplicate(),
                "At least one audio segment should have been flagged as duplicate");
    }

    /**
     * Test: same call, same site, same source radio, simulcasting to two different talkgroups.
     *
     * Success Criteria: one audio segment is flagged as duplicate.
     */
    @Test
    void sameCallSameSiteSameRadioSimulcastToDifferentTalkgroups()
    {
        AliasList aliasList = new AliasList("test");

        AudioSegment audioSegment1 = new AudioSegment(aliasList, 1);
        audioSegment1.addIdentifier(SystemConfigurationIdentifier.create("Test System"));
        audioSegment1.addIdentifier(SiteConfigurationIdentifier.create("Test Site 1"));
        audioSegment1.addIdentifier(APCO25Talkgroup.create(1));
        audioSegment1.addIdentifier(APCO25RadioIdentifier.createFrom(2));
        audioSegment1.addAudio(new float[2]);

        AudioSegment audioSegment2 = new AudioSegment(aliasList, 2);
        audioSegment2.addIdentifier(SystemConfigurationIdentifier.create("Test System"));
        audioSegment2.addIdentifier(SiteConfigurationIdentifier.create("Test Site 1"));
        audioSegment2.addIdentifier(APCO25Talkgroup.create(2));
        audioSegment2.addIdentifier(APCO25RadioIdentifier.createFrom(2));
        audioSegment1.addAudio(new float[2]);

        boolean testByTalkgroup = true;
        boolean testByRadio = true;
        ICallManagementProvider provider = new TestCallManagementProvider(testByTalkgroup, testByRadio);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener<AudioSegment> callback = audioSegment -> countDownLatch.countDown();

        DuplicateCallDetector duplicateCallDetector = new DuplicateCallDetector(provider);
        duplicateCallDetector.setDuplicateCallDetectionListener(callback);

        duplicateCallDetector.receive(audioSegment1);
        duplicateCallDetector.receive(audioSegment2);

        try
        {
            //Wait up to 100 ms, but the duplicate detector should fire within 25 ms.
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }

        audioSegment1.completeProperty().set(true);
        audioSegment2.completeProperty().set(true);

        //Test that at least one of the audio segments was marked as duplicate.
        assertTrue(audioSegment1.isDuplicate() || audioSegment2.isDuplicate(),
                "At least one audio segment should have been flagged as duplicate");
    }

    /**
     * Test: same source radio to two different talkgroups on the same site where one call is encrypted and the other
     * call is not.
     *
     * Success Criteria: neither call gets flagged as duplicate.
     */
    @Test
    void sameCallSameSiteSameRadioSimulcastToDifferentTalkgroupsOneIsEncrypted()
    {
        AliasList aliasList = new AliasList("test");

        AudioSegment audioSegment1 = new AudioSegment(aliasList, 1);
        audioSegment1.addIdentifier(SystemConfigurationIdentifier.create("Test System"));
        audioSegment1.addIdentifier(SiteConfigurationIdentifier.create("Test Site 1"));
        audioSegment1.addIdentifier(APCO25Talkgroup.create(1));
        audioSegment1.addIdentifier(APCO25RadioIdentifier.createFrom(2));
        EncryptionKeyIdentifier eki1 = EncryptionKeyIdentifier.create(APCO25EncryptionKey.create(Encryption.AES_256, 1));
        audioSegment1.addIdentifier(eki1);
        audioSegment1.addAudio(new float[2]);

        AudioSegment audioSegment2 = new AudioSegment(aliasList, 2);
        audioSegment2.addIdentifier(SystemConfigurationIdentifier.create("Test System"));
        audioSegment2.addIdentifier(SiteConfigurationIdentifier.create("Test Site 1"));
        audioSegment2.addIdentifier(APCO25Talkgroup.create(2));
        audioSegment2.addIdentifier(APCO25RadioIdentifier.createFrom(2));
        EncryptionKeyIdentifier eki2 = EncryptionKeyIdentifier.create(APCO25EncryptionKey.create(Encryption.UNENCRYPTED, 1));
        audioSegment2.addIdentifier(eki2);
        audioSegment1.addAudio(new float[2]);

        boolean testByTalkgroup = true;
        boolean testByRadio = true;
        ICallManagementProvider provider = new TestCallManagementProvider(testByTalkgroup, testByRadio);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener<AudioSegment> callback = audioSegment -> countDownLatch.countDown();

        DuplicateCallDetector duplicateCallDetector = new DuplicateCallDetector(provider);
        duplicateCallDetector.setDuplicateCallDetectionListener(callback);

        duplicateCallDetector.receive(audioSegment1);
        duplicateCallDetector.receive(audioSegment2);

        try
        {
            //Wait up to 100 ms, but the duplicate detector should fire within 25 ms.
            countDownLatch.await(100, TimeUnit.MILLISECONDS);
        }
        catch(InterruptedException e)
        {
            //Don't log the exception ... it's expected
        }

        audioSegment1.completeProperty().set(true);
        audioSegment2.completeProperty().set(true);

        //Test that neither audio segment was flagged as duplicate and audio segment 1 is flagged as encrypted.
        assertTrue(audioSegment1.isEncrypted(), "Audio segment 1 should be flagged as encrypted");
        assertFalse(audioSegment2.isEncrypted(), "Audio segment 1 should be flagged as unencrypted");
        assertFalse(audioSegment1.isDuplicate(), "Audio segment should not be flagged as duplicate.");
        assertFalse(audioSegment2.isDuplicate(), "Audio segment should not be flagged as duplicate.");
    }
}
