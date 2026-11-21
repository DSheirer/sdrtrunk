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

package io.github.dsheirer.module.decode.edacs.provoice;

import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.edacs.EDACSDemodulator;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes a ProVoice voice channel - 9600 baud FSK - into voice frames for the audio module.
 *
 * Each 80 ms frame starts with a 32-bit sync; both the conventional/standard and the Extended Addressing sync are
 * searched, in either polarity (MBTA transmits it inverted).
 *
 * Damaged voice frames are concealed the way mbelib does it: a voice frame with more than MAX_ERRORS corrected bit
 * errors is replaced by the last good one, and after MAX_REPEATS replacements in a row by silence.  Without this the
 * damaged frames reach JMBE looking error free (they are re-encoded cleanly) and play as warbles (MBTA 2026-11-09:
 * about 9% of voice frames on LCN 5 recorded in Somerville).
 */
public class ProVoiceDecoder extends Decoder implements IComplexSamplesListener, Listener<ComplexSamples>,
    ISourceEventListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProVoiceDecoder.class);

    /** Sync patterns from DSD-FME (symbol 3 = 1, symbol 1 = 0) */
    public static final long SYNC = pattern("13131333111311311133113311331133");
    public static final long SYNC_EA = pattern("31131311331331111133131311311133");
    private static final int MAX_SYNC_ERRORS = 2;
    /** mbelib's threshold: more corrected errors than this and the frame is not trusted */
    public static final int MAX_ERRORS = 5;
    public static final int MAX_REPEATS = 3;
    /**
     * Corrected bit errors from which a voice frame is not trusted when another voice frame of the
     * same 80 ms frame failed the error check.
     *
     * The error check is weak against data that is not voice: the three Golay words "correct" any 23 bits to some
     * code word, so a voice frame's worth of noise passes with 5 errors or fewer about once in a hundred.  The four
     * voice frames of a frame are sent interleaved in pairs, so damage rarely hits one and spares its neighbours:
     * when one fails, one that passes with errors is nearly always wrong too.  Measured on the 33 calls recorded at
     * Cambridge and Summit 800 on 2026-11-12, with Cambridge's error-free copy as the truth: of 75 such voice frames
     * at Summit 800, 74 were wrong and 1 right.  Played, they are the short digital noises in a damaged call and in
     * what follows the end of some transmissions (MBTA 2026-11-13 19:00:28, 853.9250: seven in a row).
     */
    public static final int SUSPECT_ERRORS = 2;
    /** Bits without a frame after which the next frame starts a new transmission (about 0.25 s) */
    private static final long NEW_TRANSMISSION_BITS = 2400;

    private final Listener<SourceEvent> mSourceEventListener = this::process;
    private final int[] mFrame = new int[ProVoiceFrameDecoder.FRAME_BITS];
    private EDACSDemodulator mDemodulator;
    private double mSampleRate;
    private long mShift;
    private long mBitCount;
    private int mFrameIndex = -1;
    private boolean mInverted;
    private long mTimestamp;
    private long mFrameCount;
    private long mErrorCount;
    private long mConcealedCount;
    private byte[] mLastGood;
    private int mRepeats;
    private long mLastFrameBit = -NEW_TRANSMISSION_BITS;

    private static long pattern(String symbols)
    {
        long value = 0;

        for(char c : symbols.toCharArray())
        {
            value = (value << 1) | (c == '3' ? 1 : 0);
        }

        return value;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.EDACS;
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventListener;
    }

    /**
     * Sets the channel sample rate.  Public for offline testing.
     */
    public void setSampleRate(double sampleRate)
    {
        if(mDemodulator == null || Math.abs(mSampleRate - sampleRate) > 1.0)
        {
            mSampleRate = sampleRate;
            mDemodulator = new EDACSDemodulator(sampleRate, this::process);
        }
    }

    private void process(SourceEvent sourceEvent)
    {
        if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
        {
            setSampleRate(sourceEvent.getValue().doubleValue());
        }
    }

    @Override
    public void receive(ComplexSamples samples)
    {
        EDACSDemodulator demodulator = mDemodulator;

        if(demodulator != null)
        {
            mTimestamp = samples.timestamp();
            demodulator.process(samples.i(), samples.q(), samples.i().length);
        }
    }

    /**
     * Processes one demodulated bit.  Public for testing.
     */
    public void process(boolean bit)
    {
        mBitCount++;

        if(mFrameIndex >= 0)
        {
            mFrame[mFrameIndex++] = (bit ^ mInverted) ? 1 : 0;

            if(mFrameIndex == mFrame.length)
            {
                mFrameIndex = -1;
                mShift = 0;
                dispatch();
            }
            return;
        }

        mShift = ((mShift << 1) | (bit ? 1 : 0)) & 0xFFFFFFFFL;

        if(mBitCount < 32)
        {
            return;
        }

        for(long sync : new long[]{SYNC_EA, SYNC})
        {
            int errors = Long.bitCount(mShift ^ sync);

            if(errors <= MAX_SYNC_ERRORS || 32 - errors <= MAX_SYNC_ERRORS)
            {
                mInverted = errors > MAX_SYNC_ERRORS;
                mFrameIndex = 0;
                return;
            }
        }
    }

    private void dispatch()
    {
        if(mBitCount - mLastFrameBit > NEW_TRANSMISSION_BITS)
        {
            logTransmission();
            mLastGood = null; //new transmission - nothing to repeat
            mRepeats = 0;
            mHeaderDetector.reset();
            mSignedOff = false;
        }
        mLastFrameBit = mBitCount;

        ProVoiceFrameDecoder.VoiceFrame[] voiceFrames = ProVoiceFrameDecoder.decode(mFrame);

        //The header frames a transmission starts with are not voice and are not played
        int[] frameErrors = new int[voiceFrames.length];

        for(int x = 0; x < voiceFrames.length; x++)
        {
            frameErrors[x] = voiceFrames[x].errors();
        }

        //A header that can be read is a header wherever it is, and gives the talkgroup
        int talkgroup = ProVoiceHeader.talkgroup(mFrame);

        if(talkgroup != ProVoiceHeader.NO_HEADER)
        {
            if(mTransmissionFrames > 0 || mSignedOff)
            {
                //Voice was played already: this is the next transmission, started before the gap that normally
                //separates two (MBTA 2026-11-12 15:17: 0.23 s after the one before)
                logTransmission();
                mLastGood = null;
                mRepeats = 0;
                mSignedOff = false;
                mBackToBackCount++;
            }

            mHeaderDetector.header(mFrame);
            mHeaderCount++;
            mTransmissionHeaders++;

            if(talkgroup != mTransmissionTalkgroup)
            {
                mTransmissionTalkgroup = talkgroup;
                getMessageListener().receive(new ProVoiceHeaderMessage(talkgroup, mTimestamp));
            }

            return;
        }

        if(mHeaderDetector.isHeader(mFrame, frameErrors))
        {
            mHeaderCount++;
            mTransmissionHeaders++;
            return;
        }

        //The sign-off at the end of a transmission is not voice and is not played
        boolean[] signOff = signOff(frameErrors);

        List<byte[]> imbeFrames = new ArrayList<>(4);
        int errors = 0;
        long concealedBefore = mConcealedCount;
        boolean damaged = false;

        for(int x = 0; x < frameErrors.length; x++)
        {
            //A voice frame that fails because it is the sign-off says nothing about the ones before it
            damaged |= !signOff[x] && frameErrors[x] > MAX_ERRORS;
        }

        for(int x = 0; x < voiceFrames.length; x++)
        {
            if(signOff[x])
            {
                mSignOffCount++;
                mTransmissionSignOff++;
                continue;
            }

            ProVoiceFrameDecoder.VoiceFrame voiceFrame = voiceFrames[x];
            int voiceErrors = voiceFrame.errors();

            if(damaged && voiceErrors >= SUSPECT_ERRORS && voiceErrors <= MAX_ERRORS)
            {
                //Passed the check beside one that failed - not trusted, concealed like it
                voiceErrors = MAX_ERRORS + 1;
                mSuspectCount++;
            }

            imbeFrames.add(conceal(IMBEFrameEncoder.encode(voiceFrame.parameters()), voiceErrors));
            errors += voiceFrame.errors();
        }

        if(imbeFrames.isEmpty())
        {
            return;
        }

        int concealed = (int)(mConcealedCount - concealedBefore);
        mFrameCount++;
        mErrorCount += errors;
        //Position of the damage within the transmission (measurement only)
        mDamagePosition.add(concealed);

        mTransmissionFrames++;
        mTransmissionErrors += errors;
        mTransmissionConcealed += concealed;
        getMessageListener().receive(new ProVoiceMessage(imbeFrames, errors, concealed, mTimestamp));
    }

    private long mTransmissionFrames;
    private long mTransmissionErrors;
    private long mTransmissionConcealed;
    private final ProVoiceDamagePosition mDamagePosition = new ProVoiceDamagePosition();
    private final ProVoiceHeaderDetector mHeaderDetector = new ProVoiceHeaderDetector();
    private long mTransmissionHeaders;
    private long mHeaderCount;
    private long mTransmissionSignOff;
    private long mSignOffCount;
    private boolean mSignedOff;
    private int mTransmissionTalkgroup = ProVoiceHeader.NO_HEADER;
    private long mBackToBackCount;
    private long mSuspectCount;

    /** Alternating bits (dotting) that mark the end of a transmission.  The longest run in 1,874 voice frames of the
     * MBTA recording of 2026-11-09 that were not the last of their transmission was 15. */
    static final int DOTTING_BITS = 16;

    /**
     * Decides which of the frame's four voice frames belong to the sign-off at the end of the
     * transmission and must not be played.
     *
     * A transmission ends with dotting - alternating bits - and what the transmitter sends from there on is not
     * voice.  The dotting usually starts part way through a frame (MBTA: most often at bit 496, after the first pair
     * of voice frames), and up to half a second of other non-voice frames can follow before the carrier drops.
     * Until this the decoder read all of it as damaged voice: it repeated the last good 20 ms up to three times and
     * played whatever passed the error check by chance - 2 or 4 "damaged" voice frames at the end of nearly every
     * call, identical bit for bit at Cambridge and Summit 800 (2026-11-12), about 20% of the last 0.48 s.
     *
     * The sign-off is recognized by dotting with both voice frames of the pair it starts in failing the error check
     * - a clean voice frame can hold 17 or 18 alternating bits, so dotting alone is not enough.  In that frame the
     * voice frames before the dotting that pass the check are played; from then on nothing is played until the next
     * transmission, or until a frame reads as voice again (3 of its 4 voice frames pass), which undoes a false
     * detection.
     */
    private boolean[] signOff(int[] frameErrors)
    {
        boolean[] drop = new boolean[frameErrors.length];
        int dotting = dottingStart(mFrame);
        int passing = 0;

        for(int error : frameErrors)
        {
            if(error <= MAX_ERRORS)
            {
                passing++;
            }
        }

        if(mSignedOff)
        {
            //After the sign-off has started: nothing is played unless the frame reads as voice again
            if(dotting < 0 && passing >= ProVoiceHeaderDetector.VOICE_FRAMES_NEEDED)
            {
                mSignedOff = false;
            }
            else
            {
                java.util.Arrays.fill(drop, true);
            }

            return drop;
        }

        if(dotting >= 0)
        {
            int pair = dotting < ProVoiceFrameDecoder.FIRST_PAIR_END ? 0 : 2;

            if(frameErrors[pair] > MAX_ERRORS && frameErrors[pair + 1] > MAX_ERRORS)
            {
                //The sign-off starts in this frame: the voice frames before it that pass the check are the last
                //of the voice
                mSignedOff = true;

                for(int x = 0; x < drop.length; x++)
                {
                    drop[x] = x >= pair || frameErrors[x] > MAX_ERRORS;
                }
            }
        }

        return drop;
    }

    /**
     * Start of the first run of at least DOTTING_BITS alternating bits, or -1.  Package-private for testing.
     */
    static int dottingStart(int[] frame)
    {
        int run = 1;

        for(int x = 1; x < frame.length; x++)
        {
            if(frame[x] != frame[x - 1])
            {
                if(++run >= DOTTING_BITS)
                {
                    return x - run + 1;
                }
            }
            else
            {
                run = 1;
            }
        }

        return -1;
    }

    /**
     * Voice frames (20 ms) at the end of transmissions that were sign-off, not voice, and were not played
     */
    public long getSignOffCount()
    {
        return mSignOffCount;
    }

    /**
     * Diagnostics (DEBUG): one line per transmission - frames, corrected bit errors and damaged voice frames, and
     * where in the transmission the damaged voice frames fall (start, middle, end).
     */
    private void logTransmission()
    {
        if(mTransmissionFrames > 0 && LOGGER.isDebugEnabled())
        {
            LOGGER.debug(String.format("ProVoice transmission: %.1f s, %.2f bit errors per voice frame, %d of %d " +
                    "voice frames damaged (%.1f%%), sync %s, header %.2f s and sign-off %.2f s not played%s",
                mTransmissionFrames * 0.08,
                mTransmissionErrors / (4.0 * mTransmissionFrames), mTransmissionConcealed, 4 * mTransmissionFrames,
                100.0 * mTransmissionConcealed / (4.0 * mTransmissionFrames), mInverted ? "inverted" : "normal",
                mTransmissionHeaders * 0.08, mTransmissionSignOff * 0.02,
                mTransmissionTalkgroup != ProVoiceHeader.NO_HEADER ? ", header talkgroup " + mTransmissionTalkgroup :
                    ", header not read"));
            LOGGER.debug(mDamagePosition.describe());
        }
        else if(mTransmissionHeaders > 0 && LOGGER.isDebugEnabled())
        {
            LOGGER.debug(String.format("ProVoice transmission: no voice - %.2f s of header only, not played",
                mTransmissionHeaders * 0.08));
        }

        mTransmissionHeaders = 0;
        mTransmissionSignOff = 0;
        mTransmissionTalkgroup = ProVoiceHeader.NO_HEADER;

        mTransmissionFrames = 0;
        mTransmissionErrors = 0;
        mTransmissionConcealed = 0;
        mDamagePosition.reset();
    }

    @Override
    public void stop()
    {
        logTransmission();
        super.stop();
    }

    /**
     * Frame concealment: returns the frame to play - this one, the last good one repeated, or null for silence.
     * Package-private for testing.
     */
    byte[] conceal(byte[] frame, int errors)
    {
        if(errors <= MAX_ERRORS)
        {
            mLastGood = frame;
            mRepeats = 0;
            return frame;
        }

        mConcealedCount++;

        if(mLastGood != null && mRepeats < MAX_REPEATS)
        {
            mRepeats++;
            return mLastGood;
        }

        mLastGood = null;
        return null;
    }

    /**
     * Voice frames (20 ms) that passed the error check but were concealed because another voice frame of the same
     * frame failed it
     */
    public long getSuspectCount()
    {
        return mSuspectCount;
    }

    /**
     * Voice frames (20 ms) replaced by a repeat or silence
     */
    public long getConcealedCount()
    {
        return mConcealedCount;
    }

    public long getFrameCount()
    {
        return mFrameCount;
    }

    /**
     * Frames (80 ms) at the start of transmissions that were header, not voice, and were not played
     */
    public long getHeaderCount()
    {
        return mHeaderCount;
    }

    /**
     * Transmissions that started less than a quarter of a second after the one before, told apart by their header
     */
    public long getBackToBackCount()
    {
        return mBackToBackCount;
    }

    public long getErrorCount()
    {
        return mErrorCount;
    }
}
