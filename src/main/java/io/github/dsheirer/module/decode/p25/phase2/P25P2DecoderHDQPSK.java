/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.gain.ComplexFeedForwardGainControl;
import io.github.dsheirer.dsp.psk.DQPSKGardnerDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.FrequencyCorrectionSyncMonitor;
import io.github.dsheirer.dsp.psk.pll.PLLBandwidth;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * P25 Phase 2 HDQPSK 2-timeslot Decoder
 */
public class P25P2DecoderHDQPSK extends P25P2Decoder implements IdentifierUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2DecoderHDQPSK.class);
    protected static final float SYMBOL_TIMING_GAIN = 0.1f;
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected DQPSKGardnerDemodulator mQPSKDemodulator;
    protected CostasLoop mCostasLoop;
    protected P25P2MessageFramer mMessageFramer;
    private ComplexFeedForwardGainControl mAGC = new ComplexFeedForwardGainControl(32);
    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    private ComplexFIRFilter2 mBasebandFilter;
    private DecodeConfigP25Phase2 mDecodeConfigP25Phase2;
    private FrequencyCorrectionSyncMonitor mFrequencyCorrectionSyncMonitor;

    public P25P2DecoderHDQPSK(DecodeConfigP25Phase2 decodeConfigP25Phase2)
    {
        super(6000.0);
        setSampleRate(25000.0);
        mDecodeConfigP25Phase2 = decodeConfigP25Phase2;
    }

    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        mBasebandFilter = new ComplexFIRFilter2(getBasebandFilter());
        mCostasLoop = new CostasLoop(getSampleRate(), getSymbolRate());
        mCostasLoop.setPLLBandwidth(PLLBandwidth.BW_300);

        mInterpolatingSampleBuffer = new InterpolatingSampleBuffer(getSamplesPerSymbol(), SYMBOL_TIMING_GAIN);
        mQPSKDemodulator = new DQPSKGardnerDemodulator(mCostasLoop, mInterpolatingSampleBuffer);

        if(mMessageFramer != null)
        {
            getDibitBroadcaster().removeListener(mMessageFramer);
        }

        //The Costas Loop receives symbol-inversion correction requests when detected.
        //The PLL gain monitor receives sync detect/loss signals from the message framer
        mMessageFramer = new P25P2MessageFramer(mCostasLoop, DecoderType.P25_PHASE2.getProtocol().getBitRate());

        if(mDecodeConfigP25Phase2 !=null)
        {
            mMessageFramer.setScrambleParameters(mDecodeConfigP25Phase2.getScrambleParameters());
        }

        mFrequencyCorrectionSyncMonitor = new FrequencyCorrectionSyncMonitor(mCostasLoop, this);
        mMessageFramer.setSyncDetectListener(mFrequencyCorrectionSyncMonitor);
        mMessageFramer.setListener(getMessageProcessor());
        mMessageFramer.setSampleRate(sampleRate);

        mQPSKDemodulator.setSymbolListener(getDibitBroadcaster());
        getDibitBroadcaster().addListener(mMessageFramer);
    }

    /**
     * Primary method for processing incoming complex sample buffers
     * @param reusableComplexBuffer containing channelized complex samples
     */
    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        //User accounting of the incoming buffer is handled by the filter
        ReusableComplexBuffer basebandFiltered = filter(reusableComplexBuffer);

        //User accounting of the incoming buffer is handled by the gain filter
        ReusableComplexBuffer gainApplied = mAGC.filter(basebandFiltered);

        mMessageFramer.setCurrentTime(reusableComplexBuffer.getTimestamp());

        //User accounting of the filtered buffer is handled by the demodulator
        mQPSKDemodulator.receive(gainApplied);
    }

    /**
     * Filters the complex buffer and returns a new reusable complex buffer with the filtered contents.
     * @param reusableComplexBuffer to filter
     * @return filtered complex buffer
     */
    protected ReusableComplexBuffer filter(ReusableComplexBuffer reusableComplexBuffer)
    {
        //User accounting of the incoming buffer is handled by the filter
        return mBasebandFilter.filter(reusableComplexBuffer);
    }

    /**
     * Constructs a baseband filter for this decoder using the current sample rate
     */
    private float[] getBasebandFilter()
    {
        //Attempt to reuse a cached (ie already-designed) filter if available
        float[] filter = mBasebandFilters.get(getSampleRate());

        if(filter == null)
        {
            FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(50000.0)
                .passBandCutoff(6500)
                .passBandAmplitude(1.0)
                .passBandRipple(0.005)
                .stopBandAmplitude(0.0)
                .stopBandStart(7200)
                .stopBandRipple(0.01)
                .build();

            try
            {
                filter = FilterFactory.getTaps(specification);
            }
            catch(FilterDesignException fde)
            {
                mLog.error("Couldn't design low pass baseband filter for sample rate: " + getSampleRate());
            }

            if(filter != null)
            {
                mBasebandFilters.put(getSampleRate(), filter);
            }
            else
            {
                throw new IllegalStateException("Couldn't design a C4FM baseband filter for sample rate: " + getSampleRate());
            }
        }

        return filter;
    }

    @Override
    protected void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                mCostasLoop.reset();
                setSampleRate(sourceEvent.getValue().doubleValue());
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                //Reset the PLL if/when the tuner PPM changes so that we can re-lock
                mCostasLoop.reset();
                break;
        }
    }

    /**
     * Resets this decoder to prepare for processing a new channel
     */
    @Override
    public void reset()
    {
        mCostasLoop.reset();
    }

    @Override
    public void start()
    {
        super.start();

        //Refresh the scramble parameters each time we start in case they change
        if(mDecodeConfigP25Phase2 != null && mDecodeConfigP25Phase2.getScrambleParameters() != null &&
            !mDecodeConfigP25Phase2.isAutoDetectScrambleParameters())
        {
            mMessageFramer.setScrambleParameters(mDecodeConfigP25Phase2.getScrambleParameters());
        }
    }

    public static class MessageErrorListener implements Listener<IMessage>
    {
        private int mMessageCount = 0;
        private int mBitErrorCount = 0;

        @Override
        public void receive(IMessage iMessage)
        {
            if(!(iMessage instanceof SyncLossMessage) && !(iMessage instanceof EncryptionSynchronizationSequence))
            {
                mMessageCount++;
            }

            if(iMessage instanceof MacMessage)
            {
                mBitErrorCount += ((MacMessage)iMessage).getBitErrorCount();
            }

            mLog.debug("[" + mMessageCount + " | " + mBitErrorCount + "] " + iMessage.toString());
        }
    }

    public static void main(String[] args)
    {
//        String path = "/media/denny/500G1EXT4/RadioRecordings/APCO25P2/CNYICC/";
//        String input = "CNYICC_Rome_CNYICC_154_250_baseband_20190322_180331_good.wav";
//        String output = "CNYICC_ROME_154_250_8";

        String path = "/home/denny/SDRTrunk/recordings/";
        String input = "CNYICC_Site_P25P2_151.37_baseband_20200221_110306.wav";
        String output = "P25P2_test";

        ScrambleParameters scrambleParameters = new ScrambleParameters(781824, 686, 677); //CNYICC
        DecodeConfigP25Phase2 decodeConfigP25Phase2 = new DecodeConfigP25Phase2();
        decodeConfigP25Phase2.setScrambleParameters(scrambleParameters);

        P25P2DecoderHDQPSK decoder = new P25P2DecoderHDQPSK(decodeConfigP25Phase2);
        decoder.setMessageListener(new MessageErrorListener());

//        decoder.setMessageListener(new Listener<IMessage>()
//        {
//            @Override
//            public void receive(IMessage iMessage)
//            {
//                mLog.debug(iMessage.toString());
//            }
//        });
        decoder.start();

//        BinaryRecorder recorder = new BinaryRecorder(Path.of(path), output, Protocol.APCO25_PHASE2);
//        decoder.setBufferListener(recorder.getReusableByteBufferListener());
//        recorder.start();

        File file = new File(path + input);

        boolean running = true;

        try(ComplexWaveSource source = new ComplexWaveSource(file))
        {
            decoder.setSampleRate(50000.0);
            source.setListener(decoder);
            source.start();

            while(running)
            {
                source.next(200, true);
            }
        }
        catch(IOException e)
        {
            mLog.error("Error", e);
            running = false;
        }

//        recorder.stop();
    }

    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return new Listener<IdentifierUpdateNotification>()
        {
            @Override
            public void receive(IdentifierUpdateNotification identifierUpdateNotification)
            {
                if(identifierUpdateNotification.getIdentifier().getForm() == Form.SCRAMBLE_PARAMETERS && mMessageFramer != null)
                {
                    ScrambleParameters scrambleParameters = (ScrambleParameters)identifierUpdateNotification.getIdentifier().getValue();
                    mMessageFramer.setScrambleParameters(scrambleParameters);
                }
            }
        };
    }
}
