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

package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.edac.ReedSolomonTestEncoder;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.isch.ISCHDecoder;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.sync.P25P2SoftSyncDetector;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.FacchTimeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.ScramblingSequence;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end JUnit tests for the P25 Phase 2 HDQPSK decoder: a synthetic outbound (FDMA) channel carrying super frame
 * fragments with I-ISCH, S-ISCH (sync) and four scrambled FACCH timeslots is generated and decoded, and each valid
 * decoded MAC message is checked against the transmitted MAC messages.
 */
public class P25P2DecoderHDQPSKTest
{
    private static final double SAMPLE_RATE = 48000.0;
    private static final double SYMBOL_RATE = 6000.0;
    private static final int FRAGMENT_COUNT = 60;
    private static final int TRAILING_FRAGMENT_COUNT = 2;
    private static final ScrambleParameters SCRAMBLE_PARAMETERS = new ScrambleParameters(0xBEE00, 0x3BB, 0x3B2);
    private static final long ISCH_CODE_WORD_OFFSET = 0x184229d461L;
    private static final int[] DUID_BITS = {0, 1, 74, 75, 244, 245, 318, 319};
    //FACCH MAC PDU: 156 bits, with the CRC-12 checksum in the final 12 bits
    private static final int MAC_LENGTH = 156;
    private static final int MAC_CRC_START = 144;
    private static final int MAC_PDU_TYPE_IDLE = 3;
    //Raised cosine transmit pulse
    private static final double ROLLOFF = 0.2;
    private static final int PULSE_SPAN_SYMBOLS = 8;
    private static final int BUFFER_SIZE = 2048;
    private static final Dibit[] DIBITS = {Dibit.D00_PLUS_1, Dibit.D01_PLUS_3, Dibit.D10_MINUS_1, Dibit.D11_MINUS_3};
    private static final int MESSAGES_PER_FRAGMENT = 4;

    /**
     * Test: carrier offsets beyond the fourth power offset estimator range are resolved by the offset ambiguity search,
     * after which every FACCH MAC message decodes.  A fragment framed while the search steps to the correct offset range
     * can produce an invalid (CRC failure) MAC message.
     */
    @Test
    void testOffsetAmbiguitySearch()
    {
        for(double carrierOffset : new double[]{1000.0, -1000.0, 1200.0, -1200.0})
        {
            Result result = decode(carrierOffset, -100.0, Double.NaN, 3, false);
            assertTrue(result.invalid() <= 1, "Invalid messages at " + carrierOffset + " Hz: " + result.invalid());
            assertTrue(result.first() <= 12, "Acquisition at " + carrierOffset + " Hz: " + result.first() + " fragments");
            assertEquals(0, result.missed(), "Missed messages after acquisition at " + carrierOffset + " Hz");
        }
    }

    /**
     * Test: with additive white gaussian noise at 12 dB in-band (+/- 3.6 kHz) carrier to noise, nearly all FACCH MAC
     * messages decode and every valid MAC message matches a transmitted MAC message.
     */
    @Test
    void testNoise()
    {
        Result result = decode(200.0, 50.0, 12.0, 5, false);
        assertTrue(result.valid() >= FRAGMENT_COUNT * MESSAGES_PER_FRAGMENT * 0.9, "Valid messages: " + result.valid());
    }

