/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.dsp.symbol;

public enum Dibit
{
    D01_PLUS_3(false, true, 1, 4, 1, (float)((Math.PI / 4) * 3)),
    D00_PLUS_1(false, false, 0, 0, 0, (float)(Math.PI / 4)),
    D10_MINUS_1(true, false, 2, 8, 2, (float)(Math.PI / -4)),
    D11_MINUS_3(true, true, 3, 12, 3, (float)((Math.PI / 4) * -3));

    private boolean mBit1;
    private boolean mBit2;
    private int mLowValue;
    private int mHighValue;
    private int mValue;
    private float mPhase;

    /**
     * Dibit constructor.
     * @param bit1 boolean value
     * @param bit2 boolean value
     * @param lowValue integer value of the low order bit
     * @param highValue integer value of the high order bit
     * @param value integer value combined of both bits.
     * @param phase optimal phase value for this symbol when transmitted.
     */
    Dibit(boolean bit1, boolean bit2, int lowValue, int highValue, int value, float phase)
    {
        mBit1 = bit1;
        mBit2 = bit2;
        mLowValue = lowValue;
        mHighValue = highValue;
        mValue = value;
        mPhase = phase;
    }

    /**
     * Binary value of the symbol
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Ideal phase angle when this symbol is transmitted.
     * @return ideal phase angle in radians.
     */
    public float getIdealPhase()
    {
        return mPhase;
    }

    /**
     * Allowable transition state 1 for trellis coded modulation.  Indicates
     * the dibit that differs from this dibit in the LSB bit position.
     */
    public Dibit getAllowableTransition1()
    {
        switch(this)
        {
            case D00_PLUS_1:
                return D01_PLUS_3;
            case D01_PLUS_3:
                return D00_PLUS_1;
            case D10_MINUS_1:
                return D11_MINUS_3;
            case D11_MINUS_3:
                return D10_MINUS_1;
            default:
                /* We'll never get here */
                return D00_PLUS_1;
        }
    }

    /**
     * Allowable transition state 2 for trellis coded modulation.  Indicates
     * the dibit that differs from this dibit in the MSB bit position.
     */
    public Dibit getAllowableTransition2()
    {
        switch(this)
        {
            case D00_PLUS_1:
                return D10_MINUS_1;
            case D01_PLUS_3:
                return D11_MINUS_3;
            case D10_MINUS_1:
                return D00_PLUS_1;
            case D11_MINUS_3:
                return D01_PLUS_3;
            default:
                /* We'll never get here */
                return D00_PLUS_1;
        }
    }

    /**
     * Indicates if the trellis coded modulation dibit is allowed to transition
     * to the dibit argument.
     */
    public boolean isAllowableTransition(Dibit dibit)
    {
        return dibit == getAllowableTransition1() ||
            dibit == getAllowableTransition2();
    }

    public boolean getBit1()
    {
        return mBit1;
    }

    public boolean getBit2()
    {
        return mBit2;
    }

    public int getLowValue()
    {
        return mLowValue;
    }

    public int getHighValue()
    {
        return mHighValue;
    }

    public static Dibit inverted(Dibit symbol)
    {
        switch(symbol)
        {
            case D10_MINUS_1:
                return D11_MINUS_3;
            case D11_MINUS_3:
                return D01_PLUS_3;
            case D00_PLUS_1:
                return D10_MINUS_1;
            case D01_PLUS_3:
            default:
                return D00_PLUS_1;
        }
    }

    /**
     * Parses a dibit from the byte value at the specified index
     * @param value to parse
     * @param index index of dibit, 0 - 3, where 0 is the most significant dibit and 3 is the least.
     * @return dibit parsed from the byte.
     */
    public static Dibit parse(byte value, int index)
    {
        switch(value >> ((3 - index) * 2) & 0x3)
        {
            case 0:
                return Dibit.D00_PLUS_1;
            case 1:
                return Dibit.D01_PLUS_3;
            case 2:
                return Dibit.D10_MINUS_1;
            case 3:
                return Dibit.D11_MINUS_3;
        }

        return Dibit.D00_PLUS_1;
    }

    public static enum Rotation {PLUS90, MINUS90, INVERT}

    /**
     * Returns the value of this dibit if the symbol were rotated in the direction indicated.
     *
     * This can be used for calculating phase rotation and phase inversion pattern derivatives from a normal sync pattern
     * @param rotation to apply to this dibit
     * @return rotated version of this dibit
     */
    public Dibit rotate(Rotation rotation)
    {
        switch(this)
        {
            case D00_PLUS_1:
                switch(rotation)
                {
                    case PLUS90:
                        return Dibit.D01_PLUS_3;
                    case MINUS90:
                        return Dibit.D10_MINUS_1;
                    case INVERT:
                        return Dibit.D11_MINUS_3;
                }
                break;
            case D01_PLUS_3:
                switch(rotation)
                {
                    case PLUS90:
                        return Dibit.D11_MINUS_3;
                    case MINUS90:
                        return Dibit.D00_PLUS_1;
                    case INVERT:
                        return Dibit.D10_MINUS_1;
                }
                break;
            case D10_MINUS_1:
                switch(rotation)
                {
                    case PLUS90:
                        return Dibit.D00_PLUS_1;
                    case MINUS90:
                        return Dibit.D11_MINUS_3;
                    case INVERT:
                        return Dibit.D01_PLUS_3;
                }
                break;
            case D11_MINUS_3:
                switch(rotation)
                {
                    case PLUS90:
                        return Dibit.D10_MINUS_1;
                    case MINUS90:
                        return Dibit.D01_PLUS_3;
                    case INVERT:
                        return Dibit.D00_PLUS_1;
                }
        }

        //We should never reach this point
        return Dibit.D00_PLUS_1;
    }

    /**
     * Returns the dibit that corresponds to the value
     * @param value 0-3
     * @return representative dibit or Dibit.00 if the value is outside of the range 0-3
     */
    public static Dibit fromValue(int value)
    {
        switch(value)
        {
            case 0:
            default:
                return Dibit.D00_PLUS_1;
            case 1:
                return Dibit.D01_PLUS_3;
            case 2:
                return Dibit.D10_MINUS_1;
            case 3:
                return Dibit.D11_MINUS_3;
        }
    }

    public static void main(String[] args)
    {
        byte a = (byte)0x1B;

        Dibit d1 = Dibit.parse(a, 0);
        Dibit d2 = Dibit.parse(a, 1);
        Dibit d3 = Dibit.parse(a, 2);
        Dibit d4 = Dibit.parse(a, 3);

        System.out.println("1: " + d1 + " 2:" + d2 + " 3:" + d3 + " 4:" + d4);
    }
}
