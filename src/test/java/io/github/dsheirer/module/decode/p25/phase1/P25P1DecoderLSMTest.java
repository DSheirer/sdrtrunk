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

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.edac.bch.BCH_63_16_23_P25;
import io.github.dsheirer.edac.trellis.P25_1_2_Node;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SyncDetector;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end JUnit tests for the P25 Phase 1 LSM decoder: a synthetic LSM (CQPSK) control channel carrying single
 * block TSBK frames is generated and decoded, and each valid decoded TSBK is checked against the transmitted TSBKs.
 */
public class P25P1DecoderLSMTest
{
    private static final double SAMPLE_RATE = 48000.0;
    private static final double SYMBOL_RATE = 4800.0;
    private static final int FRAME_COUNT = 150;
    private static final int NAC = 0x293;
    //Single block TSBK frame: 180 dibits (360 bits) including a status dibit every 36th dibit and null padding
    private static final int FRAME_DIBITS = 180;
    private static final int TSBK_DUID = 7;
    private static final int OPCODE_RFSS_STATUS_BROADCAST = 58;
    //Raised cosine transmit pulse
    private static final double ROLLOFF = 0.2;
    private static final int PULSE_SPAN_SYMBOLS = 8;
    private static final int BUFFER_SIZE = 2048;
    private static final Dibit[] DIBITS = {Dibit.D00_PLUS_1, Dibit.D01_PLUS_3, Dibit.D10_MINUS_1, Dibit.D11_MINUS_3};

    /**
     * Test: a clean signal decodes every TSBK frame, with no invalid messages.
     */
    @Test
    void testCleanSignal()
    {
        Result result = decode(0.0, 0.0, Double.NaN, 1, BUFFER_SIZE);
        assertEquals(0, result.invalid(), "Invalid messages");
        assertEquals(FRAME_COUNT, result.valid(), "Valid messages");
    }

    /**
     * Test: a carrier offset within the +/- 600 Hz fourth power offset estimator range and a 100 ppm symbol rate error
     * decode every TSBK frame.
     */
    @Test
    void testCarrierOffsetAndSymbolRateError()
    {
        Result result = decode(300.0, 100.0, Double.NaN, 2, BUFFER_SIZE);
        assertEquals(0, result.invalid(), "Invalid messages");
        assertEquals(FRAME_COUNT, result.valid(), "Valid messages");
    }

    /**
     * Test: carrier offsets beyond the fourth power offset estimator range are resolved by the offset ambiguity search,
     * after which every TSBK frame decodes.  The search steps toward the adjacent offset range nearest to the estimate,
     * so offsets on either side acquire after one search dwell period (~11 frames).
     */
    @Test
    void testOffsetAmbiguitySearch()
    {
        for(double carrierOffset : new double[]{700.0, -700.0, 900.0, -900.0})
        {
            Result result = decode(carrierOffset, -100.0, Double.NaN, 3, BUFFER_SIZE);
            assertEquals(0, result.invalid(), "Invalid messages at " + carrierOffset + " Hz");
            assertTrue(result.first() <= 16, "Acquisition at " + carrierOffset + " Hz: " + result.first() + " frames");
            assertEquals(0, result.missed(), "Missed frames after acquisition at " + carrierOffset + " Hz");
        }
    }

    /**
     * Test: acquisition of offsets near the edge of the fourth power offset estimator range is fast and independent of
     * the sample buffer size.  The estimator previously updated once per buffer, so a -550 Hz offset took 36 frames to
     * acquire with 2048 sample buffers and 55 frames with 8192 sample buffers.
     */
    @Test
    void testAcquisitionIndependentOfBufferSize()
    {
        for(int bufferSize : new int[]{512, 2048, 8192})
        {
            for(double carrierOffset : new double[]{550.0, -550.0})
            {
                Result result = decode(carrierOffset, 50.0, Double.NaN, 5, bufferSize);
                String condition = carrierOffset + " Hz with " + bufferSize + " sample buffers";
                assertEquals(0, result.invalid(), "Invalid messages at " + condition);
                assertTrue(result.first() <= 5, "Acquisition at " + condition + ": " + result.first() + " frames");
                assertEquals(0, result.missed(), "Missed frames after acquisition at " + condition);
            }
        }
    }

    /**
     * Test: with additive white gaussian noise at 12 dB in-band (+/- 3 kHz) carrier to noise, nearly all TSBK frames
     * decode and every valid TSBK matches a transmitted TSBK.
     */
    @Test
    void testNoise()
    {
        Result result = decode(200.0, 50.0, 12.0, 4, BUFFER_SIZE);
        assertTrue(result.valid() >= FRAME_COUNT - 5, "Valid messages: " + result.valid());
    }

