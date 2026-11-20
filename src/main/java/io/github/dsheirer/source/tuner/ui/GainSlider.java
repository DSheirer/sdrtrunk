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

package io.github.dsheirer.source.tuner.ui;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Integer slider with a value label, for gain settings.  The change handler is invoked each time the user moves the
 * slider to a different whole value, but not when the value is set with {@link #setValue(int)}.
 */
public class GainSlider extends HBox
{
    private final Slider mSlider;
    private final Label mValueLabel = new Label();
    private IntConsumer mChangeHandler;
    private IntFunction<String> mFormatter = String::valueOf;
    private boolean mUpdating = false;
    private boolean mApplyOnRelease = false;
    private int mValue;

    /**
     * Constructs an instance
     * @param minimum value
     * @param maximum value
     * @param value initial value
     */
    public GainSlider(int minimum, int maximum, int value)
    {
        getStyleClass().add("gain-slider");
        setAlignment(Pos.CENTER_LEFT);
        mSlider = new Slider(minimum, maximum, value);
        mSlider.setBlockIncrement(1);
        mSlider.setMajorTickUnit(1);
        mSlider.setMinorTickCount(0);
        mSlider.setShowTickMarks(maximum - minimum <= 30);
        mSlider.setSnapToTicks(true);
        mSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mSlider, Priority.ALWAYS);
        mValueLabel.getStyleClass().add("gain-slider-value");
        mValue = value;
        updateLabel();

        mSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int rounded = (int)Math.round(newValue.doubleValue());

            if(rounded != mValue)
            {
                mValue = rounded;
                updateLabel();

                if(!mUpdating && mChangeHandler != null && !(mApplyOnRelease && mSlider.isValueChanging()))
                {
                    mChangeHandler.accept(rounded);
                }
            }
        });

        mSlider.valueChangingProperty().addListener((observable, wasChanging, changing) -> {
            if(mApplyOnRelease && !changing && !mUpdating && mChangeHandler != null)
            {
                mChangeHandler.accept(mValue);
            }
        });

        getChildren().addAll(mSlider, mValueLabel);
    }

    /**
     * Sets the handler for user changes.
     */
    public void setOnChange(IntConsumer handler)
    {
        mChangeHandler = handler;
    }

    /**
     * When true, a drag of the slider thumb invokes the change handler once, when the thumb is released, instead of
     * at each whole value along the way.
     */
    public void setApplyOnRelease(boolean applyOnRelease)
    {
        mApplyOnRelease = applyOnRelease;
    }

    /**
     * Sets the formatter for the value label, e.g. to append dB.
     */
    public void setFormatter(IntFunction<String> formatter)
    {
        mFormatter = formatter;
        updateLabel();
    }

    /**
     * Sets the value without invoking the change handler.
     */
    public void setValue(int value)
    {
        mUpdating = true;
        mValue = value;
        mSlider.setValue(value);
        updateLabel();
        mUpdating = false;
    }

    /**
     * Current value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Changes the slider range without invoking the change handler.
     */
    public void setRange(int minimum, int maximum)
    {
        mUpdating = true;
        mSlider.setMin(minimum);
        mSlider.setMax(maximum);
        mSlider.setShowTickMarks(maximum - minimum <= 30);
        mValue = (int)Math.round(mSlider.getValue());
        updateLabel();
        mUpdating = false;
    }

    /**
     * Sets the tooltip on the slider.
     */
    public void setTooltip(String text)
    {
        mSlider.setTooltip(new Tooltip(text));
    }

    /**
     * Wrapped slider.
     */
    public Slider getSlider()
    {
        return mSlider;
    }

    private void updateLabel()
    {
        mValueLabel.setText(mFormatter.apply(mValue));
    }
}