    /**
     * Generates and decodes the synthetic signal.
     * @param carrierOffset in Hertz
     * @param symbolRateError in parts per million
     * @param carrierToNoise in dB, in-band (+/- 3.6 kHz), or NaN for no noise
     * @param seed for the random MAC payloads and noise
     * @param corruptIsch to corrupt both I-ISCH in the second and third fragment of each superframe
     * @return decoded message counts (valid messages for the checked fragments), the index of the first fragment with a decoded MAC message (the fragment count
     * when none decode), the count of MAC messages that didn't decode after that fragment, and the count of valid MAC
     * messages decoded from the replayed dibit stream.
     */
    private static Result decode(double carrierOffset, double symbolRateError, double carrierToNoise, long seed,
                                 boolean corruptIsch)
    {
        Random random = new Random(seed);
        ScramblingSequence scramblingSequence = new ScramblingSequence();
        scramblingSequence.update(SCRAMBLE_PARAMETERS);
        List<BinaryMessage> macs = new ArrayList<>();
        Set<BinaryMessage> transmitted = new HashSet<>();
        List<Dibit> dibits = new ArrayList<>();

        for(int fragment = 0; fragment < FRAGMENT_COUNT + TRAILING_FRAGMENT_COUNT; fragment++)
        {
            List<BinaryMessage> fragmentMacs = new ArrayList<>();

            for(int x = 0; x < MESSAGES_PER_FRAGMENT; x++)
            {
                fragmentMacs.add(getMacMessage(random));
            }

            //Trailing fragments aren't checked since the final fragment isn't dispatched until its trailing symbols are
            //processed, but they are included in the transmitted messages for the CRC false positive check.
            if(fragment < FRAGMENT_COUNT)
            {
                macs.addAll(fragmentMacs);
            }

            transmitted.addAll(fragmentMacs);

            boolean corrupt = corruptIsch && (fragment % 3) != 0;
            dibits.addAll(getFragment(fragment, fragmentMacs, scramblingSequence, corrupt, random));
        }

        float[][] samples = modulate(dibits, carrierOffset, symbolRateError, carrierToNoise, random);

        List<MacMessage> received = new ArrayList<>();
        List<ByteBuffer> buffers = new ArrayList<>();
        DecodeConfigP25Phase2 config = new DecodeConfigP25Phase2();
        config.setScrambleParameters(SCRAMBLE_PARAMETERS);
        P25P2DecoderHDQPSK decoder = new P25P2DecoderHDQPSK(config);
        decoder.setMessageListener((IMessage message) -> {
            if(message instanceof MacMessage mac && mac.getDataUnitID().isFACCH())
            {
                received.add(mac);
            }
        });
        decoder.setBufferListener(buffer -> {
            ByteBuffer copy = ByteBuffer.allocate(buffer.capacity());
            copy.put(buffer.array());
            buffers.add(copy);
        });
        decoder.start();
        decoder.setSampleRate(SAMPLE_RATE);

        for(int offset = 0; offset < samples[0].length; offset += BUFFER_SIZE)
        {
            int length = Math.min(BUFFER_SIZE, samples[0].length - offset);
            float[] i = new float[length];
            float[] q = new float[length];
            System.arraycopy(samples[0], offset, i, 0, length);
            System.arraycopy(samples[1], offset, q, 0, length);
            decoder.receive(new ComplexSamples(i, q, 0L));
        }

        decoder.stop();

        int invalid = 0;
        Set<BinaryMessage> decoded = new HashSet<>();

        for(MacMessage mac : received)
        {
            if(mac.isValid())
            {
                BinaryMessage bits = getBits(mac.getMessage());
                //A valid MAC message that doesn't match a transmitted MAC message is a CRC false positive
                assertTrue(transmitted.contains(bits), "Decoded MAC message was not transmitted");
                assertTrue(decoded.add(bits), "Decoded MAC message was duplicated");
            }
            else
            {
                invalid++;
            }
        }

        int valid = 0;

        int first = FRAGMENT_COUNT;
        int missed = 0;

        for(int x = 0; x < macs.size(); x++)
        {
            int fragment = x / MESSAGES_PER_FRAGMENT;

            if(decoded.contains(macs.get(x)))
            {
                valid++;
                first = Math.min(first, fragment);
            }
            else if(first < fragment)
            {
                missed++;
            }
        }

        return new Result(valid, invalid, first, missed, replay(buffers, new HashSet<>(macs)));
    }

