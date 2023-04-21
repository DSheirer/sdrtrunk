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

    /**
     * Enable or disable the squelch noise floor auto-track feature.
     * @param autoTrack true to enable.
     */
    void setSquelchAutoTrack(boolean autoTrack);

    /**
     * Indicates if the squelch noise floor auto-track feature is enabled.
     * @return true if enabled.
     */
    boolean isSquelchAutoTrack();
}
