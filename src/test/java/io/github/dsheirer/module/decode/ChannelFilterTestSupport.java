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
package io.github.dsheirer.module.decode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.DoubleFunction;
import java.util.function.Function;

/**
 * Support for the decoder channel filter tests: runs a decoder on a synthetic channel, with or without the source's
 * channel filter flag and optionally with the channel pre-filtered by the decoder's published channel filter, and
 * records everything the decoder outputs so that two runs can be compared exactly.
 */
public final class ChannelFilterTestSupport
{
    /**
     * Sample buffer size: even, as the decimation filters require.
     */
    public static final int BUFFER_SIZE = 2048;

    private ChannelFilterTestSupport()
    {
    }

    /**
     * Decoder or demodulator under test.  The factory wires its outputs (messages, symbol stream, audio, decoder state
     * and source events) to the output list.
     */
    public interface DecoderUnderTest
    {
        Listener<SourceEvent> getSourceEventListener();

        void receive(ComplexSamples samples);

        void stop();
    }

    /**
     * Wraps a decoder's source event listener, sample listener and stop method.
     */
    public static DecoderUnderTest of(Listener<SourceEvent> sourceEventListener, Listener<ComplexSamples> samples,
                                      Runnable stop)
    {
        return new DecoderUnderTest()
        {
            @Override
            public Listener<SourceEvent> getSourceEventListener()
            {
                return sourceEventListener;
            }

            @Override
            public void receive(ComplexSamples complexSamples)
            {
                samples.receive(complexSamples);
            }

            @Override
            public void stop()
            {
                stop.run();
            }
        };
    }

    /**
     * Interfering tone offset for a channel filter: 1 kHz into the stop band, within the sample rate.
     */
    public static double getInterfererOffset(ChannelSpecification.ChannelFilter filter, double sampleRate)
    {
        return Math.min(filter.stopFrequency() + 1000.0, sampleRate / 2.0 - 500.0);
    }

    /**
     * Runs the decoder over the signal.
     * @param factory that creates the decoder with its outputs recorded in the list it's given
     * @param signal i and q samples at the sample rate
     * @param sampleRate of the signal
     * @param channelFiltered flag on the sample rate change event
     * @param prefilter taps applied to the signal ahead of the decoder, streamed across buffers, or null
     * @return recorded output
     */
    public static List<String> run(Function<List<String>, DecoderUnderTest> factory, float[][] signal,
                                   double sampleRate, boolean channelFiltered, float[] prefilter)
    {
        return run(factory, signal, sampleRate, new boolean[]{channelFiltered}, prefilter);
    }

    /**
     * Runs the decoder over the signal after a sequence of sample rate change events.
     * @param factory that creates the decoder with its outputs recorded in the list it's given
     * @param signal i and q samples at the sample rate
     * @param sampleRate of the signal
     * @param channelFiltered flags of the sample rate change events, sent in order before the samples
     * @param prefilter taps applied to the signal ahead of the decoder, streamed across buffers, or null
     * @return recorded output
     */
    public static List<String> run(Function<List<String>, DecoderUnderTest> factory, float[][] signal,
                                   double sampleRate, boolean[] channelFiltered, float[] prefilter)
    {
        List<String> output = new ArrayList<>();
        DecoderUnderTest decoder = factory.apply(output);

        for(boolean flag : channelFiltered)
        {
            decoder.getSourceEventListener().receive(SourceEvent.sampleRateChange(sampleRate, flag, "Test"));
        }

        IRealFilter filterI = prefilter != null ? FilterFactory.getRealFilter(prefilter) : null;
        IRealFilter filterQ = prefilter != null ? FilterFactory.getRealFilter(prefilter) : null;
        int length = signal[0].length / BUFFER_SIZE * BUFFER_SIZE;

        for(int offset = 0; offset < length; offset += BUFFER_SIZE)
        {
            float[] i = new float[BUFFER_SIZE];
            float[] q = new float[BUFFER_SIZE];
            System.arraycopy(signal[0], offset, i, 0, BUFFER_SIZE);
            System.arraycopy(signal[1], offset, q, 0, BUFFER_SIZE);

            if(filterI != null)
            {
                i = filterI.filter(i);
                q = filterQ.filter(q);
            }

            decoder.receive(new ComplexSamples(i, q, (long)(offset * 1000.0 / sampleRate)));
        }

        decoder.stop();
        return output;
    }

