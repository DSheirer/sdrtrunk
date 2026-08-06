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

package io.github.dsheirer.alias.id.nac;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

/**
 * P25 Network Access Code (NAC) alias identifier
 */
public class Nac extends AliasID implements Comparable<Nac>
{
    private int mNac;

    /**
     * Default constructor for JAXB
     */
    public Nac()
    {
    }

    /**
     * Constructs with a NAC value
     * @param nac value (0-4095)
     */
    public Nac(int nac)
    {
        mNac = nac;
    }

    @Override
    public AliasIDType getType()
    {
        return AliasIDType.NAC;
    }

    @Override
    public boolean matches(AliasID id)
    {
        if(isValid() && id instanceof Nac other)
        {
            return other.isValid() && getNac() == other.getNac();
        }
        return false;
    }

    /**
     * NAC value
     * @return NAC value (0-4095)
     */
    @JacksonXmlProperty(isAttribute = true, localName = "nac")
    public int getNac()
    {
        return mNac;
    }

    /**
     * Sets the NAC value
     * @param nac value (0-4095)
     */
    public void setNac(int nac)
    {
        mNac = nac;
        updateValueProperty();
    }

    @Override
    public boolean isValid()
    {
        return mNac >= 0 && mNac <= 4095;
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @Override
    public String toString()
    {
        if(isValid())
        {
            return "NAC: " + mNac + " (x" + String.format("%03X", mNac) + ")";
        }
        return "NAC - Invalid";
    }

    @Override
    public int compareTo(Nac o)
    {
        if(isValid())
        {
            if(o.isValid())
            {
                return Integer.compare(getNac(), o.getNac());
            }
            else
            {
                return 1;
            }
        }
        return -1;
    }
}