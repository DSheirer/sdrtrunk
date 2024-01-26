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

import io.github.dsheirer.audio.broadcast.AudioStreamingManager;
import io.github.dsheirer.audio.call.ActiveAudioSegment;
import io.github.dsheirer.audio.call.AudioSegment;
import io.github.dsheirer.audio.call.Call;
import io.github.dsheirer.audio.call.CallRepository;
import io.github.dsheirer.audio.call.ICallEventListener;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.audio.playbackfx.AudioPlaybackController;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.retention.AgeUnits;
import io.github.dsheirer.preference.retention.RetentionPolicy;
import io.github.dsheirer.preference.retention.RetentionPreference;
import io.github.dsheirer.record.AudioRecordingManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Manages handling and distribution of audio segments and calls across the application.
 */
@Component("audioManager")
public class AudioManager implements Listener<AudioSegment>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioManager.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    private static final long GIGABYTES = 1024l * 1024l * 1024l;

    @Resource
    private AudioPlaybackManager mAudioPlaybackManager;
    @Resource
    private AudioPlaybackController mAudioPlaybackController;
    @Resource
    private AudioRecordingManager mAudioRecordingManager;
    @Resource
    private AudioStreamingManager mAudioStreamingManager;
    @Resource
    private DuplicateCallDetector mDuplicateCallDetector;
    @Resource
    private UserPreferences mUserPreferences;
    private CallRepository mCallRepository;
    private List<ICallEventListener> mCallListeners = new CopyOnWriteArrayList<>();
    private ActiveAudioSegmentProcessor mAudioSegmentProcessor = new ActiveAudioSegmentProcessor();
    private CallAgeOffProcessor mCallAgeOffProcessor = new CallAgeOffProcessor();
    private ScheduledFuture<?> mAudioSegmentProcessorFuture;
    private ScheduledFuture<?> mCallRetentionProcessorFuture;

    /**
     * Constructs an instance
     */
    @Autowired
    public AudioManager(CallRepository callRepository)
    {
        mCallRepository = callRepository;
    }

    @PostConstruct
    public void postConstruct()
    {
        LOGGER.info("Post constructing ......");
        mAudioSegmentProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(mAudioSegmentProcessor, 100,
                500, TimeUnit.MILLISECONDS);
        //Delay first run until 1-minute after startup.
        mCallRetentionProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(mCallAgeOffProcessor,
                30, 30, TimeUnit.SECONDS);

        //Register call event listeners
        add(mAudioRecordingManager);
        add(mAudioPlaybackController);
        add(mAudioStreamingManager);
    }

    @PreDestroy
    public void preDestroy()
    {
        LOGGER.info("AudioManager shutting down");

        if(mAudioSegmentProcessorFuture != null)
        {
            mAudioSegmentProcessorFuture.cancel(true);
            mAudioSegmentProcessorFuture = null;
        }
        mAudioSegmentProcessor.shutdown();

        if(mCallRetentionProcessorFuture != null)
        {
            mCallRetentionProcessorFuture.cancel(true);
            mCallRetentionProcessorFuture = null;
        }
    }

    /**
     * Adds the call listener to be notified of call events.
     * @param listener to add
     */
    public void add(ICallEventListener listener)
    {
        if(!mCallListeners.contains(listener))
        {
            mCallListeners.add(listener);
        }
    }

    /**
     * Broadcasts a call added event to registered listeners.
     * @param call that was added
     */
    private void broadcastAdded(Call call)
    {
        for(ICallEventListener listener: mCallListeners)
        {
            listener.added(call);
        }
    }

    /**
     * Broadcasts a call updated event to registered listeners.
     * @param call that was updated
     */
    private void broadcastUpdated(Call call)
    {
        for(ICallEventListener listener: mCallListeners)
        {
            listener.updated(call);
        }
    }

    /**
     * Broadcasts a call completed event to registered listeners.
     * @param call that was completed
     * @param audioSegment associated with the call.
     */
    private void broadcastCompleted(Call call, AudioSegment audioSegment)
    {
        for(ICallEventListener listener: mCallListeners)
        {
            audioSegment.addLease(listener.getClass().toString());
            listener.completed(call, audioSegment);
        }
    }

    /**
     * Broadcasts a call deleted event to registered listeners.
     * @param call that was deleted
     */
    private void broadcastDeleted(Call call)
    {
        for(ICallEventListener listener: mCallListeners)
        {
            listener.deleted(call);
        }
    }

    /**
     * Removes the listener from receiving call event notifications.
     * @param listener to remove
     */
    public void remove(ICallEventListener listener)
    {
        mCallListeners.remove(listener);
    }

    @Override
    public void receive(AudioSegment audioSegment)
    {
        //Let the duplicate call detector process the segment first to detect duplicates.
        audioSegment.addLease(mDuplicateCallDetector.getClass().toString());
        mDuplicateCallDetector.receive(audioSegment);
        audioSegment.addLease(mAudioSegmentProcessor.getClass().toString());
        mAudioSegmentProcessor.add(audioSegment);

        //TODO: remove for cleanup
//        audioSegment.addLease(mAudioPlaybackManager.getClass().toString());
//        mAudioPlaybackManager.receive(audioSegment);
    }

    /**
     * Active audio segment processor.  Single thread processor that runs as a scheduled task to process incoming
     * audio segments, update existing active audio segments, and process completed audio segments.
     */
    private class ActiveAudioSegmentProcessor implements Runnable
    {
        private LinkedTransferQueue<AudioSegment> mQueuedAudioSegments = new LinkedTransferQueue<>();
        private List<AudioSegment> mAudioSegmentsToProcess = new ArrayList<>();
        private Map<AudioSegment,ActiveAudioSegment> mActiveAudioSegmentsMap = new HashMap<>();
        private AtomicBoolean mProcessing = new AtomicBoolean();

        /**
         * Adds the new audio segment to be processed
         * @param audioSegment to process
         */
        public void add(AudioSegment audioSegment)
        {
            mQueuedAudioSegments.add(audioSegment);
        }

        /**
         * Performs shutdown and cleanup of any active audio segments that remain after the threaded processing
         * is terminated.
         */
        public void shutdown()
        {
            for(Map.Entry<AudioSegment,ActiveAudioSegment> entry: mActiveAudioSegmentsMap.entrySet())
            {
                //Invoke dispose() method to remove the lease on the audio segment
                entry.getValue().dispose();
            }
            mActiveAudioSegmentsMap.clear();

            for(AudioSegment audioSegment: mQueuedAudioSegments)
            {
                audioSegment.removeLease(getClass().toString());
            }
            mQueuedAudioSegments.clear();
        }

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                //Process existing active audio segments
                try
                {
                    Iterator<Map.Entry<AudioSegment,ActiveAudioSegment>> it = mActiveAudioSegmentsMap.entrySet().iterator();

                    while(it.hasNext())
                    {
                        ActiveAudioSegment activeAudioSegment = it.next().getValue();

                        if(activeAudioSegment.update())
                        {
                            broadcastUpdated(activeAudioSegment.getCall());
                        }

                        if(activeAudioSegment.isComplete())
                        {
                            //Process residual audio and close the wave recording file.
                            activeAudioSegment.closeAudio();

                            Call call = activeAudioSegment.getCall();
                            call.setComplete(true);

                            try
                            {
                                mCallRepository.save(call);
                            }
                            catch(Exception e)
                            {
                                LOGGER.error("Couldn't update call in database");
                            }
                            broadcastCompleted(call, activeAudioSegment.getAudioSegment());
                            activeAudioSegment.dispose();
                            it.remove();
                        }
                    }
                }
                catch(Throwable t)
                {
                    LOGGER.error("Error while processing existing active audio segments", t);
                }

                //Process new audio segments
                try
                {
                    mQueuedAudioSegments.drainTo(mAudioSegmentsToProcess);

                    for(AudioSegment audioSegment: mAudioSegmentsToProcess)
                    {
                        if(mActiveAudioSegmentsMap.containsKey(audioSegment))
                        {
                            ActiveAudioSegment activeAudioSegment = mActiveAudioSegmentsMap.get(audioSegment);

                            if(activeAudioSegment.update())
                            {
                                broadcastUpdated(activeAudioSegment.getCall());
                            }
                        }
                        else
                        {
                            Call call = ActiveAudioSegment.create(audioSegment);
                            ActiveAudioSegment activeAudioSegment = new ActiveAudioSegment(audioSegment, call, mUserPreferences);
                            mActiveAudioSegmentsMap.put(audioSegment, activeAudioSegment);
                            mCallRepository.save(call);
                            broadcastAdded(call);
                        }
                    }

                    mAudioSegmentsToProcess.clear();
                }
                catch(Throwable t)
                {
                    LOGGER.error("Error while processing existing active audio segments", t);
                }

                mProcessing.set(false);
            }
        }
    }

    /**
     * Call event retention and age-off processor.
     */
    public class CallAgeOffProcessor implements Runnable
    {
        @Override
        public void run()
        {
            RetentionPreference retentionPreference = mUserPreferences.getRetentionPreference();
            RetentionPolicy policy = retentionPreference.getRetentionPolicy();

            try
            {
                if(policy.isAgePolicy())
                {
                    AgeUnits ageUnits = retentionPreference.getAgeUnits();
                    int ageValue = retentionPreference.getAgeValue();
                    long threshold = System.currentTimeMillis() - ageUnits.getDuration(ageValue);
                    PageRequest requestPage1 = PageRequest.of(0, 100);
                    long start = 0;
                    Page<Call> page = mCallRepository.findCallsByEventTimeBetweenOrderByEventTime(start, threshold, requestPage1);

                    while(page.getTotalElements() > 0)
                    {
                        List<Call> calls = page.get().collect(Collectors.toList());

                        for(Call call: calls)
                        {
                            //Keep adjusting the start time to 1 millisecond more than the current call so that
                            //if there's an error deleting any given call, we don't get stuck processing it every time
                            start = call.getEventTime() + 1;

                            //Only evaluate calls that are complete
                            if(call.isComplete())
                            {
                                boolean deleteFailed = false;
                                File file = new File(call.getFile());
                                if(file.exists())
                                {
                                    try
                                    {
                                        File parentDirectory = file.getParentFile();
                                        Files.delete(file.toPath());

                                        //Delete parent directory if it is now empty
                                        if(parentDirectory.exists() && parentDirectory.isDirectory())
                                        {
                                            if(!Files.list(parentDirectory.toPath()).findFirst().isPresent())
                                            {
                                                Files.delete(parentDirectory.toPath());
                                            }
                                        }
                                    }
                                    catch(Exception io)
                                    {
                                        LOGGING_SUPPRESSOR.error("Call Event Delete", 5,
                                                "Error deleting call event recording: " + call.getFile() +
                                                        " - " + io.getLocalizedMessage());
                                        deleteFailed = true;
                                    }
                                }

                                if(!deleteFailed)
                                {
                                    mCallRepository.delete(call);
                                    broadcastDeleted(call);
                                }
                            }
                        }

                        page = mCallRepository.findCallsByEventTimeBetweenOrderByEventTime(start, threshold, requestPage1);
                    }
                }

                if(policy.isSizePolicy())
                {
                    Path callsDirectory = mUserPreferences.getDirectoryPreference().getDirectoryCalls();

                    if(Files.exists(callsDirectory))
                    {
                        long currentSize = FileUtils.sizeOfDirectory(callsDirectory.toFile());
                        long objectiveSize = retentionPreference.getObjectiveDirectorySize();

                        if(currentSize >= objectiveSize)
                        {
                            long start = 0;
                            long now = System.currentTimeMillis();
                            PageRequest pageable = PageRequest.of(0, 100);
                            Page<Call> page = mCallRepository.findCallsByEventTimeBetweenOrderByEventTime(start, now, pageable);

                            while(currentSize >= objectiveSize && page.getTotalElements() > 0)
                            {
                                List<Call> calls = page.get().collect(Collectors.toList());

                                for(Call call: calls)
                                {
                                    //Keep adjusting the start time to 1 millisecond more than the current call so that
                                    //if there's an error deleting any given call, we don't get stuck processing it every time
                                    start = call.getEventTime() + 1;

                                    if(currentSize < objectiveSize)
                                    {
                                        start = now - 1;
                                        break;
                                    }

                                    //Only evaluate calls that are complete
                                    if(call.isComplete())
                                    {
                                        boolean deleteFailed = false;
                                        File file = new File(call.getFile());
                                        if(file.exists())
                                        {
                                            try
                                            {
                                                File parentDirectory = file.getParentFile();
                                                long fileSize = FileUtils.sizeOf(file);
                                                Files.delete(file.toPath());
                                                currentSize -= fileSize;

                                                //Delete parent directory if it is now empty
                                                if(parentDirectory.exists() && parentDirectory.isDirectory())
                                                {
                                                    if(!Files.list(parentDirectory.toPath()).findFirst().isPresent())
                                                    {
                                                        Files.delete(parentDirectory.toPath());
                                                    }
                                                }
                                            }
                                            catch(Exception io)
                                            {
                                                LOGGING_SUPPRESSOR.error("Call Event Delete", 5,
                                                        "Error deleting call event recording: " + call.getFile() +
                                                                " - " + io.getLocalizedMessage());
                                                deleteFailed = true;
                                            }
                                        }

                                        if(!deleteFailed)
                                        {
                                            mCallRepository.delete(call);
                                            broadcastDeleted(call);
                                        }
                                    }
                                }

                                page = mCallRepository.findCallsByEventTimeBetweenOrderByEventTime(start, now, pageable);
                            }
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                LOGGER.error("Unexpected error while deleting call events using retention policy: " + policy, t);
            }
        }
    }
}
