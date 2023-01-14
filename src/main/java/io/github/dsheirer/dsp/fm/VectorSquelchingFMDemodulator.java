/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.dsp.squelch.PowerSquelch;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

/**
 * FM Demodulator for demodulating complex samples and producing demodulated floating point samples.
 *
 * Implements listener of source events to process runtime squelch threshold change request events
 * which are forwarded to the power squelch control.
 */
public class VectorSquelchingFMDemodulator extends VectorFMDemodulator
        implements ISquelchingFmDemodulator, Listener<SourceEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(VectorSquelchingFMDemodulator.class);
    private static final float TWO = 2.0f;
    private PowerSquelch mPowerSquelch;
    private boolean mSquelchChanged = false;

    /**
     * Creates an FM demodulator instance with a default gain of 1.0.
     */
    public VectorSquelchingFMDemodulator(float alpha, float threshold, int ramp)
    {
        mPowerSquelch = new PowerSquelch(alpha, threshold, ramp);
    }

    public void reset()
    {
    }

    /**
     * Registers the listener to receive notifications of squelch change events from the power squelch.
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mPowerSquelch.setSourceEventListener(listener);
    }

    /**
     * Demodulates the complex (I/Q) sample arrays
     * @param i inphase samples
     * @param q quadrature samples
     * @return demodulated real samples
     */
    @Override
    public float[] demodulate(float[] i, float[] q)
    {
        setSquelchChanged(false);

        float[] demodulated = super.demodulate(i, q);
        float[] magnitude = getMagnitude(i, q);

        for(int x = 0; x < i.length; x++)
        {
            mPowerSquelch.process(magnitude[x]);

            if(!(mPowerSquelch.isUnmuted() || mPowerSquelch.isDecay()))
            {
                demodulated[x] = ZERO;
            }

            if(mPowerSquelch.isSquelchChanged())
            {
                setSquelchChanged(true);
            }
        }

        return demodulated;
    }

    /**
     * Calculates the magnitude of the complex I/Q sample arrays using SIMD vector intrinsics.
     * @param i sample array
     * @param q sample array
     * @return magnitude array
     */
    public static float[] getMagnitude(float[] i, float[] q)
    {
        VectorUtilities.checkComplexArrayLength(i, q, VECTOR_SPECIES);

        float[] magnitudes = new float[i.length];
        FloatVector iVector, qVector;

        for(int samplePointer = 0; samplePointer < i.length; samplePointer += VECTOR_SPECIES.length())
        {
            iVector = FloatVector.fromArray(VECTOR_SPECIES, i, samplePointer);
            qVector = FloatVector.fromArray(VECTOR_SPECIES, q, samplePointer);
            iVector.pow(TWO).add(qVector.pow(TWO)).intoArray(magnitudes, samplePointer);
        }

        return magnitudes;
    }

    /**
     * Sets the threshold for squelch control
     * @param threshold (dB)
     */
    public void setSquelchThreshold(double threshold)
    {
        mPowerSquelch.setSquelchThreshold(threshold);
    }

    /**
     * Indicates if the squelch state has changed during the processing of buffer(s)
     */
    public boolean isSquelchChanged()
    {
        return mSquelchChanged;
    }

    /**
     * Sets or resets the squelch changed flag.
     */
    private void setSquelchChanged(boolean changed)
    {
        mSquelchChanged = changed;
    }

    /**
     * Indicates if the squelch state is currently muted
     */
    public boolean isMuted()
    {
        return mPowerSquelch.isMuted();
    }

    @Override
    public void receive(SourceEvent sourceEvent)
    {
        //Only forward squelch threshold change request events
        if(sourceEvent.getEvent() == SourceEvent.Event.REQUEST_CHANGE_SQUELCH_THRESHOLD)
        {
            mPowerSquelch.receive(sourceEvent);
        }
        else if(sourceEvent.getEvent() == SourceEvent.Event.REQUEST_CURRENT_SQUELCH_THRESHOLD)
        {
            mPowerSquelch.receive(sourceEvent);
        }
    }

    public static void main(String[] args)
    {
        int iterations = 3_000_000;
        int sampleSize = 2048;

        Random random = new Random();

        float[] iSamples = new float[sampleSize];
        float[] qSamples = new float[sampleSize];
        for(int x = 0; x < iSamples.length; x++)
        {
            iSamples[x] = random.nextFloat() * 2.0f - 1.0f;
            qSamples[x] = random.nextFloat() * 2.0f - 1.0f;
        }


        float POWER_SQUELCH_ALPHA_DECAY = 0.0004f;
        float POWER_SQUELCH_THRESHOLD_DB = -78.0f;
        int POWER_SQUELCH_RAMP = 4;

        ScalarFMDemodulator fm = new ScalarFMDemodulator();
        VectorFMDemodulator vectorFM = new VectorFMDemodulator();
        ScalarSquelchingFMDemodulator sqLegacy = new ScalarSquelchingFMDemodulator(POWER_SQUELCH_ALPHA_DECAY, POWER_SQUELCH_THRESHOLD_DB, POWER_SQUELCH_RAMP);
        VectorSquelchingFMDemodulator vectorSq = new VectorSquelchingFMDemodulator(POWER_SQUELCH_ALPHA_DECAY, POWER_SQUELCH_THRESHOLD_DB, POWER_SQUELCH_RAMP);

        boolean validation = false;

        if(validation)
        {
            float[] noSqFmSamples = fm.demodulate(iSamples, qSamples);
            float[] vectorNoSqFmSamples = vectorFM.demodulate(iSamples, qSamples);
            float[] sqFmSamples = sqLegacy.demodulate(iSamples, qSamples);
            float[] vectorSqFmSamples = vectorSq.demodulate(iSamples, qSamples);
            System.out.println("       NO SQ FM:" + Arrays.toString(noSqFmSamples));
            System.out.println("VECTOR NO SQ FM:" + Arrays.toString(vectorNoSqFmSamples));
            System.out.println("          SQ FM:" + Arrays.toString(sqFmSamples));
            System.out.println("   VECTOR SQ FM:" + Arrays.toString(vectorSqFmSamples));
        }
        else
        {
            System.out.println("Test Starting ...");
            long start = System.currentTimeMillis();

            double accumulator = 0.0;

            for(int i = 0; i < iterations; i++)
            {
//                float[] samples = sqLegacy.demodulate(iSamples, qSamples);
                float[] samples = vectorSq.demodulate(iSamples, qSamples);
                accumulator += samples[3];
            }

            double elapsed = System.currentTimeMillis() - start;

            DecimalFormat df = new DecimalFormat("0.000");
            System.out.println("Accumulator: " + accumulator);
            System.out.println("Test Complete.  Elapsed Time: " + df.format(elapsed / 1000.0d) + " seconds");
        }
    }
}