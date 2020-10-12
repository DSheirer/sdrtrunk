/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.audio.playback;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.audio.AudioException;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.mixer.MixerChannel;
import io.github.dsheirer.source.mixer.MixerChannelConfiguration;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages scheduling and playback of audio segments to the local users audio system.
 */
public class AudioPlaybackManager implements Listener<AudioSegment>, IAudioController
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioPlaybackManager.class);

    public static final String AUDIO_CHANNELS_PROPERTY = "audio.manager.channels";
    public static final String AUDIO_MIXER_PROPERTY = "audio.manager.mixer";
    public static final AudioEvent CONFIGURATION_CHANGE_STARTED =
        new AudioEvent(AudioEvent.Type.AUDIO_CONFIGURATION_CHANGE_STARTED, null);
    public static final AudioEvent CONFIGURATION_CHANGE_COMPLETE =
        new AudioEvent(AudioEvent.Type.AUDIO_CONFIGURATION_CHANGE_COMPLETE, null);
    private Broadcaster<AudioEvent> mControllerBroadcaster = new Broadcaster<>();
    private ScheduledFuture<?> mProcessingTask;
    private UserPreferences mUserPreferences;
    private MixerChannelConfiguration mMixerChannelConfiguration;
    private List<AudioOutput> mAudioOutputs = new ArrayList<>();
    private List<AudioSegment> mAudioSegments = new ArrayList<>();
    private List<AudioSegment> mPendingAudioSegments = new ArrayList<>();
    private LinkedTransferQueue<AudioSegment> mNewAudioSegmentQueue = new LinkedTransferQueue<>();

    /**
     * Constructs an instance.
     *
     * @param userPreferences for audio playback preferences
     */
    public AudioPlaybackManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        MyEventBus.getGlobalEventBus().register(this);

        MixerChannelConfiguration configuration = mUserPreferences.getPlaybackPreference().getMixerChannelConfiguration();

        if(configuration != null)
        {
            try
            {
                setMixerChannelConfiguration(configuration);
            }
            catch(AudioException ae)
            {
                mLog.error("Error during setup of audio playback configuration.  Attempted to use audio mixer [" +
                    (configuration != null ? configuration.getMixer().toString() : "null") + "] and channel [" +
                    (configuration != null ? configuration.getMixerChannel().name() : "null") + "]", ae);
            }
        }
        else
        {
            mLog.warn("No audio output devices available");
        }
    }

    /**
     * Receives audio segments from channel audio modules.
     * @param audioSegment
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
               mUserPreferences.getDuplicateCallDetectionPreference().isDuplicatePlaybackSuppressionEnabled())
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
                   mUserPreferences.getDuplicateCallDetectionPreference().isDuplicatePlaybackSuppressionEnabled())
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
                   mUserPreferences.getDuplicateCallDetectionPreference().isDuplicatePlaybackSuppressionEnabled()))
                {
                    it.remove();
                    audioSegment.decrementConsumerCount();
                }
                else if(audioSegment.isLinked())
                {
                    for(AudioOutput audioOutput: mAudioOutputs)
                    {
                        if(audioOutput.isLinkedTo(audioSegment))
                        {
                            it.remove();
                            audioOutput.play(audioSegment);
                        }
                    }
                }
            }

            //Sort audio segments by playback priority and assign to empty audio outputs
            if(!mAudioSegments.isEmpty())
            {
                mAudioSegments.sort(Comparator.comparingInt(o -> o.monitorPriorityProperty().get()));

                //Assign empty audio outputs first
                for(AudioOutput audioOutput: mAudioOutputs)
                {
                    if(audioOutput.emptyProperty().get())
                    {
                        audioOutput.play(mAudioSegments.remove(0));

                        if(mAudioSegments.isEmpty())
                        {
                            return;
                        }
                    }
                }
            }

            //Preempt ongoing audio segment playback when higher priority audio segments are available
            if(!mAudioSegments.isEmpty())
            {
                for(AudioOutput audioOutput: mAudioOutputs)
                {
                    if(mAudioSegments.get(0).monitorPriorityProperty().get() < audioOutput.audioPriorityProperty().get())
                    {
                        audioOutput.play(mAudioSegments.remove(0));

                        if(mAudioSegments.isEmpty())
                        {
                            return;
                        }
                    }
                }
            }

            //Remove any audio segments marked as complete that didn't get assigned to an output
            it = mAudioSegments.iterator(); //reset the iterator
            while(it.hasNext())
            {
                audioSegment = it.next();

                if(audioSegment.completeProperty().get() || (audioSegment.isDuplicate() &&
                   mUserPreferences.getDuplicateCallDetectionPreference().isDuplicatePlaybackSuppressionEnabled()))
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
     * Receive user preference update notifications so that we can detect when the user changes the audio output
     * device in the user preferences editor.
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.PLAYBACK)
        {
            MixerChannelConfiguration configuration = mUserPreferences.getPlaybackPreference().getMixerChannelConfiguration();

            if(configuration != null && !configuration.equals(mMixerChannelConfiguration))
            {
                try
                {
                    setMixerChannelConfiguration(configuration);
                }
                catch(AudioException ae)
                {
                    mLog.error("Error changing audio output to [" + configuration.toString() + "]", ae);
                }
            }
        }
    }

    /**
     * Configures audio playback to use the configuration specified in the entry argument.
     *
     * @param entry to use in configuring the audio playback setup.
     * @throws AudioException if there is an error
     */
    @Override
    public void setMixerChannelConfiguration(MixerChannelConfiguration entry) throws AudioException
    {
        if(entry != null)
        {
            mControllerBroadcaster.broadcast(CONFIGURATION_CHANGE_STARTED);

            if(mProcessingTask != null)
            {
                mProcessingTask.cancel(true);
            }

            for(AudioOutput audioOutput: mAudioOutputs)
            {
                audioOutput.dispose();
            }

            mAudioOutputs.clear();

            switch(entry.getMixerChannel())
            {
                case MONO:
                    AudioOutput mono = new MonoAudioOutput(entry.getMixer(), mUserPreferences);
                    mAudioOutputs.add(mono);
                    break;
                case STEREO:
                    AudioOutput left = new StereoAudioOutput(entry.getMixer(), MixerChannel.LEFT, mUserPreferences);
                    mAudioOutputs.add(left);

                    AudioOutput right = new StereoAudioOutput(entry.getMixer(), MixerChannel.RIGHT, mUserPreferences);
                    mAudioOutputs.add(right);
                    break;
                default:
                    throw new AudioException("Unsupported mixer channel configuration: " + entry.getMixerChannel());
            }

            mProcessingTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(new AudioSegmentProcessor(),
                0, 100, TimeUnit.MILLISECONDS);
            mControllerBroadcaster.broadcast(CONFIGURATION_CHANGE_COMPLETE);
            mMixerChannelConfiguration = entry;
        }
    }

    /**
     * Current audio playback mixer channel configuration setting.
     */
    @Override
    public MixerChannelConfiguration getMixerChannelConfiguration() throws AudioException
    {
        return mMixerChannelConfiguration;
    }

    /**
     * List of sorted audio outputs available for the current mixer channel configuration
     */
    @Override
    public List<AudioOutput> getAudioOutputs()
    {
        List<AudioOutput> outputs = new ArrayList<>(mAudioOutputs);

        outputs.sort(Comparator.comparing(AudioOutput::getChannelName));

        return outputs;
    }

    /**
     * Adds an audio event listener to receive audio event notifications.
     */
    @Override
    public void addControllerListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.addListener(listener);
    }

    /**
     * Removes an audio event listener from receiving audio event notifications.
     */
    @Override
    public void removeControllerListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.removeListener(listener);
    }

    /**
     * Scheduled runnable to process incoming audio segments
     */
    public class AudioSegmentProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                processAudioSegments();
            }
            catch(Throwable t)
            {
                mLog.error("Encountered error while processing audio segments", t);
            }
        }
    }
}
