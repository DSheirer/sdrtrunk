/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.duplicate.DuplicateCallDetectionPreference;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Detects duplicate calls that occur within the same system.  This detector is thread safe for the receive() method.
 *
 * Note: system in this context refers to the system name value that is used in channel configurations.  All decoder
 * channels must share the same system name for call duplication detection.
 */
public class DuplicateCallDetector implements Listener<AudioSegment>
{
    private final static Logger mLog = LoggerFactory.getLogger(DuplicateCallDetector.class);
    private DuplicateCallDetectionPreference mDuplicateCallDetectionPreference;
    private Map<String,SystemDuplicateCallDetector> mDetectorMap = new HashMap();

    public DuplicateCallDetector(UserPreferences userPreferences)
    {
        mDuplicateCallDetectionPreference = userPreferences.getDuplicateCallDetectionPreference();
    }

    @Override
    public void receive(AudioSegment audioSegment)
    {
        if(mDuplicateCallDetectionPreference.isDuplicateCallDetectionEnabled())
        {
            Identifier identifier = audioSegment.getIdentifierCollection()
                .getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);

            if(identifier instanceof SystemConfigurationIdentifier)
            {
                String system = ((SystemConfigurationIdentifier)identifier).getValue();

                synchronized(mDetectorMap)
                {
                    SystemDuplicateCallDetector detector = mDetectorMap.get(system);

                    if(detector == null)
                    {
                        detector = new SystemDuplicateCallDetector();
                        mDetectorMap.put(system, detector);
                    }

                    detector.add(audioSegment);
                }
            }
        }
    }

    public class SystemDuplicateCallDetector
    {
        private LinkedTransferQueue<AudioSegment> mAudioSegmentQueue = new LinkedTransferQueue<>();
        private List<AudioSegment> mAudioSegments = new ArrayList<>();
        private AtomicBoolean mMonitoring = new AtomicBoolean();
        private ScheduledFuture<?> mProcessorFuture;

        public SystemDuplicateCallDetector()
        {
        }

        public void add(AudioSegment audioSegment)
        {
            //Block on audio segment queue so that we don't interfere with monitoring shutdown
            synchronized(mAudioSegmentQueue)
            {
                mAudioSegmentQueue.add(audioSegment);
                startMonitoring();
            }
        }

        private void startMonitoring()
        {
            if(mMonitoring.compareAndSet(false, true))
            {
                mProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(() -> process(),
                    0, 25, TimeUnit.MILLISECONDS);
            }
        }

        private void stopMonitoring()
        {
            if(mMonitoring.compareAndSet(true, false))
            {
                if(mProcessorFuture != null)
                {
                    mProcessorFuture.cancel(true);
                }
            }
        }

        /**
         * Checks if either the TO or FROM integer identifiers for the audio segments are the same
         * @param segment1 to compare
         * @param segment2 to compare
         * @return true if integer identifiers are the same
         */
        private boolean isDuplicate(AudioSegment segment1, AudioSegment segment2)
        {
            if(mDuplicateCallDetectionPreference.isDuplicateCallDetectionByTalkgroupEnabled())
            {
                //Step 1 check for duplicate TO values
                List<Identifier> to1 = segment1.getIdentifierCollection().getIdentifiers(Role.TO);
                List<Identifier> to2 = segment2.getIdentifierCollection().getIdentifiers(Role.TO);

                if(isDuplicate(to1, to2))
                {
                    return true;
                }
            }

            if(mDuplicateCallDetectionPreference.isDuplicateCallDetectionByRadioEnabled())
            {
                //Step 2 check for duplicate FROM values
                List<Identifier> from1 = segment1.getIdentifierCollection().getIdentifiers(Role.FROM);
                List<Identifier> from2 = segment2.getIdentifierCollection().getIdentifiers(Role.FROM);

                return isDuplicate(from1, from2);
            }

            return false;
        }

        /**
         * Checks both lists of identifiers to determine if there are talkgroups or radio identifiers that are
         * the same in both lists.  Note: talkgroup check compares both talkgroups and patch groups.
         *
         * @param identifiers1
         * @param identifiers2
         * @return
         */
        private boolean isDuplicate(List<Identifier> identifiers1, List<Identifier> identifiers2)
        {
            for(Identifier identifier1: identifiers1)
            {
                if(identifier1 instanceof TalkgroupIdentifier)
                {
                    int talkgroup1 = ((TalkgroupIdentifier)identifier1).getValue();

                    for(Identifier identifier2: identifiers2)
                    {
                        if(identifier2 instanceof TalkgroupIdentifier &&
                           ((TalkgroupIdentifier)identifier2).getValue() == talkgroup1)
                        {
                            return true;
                        }
                        else if(identifier2 instanceof PatchGroupIdentifier &&
                            ((PatchGroupIdentifier)identifier2).getValue().getPatchGroup().getValue() == talkgroup1)
                        {
                            return true;
                        }
                    }
                }
                else if(identifier1 instanceof PatchGroupIdentifier)
                {
                    int talkgroup1 = ((PatchGroupIdentifier)identifier1).getValue().getPatchGroup().getValue();

                    for(Identifier identifier2: identifiers2)
                    {
                        if(identifier2 instanceof TalkgroupIdentifier &&
                            ((TalkgroupIdentifier)identifier2).getValue() == talkgroup1)
                        {
                            return true;
                        }
                        else if(identifier2 instanceof PatchGroupIdentifier &&
                            ((PatchGroupIdentifier)identifier2).getValue().getPatchGroup().getValue() == talkgroup1)
                        {
                            return true;
                        }
                    }
                }
                else if(identifier1 instanceof RadioIdentifier)
                {
                    int radio1 = ((RadioIdentifier)identifier1).getValue();

                    for(Identifier identifier2: identifiers2)
                    {
                        if(identifier2 instanceof RadioIdentifier && ((RadioIdentifier)identifier2).getValue() == radio1)
                        {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        /**
         * Processes audio segments to detect duplicates
         */
        private void process()
        {
            try
            {
                //Transfer in newly arrived audio segments
                mAudioSegmentQueue.drainTo(mAudioSegments);

                //Remove any completed audio segments.
                mAudioSegments.removeIf(audioSegment -> {
                    boolean complete = audioSegment.completeProperty().get();

                    if(complete)
                    {
                        audioSegment.decrementConsumerCount();
                    }

                    return complete;
                });

                //Only check for duplicates if there is more than one call
                if(mAudioSegments.size() > 1)
                {
                    List<AudioSegment> duplicates = new ArrayList<>();

                    int currentIndex = 0;
                    while(currentIndex < mAudioSegments.size() - 1)
                    {
                        AudioSegment current = mAudioSegments.get(currentIndex);

                        if(!current.isDuplicate())
                        {
                            int checkIndex = currentIndex + 1;

                            while(checkIndex < mAudioSegments.size())
                            {
                                AudioSegment toCheck = mAudioSegments.get(checkIndex);

                                if(!toCheck.isDuplicate())
                                {
                                    if(isDuplicate(current, toCheck))
                                    {
                                        /* Copy TO identifiers to current audio segment so that duplicates from different TGs broadcast/record correctly */
                                        for(Identifier identifier: toCheck.getIdentifierCollection().getIdentifiers(Role.TO))
                                        {
                                            if(!current.getIdentifierCollection().getIdentifiers().contains(identifier))
                                            {
                                                current.addIdentifier(identifier, false);
                                            }
                                        }

                                        toCheck.setDuplicate(true);
                                        toCheck.decrementConsumerCount();
                                        duplicates.add(toCheck);
                                    }
                                }

                                checkIndex++;
                            }
                        }

                        currentIndex++;
                    }

                    mAudioSegments.removeAll(duplicates);
                }

                //Finally, if the audio segment queue is empty, shutdown montitoring until a new segment arrives
                if(mAudioSegments.isEmpty())
                {
                    //Block on the audio segment queue so that we can shutdown before any new segments are added, and
                    //allow the add(segment) to restart monitoring as soon as needed.
                    synchronized(mAudioSegmentQueue)
                    {
                        if(mAudioSegmentQueue.isEmpty())
                        {
                            try
                            {
                                stopMonitoring();
                            }
                            catch(Exception e)
                            {
                                mLog.error("Unexpected error during duplicate audio segment monitoring shutdown", e);
                                //Do nothing, we got interrupted
                            }
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                mLog.error("Unknown error while processing audio segments for duplicate call detection.  Please report " +
                    "this to the developer.", t);
            }
        }
    }
}