    /**
     * Equivalence at the channel filter's sample rate: the decoder filtering the raw channel itself produces exactly
     * the same output as the decoder given the channel pre-filtered with the published filter taps and the channel
     * filter flag.  The flag on the raw channel must change the output, which shows that the decoder skips its filter.
     * @param label for assertion messages
     * @param factory for the decoder
     * @param signal raw channel at the filter's sample rate
     * @param filter published by the decoder
     */
    public static void assertEquivalentAtFilterRate(String label, Function<List<String>, DecoderUnderTest> factory,
                                                    float[][] signal, ChannelSpecification.ChannelFilter filter)
    {
        assertEquivalentAtRate(label, factory, signal, filter, filter.sampleRate());
    }

    /**
     * Equivalence at another sample rate where the decoder doesn't decimate (ChannelFilter.isUndecimated()), with the
     * published taps designed at that rate, as assertEquivalentAtFilterRate() does at the filter's sample rate.
     * @param label for assertion messages
     * @param factory for the decoder
     * @param signal raw channel at the sample rate
     * @param filter published by the decoder
     * @param sampleRate of the signal
     */
    public static void assertEquivalentAtRate(String label, Function<List<String>, DecoderUnderTest> factory,
                                              float[][] signal, ChannelSpecification.ChannelFilter filter,
                                              double sampleRate)
    {
        assertTrue(filter.isUndecimated(sampleRate), label + " undecimated at " + sampleRate);
        label = label + " at " + sampleRate + " Hz";
        List<String> decoderFiltered = run(factory, signal, sampleRate, false, null);
        List<String> sourceFiltered = run(factory, signal, sampleRate, true, filter.getTaps(sampleRate));
        List<String> unfiltered = run(factory, signal, sampleRate, true, null);

        assertFalse(decoderFiltered.isEmpty(), label + " produced no output");
        assertSame(label + " decoder filtered versus source filtered", decoderFiltered, sourceFiltered);
        assertFalse(Objects.equals(decoderFiltered, unfiltered), label + " output didn't change when the flag " +
            "skipped the decoder's filter on an unfiltered channel");
    }

    /**
     * A flagged sample rate change followed by an unflagged one at the filter's sample rate: the decoder filters
     * again, and its output on the raw channel equals the output with only the unflagged event.
     * @param label for assertion messages
     * @param factory for the decoder
     * @param signal raw channel at the filter's sample rate
     * @param filter published by the decoder
     */
    public static void assertUnflaggedEventRestoresFilter(String label,
                                                          Function<List<String>, DecoderUnderTest> factory,
                                                          float[][] signal, ChannelSpecification.ChannelFilter filter)
    {
        double sampleRate = filter.sampleRate();
        List<String> unflagged = run(factory, signal, sampleRate, false, null);
        List<String> flaggedThenUnflagged = run(factory, signal, sampleRate, new boolean[]{true, false}, null);
        assertFalse(unflagged.isEmpty(), label + " produced no output");
        assertSame(label + " flagged then unflagged versus unflagged", unflagged, flaggedThenUnflagged);
    }

    /**
     * At a sample rate where the decoder decimates, the channel filter flag changes nothing.
     * @param label for assertion messages
     * @param factory for the decoder
     * @param signal raw channel at the sample rate
     * @param sampleRate that the decoder decimates
     */
    public static void assertFlagIgnoredWhenDecimating(String label, Function<List<String>, DecoderUnderTest> factory,
                                                       float[][] signal, double sampleRate)
    {
        List<String> unflagged = run(factory, signal, sampleRate, false, null);
        List<String> flagged = run(factory, signal, sampleRate, true, null);
        assertFalse(unflagged.isEmpty(), label + " produced no output");
        assertSame(label + " with and without the flag at " + sampleRate + " Hz", unflagged, flagged);
    }

