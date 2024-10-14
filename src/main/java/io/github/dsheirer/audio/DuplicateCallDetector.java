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

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.duplicate.ICallManagementProvider;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects duplicate calls that occur within the same system.  This detector is thread safe for the receive() method.
 *
 * Note: system in this context refers to the system name value that is used in channel configurations.  All decoder
 * channels must share the same system name for call duplication detection.
 */
public class DuplicateCallDetector implements Listener<AudioSegment>
{
    private final static Logger mLog = LoggerFactory.getLogger(DuplicateCallDetector.class);
    private ICallManagementProvider mCallManagementProvider;
    private Map<String,SystemDuplicateCallDetector> mDetectorMap = new HashMap();
    protected Listener<AudioSegment> mDuplicateCallDetectionListener;

    /**
     * Constructs an instance.
     * @param userPreferences to access the duplicate call detection preferences.
     */
    public DuplicateCallDetector(UserPreferences userPreferences)
    {
        this(userPreferences.getCallManagementPreference());
    }

    /**
     * Constructs an instance.
     * @param callManagementProvider to provide call management preferences.
     */
    public DuplicateCallDetector(ICallManagementProvider callManagementProvider)
    {
        mCallManagementProvider = callManagementProvider;
    }

    /**
     * Optional listener to be notified each time an audio segment is flagged as duplicate.
     * @param listener to register
     */
    public void setDuplicateCallDetectionListener(Listener<AudioSegment> listener)
    {
        mDuplicateCallDetectionListener = listener;
    }

    @Override
    public void receive(AudioSegment audioSegment)
    {
        if(mCallManagementProvider.isDuplicateCallDetectionEnabled())
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
                        detector = new SystemDuplicateCallDetector(mCallManagementProvider, system);
                        mDetectorMap.put(system, detector);
                    }

