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

import io.github.dsheirer.dsp.symbol.Dibit;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import org.jtransforms.fft.FloatFFT_1D;

/**
 * DQPSK equalizer for processing decoded symbol (ie phase angle) estimates.
 */
public class DQPSKComplexLmsEqualizer
{
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 = (float)(Math.PI / 4.0);
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 = 3.0f * IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");

    private int L;
    private float[] zI;
    private float[] zQ;
    private float[] aI;
    private float[] aQ;
    private float[] qI;
    private float[] qQ;
    private int mPointer;
    private float mStepSize = 0.1f;  //TODO: make this variable based on sync vs no-sync

    private FloatFFT_1D mFFT;

    private float mYI;
    private float mYQ;

    /**
     * Constructs an instance
     * @param length
     */
    public DQPSKComplexLmsEqualizer(int length)
    {
        L = length + 1; //Length
        zI = new float[L * 2]; //Transmitted symbols - I rail
        zQ = new float[L * 2]; //Transmitted symbols - Q rail
        aI = new float[L * 2]; //Decisions - I rail
        aQ = new float[L * 2]; //Decisions - Q rail
        qI = new float[L]; //Equalizer taps - I rail
        qQ = new float[L]; //Equalizer taps - Q rail
        mFFT = new FloatFFT_1D(L);

        //Set center equalizer tap to unity gain initially.
        if(L % 2 == 1)
        {
            qI[L / 2] = 1.0f;
            qQ[L / 2] = 1.0f;
        }
        else
        {
            qI[L / 2] = 0.5f;
            qQ[L / 2] = 0.5f;
            qI[L / 2 + 1] = 0.5f;
            qQ[L / 2 + 1] = 0.5f;
        }
    }

    public float process(Dibit interimDecision, float symbolI, float symbolQ)
    {
        zI[mPointer] = symbolI;
        zI[mPointer + L] = symbolI;

        zQ[mPointer] = symbolQ;
        zQ[mPointer + L] = symbolQ;

        aI[mPointer] = interimDecision.getIdealI();
        aI[mPointer + L] = interimDecision.getIdealI();
        aQ[mPointer] = interimDecision.getIdealQ();
        aQ[mPointer + L] = interimDecision.getIdealQ();

        mPointer++;
        mPointer %= L;

        //Step 1: calculate the filtered value
        float yI = 0.0f;
        float yQ = 0.0f;

        for(int l = 0; l < L; l++)
        {
            yI += (zI[l + mPointer] * qI[l]);
            yQ += (zQ[l + mPointer] * qQ[l]);
        }

        mYI = yI;
        mYQ = yQ;

        //Step 2: calculate the error between the actual and the equalized symbols.
        float eI = aI[L / 2 + mPointer] - yI;
        float eQ = aQ[L / 2 + mPointer] - yQ;

        //Update the taps
        float adjustmentI = 0.0f;
        float adjustmentQ = 0.0f;

        //Step 3: update the taps
        for(int l = 0; l < L; l++)
        {
            adjustmentI = 2 * mStepSize * eI * zI[l + mPointer];
            qI[l] = qI[l] + adjustmentI;
            adjustmentQ = 2 * mStepSize * eQ * zQ[l + mPointer];
            qQ[l] = qQ[l] + adjustmentQ;
        }


        return (float)Math.atan2(yQ, yI);
    }

    public float getYI()
    {
        return mYI;
    }

    public float getYQ()
    {
        return mYQ;
    }

