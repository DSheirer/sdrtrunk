package io.github.dsheirer.gui.control;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Power meter for displaying signal power levels in dB scale with optional peak value and squelch threshold lines.
 */
public class DbPowerMeter extends JComponent
{
    public static final double DEFAULT_MINIMUM_POWER = -110.0d;
    public static final double DEFAULT_MAXIMUM_POWER = 0.0d;
    private static final int BAR_WIDTH = 30;
    private static final int PADDING = 3;
    private static final int DOUBLE_PADDING = PADDING * 2;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final Color COLOR_BAR = Color.LIGHT_GRAY;
    private static final Color COLOR_THRESHOLD = Color.BLUE;
    private static final Color COLOR_PEAK = Color.PINK;

    private double mMinimumValue = DEFAULT_MINIMUM_POWER;
    private double mMaximumValue = DEFAULT_MAXIMUM_POWER;
    private double mExtent = mMaximumValue - mMinimumValue;
    private double mPeak = mMinimumValue;
    private double mPower = mMinimumValue;
    private double mSquelchThreshold = mMinimumValue;

    private boolean mPeakVisible = false;
    private boolean mSquelchThresholdVisible = false;

    /**
     * Constructs an instance.
     */
    public DbPowerMeter()
    {
        setPreferredSize(new Dimension(90, 100));
        setBorder(BorderFactory.createTitledBorder("Power (dB)"));
    }

    /**
     * Resets peak, power and squelch to minimums
     */
    public void reset()
    {
        mPeak = mMinimumValue;
        mPower = mMinimumValue;
        mSquelchThreshold = mMinimumValue;
        repaint();
    }

    /**
     * Primary method for rendering the control
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        //Draw meter border
        g.setColor(getForeground());
        g.drawRect(getInsets().left, getInsets().top, BAR_WIDTH, getHeight() - getInsets().top - getInsets().bottom);

        //Draw filled bar
        g.setColor(COLOR_BAR);
        int fillHeight = getHeight() - getInsets().top - getInsets().bottom - DOUBLE_PADDING;
        int height = (int) (fillHeight * getPowerPercent() - PADDING);
        int top = (fillHeight - height) + getInsets().top + PADDING;
        g.fillRect(getInsets().left + PADDING, top, BAR_WIDTH - DOUBLE_PADDING, height);

        //Draw threshold
        if(isSquelchThresholdVisible())
        {
            g.setColor(COLOR_THRESHOLD);
            drawPercentLine(g, getSquelchThresholdPercent());
        }

        //Draw peak line
        if(isPeakVisible())
        {
            g.setColor(COLOR_PEAK);
            drawPercentLine(g, getPeakPercent());
        }

        //Draw legend/scale
        g.setColor(getForeground());
        for(double legendValue: getScaleValues())
        {
            drawScaleText(g, legendValue);
        }
    }

    /**
     * Draws a line as a percentage of the value range (extent) using the current graphics color.
     */
    private void drawPercentLine(Graphics g, double percentValue)
    {
        int totalHeight = getHeight() - getInsets().top - getInsets().bottom - DOUBLE_PADDING;
        int height = (int) (totalHeight * percentValue - PADDING);
        int top = (totalHeight - height) + getInsets().top + PADDING;
        int left = getInsets().left;
        g.drawLine(left, top, left + BAR_WIDTH, top);
    }

    /**
     * Draws a tick and text for the value
     * @param g graphics object
     * @param value to draw
     */
    private void drawScaleText(Graphics g, double value)
    {
        double percent = getPercent(value);
        int totalHeight = getHeight() - getInsets().top - getInsets().bottom - DOUBLE_PADDING;
        int height = (int) (totalHeight * percent - PADDING);
        int top = (totalHeight - height) + getInsets().top + PADDING;
        int left = getInsets().left + BAR_WIDTH - PADDING;
        int textOffset = getFontMetrics(getFont()).getMaxAscent() / 2 - 2;
        String label = DECIMAL_FORMAT.format(value);
        g.drawString(label, left + DOUBLE_PADDING, top + textOffset);
    }

    /**
     * Scale values to display, dynamically adapted to the height of the control
     */
    private List<Double> getScaleValues()
    {
        double ascent = getFontMetrics(getFont()).getMaxAscent() * 2;
        double labelCount = getHeight() / ascent;
        double interval = getExtent() / labelCount;
        List<Double> values = new ArrayList<>();
        for(double x = 0; x > getMinimumValue(); x -= interval)
        {
            values.add(x);
        }

        return values;
    }

