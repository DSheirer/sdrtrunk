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

import io.github.dsheirer.dsp.filter.channelizer.ChannelCalculator;
import io.github.dsheirer.dsp.filter.channelizer.ChannelResultsBatch;
import io.github.dsheirer.dsp.filter.channelizer.ComplexPolyphaseChannelizerM2;
import io.github.dsheirer.dsp.filter.channelizer.SynthesisFilterManager;
import io.github.dsheirer.dsp.filter.channelizer.output.ChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.OneChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.TwoChannelOutputProcessor;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Single-threaded drivers for the two channelizer paths, built from the production classes, used by the quality
 * tests and the benchmarks.  Every stage normally on another thread (the polyphase IFFT processor and each channel's
 * output processor) runs inline on the calling thread, so the CPU cost of a call is the cost of the whole path.
 */
public class ChannelizerPaths
{
    public static final int CHANNEL_BANDWIDTH = 12500;

    /**
     * A channelizer path driven with interleaved complex samples.
     */
    public interface Path
    {
        void receive(float[] interleaved, long timestamp);
    }

    /**
     * Stock polyphase path: ComplexPolyphaseChannelizerM2 (filter and IFFT/DFT), the copy of each channel out of the
     * batch, then OneChannelOutputProcessor or TwoChannelOutputProcessor (synthesis, FS/4 and frequency mixing, gain
     * and 2048 sample assembly), as PolyphaseChannelManager and PolyphaseChannelSource set them up.  Output is the
     * decoder input at the 50 kHz channel rate.
     */
    public static class Polyphase implements Path
    {
        private final ComplexPolyphaseChannelizerM2 mChannelizer;
        private final List<Consumer> mConsumers = new ArrayList<>();
        private final int[] mIndexes;
        private int mSynthesized;
        private boolean mDispatch = true;

        /**
         * @param sampleRate of the tuner
         * @param centerFrequency of the tuner
         * @param frequencies of the channels
         * @param listeners one per channel
         */
        public Polyphase(double sampleRate, long centerFrequency, long[] frequencies,
                         List<Listener<ComplexSamples>> listeners) throws Exception
        {
            int channelCount = ComplexPolyphaseChannelizerM2.getChannelCount(sampleRate);
            ChannelCalculator calculator = new ChannelCalculator(sampleRate, channelCount, centerFrequency, 2.0);
            SynthesisFilterManager filterManager = new SynthesisFilterManager();
            BitSet used = new BitSet();
            Method applyPendingUpdates = ChannelOutputProcessor.class.getDeclaredMethod("applyPendingUpdates", long.class);
            applyPendingUpdates.setAccessible(true);

            for(int x = 0; x < frequencies.length; x++)
            {
                List<Integer> indexes = calculator.getChannelIndexes(new TunerChannel(frequencies[x], CHANNEL_BANDWIDTH));
                long offset = calculator.getCenterFrequencyForIndexes(indexes) - frequencies[x];
                ChannelOutputProcessor processor;

                if(indexes.size() == 1)
                {
                    processor = new OneChannelOutputProcessor(calculator.getChannelSampleRate(), indexes,
                        calculator.getChannelCount(), null, "bench");
                }
                else
                {
                    float[] filter = filterManager.getFilter(calculator.getChannelSampleRate(),
                        calculator.getChannelBandwidth(), 2);
                    processor = new TwoChannelOutputProcessor(calculator.getChannelSampleRate(), indexes, filter,
                        calculator.getChannelCount(), null, "bench");
                    mSynthesized++;
                }

                processor.setFrequencyOffset(offset);
                applyPendingUpdates.invoke(processor, Long.MAX_VALUE);
                processor.setListener(listeners.get(x));
                int[] offsets = new int[indexes.size()];

                for(int y = 0; y < offsets.length; y++)
                {
                    offsets[y] = indexes.get(y) * 2;
                    used.set(indexes.get(y));
                }

                mConsumers.add(new Consumer(processor, offsets));
            }

            mIndexes = used.stream().toArray();

            mChannelizer = new ComplexPolyphaseChannelizerM2(sampleRate, 9)
            {
                @Override
                protected int[] prepareChannels()
                {
                    return mIndexes;
                }

                @Override
                protected void dispatch(ChannelResultsBatch batch)
                {
                    if(!mDispatch)
                    {
                        return;
                    }

                    for(Consumer consumer : mConsumers)
                    {
                        consumer.receive(batch);
                    }
                }
            };

            //Run the IFFT processor's listener inline instead of queueing the batch for its thread
            Field dispatcherField = ComplexPolyphaseChannelizerM2.class.getDeclaredField("mIFFTProcessorDispatcher");
            dispatcherField.setAccessible(true);
            dispatcherField.set(mChannelizer, new InlineIFFT(mChannelizer));
        }

