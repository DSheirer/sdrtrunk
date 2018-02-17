/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import io.github.dsheirer.dsp.gain.ComplexFeedForwardGainControl;
import io.github.dsheirer.dsp.psk.LSMDemodulator;
import io.github.dsheirer.dsp.psk.QPSKSymbolDecoder;
import io.github.dsheirer.instrument.tap.TapGroup;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.ComplexBufferToStreamConverter;
import io.github.dsheirer.sample.complex.IComplexBufferListener;
import io.github.dsheirer.source.tuner.frequency.FrequencyChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class P25_LSMDecoder extends P25Decoder implements IComplexBufferListener
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
    private QPSKSymbolDecoder mQPSKSlicer = new QPSKSymbolDecoder();
    private P25MessageFramer mMessageFramer;

    public P25_LSMDecoder(AliasList aliasList)
    {
        super(aliasList);

        mBasebandFilter = new ComplexFIRFilter_CB_CB(FilterFactory.getLowPass(
            48000, 7250, 8000, 60, WindowType.HANNING, true), 1.0f);

        mBasebandFilter.setListener(mStreamConverter);

        mStreamConverter.setListener(mAGC);

        mAGC.setListener(mLSMDemodulator);

        mLSMDemodulator.setSymbolListener(mQPSKSlicer);

        mMessageFramer = new P25MessageFramer(aliasList, mLSMDemodulator.getCostasLoop());
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
    public void setFrequencyChangeListener(Listener<FrequencyChangeEvent> listener)
    {
    }

    @Override
    public void removeFrequencyChangeListener()
    {
    }

    @Override
    public Listener<FrequencyChangeEvent> getFrequencyChangeListener()
    {
        return new Listener<FrequencyChangeEvent>()
        {
            @Override
            public void receive(FrequencyChangeEvent frequencyChangeEvent)
            {
                //Ignored
            }
        };
    }

    @Override
    public Listener<ComplexBuffer> getComplexBufferListener()
    {
        return mBasebandFilter;
    }

    public Modulation getModulation()
    {
        return Modulation.CQPSK;
    }

    @Override
    public void reset()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop()
    {
        // TODO Auto-generated method stub

    }
}
