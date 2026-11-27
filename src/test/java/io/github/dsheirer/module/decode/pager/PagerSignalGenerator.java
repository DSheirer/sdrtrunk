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

package io.github.dsheirer.module.decode.pager;

import io.github.dsheirer.module.decode.pager.flex.FlexMode;
import io.github.dsheirer.module.decode.pager.pocsag.PocsagDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test signal generator that encodes POCSAG and FLEX pages and synthesizes the FM demodulated (discriminator) audio
 * at 8 kHz, with level, DC offset, polarity and noise impairments.
 */
public class PagerSignalGenerator
{
    public static final double SAMPLE_RATE = 8000.0;

    private final Random mRandom = new Random(1234);
    private final List<Float> mSamples = new ArrayList<>();
    private double mGain = 0.6;
    private double mDcOffset = 0.15;
    private double mNoise = 0.05;
    private boolean mInverted;
    private double mTime;
    private double mSymbolClock;
    private double mTonePhase;

    public PagerSignalGenerator gain(double gain)
    {
        mGain = gain;
        return this;
    }

    public PagerSignalGenerator dcOffset(double dcOffset)
    {
        mDcOffset = dcOffset;
        return this;
    }

    public PagerSignalGenerator noise(double noise)
    {
        mNoise = noise;
        return this;
    }

    public PagerSignalGenerator inverted(boolean inverted)
    {
        mInverted = inverted;
        return this;
    }

    /**
     * Adds noise-only samples (no signal) for the duration.
     */
    public PagerSignalGenerator silence(double seconds)
    {
        int count = (int)(seconds * SAMPLE_RATE);

        for(int x = 0; x < count; x++)
        {
            mSamples.add((float)(mDcOffset + mRandom.nextGaussian() * mGain * 0.5));
            mTime += 1.0 / SAMPLE_RATE;
        }

        mSymbolClock = mTime;

        return this;
    }

    /**
     * Modulates symbol levels (-1.0 to 1.0, where -1.0 is the lower frequency) at the symbol rate.  The symbol
     * transitions are smoothed with a raised cosine shaped transition over half of the symbol period.
     */
    public PagerSignalGenerator symbols(List<Double> levels, double symbolRate)
    {
        double symbolPeriod = 1.0 / symbolRate;
        double samplePeriod = 1.0 / SAMPLE_RATE;
        //Symbol timing is continuous across calls, independent of the sample instants
        double start = mSymbolClock;
        double end = start + levels.size() * symbolPeriod;

        while(mTime < end)
        {
            double position = (mTime - start) / symbolPeriod;
            int index = Math.min(levels.size() - 1, (int)position);
            double fraction = position - index;
            double level = levels.get(index);

            //Smooth transition from the previous symbol over the first quarter of the symbol
            if(index > 0 && fraction < 0.25)
            {
                double previous = levels.get(index - 1);
                double weight = 0.5 - 0.5 * Math.cos(Math.PI * (fraction / 0.25 * 0.5 + 0.5));
                level = previous + (level - previous) * weight;
            }

            if(mInverted)
            {
                level = -level;
            }

            mSamples.add((float)(mDcOffset + level * mGain + mRandom.nextGaussian() * mNoise));
            mTime += samplePeriod;
        }

        mSymbolClock = end;
        return this;
    }

    /**
     * Modulates bits as audio frequency shift keyed (AFSK) tones with continuous phase, as when POCSAG is sent over a
     * voice channel.
     * @param bits to send
     * @param bitRate of the bits
     * @param oneTone frequency for a 1 bit
     * @param zeroTone frequency for a 0 bit
     */
    public PagerSignalGenerator afsk(List<Integer> bits, double bitRate, double oneTone, double zeroTone)
    {
        double start = mSymbolClock;
        double end = start + bits.size() / bitRate;
        double samplePeriod = 1.0 / SAMPLE_RATE;

        while(mTime < end)
        {
            int index = Math.min(bits.size() - 1, (int)((mTime - start) * bitRate));
            double frequency = bits.get(index) == 1 ? oneTone : zeroTone;
            mTonePhase += 2.0 * Math.PI * frequency * samplePeriod;
            mSamples.add((float)(mDcOffset + Math.sin(mTonePhase) * mGain + mRandom.nextGaussian() * mNoise));
            mTime += samplePeriod;
        }

        mSymbolClock = end;
        return this;
    }

