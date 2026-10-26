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
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.fm.FMDiscriminatorReliability;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO25 Phase 1 decoder.  Decimates incoming sample buffers to as close as possible to 25 kHz for ~5 samples per
 * symbol.  Employs baseband and pulse shaping filters.  Demodulates the complex baseband (I/Q) sample stream using
 * SIMD differential demodulation of the entire sample stream.  The C4FM demodulator (mSymbolProcessor) performs sync
 * detection, timing correction and PLL signal mistune correction.  Message framer performs message framing and message
 * creation.  A registered message listener receives the detected and framed messages.
 *
 * As a child of the FeedbackDecoder, this decoder provides periodic PLL measurements to the tuner for automatic PPM
 * correction.  It also provides a stream of demodulated soft symbols (in radians) for display to the user.
 *
 * Soft decision decoding is weighted by each soft symbol's reliability (see FMDiscriminatorReliability), calculated
 * from the power of the complex samples spanned by the receive filter.
 */
public class P25P1DecoderC4FM extends FeedbackDecoder implements IByteBufferProvider, IComplexSamplesListener,
        ISourceEventListener, Listener<ComplexSamples>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(P25P1DecoderC4FM.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final int SYMBOL_RATE = 4800;
    //TIA-102.BAAA C4FM transmit raised cosine filter roll-off
    private static final double RECEIVE_FILTER_ALPHA = 0.2;
    //Band limited integrate and dump receive filter settings.  Noise injection testing against P25 C4FM recordings:
    //stopband starts of 3300 - 4000 Hz perform similarly, a 5-symbol span is too short, and 12 symbols adds nothing.
    private static final double RECEIVE_FILTER_STOP_BAND_START = 3600.0; //Hertz
    private static final int RECEIVE_FILTER_SYMBOL_SPAN = 8;
    private static final double BASEBAND_FILTER_PASS_BAND = 5200.0; //Hertz
    private static final double BASEBAND_FILTER_STOP_BAND = 6500.0; //Hertz

    private final P25P1SymbolProcessorC4FM mSymbolProcessor;
    private final P25P1MessageFramer mMessageFramer = new P25P1MessageFramer();
    private final P25P1MessageProcessor mMessageProcessor = new P25P1MessageProcessor();
    private IDemodulator mDemodulator;
    private IRealDecimationFilter mDecimationFilterI;
    private IRealDecimationFilter mDecimationFilterQ;
    private IRealFilter mBasebandFilterI;
    private IRealFilter mBasebandFilterQ;
    private IRealFilter mReceiveFilter;
    private FMDiscriminatorReliability mReliability;

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
    }

    public P25P1DecoderC4FM()
    {
        mMessageProcessor.setMessageListener(getMessageListener());
        mSymbolProcessor = new P25P1SymbolProcessorC4FM(this, mMessageFramer);
    }

    @Override
    public String getProtocolDescription()
    {
        return "P25 Phase 1 C4FM";
    }

    /**
     * Sets the sample rate and configures internal decoder components.
     * @param sampleRate of the channel to decode
     */
    public void setSampleRate(double sampleRate)
    {
        float samplesPerSymbol = (float)sampleRate / SYMBOL_RATE;

        if(samplesPerSymbol <= 4.0f)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be at least " + (SYMBOL_RATE * 4) + " Hz");
        }

        int decimation = 1;

        //Identify decimation that gets us as close to 5.0 Samples Per Symbol as possible
        while(samplesPerSymbol >= 10.0)
        {
            decimation *= 2;
            samplesPerSymbol /= 2;
        }

        mDecimationFilterI = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mDecimationFilterQ = DecimationFilterFactory.getRealDecimationFilter(decimation);

        float decimatedSampleRate = (float)sampleRate / decimation;

        //Set the decimated sample rate to use for PLL error reporting.
        setDecimatedSampleRate(decimatedSampleRate);

        //Receive filter: integrate and dump (sinc) response across the signal band with a stopband above the signal band
        //to reject FM discriminator noise that an integrate and dump filter's sidelobes would otherwise pass.
        float[] receiveFilterTaps = FilterFactory.getBandLimitedIntegrateAndDump(decimatedSampleRate, SYMBOL_RATE,
                RECEIVE_FILTER_ALPHA, RECEIVE_FILTER_STOP_BAND_START, RECEIVE_FILTER_SYMBOL_SPAN);
        mReceiveFilter = new RealFIRFilter(receiveFilterTaps);
        mReliability = new FMDiscriminatorReliability(receiveFilterTaps);

        float[] basebandTaps = FilterFactory.getRemezLowPass(decimatedSampleRate, BASEBAND_FILTER_PASS_BAND,
                BASEBAND_FILTER_STOP_BAND);
        mBasebandFilterI = FilterFactory.getRealFilter(basebandTaps);
        mBasebandFilterQ = FilterFactory.getRealFilter(basebandTaps);

        mDemodulator = FmDemodulatorFactory.getFmDemodulator();
        mSymbolProcessor.setSamplesPerSymbol(samplesPerSymbol);

        //TODO: update P25 message Framer and Processor like we do in NXDN constructor
        mMessageFramer.setListener(mMessageProcessor);

        mMessageProcessor.setMessageListener(getMessageListener());
    }

    /**
     * Converts the soft symbol reliabilities to soft decision decoding weights.  Noise injection testing against a P25
     * C4FM control channel recording found that the square root of the reliability produced 1.6% more valid messages
     * at 9 dB carrier to noise (in-band) than the reliability, and 6.3% more than unweighted decoding.  The P25 LSM
     * decoder uses the reliability, which tested 5% better than the square root against LSM recordings.
     * @param reliabilities to convert (modified in place)
     * @return weights
     */
    private static float[] getWeights(float[] reliabilities)
    {
        for(int x = 0; x < reliabilities.length; x++)
        {
            reliabilities[x] = (float)Math.sqrt(reliabilities[x]);
        }

        return reliabilities;
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

        float[] i = mDecimationFilterI.decimateReal(samples.i());
        float[] q = mDecimationFilterQ.decimateReal(samples.q());

        i = mBasebandFilterI.filter(i);
        q = mBasebandFilterQ.filter(q);

        float[] demodulated = mDemodulator.demodulate(i, q);
        demodulated = mReceiveFilter.filter(demodulated);

        //Process demodulated samples into symbols and apply message sync detection and framing.  The soft symbol
        //reliabilities weight the soft decision decoding.
        mSymbolProcessor.process(demodulated, getWeights(mReliability.calculate(i, q)));
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
        mSymbolProcessor.setBufferListener(null);
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
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                //Tuner PPM changes are small (auto-PPM steps are <= 10 Hz) and the symbol processor balance tracks the
                //change.  Resetting drops the symbol processor synchronization, and the symbol timing tracking with it,
                //until the next sync detection, which costs messages on channels with long sync intervals (ie P25
                //voice).
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

        //Note: this file has lots of dropped sample periods across each burst, with otherwise high SNR good decodes
//        String file = directory + "P25-S1-Conventional-repeater-20241115_212221_469325000_QPS_Digital_Kynoch_Kynoch_Digital_59_baseband.wav";
        String file = directory + "P25-S3-C4FM-20241225_040459_152517500_NYSEG_Onondaga_Control_30_baseband.wav";

        boolean autoReplay = false;

        P25P1DecoderC4FM decoder = new P25P1DecoderC4FM();
        decoder.start();

        UserPreferences userPreferences = new UserPreferences();
        P25P1AudioModule audio1 = new P25P1AudioModule(userPreferences, new AliasList(""));

        decoder.setMessageListener(new Listener<>()
        {
            private long mBitCounter = 1;
            private int mBitErrorCounter;
            private int mValidMessageCounter;
            private int mTotalMessageCounter;

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
                    System.out.println(">>MESSAGE: TS" + iMessage.getTimeslot() + " " + iMessage + " \t\t[" + errors + " | " + mBitErrorCounter + " | Valid:" + mValidMessageCounter + " Total:" + mTotalMessageCounter + " Msgs] Rate [" + DECIMAL_FORMAT.format(bitErrorRate) + " %]");
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

            boolean hasData = true;

            while(hasData)
            {
                try
                {
                    source.next(2048, true);
                }
                catch(IOException e)
                {
                    LOGGER.info("End of file: " + e.getLocalizedMessage());
                    hasData = false;
                }
            }
        }
        catch(FileNotFoundException e)
        {
            LOGGER.error("File not found: " + file);
        }
        catch(IOException ioe)
        {
            LOGGER.error("Error", ioe);
        }

        LOGGER.info("Finished");
    }
}
