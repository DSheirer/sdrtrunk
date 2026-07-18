package io.github.dsheirer.audio;

/**
 * Used for defining an array list of filter methods to be implemented in AudioModule
 * The filters will be applied in the order given in the list.
 * Each audio filter mush implement the filter method.
 */

public abstract class AbstractAudioFilter
{
    /**
     * To be overridden by each audio filter
     * @param audio a buffer of 8 KHz audio samples
     * @return float[] filtered buffer of 8 KHz audio samples
     */
    public abstract float[] filter(float[] audio);
}