    /**
     * The filter's undecimated rates (ChannelFilter.isUndecimated()) match the rates where the decoder accepts the
     * channel and skips its own filter when the channel is flagged as filtered.  Checks the filter's sample rate and 1
     * to 2 Hz either side of every boundary of isUndecimated() found on a 50 Hz grid from 1 kHz to five times the
     * filter's sample rate.  At
     * each rate the decoder either rejects the rate change, or its output on a raw channel with a stop band interferer
     * changes with the flag (it skips its filter) or doesn't (it decimates, and the flag is ignored).
     * @param label for assertion messages
     * @param factory for the decoder
     * @param filter published by the decoder
     * @param signal generator of the raw channel at a sample rate
     */
    public static void assertUndecimatedRatesMatchDecoder(String label,
                                                          Function<List<String>, DecoderUnderTest> factory,
                                                          ChannelSpecification.ChannelFilter filter,
                                                          DoubleFunction<float[][]> signal)
    {
        List<Double> rates = new ArrayList<>();
        rates.add(filter.sampleRate());
        double step = 50.0;
        int boundaries = 0;

        for(double rate = 1000.0; rate < filter.sampleRate() * 5.0; rate += step)
        {
            if(filter.isUndecimated(rate) != filter.isUndecimated(rate + step))
            {
                double low = rate;
                double high = rate + step;

                while(high - low > 0.01)
                {
                    double middle = (low + high) / 2.0;

                    if(filter.isUndecimated(middle) == filter.isUndecimated(low))
                    {
                        low = middle;
                    }
                    else
                    {
                        high = middle;
                    }
                }

                rates.add(Math.floor(low) - 1.0);
                rates.add(Math.ceil(high) + 1.0);
                boundaries++;
            }
        }

        assertTrue(boundaries >= 2, label + " undecimated rate boundaries: " + boundaries);

        for(double rate : rates)
        {
            float[][] raw = impair(signal.apply(rate), rate, getInterfererOffset(filter, rate), 1.0, 0.05, 3);
            List<String> unflagged = runIfAccepted(factory, raw, rate, false);
            List<String> flagged = runIfAccepted(factory, raw, rate, true);
            boolean skipped = unflagged != null && flagged != null && !Objects.equals(unflagged, flagged);

            if(unflagged != null)
            {
                assertFalse(unflagged.isEmpty(), label + " produced no output at " + rate + " Hz");
            }

            assertEquals(skipped, filter.isUndecimated(rate), label + " at " + rate + " Hz: decoder " +
                (unflagged == null ? "rejects the rate" : skipped ? "skips its filter" : "doesn't skip its filter"));
        }
    }

    /**
     * Runs the decoder over the signal, or returns null if the decoder rejects the sample rate.
     */
    private static List<String> runIfAccepted(Function<List<String>, DecoderUnderTest> factory, float[][] signal,
                                              double sampleRate, boolean channelFiltered)
    {
        List<String> output = new ArrayList<>();
        DecoderUnderTest decoder = factory.apply(output);

        try
        {
            decoder.getSourceEventListener().receive(SourceEvent.sampleRateChange(sampleRate, channelFiltered,
                "Test"));
        }
        catch(RuntimeException e)
        {
            decoder.stop();
            return null;
        }

        int length = signal[0].length / BUFFER_SIZE * BUFFER_SIZE;

        for(int offset = 0; offset < length; offset += BUFFER_SIZE)
        {
            decoder.receive(new ComplexSamples(Arrays.copyOfRange(signal[0], offset, offset + BUFFER_SIZE),
                Arrays.copyOfRange(signal[1], offset, offset + BUFFER_SIZE), (long)(offset * 1000.0 / sampleRate)));
        }

        decoder.stop();
        return output;
    }

