/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.source;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.function.UnaryOperator;

/**
 * Control for editing frequency values in MHz that allows access/setting value in Hz
 *
 * Allows values in range: 1.000000 to 9999.999999 MHz
 */
public class FrequencyField extends TextField
{
    private final static Logger mLog = LoggerFactory.getLogger(FrequencyField.class);
    private DecimalFormat mDecimalFormat = new DecimalFormat("0.000000");
    private static final double CONVERT_HZ_TO_MHZ = 0.000001;
    private static final double CONVERT_MHZ_TO_HZ = 1000000;
    private static final String INTEGER_REGEX = "[1-9]\\d{0,3}";
    private static final String FRACTIONAL_REGEX = "[1-9]\\d{0,3}\\.\\d{0,6}";

    /**
     * Constructs an instance
     */
    public FrequencyField()
    {
        UnaryOperator<TextFormatter.Change> filter = change ->
        {
            String update = change.getControlNewText();

            if(update.matches(INTEGER_REGEX) || update.matches(FRACTIONAL_REGEX) || update.isEmpty())
            {
                return change;
            }

            return null;
        };

        TextFormatter<Double> formatter = new TextFormatter<Double>(new DoubleStringConverter(), null, filter);
        setTextFormatter(formatter);
    }

    /**
     * Sets the frequency for this control
     * @param frequency in range 0 <> 9,999,999,999 Hz</>
     */
    public void set(long frequency)
    {
        if(frequency < 0 || frequency > 9999999999l)
        {
            throw new IllegalArgumentException("Frequency [" + frequency + "] must be in range 0 <> 9,999,999,999");
        }

        if(frequency == 0)
        {
            setText("");
        }
        else
        {
            setText(mDecimalFormat.format(frequency * CONVERT_HZ_TO_MHZ));
        }
    }

    /**
     * Retrieves the frequency value from this control
     * @return frequency in Hz
     */
    public long get()
    {
        String raw = getText();

        if(raw != null)
        {
            try
            {
                double mhz = Double.parseDouble(raw);
                return (long)(mhz * CONVERT_MHZ_TO_HZ);
            }
            catch(Exception e)
            {
                //Ignore ... we'll return 0 for this value
            }
        }

        return 0;
    }
}
