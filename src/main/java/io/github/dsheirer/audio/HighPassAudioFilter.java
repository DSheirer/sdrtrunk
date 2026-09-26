package io.github.dsheirer.audio;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HighPassAudioFilter extends AbstractAudioFilter
{
    private static final Logger mLog = LoggerFactory.getLogger(HighPassAudioFilter.class);
    private static float[] sHighPassFilterCoefficients;

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
                .sampleRate(8000)
                .stopBandCutoff(200)
                .stopBandAmplitude(0.0)
                .stopBandRipple(0.025)
                .passBandStart(300)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .build();
        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sHighPassFilterCoefficients = designer.getImpulseResponse();
            }
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }
    }

    private final IRealFilter mHighPassFilter = FilterFactory.getRealFilter(sHighPassFilterCoefficients);

    @Override
    public float[] filter(float[] audio)
    {
        audio = mHighPassFilter.filter(audio);
        return audio;
    }
}
