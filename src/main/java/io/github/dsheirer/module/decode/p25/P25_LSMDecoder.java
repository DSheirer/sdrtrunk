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
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import io.github.dsheirer.dsp.gain.ComplexFeedForwardGainControl;
import io.github.dsheirer.dsp.psk.LSMDemodulator;
import io.github.dsheirer.dsp.psk.QPSKPolarSlicer;
import io.github.dsheirer.instrument.tap.Tap;
import io.github.dsheirer.instrument.tap.TapGroup;
import io.github.dsheirer.instrument.tap.stream.ComplexSampleTap;
import io.github.dsheirer.instrument.tap.stream.ComplexTap;
import io.github.dsheirer.instrument.tap.stream.DibitTap;
import io.github.dsheirer.instrument.tap.stream.QPSKTap;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.ComplexBufferToStreamConverter;
import io.github.dsheirer.sample.complex.reusable.IReusableComplexBufferListener;
import io.github.dsheirer.sample.complex.reusable.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class P25_LSMDecoder extends P25Decoder implements IReusableComplexBufferListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25_LSMDecoder.class);

    /* Instrumentation Taps */
    private static final String INSTRUMENT_BASEBAND_FILTER_OUTPUT = "Tap Point: Baseband Filter Output";
    private static final String INSTRUMENT_AGC_OUTPUT = "Tap Point: AGC Output";
    private static final String INSTRUMENT_LSM_DEMODULATOR_OUTPUT = "Tap Point: LSM Demodulator Output";
    private static final String INSTRUMENT_QPSK_SLICER_OUTPUT = "Tap Point: QPSK Slicer Output";

    private List<TapGroup> mAvailableTaps;

    private ComplexFIRFilter_CB_CB mBasebandFilter;
    private ComplexBufferToStreamConverter mStreamConverter = new ComplexBufferToStreamConverter();
    private ComplexFeedForwardGainControl mAGC =
        new ComplexFeedForwardGainControl(32);
    private LSMDemodulator mLSMDemodulator = new LSMDemodulator();
    private QPSKPolarSlicer mQPSKSlicer = new QPSKPolarSlicer();
    private P25MessageFramer mMessageFramer;
    private ReusableBufferListener mReusableBufferListener = new ReusableBufferListener();

    public P25_LSMDecoder(AliasList aliasList)
    {
        super(aliasList);

        mBasebandFilter = new ComplexFIRFilter_CB_CB(FilterFactory.getLowPass(
            48000, 7250, 8000, 60, WindowType.HANN, true), 1.0f);

        mBasebandFilter.setListener(mStreamConverter);

        mStreamConverter.setListener(mAGC);

        mAGC.setListener(mLSMDemodulator);

        mLSMDemodulator.setSymbolListener(mQPSKSlicer);

        mMessageFramer = new P25MessageFramer(aliasList, mLSMDemodulator);
        mQPSKSlicer.addListener(mMessageFramer);

        mMessageFramer.setListener(getMessageProcessor());
    }

    public void dispose()
    {
        super.dispose();

        mBasebandFilter.dispose();
        mBasebandFilter = null;

        mStreamConverter.dispose();
        mStreamConverter = null;

        mAGC.dispose();
        mAGC = null;

        mLSMDemodulator.dispose();
        mLSMDemodulator = null;

        mQPSKSlicer.dispose();
        mQPSKSlicer = null;

        mMessageFramer.dispose();
        mMessageFramer = null;
    }

    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return mReusableBufferListener;
    }

    public Modulation getModulation()
    {
        return Modulation.CQPSK;
    }

    /**
     * Provides a list of instrumentation taps for monitoring internal processing
     */
    @Override
    public List<TapGroup> getTapGroups()
    {
        if(mAvailableTaps == null)
        {
            mAvailableTaps = new ArrayList<>();

            TapGroup group = new TapGroup("P25 LSM Decoder");

            group.add(new ComplexTap(INSTRUMENT_BASEBAND_FILTER_OUTPUT, 0, 1.0f));
            group.add(new ComplexTap(INSTRUMENT_AGC_OUTPUT, 0, 1.0f));
            group.add(new QPSKTap(INSTRUMENT_LSM_DEMODULATOR_OUTPUT, 0, 1.0f));
            group.add(new DibitTap(INSTRUMENT_QPSK_SLICER_OUTPUT, 0, 0.1f));

            mAvailableTaps.add(group);

            if(mLSMDemodulator != null)
            {
                mAvailableTaps.addAll(mLSMDemodulator.getTapGroups());
            }
        }

        return mAvailableTaps;
    }

    /**
     * Adds the instrumentation tap
     */
    @Override
    public void registerTap(Tap tap)
    {
        if(mLSMDemodulator != null)
        {
            mLSMDemodulator.registerTap(tap);
        }

        switch(tap.getName())
        {
            case INSTRUMENT_BASEBAND_FILTER_OUTPUT:
                ComplexSampleTap baseband = (ComplexSampleTap)tap;
                mStreamConverter.setListener(baseband);
                baseband.setListener(mAGC);
                break;
            case INSTRUMENT_AGC_OUTPUT:
                ComplexSampleTap agcSymbol = (ComplexSampleTap)tap;
                mAGC.setListener(agcSymbol);
                agcSymbol.setListener(mLSMDemodulator);
                break;
            case INSTRUMENT_LSM_DEMODULATOR_OUTPUT:
                QPSKTap qpsk = (QPSKTap)tap;
                mLSMDemodulator.setSymbolListener(qpsk);
                qpsk.setListener(mQPSKSlicer);
                break;
            case INSTRUMENT_QPSK_SLICER_OUTPUT:
                mQPSKSlicer.addListener((DibitTap)tap);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized tap: " +
                    tap.getName());
        }
    }

    /**
     * Removes the instrumentation tap
     */
    @Override
    public void unregisterTap(Tap tap)
    {
        if(mLSMDemodulator != null)
        {
            mLSMDemodulator.unregisterTap(tap);
        }

        switch(tap.getName())
        {
            case INSTRUMENT_BASEBAND_FILTER_OUTPUT:
                mStreamConverter.setListener(mAGC);
                break;
            case INSTRUMENT_AGC_OUTPUT:
                mAGC.setListener(mLSMDemodulator);
                break;
            case INSTRUMENT_LSM_DEMODULATOR_OUTPUT:
                mLSMDemodulator.setSymbolListener(mQPSKSlicer);
                break;
            case INSTRUMENT_QPSK_SLICER_OUTPUT:
                mQPSKSlicer.removeListener((DibitTap)tap);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized tap: " +
                    tap.getName());
        }
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return null;
    }

    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {

    }

    @Override
    public void removeSourceEventListener()
    {

    }

    public class ReusableBufferListener implements Listener<ReusableComplexBuffer>
    {
        @Override
        public void receive(ReusableComplexBuffer reusableComplexBuffer)
        {
            float[] samples = reusableComplexBuffer.getCopyOfSamples();

            //TODO: redesign the filter chain so that we can simply pass a float array ...

            mBasebandFilter.receive(new ComplexBuffer(samples));
            reusableComplexBuffer.decrementUserCount();
        }
    }
}
