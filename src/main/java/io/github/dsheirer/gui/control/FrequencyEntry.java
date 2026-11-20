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

package io.github.dsheirer.gui.control;

import java.math.BigDecimal;

/**
 * Frequency arithmetic for the {@link FrequencyControl}, kept free of JavaFX so that it can be unit tested.
 *
 * The control shows a frequency in MHz as a fixed number of digits: {@link #INTEGER_DIGITS} MHz digits and
 * {@link #FRACTION_DIGITS} fractional digits, so the least significant digit is 1 Hz.  Digit positions are
 * numbered by their power of ten in Hertz: position 0 is the 1 Hz digit and position 9 is the 1000 MHz digit.
 */
public final class FrequencyEntry
{
    /**
     * Number of MHz (integer) digits.
     */
    public static final int INTEGER_DIGITS = 4;

    /**
     * Number of fractional MHz digits (precision to 1 Hz).
     */
    public static final int FRACTION_DIGITS = 6;

    /**
     * Total number of displayed digits.
     */
    public static final int DIGIT_COUNT = INTEGER_DIGITS + FRACTION_DIGITS;

    /**
     * Largest frequency that fits the display: 9999.999999 MHz.
     */
    public static final long MAXIMUM_DISPLAYABLE = 9_999_999_999L;

    private FrequencyEntry()
    {
    }

    /**
     * Value of the digit at the position.
     * @param frequency in Hertz
     * @param position power of ten (0 = 1 Hz)
     * @return digit 0-9
     */
    public static int getDigit(long frequency, int position)
    {
        return (int)((Math.abs(frequency) / pow10(position)) % 10);
    }

    /**
     * Indicates if the digit at the position is a leading zero, which the control shows dimmed.  The 1 MHz digit is
     * never a leading zero.
     */
    public static boolean isLeadingZero(long frequency, int position)
    {
        return position > FRACTION_DIGITS && Math.abs(frequency) < pow10(position);
    }

    /**
     * Steps the frequency by a number of units of the digit position, carrying into the higher digits, and clamps
     * the result to the range.  A step that would leave the range lands on the range limit.
     * @param frequency current value in Hertz
     * @param position power of ten for the digit being stepped
     * @param steps positive to increment, negative to decrement
     * @param minimum allowed frequency
     * @param maximum allowed frequency
     * @return stepped and clamped frequency
     */
    public static long step(long frequency, int position, int steps, long minimum, long maximum)
    {
        long stepped = frequency + (steps * pow10(position));
        return clamp(stepped, minimum, maximum);
    }

    /**
     * Replaces the digit at the position with a new value, without carry.
     */
    public static long setDigit(long frequency, int position, int digit)
    {
        long unit = pow10(position);
        int current = getDigit(frequency, position);
        return frequency + ((long)(digit - current) * unit);
    }

    /**
     * Clamps the frequency to the range.
     */
    public static long clamp(long frequency, long minimum, long maximum)
    {
        return Math.max(minimum, Math.min(maximum, frequency));
    }

    /**
     * Parses a typed or pasted frequency.  A bare number is in MHz (e.g. 851.0125) unless it is too large to be MHz
     * for the display, in which case it is read as Hertz (e.g. 851012500).  An optional unit suffix of Hz, kHz, MHz
     * or GHz (any case, with or without a space) overrides that.  Thousands separators (comma, underscore or space)
     * are ignored.
     * @param text to parse
     * @return frequency in Hertz, or null if the text isn't a frequency or has more precision than 1 Hz.
     */
    public static Long parse(String text)
    {
        if(text == null)
        {
            return null;
        }

        String value = text.trim().toLowerCase().replace(",", "").replace("_", "").replace(" ", "");

        if(value.isEmpty())
        {
            return null;
        }

        BigDecimal multiplier = null;

        if(value.endsWith("ghz"))
        {
            multiplier = BigDecimal.valueOf(1_000_000_000L);
            value = value.substring(0, value.length() - 3);
        }
        else if(value.endsWith("mhz"))
        {
            multiplier = BigDecimal.valueOf(1_000_000L);
            value = value.substring(0, value.length() - 3);
        }
        else if(value.endsWith("khz"))
        {
            multiplier = BigDecimal.valueOf(1_000L);
            value = value.substring(0, value.length() - 3);
        }
        else if(value.endsWith("hz"))
        {
            multiplier = BigDecimal.ONE;
            value = value.substring(0, value.length() - 2);
        }

        BigDecimal number;

        try
        {
            number = new BigDecimal(value);
        }
        catch(NumberFormatException nfe)
        {
            return null;
        }

        if(number.signum() < 0)
        {
            return null;
        }

        if(multiplier == null)
        {
            //A whole number too large to be MHz on the display is taken as Hertz
            boolean whole = number.stripTrailingZeros().scale() <= 0;
            multiplier = (whole && number.compareTo(BigDecimal.valueOf(10_000)) >= 0) ? BigDecimal.ONE :
                    BigDecimal.valueOf(1_000_000L);
        }

        BigDecimal hertz = number.multiply(multiplier).stripTrailingZeros();

        if(hertz.scale() > 0)
        {
            return null;
        }

        try
        {
            return hertz.longValueExact();
        }
        catch(ArithmeticException ae)
        {
            return null;
        }
    }

