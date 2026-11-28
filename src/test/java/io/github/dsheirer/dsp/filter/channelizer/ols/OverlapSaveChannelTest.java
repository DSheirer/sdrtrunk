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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog.Bandwidth;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DecoderLSM;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Overlap-save channelizer and channel: tones, a frequency sweep, block-edge error, retune phase continuity,
 * frequency correction, sample rate support, channel filter selection and the threaded channel source.
 */
public class OverlapSaveChannelTest
{
    private static final double SAMPLE_RATE = 2400000.0;
    private static final ChannelSpecification.ChannelFilter LSM_FILTER = P25P1DecoderLSM.getChannelFilter();

    /**
     * Phase-continuous complex tone generator, with a tuner local oscillator offset that can change at a sample.
     */
    private static class ToneSource
    {
        private final double mSampleRate;
        private final double[] mFrequencies;
        private final double[] mAmplitudes;
        private final double[] mPhases;
        private double mLocalOscillator;
        private double mLocalOscillatorPhase;
        private long mSample;

        ToneSource(double sampleRate, double[] frequencies, double[] amplitudes)
        {
            mSampleRate = sampleRate;
            mFrequencies = frequencies;
            mAmplitudes = amplitudes;
            mPhases = new double[frequencies.length];
        }

        /**
         * Moves the tuner local oscillator up by the offset, which moves every tone down by the offset.
         */
        void setLocalOscillatorOffset(double offset)
        {
            mLocalOscillator = offset;
        }

        long getSample()
        {
            return mSample;
        }

        float[] next(int length)
        {
            float[] samples = new float[length * 2];

            for(int x = 0; x < length; x++)
            {
                double i = 0, q = 0;

                for(int t = 0; t < mFrequencies.length; t++)
                {
                    double phase = mPhases[t] - mLocalOscillatorPhase;
                    i += mAmplitudes[t] * Math.cos(phase);
                    q += mAmplitudes[t] * Math.sin(phase);
                    mPhases[t] = Math.IEEEremainder(mPhases[t] + 2.0 * Math.PI * mFrequencies[t] / mSampleRate,
                        2.0 * Math.PI);
                }

                mLocalOscillatorPhase = Math.IEEEremainder(mLocalOscillatorPhase + 2.0 * Math.PI * mLocalOscillator /
                    mSampleRate, 2.0 * Math.PI);
                samples[2 * x] = (float)i;
                samples[2 * x + 1] = (float)q;
                mSample++;
            }

            return samples;
        }
    }

    /**
     * Collects the output of each block.
     */
    private static class Collector implements Listener<ComplexSamples>
    {
        final List<ComplexSamples> mBlocks = new ArrayList<>();

        @Override
        public void receive(ComplexSamples samples)
        {
            mBlocks.add(samples);
        }

        double[][] samples(int firstBlock)
        {
            int length = 0;

            for(int b = firstBlock; b < mBlocks.size(); b++)
            {
                length += mBlocks.get(b).i().length;
            }

            double[][] out = new double[2][length];
            int pointer = 0;

            for(int b = firstBlock; b < mBlocks.size(); b++)
            {
                ComplexSamples block = mBlocks.get(b);

                for(int x = 0; x < block.i().length; x++)
                {
                    out[0][pointer] = block.i()[x];
                    out[1][pointer++] = block.q()[x];
                }
            }

            return out;
        }
    }

    private static OverlapSaveChannel channel(OverlapSaveChannelizer channelizer, double offset, Collector collector)
    {
        int size = OverlapSaveChannel.getOutputSize(channelizer, LSM_FILTER.sampleRate());
        double rate = size * channelizer.getBinWidth();
        float[] taps = OverlapSaveChannel.designFilter(rate, LSM_FILTER.passFrequency(), LSM_FILTER.stopFrequency(),
            90.0f, OverlapSaveChannel.getMaximumFilterLength(channelizer, size));
        OverlapSaveChannel channel = new OverlapSaveChannel(channelizer, offset, size, taps, 1.0f);
        channel.setListener(collector);
        channelizer.addChannel(channel);
        return channel;
    }

    private static void drive(OverlapSaveChannelizer channelizer, ToneSource source, int blocks)
    {
        long end = (long)blocks * channelizer.getHop();

        while(source.getSample() < end)
        {
            int length = (int)Math.min(16384, end - source.getSample());
            channelizer.receive(source.next(length), 0);
        }
    }

