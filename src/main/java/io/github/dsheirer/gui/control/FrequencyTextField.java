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

import java.awt.EventQueue;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * Swing text field control for entering frequency (MHz) values.
 */
public class FrequencyTextField extends JTextField
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FrequencyTextField.class);
    //Value range: 0.000001 to 9999.999999
    private static final String REGEX = "^[0-9]{0,4}[.]?[0-9]{0,6}$";
    //This lets users start typing a really small number like 1 Hertz ... 0.00000
    private static final String ZEROS_REGEX = "^0?([.]0{0,5})?$";
    private double mMinimum;
    private double mMaximum;

    /**
     * Constructs an instance
     *
     * @param minimum allowable frequency value in Hertz
     * @param maximum allowable frequency value in Hertz
     * @param current frequency value in Hertz
     */
    public FrequencyTextField(long minimum, long maximum, long current)
    {
        super(8);
        mMinimum = minimum / 1E6d;
        mMaximum = maximum / 1E6d;

        PlainDocument document = (PlainDocument)this.getDocument();
        document.setDocumentFilter(new FrequencyFilter());
        setFrequency(current);
    }

    /**
     * Current frequency value
     * @return frequency in Hertz
     */
    public long getFrequency()
    {
        String value = getText();

        if(value == null || value.isEmpty())
        {
            return 0;
        }

        try
        {
            return (long)(Double.parseDouble(getText()) * 1E6d);
        }
        catch(Exception e)
        {
            LOGGER.error("Unable to parse frequency value from text [" + value + "] " + e.getLocalizedMessage());
        }

        return 0;
    }

    /**
     * Sets the current frequency value
     * @param frequency in Hertz
     */
    public void setFrequency(long frequency)
    {
        double frequencyMHz = frequency / 1E6d;

        if(isValid(String.valueOf(frequencyMHz)))
        {
            setText(String.valueOf(frequencyMHz));
        }
        else
        {
            LOGGER.warn("Can't set frequency [" + frequency + "Hz / " + frequencyMHz + "MHz] with current minimum [" + mMinimum + "MHz] and maximum [" + mMaximum + "MHz] limits");
        }
    }

    /**
     * Indicates if the value is a valid double value that is between the minimum and maximum extents.
     * @param value to test
     * @return true if it is valid.
     */
    private boolean isValid(String value)
    {
        if(value == null || value.isEmpty() || value.matches(ZEROS_REGEX))
        {
            return true;
        }

        if(value.matches(REGEX))
        {
            try
            {
                double frequency = Double.parseDouble(value);
                return mMinimum <= frequency && frequency <= mMaximum;
            }
            catch(NumberFormatException e)
            {
                return false;
            }
        }

        return false;
    }

    /**
     * Input filter for user entered values.
     */
    class FrequencyFilter extends DocumentFilter
    {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException
        {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append(doc.getText(0, doc.getLength()));
            sb.insert(offset, string);

            if(isValid(sb.toString()))
            {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
        {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append(doc.getText(0, doc.getLength()));
            sb.replace(offset, offset + length, text);

            if(isValid(sb.toString()))
            {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException
        {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append(doc.getText(0, doc.getLength()));
            sb.delete(offset, offset + length);

            if(isValid(sb.toString()))
            {
                super.remove(fb, offset, length);
            }
        }
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Frequency Control Test");
        frame.setSize(300, 200);
        FrequencyTextField ftf = new FrequencyTextField(1, 9_999_999_999l, 101_100_000);
        frame.setLayout(new MigLayout());
        frame.add(ftf);

        EventQueue.invokeLater(() -> frame.setVisible(true));
    }
}
