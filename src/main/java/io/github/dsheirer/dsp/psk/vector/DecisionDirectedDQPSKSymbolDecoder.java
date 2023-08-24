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

import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorFactory;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.complex.Complex;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes symbols from an array of I/Q sample data that has been differentially decoded to an offset QPSK constellation
 * such as for DMR or P25 Phase 1.
 */
public class DecisionDirectedDQPSKSymbolDecoder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionDirectedDQPSKSymbolDecoder.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");
    private static final DecimalFormat I_Q_FORMAT = new DecimalFormat("+#0.0000000;-#0.0000000");


    private static final double OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 = Math.PI / 4.0;
    private static final double OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 = 3.0 * OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final double OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 = -OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final double OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 = -OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
    private static final float TIMING_ERROR_LOOP_GAIN = 0.01f;

    private final CostasLoop mPLL;
    private int mPllIndex;
    private final Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private float mSymbolRate = 4800.0f;
    private float mSamplesPerSymbol;
    private float mSymbolPointer = 1.5f;

    //debug
    private Variance mVariance = new Variance();
    private Mean mVarianceMean = new Mean();
    private Mean mMean = new Mean();
    private Mean mCumulativeMean = new Mean();
    private float mMuTracker = 0.0f;

    /**
     * Constructor
     * @param symbolRate to evaluate
     */
    public DecisionDirectedDQPSKSymbolDecoder(float symbolRate)
    {
        mSymbolRate = symbolRate;
        mPLL = new CostasLoop(50000f, mSymbolRate);
    }

    /**
     * Sets/updates the samples per symbol value.
     * @param samplesPerSymbol to set
     */
    public void setSampleRate(float sampleRate)
    {
//        mPLL.setSampleRate(sampleRate);
        mSamplesPerSymbol = sampleRate / mSymbolRate;
    }

    /**
     * Processes the I/Q data to produce an array of decoded Dibit symbols.
     *
     * Note: the length of the i/q sample array arguments must be at least 8 larger than the length argument to allow
     * the interpolator to have an overlap of 4x preceding and 4x following samples around the interpolation point.
     * @param iSamples inphase differentially decoded samples
     * @param qSamples quadrature differentially decoded samples
     * @return array of decoded Dibit symbols.
     */
    public List<Dibit> decode(float[] iSamples, float[] qSamples, int length)
    {
        mPllIndex = 4; //The first 4 samples will have been despun during the previous decode iteration

        //debug
        mVariance.clear();
        mMean.clear();

        List<Dibit> symbols = new ArrayList<>();
        int preceding, following;
        float phaseError = 0.0f;
        float adjustment = 0.0f;
        float totalAdjustment = 0.0f;

        float debugCurrentSample = 0;
        float debugActualSymbol = 0;
        float debugIdealSymbol = 0;
        float debugTimingError = 0;

        StringBuilder sb = new StringBuilder();

        float inphaseTemp;

        for(int x = 0; x < length; x++)
        {
            //Despin enough samples to support the interpolator with 4x preceding and 4x following sampels
            while(mPllIndex < (x + 8))
            {
                Complex pll = mPLL.incrementAndGetCurrentVector();
                inphaseTemp = Complex.multiplyInphase(iSamples[mPllIndex], qSamples[mPllIndex], pll.inphase(), pll.quadrature());
                qSamples[mPllIndex] = Complex.multiplyQuadrature(iSamples[mPllIndex], qSamples[mPllIndex], pll.inphase(), pll.quadrature());
                iSamples[mPllIndex] = inphaseTemp;
                mPllIndex++;
            }
            //debug
            debugCurrentSample = (float)Math.atan2(qSamples[x + 4], iSamples[x + 4]);

            if(mSymbolPointer >= mSamplesPerSymbol)
            {
                following = (int)mSymbolPointer;
                preceding = following - 1;

                //If preceding & current symbols are in different quadrants then we potentially have noise, and we don't
                //want to adjust symbol timing, so don't interpolate, just use the preceding symbol as a symbol decision.
                if((iSamples[preceding] < 0 && iSamples[following] > 0) ||
                   (iSamples[preceding] > 0 && iSamples[following] < 0) ||
                   (qSamples[preceding] < 0 && qSamples[following] > 0) ||
                   (qSamples[preceding] > 0 && qSamples[following] < 0))
                {
                    Dibit symbol = decode(iSamples[preceding], qSamples[preceding]);

                    //debug
                    debugActualSymbol = (float)Math.atan2(qSamples[preceding], iSamples[preceding]);
                    debugIdealSymbol = (float)getRadians(symbol);
                    debugTimingError = 0f;

                    symbols.add(decode(iSamples[preceding], qSamples[preceding]));
                }
                else
                {
                    //Calculate the fractional portion for the interpolation point
                    float fractional = mSymbolPointer - following;

                    //Decode the symbol
                    float iSymbol = mInterpolator.filter(iSamples, x, fractional);
                    float qSymbol = mInterpolator.filter(qSamples, x, fractional);
                    Dibit symbol = decode(iSymbol, qSymbol);

                    symbols.add(symbol);
                    mSymbolPointer -= mSamplesPerSymbol;

                    //Calculate symbol decision point timing error and adjust
                    phaseError = calculatePhaseError(symbol, iSamples[preceding], iSymbol, iSamples[following], qSymbol);
                    mPLL.adjust(phaseError);
                    mVariance.increment(phaseError);
                    mMean.increment(phaseError);
                    mCumulativeMean.increment(phaseError);
                    adjustment = phaseError * TIMING_ERROR_LOOP_GAIN;

                    //debug
                    debugActualSymbol = (float)Math.atan2(qSymbol, iSymbol);
                    debugIdealSymbol = (float)getRadians(symbol);
                    debugTimingError = phaseError;

//                    mSymbolPointer += adjustment;

                    mMuTracker += adjustment;
                    mMuTracker %= mSamplesPerSymbol;

                    totalAdjustment += adjustment;
                }
            }

            sb.append(DECIMAL_FORMAT.format(debugCurrentSample)).append(", ");
            sb.append(DECIMAL_FORMAT.format(debugActualSymbol)).append(", "); //Actual symbol
            sb.append(DECIMAL_FORMAT.format(debugIdealSymbol)).append(", "); //Ideal symbol
            sb.append(DECIMAL_FORMAT.format(debugTimingError)).append(", "); //Timing Error
            sb.append(DECIMAL_FORMAT.format(mVariance.getResult())); //Total Timing Error
//            sb.append(DECIMAL_FORMAT.format(mMuTracker));
            sb.append("\n");

            mSymbolPointer++;
        }

        mVarianceMean.increment(mVariance.getResult());

//        System.out.println(DECIMAL_FORMAT.format(mVariance.getResult()) + ", " +
//                DECIMAL_FORMAT.format(mVarianceMean.getResult()) + ", " +
//                DECIMAL_FORMAT.format(mMean.getResult()) + ", " +
//                DECIMAL_FORMAT.format(mCumulativeMean.getResult()) + ", " +
//                DECIMAL_FORMAT.format(mMuTracker));

//        System.out.println(DECIMAL_FORMAT.format(mMuTracker));

//        System.out.println(sb.toString());
        return symbols;
    }

    /**
     * Calculates the timing error of the symbol decision by comparing the i1 preceding and i3 following samples to
     * determine vector rotation and derive a timing error from the difference between the optimal sampling point (radians)
     * and the actual symbol sample point (radians).
     *
     * @param symbol decision made from the interpolated I/Q sample pair
     * @param i1 inphase sample - preceding
     * @param i2 inphase sample - interpolated symbol
     * @param i3 inphase sample - following
     * @param q2 quadrature sample - interpolated symbol
     * @return calculated timing error where a positive value indicates that timing needs increased and vice-versa.
     */
    private float calculatePhaseError(Dibit symbol, float i1, float i2, float i3, float q2)
    {
        //Calculate radians for the current symbol
        double radians = Math.atan2(q2, i2);

        switch(symbol)
        {
            case D00_PLUS_1:
                if(i3 < i1) //positive/counter-clockwise rotation
                {
                    return (float)(OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 - radians);
                }
                else
                {
                    return (float)(radians - OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1);
                }
            case D01_PLUS_3:
                if(i3 > i1) //positive/counter-clockwise rotation
                {
                    return (float)(OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 - radians);
                }
                else
                {
                    return (float)(radians - OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3);
                }
            case D10_MINUS_1:
                if(i3 > i1) //positive/counter-clockwise rotation
                {
                    return (float)(OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 - radians);
                }
                else
                {
                    return (float)(radians - OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1);
                }
            case D11_MINUS_3:
                if(i3 < i1) //positive/counter-clockwise rotation
                {
                    return (float)(OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 - radians);
                }
                else
                {
                    return (float)(radians - OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3);
                }
        }

        return 0.0f;
    }

    /**
     * Decodes the symbol represented by the inphase and quadrature sample values.
     * @param i inphase value
     * @param q quadrature value.
     * @return decoded symbol.
     */
    private static Dibit decode(float i, float q)
    {
        if(q > 0.0f)
        {
            if(i > 0.0f)
            {
                return Dibit.D00_PLUS_1;
            }
            else
            {
                return Dibit.D01_PLUS_3;
            }

        }
        else
        {
            if(i > 0.0f)
            {
                return Dibit.D10_MINUS_1;
            }
            else
            {
                return Dibit.D11_MINUS_3;
            }
        }
    }

    /**
     * Utility method to identify optimal sampling point radians
     * @param symbol to decode
     * @return radians
     */
    private static double getRadians(Dibit symbol)
    {
        return switch(symbol)
        {
            case D00_PLUS_1 -> OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
            case D01_PLUS_3 -> OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
            case D10_MINUS_1 -> OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1;
            case D11_MINUS_3 -> OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3;
        };
    }
}
