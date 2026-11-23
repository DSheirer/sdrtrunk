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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

/**
 * Channel access code filter for digital protocols: P25 Network Access Code (NAC), DMR Color Code (CC) or NXDN
 * Radio Access Number (RAN).
 *
 * The filter holds a set of access code values and a mode.  In INCLUDE mode, only traffic carrying one of the listed
 * values is processed.  In EXCLUDE mode, traffic carrying any of the listed values is dropped and all other traffic
 * is processed.  An enabled filter with an empty value set is inactive and passes all traffic.
 *
 * Values are always stored as decimal in the playlist.  The format setting only controls how the values are displayed
 * and entered in the channel editor, so changing the format never changes the filter.
 */
public class AccessCodeFilter
{
    /**
     * Filter mode
     */
    public enum Mode
    {
        INCLUDE("Include"),
        EXCLUDE("Exclude");

        private final String mLabel;

        Mode(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    /**
     * Display and entry format for the filter values
     */
    public enum Format
    {
        HEX("Hex"),
        DECIMAL("Dec");

        private final String mLabel;

        Format(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    private boolean mEnabled = false;
    private Mode mMode = Mode.INCLUDE;
    private Format mFormat = Format.DECIMAL;
    private final TreeSet<Integer> mValues = new TreeSet<>();

    /**
     * Default constructor for Jackson
     */
    public AccessCodeFilter()
    {
    }

    /**
     * Constructs an instance with the specified display format.
     * @param format for displaying and entering values
     */
    public AccessCodeFilter(Format format)
    {
        mFormat = format;
    }

    /**
     * Creates a copy of this filter.
     */
    public AccessCodeFilter copy()
    {
        AccessCodeFilter copy = new AccessCodeFilter(mFormat);
        copy.mEnabled = mEnabled;
        copy.mMode = mMode;
        copy.mValues.addAll(mValues);
        return copy;
    }

    /**
     * Indicates if the filter is enabled by the user.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "enabled")
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Sets the enabled state.
     */
    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    /**
     * Filter mode: include (only the listed values) or exclude (everything except the listed values).
     */
    @JacksonXmlProperty(isAttribute = true, localName = "mode")
    public Mode getMode()
    {
        return mMode;
    }

    /**
     * Sets the filter mode.
     */
    public void setMode(Mode mode)
    {
        mMode = mode != null ? mode : Mode.INCLUDE;
    }

    /**
     * Display and entry format for the values.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "format")
    public Format getFormat()
    {
        return mFormat;
    }

    /**
     * Sets the display and entry format.
     */
    public void setFormat(Format format)
    {
        mFormat = format != null ? format : Format.DECIMAL;
    }

    /**
     * Filter values as a comma-separated list of decimal values, for playlist persistence.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "values")
    public String getValues()
    {
        StringJoiner joiner = new StringJoiner(",");

        for(Integer value: mValues)
        {
            joiner.add(String.valueOf(value));
        }

        return joiner.toString();
    }

    /**
     * Sets the filter values from a comma-separated list of decimal values.  Values that can't be parsed are ignored.
     */
    public void setValues(String values)
    {
        mValues.clear();
        mValues.addAll(parse(values, Format.DECIMAL, Integer.MAX_VALUE).values());
    }

    /**
     * Filter values.
     * @return sorted, unmodifiable set of values.
     */
    @JsonIgnore
    public Set<Integer> getValueSet()
    {
        return Collections.unmodifiableSet(mValues);
    }

    /**
     * Replaces the filter values.
     */
    @JsonIgnore
    public void setValueSet(Collection<Integer> values)
    {
        mValues.clear();

        if(values != null)
        {
            mValues.addAll(values);
        }
    }

    /**
     * Indicates if the filter is enabled and has at least one value, meaning that it will drop some traffic.
     */
    @JsonIgnore
    public boolean isActive()
    {
        return mEnabled && !mValues.isEmpty();
    }

    /**
     * Indicates if traffic with the specified access code value passes the filter.
     * @param value of the access code decoded from the traffic
     * @return true if the traffic should be processed
     */
    public boolean passes(int value)
    {
        if(!isActive())
        {
            return true;
        }

        boolean listed = mValues.contains(value);
        return mMode == Mode.INCLUDE ? listed : !listed;
    }

    /**
     * Formats the filter values for display in the specified format.
     * @param format to use
     * @param hexDigits minimum number of hex digits, zero-padded (e.g. 3 for a P25 NAC)
     * @return comma-separated values
     */
    public String format(Format format, int hexDigits)
    {
        return format(mValues, format, hexDigits);
    }

    /**
     * Formats the values for display in the specified format.
     * @param values to format
     * @param format to use
     * @param hexDigits minimum number of hex digits, zero-padded
     * @return comma-separated values
     */
    public static String format(Collection<Integer> values, Format format, int hexDigits)
    {
        StringJoiner joiner = new StringJoiner(", ");
        String hexPattern = "%0" + Math.max(1, hexDigits) + "X";

        for(Integer value: values)
        {
            joiner.add(format == Format.HEX ? String.format(hexPattern, value) : String.valueOf(value));
        }

        return joiner.toString();
    }

    /**
     * Parses user-entered filter values.  Values can be separated by commas, semicolons or whitespace.  Hex values may
     * optionally carry a '$' or '0x' prefix.
     * @param text to parse
     * @param format of the entered values
     * @param maximum allowed value, inclusive
     * @return parse result with the valid values and any tokens that could not be parsed or are out of range.
     */
    public static ParseResult parse(String text, Format format, int maximum)
    {
        TreeSet<Integer> values = new TreeSet<>();
        List<String> invalid = new ArrayList<>();

        if(text != null)
        {
            for(String token: text.trim().split("[,;\\s]+"))
            {
                if(token.isEmpty())
                {
                    continue;
                }

                String digits = token;

                if(format == Format.HEX)
                {
                    if(digits.startsWith("$"))
                    {
                        digits = digits.substring(1);
                    }
                    else if(digits.startsWith("0x") || digits.startsWith("0X"))
                    {
                        digits = digits.substring(2);
                    }
                }

                try
                {
                    int value = Integer.parseInt(digits, format == Format.HEX ? 16 : 10);

                    if(value >= 0 && value <= maximum)
                    {
                        values.add(value);
                    }
                    else
                    {
                        invalid.add(token);
                    }
                }
                catch(NumberFormatException nfe)
                {
                    invalid.add(token);
                }
            }
        }

        return new ParseResult(new ArrayList<>(values), invalid);
    }

    /**
     * Result of parsing user-entered filter values.
     * @param values that were successfully parsed, sorted and de-duplicated.
     * @param invalid tokens that could not be parsed or that are out of range.
     */
    public record ParseResult(List<Integer> values, List<String> invalid)
    {
        public boolean isValid()
        {
            return invalid.isEmpty();
        }
    }

    @Override
    public String toString()
    {
        if(!isActive())
        {
            return "Access Code Filter: disabled";
        }

        return "Access Code Filter: " + mMode + " [" + format(mFormat, 1) + "]";
    }
}
