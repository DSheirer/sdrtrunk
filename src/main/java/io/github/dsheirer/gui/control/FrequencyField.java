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

package io.github.dsheirer.gui.control;

import java.util.function.LongPredicate;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * Text field for a frequency in MHz (to 1 Hz).  The value is committed when the user presses Enter or leaves the
 * field.  A commit handler can reject the new value, which restores the previous value.  Escape reverts the text.
 */
public class FrequencyField extends TextField
{
    private long mFrequency;
    private LongPredicate mCommitHandler;

    /**
     * Constructs an instance
     * @param frequency initial value in Hertz
     */
    public FrequencyField(long frequency)
    {
        getStyleClass().add("frequency-field");
        setPrefColumnCount(11);
        setFrequency(frequency);

        setOnAction(e -> commit());
        focusedProperty().addListener((observable, wasFocused, focused) -> {
            if(!focused)
            {
                commit();
            }
        });
        setOnKeyPressed(e -> {
            if(e.getCode() == KeyCode.ESCAPE)
            {
                setText(FrequencyEntry.format(mFrequency));
                e.consume();
            }
        });
    }

    /**
     * Sets the handler for committed values.  The handler returns false to reject the value.
     */
    public void setCommitHandler(LongPredicate handler)
    {
        mCommitHandler = handler;
    }

    /**
     * Current frequency in Hertz.
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Sets the frequency without invoking the commit handler.
     */
    public void setFrequency(long frequency)
    {
        mFrequency = frequency;
        setText(FrequencyEntry.format(frequency));
    }

    private void commit()
    {
        Long parsed = FrequencyEntry.parse(getText());

        if(parsed == null)
        {
            setText(FrequencyEntry.format(mFrequency));
            return;
        }

        if(parsed == mFrequency)
        {
            setText(FrequencyEntry.format(mFrequency));
            return;
        }

        long previous = mFrequency;
        mFrequency = parsed;
        setText(FrequencyEntry.format(parsed));

        if(mCommitHandler != null && !mCommitHandler.test(parsed))
        {
            setFrequency(previous);
        }
    }
}
