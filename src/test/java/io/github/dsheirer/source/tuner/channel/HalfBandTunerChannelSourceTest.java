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

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.SignedByteNativeBuffer;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the heterodyne channel source applies frequency updates on the sample processing thread.
 */
public class HalfBandTunerChannelSourceTest
{
    private static final long CHANNEL_FREQUENCY = 460_000_000L;
    private static final long TUNER_FREQUENCY = 460_400_000L;
    private static final double SAMPLE_RATE = 2_400_000.0;

    private static HalfBandTunerChannelSource<INativeBuffer> source() throws Exception
    {
        HalfBandTunerChannelSource<INativeBuffer> source = new HalfBandTunerChannelSource<>(sourceEvent -> {},
            new TunerChannel(CHANNEL_FREQUENCY, 12500), SAMPLE_RATE,
            new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0), "test", null);
        source.setListener(samples -> {});
        return source;
    }

    private static void process(HalfBandTunerChannelSource<INativeBuffer> source)
    {
        source.new NativeBufferProcessor().receive(new FloatNativeBuffer(new float[8192], 0L, 0.0f));
    }

    /**
     * Test: the mixer only changes on the sample processing thread, using both the tuner frequency and the correction.
     */
    @Test
    void frequencyUpdatesAreAppliedDuringSampleProcessing() throws Exception
    {
        HalfBandTunerChannelSource<INativeBuffer> source = source();

        source.setFrequency(TUNER_FREQUENCY);
        source.setFrequencyCorrection(100);
        assertEquals(0, source.getMixerFrequency());

        process(source);
        assertEquals(TUNER_FREQUENCY - CHANNEL_FREQUENCY + 100, source.getMixerFrequency());
    }

    /**
     * Test: a sample rate that is too low to decimate passes through at the original sample rate.
     */
    @Test
    void lowSampleRateIsNotDecimated() throws Exception
    {
        HalfBandTunerChannelSource<INativeBuffer> source = new HalfBandTunerChannelSource<>(sourceEvent -> {},
            new TunerChannel(CHANNEL_FREQUENCY, 12500), 50000.0,
            new ChannelSpecification(50000.0, 12500, 6500.0, 7200.0), "test", null);

        assertEquals(50000.0, source.getSampleRate());
    }

    private static HalfBandTunerChannelSource<INativeBuffer> source(double sampleRate) throws Exception
    {
        return new HalfBandTunerChannelSource<>(sourceEvent -> {}, new TunerChannel(CHANNEL_FREQUENCY, 12500), sampleRate,
            new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0), "test", null);
    }

    /**
     * Test: a decoder can apply more decimation at the highest source decimation rates, where a 2048 sample fragment
     * produces 2 samples or less, since these are assembled into larger buffers for the decoder.
     */
    @Test
    void highDecimationProvidesBuffersThatDecodersCanDecimate() throws Exception
    {
        //Tuner sample rates that select a decimation of 1024, 2048 and 4096
        for(int[] rates : new int[][]{{30_000_000, 1024}, {60_000_000, 2048}, {120_000_000, 4096}})
        {
            for(int decoderDecimation : new int[]{2, 4, 8, 16, 32, 64})
            {
                HalfBandTunerChannelSource<INativeBuffer> source = source(rates[0]);
                IRealDecimationFilter filter = DecimationFilterFactory.getRealDecimationFilter(decoderDecimation);
                List<Throwable> errors = new ArrayList<>();
                AtomicInteger sampleCount = new AtomicInteger();

                source.setListener(samples ->
                {
                    try
                    {
                        sampleCount.addAndGet(samples.i().length);
                        filter.decimateReal(samples.i());
                    }
                    catch(Throwable t)
                    {
                        errors.add(t);
                    }
                });

                //Each native buffer has 65536 samples in 2048 sample fragments
                int nativeBuffers = 64 * rates[1] / 65536;

                for(int x = 0; x < nativeBuffers; x++)
                {
                    source.new NativeBufferProcessor().receive(new SignedByteNativeBuffer(new byte[131072], 0L, 0, 0, 1));
                }

                assertEquals(List.of(), errors);
                assertEquals((double)rates[0] / rates[1], source.getSampleRate());
                assertEquals(64 * (nativeBuffers * 65536 / rates[1] / 64), sampleCount.get());
            }
        }
    }

    /**
     * Test: assembled buffers preserve the timestamp of the first sample in each buffer.
     */
    @Test
    void assembledBuffersPreserveTimestamps() throws Exception
    {
        HalfBandTunerChannelSource<INativeBuffer> source = source(120_000_000.0);
        List<ComplexSamples> buffers = new ArrayList<>();
        source.setListener(buffers::add);

        //Each native buffer has 32768 samples that produce 8 decimated samples, so 12 native buffers provide 96 samples
        for(long timestamp = 1000; timestamp <= 12000; timestamp += 1000)
        {
            source.new NativeBufferProcessor().receive(new FloatNativeBuffer(new float[65536], timestamp, 0.0f));
        }

        assertEquals(1, buffers.size());
        assertEquals(64, buffers.getFirst().i().length);
        assertEquals(64, buffers.getFirst().q().length);
        assertEquals(1000L, buffers.getFirst().timestamp());
    }

    /**
     * Test: assembling sample fragments doesn't change the samples, so the output matches decimating complete blocks,
     * including when sample buffers end with a short fragment.
     */
    @Test
    void assembledInputMatchesContinuousDecimation() throws Exception
    {
        int decimation = 4096;
        //Native buffers provide a 2048 sample fragment and a 1024 sample fragment, which straddle the filter blocks
        int fragment = 3072;
        int fragments = 800;
        float[] interleaved = new float[fragment * fragments * 2];

        for(int x = 0; x < fragment * fragments; x++)
        {
            double phase = 2.0 * Math.PI * 3000.0 * x / 120_000_000.0;
            interleaved[x * 2] = (float)Math.cos(phase);
            interleaved[x * 2 + 1] = (float)Math.sin(phase);
        }

        HalfBandTunerChannelSource<INativeBuffer> source = source(120_000_000.0);
        List<ComplexSamples> buffers = new ArrayList<>();
        source.setListener(buffers::add);

        for(int x = 0; x < fragments; x++)
        {
            float[] samples = Arrays.copyOfRange(interleaved, x * fragment * 2, (x + 1) * fragment * 2);
            source.new NativeBufferProcessor().receive(new FloatNativeBuffer(samples, 0L, 0.0f));
        }

        IRealDecimationFilter expectedI = DecimationFilterFactory.getRealDecimationFilter(decimation);
        float[] blockI = new float[decimation];
        List<Float> expected = new ArrayList<>();

        for(int x = 0; x + decimation <= fragment * fragments; x += decimation)
        {
            for(int y = 0; y < decimation; y++)
            {
                blockI[y] = interleaved[(x + y) * 2];
            }

            expected.add(expectedI.decimateReal(blockI)[0]);
        }

        int pointer = 0;

        for(ComplexSamples buffer : buffers)
        {
            for(float sample : buffer.i())
            {
                assertEquals(expected.get(pointer++), sample, 1e-4f);
            }
        }

        assertEquals(expected.size() / 64 * 64, pointer);
        assertTrue(pointer > 0);
    }

    /**
     * Test: concurrent tuner frequency and channel correction updates are both reflected in the mixer.
     */
    @Test
    void concurrentUpdatesAreNotLost() throws Exception
    {
        for(int trial = 0; trial < 1000; trial++)
        {
            HalfBandTunerChannelSource<INativeBuffer> source = source();
            Thread tuner = new Thread(() -> source.setFrequency(TUNER_FREQUENCY));
            Thread correction = new Thread(() -> source.setFrequencyCorrection(100));
            tuner.start();
            correction.start();
            tuner.join();
            correction.join();

            process(source);
            assertEquals(TUNER_FREQUENCY - CHANNEL_FREQUENCY + 100, source.getMixerFrequency());
        }
    }
}
