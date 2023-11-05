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

package io.github.dsheirer.gui.control;

import java.util.function.UnaryOperator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

/**
 * Text field control for integer values.
 */
public class IntegerTextField extends TextField
{
    private static final String REGEX = "[1-9][0-9]*";

    /**
     * Constructs an instance
     */
    public IntegerTextField()
    {
        UnaryOperator<TextFormatter.Change> filter = change ->
        {
            String newText = change.getControlNewText();

            if(newText.isEmpty() || newText.matches(REGEX))
            {
                return change;
            }

            return null;
        };

        setTextFormatter(new TextFormatter<Integer>(new IntegerStringConverter(), null, filter));
    }

    /**
     * Current integer value of the control, or null if the value is not parsable as an integer
     */
    public Integer get()
    {
        String rawText = getText();

        try
        {
            return Integer.parseInt(rawText);
        }
        catch(Exception pe)
        {
            //Do nothing ... we couldn't parse the value
        }

        return 0;
    }

    /**
     * Sets the integer value in the control
     */
    public void set(int value)
    {
        setText(String.valueOf(value));
    }
}
