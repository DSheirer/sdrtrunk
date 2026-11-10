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
package io.github.dsheirer.dsp.filter.channelizer;

/**
 * Direct Discrete Fourier Transform of selected polyphase channels, as an alternative to an IFFT of all channels when
 * only a few channels are in use.  Produces the same channel values as a scaled IFFT.
 *
 * Results are written in place to each channel's position in each block.  All other positions in the block retain their
 * input values and must not be used.
 */
public abstract class ChannelDFT
{
    private final int mChannelCount;
    private final float[][] mChannelInphaseCoefficients;
    private final float[][] mChannelQuadratureCoefficients;
    private float[] mResults = new float[0];

    /**
     * Constructs an instance
     * @param channelCount as the size of the transform
     */
    protected ChannelDFT(int channelCount)
    {
        mChannelCount = channelCount;
        mChannelInphaseCoefficients = new float[channelCount][];
        mChannelQuadratureCoefficients = new float[channelCount][];
    }

    /**
     * Largest number of channels where this implementation is faster than an IFFT of all channels.
     */
    public abstract int getMaximumChannels();

    /**
     * Calculates the inphase and quadrature values for a channel.
     *
     * @param samples containing the block
     * @param offset to the start of the block
     * @param length of the block in array elements
     * @param inphaseCoefficients for the channel
     * @param quadratureCoefficients for the channel
     * @param results array to receive the inphase and quadrature values
     * @param resultsOffset for the inphase value, followed by the quadrature value
     */
    protected abstract void transform(float[] samples, int offset, int length, float[] inphaseCoefficients,
                                      float[] quadratureCoefficients, float[] results, int resultsOffset);

    /**
     * Transforms the specified channels in each block in the samples array.
     * @param samples containing one or more contiguous blocks of interleaved I/Q values for each channel
     * @param channels to transform
     */
    public void process(float[] samples, int[] channels)
    {
        int blockLength = mChannelCount * 2;
        float[][] inphaseCoefficients = new float[channels.length][];
        float[][] quadratureCoefficients = new float[channels.length][];

        for(int x = 0; x < channels.length; x++)
        {
            updateCoefficients(channels[x]);
            inphaseCoefficients[x] = mChannelInphaseCoefficients[channels[x]];
            quadratureCoefficients[x] = mChannelQuadratureCoefficients[channels[x]];
        }

        if(mResults.length < channels.length * 2)
        {
            mResults = new float[channels.length * 2];
        }

        float[] results = mResults;

        for(int offset = 0; offset < samples.length; offset += blockLength)
        {
            //Each channel needs all of the input values, so write the results after all channels are calculated
            for(int x = 0; x < channels.length; x++)
            {
                transform(samples, offset, blockLength, inphaseCoefficients[x], quadratureCoefficients[x], results, x * 2);
            }

            for(int x = 0; x < channels.length; x++)
            {
                samples[offset + channels[x] * 2] = results[x * 2];
                samples[offset + channels[x] * 2 + 1] = results[x * 2 + 1];
            }
        }
    }

    /**
     * Creates the coefficients for a channel on first use.  Coefficients are arranged to align with interleaved I/Q
     * samples so that each output value is a dot product of the block and a coefficient array.
     * @param channel to update
     */
    private void updateCoefficients(int channel)
    {
        if(mChannelInphaseCoefficients[channel] == null)
        {
            float[] inphase = new float[mChannelCount * 2];
            float[] quadrature = new float[mChannelCount * 2];

            for(int x = 0; x < mChannelCount; x++)
            {
                double angle = 2.0 * Math.PI * channel * x / mChannelCount;
                float cosine = (float)(Math.cos(angle) / mChannelCount);
                float sine = (float)(Math.sin(angle) / mChannelCount);
                inphase[2 * x] = cosine;
                inphase[2 * x + 1] = -sine;
                quadrature[2 * x] = sine;
                quadrature[2 * x + 1] = cosine;
            }

            mChannelInphaseCoefficients[channel] = inphase;
            mChannelQuadratureCoefficients[channel] = quadrature;
        }
    }
}
