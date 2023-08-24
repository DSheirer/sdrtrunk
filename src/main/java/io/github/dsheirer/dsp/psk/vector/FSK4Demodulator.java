/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.module.decode.dmr.DMRMessageFramer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSK4Demodulator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FSK4Demodulator.class);

    private float[] mIBuffer = new float[20];
    private float[] mQBuffer = new float[20];
    private int mSymbolRate;
    private float mSampleRate;
    private float mSamplesPerSymbol;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");

    public FSK4Demodulator(int symbolRate)
    {
        mSymbolRate = symbolRate;
    }

    public void setSampleRate(double sampleRate)
    {
        mSampleRate = (float)sampleRate;
        mSamplesPerSymbol = (float)sampleRate / mSymbolRate;
    }

    public void receive(ComplexSamples samples)
    {
        int bufferLength = samples.i().length;

        mIBuffer[0] = mIBuffer[mIBuffer.length - 1];
        mQBuffer[0] = mQBuffer[mQBuffer.length - 1];

        int requiredBufferLength = bufferLength + 1;
        if(mIBuffer.length != requiredBufferLength)
        {
            mIBuffer = Arrays.copyOf(mIBuffer, requiredBufferLength);
            mQBuffer = Arrays.copyOf(mQBuffer, requiredBufferLength);
        }

        System.arraycopy(samples.i(), 0, mIBuffer, 1, bufferLength);
        System.arraycopy(samples.q(), 0, mQBuffer, 1, bufferLength);

        float i, q;
        float[] demodulated = new float[bufferLength];

        for(int x = 0; x < bufferLength; x++)
        {
            i = (mIBuffer[x] * mIBuffer[x + 1]) - (-mQBuffer[x] * mQBuffer[x + 1]);
            q = (mIBuffer[x] * mQBuffer[x + 1]) + (-mQBuffer[x] * mIBuffer[x + 1]);
            demodulated[x] = 8.0f * (float)Math.atan2(q, i);

            System.out.println(DECIMAL_FORMAT.format(demodulated[x]));
        }

        int a = 0;
    }


    public static void main(String[] args)
    {
        LOGGER.info("Starting ...");

        float[] coefficients = null;
        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(50000)
                .passBandCutoff(5100)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandAmplitude(0.0)
                .stopBandStart(6500)
                .stopBandRipple(0.01)
                .build();

        try
        {
            coefficients = FilterFactory.getTaps(specification);//
        }
        catch(Exception fde) //FilterDesignException
        {
            System.out.println("Error");
        }

        IRealFilter iChannelFilter = FilterFactory.getRealFilter(coefficients);
        IRealFilter qChannelFilter = FilterFactory.getRealFilter(coefficients);

        double samplesPerSymbol = 50000.0 / 4800.0;
        int order = 16;
        float alpha = 0.2f;
        float[] matchedFilterCoefficients = FilterFactory.getRootRaisedCosine(samplesPerSymbol, order, alpha);

        IRealFilter iMatchedFilter = FilterFactory.getRealFilter(matchedFilterCoefficients);
        IRealFilter qMatchedFilter = FilterFactory.getRealFilter(matchedFilterCoefficients);

        String file = "/home/denny/SDRTrunk/recordings/20230819_064211_451250000_SaiaNet_Syracuse_Control_29_baseband.wav";
//        String file = "/home/denny/SDRTrunk/recordings/20230819_064344_454575000_JPJ_Communications_(DMR)_Madison_Control_28_baseband.wav";
        boolean autoReplay = false;

        FSK4Demodulator demodulator = new FSK4Demodulator(4800);

//        DQPSKVectorDemodulator2 demodulator = new DQPSKVectorDemodulator2(4800);
        DMRMessageFramer framer = new DMRMessageFramer(null);
//        demodulator.setSymbolListener(dibits -> {
//            for(Dibit dibit : dibits)
//            {
//                framer.receive(dibit);
//            }
//        });
        framer.setListener(iMessage -> System.out.println(iMessage));

        Listener<INativeBuffer> nativeBufferListener = (INativeBuffer iNativeBuffer) ->
        {
            Iterator<ComplexSamples> it = iNativeBuffer.iterator();
            while(it.hasNext())
            {
                ComplexSamples unfiltered = it.next();

                float[] iFiltered = unfiltered.i();
                float[] qFiltered = unfiltered.q();

                iFiltered = iChannelFilter.filter(iFiltered);
                qFiltered = qChannelFilter.filter(qFiltered);

//                iFiltered = iMatchedFilter.filter(iFiltered);
//                qFiltered = qMatchedFilter.filter(qFiltered);

                ComplexSamples filtered = new ComplexSamples(iFiltered, qFiltered, unfiltered.timestamp());
                demodulator.receive(filtered);
//                demodulator.receive(unfiltered);
//                legacy.receive(unfiltered);
            }
        };
        try(ComplexWaveSource source = new ComplexWaveSource(new File(file), autoReplay))
        {
            source.setListener(nativeBufferListener);
            source.start();
            demodulator.setSampleRate(source.getSampleRate());

            while(true)
            {
                source.next(2048, true);
                //wait
            }
        }
        catch(IOException ioe)
        {
            LOGGER.error("Error", ioe);
        }

        LOGGER.info("Finished");
    }

}