    /**
     * Current power level
     * @return power level (dB)
     */
    public double getPower()
    {
        return mPower;
    }

    /**
     * Sets the current power level constrained to the minimum and maximum values for this control.
     * @param power (dB)
     */
    public void setPower(double power)
    {
        if(power > getMaximumValue())
        {
            mPower = getMaximumValue();
        }
        else if(power < getMinimumValue())
        {
            mPower = getMinimumValue();
        }
        else
        {
            mPower = power;
        }

        repaint();
    }

    /**
     * Squelch threshold value.
     * @return threshold (dB)
     */
    public double getSquelchThreshold()
    {
        return mSquelchThreshold;
    }

    /**
     * Sets the squelch thresold value
     * @param squelchThreshold (dB)
     */
    public void setSquelchThreshold(double squelchThreshold)
    {
        mSquelchThreshold = squelchThreshold;
        repaint();
    }

    /**
     * Minimum displayable power level
     * @return minimum (dB) - defaults to -110 dB
     */
    public double getMinimumValue()
    {
        return mMinimumValue;
    }

    /**
     * Sets the minimum displayable power level
     * @param minimumValue (dB)
     */
    public void setMinimumValue(double minimumValue)
    {
        mMinimumValue = minimumValue;
        updateExtent();
        repaint();
    }

    /**
     * Maximum displayable power level
     * @return maximum (dB) - defaults to 0 dB
     */
    public double getMaximumValue()
    {
        return mMaximumValue;
    }

    /**
     * Sets the maximum displayable power level
     * @param maximumValue (dB)
     */
    public void setMaximumValue(double maximumValue)
    {
        mMaximumValue = maximumValue;
        updateExtent();
        repaint();
    }

    /**
     * Updates the min/max extent
     */
    private void updateExtent()
    {
        mExtent = getMaximumValue() - getMinimumValue();
    }

    /**
     * Extent or range of displayable values.
     * @return extent (dB)
     */
    private double getExtent()
    {
        return mExtent;
    }

    /**
     * Peak value line
     * @return peak (dB)
     */
    public double getPeak()
    {
        return mPeak;
    }

    /**
     * Sets the peak value line
     * @param peak (db)
     */
    public void setPeak(double peak)
    {
        mPeak = peak;
        repaint();
    }

    /**
     * Indicates if the peak line is set to visible and the current peak value is within the min/max range.
     */
    public boolean isPeakVisible()
    {
        return mPeakVisible & isValidValue(getPeak());
    }

    /**
     * Sets the peak line visibility
     * @param peakVisible value
     */
    public void setPeakVisible(boolean peakVisible)
    {
        mPeakVisible = peakVisible;
        repaint();
    }

    /**
     * Indicates if the squelch threshold line is set to visible and the current squelch threshold value is
     * within the min/max displayable value range.
     * @return true if visible.
     */
    public boolean isSquelchThresholdVisible()
    {
        return mSquelchThresholdVisible & isValidValue(getSquelchThreshold());
    }

    /**
     * Sets the visibility of the squelch threshold line
     * @param squelchThresholdVisible visibility
     */
    public void setSquelchThresholdVisible(boolean squelchThresholdVisible)
    {
        mSquelchThresholdVisible = squelchThresholdVisible;
        repaint();
    }

    /**
     * Calculates the percentage of the min/max extent for the value
     * @param value to calculate
     * @return percentage of displayable value range
     */
    private double getPercent(double value)
    {
        return (value - getMinimumValue()) / getExtent();
    }

    /**
     * Power as percentage of displayable range
     */
    private double getPowerPercent()
    {
        return getPercent(mPower);
    }

    /**
     * Squelch threshold as percentage of displayable range
     */
    private double getSquelchThresholdPercent()
    {
        return getPercent(mSquelchThreshold);
    }

    /**
     * Peak as percentage of displayable range
     */
    private double getPeakPercent()
    {
        return getPercent(mPeak);
    }

    /**
     * Indicates if the value is valid, meaning it falls within the min/max range of displayable values.
     */
    private boolean isValidValue(double value)
    {
        return getMinimumValue() <= value && value <= getMaximumValue();
    }
}
