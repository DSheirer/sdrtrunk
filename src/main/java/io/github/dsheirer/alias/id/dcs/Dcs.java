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

package io.github.dsheirer.alias.id.dcs;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.module.decode.dcs.DCSCode;

/**
 * Digital Coded Squelch (DCS) tone identifier
 */
public class Dcs extends AliasID implements Comparable<Dcs>
{
    private DCSCode mDCSCode;

    @Override
    public AliasIDType getType()
    {
        return AliasIDType.DCS;
    }

    @Override
    public boolean matches(AliasID id)
    {
        if(isValid() && id instanceof Dcs other)
        {
            return other.isValid() && getDCSCode().equals(other.getDCSCode());
        }

        return false;
    }

    /**
     * DCS code
     * @return DCS code or null.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "code")
    public DCSCode getDCSCode()
    {
        return mDCSCode;
    }

    /**
     * Sets the DCS code value.
     * @param dcsCode to set
     */
    public void setDCSCode(DCSCode dcsCode)
    {
        mDCSCode = dcsCode;
        updateValueProperty();
    }

    @Override
    public boolean isValid()
    {
        return mDCSCode != null;
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
            return getDCSCode().toString();
        }

        return "DCS-Invalid - No Tone Selected";
    }

    @Override
    public int compareTo(Dcs o)
    {
        if(isValid())
        {
            if(o.isValid())
            {
                return getDCSCode().compareTo(o.getDCSCode());
            }
            else
            {
                return 1;
            }
        }

        return -1;
    }
}
