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
package io.github.dsheirer.audio.playback;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.audio.AudioException;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.playback.PlayTestAudioRequest;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages scheduling and playback of audio segments to the local users audio system.
 */
public class AudioPlaybackManager implements Listener<AudioSegment>, IAudioController
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioPlaybackManager.class);
    private final AudioSegmentPrioritySorter mAudioSegmentPrioritySorter = new AudioSegmentPrioritySorter();
    private final Broadcaster<AudioEvent> mControllerBroadcaster = new Broadcaster<>();
    private final List<AudioSegment> mAudioSegments = new ArrayList<>();
    private final List<AudioSegment> mPendingAudioSegments = new ArrayList<>();
    private final LinkedTransferQueue<AudioSegment> mNewAudioSegmentQueue = new LinkedTransferQueue<>();
    private final ReentrantLock mAudioChannelsLock = new ReentrantLock();
    private final UserPreferences mUserPreferences;
    private AudioPlaybackDeviceDescriptor mAudioPlaybackDevice;
    private AudioOutput mAudioOutput;
    private ScheduledFuture<?> mProcessingTask;

    /**
     * Constructs an instance.
     *
     * @param userPreferences for audio playback preferences
     */
    public AudioPlaybackManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        MyEventBus.getGlobalEventBus().register(this);
        AudioPlaybackDeviceDescriptor device = mUserPreferences.getPlaybackPreference().getAudioPlaybackDevice();

        if(device != null)
        {
            try
            {
                setAudioPlaybackDevice(device);
            }
            catch(AudioException ae)
            {
                mLog.error("Error during setup of audio playback configuration.  Attempted to use device [" +
                        device + "]", ae);
            }
        }
        else
        {
            mLog.warn("No audio output devices available");
        }

        //Even if we don't have an audio device, setup the queue processor to always process the audio segment queue
        ScheduledExecutorService scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("sdrtrunk audio manager"));
        mProcessingTask = scheduledExecutorService.scheduleAtFixedRate(new AudioSegmentProcessor(),
                0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Receives audio segments from channel audio modules.
     * @param audioSegment to receive and process
     */
    @Override
    public void receive(AudioSegment audioSegment)
    {
        mNewAudioSegmentQueue.add(audioSegment);
    }

    /**
     * Processes new audio segments and automatically assigns them to audio outputs.
     *
     * Note: this method is intended to be repeatedly invoked by a scheduled processing thread.
     */
    private void processAudioSegments()
    {
        //Process new audio segments queue.  If segment has audio, queue it for replay, otherwise place in pending queue
        AudioSegment newSegment = mNewAudioSegmentQueue.poll();

        while(newSegment != null)
        {
            if(newSegment.isDuplicate() &&
               mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled())
            {
                newSegment.decrementConsumerCount();
            }
            else if(newSegment.hasAudio())
            {
                mAudioSegments.add(newSegment);
            }
            else
            {
                mPendingAudioSegments.add(newSegment);
            }

            newSegment = mNewAudioSegmentQueue.poll();
        }

        //Transfer pending audio segments that now have audio or that completed without ever having audio
        if(!mPendingAudioSegments.isEmpty())
        {
            Iterator<AudioSegment> it = mPendingAudioSegments.iterator();

            AudioSegment audioSegment;

            while(it.hasNext())
            {
                audioSegment = it.next();

                if(audioSegment.isDuplicate() &&
                   mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled())
                {
                    it.remove();
                    audioSegment.decrementConsumerCount();
                }
                else if(audioSegment.hasAudio())
                {
                    //Queue it up for replay
                    it.remove();
                    mAudioSegments.add(audioSegment);
                }
                else if(audioSegment.completeProperty().get())
                {
                    //Rare situation: the audio segment completed but never had audio ... dispose it
                    it.remove();
                    audioSegment.decrementConsumerCount();
                }
            }
        }

        //Process all audio segments that have audio
        if(!mAudioSegments.isEmpty())
        {
            Iterator<AudioSegment> it = mAudioSegments.iterator();
            AudioSegment audioSegment;

            //Remove any audio segments flagged as do not monitor.  Don't remove completed segments yet, because
            //we want to give them a brief chance at playback.  Automatically assign linked audio segments to the
            //current audio output for audio continuity
            while(it.hasNext())
            {
                audioSegment = it.next();

                if(audioSegment.isDoNotMonitor() || (audioSegment.isDuplicate() &&
                   mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled()))
                {
                    it.remove();
                    audioSegment.decrementConsumerCount();
                }
                else if(audioSegment.isLinked())
                {
                    mAudioChannelsLock.lock();

                    try
                    {
                        for(AudioChannel audioOutput: mAudioOutput.getAudioProvider().getAudioChannels())
                        {
                            if(audioOutput.isLinkedTo(audioSegment))
                            {
                                it.remove();
                                audioOutput.play(audioSegment);
                            }
                        }
                    }
                    finally
                    {
                        mAudioChannelsLock.unlock();
                    }
                }
            }

            //Sort audio segments by playback priority and assign to empty audio outputs
            if(!mAudioSegments.isEmpty())
            {
                mAudioSegments.sort(mAudioSegmentPrioritySorter);
                mAudioChannelsLock.lock();

                try
                {
                    //Assign empty audio outputs first
                    for(AudioChannel audioChannel: mAudioOutput.getAudioProvider().getAudioChannels())
                    {
                        if(audioChannel.isEmpty())
                        {
                            audioChannel.play(mAudioSegments.removeFirst());
                            if(mAudioSegments.isEmpty())
                            {
                                return;
                            }
                        }
                    }
                }
                finally
                {
                    mAudioChannelsLock.unlock();
                }
            }

            //Remove any audio segments marked as complete that didn't get assigned to an output
            it = mAudioSegments.iterator(); //reset the iterator
            while(it.hasNext())
            {
                audioSegment = it.next();

                if(audioSegment.completeProperty().get() || (audioSegment.isDuplicate() &&
                   mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled()))
                {
                    it.remove();
                    audioSegment.decrementConsumerCount();
                }
            }
        }
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
        if(mProcessingTask != null)
        {
            mProcessingTask.cancel(true);
            mProcessingTask = null;
        }

        mNewAudioSegmentQueue.clear();
        mAudioSegments.clear();
    }

    /**
     * Receives a request from the global event bus to playback a test audio sequence via the specified audio channel
     * @param request with test audio and channel number
     */
    @Subscribe
    public void playTestAudio(PlayTestAudioRequest request)
    {
        if(mAudioOutput != null)
        {
            mAudioOutput.playTestAudio(request);
        }
    }

    /**
     * Receive user preference update notifications so that we can detect when the user changes the audio output
     * device in the user preferences editor.
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.PLAYBACK)
        {
            AudioPlaybackDeviceDescriptor device = mUserPreferences.getPlaybackPreference().getAudioPlaybackDevice();

            if(device != null && !device.equals(getAudioPlaybackDevice()))
            {
                try
                {
                    setAudioPlaybackDevice(device);
                }
                catch(AudioException ae)
                {
                    mLog.error("Error changing audio output to [" + device + "]", ae);
                }
            }
        }
    }

    /**
     * Configures audio playback to use the audio device argument.
     *
     * @param audioDevice to use in configuring the audio playback setup.
     * @throws AudioException if there is an error
     */
    @Override
    public void setAudioPlaybackDevice(AudioPlaybackDeviceDescriptor audioDevice) throws AudioException
    {
        if(audioDevice != null)
        {
            mControllerBroadcaster.broadcast(new AudioEvent(AudioEvent.Type.AUDIO_CONFIGURATION_CHANGE_STARTED,
                    audioDevice.getMixerInfo().getName()));

            mAudioChannelsLock.lock();

            try
            {
                if(mAudioOutput != null)
                {
                    mAudioOutput.dispose();
                }

                int channelCount = audioDevice.getAudioFormat().getChannels();

                switch(channelCount)
                {
                    case 1:
                        mAudioOutput = new AudioOutput(audioDevice, new AudioProviderMono(mUserPreferences));
                        break;
                    case 2:
                        mAudioOutput = new AudioOutput(audioDevice, new AudioProviderStereo(mUserPreferences));
                        break;
                    default:
//                        mAudioOutput = new AudioOutput(audioDevice, new AudioProviderMultiChannel(mUserPreferences,
//                                audioDevice.getAudioFormat()));
                        throw new AudioException("Unsupported mixer channel configuration channel count: " + channelCount);
                }

                //Note: audio output can use an alternate device if the requested device can't be used, so we assign
                //the descriptor that was actually used by the audio output
                mAudioPlaybackDevice = mAudioOutput.getAudioPlaybackDeviceDescriptor();
            }
            finally
            {
                mAudioChannelsLock.unlock();
            }

            mControllerBroadcaster.broadcast(new AudioEvent(AudioEvent.Type.AUDIO_CONFIGURATION_CHANGE_COMPLETE,
                    mAudioPlaybackDevice.getMixerInfo().getName()));
        }
    }

    /**
     * Audio output device.  Note: this can be null depending on when it is accessed.
     * @return audio output or null.
     */
    public AudioOutput getAudioOutput()
    {
        return mAudioOutput;
    }

    /**
     * Current audio playback mixer channel configuration setting.
     */
    @Override
    public AudioPlaybackDeviceDescriptor getAudioPlaybackDevice()
    {
        return mAudioPlaybackDevice;
    }

    /**
     * List of sorted audio outputs available for the current mixer channel configuration
     */
    @Override
    public List<AudioChannel> getAudioChannels()
    {
        if(mAudioOutput != null)
        {
            return mAudioOutput.getAudioProvider().getAudioChannels();
        }

        return Collections.emptyList();
    }

    /**
     * Adds an audio event listener to receive audio event notifications.
     */
    @Override
    public void addAudioEventListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.addListener(listener);
    }

    /**
     * Removes an audio event listener from receiving audio event notifications.
     */
    @Override
    public void removeAudioEventListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.removeListener(listener);
    }

    /**
     * Scheduled runnable to process incoming audio segments
     */
    public class AudioSegmentProcessor implements Runnable
    {
        private final AtomicBoolean mProcessing = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                try
                {
                    processAudioSegments();
                }
                catch(Throwable t)
                {
                    mLog.error("Encountered error while processing audio segments", t);
                }

                mProcessing.set(false);
            }
        }
    }

    /**
     * Audio segment comparator for sorting audio segments by: 1)Playback priority and 2)Segment start time
     */
    public static class AudioSegmentPrioritySorter implements Comparator<AudioSegment>
    {
        @Override
        public int compare(AudioSegment segment1, AudioSegment segment2)
        {
            if(segment1 == null || segment2 == null)
            {
                return -1;
            }

            //If priority is the same, sort by start time
            if(segment1.monitorPriorityProperty().get() == segment2.monitorPriorityProperty().get())
            {
                return Long.compare(segment1.getStartTimestamp(), segment2.getStartTimestamp());
            }
            //Otherwise, sort by priority
            else
            {
                return Integer.compare(segment1.monitorPriorityProperty().get(), segment2.monitorPriorityProperty().get());
            }
        }
    }
}