    /**
     * Replays the demodulated dibit stream through a message framer with hard sync detection.
     * @return count of valid MAC messages that match a checked (transmitted) MAC message.
     */
    private static int replay(List<ByteBuffer> buffers, Set<BinaryMessage> transmitted)
    {
        Set<BinaryMessage> decoded = new HashSet<>();
        P25P2MessageFramer framer = new P25P2MessageFramer();
        framer.setScrambleParameters(SCRAMBLE_PARAMETERS);
        P25P2MessageProcessor processor = new P25P2MessageProcessor();
        processor.setMessageListener((IMessage message) -> {
            if(message instanceof MacMessage mac && mac.getDataUnitID().isFACCH() && mac.isValid())
            {
                BinaryMessage bits = getBits(mac.getMessage());

                if(transmitted.contains(bits))
                {
                    decoded.add(bits);
                }
            }
        });
        framer.setListener(processor);
        framer.start();

        for(ByteBuffer buffer : buffers)
        {
            framer.receive(buffer);
        }

        return decoded.size();
    }

    /**
     * Copies the MAC message bits for comparison against the transmitted MAC messages.
     */
    private static BinaryMessage getBits(CorrectedBinaryMessage message)
    {
        BinaryMessage bits = new BinaryMessage(MAC_LENGTH);

        for(int x = 0; x < MAC_LENGTH; x++)
        {
            if(message.get(x))
            {
                bits.set(x);
            }
        }

        return bits;
    }

    /**
     * Creates a random MAC IDLE PDU with a NULL INFORMATION MAC structure, random payload bits and the CRC-12 checksum.
     */
    private static BinaryMessage getMacMessage(Random random)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(MAC_LENGTH);
        message.load(0, 3, MAC_PDU_TYPE_IDLE);
        //Opcode (bits 8-15) is zero for the NULL INFORMATION MAC structure

        for(int x = 16; x < MAC_CRC_START; x++)
        {
            if(random.nextBoolean())
            {
                message.set(x);
            }
        }

        //The checksum isn't included in the CRC calculation, so exactly one checksum value passes the CRC check
        for(int checksum = 0; checksum < 4096; checksum++)
        {
            message.load(MAC_CRC_START, 12, checksum);

            if(CRCP25.crc12_FACCH(message))
            {
                return getBits(message);
            }
        }

