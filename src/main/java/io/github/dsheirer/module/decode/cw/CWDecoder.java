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

package io.github.dsheirer.module.decode.cw;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventListener;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import java.util.Arrays;
import java.util.List;

/**
 * Auxiliary Morse code (CW) station ID decoder for the demodulated audio of an NBFM channel.
 *
 * The tone finder checks each 32 millisecond frame (every 16 milliseconds) for a steady tone between 325 and 2000 Hz.
 * A tone starts a tone episode and the audio is buffered until no tone has been detected for one second, or until the
 * NBFM noise squelch closes.  The episode audio is then decoded at the most common tone frequency: the keying is
 * extracted as key-down/key-up durations and parsed as Morse code.  Only keying that is consistent with Morse code
 * timing produces a {@link CWMessage}, so voice and data signals that also contain tones are ignored.
 */
public class CWDecoder extends Decoder implements IRealBufferListener, Listener<float[]>, IDecoderStateEventListener
{
    public static final double SAMPLE_RATE = 8000.0;
    private static final int FRAME_LENGTH = 256;
    private static final int FRAME_HOP = 128;
    private static final int HISTORY_LENGTH = 1024;
    private static final int PREROLL_LENGTH = 512;
    private static final int END_OF_EPISODE_SAMPLES = (int)SAMPLE_RATE;
    private static final int MAXIMUM_EPISODE_SAMPLES = (int)(SAMPLE_RATE * 30);
    private static final double MINIMUM_FREQUENCY_CONSISTENCY = 0.5;

    private final CWToneFinder mToneFinder = new CWToneFinder(SAMPLE_RATE, FRAME_LENGTH);
    private final Listener<DecoderStateEvent> mDecoderStateEventListener = this::receiveDecoderStateEvent;
    private final float[] mHistory = new float[HISTORY_LENGTH];
    private int mHistoryPointer;
    private int mSamplesSinceFrame;

    //Tone episode
    private boolean mEpisodeActive;
    private float[] mEpisode = new float[(int)SAMPLE_RATE * 4];
    private int mEpisodeLength;
    private int mSamplesSinceTone;
    private long mEpisodeTimestamp;
    private final int[] mToneBinCounts = new int[CWToneFinder.getBinCount()];