    /**
     * Generated audio samples
     */
    public float[] getSamples()
    {
        float[] samples = new float[mSamples.size()];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = mSamples.get(x);
        }

        return samples;
    }

    /**
     * Binary symbol levels where a 1 bit is the lower frequency (-1.0) as specified for POCSAG and the FLEX sync.
     */
    public static List<Double> bitsLowIsOne(List<Integer> bits)
    {
        List<Double> levels = new ArrayList<>();

        for(int bit: bits)
        {
            levels.add(bit == 1 ? -1.0 : 1.0);
        }

        return levels;
    }

    /* ******************************* POCSAG ******************************* */

    /**
     * Creates a 32-bit POCSAG codeword from 21 data bits.
     */
    public static int pocsagCodeword(int data)
    {
        int bch = BCH3121.encode(data);
        return (bch << 1) | (Integer.bitCount(bch) & 1);
    }

    /**
     * Creates the codewords for a POCSAG alphanumeric page (address + message codewords).
     */
    public static List<Integer> pocsagAlphaPage(int capcode, int function, String text)
    {
        List<Integer> bits = new ArrayList<>();

        for(char c: text.toCharArray())
        {
            for(int bit = 0; bit < 7; bit++)
            {
                bits.add((c >> bit) & 1);
            }
        }

        //Terminate with EOT
        for(int bit = 0; bit < 7; bit++)
        {
            bits.add((0x04 >> bit) & 1);
        }

        while(bits.size() % 20 != 0)
        {
            bits.add(0);
        }

        List<Integer> codewords = new ArrayList<>();
        codewords.add(pocsagAddress(capcode, function));

        for(int x = 0; x < bits.size(); x += 20)
        {
            int value = 0;

            for(int y = 0; y < 20; y++)
            {
                value = (value << 1) | bits.get(x + y);
            }

            codewords.add(pocsagCodeword((1 << 20) | value));
        }

        return codewords;
    }

    /**
     * Creates the codewords for a POCSAG numeric page.
     */
    public static List<Integer> pocsagNumericPage(int capcode, int function, String digits)
    {
        String characters = "0123456789*U -][";
        StringBuilder padded = new StringBuilder(digits);

        while(padded.length() % 5 != 0)
        {
            padded.append(' ');
        }

        List<Integer> codewords = new ArrayList<>();
        codewords.add(pocsagAddress(capcode, function));

        for(int x = 0; x < padded.length(); x += 5)
        {
            int value = 0;

            for(int y = 0; y < 5; y++)
            {
                int digit = characters.indexOf(padded.charAt(x + y));
                value = (value << 4) | (Integer.reverse(digit) >>> 28);
            }

            codewords.add(pocsagCodeword((1 << 20) | value));
        }

        return codewords;
    }

    private static int pocsagAddress(int capcode, int function)
    {
        return pocsagCodeword(((capcode >>> 3) << 2) | (function & 0x3));
    }

    /**
     * Creates the bits for a POCSAG transmission of the pages: preamble, then batches with each page starting in the
     * frame assigned by its capcode.
     * @param pages each list is the codewords for a page, starting with the address codeword
     * @param capcodes for each page, to determine the frame
     */
    public static List<Integer> pocsagTransmission(List<List<Integer>> pages, List<Integer> capcodes)
    {
        List<Integer> codewords = new ArrayList<>();

        for(int page = 0; page < pages.size(); page++)
        {
            int frame = capcodes.get(page) & 0x7;

            //Pad with idle codewords until the codeword position (within the batch) matches the frame
            while((codewords.size() % 16) / 2 != frame || codewords.size() % 2 != 0)
            {
                codewords.add(PocsagDecoder.IDLE_CODEWORD);
            }

            codewords.addAll(pages.get(page));
        }

        codewords.add(PocsagDecoder.IDLE_CODEWORD);

        while(codewords.size() % 16 != 0)
        {
            codewords.add(PocsagDecoder.IDLE_CODEWORD);
        }

        List<Integer> bits = new ArrayList<>();

        for(int x = 0; x < 576; x++)
        {
            bits.add((x + 1) % 2);
        }

        for(int x = 0; x < codewords.size(); x++)
        {
            if(x % 16 == 0)
            {
                addBits(bits, PocsagDecoder.SYNC_CODEWORD, 32);
            }

            addBits(bits, codewords.get(x), 32);
        }

        return bits;
    }

    private static void addBits(List<Integer> bits, long value, int count)
    {
        for(int x = count - 1; x >= 0; x--)
        {
            bits.add((int)((value >>> x) & 1));
        }
    }

    /* ******************************* FLEX ******************************* */

    /**
     * Encodes 21 FLEX data bits into a 32-bit FLEX word where bit 0 is transmitted first.
     */
    public static int flexWord(int data)
    {
        int reversed = Integer.reverse(data & 0x1FFFFF) >>> 11;
        int word = BCH3121.reverse31(BCH3121.encode(reversed));
        return word | ((Integer.bitCount(word) & 1) << 31);
    }

    /**
     * Creates a frame information word with a valid checksum
     */
    public static int flexFrameInformation(int cycle, int frame)
    {
        int fiw = (cycle & 0xF) << 4 | (frame & 0x7F) << 8;
        int sum = ((fiw >>> 4) & 0xF) + ((fiw >>> 8) & 0xF) + ((fiw >>> 12) & 0xF) + ((fiw >>> 16) & 0xF) +
            ((fiw >>> 20) & 0x1);
        return fiw | ((0xF - (sum & 0xF)) & 0xF);
    }

    /**
     * Builder for the 88 data words of a FLEX phase.
     */
    public static class FlexPhase
    {
        private final List<Integer> mAddresses = new ArrayList<>();
        private final List<Integer> mVectors = new ArrayList<>();
        private final List<List<Integer>> mMessages = new ArrayList<>();
        private final List<Integer> mVectorTypes = new ArrayList<>();

        public FlexPhase alpha(int capcode, String text)
        {
            List<Integer> words = new ArrayList<>();
            //Fragment header: fragment number 3 (complete), not continued
            words.add(3 << 11);

            List<Integer> characters = new ArrayList<>();
            characters.add(0); //Message signature character, skipped by the decoder

            for(char c: text.toCharArray())
            {
                characters.add((int)c);
            }

            while(characters.size() % 3 != 0)
            {
                characters.add(0x03);
            }

            for(int x = 0; x < characters.size(); x += 3)
            {
                words.add(characters.get(x) | characters.get(x + 1) << 7 | characters.get(x + 2) << 14);
            }

            return add(capcode, 5, words);
        }

        public FlexPhase numeric(int capcode, String digits)
        {
            String characters = "0123456789 U -][";
            List<Integer> bits = new ArrayList<>();
            bits.add(0);
            bits.add(0);

            for(char c: digits.toCharArray())
            {
                int digit = characters.indexOf(c);

                for(int bit = 0; bit < 4; bit++)
                {
                    bits.add((digit >> bit) & 1);
                }
            }

            //Fill with 0xC (fill) digits
            while(bits.size() % 21 != 0)
            {
                bits.add((0xC >> ((bits.size() - 2) % 4)) & 1);
            }

            List<Integer> words = new ArrayList<>();

            for(int x = 0; x < bits.size(); x += 21)
            {
                int word = 0;

                for(int bit = 0; bit < 21; bit++)
                {
                    word |= bits.get(x + bit) << bit;
                }

                words.add(word);
            }

            return add(capcode, 3, words);
        }

        public FlexPhase tone(int capcode)
        {
            return add(capcode, 2, new ArrayList<>());
        }

        private FlexPhase add(int capcode, int type, List<Integer> words)
        {
            mAddresses.add(capcode + 0x8000);
            mVectorTypes.add(type);
            mMessages.add(words);
            return this;
        }

        /**
         * Builds the 88 FLEX words (encoded) for the phase.
         */
        public int[] build()
        {
            int[] data = new int[88];
            int addressStart = 1;
            int vectorStart = addressStart + mAddresses.size();
            int messageStart = vectorStart + mAddresses.size();

            data[0] = (vectorStart << 10) | ((addressStart - 1) << 8) | 0xF;

            int messagePointer = messageStart;

            for(int x = 0; x < mAddresses.size(); x++)
            {
                data[addressStart + x] = mAddresses.get(x);
                int type = mVectorTypes.get(x);
                List<Integer> words = mMessages.get(x);
                int vector;

                if(type == 2)
                {
                    vector = (type << 4) | (1 << 7);
                }
                else if(type == 3)
                {
                    vector = (type << 4) | (messagePointer << 7) | ((words.size() - 1) << 14);
                }
                else
                {
                    vector = (type << 4) | (messagePointer << 7) | (words.size() << 14);
                }

                data[vectorStart + x] = vector;

                for(int word: words)
                {
                    data[messagePointer++] = word;
                }
            }

            for(int x = messagePointer; x < 88; x++)
            {
                data[x] = 0x1FFFFF;
            }

            int[] encoded = new int[88];

            for(int x = 0; x < 88; x++)
            {
                encoded[x] = flexWord(data[x]);
            }

            return encoded;
        }
    }

    /**
     * Adds a FLEX frame to the signal.
     * @param mode of the frame
     * @param cycle number
     * @param frame number
     * @param phases encoded words for phases A-D (unused phases may be null)
     */
    public PagerSignalGenerator flexFrame(FlexMode mode, int cycle, int frame, int[][] phases)
    {
        //Sync 1 and FIW at 1600/2
        List<Integer> syncBits = new ArrayList<>();

        for(int x = 0; x < 32; x++)
        {
            syncBits.add(x % 2);
        }

        long sync = ((long)mode.getSyncCode() << 48) | (0xA6C6AAAAL << 16) | (~mode.getSyncCode() & 0xFFFFL);
        addBits(syncBits, sync, 64);
        List<Double> levels = bitsLowIsOne(syncBits);

        //FIW dotting and FIW are 'positive is one'
        for(int x = 0; x < 16; x++)
        {
            levels.add(x % 2 == 0 ? 1.0 : -1.0);
        }

        int fiw = flexWord(flexFrameInformation(cycle, frame));

        for(int x = 0; x < 32; x++)
        {
            levels.add(((fiw >>> x) & 1) == 1 ? 1.0 : -1.0);
        }

        symbols(levels, 1600);

        //Sync 2 and data at the mode symbol rate
        int symbolRate = mode.getSymbolRate();
        List<Double> data = new ArrayList<>();

        for(int x = 0; x < symbolRate * 25 / 1000; x++)
        {
            data.add(x % 2 == 0 ? 1.0 : -1.0);
        }

        int[][] words = new int[4][];

        for(int x = 0; x < 4; x++)
        {
            words[x] = phases.length > x && phases[x] != null ? phases[x] : new PagerSignalGenerator.FlexPhase().build();
        }

        for(int counter = 0; counter < 2816; counter++)
        {
            int index = ((counter >>> 5) & 0xFFF8) | (counter & 0x7);
            int bit = (counter >>> 3) & 0x1F;

            data.add(level(mode, bitOf(words[0][index], bit), bitOf(words[1][index], bit)));

            if(symbolRate == 3200)
            {
                data.add(level(mode, bitOf(words[2][index], bit), bitOf(words[3][index], bit)));
            }
        }

        symbols(data, symbolRate);
        return this;
    }

    private static int bitOf(int word, int bit)
    {
        return (word >>> bit) & 1;
    }

    /**
     * Symbol level for the A and B bits: 2-level is +1/-1 from the A bit, 4-level uses the Gray code
     * (A,B) 1,0 = +1.0, 1,1 = +1/3, 0,1 = -1/3, 0,0 = -1.0
     */
    private static double level(FlexMode mode, int bitA, int bitB)
    {
        if(mode.getLevels() == 2)
        {
            return bitA == 1 ? 1.0 : -1.0;
        }

        if(bitA == 1)
        {
            return bitB == 1 ? 1.0 / 3.0 : 1.0;
        }

        return bitB == 1 ? -1.0 / 3.0 : -1.0;
    }
}
