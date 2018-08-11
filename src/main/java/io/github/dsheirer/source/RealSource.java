package io.github.dsheirer.source;

import io.github.dsheirer.sample.Provider;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;


public abstract class RealSource extends Source implements Provider<ReusableFloatBuffer>
{
    @Override
    public SampleType getSampleType()
    {
        return SampleType.REAL;
    }
}
