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

package io.github.dsheirer.dsp.c4fm;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;

/**
 * Differential QPSK (ie P25 Phase 1 LSM/CQPSK and P25 Phase 2 HDQPSK) demodulator that produces a real valued sample
 * stream for the C4FM symbol processor.  Each output sample is the phase change across the preceding symbol period, in radians per sample (ie
 * the symbol phase divided by the samples per symbol), which is the same domain as the C4FM decoder's FM demodulated
 * and receive filtered sample stream.
 *
 * Processing steps:
 *
 * 1. FM discriminator (atan2) produces the phase change between consecutive samples.  A full range (atan2)
 * discriminator is required because the LSM signal trajectory passes near the origin between symbols where the phase
 * change between samples can exceed PI/2, which would fold with an atan(Q/I) discriminator.
 * 2. Coarse frequency offset removal: subtracts the estimated carrier offset from each discriminator sample.
 * 3. One symbol period integrator: the moving sum of the discriminator output is the phase change across the symbol
 * period: arg(x[n] * conj(x[n - samplesPerSymbol])), modulo 2 * PI.
 * 4. Phase wrap to +/- PI.  The moving sum is only correct modulo 2 * PI since a discriminator sample near the origin
 * can be wrapped by 2 * PI, and that error persists across the integrator span.
 * 5. Carrier offset estimation: the fourth power of the differential QPSK symbols removes the modulation so that the
 * residual phase is four times the carrier offset (radians per symbol).  This is a constellation domain estimator,
 * which isn't biased by the spectral tilt of a frequency selective (ie simulcast multipath) channel the way that a
 * spectral centroid (ie mean discriminator output) estimator is.
 *
 * The fourth power estimator is ambiguous modulo PI/2 radians per symbol (1200 Hz at 4800 baud, 1500 Hz at 6000 baud),
 * so the estimator only resolves offsets within +/- 1/8 of the symbol rate (ie +/- 600 Hz at 4800 baud).  When a signal is present (ie the fourth power coherence exceeds the noise
 * threshold) but the symbol processor is unable to synchronize, the estimate is stepped by +/- PI/2 radians per symbol
 * to search the adjacent offset ranges.  An offset just beyond the range aliases to an estimate near the opposite edge
 * of the range (ie -650 Hz estimates as +550 Hz), so the search first steps toward the adjacent range nearest to the
 * estimate (ie -1200 Hz for a positive estimate) and then the other adjacent range.  The fourth power coherence is the
 * same at each ambiguous offset, so high coherence without synchronization indicates an incorrect offset range, while
 * low coherence indicates that there is no signal.  The search holds the current offset range while there is no signal
 * (ie between transmissions on a traffic channel) so that the next transmission starts with the offset range that last
 * synchronized.  Once an offset range is verified by the decoder (ie a valid P25 Phase 1 NID or a valid P25 Phase 2
 * MAC message, see offsetVerified()), the search uses a longer dwell period before stepping away from it.  Symbol
 * processor synchronization alone doesn't verify the offset range since a false sync detection in an incorrect offset
 * range would otherwise hold the search in that range for the longer dwell period.
 *
 * Soft symbol reliability: the demodulated phase change across the symbol period is the difference between the phases
 * of the samples at either end of the period, and the phase noise variance of each sample is inversely proportional to
 * its power, so the noise variance of the phase change is proportional to 1/|x[n]|^2 + 1/|x[n - samplesPerSymbol]|^2.
 * The reliability (inverse noise variance) of each demodulated sample, (|x[n]|^2 * |x[n - samplesPerSymbol]|^2) /
 * (|x[n]|^2 + |x[n - samplesPerSymbol]|^2), weights the soft decision decoding (see getReliabilities()).  The LSM
 * signal envelope passes near the origin between symbols and simulcast multipath produces deep envelope fades, so the
 * symbol error rate varies by orders of magnitude with the envelope.  Testing against a simulcast control channel
 * recording found that 60% of the symbol errors occur in the 15% of the symbols with the lowest envelope.
 *
 * Since the carrier offset is removed ahead of the symbol processor, the symbol processor's balance (PLL) value only
 * represents the residual offset.  Use getFrequencyOffset() to include the coarse offset when reporting the carrier
 * offset to the tuner.
 */
