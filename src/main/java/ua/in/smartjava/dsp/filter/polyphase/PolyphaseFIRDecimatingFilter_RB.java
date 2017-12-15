/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package ua.in.smartjava.dsp.filter.polyphase;

import ua.in.smartjava.dsp.filter.design.FilterDesignException;
import ua.in.smartjava.dsp.filter.fir.FIRFilter;
import ua.in.smartjava.dsp.filter.fir.FIRFilterSpecification;
import ua.in.smartjava.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import ua.in.smartjava.dsp.mixer.Oscillator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.RealBuffer;
import ua.in.smartjava.sample.real.RealToRealBufferAssembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolyphaseFIRDecimatingFilter_RB extends FIRFilter implements Listener<RealBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger( PolyphaseFIRDecimatingFilter_RB.class );

    private List<FilterStage> mFilterStages = new ArrayList<>();
    private int mFilterStagePointer;
    private Listener<RealBuffer> mListener;
    private RealToRealBufferAssembler mAssembler;
    private int mDecimationRatio;
    private float mAccumulator = 0.0f;
    private float mGain = 1.0f;

    /**
     * Polyphase ua.in.smartjava.filter stage for decimating real-valued ua.in.smartjava.sample buffers by the decimation ratio.  Creates a number of
     * polyphase ua.in.smartjava.filter arms/stages equal to the decimation ratio.
     *
     * Note: output real-valued ua.in.smartjava.buffer sizes are determined by the first input ua.in.smartjava.buffer that is presented to the ua.in.smartjava.filter
     * where the output ua.in.smartjava.buffer size is equal to the floor of the input ua.in.smartjava.buffer size divided by the decimation ratio.
     *
     * @param coefficients of a low pass ua.in.smartjava.filter designed for the input ua.in.smartjava.sample rate with a cutoff frequency that conforms
     * to the nyquist frequency for an output ua.in.smartjava.sample rate equal to the input ua.in.smartjava.sample rate divided by the decimation ratio.
     *
     * @param decimationRatio to decimate the input ua.in.smartjava.sample rate by.
     * @param gain to apply to the filtered output.  Use a value of 1.0 to apply no gain.
     */
    public PolyphaseFIRDecimatingFilter_RB(float[] coefficients, int decimationRatio, float gain)
    {
        mDecimationRatio = decimationRatio;
        mGain = gain;

        createFilterStages(coefficients, mDecimationRatio);
        mFilterStagePointer = mFilterStages.size() - 1;
    }

    /**
     * Creates polyphase ua.in.smartjava.filter stages (arms) from the set of coefficients.
     *
     * @param coefficients for the low-pass ua.in.smartjava.filter
     * @param decimationRatio to determine the number of polyphase ua.in.smartjava.filter stages.
     */
    private void createFilterStages(float[] coefficients, int decimationRatio)
    {
        int tapSize = (int)Math.ceil((double)coefficients.length / (double)decimationRatio);

        float[][] coefficientSets = new float[decimationRatio][tapSize];

        int stagePointer = 0;
        int coefficientPointer = 0;

        //Split the coefficients up into polyphase stage coefficient sets
        for(int x = 0; x < coefficients.length; x++)
        {
            coefficientSets[stagePointer++][coefficientPointer] = coefficients[x];

            if(stagePointer >= decimationRatio)
            {
                stagePointer = 0;
                coefficientPointer++;
            }
        }

        //Create the stages
        for(int x = 0; x < decimationRatio; x++)
        {
            mFilterStages.add(new FilterStage(coefficientSets[x]));
        }
    }

    public void dispose()
    {
        mListener = null;
    }

    @Override
    public void receive(RealBuffer buffer)
    {
        if(mAssembler == null)
        {
            mAssembler = new RealToRealBufferAssembler(buffer.getSamples().length / mDecimationRatio);
            mAssembler.setListener(mListener);
        }

        for(float sample: buffer.getSamples())
        {
            filter(sample);
        }

        int a = 0;
    }

    private void filter(float sample)
    {
        FilterStage stage = mFilterStages.get(mFilterStagePointer--);

        mAccumulator += stage.filter(sample);

        if(mFilterStagePointer < 0)
        {
            mAssembler.receive(mAccumulator * mGain);
            mAccumulator = 0.0f;

            mFilterStagePointer = mFilterStages.size() - 1;
        }
    }

    public void setListener(Listener<RealBuffer> listener)
    {
        mListener = listener;

        if(mAssembler != null)
        {
            mAssembler.setListener(mListener);
        }
    }

    public void removeListener()
    {
        mListener = null;

        if(mAssembler != null)
        {
            mAssembler.setListener(null);
        }
    }

    public static void main(String[] args)
    {
        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate( 48000 )
                .gridDensity( 16 )
                .passBandCutoff( 3000 )
                .passBandAmplitude( 1.0 )
                .passBandRipple( 0.01 )
                .stopBandStart( 4000 )
                .stopBandAmplitude( 0.0 )
                .stopBandRipple( 0.027 )
                .build();

        RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

        try
        {
            if(!designer.isValid())
            {
                throw new FilterDesignException("Couldn't design the ua.in.smartjava.filter");
            }

            float[] coefficients = designer.getImpulseResponse();
            PolyphaseFIRDecimatingFilter_RB filter = new PolyphaseFIRDecimatingFilter_RB(coefficients, 6, 1.0f);
            filter.setListener(new Listener<RealBuffer>()
            {
                @Override
                public void receive(RealBuffer realBuffer)
                {
                    mLog.info("Filtered: " + Arrays.toString(realBuffer.getSamples()));
                }
            });

            Oscillator oscillator = new Oscillator(3400, 48000);

            float[] samples = new float[500];
            for(int x = 0; x < 500; x++)
            {
                samples[x] = oscillator.getFloat();
                oscillator.rotate();
            }

            RealBuffer buffer = new RealBuffer(samples);
            filter.receive(buffer);
        }
        catch(FilterDesignException e)
        {
            mLog.debug("Error designing ua.in.smartjava.filter", e);
        }

    }
}
