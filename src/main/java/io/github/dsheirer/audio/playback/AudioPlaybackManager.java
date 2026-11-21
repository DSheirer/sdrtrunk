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
    private final Broadcaster<ChannelPlayback> mPlaybackBroadcaster = new Broadcaster<>();
    private final List<AudioSegment> mAudioSegments = new ArrayList<>();
    private final List<AudioSegment> mPendingAudioSegments = new ArrayList<>();
    private final LinkedTransferQueue<AudioSegment> mNewAudioSegmentQueue = new LinkedTransferQueue<>();
    private final ReentrantLock mAudioChannelsLock = new ReentrantLock();
    private final UserPreferences mUserPreferences;
    private AudioPlaybackDeviceDescriptor mAudioPlaybackDevice;
    private AudioOutput mAudioOutput;
    private ScheduledFuture<?> mProcessingTask;
    private final TalkgroupHold[] mHolds = new TalkgroupHold[2];
    private final long[] mHoldActivity = new long[2];

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
        updateHoldActivity();

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
            List<AudioSegment> displaced = new ArrayList<>();

            //Remove any audio segments flagged as do not monitor.  Don't remove completed segments yet, because
            //we want to give them a brief chance at playback.  Automatically assign linked audio segments to the
            //current audio output for audio continuity
            while(it.hasNext())
            {
                audioSegment = it.next();

                if((audioSegment.isDoNotMonitor() && getHoldingChannelIndex(audioSegment) < 0) ||
                   (audioSegment.isDuplicate() &&
                   mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled()))
                {
                    it.remove();
                    audioSegment.decrementConsumerCount();
                }
                else if(audioSegment.isLinked())
                {
                    if(mAudioOutput != null)
                    {
                        mAudioChannelsLock.lock();

                        try
                        {
                            for(AudioChannel audioOutput: mAudioOutput.getAudioProvider().getAudioChannels())
                            {
                                if(audioOutput.isLinkedTo(audioSegment))
                                {
                                    it.remove();

                                    //Linked audio continues the current call, so it goes ahead of anything queued
                                    AudioSegment queued = audioOutput.pollQueuedAudioSegment();

                                    while(queued != null)
                                    {
                                        displaced.add(queued);
                                        queued = audioOutput.pollQueuedAudioSegment();
                                    }

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
            }

            mAudioSegments.addAll(displaced);

            //Sort audio segments by playback priority and assign to empty audio outputs
            if(!mAudioSegments.isEmpty() && mAudioOutput != null)
            {
                mAudioSegments.sort(mAudioSegmentPrioritySorter);
                mAudioChannelsLock.lock();

                try
                {
                    assignAudioSegments(mAudioSegments, mAudioOutput.getAudioProvider().getAudioChannels());
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

    /**
     * Assigns waiting audio segments, in priority order, to empty audio channels.  Calls to a hold talkgroup only go to
     * the channel that holds the talkgroup, and other calls only go to channels without a hold talkgroup.
     *
     * @param waiting audio segments, sorted by playback priority.  Assigned segments are removed.
     * @param audioChannels to assign to.
     */
    static void assignAudioSegments(List<AudioSegment> waiting, List<AudioChannel> audioChannels)
    {
        Iterator<AudioSegment> it = waiting.iterator();

        while(it.hasNext())
        {
            AudioSegment audioSegment = it.next();
            AudioChannel holding = getHoldingChannel(audioSegment, audioChannels);
            AudioChannel target = null;

            if(holding != null)
            {
                if(holding.isEmpty())
                {
                    target = holding;
                }
            }
            else
            {
                for(AudioChannel audioChannel: audioChannels)
                {
                    if(audioChannel.getHold() == null && audioChannel.isEmpty() && !audioChannel.isReplaying())
                    {
                        target = audioChannel;
                        break;
                    }
                }
            }

            if(target != null)
            {
                it.remove();
                target.play(audioSegment);
            }
        }

        //Priority replacement of queued segments only applies to channels and calls without a hold talkgroup
        List<AudioChannel> unheld = new ArrayList<>();

        for(AudioChannel audioChannel: audioChannels)
        {
            if(audioChannel.getHold() == null && !audioChannel.isReplaying())
            {
                unheld.add(audioChannel);
            }
        }

        if(!unheld.isEmpty() && !waiting.isEmpty())
        {
            List<AudioSegment> unmatched = new ArrayList<>();
            it = waiting.iterator();

            while(it.hasNext())
            {
                AudioSegment audioSegment = it.next();

                if(getHoldingChannel(audioSegment, audioChannels) == null)
                {
                    unmatched.add(audioSegment);
                    it.remove();
                }
            }

            replaceLowerPriorityQueuedSegments(unmatched, unheld);
            waiting.addAll(unmatched);
        }
    }

    /**
     * Channel that holds the talkgroup of the audio segment, or null.
     */
    static AudioChannel getHoldingChannel(AudioSegment audioSegment, List<AudioChannel> audioChannels)
    {
        for(AudioChannel audioChannel: audioChannels)
        {
            TalkgroupHold hold = audioChannel.getHold();

            if(hold != null && hold.matches(audioSegment))
            {
                return audioChannel;
            }
        }

        return null;
    }

    /**
     * Index of the channel that holds the talkgroup of the audio segment, or -1.
     */
    private int getHoldingChannelIndex(AudioSegment audioSegment)
    {
        for(int x = 0; x < mHolds.length; x++)
        {
            TalkgroupHold hold = mHolds[x];

            if(hold != null && hold.matches(audioSegment))
            {
                return x;
            }
        }

        return -1;
    }

    /**
     * Sets (or clears, with null) the hold talkgroup for the audio channel.  Setting a talkgroup that another channel
     * holds moves the hold to this channel.  Queued calls that don't match the hold talkgroup are returned for
     * assignment to another channel, and a call that is playing and doesn't match stops.
     *
     * @param channelIndex of the audio channel (0 = left/mono, 1 = right)
     * @param hold talkgroup or null to release the hold
     */
    public void setHold(int channelIndex, TalkgroupHold hold)
    {
        if(channelIndex < 0 || channelIndex >= mHolds.length)
        {
            return;
        }

        mAudioChannelsLock.lock();

        try
        {
            if(hold != null)
            {
                for(int x = 0; x < mHolds.length; x++)
                {
                    if(x != channelIndex && hold.isSameTalkgroup(mHolds[x]))
                    {
                        applyHold(x, null);
                    }
                }
            }

            applyHold(channelIndex, hold);
        }
        finally
        {
            mAudioChannelsLock.unlock();
        }

        mLog.info("Audio channel " + channelIndex + (hold != null ? " hold talkgroup [" + hold + "]" :
            " hold talkgroup released"));
    }

    /**
     * Applies the hold to the channel.  Audio channels lock must be held.
     */
    private void applyHold(int channelIndex, TalkgroupHold hold)
    {
        mHolds[channelIndex] = hold;
        mHoldActivity[channelIndex] = System.currentTimeMillis();

        if(mAudioOutput != null)
        {
            List<AudioChannel> channels = mAudioOutput.getAudioProvider().getAudioChannels();

            if(channelIndex < channels.size())
            {
                AudioChannel channel = channels.get(channelIndex);
                channel.setHold(hold);

                if(hold != null)
                {
                    //Return queued calls that can no longer play on this channel
                    List<AudioSegment> keep = new ArrayList<>();
                    AudioSegment queued = channel.pollQueuedAudioSegment();

                    while(queued != null)
                    {
                        if(hold.matches(queued))
                        {
                            keep.add(queued);
                        }
                        else
                        {
                            mNewAudioSegmentQueue.add(queued);
                        }

                        queued = channel.pollQueuedAudioSegment();
                    }

                    for(AudioSegment segment: keep)
                    {
                        channel.play(segment);
                    }
                }
            }
        }
    }

    /**
     * Registers the listener to be notified when an audio channel starts or stops playing a call.  Notified on the
     * audio playback thread.
     */
    public void addPlaybackListener(Listener<ChannelPlayback> listener)
    {
        mPlaybackBroadcaster.addListener(listener);
    }

    public void removePlaybackListener(Listener<ChannelPlayback> listener)
    {
        mPlaybackBroadcaster.removeListener(listener);
    }

    /**
     * Current playback state of each audio channel, for a listener that registers after playback started.
     */
    public List<ChannelPlayback> getChannelPlayback()
    {
        List<ChannelPlayback> playback = new ArrayList<>();
        List<AudioChannel> channels = getAudioChannels();

        for(int x = 0; x < channels.size(); x++)
        {
            playback.add(getChannelPlayback(channels.get(x), x, channels.size()));
        }

        return playback;
    }

    private static ChannelPlayback getChannelPlayback(AudioChannel channel, int channelIndex, int channelCount)
    {
        AudioSegment current = channel.getCurrentAudioSegment();
        ReplaySource replay = channel.getReplay();
        return new ChannelPlayback(channelIndex, channel.getChannelName(), channelCount,
            current != null ? current.getId() : 0, replay != null && !replay.isFinished() ? replay.getCallId() : 0);
    }

    /**
     * Replays a recorded call through the audio channels.  A channel that is already replaying is reused, otherwise
     * an idle channel (left first), otherwise the left (or mono) channel, which drops the call that it is playing.
     * Live calls aren't assigned to the channel while it replays.
     *
     * @param replay to play
     * @return index of the channel that replays the call, or -1 if there is no audio output
     */
    public int replay(ReplaySource replay)
    {
        mAudioChannelsLock.lock();

        try
        {
            List<AudioChannel> channels = getAudioChannels();

            if(channels.isEmpty())
            {
                return -1;
            }

            int selected = -1;

            for(int x = 0; x < channels.size() && selected < 0; x++)
            {
                if(channels.get(x).isReplaying())
                {
                    selected = x;
                }
            }

            for(int x = 0; x < channels.size() && selected < 0; x++)
            {
                AudioChannel channel = channels.get(x);

                if(!channel.hasAudioSegment() && channel.isEmpty())
                {
                    selected = x;
                }
            }

            if(selected < 0)
            {
                selected = 0;
            }

            channels.get(selected).startReplay(replay);
            return selected;
        }
        finally
        {
            mAudioChannelsLock.unlock();
        }
    }

    /**
     * Hold talkgroup for the audio channel, or null.
     */
    public TalkgroupHold getHold(int channelIndex)
    {
        return channelIndex >= 0 && channelIndex < mHolds.length ? mHolds[channelIndex] : null;
    }

    /**
     * Updates the last activity time of each hold talkgroup and releases holds that have been idle longer than the
     * automatic release time.
     */
    private void updateHoldActivity()
    {
        int minutes = mUserPreferences.getPlaybackPreference().getHoldAutoReleaseMinutes();
        long now = System.currentTimeMillis();

        for(int x = 0; x < mHolds.length; x++)
        {
            TalkgroupHold hold = mHolds[x];

            if(hold == null)
            {
                continue;
            }

            boolean active = false;

            for(AudioSegment audioSegment: mAudioSegments)
            {
                if(hold.matches(audioSegment))
                {
                    active = true;
                    break;
                }
            }

            if(!active && mAudioOutput != null)
            {
                List<AudioChannel> channels = mAudioOutput.getAudioProvider().getAudioChannels();
                active = x < channels.size() && channels.get(x).isPlayingHeldTalkgroup();
            }

            if(active)
            {
                mHoldActivity[x] = now;
            }
            else if(minutes > 0 && (now - mHoldActivity[x]) > TimeUnit.MINUTES.toMillis(minutes))
            {
                mLog.info("Releasing idle hold talkgroup [" + hold + "] after " + minutes + " minutes");
                setHold(x, null);
            }
        }
    }

    /**
     * Swaps audio segments queued on the audio channels for waiting audio segments that have a higher playback priority,
     * so that a segment queued behind the current playback can't hold a higher priority segment out of the channel.
     * Audio that is currently playing is not interrupted.  Displaced segments are returned to the waiting list.
     *
     * @param waiting audio segments, sorted by playback priority.
     * @param audioChannels to evaluate.
     */
    static void replaceLowerPriorityQueuedSegments(List<AudioSegment> waiting, List<AudioChannel> audioChannels)
    {
        for(AudioChannel audioChannel: audioChannels)
        {
            if(waiting.isEmpty())
            {
                return;
            }

            AudioSegment queued = audioChannel.peekQueuedAudioSegment();

            if(queued != null &&
               waiting.getFirst().monitorPriorityProperty().get() < queued.monitorPriorityProperty().get())
            {
                //The audio thread may have taken the queued segment since the peek, so only return what we removed.
                queued = audioChannel.pollQueuedAudioSegment();
                audioChannel.play(waiting.removeFirst());

                if(queued != null)
                {
                    waiting.add(queued);
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

                //Re-apply hold talkgroups to the new channels (and drop a right channel hold for a mono device)
                List<AudioChannel> channels = mAudioOutput.getAudioProvider().getAudioChannels();

                for(int x = 0; x < channels.size(); x++)
                {
                    final int channelIndex = x;
                    final AudioChannel channel = channels.get(x);
                    final int outputChannels = channels.size();
                    channel.setPlaybackListener(() -> mPlaybackBroadcaster.broadcast(
                        getChannelPlayback(channel, channelIndex, outputChannels)));
                }

                for(int x = 0; x < mHolds.length; x++)
                {
                    if(x < channels.size())
                    {
                        channels.get(x).setHold(mHolds[x]);
                    }
                    else
                    {
                        mHolds[x] = null;
                    }
                }
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
