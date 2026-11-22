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
package io.github.dsheirer.gui.power;

import java.text.DecimalFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Vertical bar meter for a decibel power level, with optional peak and squelch threshold lines and a value scale.
 *
 * Use this class on the JavaFX application thread.
 */
public class PowerMeter extends Region
{
    public static final double DEFAULT_MINIMUM_POWER = -110.0d;
    public static final double DEFAULT_MAXIMUM_POWER = 0.0d;
    private static final double BAR_WIDTH = 30;
    private static final double PADDING = 3;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final Color COLOR_BAR = Color.LIGHTGRAY;
    private static final Color COLOR_THRESHOLD = Color.BLUE;
    private static final Color COLOR_PEAK = Color.PINK;

    private final Canvas mCanvas = new Canvas();
    private final ObjectProperty<Paint> mForeground = new SimpleObjectProperty<>(Color.BLACK);
    private final ObjectProperty<Font> mFont = new SimpleObjectProperty<>(Font.getDefault());
    private final double mMinimumValue = DEFAULT_MINIMUM_POWER;
    private final double mMaximumValue = DEFAULT_MAXIMUM_POWER;
    private double mPeak = mMinimumValue;
    private double mPower = mMinimumValue;
    private double mSquelchThreshold = mMinimumValue;
    private boolean mPeakVisible = false;
    private boolean mSquelchThresholdVisible = false;

    /**
     * Constructs an instance.
     */
    public PowerMeter()
    {
        getChildren().add(mCanvas);
        setPrefSize(90, 100);
        setMinSize(70, 60);
        mForeground.addListener((observable, oldValue, newValue) -> draw());
        mFont.addListener((observable, oldValue, newValue) -> draw());
    }

    /**
     * Color for the meter outline and scale text.  Bind it to a themed control's text color.
     */
    public ObjectProperty<Paint> foregroundProperty()
    {
        return mForeground;
    }

    /**
     * Font for the scale text.
     */
    public ObjectProperty<Font> fontProperty()
    {
        return mFont;
    }

    @Override
    protected void layoutChildren()
    {
        double width = snapSizeX(getWidth());
        double height = snapSizeY(getHeight());

        if(mCanvas.getWidth() != width || mCanvas.getHeight() != height)
        {
            mCanvas.setWidth(width);
            mCanvas.setHeight(height);
            draw();
        }
    }

    /**
     * Resets peak, power and squelch to minimums
     */
    public void reset()
    {
        mPeak = mMinimumValue;
        mPower = mMinimumValue;
        mSquelchThreshold = mMinimumValue;
        draw();
    }

    /**
     * Sets the current power level constrained to the minimum and maximum values for this control.
     * @param power (dB)
     */
    public void setPower(double power)
    {
        mPower = Math.clamp(power, mMinimumValue, mMaximumValue);
        draw();
    }

    /**
     * Sets the peak value line
     * @param peak (db)
     */
    public void setPeak(double peak)
    {
        mPeak = peak;
        draw();
    }

    /**
     * Sets the squelch threshold value
     * @param squelchThreshold (dB)
     */
    public void setSquelchThreshold(double squelchThreshold)
    {
        mSquelchThreshold = squelchThreshold;
        draw();
    }

    /**
     * Sets the peak line visibility
     */
    public void setPeakVisible(boolean peakVisible)
    {
        mPeakVisible = peakVisible;
        draw();
    }

    /**
     * Sets the visibility of the squelch threshold line
     */
    public void setSquelchThresholdVisible(boolean squelchThresholdVisible)
    {
        mSquelchThresholdVisible = squelchThresholdVisible;
        draw();
    }

    private void draw()
    {
        double width = mCanvas.getWidth();
        double height = mCanvas.getHeight();
        GraphicsContext gc = mCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        if(width <= 0 || height <= 0)
        {
            return;
        }

        double meterHeight = height - 1;

        //Meter outline
        gc.setStroke(mForeground.get());
        gc.setLineWidth(1);
        gc.strokeRect(0.5, 0.5, BAR_WIDTH, meterHeight - 0.5);

        //Filled bar
        gc.setFill(COLOR_BAR);
        double top = getY(getPercent(mPower), meterHeight);
        gc.fillRect(PADDING, top, BAR_WIDTH - 2 * PADDING + 1, meterHeight - PADDING - top);

        if(mSquelchThresholdVisible && isValidValue(mSquelchThreshold))
        {
            gc.setStroke(COLOR_THRESHOLD);
            drawLine(gc, getY(getPercent(mSquelchThreshold), meterHeight));
        }

        if(mPeakVisible && isValidValue(mPeak))
        {
            gc.setStroke(COLOR_PEAK);
            drawLine(gc, getY(getPercent(mPeak), meterHeight));
        }

        //Scale, with the number of labels adapted to the height of the control
        Font font = mFont.get() != null ? mFont.get() : Font.getDefault();
        gc.setFont(font);
        gc.setFill(mForeground.get());
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        double extent = mMaximumValue - mMinimumValue;
        double labelCount = Math.max(1, height / (font.getSize() * 2.5));
        double interval = extent / labelCount;

        for(double value = mMaximumValue; value > mMinimumValue; value -= interval)
        {
            double y = Math.clamp(getY(getPercent(value), meterHeight), font.getSize() / 2, height - font.getSize() / 2);
            gc.fillText(DECIMAL_FORMAT.format(value), BAR_WIDTH + PADDING * 2, y);
        }
    }

    private void drawLine(GraphicsContext gc, double y)
    {
        double snapped = Math.floor(y) + 0.5;
        gc.strokeLine(0.5, snapped, BAR_WIDTH + 0.5, snapped);
    }

    private double getY(double percent, double meterHeight)
    {
        double fillHeight = meterHeight - 2 * PADDING;
        return PADDING + fillHeight * (1.0 - percent);
    }

    private double getPercent(double value)
    {
        return (value - mMinimumValue) / (mMaximumValue - mMinimumValue);
    }

    private boolean isValidValue(double value)
    {
        return mMinimumValue <= value && value <= mMaximumValue;
    }
}
