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

/**
 * Utility for calculating phase inverted sync pattern derivatives for QPSK constellations
 */
public class QPSKPhaseInversionCalculator
{
    public static String[] LOOKUP_MINUS_90 = {"A","8","B","9","2","0","3","1","E","C","F","D","6","4","7","5"};
    public static String[] LOOKUP_PLUS_90 = {"5","7","4","6","D","F","C","E","1","3","0","2","9","B","8","A"};
    public static String[] LOOKUP_180 = {"F","E","D","C","B","A","9","8","7","6","5","4","3","2","1","0"};

    public static void main(String[] args)
    {
        //DMR sync patterns
        String[] syncs = {"755FD7DF75F7", "DFF57D75DF5D", "7F7D5DD57DFD", "D5D7F77FD757", "77D55F7DFD77",
            "5D577F7757FF", "F7FDD5DDFD55", "7DFFD5F55D5F", "D7557F5FF7F5", "DD7FF5D757DD"};

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
                sbP90.append(LOOKUP_PLUS_90[value]);
                sbM90.append(LOOKUP_MINUS_90[value]);
                sb180.append(LOOKUP_180[value]);
            }

            System.out.println("NORMAL: " + sync);
            System.out.println("   +90: " + sbP90.toString());
            System.out.println("   -90: " + sbM90.toString());
            System.out.println("   180: " + sb180.toString());
        }
    }
}
