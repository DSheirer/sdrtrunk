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

package io.github.dsheirer.module.decode.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.dcs.DCSCode;

/**
 * Channel-level tone/code filter configuration. Supports CTCSS, DCS, and P25 NAC filtering.
 * Multiple filters can be configured per channel. Audio is only passed when at least one
 * configured filter matches the received signal.
 */
public class ChannelToneFilter
{
    /**
     * Types of tone/code squelch supported
     */
    public enum ToneType
    {
        CTCSS("CTCSS"),
        DCS("DCS"),
        NAC("NAC");

        private final String mLabel;

        ToneType(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    private ToneType mToneType = ToneType.CTCSS;
    private String mValue = "";
    private String mLabel = "";

    /**
     * Default constructor for Jackson XML deserialization
     */
    public ChannelToneFilter()
    {
    }

    /**
     * Constructs an instance
     * @param toneType of this filter (CTCSS, DCS, or NAC)
     * @param value the tone/code value as a string
     * @param label user-friendly label for this filter (e.g., "Chelsea PD")
     */
    public ChannelToneFilter(ToneType toneType, String value, String label)
    {
        mToneType = toneType;
        mValue = value;
        mLabel = label;
    }

    /**
     * Tone type (CTCSS, DCS, or NAC)
     */
    @JacksonXmlProperty(isAttribute = true, localName = "toneType")
    public ToneType getToneType()
    {
        return mToneType;
    }

    public void setToneType(ToneType toneType)
    {
        mToneType = toneType;
    }

    /**
     * Value for the tone/code filter.
     * For CTCSS: the CTCSSCode enum name (e.g. "TONE_1B" for 107.2 Hz)
     * For DCS: the DCSCode enum name (e.g. "D023N")
     * For NAC: integer value as string (e.g. "512")
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
     * User-friendly label (e.g., "Chelsea PD", "Medford FD")
     */
    @JacksonXmlProperty(isAttribute = true, localName = "label")
    public String getLabel()
    {
        return mLabel;
    }

    public void setLabel(String label)
    {
        mLabel = label;
    }

    /**
     * Returns the CTCSS code for this filter, or null if not a CTCSS filter or value is invalid.
     */
    @JsonIgnore
    public CTCSSCode getCTCSSCode()
    {
        if(mToneType == ToneType.CTCSS && mValue != null && !mValue.isEmpty())
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
        if(mToneType == ToneType.DCS && mValue != null && !mValue.isEmpty())
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
     * Returns the NAC value for this filter, or -1 if not a NAC filter or value is invalid.
     */
    @JsonIgnore
    public int getNacValue()
    {
        if(mToneType == ToneType.NAC && mValue != null && !mValue.isEmpty())
        {
            try
            {
                int nac = Integer.parseInt(mValue);
                if(nac >= 0 && nac <= 4095)
                {
                    return nac;
                }
            }
            catch(NumberFormatException e)
            {
                // Try hex
                try
                {
                    String hex = mValue.startsWith("0x") ? mValue.substring(2) : mValue;
                    int nac = Integer.parseInt(hex, 16);
                    if(nac >= 0 && nac <= 4095)
                    {
                        return nac;
                    }
                }
                catch(NumberFormatException nfe)
                {
                    // Invalid
                }
            }
        }
        return -1;
    }

    /**
     * Indicates if this filter configuration is valid
     */
    @JsonIgnore
    public boolean isValid()
    {
        return switch(mToneType)
        {
            case CTCSS -> getCTCSSCode() != null && getCTCSSCode() != CTCSSCode.UNKNOWN;
            case DCS -> getDCSCode() != null;
            case NAC -> getNacValue() >= 0;
        };
    }

    /**
     * Display string for this filter
     */
    @JsonIgnore
    public String getDisplayString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mToneType).append(": ");

        switch(mToneType)
        {
            case CTCSS:
                CTCSSCode ctcss = getCTCSSCode();
                sb.append(ctcss != null ? ctcss.getDisplayString() : mValue);
                break;
            case DCS:
                DCSCode dcs = getDCSCode();
                sb.append(dcs != null ? dcs.toString() : mValue);
                break;
            case NAC:
                int nac = getNacValue();
                if(nac >= 0)
                {
                    sb.append(nac).append(" (x").append(String.format("%03X", nac)).append(")");
                }
                else
                {
                    sb.append(mValue);
                }
                break;
        }

        if(mLabel != null && !mLabel.isEmpty())
        {
            sb.append(" — ").append(mLabel);
        }

        return sb.toString();
    }

    @Override
    public String toString()
    {
        return getDisplayString();
    }
}
