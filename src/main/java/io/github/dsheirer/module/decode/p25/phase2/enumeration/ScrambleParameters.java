/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.enumeration;

/**
 * APCO25 Linear Feedback Shift Register (LFSR) seed parameters.
 */
public class ScrambleParameters
{
    private int mWacn;
    private int mSystem;
    private int mNac;

    /**
     * Constructs a parameters instance
     */
    public ScrambleParameters(int wacn, int system, int nac)
    {
        mWacn = wacn;
        mSystem = system;
        mNac = nac;
    }

    /**
     * WACN
     * @return wacn
     */
    public int getWACN()
    {
        return mWacn;
    }

    /**
     * System
     * @return system
     */
    public int getSystem()
    {
        return mSystem;
    }

    /**
     * NAC
     * @return nac
     */
    public int getNAC()
    {
        return mNac;
    }
}
