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
 * Text formatter for integer values displayed and edited as LTR formatted values that constrains values to
 * specified minimum and maximum valid values.
 */
public class LtrFormatter extends TextFormatter<Integer>
{
    private static final Logger mLog = LoggerFactory.getLogger(LtrFormatter.class);

    /**
     * Constructs an instance
     * @param minimum allowed value
     * @param maximum allowed value
     */
    public LtrFormatter(int minimum, int maximum)
    {
        super(new LtrIntegerStringConverter(), null, new LtrFilter(minimum, maximum));
    }

    /**
     * Formatted text change filter that only allows formatted characters where the converted decimal value
     * is also constrained within minimum and maximum valid values.
     */
    public static class LtrFilter implements UnaryOperator<Change>
    {
        private int mMinimum;
        private int mMaximum;

        public LtrFilter(int minimum, int maximum)
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

            if(LtrIntegerStringConverter.isValid(updatedText))
            {
                return change;
            }

            return null;
        }
    }

    /**
     * LTR format integer string converter.  Obtain value as an integer but user edits formatted values.
     */
    public static class LtrIntegerStringConverter extends StringConverter<Integer>
    {
        private static final Pattern LTR_PATTERN = Pattern.compile("([01]?)(?:-(\\d{0,2})(?:-(\\d{0,3}))?)?");

        private static final int AREA_MASK = 0x2000;
        private static final int LCN_MASK = 0x1F00;
        private static final int TALKGROUP_MASK = 0xFF;

        @Override
        public String toString(Integer value)
        {
            // If the specified value is null, return a zero-length String
            if (value == null)
            {
                return "";
            }

            int area = (value & AREA_MASK) >> 13;
            int lcn = (value & LCN_MASK) >> 8;
            int talkgroup = (value & TALKGROUP_MASK);

            StringBuilder sb = new StringBuilder();
            sb.append(area);
            sb.append("-");
            sb.append(String.format("%02d", lcn));
            sb.append("-");
            sb.append(String.format("%03d", talkgroup));

            return sb.toString();
        }

        @Override
        public Integer fromString(String value)
        {
            if(value == null || value.isEmpty())
            {
                return null;
            }

            Matcher m = LTR_PATTERN.matcher(value);

            if(m.matches())
            {
                int a = 0;
                int b = 0;
                int c = 0;

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

                try
                {
                    c = Integer.parseInt(m.group(3));
                }
                catch(Exception e)
                {
                    //do nothing
                }

                int talkgroup = (a << 13) + (b << 8) + c;

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

            Matcher m = LTR_PATTERN.matcher(value);

            if(m.matches())
            {
                //We don't have to check the first digit - it will only match a 0 or 1
                String part2 = m.group(2);

                if(part2 != null && !part2.isEmpty())
                {
                    try
                    {
                        int value2 = Integer.parseInt(part2);

                        if(value2 < 0 || value2 > 31)
                        {
                            return false;
                        }
                    }
                    catch(Exception e)
                    {
                        //Do nothing
                    }
                }

                String part3 = m.group(3);

                if(part3 != null && !part3.isEmpty())
                {
                    try
                    {
                        int value3 = Integer.parseInt(part3);

                        if(value3 < 0 || value3 > 255)
                        {
                            return false;
                        }
                    }
                    catch(Exception e)
                    {
                        //Do nothing
                    }
                }

                //if part 2 and/or part 3 is null or empty then valid = true
                return true;
            }

            return false;
        }
    }
}
