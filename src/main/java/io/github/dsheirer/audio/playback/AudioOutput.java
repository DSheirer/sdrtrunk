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

import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.preference.playback.PlayTestAudioRequest;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * Audio output/playback channel for a single audio mixer channel.  Providers support for playback of audio segments
 * and broadcasts audio segment metadata to registered listeners (ie gui components).
 */
public class AudioOutput implements LineListener
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioOutput.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(mLog);
    private static final int BUFFER_SIZE_SAMPLES = 1000; //At 8 kHz audio
    private final AudioPlaybackDeviceDescriptor mAudioPlaybackDeviceDescriptor;
    private final AudioProvider mAudioProvider;
    private FloatControl mGainControl;
    private ScheduledExecutorService mScheduledExecutorService;
    private ScheduledFuture<?> mProcessorFuture;
    private SourceDataLine mSourceDataLine;
    private boolean mCanProcessAudio = false;
    private boolean mRunning = false;


    /**
     * Audio output for the selected audio playback device and provider.  Opens a SourceDataLine from the miser for the
     * audio format specified in the descriptor.  Employs a single threaded executor to process audio 10x a second and
     * manages the start/stop control for the dataline based on audio availability.
     *
     * @param descriptor for the mixer and audio format
     * @param audioProvider for access to audio from audio segments
     */
    public AudioOutput(AudioPlaybackDeviceDescriptor descriptor, AudioProvider audioProvider)
    {
        mAudioProvider = audioProvider;
        Mixer mixer = AudioSystem.getMixer(descriptor.getMixerInfo());

        if(mixer == null)
        {
            List<AudioPlaybackDeviceDescriptor> descriptors = AudioPlaybackDeviceManager.getAudioPlaybackDevices();

            int selected = 0;

            while((mixer == null) && (selected < descriptors.size()))
            {
                descriptor = descriptors.get(selected++);
                mixer = AudioSystem.getMixer(descriptor.getMixerInfo());
            }
        }

        mAudioPlaybackDeviceDescriptor = descriptor;

        if(mixer != null)
        {
            try
            {
                mSourceDataLine = (SourceDataLine) mixer.getLine(new DataLine.Info(SourceDataLine.class,
                        descriptor.getAudioFormat()));

                if(mSourceDataLine != null)
                {
                    mSourceDataLine.addLineListener(this);
                    try
                    {
                        Control gain = mSourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                        mGainControl = (FloatControl) gain;
                    }
                    catch(IllegalArgumentException iae)
                    {
                        LOGGING_SUPPRESSOR.error("no gain control", 2, "Couldn't obtain " +
                                "MASTER GAIN control for stereo line [" + mixer.getMixerInfo().getName() + "]");
                    }

                    mCanProcessAudio = true;
                }
            }
            catch(LineUnavailableException e)
            {
                mLog.error("Couldn't open source data line for mixer: " + descriptor.getMixerInfo().getName(), e);
            }

            if(mCanProcessAudio)
            {
                openSourceDataLine();

                //The audio provider gives us audio 160 samples per interval representing 20 milliseconds of audio.
                // Run the scheduled executor at just slightly faster than that time, recognizing that it will
                // block against the mixer source data line until it can write all 160 samples.
                mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory(
                        "sdrtrunk audio output " + descriptor.getMixerInfo().getName()));
                mProcessorFuture = mScheduledExecutorService.scheduleAtFixedRate(new AudioProcessor(),
                        0, 19, TimeUnit.MILLISECONDS);
            }
            else
            {
                //Disable each of the audio channels so they don't queue audio segments.
                for(AudioChannel audioChannel: mAudioProvider.getAudioChannels())
                {
                    audioChannel.setDisabled(true);
                }
            }
        }
    }

    /**
     * Opens and starts the source data line.
     */
    private void openSourceDataLine()
    {
        try
        {
            if(mSourceDataLine != null && !mSourceDataLine.isOpen())
            {
                int bufferSize = BUFFER_SIZE_SAMPLES * 2 * mAudioPlaybackDeviceDescriptor.getAudioFormat().getChannels();
                mSourceDataLine.open(mAudioProvider.getAudioFormat(), bufferSize);
                bufferSize = mSourceDataLine.getBufferSize();
                //Fill the line with silence so we can start it
                mSourceDataLine.write(new byte[bufferSize], 0, bufferSize);
                mSourceDataLine.start();

                for(AudioChannel audioChannel: mAudioProvider.getAudioChannels())
                {
                    audioChannel.setDisabled(false);
                }
            }
        }
        catch(LineUnavailableException e)
        {
            LOGGING_SUPPRESSOR.error("Can't open", 3, "Error opening source data line", e);
            mRunning = false;

            for(AudioChannel audioChannel: mAudioProvider.getAudioChannels())
            {
                audioChannel.setDisabled(true);
            }
        }
    }

    /**
     * Plays the test audio over the selected audio channel(s)
     * @param request to play test audio
     */
    public void playTestAudio(PlayTestAudioRequest request)
    {
        //If we're not running, resize the audio samples to fill the buffer enough to trigger playback.
        float[] audio = mRunning ? request.audio() : Arrays.copyOf(request.audio(), BUFFER_SIZE_SAMPLES);

        List<AudioChannel> channels = mAudioProvider.getAudioChannels();

        if(request.isAllChannels())
        {
            for(AudioChannel channel : channels)
            {
                channel.playTest(audio);
            }
        }
        else if(request.channel() < channels.size())
        {
            channels.get(request.channel()).playTest(audio);
        }
        else if(!channels.isEmpty())
        {
            channels.getFirst().playTest(audio);
        }
        else
        {
            LOGGING_SUPPRESSOR.info("No Audio Channels", 2,
                    "Unable to play test audio - no audio channels configured currently");
        }
    }

    /**
     * Audio playback device descriptor for this audio output
     */
    public AudioPlaybackDeviceDescriptor getAudioPlaybackDeviceDescriptor()
    {
        return mAudioPlaybackDeviceDescriptor;
    }

    /**
     * Audio provider managed by this audio output
     */
    public AudioProvider getAudioProvider()
    {
        return mAudioProvider;
    }

    /**
     * Process audio from the audio provider and writes it to the mixer's source data line.  Calls to this method can
     * block on the source data line until it has capacity available to accept the audio.
     */
    private void processAudio()
    {
        if(mSourceDataLine == null)
        {
            LOGGING_SUPPRESSOR.error("null output", 2,
                    "Audio Output source data line is null - ignoring audio playback request");
            return;
        }

        ByteBuffer buffer = mAudioProvider.getAudio();

        if(buffer != null)
        {
            //This is a blocking method call.
            int wrote = mSourceDataLine.write(buffer.array(), 0, buffer.array().length);

            //Around JDK 22 something started causing the source data line to fail to accept audio byte data via the
            //write() method. When this happens, close and reopen the data line to clear the error state.
            if(wrote <= 0)
            {
                LOGGING_SUPPRESSOR.info("Stalled Data Line", 3,
                        "Audio playback data line has stopped accepting samples - recycling to clear the error");
                mSourceDataLine.close();
                openSourceDataLine();
            }
        }
    }

    /**
     * Prepares this audio output for disposal.
     */
    public void dispose()
    {
        if(mProcessorFuture != null)
        {
            mProcessorFuture.cancel(true);
        }
        mProcessorFuture = null;

        if(mScheduledExecutorService != null)
        {
            mScheduledExecutorService.shutdownNow();
            mScheduledExecutorService = null;
        }

        mAudioProvider.dispose();
        mCanProcessAudio = false;

        if(mSourceDataLine != null)
        {
            mSourceDataLine.stop();
            mSourceDataLine.flush();
            mSourceDataLine.close();
        }
        mSourceDataLine = null;
        mGainControl = null;
        mRunning = false;
    }

    /**
     * Sets the mute state for this audio output channel
     */
    public void setMuted(boolean muted)
    {
        for(AudioChannel audioChannel: mAudioProvider.getAudioChannels())
        {
            audioChannel.setMuted(muted);
        }

        AudioEvent.Type type = muted ? AudioEvent.Type.AUDIO_MUTED : AudioEvent.Type.AUDIO_UNMUTED;
        mAudioProvider.notify(type);
    }

    /**
     * Current mute state for this audio output channel
     */
    public boolean isMuted()
    {
        if(!mAudioProvider.getAudioChannels().isEmpty())
        {
            return mAudioProvider.getAudioChannels().getFirst().isMuted();
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

    /**
     * Indicates if this audio output has a gain control available
     */
    public boolean hasGainControl()
    {
        return mGainControl != null;
    }

    /**
     * Monitors the source data line playback state and broadcasts audio events to the registered listener as the
     * state changes
     */
    @Override
    public void update(LineEvent event)
    {
        LineEvent.Type type = event.getType();

        if(type == LineEvent.Type.START)
        {
            mAudioProvider.notify(AudioEvent.Type.AUDIO_STARTED);
        }
        else if(type == LineEvent.Type.STOP)
        {
            mAudioProvider.notify(AudioEvent.Type.AUDIO_STOPPED);
        }
    }

    /**
     * Audio Processor thread
     */
    public class AudioProcessor implements Runnable
    {
        private final AtomicBoolean mProcessing = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                try
                {
                    processAudio();
                }
                catch(Throwable t)
                {
                    mLog.error("Error while processing audio buffers", t);
                }

                mProcessing.set(false);
            }
        }
    }
}
