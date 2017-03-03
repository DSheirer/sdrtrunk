/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package dsp.filter.fir.remez;

import dsp.filter.fir.FIRFilterSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolyphaseChannelizerFilterFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelizerFilterFactory.class);

    private static final double OBJECTIVE_BAND_EDGE_COEFFICIENT_AMPLITUDE = Math.sqrt(2.0) / 2.0; //.707xxx

    public static float[] getFilter(int sampleRate, int channelBandwidth)
    {
        FIRLinearPhaseFilterType type = FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL;

        int passBandStart = 0;
        int gridDensity = 32;

        //Set pass band to stop at about 80% of channel bandwidth as starting point
//        int passBandStop = (int)(channelBandwidth * 0.95);
        int passBandStop = channelBandwidth;

        //Set the stop band to start at 40% into the adjacent channel, since we're oversampling by 2.
        int stopBandStart = (int)(channelBandwidth * 1.40);

        double passRipple = 0.1d;
        double stopRipple = 0.01d;

//        int order = FIRFilterSpecification.estimateBandPassOrder(sampleRate, passBandStart, passBandStop,
//            passRipple, stopRipple);
        int order = FIRFilterSpecification.estimateFilterOrder( sampleRate, passBandStop,
            stopBandStart, passRipple, stopRipple );

        // Ensure even order since we're designing a Type 1 filter
        if(order % 2 == 1)
        {
            order++;
        }

        FIRFilterSpecification specification = new PolyphaseChannelizerDesigner(order, gridDensity);

        FIRFilterSpecification.FrequencyBand passBand = new FIRFilterSpecification.FrequencyBand(sampleRate, 0,
            passBandStop, 1.0, passRipple);
        FIRFilterSpecification.FrequencyBand stopBand = new FIRFilterSpecification.FrequencyBand(sampleRate,
            stopBandStart, (int)(sampleRate / 2), 0.0, stopRipple);

        specification.addFrequencyBand(passBand);
        specification.addFrequencyBand(stopBand);

        boolean complete = false;
        int stepSize = 100;
        double bandEdgeFrequency = Math.cos(Math.PI * (double)channelBandwidth / (double)(sampleRate / 2));
        float[] filter = null;

        while(!complete)
        {
            mLog.debug("Step Size: " + stepSize);

            RemezFIRFilterDesigner2 designer = new RemezFIRFilterDesigner2(specification);

            complete = true; //temporary

            double bandEdgeAmplitude = designer.getFrequencyResponse(bandEdgeFrequency);
            mLog.debug("Coefficient Amplitude at Band Edge is: " + bandEdgeAmplitude + " for frequency: " + passBandStop);

            if(bandEdgeAmplitude < OBJECTIVE_BAND_EDGE_COEFFICIENT_AMPLITUDE)
            {
                if(stepSize <= 1)
                {
                    complete = true;
                    try
                    {
                        filter = designer.getImpulseResponse();
                    }
                    catch(Exception e)
                    {
                        mLog.debug("Error getting filter response", e);
                    }
                }
                else
                {
                    passBandStop += stepSize;
                    stepSize /= 10;
                }
            }
            else
            {
                passBandStop -= stepSize;
            }

            if(!complete)
            {
                specification.clearFrequencyBands();

                passBand = new FIRFilterSpecification.FrequencyBand(sampleRate, 0,
                    passBandStop, 1.0, passRipple);
                specification.addFrequencyBand(passBand);
                specification.addFrequencyBand(stopBand);
            }
        }

        return filter;
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        float[] filter = getFilter(2500000, 12500);

        mLog.debug("Finished ...");
    }
}
