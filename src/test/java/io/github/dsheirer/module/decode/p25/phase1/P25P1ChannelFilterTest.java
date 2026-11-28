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
package io.github.dsheirer.module.decode.p25.phase1;

import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertEquivalentAtFilterRate;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertEquivalentAtRate;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertFlagIgnoredWhenDecimating;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertUndecimatedRatesMatchDecoder;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertUnflaggedEventRestoresFilter;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.bytes;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.getInterfererOffset;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.impair;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.of;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.run;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.shape;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.edac.bch.BCH_63_16_23_P25;
import io.github.dsheirer.module.decode.ChannelFilterTestSupport.DecoderUnderTest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SyncDetector;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * P25 Phase 1 C4FM and LSM decoder channel filters, on a synthetic control channel of single block TSBK frames: at
 * the filter's sample rate the decoded TSBKs and the dibit stream are identical whether the decoder filters the raw
 * channel itself or receives it pre-filtered with the published taps and the channel filter flag, the flag changes
 * nothing at a sample rate where the decoder decimates, and the published taps are the decoder's own design.
 */
public class P25P1ChannelFilterTest
{
    private static final double SYMBOL_RATE = 4800.0;
    private static final int FRAME_COUNT = 60;
    private static final int FRAME_DIBITS = 180;
    private static final int NAC = 0x293;
    private static final int TSBK_DUID = 7;
    private static final int OPCODE_RFSS_STATUS_BROADCAST = 58;
    private static final double NOISE = 0.05;

    /**
     * Dibits of the frames: a sync, NID and trellis encoded random TSBK per frame, with status dibits.
     */
    private static List<Dibit> getDibits(long seed)
    {
        Random random = new Random(seed);
        List<Dibit> dibits = new ArrayList<>();

        //Trailing frame: the decoder dispatches a message after its trailing symbols are processed
        for(int frame = 0; frame <= FRAME_COUNT; frame++)
        {
            BitSet tsbk = new BitSet();
            tsbk.set(0); //Last block
            P25P1DecoderLSMTest.setValue(tsbk, 2, 6, OPCODE_RFSS_STATUS_BROADCAST);

            for(int x = 16; x < 80; x++)
            {
                tsbk.set(x, random.nextBoolean());
            }

            List<Dibit> data = new ArrayList<>();
            P25P1DecoderLSMTest.addBits(data, P25P1SyncDetector.SYNC_PATTERN, 48);
            P25P1DecoderLSMTest.addBits(data, BCH_63_16_23_P25.encode(NAC, TSBK_DUID) << 1, 64);
            data.addAll(P25P1DecoderLSMTest.encodeTSBK(tsbk));
            int pointer = 0;

            for(int x = 1; x <= FRAME_DIBITS; x++)
            {
                if(x % 36 == 0)
                {
                    dibits.add(Dibit.D01_PLUS_3); //Outbound (repeater) status
                }
                else
                {
                    dibits.add(pointer < data.size() ? data.get(pointer++) : Dibit.D00_PLUS_1);
                }
            }
        }

        return dibits;
    }

    /**
     * C4FM: raised cosine shaped frequency pulses at +/- 600 and +/- 1800 Hz, frequency modulated.
     */
    private static float[][] c4fm(double sampleRate)
    {
        List<Dibit> dibits = getDibits(21);
        double[] levels = new double[dibits.size()];

        for(int x = 0; x < levels.length; x++)
        {
            levels[x] = switch(dibits.get(x))
            {
                case D01_PLUS_3 -> 3.0;
                case D00_PLUS_1 -> 1.0;
                case D10_MINUS_1 -> -1.0;
                default -> -3.0;
            };
        }

        double[] frequency = shape(levels, sampleRate, SYMBOL_RATE, 0.2);
        float[] i = new float[frequency.length];
        float[] q = new float[frequency.length];
        double phase = 0.0;

        for(int n = 0; n < frequency.length; n++)
        {
            phase += 2.0 * Math.PI * frequency[n] * 600.0 / sampleRate;
            i[n] = (float)Math.cos(phase);
            q[n] = (float)Math.sin(phase);
        }

        return new float[][]{i, q};
    }

    /**
     * LSM: differential QPSK with raised cosine pulses.
     */
    private static float[][] lsm(double sampleRate)
    {
        List<Dibit> dibits = getDibits(22);
        double[] symbolI = new double[dibits.size()];
        double[] symbolQ = new double[dibits.size()];
        double phase = 0.0;

        for(int x = 0; x < dibits.size(); x++)
        {
            phase += dibits.get(x).getIdealPhase();
            symbolI[x] = Math.cos(phase);
            symbolQ[x] = Math.sin(phase);
        }

        double[] i = shape(symbolI, sampleRate, SYMBOL_RATE, 0.2);
        double[] q = shape(symbolQ, sampleRate, SYMBOL_RATE, 0.2);
        float[][] samples = new float[2][i.length];

        for(int n = 0; n < i.length; n++)
        {
            samples[0][n] = (float)i[n];
            samples[1][n] = (float)q[n];
        }

        return samples;
    }