    /**
     * Compares two outputs exactly, reporting the first difference.
     */
    public static void assertSame(String label, List<String> expected, List<String> actual)
    {
        int common = Math.min(expected.size(), actual.size());

        for(int x = 0; x < common; x++)
        {
            if(!expected.get(x).equals(actual.get(x)))
            {
                fail(label + ": output " + x + " of " + expected.size() + " differs: [" +
                    abbreviate(expected.get(x)) + "] versus [" + abbreviate(actual.get(x)) + "]");
            }
        }

        if(expected.size() != actual.size())
        {
            fail(label + ": output count " + expected.size() + " versus " + actual.size());
        }
    }

    private static String abbreviate(String value)
    {
        return value.length() > 120 ? value.substring(0, 120) + "..." : value;
    }

    /**
     * Records a symbol stream buffer.
     */
    public static String bytes(ByteBuffer buffer)
    {
        ByteBuffer copy = buffer.duplicate();
        copy.position(0);
        byte[] bytes = new byte[copy.limit()];
        copy.get(bytes);
        return "B" + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Records an audio buffer exactly, as the bits of each sample.
     */
    public static String floats(String prefix, float[] samples)
    {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 4);

        for(float sample : samples)
        {
            buffer.putInt(Float.floatToRawIntBits(sample));
        }

        return prefix + Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * Four level FSK (C4FM, DMR, NXDN): random symbols with raised cosine shaped frequency pulses, frequency modulated.
     * @param sampleRate of the signal
     * @param symbolRate in symbols per second
     * @param deviation of the outer (+/- 3) symbols in hertz
     * @param seconds of signal
     * @param seed for the symbols
     * @return i and q samples
     */
    public static float[][] fsk4(double sampleRate, double symbolRate, double deviation, double seconds, long seed)
    {
        Random random = new Random(seed);
        int symbols = (int)(seconds * symbolRate);
        double[] levels = new double[symbols];

        for(int x = 0; x < symbols; x++)
        {
            levels[x] = (2 * random.nextInt(4) - 3) / 3.0;
        }

        double[] frequency = shape(levels, sampleRate, symbolRate, 0.2);
        int length = frequency.length;
        float[] i = new float[length];
        float[] q = new float[length];
        double phase = 0.0;

        for(int n = 0; n < length; n++)
        {
            phase += 2.0 * Math.PI * frequency[n] * deviation / sampleRate;
            i[n] = (float)Math.cos(phase);
            q[n] = (float)Math.sin(phase);
        }

        return new float[][]{i, q};
    }

    /**
     * Pi/4 DQPSK (P25 LSM and Phase 2): random dibits, differentially encoded, with raised cosine pulses.
     * @param sampleRate of the signal
     * @param symbolRate in symbols per second
     * @param seconds of signal
     * @param seed for the symbols
     * @return i and q samples
     */
    public static float[][] dqpsk(double sampleRate, double symbolRate, double seconds, long seed)
    {
        Random random = new Random(seed);
        int symbols = (int)(seconds * symbolRate);
        double[] symbolI = new double[symbols];
        double[] symbolQ = new double[symbols];
        double[] steps = {Math.PI / 4.0, 3.0 * Math.PI / 4.0, -Math.PI / 4.0, -3.0 * Math.PI / 4.0};
        double phase = 0.0;

        for(int x = 0; x < symbols; x++)
        {
            phase += steps[random.nextInt(4)];
            symbolI[x] = Math.cos(phase);
            symbolQ[x] = Math.sin(phase);
        }

        double[] i = shape(symbolI, sampleRate, symbolRate, 0.2);
        double[] q = shape(symbolQ, sampleRate, symbolRate, 0.2);
        return new float[][]{toFloat(i), toFloat(q)};
    }

    /**
     * Tones (a voice stand-in), frequency modulated.
     * @param sampleRate of the signal
     * @param deviation peak, in hertz
     * @param seconds of signal
     * @return i and q samples
     */
    public static float[][] fmTones(double sampleRate, double deviation, double seconds)
    {
        int length = (int)(seconds * sampleRate);
        float[] i = new float[length];
        float[] q = new float[length];
        double phase = 0.0;

        for(int n = 0; n < length; n++)
        {
            double t = n / sampleRate;
            double modulation = 0.5 * Math.sin(2.0 * Math.PI * 440.0 * t) + 0.3 * Math.sin(2.0 * Math.PI * 1270.0 * t) +
                0.2 * Math.sin(2.0 * Math.PI * 2210.0 * t);
            phase += 2.0 * Math.PI * deviation * modulation / sampleRate;
            i[n] = (float)Math.cos(phase);
            q[n] = (float)Math.sin(phase);
        }

        return new float[][]{i, q};
    }

    /**
     * Tones (a voice stand-in), amplitude modulated on a carrier at the channel center.
     * @param sampleRate of the signal
     * @param seconds of signal
     * @return i and q samples
     */
    public static float[][] amTones(double sampleRate, double seconds)
    {
        int length = (int)(seconds * sampleRate);
        float[] i = new float[length];
        float[] q = new float[length];

        for(int n = 0; n < length; n++)
        {
            double t = n / sampleRate;
            double modulation = 0.3 * Math.sin(2.0 * Math.PI * 440.0 * t) + 0.2 * Math.sin(2.0 * Math.PI * 1270.0 * t);
            i[n] = (float)(0.5 * (1.0 + modulation));
            q[n] = 0.0f;
        }

        return new float[][]{i, q};
    }

    /**
     * Adds an adjacent channel tone and white gaussian noise, so that the channel filter has work to do.
     * @param signal to modify in place
     * @param sampleRate of the signal
     * @param interfererOffset of the interfering tone in hertz
     * @param interfererAmplitude of the tone
     * @param noise standard deviation of each of i and q
     * @param seed for the noise
     * @return the signal
     */
    public static float[][] impair(float[][] signal, double sampleRate, double interfererOffset,
                                   double interfererAmplitude, double noise, long seed)
    {
        Random random = new Random(seed);

        for(int n = 0; n < signal[0].length; n++)
        {
            double phase = 2.0 * Math.PI * interfererOffset * n / sampleRate;
            signal[0][n] += (float)(interfererAmplitude * Math.cos(phase) + noise * random.nextGaussian());
            signal[1][n] += (float)(interfererAmplitude * Math.sin(phase) + noise * random.nextGaussian());
        }

        return signal;
    }

    /**
     * Shapes symbol values with raised cosine pulses at the sample rate, with a lead in and lead out of half the pulse
     * span (four symbols) each.
     */
    public static double[] shape(double[] symbols, double sampleRate, double symbolRate, double rolloff)
    {
        int span = 8;
        double samplesPerSymbol = sampleRate / symbolRate;
        int length = (int)((symbols.length + span) * samplesPerSymbol);
        double[] shaped = new double[length];

        for(int n = 0; n < length; n++)
        {
            double t = n / samplesPerSymbol - span / 2.0;
            double sum = 0.0;

            for(int k = (int)Math.ceil(t - span / 2.0); k <= t + span / 2.0; k++)
            {
                if(k >= 0 && k < symbols.length)
                {
                    sum += symbols[k] * raisedCosine(t - k, rolloff);
                }
            }

            shaped[n] = sum;
        }

        return shaped;
    }

    public static double raisedCosine(double t, double rolloff)
    {
        double sinc = Math.abs(t) < 1e-9 ? 1.0 : Math.sin(Math.PI * t) / (Math.PI * t);
        double denominator = 1.0 - Math.pow(2.0 * rolloff * t, 2.0);

        if(Math.abs(denominator) < 1e-9)
        {
            return Math.PI / 4.0 * sinc;
        }

        return sinc * Math.cos(Math.PI * rolloff * t) / denominator;
    }

    private static float[] toFloat(double[] values)
    {
        float[] floats = new float[values.length];

        for(int x = 0; x < values.length; x++)
        {
            floats[x] = (float)values[x];
        }

        return floats;
    }
}
