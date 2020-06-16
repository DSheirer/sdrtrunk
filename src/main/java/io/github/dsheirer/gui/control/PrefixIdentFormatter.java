/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.control;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text formatter for talkgroups that use prefix and ident format that constrains values to specified minimum and
 * maximum valid values.
 */
public class PrefixIdentFormatter extends TextFormatter<Integer>
{
    private static final Logger mLog = LoggerFactory.getLogger(PrefixIdentFormatter.class);

    /**
     * Constructs an instance
     * @param minimum allowed value
     * @param maximum allowed value
     */
    public PrefixIdentFormatter(int minimum, int maximum)
    {
        super(new PrefixIdentIntegerStringConverter(), null, new PrefixIdentFilter(minimum, maximum));
    }

    /**
     * Formatted text change filter that only allows formatted characters where the converted decimal value
     * is also constrained within minimum and maximum valid values.
     */
    public static class PrefixIdentFilter implements UnaryOperator<Change>
    {
        private int mMinimum;
        private int mMaximum;

        public PrefixIdentFilter(int minimum, int maximum)
        {
            mMinimum = minimum;
            mMaximum = maximum;
        }

        private boolean isValid(Integer value)
        {
            return value != null && mMinimum <= value && value <= mMaximum;
        }

        @Override
        public Change apply(Change change)
        {
            String updatedText = change.getControlNewText();

            if(updatedText == null || updatedText.isEmpty())
            {
                return change;
            }

            if(PrefixIdentIntegerStringConverter.isValid(updatedText))
            {
                return change;
            }

            return null;
        }
    }

    /**
     * Prefix-Ident integer string converter.  Obtain value as an integer but user edits formatted values.
     */
    public static class PrefixIdentIntegerStringConverter extends StringConverter<Integer>
    {
        private static final Pattern PREFIX_IDENT_PATTERN = Pattern.compile("(\\d{0,3})(?:-(\\d{0,4}))?");

        private static final int PREFIX_MASK = 0xFE000;
        private static final int IDENT_MASK = 0x1FFF;

        @Override
        public String toString(Integer value)
        {
            // If the specified value is null, return a zero-length String
            if (value == null)
            {
                return "";
            }

            int prefix = (value & PREFIX_MASK) >> 13;
            int ident = (value & IDENT_MASK);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%03d", prefix));
            sb.append("-");
            sb.append(String.format("%04d", ident));

            return sb.toString();
        }

        @Override
        public Integer fromString(String value)
        {
            if(value == null || value.isEmpty())
            {
                return null;
            }

            Matcher m = PREFIX_IDENT_PATTERN.matcher(value);

            if(m.matches())
            {
                int a = 0;
                int b = 0;

                try
                {
                    a = Integer.parseInt(m.group(1));
                }
                catch(Exception e)
                {
                    //do nothing
                }

                try
                {
                    b = Integer.parseInt(m.group(2));
                }
                catch(Exception e)
                {
                    //do nothing
                }

                int talkgroup = (a << 13) + b;

                return talkgroup;
            }

            return null;
        }

        public static boolean isValid(String value)
        {
            if(value == null || value.isEmpty())
            {
                return true;
            }

            Matcher m = PREFIX_IDENT_PATTERN.matcher(value);

            if(m.matches())
            {
                //We don't have to check the first digit - it will only match a 0 or 1
                String rawPrefix = m.group(1);

                if(rawPrefix != null && !rawPrefix.isEmpty())
                {
                    try
                    {
                        int prefix = Integer.parseInt(rawPrefix);

                        if(prefix < 0 || prefix > 127)
                        {
                            return false;
                        }
                    }
                    catch(Exception e)
                    {
                        //Do nothing
                    }
                }

                String rawIdent = m.group(2);

                if(rawIdent != null && !rawIdent.isEmpty())
                {
                    try
                    {
                        int ident = Integer.parseInt(rawIdent);

                        if(ident < 0 || ident > 8191)
                        {
                            return false;
                        }
                    }
                    catch(Exception e)
                    {
                        //Do nothing
                    }
                }

                //if prefix and/or ident is null or empty then valid = true
                return true;
            }

            return false;
        }
    }
}
