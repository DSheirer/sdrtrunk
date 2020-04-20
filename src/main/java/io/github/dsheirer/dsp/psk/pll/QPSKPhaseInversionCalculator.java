/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.dsp.psk.pll;

import io.github.dsheirer.dsp.symbol.Dibit;

/**
 * Utility for calculating phase inverted sync pattern derivatives for QPSK constellations
 */
public class QPSKPhaseInversionCalculator
{
    public static void main(String[] args)
    {

        //DMR sync patterns
//        String[] syncs = {"755FD7DF75F7", "DFF57D75DF5D", "7F7D5DD57DFD", "D5D7F77FD757", "77D55F7DFD77",
//            "5D577F7757FF", "F7FDD5DDFD55", "7DFFD5F55D5F", "D7557F5FF7F5", "DD7FF5D757DD"};

        //P25 Phase 2 sync pattern
        String[] syncs = {"575D57F7FF"};

        //P25 Phase 1 sync pattern
//        String[] syncs = {"5575F5FF77FF"};


        for(String sync: syncs)
        {
            StringBuilder sbNorm = new StringBuilder();
            StringBuilder sbP90 = new StringBuilder();
            StringBuilder sbM90 = new StringBuilder();
            StringBuilder sb180 = new StringBuilder();

            for(char letter: sync.toCharArray())
            {
                int value;

                if(Character.isAlphabetic(letter))
                {
                    value = letter - 55;
                }
                else
                {
                    value = letter - 48;
                }

                sbNorm.append(letter);
                sbP90.append(rotate(value, Dibit.Rotation.PLUS90));
                sbM90.append(rotate(value, Dibit.Rotation.MINUS90));
                sb180.append(rotate(value, Dibit.Rotation.INVERT));
            }

            System.out.println("NORMAL: " + sync);
            System.out.println("   +90: " + sbP90);
            System.out.println("   -90: " + sbM90);
            System.out.println("   180: " + sb180);
        }
    }

    /**
     * Converts the hexadecimal value to an integer, decomposes it into high and low order dibits, rotates the dibits
     * in the specified direction, and recombines the rotated dibits into a 4-bit value, returned as a hex character.
     * @param value to rotate
     * @param direction direction
     * @return rotated hex character
     */
    public static String rotate(int value, Dibit.Rotation direction)
    {
        try
        {
            Dibit dibitHigh = getHighDibit(value);
            Dibit dibitLow = getLowDibit(value);
            return getHex(dibitHigh.rotate(direction), dibitLow.rotate(direction));
        }
        catch(Exception e)
        {
            //ignore
        }

        return "-";
    }

    /**
     * Combines the bit values from the two dibits and converts the integer value to a hex character
     * @param d1 containing the high order bits
     * @param d2 containing the low order bits
     * @return string hex character representing the value of these dibits combined
     */
    public static String getHex(Dibit d1, Dibit d2)
    {
        int value = d1.getHighValue() + d2.getLowValue();
        return Integer.toHexString(value).toUpperCase();
    }

    /**
     * Calculates the dibit that represents the bits in the 0x8 and 0x4 bit positions of the value
     * @param value
     * @return a dibit
     */
    public static Dibit getHighDibit(int value)
    {
        return Dibit.fromValue((value >> 2) & 3);
    }

    /**
     * Calculates the dibit that represents the bits in the 0x2 and 0x1 bit positions of the value
     * @param value
     * @return a dibit
     */
    public static Dibit getLowDibit(int value)
    {
        return Dibit.fromValue(value & 3);
    }
}
