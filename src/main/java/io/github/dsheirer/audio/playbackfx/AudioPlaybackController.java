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

package io.github.dsheirer.audio.playbackfx;

import io.github.dsheirer.audio.call.AudioSegment;
import io.github.dsheirer.audio.call.Call;
import io.github.dsheirer.audio.call.ICallEventListener;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.source.mixer.MixerChannel;
import io.github.dsheirer.util.ThreadPool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Controller for application audio playback channels
 */
@Component("audioPlaybackController")
public class AudioPlaybackController implements ICallEventListener, IAudioPlaybackStatusListener
{
    private static final Logger LOG = LoggerFactory.getLogger(AudioPlaybackController.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOG);
    private static final double AUDIO_BALANCE_LEFT = -1.0;
    private static final double AUDIO_BALANCE_RIGHT = 1.0;

    private Map<MixerChannel, AudioPlaybackChannelController> mControllerMap = new HashMap<>();
    private LinkedTransferQueue<Call> mNewCallQueue = new LinkedTransferQueue<>();
    private List<Call> mCalls = new ArrayList<>();
    private LinkedTransferQueue<PlaybackRequest> mRequestQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mQueueProcessorFuture;
    private int mMaxCallQueueSize = 25;

    /**
     * Constructs an instance
     */
    public AudioPlaybackController()
    {
        mControllerMap.put(MixerChannel.LEFT, new AudioPlaybackChannelController(MixerChannel.LEFT, AUDIO_BALANCE_LEFT));
        mControllerMap.put(MixerChannel.RIGHT, new AudioPlaybackChannelController(MixerChannel.RIGHT, AUDIO_BALANCE_RIGHT));

        //Register as audio playback status listener
        for(AudioPlaybackChannelController controller: mControllerMap.values())
        {
            controller.add(this);
        }
    }

