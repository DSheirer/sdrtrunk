package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector implementation of a complex mixer that uses JDK 17+ SIMD instructions.
 */
public class VectorComplexMixer extends ComplexMixer
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    /**
     * Constructs an instance
     * @param frequency of the oscillator
     * @param sampleRate of the oscillator
     */
    public VectorComplexMixer(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator.
     * @param iSamples complex samples to mix
     * @param qSamples complex samples to mix
     * @return mixed samples
     */
    @Override public ComplexSamples mix(float[] iSamples, float[] qSamples)
    {
        VectorUtilities.checkComplexArrayLength(iSamples, qSamples, VECTOR_SPECIES);

        //Reuse this complex samples buffer to store the results and return to caller
        ComplexSamples mixer = generate(iSamples.length);

        float[] iMixer = mixer.i();
        float[] qMixer = mixer.q();

        FloatVector iS, qS, iM, qM;

        for(int x = 0; x < iSamples.length; x += VECTOR_SPECIES.length())
        {
            iS = FloatVector.fromArray(VECTOR_SPECIES, iSamples, x);
            qS = FloatVector.fromArray(VECTOR_SPECIES, qSamples, x);
            iM = FloatVector.fromArray(VECTOR_SPECIES, iMixer, x);
            qM = FloatVector.fromArray(VECTOR_SPECIES, qMixer, x);

            //Perform complex mixing and store results back to mixer arrays
            iM.mul(iS).sub(qM.mul(qS)).intoArray(iMixer, x);
            qM.mul(iS).add(iM.mul(qS)).intoArray(qMixer, x);
        }

        return mixer;
    }
}