    private static DecoderUnderTest c4fmDecoder(List<String> output)
    {
        P25P1DecoderC4FM decoder = new P25P1DecoderC4FM();
        decoder.setMessageListener(message -> output.add(message(message)));
        decoder.setBufferListener(buffer -> output.add(bytes(buffer)));
        decoder.start();
        return of(decoder.getSourceEventListener(), decoder::receive, decoder::stop);
    }

    private static DecoderUnderTest lsmDecoder(List<String> output)
    {
        P25P1DecoderLSM decoder = new P25P1DecoderLSM();
        decoder.setMessageListener(message -> output.add(message(message)));
        decoder.setBufferListener(buffer -> output.add(bytes(buffer)));
        decoder.start();
        return of(decoder.getSourceEventListener(), decoder::receive, decoder::stop);
    }

    private static String message(Object message)
    {
        if(message instanceof TSBKMessage tsbk)
        {
            return (tsbk.isValid() ? "T" : "X") + tsbk.getMessage().toHexString();
        }

        return "M" + message;
    }

    private static void check(String label, Function<List<String>, DecoderUnderTest> factory,
                              ChannelSpecification.ChannelFilter filter, DoubleFunction<float[][]> signal)
    {
        double rate = filter.sampleRate();
        float[][] raw = impair(signal.apply(rate), rate, getInterfererOffset(filter, rate), 1.0, NOISE, 1);
        assertEquivalentAtFilterRate(label, factory, raw, filter);
        assertUnflaggedEventRestoresFilter(label, factory, raw, filter);
        double otherRate = rate * 1.024;
        assertEquivalentAtRate(label, factory, impair(signal.apply(otherRate), otherRate,
            getInterfererOffset(filter, otherRate), 1.0, NOISE, 4), filter, otherRate);
        assertUndecimatedRatesMatchDecoder(label, factory, filter, signal);

        //The comparison covers decoded messages: nearly every TSBK decodes
        long valid = run(factory, raw, rate, false, null).stream().filter(item -> item.startsWith("T")).count();
        assertTrue(valid >= FRAME_COUNT - 5, label + " valid TSBKs: " + valid);

        double decimatingRate = rate * 2.0;
        assertFlagIgnoredWhenDecimating(label, factory, impair(signal.apply(decimatingRate), decimatingRate,
            getInterfererOffset(filter, rate), 1.0, NOISE, 2), decimatingRate);
    }

    @Test
    void c4fm()
    {
        check("C4FM", P25P1ChannelFilterTest::c4fmDecoder, P25P1DecoderC4FM.getChannelFilter(),
            P25P1ChannelFilterTest::c4fm);
    }

    @Test
    void lsm()
    {
        check("LSM", P25P1ChannelFilterTest::lsmDecoder, P25P1DecoderLSM.getChannelFilter(),
            P25P1ChannelFilterTest::lsm);
    }

    /**
     * The published taps are each decoder's own design, at the filter's sample rate and at another rate, and the
     * Phase 1 configuration publishes the filter for its modulation.
     */
    @Test
    void tapsAreTheDecodersOwnDesign()
    {
        for(double rate : new double[]{25000.0, 37500.0})
        {
            assertArrayEquals(FilterFactory.getRemezLowPass((float)rate, 5200.0, 6500.0),
                P25P1DecoderC4FM.getChannelFilter().getTaps(rate), "C4FM at " + rate);
            assertArrayEquals(FilterFactory.getRemezLowPass((float)rate, 4400.0, 5600.0),
                P25P1DecoderLSM.getChannelFilter().getTaps(rate), "LSM at " + rate);
        }

        assertEquals(25000.0, P25P1DecoderC4FM.getChannelFilter().sampleRate());
        assertEquals(25000.0, P25P1DecoderLSM.getChannelFilter().sampleRate());

        DecodeConfigP25Phase1 config = new DecodeConfigP25Phase1();
        config.setModulation(Modulation.C4FM);
        assertEquals(5200.0, config.getChannelSpecification().getChannelFilter().passFrequency());
        config.setModulation(Modulation.CQPSK);
        assertEquals(4400.0, config.getChannelSpecification().getChannelFilter().passFrequency());
    }
}