        /**
         * Turns delivery to the channel output processors on or off, to measure the channelizer front end alone (the
         * channels still select which polyphase channels are transformed).
         */
        public void setDispatch(boolean dispatch)
        {
            mDispatch = dispatch;
        }

        /**
         * Number of channels that need two-channel synthesis.
         */
        public int getSynthesizedCount()
        {
            return mSynthesized;
        }

        @Override
        public void receive(float[] interleaved, long timestamp)
        {
            mChannelizer.receive(new InterleavedComplexSamples(interleaved, timestamp));
        }

        private static class InlineIFFT extends ComplexPolyphaseChannelizerM2.IFFTProcessorDispatcher
        {
            private Listener<ChannelResultsBatch> mInline;

            InlineIFFT(ComplexPolyphaseChannelizerM2 channelizer)
            {
                channelizer.super(25);
            }

            @Override
            public void setListener(Listener<ChannelResultsBatch> listener)
            {
                mInline = listener;
                super.setListener(listener);
            }

            @Override
            public boolean offer(ChannelResultsBatch batch)
            {
                mInline.receive(batch);
                return true;
            }
        }

        /**
         * Copies a channel's samples out of the batch exactly as ChannelOutputProcessor.receiveChannelResults() does,
         * then runs the output processor.
         */
        private static class Consumer
        {
            private final ChannelOutputProcessor mProcessor;
            private final int[] mOffsets;
            private float[] mSamples = new float[0];

            Consumer(ChannelOutputProcessor processor, int[] offsets)
            {
                mProcessor = processor;
                mOffsets = offsets;
            }

            void receive(ChannelResultsBatch batch)
            {
                float[] source = batch.getSamples();
                int blockLength = batch.getBlockLength();
                int blockCount = batch.getBlockCount();
                int length = blockCount * mOffsets.length * 2;

                if(mSamples.length != length)
                {
                    mSamples = new float[length];
                }

                float[] samples = mSamples;
                int pointer = 0;

                for(int block = 0; block < blockCount; block++)
                {
                    int blockOffset = block * blockLength;

                    for(int offset : mOffsets)
                    {
                        samples[pointer++] = source[blockOffset + offset];
                        samples[pointer++] = source[blockOffset + offset + 1];
                    }
                }

                mProcessor.process(samples, blockCount);
            }
        }
    }

    /**
     * Output of an overlap-save channel.
     */
    public enum Output
    {
        /** 50 kHz, a wide channel filter, for the decoder's own decimation and baseband filter */
        DECODER_INPUT,
        /** 25 kHz, filtered to the decoder's channel specification, replacing the decoder's decimation and filter */
        DECODER_READY
    }

    /**
     * Overlap-save prototype path.
     */
    public static class OverlapSave implements Path
    {
        public static final float ATTENUATION = 90.0f;
        private final OverlapSaveChannelizer mChannelizer;
        private final List<OverlapSaveChannel> mChannels = new ArrayList<>();

        /**
         * @param channelizer to use
         * @param centerFrequency of the tuner
         * @param frequencies of the channels
         * @param output rate and filtering
         * @param spec channel specification for DECODER_READY output
         * @param listeners one per channel
         */
        public OverlapSave(OverlapSaveChannelizer channelizer, long centerFrequency, long[] frequencies, Output output,
                           DecoderSpec spec, List<Listener<ComplexSamples>> listeners)
        {
            mChannelizer = channelizer;
            double rate = output == Output.DECODER_INPUT ? DecoderSpec.DECODER_INPUT_RATE : DecoderSpec.DECODER_RATE;
            int outputSize = (int)Math.round(rate / channelizer.getBinWidth());
            int maximum = OverlapSaveChannel.getMaximumFilterLength(channelizer, outputSize);
            float[] taps = output == Output.DECODER_INPUT ?
                OverlapSaveChannel.designFilter(rate, 7000, 12500, ATTENUATION, maximum) :
                OverlapSaveChannel.designFilter(rate, spec.getPass(), spec.getStop(), ATTENUATION, maximum);

            for(int x = 0; x < frequencies.length; x++)
            {
                OverlapSaveChannel channel = new OverlapSaveChannel(channelizer, frequencies[x] - centerFrequency,
                    outputSize, taps, 1.0f);
                channel.setListener(listeners.get(x));
                channelizer.addChannel(channel);
                mChannels.add(channel);
            }
        }

        public OverlapSaveChannelizer getChannelizer()
        {
            return mChannelizer;
        }

        public List<OverlapSaveChannel> getChannels()
        {
            return mChannels;
        }

        @Override
        public void receive(float[] interleaved, long timestamp)
        {
            mChannelizer.receive(interleaved, timestamp);
        }
    }
}
