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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.dsp.c4fm.DQPSKDifferentialDemodulator;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.p25.audio.P25P1AudioModule;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IByteBufferProvider;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO25 Phase 1 Linear Simulcast Modulation (LSM) decoder.  Decimates incoming sample buffers to 4-8 samples per
 * symbol.  Employs a flat baseband (channel) filter without a matched (root raised cosine) filter since the differential
 * demodulator is a limiter-discriminator receiver where narrowing the bandwidth ahead of the discriminator distorts the
 * phase trajectory and degrades performance (see BASEBAND_FILTER_PASS_BAND).  The differential demodulator
 * (mDemodulator) removes the coarse carrier offset and differentially demodulates the filtered complex baseband (I/Q)
 * sample stream into the phase change across each symbol period, in the same radians per sample domain as the C4FM
 * decoder.  This allows the LSM decoder to share the C4FM symbol processor (mSymbolProcessor) that performs sync
 * detection, timing correction, equalization and balance (PLL) residual mistune correction.  Message framer performs
 * message framing and message creation, using the demodulator's soft symbol reliabilities to weight soft decision
 * decoding.  A registered message listener receives the detected and framed messages.
 *
 * As a child of the FeedbackDecoder, this decoder provides periodic PLL measurements to the tuner for automatic PPM
 * correction.  It also provides a stream of demodulated soft symbols (in radians) for display to the user.
 */
