package io.github.dsheirer.source;

import io.github.dsheirer.sample.Provider;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.complex.ComplexBuffer;

public abstract class ComplexSource extends Source implements Provider<ComplexBuffer>
{
    @Override
    public SampleType getSampleType()
    {
        return SampleType.COMPLEX;
    }
}