    /**
     * Least squares complex amplitude of a tone at the frequency over the samples.
     */
    private static double[] fit(double[][] samples, double frequency, double sampleRate)
    {
        double si = 0, sq = 0;
        int n = samples[0].length;

        for(int x = 0; x < n; x++)
        {
            double phase = -2.0 * Math.PI * frequency * x / sampleRate;
            double ci = Math.cos(phase), cq = Math.sin(phase);
            si += samples[0][x] * ci - samples[1][x] * cq;
            sq += samples[0][x] * cq + samples[1][x] * ci;
        }

        return new double[]{si / n, sq / n};
    }

    /**
     * Error power of each sample against the tone, relative to the tone power.
     */
    private static double[] errors(double[][] samples, double frequency, double sampleRate, double[] amplitude)
    {
        double power = amplitude[0] * amplitude[0] + amplitude[1] * amplitude[1];
        double[] errors = new double[samples[0].length];

        for(int x = 0; x < errors.length; x++)
        {
            double phase = 2.0 * Math.PI * frequency * x / sampleRate;
            double ei = amplitude[0] * Math.cos(phase) - amplitude[1] * Math.sin(phase);
            double eq = amplitude[0] * Math.sin(phase) + amplitude[1] * Math.cos(phase);
            double di = samples[0][x] - ei, dq = samples[1][x] - eq;
            errors[x] = (di * di + dq * dq) / power;
        }

        return errors;
    }

    private static double db(double ratio)
    {
        return 10.0 * Math.log10(ratio);
    }

    private static double mean(double[] values, int from, int to)
    {
        double sum = 0;

        for(int x = from; x < to; x++)
        {
            sum += values[x];
        }

        return sum / (to - from);
    }

    @Test
    void sampleRateSupport()
    {
        assertTrue(OverlapSaveChannelizer.isSupported(10000000.0));
        assertTrue(OverlapSaveChannelizer.isSupported(2400000.0));
        assertTrue(OverlapSaveChannelizer.isSupported(2048000.0));
        assertTrue(OverlapSaveChannelizer.isSupported(2500000.0));
        assertFalse(OverlapSaveChannelizer.isSupported(2800000.0), "70,000 bins has a factor of 7");
        assertFalse(OverlapSaveChannelizer.isSupported(8064000.0), "201,600 bins has a factor of 7");
        assertFalse(OverlapSaveChannelizer.isSupported(2400010.0), "not a whole number of 40 Hz bins");

        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(2048000.0);
        assertEquals(640, OverlapSaveChannel.getOutputSize(channelizer, 25000.0), "25.6 kHz at 2.048 MS/s");
        assertEquals(625, OverlapSaveChannel.getOutputSize(OverlapSaveChannelizer.create(SAMPLE_RATE), 25000.0));
        assertEquals(1250, OverlapSaveChannel.getOutputSize(OverlapSaveChannelizer.create(SAMPLE_RATE), 50000.0));
    }

    /**
     * Tones inside the channel at several channel offsets (on a bin, between bins, bin shifts that are and aren't a
     * multiple of the overlap factor) come out at the right frequency and amplitude, with no error at block edges.
     */
    @Test
    void toneAndBlockEdgeError()
    {
        double[] offsets = {0.0, 40.0 * 5, 40.0 * 7, 12345.0, -654321.5, 1000000.0 + 17.0};
        double tone = 1234.5;

        for(double offset : offsets)
        {
            OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
            Collector collector = new Collector();
            OverlapSaveChannel channel = channel(channelizer, offset, collector);
            ToneSource source = new ToneSource(SAMPLE_RATE, new double[]{offset + tone}, new double[]{0.5});
            drive(channelizer, source, 12);

            double rate = channel.getOutputSampleRate();
            double[][] samples = collector.samples(1);
            double[] amplitude = fit(samples, tone, rate);
            double gain = Math.hypot(amplitude[0], amplitude[1]) / 0.5;
            assertEquals(0.0, 20.0 * Math.log10(gain), 0.01, "gain at offset " + offset);

            double[] errors = errors(samples, tone, rate, amplitude);
            int length = channel.getOutputLength();
            int edge = length / 10;
            double edgeError = 0, interiorError = 0;
            int blocks = errors.length / length;

            for(int b = 0; b < blocks; b++)
            {
                int start = b * length;
                edgeError += (mean(errors, start, start + edge) + mean(errors, start + length - edge, start + length));
                interiorError += mean(errors, start + edge, start + length - edge);
            }

            edgeError /= 2 * blocks;
            interiorError /= blocks;
            assertTrue(db(mean(errors, 0, errors.length)) < -110, "EVM at offset " + offset + ": " +
                db(mean(errors, 0, errors.length)));
            assertTrue(db(edgeError) < -110 && db(edgeError) < db(interiorError) + 3.0, "block edge EVM " +
                db(edgeError) + " interior " + db(interiorError) + " at offset " + offset);
        }
    }

