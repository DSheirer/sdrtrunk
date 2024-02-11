/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import java.util.function.UnaryOperator;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.LongStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text formatter for long values that constrains values to specified minimum and maximum valid values.
 */
public class LongFormatter extends TextFormatter<Long>
{
    private static final Logger mLog = LoggerFactory.getLogger(LongFormatter.class);

    /**
     * Constructs an instance
     * @param minimum allowed value
     * @param maximum allowed value
     */
    public LongFormatter(int minimum, int maximum)
    {
        super(new LongStringConverter(), null, new LongFilter(minimum, maximum));
    }

    /**
     * Formatted text change filter that only allows hexadecimal characters where the converted decimal value
     * is also constrained within minimum and maximum valid values.
     */
    public static class LongFilter implements UnaryOperator<Change>
    {
        private String DECIMAL_REGEX = "\\-?[0-9].*";
        private int mMinimum;
        private int mMaximum;

        /**
         * Constructs an instance
         * @param minimum value
         * @param maximum value
         */
        public LongFilter(int minimum, int maximum)
        {
            mMinimum = minimum;
            mMaximum = maximum;
        }

        /**
         * Indicates if the value argument is parsable as an integer, or is empty or null.
         */
        private boolean isValid(String value)
        {
            if(value == null || value.isEmpty())
            {
                return true;
            }

            try
            {
                long parsed = Long.parseLong(value);
                return mMinimum <= parsed && parsed <= mMaximum;
            }
            catch(Exception e)
            {
                //no-op
            }

            return false;
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

                if(!updatedText.matches(DECIMAL_REGEX) || !isValid(updatedText))
                {
                    return null;
                }
            }

            return change;
        }
    }
}
