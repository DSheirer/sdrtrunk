/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.squelchDecoder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.squelchDecoder.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.squelchDecoder.dcs.DCSCode;

import java.util.EnumSet;
import java.util.Set;


/**
 * Channel-level tone/code filter configuration. Supports CTCSS & DCS.
 * Audio is only passed when at least one
 * configured filter matches the received signal.
 */
public class squelchDecoderConfig
{

    public enum SquelchType
    {
        NONE("None"),
        CTCSS("CTCSS"),
        DCS("DCS");

        public static final Set<SquelchType> SQUELCH_TYPE ;
        static
        {
            EnumSet<SquelchType> types = EnumSet.allOf(SquelchType.class);
            SQUELCH_TYPE = java.util.Collections.unmodifiableSet(types);
        }
        private final String mLabel;

        SquelchType(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    private SquelchType mSquelchType = SquelchType.NONE;
    private String mValue = "";

    /**
     * Default constructor for Jackson XML deserialization
     */
    public squelchDecoderConfig()
    {
    }

    /**
     * Constructs an instance
     * @param squelchType of this filter (CTCSS, DCS)
     * @param value the tone/code value as a string
     */
    public squelchDecoderConfig(SquelchType squelchType, String value)
    {
        mSquelchType = squelchType;
        mValue = value;
    }

    /**
     * Squelch filter type (None, CTCSS, DCS)
     */
    @JacksonXmlProperty(isAttribute = true, localName = "squelchType")
    public SquelchType getSquelchType()
    {
        return mSquelchType;
    }

    public void setSquelchType(SquelchType squelchType)
    {
        mSquelchType = squelchType;
    }

    /**
     * Value for the tone/code filter.
     * For CTCSS: the CTCSSCode enum name is the frequency (e.g. "179.9")
     * For DCS: the DCSCode enum name (e.g. "D023N")
     */
    @JacksonXmlProperty(isAttribute = true, localName = "value")
    public String getValue()
    {
        return mValue;
    }

    public void setValue(String value)
    {
        mValue = value;
    }

    /**
     * Returns the CTCSS code for this filter, or null if not a CTCSS filter or value is invalid.
     */
    @JsonIgnore
    public CTCSSCode getCTCSSCode()
    {
        if(mSquelchType == SquelchType.CTCSS && mValue != null && !mValue.isEmpty())
        {
            try
            {
                return CTCSSCode.valueOf(mValue);
            }
            catch(IllegalArgumentException e)
            {
                // Try to parse as frequency
                try
                {
                    float freq = Float.parseFloat(mValue);
                    return CTCSSCode.fromFrequency(freq);
                }
                catch(NumberFormatException nfe)
                {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns the DCS code for this filter, or null if not a DCS filter or value is invalid.
     */
    @JsonIgnore
    public DCSCode getDCSCode()
    {
        if(mSquelchType == SquelchType.DCS && mValue != null && !mValue.isEmpty())
        {
            try
            {
                return DCSCode.valueOf(mValue);
            }
            catch(IllegalArgumentException e)
            {
                return null;
            }
        }
        return null;
    }

    /**
     * Indicates if this filter configuration is valid
     */
    @JsonIgnore
    public boolean isValid()
    {
        return switch(mSquelchType)
        {
            case CTCSS -> getCTCSSCode() != null && getCTCSSCode() != CTCSSCode.UNKNOWNH;
            case DCS -> getDCSCode() != null;
            case NONE -> false;
        };
    }

    /**
     * Display string for this filter
     */
    @JsonIgnore
    public String getDisplayString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mSquelchType).append(": ");

        switch(mSquelchType)
        {
            case CTCSS:
                CTCSSCode ctcss = getCTCSSCode();
                sb.append(ctcss != null ? ctcss.getDisplayString() : mValue);
                break;
            case DCS:
                DCSCode dcs = getDCSCode();
                sb.append(dcs != null ? dcs.toString() : mValue);
                break;
            case NONE:
                sb.append("No squelch filter configured.");
                break;
        }
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return getDisplayString();
    }
}
