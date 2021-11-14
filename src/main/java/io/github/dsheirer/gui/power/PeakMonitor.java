package io.github.dsheirer.gui.power;

/**
 * Peak value monitor
 */
public class PeakMonitor
{
    private double mInitialValue;
    private double mPeak;

    /**
     * Constructs an instance
     * @param initialValue for the peak value
     */
    public PeakMonitor(double initialValue)
    {
        mInitialValue = initialValue;
        reset();
    }

    /**
     * Current peak value
     */
    public double getPeak()
    {
        return mPeak;
    }

    /**
     * Processes the value and returns the current peak value
     * @param value to process
     * @return current peak value
     */
    public double process(double value)
    {
        if(value > mPeak)
        {
            mPeak = value;
        }

        return getPeak();
    }

    /**
     * Resets the peak value to the initial value
     */
    public void reset()
    {
        mPeak = mInitialValue;
    }
}
