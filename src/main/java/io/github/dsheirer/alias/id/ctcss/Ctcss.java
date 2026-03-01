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

package io.github.dsheirer.alias.id.ctcss;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;

/**
 * Continuous Tone-Coded Squelch System (CTCSS) tone identifier for alias matching
 */
public class Ctcss extends AliasID implements Comparable<Ctcss>
{
    private CTCSSCode mCTCSSCode;

    @Override
    public AliasIDType getType()
    {
        return AliasIDType.CTCSS;
    }

    @Override
    public boolean matches(AliasID id)
    {
        if(isValid() && id instanceof Ctcss other)
        {
            return other.isValid() && getCTCSSCode().equals(other.getCTCSSCode());
        }

        return false;
    }

    /**
     * CTCSS code
     * @return CTCSS code or null.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "code")
    public CTCSSCode getCTCSSCode()
    {
        return mCTCSSCode;
    }

    /**
     * Sets the CTCSS code value.
     * @param ctcssCode to set
     */
    public void setCTCSSCode(CTCSSCode ctcssCode)
    {
        mCTCSSCode = ctcssCode;
        updateValueProperty();
    }

    @Override
    public boolean isValid()
    {
        return mCTCSSCode != null;
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
            return getCTCSSCode().toString();
        }

        return "CTCSS-Invalid - No Tone Selected";
    }

    @Override
    public int compareTo(Ctcss o)
    {
        if(isValid())
        {
            if(o.isValid())
            {
                return getCTCSSCode().compareTo(o.getCTCSSCode());
            }
            else
            {
                return 1;
            }
        }

        return -1;
    }
}