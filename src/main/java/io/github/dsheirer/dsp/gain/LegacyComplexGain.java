package io.github.dsheirer.dsp.gain;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;

public class LegacyComplexGain implements Listener<Complex>
{
    private float mGain;
    private Listener<Complex> mListener;

    public LegacyComplexGain(float gain)
    {
        mGain = gain;
    }

    public Complex apply(Complex sample)
    {
        sample.multiply(mGain);

        return sample;
    }

    @Override
    public void receive(Complex sample)
    {
        if(mListener != null)
        {
            apply(sample);

            mListener.receive(sample);
        }
    }

    public void setListener(Listener<Complex> listener)
    {
        mListener = listener;
    }
}
