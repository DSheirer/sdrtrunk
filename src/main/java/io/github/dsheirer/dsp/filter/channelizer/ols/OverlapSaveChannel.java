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
package io.github.dsheirer.dsp.filter.channelizer.ols;

import io.github.dsheirer.dsp.fft.MixedRadixComplexFFT;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * One channel of the overlap-save channelizer.  For each block it copies the output bandwidth's worth of bins around
 * the channel's nearest bin, multiplies them by the channel filter's frequency response, and inverse transforms them at
 * the output size, which filters, translates to baseband and decimates in one step.  It then discards the samples
 * that the circular convolution wrapped (the overlap), removes the phase step that a bin shift that is not a multiple
 * of the overlap factor leaves between blocks (the block phase correction of Renfors, Yli-Kaakinen and harris,
 * equation (12), which lets the bin shift be any bin, not only multiples of the overlap factor), and mixes out the
 * residual offset between the channel frequency and the bin center plus any frequency correction.
 *
 * The work is split in two so that the channelizer's thread only copies bins: gather() copies the block's bins for
 * the current tuning, and process() (normally on the channel's own thread) does the rest.  Process the gathered blocks
 * in order from a single thread.  The tuning can change between blocks: the block phase is accumulated, so the output
 * phase stays continuous across a change of the center bin, and the residual mixer phase is continuous across a change
 * of offset or correction.
 *
 * The filter is a set of FIR taps at the output rate.  Its length must not exceed the overlap at the output rate plus
 * one, and its stop band should reach the output Nyquist edge, since bins outside the output bandwidth are dropped.
 *
 * See the package documentation (package-info.java) for the theory of operation and references.
 */
public class OverlapSaveChannel implements OverlapSaveChannelizer.BlockListener
{
    private final int mFFTSize;
    private final int mHop;
    private final int mOutputSize;
    private final int mDiscard;
    private final int mOutputLength;
    private final double mGroupDelay;
    private final double mBinWidth;
    private final double mOutputSampleRate;
    private final float[] mMaskI;
    private final float[] mMaskQ;
    private final float[] mBuffer;
    private final float[] mInlineBins;
    private final MixedRadixComplexFFT mInverseFFT;
    private volatile Tuning mTuning;
    private volatile double mFrequencyCorrection;
    private long mBlockPhaseIndex;
    private long mLastBlock = Long.MIN_VALUE;
    private double mResidualPhase;
    private Listener<ComplexSamples> mListener;

    /**
     * Constructs an instance.
     * @param channelizer providing the block parameters
     * @param frequencyOffset of the channel center from the tuner center in Hertz
     * @param outputSize number of bins, which sets the output sample rate to binWidth x outputSize and must divide the
     * FFT size into a decimation that also divides the hop and the overlap
     * @param taps of the channel filter designed at the output sample rate
     * @param gain applied to the output
     */
    public OverlapSaveChannel(OverlapSaveChannelizer channelizer, double frequencyOffset, int outputSize, float[] taps,
                              float gain)
    {
        mFFTSize = channelizer.getFFTSize();
        mHop = channelizer.getHop();
        mOutputSize = outputSize;

        if(!isValidOutputSize(channelizer, outputSize))
        {
            throw new IllegalArgumentException("Output size [" + outputSize + "] must divide FFT size [" + mFFTSize +
                "] into a decimation that divides the hop [" + mHop + "] and the overlap [" + (mFFTSize - mHop) + "]");
        }

        int decimation = mFFTSize / outputSize;
        mDiscard = (mFFTSize - mHop) / decimation;
        mOutputLength = mHop / decimation;

        if(taps.length - 1 > mDiscard)
        {
            throw new IllegalArgumentException("Filter length [" + taps.length + "] exceeds the overlap at the " +
                "output rate [" + mDiscard + "] plus one");
        }

        mGroupDelay = (taps.length - 1) / 2.0;
        mBinWidth = channelizer.getBinWidth();
        mOutputSampleRate = mBinWidth * outputSize;

        //Frequency response of the taps at the output size, scaled for the unscaled forward and inverse FFTs
        float[] response = new float[outputSize * 2];

        for(int x = 0; x < taps.length; x++)
        {
            response[2 * x] = taps[x];
        }

        new MixedRadixComplexFFT(outputSize).forward(response);
        mMaskI = new float[outputSize];
        mMaskQ = new float[outputSize];
        float scale = gain / mFFTSize;

        for(int m = 0; m < outputSize; m++)
        {
            mMaskI[m] = response[2 * m] * scale;
            mMaskQ[m] = response[2 * m + 1] * scale;
        }

        mBuffer = new float[outputSize * 2];
        mInlineBins = new float[outputSize * 2];
        mInverseFFT = new MixedRadixComplexFFT(outputSize);
        setFrequencyOffset(frequencyOffset);
    }

    /**
     * Indicates if a channel of the output size can be sliced from the channelizer's blocks.
     */
    public static boolean isValidOutputSize(OverlapSaveChannelizer channelizer, int outputSize)
    {
        int size = channelizer.getFFTSize();
        int hop = channelizer.getHop();

        if(outputSize <= 0 || outputSize > size || size % outputSize != 0)
        {
            return false;
        }

        int decimation = size / outputSize;
        return hop % decimation == 0 && (size - hop) % decimation == 0;
    }

    /**
     * Smallest valid output size whose sample rate is at least the minimum sample rate.
     * @param channelizer providing the block parameters
     * @param minimumSampleRate of the channel
     * @return output size, in bins
     * @throws IllegalArgumentException if the channelizer can't provide the sample rate
     */
    public static int getOutputSize(OverlapSaveChannelizer channelizer, double minimumSampleRate)
    {
        int size = Math.max(1, (int)Math.ceil(minimumSampleRate / channelizer.getBinWidth() - 1E-9));

        while(size <= channelizer.getFFTSize())
        {
            if(isValidOutputSize(channelizer, size))
            {
                return size;
            }

            size++;
        }

        throw new IllegalArgumentException("Channelizer can't provide a sample rate of [" + minimumSampleRate + "]");
    }

    /**
     * Longest filter that a channel of the output size can use with the channelizer's overlap.
     */
    public static int getMaximumFilterLength(OverlapSaveChannelizer channelizer, int outputSize)
    {
        int decimation = channelizer.getFFTSize() / outputSize;
        return (channelizer.getFFTSize() - channelizer.getHop()) / decimation + 1;
    }

    /**
     * Designs a unity gain Kaiser windowed sinc low-pass filter at the output rate, cut off midway between the pass
     * and stop frequencies.  The length meets the attenuation over the transition band when the maximum length allows,
     * otherwise it is the maximum odd length and the transition band is wider.
     * @param sampleRate output sample rate
     * @param pass frequency in Hertz
     * @param stop frequency in Hertz
     * @param attenuation in dB
     * @param maximumLength of the filter
     * @return taps
     */
    public static float[] designFilter(double sampleRate, double pass, double stop, float attenuation,
                                       int maximumLength)
    {
        double transition = 2.0 * Math.PI * (stop - pass) / sampleRate;
        int length = (int)Math.ceil((attenuation - 7.95) / (2.285 * transition)) + 1;
        length = Math.min(length, maximumLength);

        if(length % 2 == 0)
        {
            length--;
        }

        try
        {
            float[] taps = FilterFactory.getKaiserSinc(length, (pass + stop) / 2.0 / sampleRate, attenuation);
            double sum = 0.0;

            for(float tap : taps)
            {
                sum += tap;
            }

            for(int x = 0; x < taps.length; x++)
            {
                taps[x] /= (float)sum;
            }

            return taps;
        }
        catch(FilterDesignException fde)
        {
            throw new IllegalArgumentException("Unable to design filter", fde);
        }
    }

    /**
     * Convolves two filters, for a cascade of filters as one set of taps.
     * @param a taps
     * @param b taps
     * @return taps of length a.length + b.length - 1
     */
    public static float[] convolve(float[] a, float[] b)
    {
        float[] out = new float[a.length + b.length - 1];

        for(int x = 0; x < a.length; x++)
        {
            for(int y = 0; y < b.length; y++)
            {
                out[x + y] += a[x] * b[y];
            }
        }

        return out;
    }

    public void setListener(Listener<ComplexSamples> listener)
    {
        mListener = listener;
    }

    public double getOutputSampleRate()
    {
        return mOutputSampleRate;
    }

    /**
     * Output samples per block.
     */
    public int getOutputLength()
    {
        return mOutputLength;
    }

    /**
     * Number of output bins, which is the length in complex samples of a gathered block.
     */
    public int getOutputSize()
    {
        return mOutputSize;
    }

    public int getCenterBin()
    {
        return mTuning.mCenterBin;
    }

    /**
     * Channel frequency offset from the bin center, in Hertz, before frequency correction.
     */
    public double getResidualFrequency()
    {
        return mTuning.mResidualFrequency;
    }

    /**
     * Sets the channel's offset from the tuner center.  It applies to the next block gathered.
     * @param frequencyOffset of the channel center from the tuner center in Hertz
     */
    public void setFrequencyOffset(double frequencyOffset)
    {
        mTuning = new Tuning(frequencyOffset);
    }

    /**
     * Sets a frequency correction that is added to the residual mixer, the same as the polyphase channel's mixer
     * offset: a positive correction moves the output spectrum up.  It applies to the next block processed.
     * @param correction in Hertz
     */
    public void setFrequencyCorrection(double correction)
    {
        mFrequencyCorrection = correction;
    }

    /**
     * Copies the block's bins for the current tuning.
     * @param spectrumI inphase bins of the forward FFT
     * @param spectrumQ quadrature bins
     * @param bins to receive the interleaved bins, getOutputSize() complex values in ascending frequency order
     * @return tuning that the bins were gathered with, for process()
     */
    public Tuning gather(float[] spectrumI, float[] spectrumQ, float[] bins)
    {
        Tuning tuning = mTuning;
        int start = tuning.mFirstBin;
        int first = Math.min(mOutputSize, mFFTSize - start);
        int pointer = 0;

        //The bins wrap at most once since the output size is at most the FFT size
        for(int bin = start; bin < start + first; bin++)
        {
            bins[pointer++] = spectrumI[bin];
            bins[pointer++] = spectrumQ[bin];
        }

        for(int bin = 0; bin < mOutputSize - first; bin++)
        {
            bins[pointer++] = spectrumI[bin];
            bins[pointer++] = spectrumQ[bin];
        }

        return tuning;
    }

    /**
     * Filters, inverse transforms and phase corrects one block of gathered bins.
     * @param bins from gather().  Not modified.
     * @param tuning returned by gather()
     * @param block index of this block since the channelizer started
     * @param timestamp of the block's first output sample
     * @return output samples
     */
    public ComplexSamples process(float[] bins, Tuning tuning, long block, long timestamp)
    {
        final float[] buffer = mBuffer;
        final float[] maskI = mMaskI;
        final float[] maskQ = mMaskQ;
        final int size = mOutputSize;
        final int half = size / 2;

        //Gathered bin p is output bin p - half, which the inverse FFT holds at index (p - half) mod size
        for(int p = 0; p < size; p++)
        {
            int m = p < half ? p + size - half : p - half;
            float si = bins[2 * p];
            float sq = bins[2 * p + 1];
            float mi = maskI[m];
            float mq = maskQ[m];
            buffer[2 * m] = si * mi - sq * mq;
            buffer[2 * m + 1] = si * mq + sq * mi;
        }

        mInverseFFT.inverse(buffer, false);

        //Block phase correction (Renfors, Yli-Kaakinen and harris, equation (12); see package-info): it removes the
        //phase step between blocks that a center bin that isn't a multiple of the overlap factor leaves, so the center
        //bin isn't limited to multiples of the overlap factor.  Advance the accumulated block phase by this block's
        //step for each block since the last one processed, so that a change of center bin or a dropped block doesn't
        //leave a phase step.
        long elapsed = mLastBlock == Long.MIN_VALUE ? 0 : block - mLastBlock;
        mLastBlock = block;
        mBlockPhaseIndex = Math.floorMod(mBlockPhaseIndex + tuning.mBlockPhaseStep * Math.floorMod(elapsed, mFFTSize),
            mFFTSize);

        double residualStep = getResidualStep(tuning);

        if(elapsed > 1)
        {
            mResidualPhase = Math.IEEEremainder(mResidualPhase + residualStep * mOutputLength * (elapsed - 1),
                2.0 * Math.PI);
        }

        //The residual mixer follows the channel filter, so reference its phase to the filter's input by backing it off
        //by the filter's group delay.  Otherwise a change of residual frequency or correction would leave a phase step
        //of the frequency change times the group delay.
        double phase = -2.0 * Math.PI * mBlockPhaseIndex / mFFTSize + mResidualPhase - residualStep * mGroupDelay;
        double pi = Math.cos(phase);
        double pq = Math.sin(phase);
        double stepI = Math.cos(residualStep);
        double stepQ = Math.sin(residualStep);

        float[] i = new float[mOutputLength];
        float[] q = new float[mOutputLength];

        for(int x = 0; x < mOutputLength; x++)
        {
            int source = 2 * (mDiscard + x);
            float bi = buffer[source];
            float bq = buffer[source + 1];
            i[x] = (float)(bi * pi - bq * pq);
            q[x] = (float)(bi * pq + bq * pi);
            double next = pi * stepI - pq * stepQ;
            pq = pi * stepQ + pq * stepI;
            pi = next;
        }

        mResidualPhase = Math.IEEEremainder(mResidualPhase + residualStep * mOutputLength, 2.0 * Math.PI);
        return new ComplexSamples(i, q, timestamp);
    }

    /**
     * Residual mixer phase step per output sample, in radians.
     */
    private double getResidualStep(Tuning tuning)
    {
        return 2.0 * Math.PI * (mFrequencyCorrection - tuning.mResidualFrequency) / mOutputSampleRate;
    }

    /**
     * Inline processing on the channelizer's thread: gathers and processes the block and sends the output to the
     * listener.
     */
    @Override
    public void processBlock(float[] spectrumI, float[] spectrumQ, long block, long timestamp)
    {
        Tuning tuning = gather(spectrumI, spectrumQ, mInlineBins);
        ComplexSamples samples = process(mInlineBins, tuning, block, timestamp);

        if(mListener != null)
        {
            mListener.receive(samples);
        }
    }

    /**
     * Bin selection and block phase step for one channel offset.
     */
    public final class Tuning
    {
        private final int mCenterBin;
        private final int mFirstBin;
        private final double mResidualFrequency;
        private final long mBlockPhaseStep;

        private Tuning(double frequencyOffset)
        {
            mCenterBin = (int)Math.round(frequencyOffset / mBinWidth);
            mResidualFrequency = frequencyOffset - mCenterBin * mBinWidth;
            int center = Math.floorMod(mCenterBin, mFFTSize);
            mFirstBin = Math.floorMod(center - mOutputSize / 2, mFFTSize);
            //Block phase step k0 L mod N, in turns of 1 / N: zero when k0 is a multiple of the overlap factor, and
            //removed in process() otherwise (Renfors, Yli-Kaakinen and harris, equation (12))
            mBlockPhaseStep = (long)center * mHop % mFFTSize;
        }
    }
}