    /**
     * Formats the frequency in MHz with all six fractional digits, e.g. 851.012500
     */
    public static String format(long frequency)
    {
        return BigDecimal.valueOf(frequency, FRACTION_DIGITS).toPlainString();
    }

    /**
     * Ten raised to the power.
     */
    public static long pow10(int power)
    {
        long value = 1;

        for(int x = 0; x < power; x++)
        {
            value *= 10;
        }

        return value;
    }

    /**
     * Calculator style typed entry.  Digits typed before the decimal point fill the MHz digits from the right and
     * digits typed after it fill the fractional digits from the left, so typing 851.0125 shows 851.012500.
     */
    public static class TypedEntry
    {
        private final StringBuilder mIntegerPart = new StringBuilder();
        private final StringBuilder mFractionPart = new StringBuilder();
        private boolean mDecimal = false;

        /**
         * Adds a typed character.
         * @param c digit or decimal point
         * @return true if the character was accepted
         */
        public boolean type(char c)
        {
            if(c == '.' || c == ',')
            {
                if(mDecimal)
                {
                    return false;
                }

                mDecimal = true;
                return true;
            }

            if(c < '0' || c > '9')
            {
                return false;
            }

            if(mDecimal)
            {
                if(mFractionPart.length() >= FRACTION_DIGITS)
                {
                    return false;
                }

                mFractionPart.append(c);
                return true;
            }

            //Ignore leading zeros so they don't use up the MHz digits
            if(mIntegerPart.isEmpty() && c == '0')
            {
                return true;
            }

            if(mIntegerPart.length() >= INTEGER_DIGITS)
            {
                return false;
            }

            mIntegerPart.append(c);
            return true;
        }

        /**
         * Removes the last typed character.
         * @return true if there is still something typed after the removal.
         */
        public boolean backspace()
        {
            if(!mFractionPart.isEmpty())
            {
                mFractionPart.setLength(mFractionPart.length() - 1);
            }
            else if(mDecimal)
            {
                mDecimal = false;
            }
            else if(!mIntegerPart.isEmpty())
            {
                mIntegerPart.setLength(mIntegerPart.length() - 1);
            }

            return !isEmpty();
        }

        /**
         * Indicates if nothing has been typed.
         */
        public boolean isEmpty()
        {
            return mIntegerPart.isEmpty() && mFractionPart.isEmpty() && !mDecimal;
        }

        /**
         * Number of fractional digits typed so far.
         */
        public int getFractionDigitsTyped()
        {
            return mFractionPart.length();
        }

        /**
         * Indicates if the decimal point has been typed.
         */
        public boolean hasDecimal()
        {
            return mDecimal;
        }

        /**
         * Number of MHz digits typed so far.
         */
        public int getIntegerDigitsTyped()
        {
            return mIntegerPart.length();
        }

        /**
         * Frequency for what has been typed so far.
         */
        public long getFrequency()
        {
            long mhz = mIntegerPart.isEmpty() ? 0 : Long.parseLong(mIntegerPart.toString());
            long fraction = 0;

            if(!mFractionPart.isEmpty())
            {
                fraction = Long.parseLong(mFractionPart.toString()) * pow10(FRACTION_DIGITS - mFractionPart.length());
            }

            return (mhz * 1_000_000L) + fraction;
        }

        /**
         * Indicates if the digit at the position has been typed, as opposed to an implied trailing zero.
         */
        public boolean isTyped(int position)
        {
            if(position >= FRACTION_DIGITS)
            {
                return position - FRACTION_DIGITS < mIntegerPart.length();
            }

            return (FRACTION_DIGITS - 1 - position) < mFractionPart.length();
        }

        /**
         * Clears the typed entry.
         */
        public void clear()
        {
            mIntegerPart.setLength(0);
            mFractionPart.setLength(0);
            mDecimal = false;
        }
    }
}
