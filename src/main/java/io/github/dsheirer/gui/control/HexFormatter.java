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

/**
 * Text formatter for integer values displayed and edited as hexadecimal values that constrains values to
 * specified minimum and maximum valid values.
 */
public class HexFormatter extends TextFormatter<Integer>
{
    private static final Logger mLog = LoggerFactory.getLogger(HexFormatter.class);

    /**
     * Constructs an instance
     * @param minimum allowed value
     * @param maximum allowed value
     */
    public HexFormatter(int minimum, int maximum)
    {
        super(new HexIntegerStringConverter(), null, new HexFilter(minimum, maximum));
    }

    /**
     * Formatted text change filter that only allows hexadecimal characters where the converted decimal value
     * is also constrained within minimum and maximum valid values.
     */
    public static class HexFilter implements UnaryOperator<TextFormatter.Change>
    {
        private String HEXADECIMAL_REGEX = "[0-9A-Fa-f].*";
        private int mMinimum;
        private int mMaximum;

        public HexFilter(int minimum, int maximum)
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
            //Only validate if the user added text to the control.  Otherwise, allow it to go through
            if(change.getText() != null)
            {
                String updatedText = change.getControlNewText();

                if(updatedText == null || updatedText.isEmpty())
                {
                    return change;
                }

                if(!updatedText.matches(HEXADECIMAL_REGEX) ||
                   !isValid(HexIntegerStringConverter.getValue(change.getControlNewText())))
                {
                    return null;
                }
                else
                {
                    //Force all hex letters to uppercase
                    if(change.getText().matches("[a-f].*"))
                    {
                        change.setText(change.getText().toUpperCase());
                    }
                }
            }

            return change;
        }
    }

    /**
     * Hexadecimal integer string converter.  Obtain value as an integer but user edits hexadecimal values.
     */
    public static class HexIntegerStringConverter extends StringConverter<Integer>
    {
        @Override
        public String toString(Integer value)
        {
            // If the specified value is null, return a zero-length String
            if (value == null)
            {
                return "";
            }

            return (Integer.toHexString(value.intValue()).toUpperCase());
        }

        @Override
        public Integer fromString(String value)
        {
            return getValue(value);
        }

        public static Integer getValue(String value)
        {
            // If the specified value is null or zero-length, return null
            if (value == null) {
                return null;
            }

            value = value.trim();

            if (value.length() < 1) {
                return null;
            }

            return Integer.valueOf(value, 16);
        }
    }
}