    /**
     * Constructs an instance
     */
    public CWDecoder()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.CW_ID;
    }

    @Override
    public Listener<float[]> getBufferListener()
    {
        return this;
    }

    @Override
    public void receive(float[] samples)
    {
        for(float sample: samples)
        {
            mHistory[mHistoryPointer] = sample;
            mHistoryPointer = (mHistoryPointer + 1) % HISTORY_LENGTH;

            if(mEpisodeActive)
            {
                appendEpisode(sample);
                mSamplesSinceTone++;
            }

            if(++mSamplesSinceFrame >= FRAME_HOP)
            {
                mSamplesSinceFrame = 0;
                processFrame();
            }
        }
    }

    /**
     * Checks the most recent frame for a tone, starts a tone episode on a tone and ends the episode after a period
     * without a tone.
     */
    private void processFrame()
    {
        int frameStart = (mHistoryPointer - FRAME_LENGTH + HISTORY_LENGTH) % HISTORY_LENGTH;
        int bin = mToneFinder.find(mHistory, frameStart);

        if(bin != CWToneFinder.NO_TONE)
        {
            if(!mEpisodeActive)
            {
                startEpisode();
            }

            mToneBinCounts[bin]++;
            mSamplesSinceTone = 0;
        }

        if(mEpisodeActive)
        {
            if(mSamplesSinceTone >= END_OF_EPISODE_SAMPLES)
            {
                endEpisode();
            }
            else if(mEpisodeLength >= MAXIMUM_EPISODE_SAMPLES)
            {
                //Continuous tones (e.g. voice) - not a CW ID
                clearEpisode();
            }
        }
    }

    /**
     * Starts a tone episode with the recent audio history as pre-roll.
     */
    private void startEpisode()
    {
        clearEpisode();
        mEpisodeActive = true;
        mEpisodeTimestamp = System.currentTimeMillis();

        for(int x = 0; x < PREROLL_LENGTH; x++)
        {
            appendEpisode(mHistory[(mHistoryPointer - PREROLL_LENGTH + x + HISTORY_LENGTH) % HISTORY_LENGTH]);
        }
    }

    private void appendEpisode(float sample)
    {
        if(mEpisodeLength == mEpisode.length)
        {
            mEpisode = Arrays.copyOf(mEpisode, Math.min(MAXIMUM_EPISODE_SAMPLES + FRAME_HOP, mEpisode.length * 2));
        }

        if(mEpisodeLength < mEpisode.length)
        {
            mEpisode[mEpisodeLength++] = sample;
        }
    }

    /**
     * Clears the current episode.
     */
    private void clearEpisode()
    {
        mEpisodeActive = false;
        mEpisodeLength = 0;
        mSamplesSinceTone = 0;
        Arrays.fill(mToneBinCounts, 0);
    }

    /**
     * Ends the tone episode and decodes it.
     */
    private void endEpisode()
    {
        if(mEpisodeActive)
        {
            CWMessage message = decode(Arrays.copyOf(mEpisode, mEpisodeLength), mToneBinCounts, mEpisodeTimestamp);

            if(message != null)
            {
                getMessageListener().receive(message);
            }
        }

        clearEpisode();
    }

    /**
     * Decodes the episode audio.
     * @param audio of the episode
     * @param toneBinCounts number of frames where each tone bin was detected
     * @param timestamp of the start of the episode
     * @return decoded message or null
     */
    static CWMessage decode(float[] audio, int[] toneBinCounts, long timestamp)
    {
        //Most common tone frequency, allowing for the tone to fall between adjacent bins
        int best = 0;
        int bestCount = 0;
        int total = 0;

        for(int bin = 0; bin < toneBinCounts.length; bin++)
        {
            total += toneBinCounts[bin];
            int count = toneBinCounts[bin] + (bin > 0 ? toneBinCounts[bin - 1] : 0) +
                (bin < toneBinCounts.length - 1 ? toneBinCounts[bin + 1] : 0);

            if(count > bestCount)
            {
                bestCount = count;
                best = bin;
            }
        }

        if(total == 0 || bestCount < total * MINIMUM_FREQUENCY_CONSISTENCY)
        {
            return null;
        }

        double frequency = getWeightedFrequency(toneBinCounts, best);
        List<MorseCodeParser.KeyingRun> runs = CWKeyingExtractor.extract(audio, SAMPLE_RATE, frequency);
        MorseCodeParser.Result result = MorseCodeParser.parse(runs);

        if(result == null)
        {
            return null;
        }

        return new CWMessage(timestamp, result.text(), (int)(Math.round(frequency / 10.0) * 10),
            result.wordsPerMinute());
    }

    /**
     * Tone frequency as the count-weighted average of the best bin and its neighbors.
     */
    private static double getWeightedFrequency(int[] counts, int bin)
    {
        double sum = 0.0;
        double weight = 0.0;

        for(int x = Math.max(0, bin - 1); x <= Math.min(counts.length - 1, bin + 1); x++)
        {
            sum += counts[x] * CWToneFinder.getFrequency(x);
            weight += counts[x];
        }

        return weight > 0 ? sum / weight : CWToneFinder.getFrequency(bin);
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return mDecoderStateEventListener;
    }

    /**
     * Decodes any tone episode when the NBFM decoder reports that the channel is idle (squelch closed), since the
     * audio stream is not continuous across the squelch closure.
     */
    private void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if((event.getState() == State.IDLE && event.getEvent() == DecoderStateEvent.Event.CONTINUATION) ||
            event.getEvent() == DecoderStateEvent.Event.REQUEST_RESET)
        {
            if(mEpisodeActive)
            {
                endEpisode();
            }
        }
    }

    @Override
    public void reset()
    {
        endEpisode();
        Arrays.fill(mHistory, 0.0f);
        mHistoryPointer = 0;
        mSamplesSinceFrame = 0;
    }

    @Override
    public void stop()
    {
        reset();
        super.stop();
    }
}
