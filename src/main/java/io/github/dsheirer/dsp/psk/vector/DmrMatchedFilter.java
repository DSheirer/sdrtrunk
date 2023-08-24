/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.vector;

import java.text.DecimalFormat;

public class DmrMatchedFilter
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");

    public static void main(String[] args)
    {
        for(int x = 0; x <= 3000; x += 20)
        {
            if(x <= 1920)
            {
                System.out.println(x + "," + DECIMAL_FORMAT.format(1.0));
            }
            else if(x <= 2880)
            {
                double value = Math.abs(Math.cos(Math.PI * x / 1920.0));
                System.out.println(x + "," + DECIMAL_FORMAT.format(value));
            }
            else
            {
                System.out.println(x + "," + DECIMAL_FORMAT.format(0));
            }
        }
    }
}