        throw new IllegalStateException("Unable to calculate the CRC-12 checksum");
    }

    /**
     * Creates the 720 dibit super frame fragment: I-ISCH 1, timeslot A, I-ISCH 2, timeslot B, S-ISCH (sync),
     * timeslot C, S-ISCH (sync), timeslot D.
     * @param fragment index, which determines the ISCH sequence (fragment 1-3 of the superframe)
     * @param macs for timeslots A-D
     * @param scramblingSequence to scramble the timeslots
     * @param corruptIsch to corrupt both I-ISCH beyond the error correction capability
     * @param random for selecting the corrupted bits
     */
    private static List<Dibit> getFragment(int fragment, List<BinaryMessage> macs, ScramblingSequence scramblingSequence,
                                           boolean corruptIsch, Random random)
    {
        int ischSequence = fragment % 3;
        int superframeSequence = (fragment / 3) % 4;
        List<Dibit> dibits = new ArrayList<>();

        for(int position = 0; position < 4; position++)
        {
            switch(position)
            {
                case 0, 1 -> addBits(dibits, getIISCH(position, ischSequence, superframeSequence, corruptIsch, random), 40);
                default -> addBits(dibits, P25P2SoftSyncDetector.SYNC_PATTERN, 40);
            }

            BinaryMessage sequence = scramblingSequence.getTimeslotSequence(ischSequence * 4 + position);
            BinaryMessage timeslot = getTimeslot(macs.get(position), sequence);

            for(int x = 0; x < 320; x += 2)
            {
                dibits.add(DIBITS[(timeslot.get(x) ? 2 : 0) + (timeslot.get(x + 1) ? 1 : 0)]);
            }
        }

        return dibits;
    }

    /**
     * Creates the 40-bit I-ISCH codeword.
     * @param channel 0 (timeslot 1) or 1 (timeslot 2)
     * @param ischSequence 0-2 (fragment 1-3 of the superframe)
     * @param superframeSequence 0-3
     * @param corrupt to flip 12 codeword bits, which exceeds the 7 bit error correction capability
     * @param random for selecting the corrupted bits
     */
    private static long getIISCH(int channel, int ischSequence, int superframeSequence, boolean corrupt, Random random)
    {
        BinaryMessage message = new BinaryMessage(9);
        message.load(2, 2, channel);
        message.load(4, 2, ischSequence);
        message.load(7, 2, superframeSequence);
        long codeword = ISCHDecoder.getCodeWord(message) ^ ISCH_CODE_WORD_OFFSET;

        if(corrupt)
        {
            Set<Integer> bits = new HashSet<>();

            while(bits.size() < 12)
            {
                bits.add(random.nextInt(40));
            }

            for(int bit : bits)
            {
                codeword ^= 1L << bit;
            }
        }

        return codeword;
    }

    /**
     * Creates the scrambled 320-bit FACCH timeslot: RS(63,35,29) encodes the 26 MAC message hexbits, loads the message
     * and transmitted (19) parity hexbits into the FACCH timeslot fields, scrambles the timeslot, and sets the
     * (unscrambled) data unit ID.
     */
    private static BinaryMessage getTimeslot(BinaryMessage mac, BinaryMessage scramblingSequence)
    {
        //Message hexbit h (INFO_h+1) is codeword index 53 - h, and parity hexbit p (PARITY_p) is codeword index 28 - p
        int[] message = new int[35];

        for(int h = 0; h < 26; h++)
        {
            message[25 - h] = mac.getInt(h * 6, h * 6 + 5);
        }

        int[] codeword = ReedSolomonTestEncoder.encode(message);
        BinaryMessage timeslot = new BinaryMessage(320);

        for(int h = 1; h <= 26; h++)
        {
            load(timeslot, getFacchField("INFO_" + h), codeword[54 - h]);
        }

        for(int p = 1; p <= 19; p++)
        {
            load(timeslot, getFacchField("PARITY_" + p), codeword[28 - p]);
        }

        timeslot.xor(scramblingSequence);

        int duid = DataUnitID.SCRAMBLED_FACCH.getValueWithParity();

        for(int x = 0; x < DUID_BITS.length; x++)
        {
            if(((duid >> (7 - x)) & 1) == 1)
            {
                timeslot.set(DUID_BITS[x]);
            }
            else
            {
                timeslot.clear(DUID_BITS[x]);
            }
        }

        return timeslot;
    }

    /**
     * Accesses the FACCH timeslot hexbit field (IntField or FragmentedIntField) by name.
     */
    private static Object getFacchField(String name)
    {
        try
        {
            Field field = FacchTimeslot.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        }
        catch(ReflectiveOperationException e)
        {
            throw new IllegalStateException("FACCH timeslot field " + name, e);
        }
    }

    /**
     * Loads the 6-bit value into the field bits, most significant bit first.
     */
    private static void load(BinaryMessage message, Object field, int value)
    {
        int[] indices;

        if(field instanceof IntField intField)
        {
            indices = new int[intField.width()];

            for(int x = 0; x < indices.length; x++)
            {
                indices[x] = intField.start() + x;
            }
        }
        else
        {
            indices = ((FragmentedIntField)field).indices();
        }

        for(int x = 0; x < indices.length; x++)
        {
            if(((value >> (indices.length - 1 - x)) & 1) == 1)
            {
                message.set(indices[x]);
            }
        }
    }

    /**
     * Modulates the dibits as HDQPSK: differential QPSK with raised cosine pulse shaping.
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
                Math.sqrt(Math.pow(10.0, -carrierToNoise / 10.0) * SAMPLE_RATE / 7200.0 / 2.0);

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

    private record Result(int valid, int invalid, int first, int missed, int replayValid) {}
}