    /**
     * Post instantiation/startup steps.
     */
    @PostConstruct
    public void postConstruct()
    {
        if(mQueueProcessorFuture == null)
        {
            //Run at 250ms or four times a second
            mQueueProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new QueueProcessor(),
                    0, 250, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Pre-destruction or shutdown steps
     */
    @PreDestroy
    public void preDestroy()
    {
        if(mQueueProcessorFuture != null)
        {
            mQueueProcessorFuture.cancel(true);
        }

        mRequestQueue.clear();
        mNewCallQueue.clear();
        mCalls.clear();
    }

    /**
     * Process request and call queues and manage playback state of both channels.  This method is intended to be
     * invoked by a (single) threaded processor.
     */
    private void process()
    {
        //Add new calls to the calls list.
        mNewCallQueue.drainTo(mCalls);

        //Process the request queue
        PlaybackRequest request = mRequestQueue.poll();

        while(request != null)
        {
            AudioPlaybackChannelController controller = mControllerMap.get(request.mixerChannel);

            switch(request.playbackMode)
            {
                case AUTO -> controller.auto();
                case LOCKED -> controller.lockAutoTo(request.identifier, request.system);
                case MUTE -> controller.mute();
                case REPLAY ->
                {
                    controller.replay(request.call);
                    mCalls.remove(request.call);
                }
            }

            request = mRequestQueue.poll();
        }

        //Remove duplicate calls
        mCalls.removeIf(call -> call.isDuplicate());

        if(mCalls.isEmpty())
        {
            return;
        }

        AudioPlaybackChannelController left = mControllerMap.get(MixerChannel.LEFT);
        AudioPlaybackChannelController right = mControllerMap.get(MixerChannel.LEFT);

        //Reduce calls queue size to max size but don't remove any calls tagged for locked replay.
        int excessCalls = mCalls.size() - mMaxCallQueueSize;
        if(excessCalls > 0)
        {
            Iterator<Call> it = mCalls.iterator();

            while(it.hasNext() && excessCalls > 0)
            {
                Call call = it.next();
                if(!(left.isLockedFor(call) || right.isLockedFor(call)))
                {
                    it.remove();
                    excessCalls--;
                    System.out.println("Removed excess call TO:" + call.getToId() + " FM:" + call.getFromId());
                }
            }
        }

        //Process left audio channel if it is currently empty and set for auto replay.
        if(left.isAvailableForAutoPlay())
        {
            List<Call> leftCalls = new ArrayList<>(mCalls);

            //Remove any calls locked to the right channel
            if(right.isLocked())
            {
                Iterator<Call> it = leftCalls.iterator();
                while(it.hasNext())
                {
                    if(right.isLockedFor(it.next()))
                    {
                        it.remove();;
                    }
                }
            }

            if(left.isLocked())
            {
                for(int x = 0; x < leftCalls.size(); x++)
                {
                    Call call = leftCalls.get(x);
                    if(left.isLockedFor(call))
                    {
                        mCalls.remove(call);
                        left.play(call);
                        break;
                    }
                }
            }
            else
            {
                Call call = leftCalls.get(0);
                mCalls.remove(call);
                System.out.println("Playing call to: " + call.getToId() + " fm:" + call.getFromId());
                left.play(call);
            }

        }

        if(mCalls.isEmpty())
        {
            return;
        }

        //Process right audio channel if it is currently empty and set for auto replay.
        if(right.isAvailableForAutoPlay())
        {
            List<Call> rightCalls = new ArrayList<>(mCalls);

            //Remove any calls locked to the left channel
            if(left.isLocked())
            {
                Iterator<Call> it = rightCalls.iterator();
                while(it.hasNext())
                {
                    if(left.isLockedFor(it.next()))
                    {
                        it.remove();;
                    }
                }
            }

            if(right.isLocked())
            {
                for(int x = 0; x < rightCalls.size(); x++)
                {
                    Call call = rightCalls.get(x);
                    if(right.isLockedFor(call))
                    {
                        mCalls.remove(call);
                        right.play(call);
                        break;
                    }
                }
            }
            else
            {
                Call call = rightCalls.get(0);
                mCalls.remove(call);
                right.play(call);
            }
        }
    }

    /**
     * Request replay of the specified call.
     * @param mixerChannel for the replay
     * @param call to replay.
     */
    public void replay(MixerChannel mixerChannel, Call call)
    {
        mRequestQueue.add(new PlaybackRequest(mixerChannel, PlaybackMode.REPLAY, call, null, null));
    }

    /**
     * Request auto-playback mode and optionally specifies a locked identifier.
     * @param mixerChannel for the auto mode.
     * @param identifier (optional) for locked replay.
     * @param system (optional) for locked replay.
     */
    public void auto(MixerChannel mixerChannel, String identifier, String system)
    {
        mRequestQueue.add(new PlaybackRequest(mixerChannel,
                (identifier == null ? PlaybackMode.AUTO : PlaybackMode.LOCKED), null, identifier, system));
    }

    /**
     * Mutes the specified mixer channel.
     * @param mixerChannel to mute.
     */
    public void mute(MixerChannel mixerChannel)
    {
        mRequestQueue.add(new PlaybackRequest(mixerChannel, PlaybackMode.MUTE, null, null, null));
    }

    @Override
    public void playbackStatusUpdated(AudioPlaybackChannelController controller, MediaPlayer.Status previousStatus,
                                      MediaPlayer.Status currentStatus)
    {
        //TODO: do I need this?
    }

    @Override
    public void endOfMedia(AudioPlaybackChannelController controller)
    {
        //TODO: do I need this?
    }

    /**
     * Primary audio controller for manual playback.
     */
    public AudioPlaybackChannelController getPrimaryController()
    {
        return mControllerMap.get(MixerChannel.LEFT);
    }

    /**
     * Get the channel controller associated with the mixer channel.
     * @return controller
     */
    public AudioPlaybackChannelController getChannelController(MixerChannel mixerChannel)
    {
        return mControllerMap.get(mixerChannel);
    }

    @Override
    public void added(Call call)
    {
        mNewCallQueue.add(call);
    }

    @Override
    public void updated(Call call) {}
    @Override
    public void completed(Call call, AudioSegment audioSegment) {}
    @Override
    public void deleted(Call call) {}

    /**
     * Playback request
     * @param mixerChannel for the request (LEFT or RIGHT)
     * @param playbackMode to apply
     * @param call (optional) to replay
     * @param identifier (optional) for locked replay.
     * @param system (optional) for locked replay.
     */
    private record PlaybackRequest(MixerChannel mixerChannel, PlaybackMode playbackMode, Call call,
                                   String identifier, String system)
    {
    }

    /**
     * Runnable queue processor.
     */
    private class QueueProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                process();
            }
            catch(Throwable t)
            {
                LOGGING_SUPPRESSOR.error(t.getMessage(), 3, "Error while processing audio queue", t);
            }
        }
    }
}
