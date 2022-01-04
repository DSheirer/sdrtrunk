package io.github.dsheirer.dsp.am;

/**
 * AM Demodulator interface
 */
public interface IAmDemodulator
{
    /**
     * Demodulate the complex I & Q sample arrays
     * @param i samples
     * @param q samples
     * @return demodulated samples.
     */
    float[] demodulate(float[] i, float[] q);
}
