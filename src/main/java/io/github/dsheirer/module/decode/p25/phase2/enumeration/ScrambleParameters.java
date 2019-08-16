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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * APCO25 Linear Feedback Shift Register (LFSR) seed parameters.
 */
public class ScrambleParameters
{
    private int mWacn;
    private int mSystem;
    private int mNac;

    public ScrambleParameters()
    {
        //no-arg constructor for jackson deserialization
    }

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
    @JacksonXmlProperty(isAttribute = true, localName = "wacn")
    public int getWACN()
    {
        return mWacn;
    }

    public void setWacn(int wacn)
    {
        mWacn = wacn;
    }

    /**
     * System
     * @return system
     */
    @JacksonXmlProperty(isAttribute = true, localName = "system")
    public int getSystem()
    {
        return mSystem;
    }

    public void setSystem(int system)
    {
        mSystem = system;
    }

    /**
     * NAC
     * @return nac
     */
    @JacksonXmlProperty(isAttribute = true, localName = "nac")
    public int getNAC()
    {
        return mNac;
    }

    public void setNAC(int nac)
    {
        mNac = nac;
    }

    /**
     * Creates a copy of this instance.
     */
    public ScrambleParameters copy()
    {
        return new ScrambleParameters(mWacn, mSystem, mNac);
    }

    @Override
    public String toString()
    {
        return "SCRAMBLE PARAMETERS WACN:" + mWacn + " SYSTEM:" + mSystem + " NAC:" + mNac;
    }
}
