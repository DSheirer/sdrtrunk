/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.output;

import audio.AudioEvent;
import audio.AudioPacket;
import audio.AudioPacket.Type;
import channel.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Broadcaster;
import sample.Listener;
import source.mixer.MixerChannel;
import util.ThreadPool;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AudioOutput implements Listener<AudioPacket>, LineListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioOutput.class);

    private LinkedTransferQueue<AudioPacket> mBuffer = new LinkedTransferQueue<>();
    private int mBufferStartThreshold;
    private int mBufferStopThreshold;

    private Listener<Metadata> mMetadataListener;
    private Broadcaster<AudioEvent> mAudioEventBroadcaster = new Broadcaster<>();

    private ScheduledFuture<?> mProcessorTask;

    private SourceDataLine mOutput;
    private Mixer mMixer;
    private MixerChannel mMixerChannel;
    private FloatControl mGainControl;
    private BooleanControl mMuteControl;

    private AudioEvent mAudioStartEvent;
    private AudioEvent mAudioStopEvent;

    private boolean mCanProcessAudio = false;
    private long mLastActivity = System.currentTimeMillis();

    /**
     * Single audio channel playback with automatic starting and stopping of the
     * underlying sourcedataline specified by the mixer and mixer channel
     * arguments.
     *
     * Maintains an internal non-blocking audio packet queue and processes this
     * queue 25 times a second (every 40 ms).
     *
     * @param mixer to obtain source data line
     * @param mixerChannel either mono or left/right stereo
     * @param audioFormat to use during playback
     * @param lineInfo to use when obtaining the source data line
     * @param requestedBufferSize of approximately 1 second of audio
     */
    public AudioOutput(Mixer mixer,
                       MixerChannel mixerChannel,
                       AudioFormat audioFormat,
                       Line.Info lineInfo,
                       int requestedBufferSize)
    {
        mMixer = mixer;
        mMixerChannel = mixerChannel;

        try
        {
            mOutput = (SourceDataLine) mMixer.getLine(lineInfo);

            if(mOutput != null)
            {
                mOutput.open(audioFormat, requestedBufferSize);

                //Start threshold: buffer is full with 10% or less of capacity remaining
                mBufferStartThreshold = (int) (mOutput.getBufferSize() * 0.10);

                //Stop threshold: buffer is empty with 90% or more capacity available
                mBufferStopThreshold = (int) (mOutput.getBufferSize() * 0.90);

                mOutput.addLineListener(this);

                if(mOutput != null)
                {
                    try
                    {
                        Control gain = mOutput.getControl(FloatControl.Type.MASTER_GAIN);
                        mGainControl = (FloatControl) gain;
                    }
                    catch(IllegalArgumentException iae)
                    {
                        mLog.warn("Couldn't obtain MASTER GAIN control for stereo line [" +
                            mixer.getMixerInfo().getName() + " | " + getChannelName() + "]");
                    }

                    try
                    {
                        Control mute = mOutput.getControl(BooleanControl.Type.MUTE);
                        mMuteControl = (BooleanControl) mute;
                    }
                    catch(IllegalArgumentException iae)
                    {
                        mLog.warn("Couldn't obtain MUTE control for stereo line [" +
                            mixer.getMixerInfo().getName() + " | " + getChannelName() + "]");
                    }

					/* Run the queue processor task every 40 milliseconds or 25 times a second */
                    mProcessorTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(new BufferProcessor(),
                        0, 40, TimeUnit.MILLISECONDS);
                }

                mAudioStartEvent = new AudioEvent(AudioEvent.Type.AUDIO_STARTED,
                    getChannelName());
                mAudioStopEvent = new AudioEvent(AudioEvent.Type.AUDIO_STOPPED,
                    getChannelName());

                mCanProcessAudio = true;
            }
        }
        catch(LineUnavailableException e)
        {
            mLog.error("Couldn't obtain audio source data line for "
                + "audio output - mixer [" + mMixer.getMixerInfo().getName() + "]");
        }
    }

    public void reset()
    {
        broadcast(new AudioEvent(AudioEvent.Type.AUDIO_STOPPED, getChannelName()));
    }

    public void dispose()
    {
        mCanProcessAudio = false;

        if(mProcessorTask != null)
        {
            mProcessorTask.cancel(true);
        }

        mProcessorTask = null;

        mBuffer.clear();

        mAudioEventBroadcaster.dispose();
        mAudioEventBroadcaster = null;
        mMetadataListener = null;

        if(mOutput != null)
        {
            mOutput.close();
        }

        mOutput = null;
        mGainControl = null;
        mMuteControl = null;
    }

    /**
     * Converts the audio packet data into a byte buffer format appropriate for
     * the underlying source data line.
     */
    protected abstract ByteBuffer convert(AudioPacket packet);

    /**
     * Audio output channel name
     */
    public String getChannelName()
    {
        return mMixerChannel.getLabel();
    }

    /**
     * Mixer Channel for this audio output
     */
    protected MixerChannel getMixerChannel()
    {
        return mMixerChannel;
    }

    /**
     * Registers a single listener to receive audio start and audio stop events
     */
    public void addAudioEventListener(Listener<AudioEvent> listener)
    {
        mAudioEventBroadcaster.addListener(listener);
    }

    public void removeAudioEventListener(Listener<AudioEvent> listener)
    {
        mAudioEventBroadcaster.removeListener(listener);
    }

    /**
     * Broadcasts an audio event to the registered listener
     */
    private void broadcast(AudioEvent audioEvent)
    {
        mAudioEventBroadcaster.broadcast(audioEvent);
    }

    /**
     * Registers a single listener to receive the audio metadata from each
     * audio packet
     */
    public void setMetadataListener(Listener<Metadata> listener)
    {
        mMetadataListener = listener;
    }

    public void removeAudioMetadataListener()
    {
        mMetadataListener = null;
    }

    /**
     * Broadcasts audio metadata to the registered listener
     */
    private void broadcast(Metadata metadata)
    {
        if(mMetadataListener != null)
        {
            mMetadataListener.receive(metadata);
        }
    }

    /**
     * Timestamp of either last buffer received or last buffer processed
     */
    public long getLastActivityTimestamp()
    {
        return mLastActivity;
    }

    /**
     * Updates the last activity timestamp to current system time
     */
    public void updateTimestamp()
    {
        mLastActivity = System.currentTimeMillis();
    }

    @Override
    public void receive(AudioPacket packet)
    {
        if(mCanProcessAudio)
        {
            //Update the activity timestamp so that this audio output doesn't
            //get disconnected before it starts processing the audio stream
            updateTimestamp();

            mBuffer.add(packet);
        }
    }

    public class BufferProcessor implements Runnable
    {
        private AtomicBoolean mProcessing = new AtomicBoolean();

        public BufferProcessor()
        {
        }

        @Override
        public void run()
        {
            try
            {
				/* The processing flag ensures that only one instance of the
				 * processor can run at any given time */
                if(mProcessing.compareAndSet(false, true))
                {
                    List<AudioPacket> packets = new ArrayList<AudioPacket>();

                    mBuffer.drainTo(packets);

                    for(AudioPacket packet : packets)
                    {
                        if(packet.getType() == Type.AUDIO)
                        {
                            broadcast(packet.getMetadata());

                            ByteBuffer buffer = convert(packet);

                            int wrote = 0;

                            if(!mOutput.isRunning())
                            {
                                int toWrite = mOutput.available();

                                if(toWrite > buffer.array().length)
                                {
                                    toWrite = buffer.array().length;
                                }

                                //Top off the buffer and check if we can start it
                                wrote += mOutput.write(buffer.array(), 0, toWrite);

                                checkStart();
                            }

                            if(mOutput.isRunning() && wrote < buffer.array().length)
                            {
                                //Blocking write
                                wrote += mOutput.write(buffer.array(), wrote,
                                    buffer.array().length - wrote);
                            }

                            updateTimestamp();
                        }
                    }

                    checkStop();

                    mProcessing.set(false);
                }
            }
            catch(Exception e)
            {
                mLog.error("Error while processing audio buffers", e);
            }
        }

        /**
         * Starts audio playback once audio buffer is almost full and remaining
         * capacity falls below the start threshold.
         */
        private void checkStart()
        {
            if(mCanProcessAudio &&
                !mOutput.isRunning() &&
                mOutput.available() <= mBufferStartThreshold)
            {
                mOutput.start();
            }
        }

        /**
         * Stops audio playback and drains the audio buffer to empty when the
         * audio buffer is mostly empty and the available buffer capacity
         * exceeds the stop threshold
         */
        private void checkStop()
        {
            if(mCanProcessAudio &&
                mOutput.isRunning() &&
                mOutput.available() >= mBufferStopThreshold)
            {
                mOutput.drain();
                mOutput.stop();
            }
        }
    }

    /**
     * Sets the mute state for this audio output channel
     */
    public void setMuted(boolean muted)
    {
        if(mMuteControl != null)
        {
            mMuteControl.setValue(muted);

            broadcast(new AudioEvent(muted ? AudioEvent.Type.AUDIO_MUTED :
                AudioEvent.Type.AUDIO_UNMUTED, getChannelName()));
        }
    }

    /**
     * Current mute state for this audio output channel
     */
    public boolean isMuted()
    {
        if(mMuteControl != null)
        {
            return mMuteControl.getValue();
        }

        return false;
    }

    /**
     * Gain/volume control for this audio output channel, if one is available.
     */
    public FloatControl getGainControl()
    {
        return mGainControl;
    }

    public boolean hasGainControl()
    {
        return mGainControl != null;
    }

    /**
     * Monitors the source data line playback state and broadcasts audio events
     * to the registered listener as the state changes
     */
    @Override
    public void update(LineEvent event)
    {
        LineEvent.Type type = event.getType();

        if(type == LineEvent.Type.START)
        {
            mAudioEventBroadcaster.broadcast(mAudioStartEvent);
        }
        else if(type == LineEvent.Type.STOP)
        {
            mAudioEventBroadcaster.broadcast(mAudioStopEvent);
        }
    }
}