                    detector.add(audioSegment);
                }
            }
        }
    }

    /**
     * System level duplicate call detector.  Uses a scheduled executor to run every 25 ms to compare all ongoing call
     * audio segments to detect duplicates.
     *
     * All audio segments remain in the queue until they are flagged as complete.  While in the queue, each call is
     * compared against the others to detect duplicates.  Once all calls are either flagged as complete or flagged as
     * duplicate and removed, the queue is empty and the monitoring is shutdown until a new audio segment arrives and
     * then the monitoring starts again.
     */
    public class SystemDuplicateCallDetector
    {
        private final LinkedTransferQueue<AudioSegment> mAudioSegmentQueue = new LinkedTransferQueue<>();
        private final List<AudioSegment> mAudioSegments = new ArrayList<>();
        private ScheduledFuture<?> mProcessorFuture;
        private Lock mLock = new ReentrantLock();
        private boolean mMonitoring = false;
        private final ICallManagementProvider mCallManagementProvider;
        private String mSystem;

        /**
         * Constructs an instance
         * @param callManagementProvider to check for duplicate monitoring preferences
         */
        public SystemDuplicateCallDetector(ICallManagementProvider callManagementProvider, String system)
        {
            mCallManagementProvider = callManagementProvider;
            mSystem = system;
        }

        /**
         * Adds the audio segment to the monitoring queue.
         * @param audioSegment to add
         */
        public void add(AudioSegment audioSegment)
        {
            mLock.lock();

            try
            {
                mAudioSegmentQueue.add(audioSegment);
                startMonitoring();
            }
            finally
            {
                mLock.unlock();
            }
        }

        /**
         * Starts the call monitoring thread if it's not already running.
         *
         * Note: this method should only be called from a thread with the lock acquired.
         */
        private void startMonitoring()
        {
            if(!mMonitoring)
            {
                mProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::process,
                    0, 25, TimeUnit.MILLISECONDS);
                mMonitoring = true;
            }
        }

        /**
         * Stops the call monitoring thread if the audio segements queue is empty.
         *
         * Note: this should only be called from a separate thread and not from within the scheduled monitoring thread
         * because cancelling the scheduled timer from within the process() method would kill the thread without
         * releasing the lock, causing a deadlock.
         */
        private void stopMonitoring()
        {
            mLock.lock();

            try
            {
                //Recheck the audio segments queue to make sure we didn't slip in another audio segment before we can
                //shut down the scheduled monitoring thread.
                if(mMonitoring && mAudioSegments.isEmpty())
                {
                    if(mProcessorFuture != null)
                    {
                        mProcessorFuture.cancel(true);
                        mProcessorFuture = null;
                    }

                    mMonitoring = false;
                }
            }
            finally
            {
                mLock.unlock();
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
            if(mCallManagementProvider.isDuplicateCallDetectionByTalkgroupEnabled())
            {
                //Step 1 check for duplicate TO values
                List<Identifier> to1 = segment1.getIdentifierCollection().getIdentifiers(Role.TO);
                List<Identifier> to2 = segment2.getIdentifierCollection().getIdentifiers(Role.TO);

                if(isDuplicate(to1, to2))
                {
                    return true;
                }
            }

            if(mCallManagementProvider.isDuplicateCallDetectionByRadioEnabled())
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
        public static boolean isDuplicate(List<Identifier> identifiers1, List<Identifier> identifiers2)
        {
            for(Identifier identifier1: identifiers1)
            {
                if(identifier1 instanceof TalkgroupIdentifier tgId1)
                {
                    int tg1 = tgId1.getValue();

                    for(Identifier identifier2: identifiers2)
                    {
                        if(identifier2 instanceof TalkgroupIdentifier tgId2 && tgId2.getValue() == tg1)
                        {
                            return true;
                        }
                        else if(identifier2 instanceof PatchGroupIdentifier pgId2 &&
                                pgId2.getValue().getPatchGroup().getValue() == tg1)
                        {
                            return true;
                        }
                    }
                }
                else if(identifier1 instanceof PatchGroupIdentifier pgId1)
                {
                    int talkgroup1 = pgId1.getValue().getPatchGroup().getValue();

                    for(Identifier identifier2: identifiers2)
                    {
                        if(identifier2 instanceof TalkgroupIdentifier tgId2 && tgId2.getValue() == talkgroup1)
                        {
                            return true;
                        }
                        else if(identifier2 instanceof PatchGroupIdentifier pgId2 &&
                                pgId2.getValue().getPatchGroup().getValue() == talkgroup1)
                        {
                            return true;
                        }
                    }
                }
                else if(identifier1 instanceof RadioIdentifier raId1)
                {
                    int radio1 = raId1.getValue();

                    for(Identifier identifier2: identifiers2)
                    {
                        if(identifier2 instanceof RadioIdentifier raId2 && raId2.getValue() == radio1)
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
            mLock.lock();

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

                //Remove any encrypted audio segments.
                mAudioSegments.removeIf(audioSegment -> {
                    boolean encrypted = audioSegment.isEncrypted();

                    if(encrypted)
                    {
                        audioSegment.decrementConsumerCount();
                    }


                    return encrypted;
                });

                //Only check for duplicates if there is more than one call
                if(mAudioSegments.size() > 1)
                {
                    List<AudioSegment> duplicates = new ArrayList<>();

                    int currentIndex = 0;
                    while(currentIndex < mAudioSegments.size() - 1)
                    {
                        AudioSegment current = mAudioSegments.get(currentIndex);

                        if(current.hasAudio() && !current.isDuplicate())
                        {
                            int checkIndex = currentIndex + 1;

                            while(checkIndex < mAudioSegments.size())
                            {
                                AudioSegment toCheck = mAudioSegments.get(checkIndex);

                                if(!toCheck.isDuplicate())
                                {
                                    if(isDuplicate(current, toCheck))
                                    {
                                        toCheck.setDuplicate(true);
                                        toCheck.decrementConsumerCount();
                                        duplicates.add(toCheck);

                                        //Notify optional listener that we flagged the call as duplicate.
                                        if(mDuplicateCallDetectionListener != null)
                                        {
                                            mDuplicateCallDetectionListener.receive(toCheck);
                                        }
                                    }
                                }

                                checkIndex++;
                            }
                        }

                        currentIndex++;
                    }

                    mAudioSegments.removeAll(duplicates);
                }

                //Finally, if the audio segment queue is now empty, shutdown monitoring until a new segment arrives.
                //The monitor shutdown method has to be called on a separate thread so that we don't kill our current
                // thread and fail to release the lock.
                if(mAudioSegments.isEmpty())
                {
                    ThreadPool.CACHED.submit(this::stopMonitoring);
                }
            }
            catch(Throwable t)
            {
                mLog.error("Unknown error while processing audio segments for duplicate call detection.  Please report " +
                    "this to the developer.", t);
            }
            finally
            {
                mLock.unlock();
            }
        }
    }
}
