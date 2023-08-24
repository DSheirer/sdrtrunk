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
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import java.text.DecimalFormat;
import java.util.Random;
import org.jtransforms.fft.FloatFFT_1D;

/**
 * DQPSK equalizer for processing decoded symbol (ie phase angle) estimates.
 */
public class DQPSKLmsEqualizer
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");
    private static final float MAX_CENTER_TAP = 1.0f;
    private static final float MAX_OFF_CENTER_TAP = 0.5f;
//    private static final float MAX_VALUE = (float)(3.0 / Math.sqrt(2));

    private int L;
    private float[] z;
    private float[] a;
    private float[] q;
    private int mPointer;
    private float mStepSize = 0.1f;  //TODO: make this variable based on sync vs no-sync
    private boolean mConstrain = true;
    private FloatFFT_1D mFFT;

    /**
     * Constructs an instance
     * @param length
     */
    public DQPSKLmsEqualizer(int length)
    {
        L = 2 * length + 1; //Length

        z = new float[L * 2]; //Transmitted symbols
        a = new float[L * 2]; //Decisions
        q = new float[L]; //Equalizer taps
        mFFT = new FloatFFT_1D(L);

        //Set center equalizer tap to unity gain initially.
        if(L % 2 == 1)
        {
            q[L / 2] = 1.0f;
        }
    }

    public void processNoUpdate(Dibit interimDecision, float symbol)
    {
//        symbol = Math.min(symbol, MAX_VALUE);
//        symbol = Math.max(symbol, -MAX_VALUE);
        z[mPointer] = symbol;
        z[mPointer + L] = symbol;
        a[mPointer] = interimDecision.getIdealPhase();
        a[mPointer + L] = interimDecision.getIdealPhase();
        mPointer++;
        mPointer %= L;
    }

    public float process(Dibit interimDecision, float symbol)
    {
//        symbol = Math.min(symbol, MAX_VALUE);
//        symbol = Math.max(symbol, -MAX_VALUE);
        z[mPointer] = symbol;
        z[mPointer + L] = symbol;
        a[mPointer] = interimDecision.getIdealPhase();
        a[mPointer + L] = interimDecision.getIdealPhase();
        mPointer++;
        mPointer %= L;

        //Step 1: calculate the filtered value
        float y = 0.0f;

        for(int l = 0; l < L; l++)
        {
            y += ((z[l + mPointer] - a[l + mPointer]) * q[l]);
        }

        if(Float.isNaN(y) || Float.isInfinite(y))
        {
            y = 0f;
        }

        float e = a[L / 2 + mPointer] - y;

        //Update the taps
        for(int l = 0; l < L; l++)
        {
            if(l != L/2)
            {
                float adjustment = 2 * mStepSize * e * (z[l + mPointer] - a[l + mPointer]);
                q[l] = q[l] + adjustment;

                if(Float.isNaN(q[l]) || Float.isInfinite(q[l]))
                {
                    q[l] = 0.0f;
                }
            }

        }

        return y;
    }

    private float[] getTaps()
    {
        return q;
    }

    public void syncDetected(Dibit[] dibits)
    {
        int pointer = mPointer;

        float[] tapError = new float[L];
        tapError[0] = z[pointer] - a[pointer];
        float totalError = tapError[0];

        pointer++;
        for(int x = 0; x < dibits.length; x++)
        {
            pointer %= L;
            float existing = a[pointer];
            float existingPlusL = a[pointer + L];
            a[pointer] = dibits[x].getIdealPhase();
            a[pointer + L] = dibits[x].getIdealPhase();

            //Recalculate the error contribution for each tap and total error
            tapError[x + 1] = z[pointer] - a[pointer];
            totalError += Math.abs(tapError[x]);
            pointer++;
        }

        //Calculate the error contribution that causes the main tap value to not be error-free and proportionally
        //distribute that error across the other taps in the form of corrective tap error.
        float mainTapError = tapError[L / 2];

        float[] objective = new float[L];
        float totalObjective = 0.0f;
        float totalPercentage = 0.0f;
        for(int x = 0; x < q.length; x++)
        {
            if(x != L/2) //Don't update the main tap ... leave it at unity (1.0)
            {
                q[x] = -mainTapError / 24 / tapError[x];
            }
        }

        //Step 1: calculate the filtered value
        float y = 0.0f;

        for(int l = 0; l < L; l++)
        {
            y += ((z[mPointer + l] - a[mPointer + 1]) * q[l]);
        }

        float yMinusMain = y - (z[mPointer + (L / 2)]);

        float actual = a[mPointer + (L / 2)];
        float transmitted = z[mPointer + (L / 2)];
        float delta = actual - y;


    }

    public static void main(String[] args)
    {
        int length = 12;
        int delay = length;

        DQPSKLmsEqualizer equalizer = new DQPSKLmsEqualizer(length);

        Dibit[] symbols = new Dibit[length + 24];
        Random random = new Random();

        int symbolPointer = 0;
        for(int x = 0; x < length; x++)
        {
            int lookup = random.nextInt(4);
            symbols[symbolPointer++] = Dibit.values()[lookup];
        }

        Dibit[] sync = DMRSyncPattern.BASE_STATION_DATA.toDibits();
        for(Dibit dibit: sync)
        {
            symbols[symbolPointer++] = dibit;
        }


        float[] transmitted = new float[symbols.length];

        for(int x = 0; x < transmitted.length; x++)
        {
            float ideal = symbols[x].getIdealPhase();
            transmitted[x] = ideal;
        }

        float[] equalized = new float[symbols.length];

        for(int x = 0; x < equalized.length; x++)
        {
            equalized[x] = equalizer.process(symbols[x], transmitted[x]);
//
//            if(x > delay)
//            {
//                try
//                {
//                    System.out.println(x + ", TRANS:" +
//                            DECIMAL_FORMAT.format(transmitted[x - delay]) + ", EQUAL:" +
//                            DECIMAL_FORMAT.format(equalized[x]) + ", IDEAL:" +
//                            DECIMAL_FORMAT.format(symbols[x - delay].getIdealPhase()) + ", ERROR:" +
//                            DECIMAL_FORMAT.format(symbols[x - delay].getIdealPhase() - equalized[x]) + "," +
//                            symbols[x - delay] + "    TAPS:" + Arrays.toString(equalizer.getTaps()));
//                }
//                catch(Exception e)
//                {
//                    e.printStackTrace();
//                }
//            }
        }

        equalizer.syncDetected(DMRSyncPattern.BASE_STATION_DATA.toDibits());

        System.out.println("\n");
    }
}
