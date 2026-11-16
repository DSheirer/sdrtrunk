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

package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EDACS control channel bit slicer.  Receives FM-demodulated samples at 48 kHz (5 samples per symbol at 9600 baud) and
 * feeds the sliced bits to the frame processors.
 *
 * <p>The symbol timing is not recovered.  A frame processor runs on each of the 5 sample phases and each frame is
 * sent once, from whichever phase decodes it first.  A frame only decodes when its words pass the BCH check, so a
 * phase that produces a frame is a usable phase.</p>
 *
 * <p>The DC tracker follows the mean of the discriminator output (carrier frequency offset plus any bias from the FM
 * demodulator).  It is slow against the bit rate so that it does not follow the data.</p>
 */
public class EDACSSyncDetector
{
    private final static Logger mLog = LoggerFactory.getLogger(EDACSSyncDetector.class);

    /** EDACS control channels are resampled to 48 kHz, or 5 samples/symbol. */
    private static final int PHASE_CANDIDATES = 5;

    /**
     * Samples after a frame during which only the phase that sent it may send again.  Longer than the 5 samples
     * within which the other phases decode the same frame, far shorter than the 1440 samples between frames, so the
     * phase is in effect chosen again for every frame.
     */
    private static final long PHASE_STALE_SAMPLES = 100L;

    /** DC tracker exponential moving average alpha. Slow enough to avoid tracking symbol data. */
    private static final double AFC_ALPHA = 0.001;

    /** DC bias clamp to prevent the tracker from wandering far from the carrier. */
    private static final float AFC_MAX = 2.0f;

    private final EDACSFrameProcessor[] mPhaseFrameProcessors = new EDACSFrameProcessor[PHASE_CANDIDATES];
    private long mSampleIndex;
    private int mActivePhase;
    private long mLastActiveDecodeSampleIndex;
    private boolean mLocked;
    private double mAfc;

    private final boolean mEsk;

    public EDACSSyncDetector()
    {
        this(false);
    }

    /**
     * Constructs an instance
     * @param esk true if the control channel uses EDACS Security Key (ESK)
     */
    public EDACSSyncDetector(boolean esk)
    {
        mEsk = esk;
        reset();
    }

    /**
     * Resets the detector, for example when the sample rate of the source changes.
     */
    public void reset()
    {
        for(int x = 0; x < mPhaseFrameProcessors.length; x++)
        {
            mPhaseFrameProcessors[x] = new EDACSFrameProcessor(mEsk);
        }

        mSampleIndex = 0;
        mActivePhase = 0;
        mLastActiveDecodeSampleIndex = -PHASE_STALE_SAMPLES - 1;
        mLocked = false;
        mAfc = 0.0;
    }

    /**
     * Processes demodulated samples at 48 kHz.
     * @param demodulated samples
     * @param messageListener to receive decoded messages
     */
    public void process(float[] demodulated, Listener<IMessage> messageListener)
    {
        for(float sample : demodulated)
        {
            mAfc = mAfc * (1.0 - AFC_ALPHA) + sample * AFC_ALPHA;
            if(mAfc > AFC_MAX) mAfc = AFC_MAX;
            if(mAfc < -AFC_MAX) mAfc = -AFC_MAX;

            int bit = (sample - (float)mAfc) >= 0 ? 0 : 1;
            int phase = (int)(mSampleIndex % PHASE_CANDIDATES);

            mPhaseFrameProcessors[phase].processBit(bit, message ->
            {
                if(messageListener == null)
                {
                    return;
                }

                //Send each frame once, from whichever phase decodes it first.  The phases that decode the same
                //frame finish within 5 samples of each other, so anything from another phase that soon after
                //is the same frame again.
                if(phase == mActivePhase || mSampleIndex - mLastActiveDecodeSampleIndex > PHASE_STALE_SAMPLES)
                {
                    mActivePhase = phase;
                    mLastActiveDecodeSampleIndex = mSampleIndex;

                    if(!mLocked)
                    {
                        mLocked = true;
                        mLog.debug("EDACS control channel locked on phase " + phase);
                    }

                    messageListener.receive(message);
                }
            });

            mSampleIndex++;
        }
    }
}
