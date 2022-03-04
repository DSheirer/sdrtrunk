package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Gain control that applies gain to a complex sample buffer to achieve unity gain for
 * the single sample that has the largest envelope within the buffer.  Uses JDK 17+ SIMD
 * vector intrinsics to process the buffer samples.
 */
public class VectorComplexGainControl implements IComplexGainControl
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final float OBJECTIVE_ENVELOPE = 1.0f;
    private static final float MINIMUM_ENVELOPE = 0.0001f;
    private static final float ENVELOPE_ESTIMATE = 0.4f;

    @Override public ComplexSamples process(float[] i, float[] q)
    {
        VectorUtilities.checkComplexArrayLength(i, q, VECTOR_SPECIES);

        //Determine the largest envelope
        FloatVector iAbs, qAbs, maxEnvelope;

        maxEnvelope = FloatVector.zero(VECTOR_SPECIES).add(MINIMUM_ENVELOPE);

        for(int bufferPointer = 0; bufferPointer < i.length; bufferPointer += VECTOR_SPECIES.length())
        {
            iAbs = FloatVector.fromArray(VECTOR_SPECIES, i, bufferPointer).abs();
            qAbs = FloatVector.fromArray(VECTOR_SPECIES, q, bufferPointer).abs();
            maxEnvelope = maxEnvelope.max(iAbs.add(qAbs.mul(ENVELOPE_ESTIMATE)));
            maxEnvelope = maxEnvelope.max(qAbs.add(iAbs.mul(ENVELOPE_ESTIMATE)));
        }

        float gain = OBJECTIVE_ENVELOPE / maxEnvelope.reduceLanes(VectorOperators.MAX);

        float[] iProcessed = new float[i.length];
        float[] qProcessed = new float[q.length];

        //Apply gain to all I & Q samples
        for(int bufferPointer = 0; bufferPointer < i.length; bufferPointer += VECTOR_SPECIES.length())
        {
            FloatVector.fromArray(VECTOR_SPECIES, i, bufferPointer).mul(gain).intoArray(iProcessed, bufferPointer);
            FloatVector.fromArray(VECTOR_SPECIES, q, bufferPointer).mul(gain).intoArray(qProcessed, bufferPointer);
        }

        return new ComplexSamples(iProcessed, qProcessed);
    }
}
