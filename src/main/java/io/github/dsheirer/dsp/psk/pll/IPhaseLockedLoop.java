/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.psk.pll;

import io.github.dsheirer.sample.complex.Complex;

/**
 * Phase-Locked Loop (PLL) interface
 */
public interface IPhaseLockedLoop
{
    /**
     * Increments the PLL by one sample period
     */
    void increment();

    /**
     * Applies an error value to the PLL.
     *
     * @param error value in the range of -PI <> +PI
     */
    void adjust(double error);

    /**
     * Adjusts the current phase of the loop to account for +/-90 or 180 degree inverted phase locks when detected
     * @param correction to apply to the phase of the loop
     */
    void correctInversion(double correction);

    /**
     * Current vector of the PLL to use in de-spinning the incoming samples.
     * @return complex vector
     */
    Complex getCurrentVector();

    /**
     * Increments the phase rotation and gets the current vector of the PLL to use in de-spinning the incoming samples.
     * @return complex vector
     */
    Complex incrementAndGetCurrentVector();

    /**
     * Reset tracking to 0
     */
    void reset();
}