    public void syncDetected(float[] sync)
    {

//TODO: fix this up to update both I and Q
        float[] ideal = Arrays.copyOfRange(sync, sync.length - L, sync.length);
        float[] actual = Arrays.copyOfRange(zI, mPointer, mPointer + L);

        int offset = 0;
        //Overwrite the symbol decision vector
        for(int x = 0; x < L; x++)
        {
            offset = mPointer + x;
            offset %= L;
            aI[offset] = sync[x];
            aI[offset + L] = sync[x];
        }

        //FFT calculations
        mFFT.realForward(ideal);
        mFFT.realForward(actual);

        float[] delta = new float[ideal.length];

        for(int x = 0; x < ideal.length; x++)
        {
            delta[x] = ideal[x] - actual[x];
        }

//        System.out.println(" IDEAL: " + Arrays.toString(ideal));
//        System.out.println("ACTUAL: " + Arrays.toString(actual));
//        System.out.println("DELTA1: " + Arrays.toString(delta));
        mFFT.realInverse(delta, true);
//        System.out.println("DELTA2: " + Arrays.toString(delta));
        qI = delta;
    }

    private void printTaps()
    {
        System.out.println("I:" + Arrays.toString(qI));
        System.out.println("Q:" + Arrays.toString(qQ));
    }

    public static void main(String[] args)
    {
        int length = 8;
        int delay = length / 2;
        DQPSKComplexLmsEqualizer equalizer = new DQPSKComplexLmsEqualizer(length);

        Dibit[] symbols = new Dibit[500];
        Random random = new Random();

        for(int x = 0; x < symbols.length; x++)
        {
            int lookup = random.nextInt(4);
            symbols[x] = Dibit.values()[lookup];
        }

        float[] transmittedI = new float[symbols.length];
        float[] transmittedQ = new float[symbols.length];
        float inducedError = 1.0f;
        float[] interferorI = new float[symbols.length];
        float[] interferorQ = new float[symbols.length];
        float interferorDelayI = (float)Math.toRadians(25);
        float interfererGainI = .4f;

        for(int x = 0; x < transmittedI.length; x++)
        {
            transmittedI[x] = symbols[x].getIdealI() * inducedError;
            transmittedQ[x] = symbols[x].getIdealQ() * inducedError;
        }

        float mixerQ = (float)(Math.sin(interferorDelayI) * interfererGainI);
        float mixerI = (float)(Math.cos(interferorDelayI) * interfererGainI);

        for(int x = 0; x < transmittedI.length; x++)
        {
            interferorI[x] = (transmittedI[x] * mixerI) - (transmittedQ[x] * mixerQ);
            interferorQ[x] = (transmittedI[x] * mixerQ) + (transmittedQ[x] * mixerI);
        }

        for(int x = 0; x < transmittedI.length; x++)
        {
            transmittedI[x] += interferorI[x];
            transmittedQ[x] += interferorQ[x];
        }

        float[] equalized = new float[symbols.length];

        for(int x = 0; x < equalized.length; x++)
        {
            equalized[x] = equalizer.process(symbols[x], transmittedI[x], transmittedQ[x]);

            if(x > delay)
            {
                float transmit = (float)Math.atan2(transmittedQ[x - delay], transmittedI[x - delay]);
                try
                {
                    System.out.println(x + "," +
                            DECIMAL_FORMAT.format(transmittedI[x - delay]) + "," +
                            DECIMAL_FORMAT.format(equalizer.getYI()) + "," +
                            DECIMAL_FORMAT.format(transmittedQ[x - delay]) + "," +
                            DECIMAL_FORMAT.format(equalizer.getYQ()) + ", TRANSMITTED:" +
                            DECIMAL_FORMAT.format(transmit) + ", IDEAL:" +
                            DECIMAL_FORMAT.format(symbols[x - delay].getIdealPhase()) + ", EQUALIZED:" +
                            DECIMAL_FORMAT.format(equalized[x]) + ", E-DELTA:" +
                            DECIMAL_FORMAT.format(symbols[x - delay].getIdealPhase() - equalized[x]) + ", T-DELTA:" +
                            DECIMAL_FORMAT.format(symbols[x - delay].getIdealPhase() - transmit) + ",   " +
                            symbols[x - delay]);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        equalizer.printTaps();

        System.out.println("");
        for(Dibit dibit: Dibit.values())
        {
            System.out.println(dibit + " I:" + dibit.getIdealI() + " Q:" + dibit.getIdealQ());
        }
    }
}