public class DQPSKDifferentialDemodulator
{
    private static final double TWO_PI = 2.0 * Math.PI;
    //Carrier offset ambiguity step of the fourth power estimator, in radians per symbol
    private static final double AMBIGUITY_STEP = Math.PI / 2.0;
    //The estimator, signal presence detection and ambiguity search update once per block of symbols, independent of
    //the sample buffer size.  The coherence (magnitude of the averaged unit vectors) of noise depends on the number of
    //samples averaged, so a fixed block length also keeps the signal presence threshold independent of buffer size.
    private static final int ESTIMATOR_BLOCK_SYMBOLS = 200;
    //Coarse offset estimator loop gain applied to each block's fourth power phase error while the symbol processor is
    //synchronized (~6,600 symbol time constant)
    private static final double ESTIMATOR_GAIN = 0.03;
    //Estimator loop gain while the symbol processor is unsynchronized, so that the estimate converges within the
    //ambiguity search dwell period.  With the tracking gain, the estimate converged over several dwell periods and an
    //offset near the edge of the estimator range (ie -550 Hz) took longer to acquire than offsets beyond the range.
    private static final double ESTIMATOR_ACQUISITION_GAIN = 0.2;
    //Block coherence at and above which the estimator applies the full loop gain
    private static final double ESTIMATOR_FULL_GAIN_COHERENCE = 0.3;
    //Minimum average fourth power coherence (magnitude of the averaged unit vectors) to detect signal presence, to update
    //the estimate and to advance the ambiguity search.  LSM recordings produce values above 0.3 without injected noise
    //and average 0.11-0.14 at 0 dB carrier to noise (50 kHz).  Single block coherence values for noise reach 0.10, so
    //the coherence is averaged across blocks, where the maximum noise value is 0.041 across 10 minutes of noise.
    private static final double SIGNAL_MINIMUM_COHERENCE = 0.07;
    //Coherence averaging gain, applied per block (~10 blocks or ~2,000 symbols)
    private static final double COHERENCE_AVERAGING_GAIN = 0.1;
    //Symbol count with a signal present but without synchronization before stepping to the next ambiguity search offset
    private static final int SEARCH_DWELL_SYMBOLS = 2000;
    //Symbol count with a signal present but without synchronization before stepping away from an offset range that
    //was verified (~5 seconds).  At low SNR, sync is frequently lost and reacquired, and the shorter search dwell caused
    //the search to spend half of the time in an incorrect offset range at 0 dB carrier to noise.
    private static final int VERIFIED_DWELL_SYMBOLS = 24000;
    //Ambiguity search offsets, as multiples of the ambiguity step, relative to the estimator's offset.  The search
    //direction sets the sign of the adjacent range steps.
    private static final int[] SEARCH_STEPS = {0, 1, -1};

    private IRealFilter mIntegrator;
    //Sample power history spanning the reliability symbol period delay, carried across sample buffers
    private float[] mPowerHistory = new float[1];
    private float[] mReliabilities = new float[0];
    private double mSamplesPerSymbol = 1.0;
    private float mPreviousI;
    private float mPreviousQ;
    //Fourth power estimator offset, radians per symbol
    private double mEstimatorOffset = 0.0;
    private int mSearchIndex = 0;
    private int mSearchDirection = 1;
    private double mUnsynchronizedSignalSymbols = 0;
    //Indicates the current search offset range was verified by the decoder since it was selected
    private boolean mSearchIndexVerified = false;
    private double mAverageCoherence = 0.0;
    //Block length in samples and the fourth power accumulation for the block under accumulation
    private int mBlockLength = ESTIMATOR_BLOCK_SYMBOLS;
    private int mBlockPointer = 0;
    private double mBlockI = 0.0;
    private double mBlockQ = 0.0;

    /**
     * Constructs an instance
     */
    public DQPSKDifferentialDemodulator()
    {
    }

