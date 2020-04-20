/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.afsk;

import io.github.dsheirer.buffer.FloatAveragingBuffer;
import io.github.dsheirer.dsp.mixer.IOscillator;
import io.github.dsheirer.dsp.mixer.Oscillator;
import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class AFSKTest
{
    private final static Logger mLog = LoggerFactory.getLogger(AFSK1200Decoder.class);

    private static final DecimalFormat df = new DecimalFormat("00.000");

    public static String format(double value)
    {
        if(value > 0)
        {
            return " " + df.format(value);
        }
        else
        {
            return df.format(value);
        }
    }

    public static float[] generateAFSK1200(boolean[] symbols, int samplesPerSymbol, float gain)
    {
        float[] samples = new float[symbols.length * samplesPerSymbol];

        Oscillator oscillator = new Oscillator(1200.0, (double)samplesPerSymbol * 1200);

        for(int x = 0; x < symbols.length; x++)
        {
            oscillator.setFrequency(symbols[x] ? 1200.0 : 1800.0);

            for(int y = 0; y < samplesPerSymbol; y++)
            {
                Complex sample = new Complex(oscillator.inphase(), oscillator.quadrature());
                sample.multiply(gain);
                samples[x * samplesPerSymbol + y] = sample.inphase();
                oscillator.rotate();
            }
        }

        return samples;
    }

    public static void main(String[] args)
    {
        int sampleSize = 6;

        FloatAveragingBuffer avg1200 = new FloatAveragingBuffer(sampleSize);
        FloatAveragingBuffer avg1800 = new FloatAveragingBuffer(sampleSize);

        IOscillator oscillator1200 = new Oscillator(1200.0, 7200.0);
        IOscillator oscillator1800 = new Oscillator(1800.0, 7200.0);
        oscillator1200.generateReal(1);
        oscillator1800.generateReal(1);

        float[] reference1200 = oscillator1200.generateReal(sampleSize);
        float[] reference1800 = oscillator1800.generateReal(sampleSize);

        boolean[] symbols = new boolean[]{true,false,true,false,true,false,true,false,true,true,true,false,true,false,
            false,false,true,false,false,false,false,true,false,true,true,true,true,false,true,true,false,true,false,
            true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,
            false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,
            true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,
            false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,
            true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,
            false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,true,false,
            true,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,
            false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false};

        float[] samples = generateAFSK1200(symbols, 6, 0.5f);

        mLog.debug("Length:" + samples.length);

        int offset = 0;

        float correlation1200 = 0.0f;
        float correlation1800 = 0.0f;

        while(offset + sampleSize < samples.length)
        {
            correlation1200 = 0.0f;
            correlation1800 = 0.0f;

            for(int x = 0; x < sampleSize; x++)
            {
                correlation1200 += reference1200[x] * samples[x + offset];
                correlation1800 += reference1800[x] * samples[x + offset];
            }

            correlation1200 = avg1200.get(FastMath.abs(correlation1200));
            correlation1800 = avg1800.get(FastMath.abs(correlation1800));

            mLog.debug(offset + " 1200:" + format(correlation1200) + " 1800:" + format(correlation1800) +
            " Decision: " + (correlation1200 > correlation1800 ? "0" : "1"));

            offset++;
        }
    }
}