    /**
     * Tones outside the channel, from the adjacent 12.5 kHz channel to across the tuner band, are rejected by at
     * least 80 dB.
     */
    @Test
    void sweepRejection()
    {
        double channelOffset = 300012.5;
        double[] away = {12500, -12500, 18750, 25000, -25000, 50000, 125000, -250000, 600000, -1100000};

        for(double distance : away)
        {
            OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
            Collector collector = new Collector();
            channel(channelizer, channelOffset, collector);
            drive(channelizer, new ToneSource(SAMPLE_RATE, new double[]{channelOffset + distance},
                new double[]{0.5}), 6);
            double[][] samples = collector.samples(1);
            double power = 0;

            for(int x = 0; x < samples[0].length; x++)
            {
                power += samples[0][x] * samples[0][x] + samples[1][x] * samples[1][x];
            }

            double rejection = -db(power / samples[0].length / 0.25);
            assertTrue(rejection >= 80.0, "rejection " + rejection + " dB at " + distance + " Hz");
        }
    }

    /**
     * Decoder-ready channel (production mask: the decoder's own filter cascaded with a Kaiser low-pass): across the
     * decoder's pass band the response follows the decoder's own filter within 0.02 dB, and tones from 8.5 to 16.5 kHz
     * either side of the channel center (the adjacent channel beyond the decoder's transition band) are rejected by at
     * least 120 dB.  At 2.4 MS/s (25 kHz): P25 Phase 1 LSM, NBFM, P25 Phase 2 and the FM demodulator module.  At rates
     * other than the filter's own: LSM at 2.048 MS/s (25.6 kHz), NXDN 9600 at 10 MS/s (25 kHz in place of 24 kHz) and
     * AM 6 kHz at 10 MS/s (16 kHz in place of 12.5 kHz).  AM 6 kHz at 16 kHz and NBFM 7.5 kHz at 25 kHz have long
     * decoder filters that leave the Kaiser filter 23 and 13 taps, so its transition band starts at the decoder's pass
     * edge.  With 13 taps it adds less beyond the pass band, so for NBFM 7.5 kHz the adjacent band is rejected by at
     * least 90 dB.  The channel is on a bin, so that the mask is centered on the channel and the decoder filter's
     * response applies as designed.
     */
    @Test
    void decoderReadyMaskCascadesDecoderFilter()
    {
        double channelOffset = 300000.0;
        ChannelSpecification lsm = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0, LSM_FILTER);
        List<ChannelSpecification> specifications = List.of(lsm,
            new io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM().getChannelSpecification(),
            new io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2().getChannelSpecification(),
            new io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard().getChannelSpecification(),
            lsm, nxdn(TransmissionMode.M9600), am(Bandwidth.BW_6_0), nbfm(Bandwidth.BW_7_5));
        double[] sampleRates = {SAMPLE_RATE, SAMPLE_RATE, SAMPLE_RATE, SAMPLE_RATE, 2048000.0, 10000000.0, 10000000.0,
            SAMPLE_RATE};
        double[] outputRates = {25000.0, 25000.0, 25000.0, 25000.0, 25600.0, 25000.0, 16000.0, 25000.0};
        double[] rejections = {120.0, 120.0, 120.0, 120.0, 120.0, 120.0, 120.0, 90.0};