    /**
     * Sets or updates the samples per symbol.
     * @param samplesPerSymbol for the incoming sample stream
     */
    public void setSamplesPerSymbol(double samplesPerSymbol)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mIntegrator = new RealFIRFilter(FilterFactory.getDifferentialIntegrator(samplesPerSymbol));
        mPowerHistory = new float[Math.max(1, (int)Math.round(samplesPerSymbol))];
        mBlockLength = (int)Math.round(ESTIMATOR_BLOCK_SYMBOLS * samplesPerSymbol);
        resetBlock();
    }

    /**
     * Resets the carrier offset estimate after a tuner center frequency change.
     */
    public void reset()
    {
        mEstimatorOffset = 0.0;
        mSearchIndex = 0;
        mSearchDirection = 1;
        mUnsynchronizedSignalSymbols = 0;
        mSearchIndexVerified = false;
        mAverageCoherence = 0.0;
        resetBlock();
    }

    /**
     * Resets the fourth power accumulation for the block under accumulation.
     */
    private void resetBlock()
    {
        mBlockPointer = 0;
        mBlockI = 0.0;
        mBlockQ = 0.0;
    }

    /**
     * Notification that the decoder verified the current carrier offset range (ie a valid NID or MAC message was
     * decoded) so that the ambiguity search uses the longer dwell period before stepping away from it.
     */
    public void offsetVerified()
    {
        mSearchIndexVerified = true;
    }

    /**
     * Carrier offset (coarse) removed by this demodulator.
     * @return offset in radians per sample.
     */
    public float getFrequencyOffset()
    {
        return (float)(getOffset() / mSamplesPerSymbol);
    }

    /**
     * Soft symbol reliabilities for each of the samples produced by the most recent demodulate() call.
     * @return reliabilities (inverse of the relative phase noise variance), parallel to the demodulated samples.
     */
    public float[] getReliabilities()
    {
        return mReliabilities;
    }

    /**
     * Current total offset including the ambiguity search step.
     * @return offset in radians per symbol.
     */
    private double getOffset()
    {
        return mEstimatorOffset + SEARCH_STEPS[mSearchIndex] * mSearchDirection * AMBIGUITY_STEP;
    }

    /**
     * Demodulates the filtered complex samples.
     * @param i inphase samples
     * @param q quadrature samples
     * @param processorSynchronized indicates if the symbol processor is currently synchronized to the signal, which
     * controls the ambiguity search.
     * @return differentially demodulated samples in radians per sample.
     */
    public float[] demodulate(float[] i, float[] q, boolean processorSynchronized)
    {
        float[] discriminated = new float[i.length];
        float previousI = mPreviousI;
        float previousQ = mPreviousQ;
        float demodulatedI, demodulatedQ;

        for(int x = 0; x < i.length; x++)
        {
            //Multiply the current sample by the complex conjugate of the previous sample
            demodulatedI = (i[x] * previousI) + (q[x] * previousQ);
            demodulatedQ = (q[x] * previousI) - (i[x] * previousQ);
            discriminated[x] = (float)Math.atan2(demodulatedQ, demodulatedI);
            previousI = i[x];
            previousQ = q[x];
        }

        mPreviousI = previousI;
        mPreviousQ = previousQ;
        mReliabilities = calculateReliabilities(i, q);

        float[] demodulated = mIntegrator.filter(discriminated);

        //Remove the carrier offset (the integrator has unity gain at DC, so the offset is removed after integration to
        //apply each block's updated estimate), wrap each sample to +/- PI radians per symbol, and accumulate the fourth
        //power of each symbol phase.
        double samplesPerSymbol = mSamplesPerSymbol;
        float wrap = (float)(TWO_PI / samplesPerSymbol);
        float offset = getFrequencyOffset();
        double theta;

        for(int x = 0; x < demodulated.length; x++)
        {
            demodulated[x] -= offset;
            demodulated[x] -= wrap * Math.round(demodulated[x] / wrap);
            theta = 4.0 * demodulated[x] * samplesPerSymbol;
            mBlockI += Math.cos(theta);
            mBlockQ += Math.sin(theta);

            if(++mBlockPointer >= mBlockLength)
            {
                updateEstimate(processorSynchronized);
                offset = getFrequencyOffset();
            }
        }

        return demodulated;
    }

    /**
     * Updates the carrier offset estimate, the signal presence detection and the ambiguity search from the fourth power
     * accumulation for the completed block.
     * @param processorSynchronized state of the symbol processor
     */
    private void updateEstimate(boolean processorSynchronized)
    {
        //Ideal symbols (+/- PI/4 and +/- 3PI/4) all map to PI in the fourth power, so the residual angle is four times
        //the residual carrier offset (radians per symbol).
        double coherence = Math.sqrt(mBlockI * mBlockI + mBlockQ * mBlockQ) / mBlockPointer;
        mAverageCoherence += (coherence - mAverageCoherence) * COHERENCE_AVERAGING_GAIN;
        boolean signalPresent = mAverageCoherence > SIGNAL_MINIMUM_COHERENCE;

        if(signalPresent)
        {
            //Weight the update by this block's coherence so that noise blocks (ie after a transmission ends, while the
            //average coherence decays) don't move the estimate.
            double error = Math.atan2(-mBlockQ, -mBlockI) / 4.0;
            double weight = Math.min(1.0, coherence / ESTIMATOR_FULL_GAIN_COHERENCE);
            double gain = processorSynchronized ? ESTIMATOR_GAIN : ESTIMATOR_ACQUISITION_GAIN;
            mEstimatorOffset += error * gain * weight;
            //Constrain the estimator to the fundamental range.  The ambiguity search covers the adjacent ranges.
            mEstimatorOffset = Math.clamp(mEstimatorOffset, -AMBIGUITY_STEP / 2, AMBIGUITY_STEP / 2);
        }

        updateSearch(mBlockPointer, processorSynchronized, signalPresent);
        resetBlock();
    }

    /**
     * Calculates the reliability of each demodulated sample from the power of the samples at either end of the symbol
     * period: (|x[n]|^2 * |x[n - samplesPerSymbol]|^2) / (|x[n]|^2 + |x[n - samplesPerSymbol]|^2)
     * @param i inphase samples
     * @param q quadrature samples
     * @return reliabilities
     */
    private float[] calculateReliabilities(float[] i, float[] q)
    {
        float[] history = mPowerHistory;
        int delay = history.length;
        float[] power = new float[i.length];
        float[] reliabilities = new float[i.length];
        float current, delayed, sum;

        for(int x = 0; x < i.length; x++)
        {
            power[x] = i[x] * i[x] + q[x] * q[x];
        }

        for(int x = 0; x < i.length; x++)
        {
            current = power[x];
            delayed = x >= delay ? power[x - delay] : history[x];
            sum = current + delayed;
            reliabilities[x] = sum > 0.0f ? current * delayed / sum : 0.0f;
        }

        //Retain the final (delay) sample powers for the next buffer, shifting forward when the buffer is short
        if(i.length >= delay)
        {
            System.arraycopy(power, i.length - delay, history, 0, delay);
        }
        else
        {
            System.arraycopy(history, i.length, history, 0, delay - i.length);
            System.arraycopy(power, 0, history, delay - i.length, i.length);
        }

        return reliabilities;
    }

    /**
     * Steps the ambiguity search offset when a signal is present and the symbol processor hasn't synchronized within
     * the dwell period.  Holds the current search offset while there is no signal.
     * @param sampleCount in the completed block
     * @param processorSynchronized state of the symbol processor
     * @param signalPresent indicates if a signal is present
     */
    private void updateSearch(int sampleCount, boolean processorSynchronized, boolean signalPresent)
    {
        if(processorSynchronized)
        {
            mUnsynchronizedSignalSymbols = 0;
            return;
        }

        if(signalPresent)
        {
            mUnsynchronizedSignalSymbols += sampleCount / mSamplesPerSymbol;

            if(mUnsynchronizedSignalSymbols > (mSearchIndexVerified ? VERIFIED_DWELL_SYMBOLS : SEARCH_DWELL_SYMBOLS))
            {
                mUnsynchronizedSignalSymbols = 0;
                mSearchIndexVerified = false;
                mSearchIndex = (mSearchIndex + 1) % SEARCH_STEPS.length;

                //Leaving the estimator's range: step first toward the adjacent range nearest to the estimate
                if(mSearchIndex == 1)
                {
                    mSearchDirection = mEstimatorOffset > 0.0 ? -1 : 1;
                }
            }
        }
    }
}