    /**
     * Generates and decodes the synthetic signal.
     * @param carrierOffset in Hertz
     * @param symbolRateError in parts per million
     * @param carrierToNoise in dB, in-band (+/- 3 kHz), or NaN for no noise
     * @param seed for the random TSBK payloads and noise
     * @param bufferSize of the sample buffers delivered to the decoder (even)
     * @return decoded message counts, the index of the first decoded TSBK frame (the frame count when
     * none decode) and the count of TSBK frames that
     * didn't decode after the first decoded TSBK frame.
     */
    private static Result decode(double carrierOffset, double symbolRateError, double carrierToNoise, long seed,
                                 int bufferSize)
    {
        Random random = new Random(seed);
        List<BitSet> tsbks = new ArrayList<>();
        Set<BitSet> transmitted = new HashSet<>();
        List<Dibit> dibits = new ArrayList<>();

        for(int frame = 0; frame < FRAME_COUNT; frame++)
        {
            BitSet tsbk = getTSBK(random);
            tsbks.add(tsbk);
            transmitted.add(tsbk);
            dibits.addAll(getFrame(tsbk));
        }

        //Trailing frame that isn't checked: the decoder dispatches a message after its trailing symbols are processed
        dibits.addAll(getFrame(getTSBK(random)));

        float[][] samples = modulate(dibits, carrierOffset, symbolRateError, carrierToNoise, random);

        List<TSBKMessage> received = new ArrayList<>();
        P25P1DecoderLSM decoder = new P25P1DecoderLSM();
        decoder.setMessageListener((IMessage message) -> {
            if(message instanceof TSBKMessage tsbk)
            {
                received.add(tsbk);
            }
        });
        decoder.start();
        decoder.setSampleRate(SAMPLE_RATE);

        for(int offset = 0; offset < samples[0].length; offset += bufferSize)
        {
            int length = Math.min(bufferSize, samples[0].length - offset);
            float[] i = new float[length];
            float[] q = new float[length];
            System.arraycopy(samples[0], offset, i, 0, length);
            System.arraycopy(samples[1], offset, q, 0, length);
            decoder.receive(new ComplexSamples(i, q, 0L));
        }

        decoder.stop();

        int valid = 0;
        int invalid = 0;
        Set<BitSet> decoded = new HashSet<>();

        for(TSBKMessage tsbk : received)
        {
            if(tsbk.isValid())
            {
                BitSet bits = new BitSet();

                for(int x = 0; x < 80; x++)
                {
                    bits.set(x, tsbk.getMessage().get(x));
                }

                //A valid TSBK that doesn't match a transmitted TSBK is a CRC false positive
                assertTrue(transmitted.contains(bits), "Decoded TSBK was not transmitted");
                assertTrue(decoded.add(bits), "Decoded TSBK was duplicated");
                valid++;
            }
            else
            {
                invalid++;
            }
        }

        int first = tsbks.size();
        int missed = 0;

        for(int x = 0; x < tsbks.size(); x++)
        {
            if(decoded.contains(tsbks.get(x)))
            {
                first = Math.min(first, x);
            }
            else if(first < x)
            {
                missed++;
            }
        }

        return new Result(valid, invalid, first, missed);
    }

    /**
     * Creates a random single block (last block) TSBK, 80 message bits.
     */
    private static BitSet getTSBK(Random random)
    {
        BitSet tsbk = new BitSet();
        tsbk.set(0); //Last block
        setValue(tsbk, 2, 6, OPCODE_RFSS_STATUS_BROADCAST);

        for(int x = 16; x < 80; x++)
        {
            tsbk.set(x, random.nextBoolean());
        }

        return tsbk;
    }

    /**
     * Creates the frame dibits for the TSBK: sync, NID, interleaved trellis encoded TSBK and null padding, with a
     * status dibit at every 36th dibit.
     */
    private static List<Dibit> getFrame(BitSet tsbk)
    {
        List<Dibit> data = new ArrayList<>();
        addBits(data, P25P1SyncDetector.SYNC_PATTERN, 48);
        addBits(data, BCH_63_16_23_P25.encode(NAC, TSBK_DUID) << 1, 64); //64th (parity) bit is zero
        data.addAll(encodeTSBK(tsbk));

        List<Dibit> frame = new ArrayList<>();
        int pointer = 0;

        for(int x = 1; x <= FRAME_DIBITS; x++)
        {
            if(x % 36 == 0)
            {
                frame.add(Dibit.D01_PLUS_3); //Outbound (repeater) status
            }
            else
            {
                frame.add(pointer < data.size() ? data.get(pointer++) : Dibit.D00_PLUS_1);
            }
        }

        return frame;
    }