        for(int x = 0; x < specifications.size(); x++)
        {
            ChannelSpecification specification = specifications.get(x);
            double sampleRate = sampleRates[x];
            ChannelSpecification.ChannelFilter filter = specification.getChannelFilter();
            assertTrue(OverlapSaveChannelSource.isDecoderReady(OverlapSaveChannelizer.create(sampleRate),
                specification));
            double reference = toneGain(specification, sampleRate, channelOffset, 0.0);
            float[] taps = null;
            double rate = 0;

            for(double tone = 500.0; tone <= filter.passFrequency(); tone += 500.0)
            {
                if(taps == null)
                {
                    OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(sampleRate);
                    rate = OverlapSaveChannelSource.createChannel(channelizer, channelOffset, specification)
                        .getOutputSampleRate();
                    assertEquals(outputRates[x], rate, 0.001, "output rate at " + sampleRate);
                    taps = filter.getTaps(rate);
                }

                double expected = response(taps, tone, rate) / response(taps, 0.0, rate);
                double measured = toneGain(specification, sampleRate, channelOffset, tone) / reference;
                assertEquals(20.0 * Math.log10(expected), 20.0 * Math.log10(measured), 0.02, "pass band at " + tone +
                    " Hz, filter pass " + filter.passFrequency() + " at " + sampleRate);
            }

            for(double distance = 8500.0; distance <= 16500.0; distance += 1000.0)
            {
                for(double sign : new double[]{1.0, -1.0})
                {
                    double gain = toneGain(specification, sampleRate, channelOffset, sign * distance);
                    double rejection = -20.0 * Math.log10(gain / reference);
                    assertTrue(rejection >= rejections[x], "adjacent band rejection " + rejection + " dB at " +
                        sign * distance + " Hz, filter pass " + filter.passFrequency() + " at " + sampleRate);
                }
            }
        }
    }

    /**
     * RMS output amplitude of a production decoder-ready channel for a unit tone at the offset from the channel.
     */
    private static double toneGain(ChannelSpecification specification, double sampleRate, double channelOffset,
                                   double tone)
    {
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(sampleRate);
        OverlapSaveChannel channel = OverlapSaveChannelSource.createChannel(channelizer, channelOffset, specification);
        Collector collector = new Collector();
        channel.setListener(collector);
        channelizer.addChannel(channel);
        drive(channelizer, new ToneSource(sampleRate, new double[]{channelOffset + tone}, new double[]{1.0}), 6);
        double[][] samples = collector.samples(1);
        double power = 0;

        for(int x = 0; x < samples[0].length; x++)
        {
            power += samples[0][x] * samples[0][x] + samples[1][x] * samples[1][x];
        }

        return Math.sqrt(power / samples[0].length);
    }

    /**
     * Magnitude response of FIR taps at the frequency.
     */
    private static double response(float[] taps, double frequency, double sampleRate)
    {
        double re = 0, im = 0;

        for(int k = 0; k < taps.length; k++)
        {
            re += taps[k] * Math.cos(2.0 * Math.PI * frequency * k / sampleRate);
            im -= taps[k] * Math.sin(2.0 * Math.PI * frequency * k / sampleRate);
        }

        return Math.hypot(re, im);
    }

    /**
     * The tuner retunes by a step that isn't a whole number of bins or a multiple of the overlap factor, at the start
     * of a block's new samples, and the channel is retuned at that block boundary.  After the one block whose filter
     * memory spans the retune, the output continues the same tone with no phase step.
     */
    @Test
    void retunePhaseContinuity()
    {
        double offset = 250000.0;
        double step = 12345.6;
        double tone = 987.0;
        int retuneBlock = 6;
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        Collector collector = new Collector();
        OverlapSaveChannel channel = channel(channelizer, offset, collector);
        channelizer.setBlockBoundaryTask(() -> {
            if(channelizer.getBlockCount() == retuneBlock)
            {
                channel.setFrequencyOffset(offset - step);
            }
        });
        ToneSource source = new ToneSource(SAMPLE_RATE, new double[]{offset + tone}, new double[]{0.5});

        //The first block's overlap starts out empty, so the new samples of block b start at input sample b x hop
        long retuneSample = (long)retuneBlock * channelizer.getHop();

        while(source.getSample() < retuneSample)
        {
            channelizer.receive(source.next((int)Math.min(16384, retuneSample - source.getSample())), 0);
        }

        source.setLocalOscillatorOffset(step);
        drive(channelizer, source, 14);

        double rate = channel.getOutputSampleRate();
        int length = channel.getOutputLength();
        double[][] all = collector.samples(0);
        double[][] before = new double[2][];
        before[0] = java.util.Arrays.copyOfRange(all[0], length, retuneBlock * length);
        before[1] = java.util.Arrays.copyOfRange(all[1], length, retuneBlock * length);
        double[] amplitude = fit(before, tone, rate);

        //Reference the fit to output sample zero, then measure every sample against that one continuous tone
        double phase = -2.0 * Math.PI * tone * length / rate;
        double[] reference = {amplitude[0] * Math.cos(phase) - amplitude[1] * Math.sin(phase),
            amplitude[0] * Math.sin(phase) + amplitude[1] * Math.cos(phase)};
        double[] errors = errors(all, tone, rate, reference);

        assertTrue(db(mean(errors, length, retuneBlock * length)) < -110, "before the retune");
        assertTrue(db(mean(errors, (retuneBlock + 1) * length, errors.length)) < -110, "after the retune: " +
            db(mean(errors, (retuneBlock + 1) * length, errors.length)) + " dB");
    }

    /**
     * A frequency correction moves the output tone by the correction with a continuous phase.
     */
    @Test
    void frequencyCorrection()
    {
        double offset = -400000.0;
        double tone = 500.0;
        double correction = 210.0;
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        Collector collector = new Collector();
        OverlapSaveChannel channel = channel(channelizer, offset, collector);
        channelizer.setBlockBoundaryTask(() -> {
            if(channelizer.getBlockCount() == 5)
            {
                channel.setFrequencyCorrection(correction);
            }
        });
        drive(channelizer, new ToneSource(SAMPLE_RATE, new double[]{offset + tone}, new double[]{0.5}), 10);

        double rate = channel.getOutputSampleRate();
        int length = channel.getOutputLength();
        double[][] after = new double[2][];
        double[][] all = collector.samples(0);
        after[0] = java.util.Arrays.copyOfRange(all[0], 5 * length, all[0].length);
        after[1] = java.util.Arrays.copyOfRange(all[1], 5 * length, all[1].length);
        double[] amplitude = fit(after, tone + correction, rate);
        assertEquals(0.5, Math.hypot(amplitude[0], amplitude[1]), 0.001);
        assertTrue(db(mean(errors(after, tone + correction, rate, amplitude), 0, after[0].length)) < -110);
    }

    /**
     * The channel source applies the consumer's channel filter at the filter's sample rate when the specification
     * has one that the channelizer can apply, and otherwise delivers 50 kHz like a polyphase channel.
     */
    @Test
    void channelFilterSelection()
    {
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        TunerChannel tunerChannel = new TunerChannel(851012500, 12500);
        ChannelSpecification filtered = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0, LSM_FILTER);
        ChannelSpecification plain = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0);

        OverlapSaveChannelSource source = new OverlapSaveChannelSource(null, tunerChannel, filtered, channelizer,
            851000000, "test", null);
        assertTrue(source.isChannelFiltered());
        assertEquals(25000.0, source.getSampleRate(), 0.001);

        source = new OverlapSaveChannelSource(null, tunerChannel, plain, channelizer, 851000000, "test", null);
        assertFalse(source.isChannelFiltered());
        assertEquals(50000.0, source.getSampleRate(), 0.001);
    }

    /**
     * A channel fits when its own bandwidth is within the tuner's sampled bandwidth, even though its output band
     * (25 kHz decoder-ready, 50 kHz wide) extends past the band edge.  The channel source manager validates center
     * frequencies by the channel's bandwidth, so the two agree at a tuner with 100% usable bandwidth.
     */
    @Test
    void bandEdgeChannelFits()
    {
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        long center = 851000000;
        long edge = center + (long)(SAMPLE_RATE / 2) - 6250;
        ChannelSpecification lsm = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0, LSM_FILTER);
        ChannelSpecification plain = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0);

        for(ChannelSpecification specification : List.of(lsm, plain))
        {
            new OverlapSaveChannelSource(null, new TunerChannel(edge, 12500), specification, channelizer, center,
                "test", null);
            new OverlapSaveChannelSource(null, new TunerChannel(center - (edge - center), 12500), specification,
                channelizer, center, "test", null);
            assertThrows(IllegalArgumentException.class, () -> new OverlapSaveChannelSource(null,
                new TunerChannel(edge + 1, 12500), specification, channelizer, center, "test", null));
        }
    }

    /**
     * A channel is decoder-ready at an available rate that the decoder uses without decimating, where the decoder's
     * filter fits the overlap: the filter's own rate if available, otherwise the lowest qualifying rate above it.
     * Otherwise the source falls back to the wide 50 kHz channel, unflagged.
     */
    @Test
    void decoderReadyRateAndFilterLength()
    {
        TunerChannel tunerChannel = new TunerChannel(851012500, 12500);
        ChannelSpecification nxdn4800 = nxdn(TransmissionMode.M4800);
        ChannelSpecification lsm = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0, LSM_FILTER);

        //2.4 MS/s: 12 kHz is available (decimation 200 divides the 12,000-sample overlap), but the 61-tap limit there
        //is shorter than the NXDN 4800 filter
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        int outputSize = OverlapSaveChannel.getOutputSize(channelizer, 12000.0);
        assertEquals(12000.0, outputSize * channelizer.getBinWidth(), 0.001);
        assertTrue(nxdn4800.getChannelFilter().getTaps(12000.0).length >
            OverlapSaveChannel.getMaximumFilterLength(channelizer, outputSize));
        assertFalse(OverlapSaveChannelSource.isDecoderReady(channelizer, nxdn4800));
        assertEquals(25000.0, getDecoderReadyRate(channelizer, lsm), 0.001);

        //10 MS/s: NXDN 4800's filter is too long at every rate it accepts (its 125 Hz transition), so it gets the wide
        //channel.  NXDN 9600 gets 25 kHz and AM 6 kHz gets 16 kHz, since 24 and 12.5 kHz aren't available.  AM 3 kHz's
        //filter is too long at every rate it accepts.
        channelizer = OverlapSaveChannelizer.create(10000000.0);
        assertFalse(OverlapSaveChannelSource.isDecoderReady(channelizer, nxdn4800));
        assertEquals(25000.0, getDecoderReadyRate(channelizer, lsm), 0.001);
        assertEquals(25000.0, getDecoderReadyRate(channelizer, nxdn(TransmissionMode.M9600)), 0.001);
        assertEquals(16000.0, getDecoderReadyRate(channelizer, am(Bandwidth.BW_6_0)), 0.001);
        assertFalse(OverlapSaveChannelSource.isDecoderReady(channelizer, am(Bandwidth.BW_3_0)));

        OverlapSaveChannelSource source = new OverlapSaveChannelSource(null, tunerChannel, nxdn4800, channelizer,
            851000000, "test", null);
        assertFalse(source.isChannelFiltered());
        assertEquals(50000.0, source.getSampleRate(), 0.001);

        source = new OverlapSaveChannelSource(null, tunerChannel, am(Bandwidth.BW_6_0), channelizer, 851000000,
            "test", null);
        assertTrue(source.isChannelFiltered());
        assertEquals(16000.0, source.getSampleRate(), 0.001);

        //2.048 MS/s: 25 kHz isn't available, so 25.6 kHz
        channelizer = OverlapSaveChannelizer.create(2048000.0);
        assertEquals(25600.0, getDecoderReadyRate(channelizer, lsm), 0.001);
        source = new OverlapSaveChannelSource(null, tunerChannel, lsm, channelizer, 851000000, "test", null);
        assertTrue(source.isChannelFiltered());
        assertEquals(25600.0, source.getSampleRate(), 0.001);

        assertFalse(OverlapSaveChannelSource.isDecoderReady(channelizer, null));
        assertFalse(OverlapSaveChannelSource.isDecoderReady(channelizer,
            new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0)));
    }

    private static double getDecoderReadyRate(OverlapSaveChannelizer channelizer, ChannelSpecification specification)
    {
        return OverlapSaveChannelSource.getDecoderReadyOutputSize(channelizer, specification) *
            channelizer.getBinWidth();
    }

    private static ChannelSpecification nxdn(TransmissionMode mode)
    {
        DecodeConfigNXDN config = new DecodeConfigNXDN();
        config.setTransmissionMode(mode);
        return config.getChannelSpecification();
    }

    private static ChannelSpecification nbfm(Bandwidth bandwidth)
    {
        io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM config =
            new io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM();
        config.setBandwidth(bandwidth);
        return config.getChannelSpecification();
    }

    private static ChannelSpecification am(Bandwidth bandwidth)
    {
        DecodeConfigAM config = new DecodeConfigAM();
        config.setBandwidth(bandwidth);
        return config.getChannelSpecification();
    }

    /**
     * The channel source processes the gathered blocks on its own dispatcher thread and delivers one buffer per block.
     */
    @Test
    void channelSourceDispatch() throws Exception
    {
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        double offset = -123456.0;
        long tunerFrequency = 450000000;
        TunerChannel tunerChannel = new TunerChannel(tunerFrequency + (long)offset, 12500);
        ChannelSpecification specification = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0, LSM_FILTER);
        int blocks = 8;
        CountDownLatch latch = new CountDownLatch(blocks);
        List<ComplexSamples> received = new ArrayList<>();

        OverlapSaveChannelSource[] holder = new OverlapSaveChannelSource[1];
        Listener<SourceEvent> producer = event -> {
            if(event.getEvent() == SourceEvent.Event.REQUEST_START_SAMPLE_STREAM)
            {
                channelizer.addChannel(holder[0]);
            }
        };
        OverlapSaveChannelSource source = new OverlapSaveChannelSource(producer, tunerChannel, specification,
            channelizer, tunerFrequency, "test overlap-save channel", null);
        holder[0] = source;
        source.setListener(samples -> {
            synchronized(received)
            {
                received.add(samples);
            }
            latch.countDown();
        });
        source.setFrequencyCorrection(100);
        source.start();

        try
        {
            drive(channelizer, new ToneSource(SAMPLE_RATE, new double[]{offset + 700.0}, new double[]{0.5}), blocks);
            assertTrue(latch.await(10, TimeUnit.SECONDS), "blocks delivered");
        }
        finally
        {
            source.stop();
        }

        Collector collector = new Collector();

        synchronized(received)
        {
            assertEquals(blocks, received.size());
            collector.mBlocks.addAll(received);
        }

        assertEquals(500, collector.mBlocks.get(0).i().length, "one block per buffer");
        double[][] samples = collector.samples(1);
        double[] amplitude = fit(samples, 800.0, source.getSampleRate());
        //The channel filter includes the decoder's own filter and its +/- 0.01 pass band ripple
        assertEquals(0.5, Math.hypot(amplitude[0], amplitude[1]), 0.006);
        assertTrue(db(mean(errors(samples, 800.0, source.getSampleRate(), amplitude), 0, samples[0].length)) < -110);
    }

    /**
     * An in-band tone has the same amplitude at the output of an overlap-save channel as at the output of a polyphase
     * channel (one channel and two-channel synthesis), so that levels, squelch thresholds and power displays match.
     */
    @Test
    void gainMatchesPolyphase() throws Exception
    {
        long center = 450000000;
        long[] frequencies = {center + 300000, center + 312500};
        double tone = 1500.0;
        List<Listener<ComplexSamples>> listeners = new ArrayList<>();
        List<Collector> collectors = new ArrayList<>();

        for(long ignored : frequencies)
        {
            Collector collector = new Collector();
            collectors.add(collector);
            listeners.add(collector);
        }

        ChannelizerPaths.Polyphase polyphase = new ChannelizerPaths.Polyphase(SAMPLE_RATE, center, frequencies,
            listeners);
        assertEquals(1, polyphase.getSynthesizedCount());

        for(int x = 0; x < frequencies.length; x++)
        {
            ToneSource source = new ToneSource(SAMPLE_RATE, new double[]{frequencies[x] - center + tone},
                new double[]{0.5});
            collectors.get(x).mBlocks.clear();
            ChannelizerPaths.Polyphase path = x == 0 ? polyphase : new ChannelizerPaths.Polyphase(SAMPLE_RATE, center,
                new long[]{frequencies[x]}, List.of(collectors.get(x)));

            for(int chunk = 0; chunk < 40; chunk++)
            {
                path.receive(source.next(16384), 0);
            }

            double[][] samples = collectors.get(x).samples(2);
            double[] amplitude = fit(samples, tone, 50000.0);
            double polyphaseGain = 20.0 * Math.log10(Math.hypot(amplitude[0], amplitude[1]) / 0.5);
            assertEquals(0.0, polyphaseGain, 0.1, "polyphase gain for channel " + x);
        }
    }
}
