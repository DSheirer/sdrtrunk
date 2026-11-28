/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.dsp.filter.channelizer.ols;

import io.github.dsheirer.dsp.filter.ChannelFilterDesigner;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Channel specifications and the decoder front ends (decimation by 2 and baseband filter, built exactly as the stock
 * decoders build them at a 50 kHz channel rate) that an overlap-save channel could make unnecessary.
 */
public enum DecoderSpec
{
    /** P25 Phase 1 CQPSK/LSM: P25P1DecoderLSM baseband filter, Remez 4400/5600 Hz at 25 kHz */
    P25_LSM(4400, 5600),
    /** P25 Phase 1 C4FM: P25P1DecoderC4FM baseband filter, Remez 5200/6500 Hz at 25 kHz */
    P25_C4FM(5200, 6500),
    /** NBFM 12.5 kHz: NBFMDecoder channel filter, ChannelFilterDesigner 5000/6250 Hz at 25 kHz */
    NBFM_12_5(5000, 6250);

    public static final double DECODER_INPUT_RATE = 50000.0;
    public static final double DECODER_RATE = 25000.0;

    private final double mPass;
    private final double mStop;

    DecoderSpec(double pass, double stop)
    {
        mPass = pass;
        mStop = stop;
    }

    public double getPass()
    {
        return mPass;
    }

    public double getStop()
    {
        return mStop;
    }

    /**
     * Baseband filter taps at the decoder rate, as the stock decoder designs them.
     */
    public float[] getBasebandTaps()
    {
        return switch(this)
        {
            case NBFM_12_5 -> ChannelFilterDesigner.design(DECODER_RATE, 12500);
            default -> FilterFactory.getRemezLowPass(DECODER_RATE, mPass, mStop);
        };
    }

    /**
     * Creates the decoder's own front end: decimate I and Q by 2, then the baseband filter.
     */
    public Listener<ComplexSamples> createFrontEnd(Listener<ComplexSamples> listener)
    {
        return new FrontEnd(getBasebandTaps(), listener);
    }

    private static class FrontEnd implements Listener<ComplexSamples>
    {
        private final IRealDecimationFilter mDecimationI = DecimationFilterFactory.getRealDecimationFilter(2);
        private final IRealDecimationFilter mDecimationQ = DecimationFilterFactory.getRealDecimationFilter(2);
        private final IRealFilter mFilterI;
        private final IRealFilter mFilterQ;
        private final Listener<ComplexSamples> mListener;

        FrontEnd(float[] taps, Listener<ComplexSamples> listener)
        {
            mFilterI = FilterFactory.getRealFilter(taps);
            mFilterQ = FilterFactory.getRealFilter(taps.clone());
            mListener = listener;
        }

        @Override
        public void receive(ComplexSamples samples)
        {
            float[] i = mFilterI.filter(mDecimationI.decimateReal(samples.i()));
            float[] q = mFilterQ.filter(mDecimationQ.decimateReal(samples.q()));
            mListener.receive(new ComplexSamples(i, q, samples.timestamp()));
        }
    }
}
