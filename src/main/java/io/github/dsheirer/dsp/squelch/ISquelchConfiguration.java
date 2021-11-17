package io.github.dsheirer.dsp.squelch;

/**
 * Interface for configurations that support squelch threshold setting.
 */
public interface ISquelchConfiguration
{
    /**
     * Sets the squelch threshold
     * @param threshold (dB)
     */
    void setSquelchThreshold(int threshold);

    /**
     * Squelch threshold
     * @return threshold (dB)
     */
    int getSquelchThreshold();
}