    /**
     * Adds the CRC-CCITT checksum, 1/2 rate trellis encodes and interleaves the TSBK into 98 dibits.
     */
    private static List<Dibit> encodeTSBK(BitSet tsbk)
    {
        BitSet message = (BitSet)tsbk.clone();
        int checksum = 0xFFFF;

        for(int x = message.nextSetBit(0); x >= 0 && x < 80; x = message.nextSetBit(x + 1))
        {
            checksum ^= CRCP25.CCITT_80_CHECKSUMS[x];
        }

        setValue(message, 80, 16, checksum);

        //48 input dibits plus a flushing zero input produce 98 encoded dibits
        int[] encoded = new int[98];
        int state = 0;

        for(int step = 0; step < 49; step++)
        {
            int input = step < 48 ? (message.get(2 * step) ? 2 : 0) + (message.get(2 * step + 1) ? 1 : 0) : 0;
            int output = P25_1_2_Node.TRANSITION_MATRIX[state][input];
            encoded[2 * step] = output >> 2;
            encoded[2 * step + 1] = output & 3;
            state = input;
        }

        List<Dibit> interleaved = new ArrayList<>();

        for(int dibit = 0; dibit < encoded.length; dibit++)
        {
            interleaved.add(DIBITS[encoded[P25P1Interleave.DATA_DEINTERLEAVE[2 * dibit] / 2]]);
        }

        return interleaved;
    }

    /**
     * Modulates the dibits as LSM: differential QPSK with raised cosine pulse shaping.
     * @return i and q sample arrays
     */
    private static float[][] modulate(List<Dibit> dibits, double carrierOffset, double symbolRateError,
                                      double carrierToNoise, Random random)
    {
        double[] symbolI = new double[dibits.size()];
        double[] symbolQ = new double[dibits.size()];
        double phase = 0.0;

        for(int x = 0; x < dibits.size(); x++)
        {
            phase += dibits.get(x).getIdealPhase();
            symbolI[x] = Math.cos(phase);
            symbolQ[x] = Math.sin(phase);
        }

        double samplesPerSymbol = SAMPLE_RATE / (SYMBOL_RATE * (1.0 + symbolRateError * 1e-6));
        //The decimation filter requires an even sample count
        int length = (int)((dibits.size() + PULSE_SPAN_SYMBOLS) * samplesPerSymbol) / 2 * 2;
        float[] i = new float[length];
        float[] q = new float[length];

        //Unit power symbols: the in-band noise variance is the in-band fraction of the total noise variance
        double noise = Double.isNaN(carrierToNoise) ? 0.0 :
                Math.sqrt(Math.pow(10.0, -carrierToNoise / 10.0) * SAMPLE_RATE / 6000.0 / 2.0);

        for(int n = 0; n < length; n++)
        {
            double t = n / samplesPerSymbol - PULSE_SPAN_SYMBOLS / 2.0; //Symbol periods
            double sumI = 0.0;
            double sumQ = 0.0;

            for(int k = (int)Math.ceil(t - PULSE_SPAN_SYMBOLS / 2.0); k <= t + PULSE_SPAN_SYMBOLS / 2.0; k++)
            {
                if(k >= 0 && k < dibits.size())
                {
                    double pulse = getRaisedCosine(t - k);
                    sumI += symbolI[k] * pulse;
                    sumQ += symbolQ[k] * pulse;
                }
            }

            double carrier = 2.0 * Math.PI * carrierOffset * n / SAMPLE_RATE;
            double cos = Math.cos(carrier);
            double sin = Math.sin(carrier);
            i[n] = (float)(sumI * cos - sumQ * sin + noise * random.nextGaussian());
            q[n] = (float)(sumI * sin + sumQ * cos + noise * random.nextGaussian());
        }

        return new float[][]{i, q};
    }

    /**
     * Raised cosine pulse value
     * @param t time in symbol periods
     */
    private static double getRaisedCosine(double t)
    {
        double sinc = Math.abs(t) < 1e-9 ? 1.0 : Math.sin(Math.PI * t) / (Math.PI * t);
        double denominator = 1.0 - Math.pow(2.0 * ROLLOFF * t, 2.0);

        if(Math.abs(denominator) < 1e-9)
        {
            return Math.PI / 4.0 * sinc;
        }

        return sinc * Math.cos(Math.PI * ROLLOFF * t) / denominator;
    }

    /**
     * Adds the most significant bits first, as dibits.
     */
    private static void addBits(List<Dibit> dibits, long value, int bitCount)
    {
        for(int bit = bitCount - 2; bit >= 0; bit -= 2)
        {
            dibits.add(DIBITS[(int)((value >> bit) & 3)]);
        }
    }

    /**
     * Sets the value at the bit offset, most significant bit first.
     */
    private static void setValue(BitSet bits, int offset, int bitCount, int value)
    {
        for(int x = 0; x < bitCount; x++)
        {
            bits.set(offset + x, ((value >> (bitCount - 1 - x)) & 1) == 1);
        }
    }

    private record Result(int valid, int invalid, int first, int missed) {}
}
