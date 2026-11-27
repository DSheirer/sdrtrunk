/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.channel;

import java.util.function.DoubleFunction;

/**
 * Channel source specification
 */
public class ChannelSpecification
{
    private double mMinimumSampleRate;
    private int mBandwidth;
    private double mPassFrequency;
    private double mStopFrequency;
    private ChannelFilter mChannelFilter;

    /**
     * Constructs a channel specification instance.
     * @param minimumSampleRate requested for the channel
     * @param bandwidth
     * @param passFrequency for the pass band of the channel, usually half of the bandwidth value.
     * @param stopFrequency for the stop band of teh channel, usually slightly more than half of the bandwidth value.
     */
    public ChannelSpecification(double minimumSampleRate, int bandwidth, double passFrequency, double stopFrequency)
    {
        mMinimumSampleRate = minimumSampleRate;
        mBandwidth = bandwidth;
        mPassFrequency = passFrequency;
        mStopFrequency = stopFrequency;
    }

    /**
     * Constructs a channel specification instance with a decoder channel filter.
     * @param minimumSampleRate requested for the channel
     * @param bandwidth of the channel
     * @param passFrequency for the pass band of the channel
     * @param stopFrequency for the stop band of the channel
     * @param channelFilter that the decoder applies ahead of demodulation, which a source may apply instead
     */
    public ChannelSpecification(double minimumSampleRate, int bandwidth, double passFrequency, double stopFrequency,
                                ChannelFilter channelFilter)
    {
        this(minimumSampleRate, bandwidth, passFrequency, stopFrequency);
        mChannelFilter = channelFilter;
    }

    /**
     * Low-pass channel filter that the decoder applies to the complex samples ahead of demodulation, when the decoder
     * receives the channel at the filter's sample rate.  A tuner channel source that applies this filter itself (at
     * least as well) and delivers the channel at a sample rate that the decoder uses without decimation signals it
     * with SourceEvent.isChannelFiltered() so that the decoder can skip its own filter.
     * @return filter or null if the decoder doesn't support a pre-filtered channel
     */
    public ChannelFilter getChannelFilter()
    {
        return mChannelFilter;
    }

    /**
     * Indicates if this specification has a decoder channel filter.
     */
    public boolean hasChannelFilter()
    {
        return mChannelFilter != null;
    }

    /**
     * Decoder channel filter.
     * @param sampleRate a sample rate at which the decoder doesn't decimate, and at which a source applies the filter
     * @param passFrequency end of the pass band in hertz
     * @param stopFrequency start of the stop band in hertz
     * @param design of the decoder's own filter taps for a sample rate, exactly as the decoder designs them, so that a
     * source can reproduce the decoder's filter response (the decoder's demodulator, symbol recovery and squelch are
     * tuned to it) and may add further stop band attenuation.
     */
    public record ChannelFilter(double sampleRate, double passFrequency, double stopFrequency,
                                DoubleFunction<float[]> design)
    {
        /**
         * Decoder's own filter taps for the sample rate.
         */
        public float[] getTaps(double sampleRate)
        {
            return design.apply(sampleRate);
        }
    }

    /**
     * Minimum requested sample rate for the channel
     */
    public double getMinimumSampleRate()
    {
        return mMinimumSampleRate;
    }

    /**
     * Bandwidth of the channel in Hertz
     */
    public int getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Pass band frequency for filtering the channel
     */
    public double getPassFrequency()
    {
        return mPassFrequency;
    }

    /**
     * Stop band frequency for filtering the channel
     */
    public double getStopFrequency()
    {
        return mStopFrequency;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Channel Specification - min sample rate: ").append(mMinimumSampleRate);
        sb.append(" bandwidth: ").append(mBandwidth);
        sb.append(" pass: ").append(mPassFrequency);
        sb.append(" stop: ").append(mStopFrequency);

        if(mChannelFilter != null)
        {
            sb.append(" channel filter: ").append(mChannelFilter);
        }
        return sb.toString();
    }
}
