/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */
package io.github.dsheirer.alias.id.ctcss;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;

/** Continuous Tone Coded Squelch (CTCSS) alias identifier. */
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
        return isValid() && id instanceof Ctcss other && other.isValid() &&
            getCTCSSCode().equals(other.getCTCSSCode());
    }

    @JacksonXmlProperty(isAttribute = true, localName = "code")
    public CTCSSCode getCTCSSCode()
    {
        return mCTCSSCode;
    }

    public void setCTCSSCode(CTCSSCode ctcssCode)
    {
        mCTCSSCode = ctcssCode;
        updateValueProperty();
    }

    @Override
    public boolean isValid()
    {
        return mCTCSSCode != null && CTCSSCode.STANDARD_CODES.contains(mCTCSSCode);
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return isValid() ? "CTCSS " + getCTCSSCode().getDisplayString() :
            "CTCSS-Invalid - No Tone Selected";
    }

    @Override
    public int compareTo(Ctcss other)
    {
        if(!isValid())
        {
            return other.isValid() ? -1 : 0;
        }
        return other.isValid() ? getCTCSSCode().compareTo(other.getCTCSSCode()) : 1;
    }
}