public class P25P1DecoderLSM extends FeedbackDecoder implements IByteBufferProvider, IComplexSamplesListener,
        ISourceEventListener, Listener<ComplexSamples>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(P25P1DecoderLSM.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    //Baseband filter settings.  Testing against LSM recordings: a 7250 Hz pass band passes adjacent channel
    //interference and a matched (4800 baud root raised cosine) filter increases symbol errors.  Without injected noise,
    //flat pass bands from 4800 to 5600 Hz perform best, while narrower pass bands perform better with injected noise.
    //A 4400 Hz pass band balances both: relative to 5200 Hz, 0.3% fewer valid messages without injected noise and
    //25-50% more valid messages at 3-0 dB carrier to noise.
    private static final double BASEBAND_FILTER_PASS_BAND = 4400.0; //Hertz
    private static final double BASEBAND_FILTER_STOP_BAND = 5600.0; //Hertz
    private static final int SYMBOL_RATE = 4800;

    private final P25P1SymbolProcessorLSM mSymbolProcessor;
    private final DQPSKDifferentialDemodulator mDemodulator = new DQPSKDifferentialDemodulator();
    private final P25P1MessageFramer mMessageFramer = new P25P1MessageFramer();
    private final P25P1MessageProcessor mMessageProcessor = new P25P1MessageProcessor();
    private IRealDecimationFilter mDecimationFilterI;
    private IRealDecimationFilter mDecimationFilterQ;
    private IRealFilter mBasebandFilterI;
    private IRealFilter mBasebandFilterQ;

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
    }

    /**
     * Constructs an instance
     */
    public P25P1DecoderLSM()
    {
        mMessageProcessor.setMessageListener(getMessageListener());
        mMessageFramer.setPhaseWrappedSoftSymbols(true);
        mSymbolProcessor = new P25P1SymbolProcessorLSM(this, mMessageFramer);
    }

    @Override
    public String getProtocolDescription()
    {
        return "P25 Phase 1 LSM";
    }

    /**
     * Sets the sample rate and configures internal components.
     * @param sampleRate of the channel to decode
     */
    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= SYMBOL_RATE * 2)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >9600 (2 * " +
                    SYMBOL_RATE + " symbol rate)");
        }

        int decimation = 1;

        //Identify decimation that gets as close to 4.0 Samples Per Symbol as possible (4800 x 4.0 = 19.2 kHz)
        while((sampleRate / decimation) >= 38400)
        {
            decimation *= 2;
        }

        mDecimationFilterI = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mDecimationFilterQ = DecimationFilterFactory.getRealDecimationFilter(decimation);

        float decimatedSampleRate = (float)sampleRate / decimation;

        //Set the decimated sample rate to use for PLL error reporting.
        setDecimatedSampleRate(decimatedSampleRate);

        float[] basebandTaps = FilterFactory.getRemezLowPass(decimatedSampleRate, BASEBAND_FILTER_PASS_BAND,
                BASEBAND_FILTER_STOP_BAND);
        mBasebandFilterI = FilterFactory.getRealFilter(basebandTaps);
        mBasebandFilterQ = FilterFactory.getRealFilter(basebandTaps);
        float samplesPerSymbol = decimatedSampleRate / (float)SYMBOL_RATE;

        mDemodulator.setSamplesPerSymbol(samplesPerSymbol);
        mSymbolProcessor.setSamplesPerSymbol(samplesPerSymbol);

        mMessageFramer.setListener(mMessageProcessor);
    }

    /**
     * Primary method for processing incoming complex sample buffers
     * @param samples containing channelized complex samples
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        //Update the message framer with the timestamp from the incoming sample buffer.
        mMessageFramer.setTimestamp(samples.timestamp());

        float[] i = samples.i();
        float[] q = samples.q();

        i = mDecimationFilterI.decimateReal(i);
        q = mDecimationFilterQ.decimateReal(q);

        i = mBasebandFilterI.filter(i);
        q = mBasebandFilterQ.filter(q);

        //Differential demodulation: phase change across each symbol period (radians per sample)
        float[] demodulated = mDemodulator.demodulate(i, q, mSymbolProcessor.isSynchronized());

        //Process demodulated samples into symbols and apply message sync detection and framing.  The soft symbol
        //reliabilities weight the soft decision decoding.
        mSymbolProcessor.process(demodulated, mDemodulator.getReliabilities());
    }

    /**
     * Includes the coarse carrier offset that the differential demodulator removes ahead of the symbol processor,
     * since the symbol processor's PLL error only represents the residual offset.  The symbol processor invokes this
     * method when the message framer decodes a valid NID, which also verifies the demodulator's carrier offset range.
     * @param pllError residual carrier offset (radians per sample) from the symbol processor
     */
    @Override
    public void processPLLError(float pllError)
    {
        mDemodulator.offsetVerified();
        super.processPLLError(pllError - mDemodulator.getFrequencyOffset());
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the symbol processor
     */
    @Override
    public void setBufferListener(Listener<ByteBuffer> listener)
    {
        mSymbolProcessor.setBufferListener(listener);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the symbol processor
     */
    @Override
    public void removeBufferListener(Listener<ByteBuffer> listener)
    {
        setBufferListener(null);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the symbol processor
     */
    @Override
    public boolean hasBufferListeners()
    {
        return mSymbolProcessor.hasBufferListener();
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return this::process;
    }

    @Override
    public void start()
    {
        super.start();
        mMessageFramer.start();
    }

    @Override
    public void stop()
    {
        super.stop();
        mMessageFramer.stop();
        mSymbolProcessor.dispose();
    }

    /**
     * Process source events
     */
    private void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
                mSymbolProcessor.resetBalance();
                mDemodulator.reset();
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                //Tuner PPM changes are small (auto-PPM steps are <= 10 Hz) and the carrier offset estimator and symbol
                //processor balance track the change.  Resetting would discard the carrier offset estimate and force an
                //offset ambiguity search when the offset exceeds the +/- 600 Hz fourth power estimator range.
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                setSampleRate(sourceEvent.getValue().doubleValue());
                break;
        }
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    public static void main(String[] args)
    {
        LOGGER.info("Starting ...");

//        String directory = "D:\\DQPSK Equalizer Research - P25\\"; //Windows
        String directory = "/run/media/denny/T9/DQPSK Equalizer Research - P25/"; //Linux

//        String file = directory + "P25-S2-LSM-20241225_040119_460500000_CNYICC_Onondaga_Onondaga_CC_0_baseband.wav";
//        String file = directory + "P25-S4-LSM-TCH-Data-20250105_141051_453587500_CNYICC_Onondaga_T-Onondaga_CC_38_baseband.wav";
//        String file = directory + "P25-S5-LSM-CCH-20250923_034304_460500000_CNYICC_Onondaga_Onondaga-CC_0_baseband.wav";
//        String file = directory + "P25-S6-LSM-CCH-20251025_071546_460500000_CNYICC_Onondaga_Onondaga-CC_0_baseband.wav";
//        String file = directory + "P25-S7-LSM-CCH-20261027_030041_460475000_Central-New-York-Interoperable-Communications-Consortium-(CNYICC)_Madison-Simulcast_Control_51_baseband.wav";
//        String file = directory + "P25-S8-LSM-TCH-20261027_030138_460250000_Central-New-York-Interoperable-Communications-Consortium-(CNYICC)_Madison-Simulcast_T-Control_52_baseband.wav";
//        String file = directory + "P25-S9-LSM-TCH-20261027_030248_460175000_Central-New-York-Interoperable-Communications-Consortium-(CNYICC)_Madison-Simulcast_T-Control_53_baseband.wav";
//        String file = directory + "P25-S10-LSM-TCH-20261027_031432_460175000_Central-New-York-Interoperable-Communications-Consortium-(CNYICC)_Madison-Simulcast_T-Control_11_baseband.wav";
        String file = directory + "P25-S11-LSM-TCH-20261027_031603_460250000_Central-New-York-Interoperable-Communications-Consortium-(CNYICC)_Madison-Simulcast_T-Control_12_baseband.wav";
        boolean autoReplay = false;

        P25P1DecoderLSM decoder = new P25P1DecoderLSM();
        decoder.start();

        UserPreferences userPreferences = new UserPreferences();
        P25P1AudioModule audio1 = new P25P1AudioModule(userPreferences, new AliasList(""));

        decoder.setMessageListener(new Listener<>()
        {
            private long mBitCounter = 1;
            private int mBitErrorCounter;
            private int mValidMessageCounter;
            private int mTotalMessageCounter;
            private int mMessageCounter = 1;

            @Override
            public void receive(IMessage iMessage)
            {
                int errors = 0;

                audio1.receive(iMessage);

                if(iMessage instanceof P25P1Message message)
                {
                    mBitCounter += 288;

                    if(message.getMessage() != null)
                    {
                        errors = message.getMessage().getCorrectedBitCount();
                    }

                    mTotalMessageCounter++;

                    if(mTotalMessageCounter == 492)
                    {
                        int a = 0;
                    }
                    if(message.isValid())
                    {
                        mBitErrorCounter += Math.max(errors, 0);
                        mValidMessageCounter++;
                    }
                }

                double bitErrorRate = (double)mBitErrorCounter / (double)mBitCounter * 100.0;

                boolean logEverything = true;
                boolean logIdles = true;

                if(logEverything)
                {
                    if(iMessage.toString().contains("PLACEHOLDER"))
                    {
                        int a = 0;
                    }

                    if(iMessage instanceof SyncLossMessage)
                    {
                        System.out.println(" >> MESSAGE: TS" + iMessage.getTimeslot() + " " + iMessage + " \t\t[" + errors + " | " + mBitErrorCounter + " | Valid:" + mValidMessageCounter + " Total:" + mTotalMessageCounter + " Msgs] Rate [" + DECIMAL_FORMAT.format(bitErrorRate) + " %]");
                    }
                    else
                    {
                        System.out.println(++mMessageCounter + " >> MESSAGE: TS" + iMessage.getTimeslot() + " " + iMessage + " \t\t[" + errors + " | " + mBitErrorCounter + " | Valid:" + mValidMessageCounter + " Total:" + mTotalMessageCounter + " Msgs] Rate [" + DECIMAL_FORMAT.format(bitErrorRate) + " %]");
                    }
                }
                else
                {
                    if(logIdles && iMessage instanceof SyncLossMessage)
                    {
                        System.out.println(">>MESSAGE: TS" + iMessage.getTimeslot() + " " + iMessage + " \t\t[" + errors + " | " + mBitErrorCounter + " | Valid:" + mValidMessageCounter + " Total:" + mTotalMessageCounter + " Msgs] Rate [" + DECIMAL_FORMAT.format(bitErrorRate) + " %]");
                    }
                }
            }
        });

        try(ComplexWaveSource source = new ComplexWaveSource(new File(file), autoReplay))
        {
            source.setListener(iNativeBuffer -> {
                Iterator<ComplexSamples> it = iNativeBuffer.iterator();

                while(it.hasNext())
                {
                    decoder.receive(it.next());
                }
            });
            source.start();
            decoder.setSampleRate(source.getSampleRate());

            while(true)
            {
                source.next(2048, true);
            }
        }
        catch(IOException ioe)
        {
            LOGGER.error("Error", ioe);
        }

        LOGGER.info("Finished");
    }
}
