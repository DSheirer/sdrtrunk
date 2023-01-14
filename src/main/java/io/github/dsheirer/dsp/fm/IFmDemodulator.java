package io.github.dsheirer.dsp.fm;

/**
 * Interface for FM demodulator
 */
public interface IFmDemodulator
{
    /**
     * Demodulate the complex sample array and return an array of real audio samples.
     * @param samples to demodulate
     * @return demodulated samples.
     */
    float[] demodulate(float[] i, float[] q);
}
