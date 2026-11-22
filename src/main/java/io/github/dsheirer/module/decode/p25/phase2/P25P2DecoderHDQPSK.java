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
package io.github.dsheirer.module.decode.p25.phase2;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.state.MultiChannelState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.c4fm.DQPSKDifferentialDemodulator;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.p25.P25FrequencyBandPreloadDataContent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.audio.P25P2AudioModule;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IByteBufferProvider;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO25 Phase 2 HDQPSK 2-timeslot decoder.  Decimates incoming sample buffers to 4-8 samples per symbol.  Employs a
 * flat baseband (channel) filter without a matched (root raised cosine) filter since the differential demodulator is a
 * limiter-discriminator receiver where narrowing the bandwidth ahead of the discriminator distorts the phase trajectory.
 * The differential demodulator (mDemodulator) removes the coarse carrier offset and differentially demodulates the
 * filtered complex baseband (I/Q) sample stream into the phase change across each symbol period, in the same radians
 * per sample domain as the C4FM decoders.  This allows the decoder to share the C4FM symbol processor framework
 * (mSymbolProcessor) that performs sync detection, timing correction, equalization and balance (PLL) residual mistune
 * correction.  The message framer frames super frame fragments and a registered message listener receives the decoded
 * messages.
 *
 * As a child of the FeedbackDecoder, this decoder provides periodic PLL measurements to the tuner for automatic PPM
 * correction.  It also provides a stream of demodulated soft symbols (in radians) for display to the user.
 */
public class P25P2DecoderHDQPSK extends FeedbackDecoder implements ISourceEventListener, IComplexSamplesListener,
        Listener<ComplexSamples>, IByteBufferProvider, IdentifierUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2DecoderHDQPSK.class);
    //Baseband filter settings.  The filter precedes the differential demodulator's carrier offset removal, so the pass
    //band must accommodate the carrier offset.  Testing against Phase 2 recordings (pass/stop: total valid MAC messages)
    //5500/7000: 9210, 6000/7500: 9273, 6500/7800: 9294, 7000/8500: 9262.  Narrower pass bands lost the most valid
    //messages on a recording with a 1.8 kHz carrier offset.
    private static final double BASEBAND_FILTER_PASS_BAND = 6500.0; //Hertz
    private static final double BASEBAND_FILTER_STOP_BAND = 7800.0; //Hertz
    private static final int SYMBOL_RATE = 6000;
    private final P25P2SymbolProcessor mSymbolProcessor;
    private final DQPSKDifferentialDemodulator mDemodulator = new DQPSKDifferentialDemodulator();
    private final P25P2MessageFramer mMessageFramer = new P25P2MessageFramer();
    private final P25P2MessageProcessor mMessageProcessor = new P25P2MessageProcessor();
    private final DecodeConfigP25Phase2 mDecodeConfigP25Phase2;
    private IRealDecimationFilter mDecimationFilterI;
    private IRealDecimationFilter mDecimationFilterQ;
    private IRealFilter mBasebandFilterI;
    private IRealFilter mBasebandFilterQ;

    /**
     * Constructs an instance
     * @param decodeConfigP25Phase2 with optional scramble parameters
     */
    public P25P2DecoderHDQPSK(DecodeConfigP25Phase2 decodeConfigP25Phase2)
    {
        mDecodeConfigP25Phase2 = decodeConfigP25Phase2;
        mMessageProcessor.setMessageListener(getMessageListener());
        mMessageFramer.setListener(mMessageProcessor);
        mSymbolProcessor = new P25P2SymbolProcessor(this, mMessageFramer);

        if(mDecodeConfigP25Phase2 != null)
        {
            mMessageFramer.setScrambleParameters(mDecodeConfigP25Phase2.getScrambleParameters());
        }

        setSampleRate(25000.0);
    }

    @Override
    public String getProtocolDescription()
    {
        return "P25 Phase 2";
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE2;
    }

    /**
     * Preloads P25 frequency bands when this is a traffic channel, since they are not transmitted on the traffic
     * channel and the decoder state needs accurate frequency values to pass to the traffic channel manager for
     * call event tracking.
     * @param preLoadDataContent with known frequency bands.
     */
    @Subscribe
    public void process(P25FrequencyBandPreloadDataContent preLoadDataContent)
    {
        mMessageProcessor.preload(preLoadDataContent);
    }

    /**
     * Sets the sample rate and configures internal components.
     * @param sampleRate of the channel to decode
     */
    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= SYMBOL_RATE * 2)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >12000 (2 * " +
                    SYMBOL_RATE + " symbol rate)");
        }

        int decimation = 1;

        //Identify decimation that gets as close to 4.0 Samples Per Symbol as possible (6000 x 4.0 = 24 kHz)
        while((sampleRate / decimation) >= 48000)
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

        //Differential demodulation: phase change across each symbol period (radians per sample)
        float[] demodulated = mDemodulator.demodulate(i, q, mSymbolProcessor.isSynchronized());

        //Process demodulated samples into symbols and apply sync detection and message framing.
        mSymbolProcessor.process(demodulated);
    }

    /**
     * Includes the coarse carrier offset that the differential demodulator removes ahead of the symbol processor,
     * since the symbol processor's PLL error only represents the residual offset.  The symbol processor invokes this
     * method when the message framer produces a valid super frame fragment, which also verifies the demodulator's
     * carrier offset range.
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

    /**
     * Listener interface to receive reusable complex buffers
     */
    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
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
                //Tuner PPM changes are small and the carrier offset estimator and symbol processor balance track the
                //change, so a reset would only discard the carrier offset estimate.
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                setSampleRate(sourceEvent.getValue().doubleValue());
                break;
        }
    }

    /**
     * Resets this decoder to prepare for processing a new channel
     */
    @Override
    public void reset()
    {
        mSymbolProcessor.resetBalance();
        mDemodulator.reset();
    }

    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return identifierUpdateNotification -> {
            if(identifierUpdateNotification.getIdentifier().getForm() == Form.SCRAMBLE_PARAMETERS)
            {
                ScrambleParameters scrambleParameters = (ScrambleParameters)identifierUpdateNotification.getIdentifier().getValue();
                mMessageFramer.setScrambleParameters(scrambleParameters);
            }
        };
    }

    public static void main(String[] args)
    {
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.getJmbeLibraryPreference().setPathJmbeLibrary(Path.of("/home/denny/SDRTrunk/jmbe/jmbe-1.0.9.jar"));

        Channel channel = new Channel("Phase 2 Test", Channel.ChannelType.TRAFFIC);
        DecodeConfigP25Phase2 config = new DecodeConfigP25Phase2();
        channel.setDecodeConfiguration(config);

        Path directory = Paths.get("/run/media/denny/T9/P25P2 Baseband Samples/");
        Path path = directory.resolve("20240303_203244_859412500_Duke_Energy_P25_Lake_Control_28_baseband.wav");

//        ScrambleParameters pcwinScrambleParameters = new ScrambleParameters(0xBEE00, 0x3BB, 0x3B2);
//        Path path = directory.resolve("20261029_043353_853425000_PCWIN_Tucson_TCH_168_baseband.wav");
//        Path path = directory.resolve("20261029_044105_851562500_PCWIN_Tucson_TCH_168_baseband.wav");
//        Path path = directory.resolve("20261029_044126_851612500_PCWIN_Tucson_TCH_289_baseband.wav");
//        Path path = directory.resolve("20261029_044556_852450000_PCWIN_Tucson_TCH_168_baseband.wav");
//        Path path = directory.resolve("20261029_044902_853175000_PCWIN_Tucson_TCH_168_baseband.wav");



        AliasList aliasList = new AliasList("bogus");
        ProcessingChain processingChain = new ProcessingChain(channel, new AliasModel());
        P25TrafficChannelManager trafficChannelManager = new P25TrafficChannelManager(channel);
        PatchGroupManager patchGroupManager = new PatchGroupManager();
        P25P2DecoderState ds1 = new P25P2DecoderState(channel, P25P2Message.TIMESLOT_1, trafficChannelManager,
                patchGroupManager);
//        Listener<IDecodeEvent> listener = event -> mLog.info("\n>>>>>>> Event: " + event + "\n");
//        ds1.addDecodeEventListener(listener);
        ds1.start();
        P25P2DecoderState ds2 = new P25P2DecoderState(channel, P25P2Message.TIMESLOT_2, trafficChannelManager,
                patchGroupManager);
//        ds2.addDecodeEventListener(listener);
        ds2.start();
        processingChain.addModule(ds1);
        processingChain.addModule(ds2);
        P25P2AudioModule am1 = new P25P2AudioModule(userPreferences, P25P2Message.TIMESLOT_1, aliasList);
        am1.start();
        P25P2AudioModule am2 = new P25P2AudioModule(userPreferences, P25P2Message.TIMESLOT_2, aliasList);
        am2.start();
        processingChain.addModule(am1);
        processingChain.addModule(am2);
//        processingChain.addAudioSegmentListener(audioSegment -> mLog.info("**** Audio Segment *** " + audioSegment));
        MultiChannelState multiChannelState = new MultiChannelState(channel, null, config.getTimeslots());
        multiChannelState.start();
//        Broadcaster<SquelchStateEvent> squelchBroadcaster = new Broadcaster<>();
//        multiChannelState.setSquelchStateListener(squelchBroadcaster);
//        squelchBroadcaster.addListener(am1.getSquelchStateListener());
//        squelchBroadcaster.addListener(am2.getSquelchStateListener());

        try(ComplexWaveSource source = new ComplexWaveSource(path.toFile(), false))
        {
            P25P2DecoderHDQPSK decoder = new P25P2DecoderHDQPSK(config);
            decoder.setMessageListener(message -> {
                mLog.info(message.toString());

                if(message.getTimeslot() == P25P2Message.TIMESLOT_1)
                {
                    ds1.receive(message);
                    am1.receive(message);
                }
                else
                {
                    ds2.receive(message);
                    am2.receive(message);
                }

                if(message.toString().contains("GPS LOCATION"))
                {
                    if(message instanceof MacMessage mac)
                    {
                        mLog.warn("Mac Structure Class: " + mac.getMacStructure().getClass());
                        mLog.warn("Opcode:" + mac.getMacStructure().getOpcode().name());
                    }
                    else
                    {
                        mLog.warn("Class: " + message.getClass());
                    }
                }
            });
            source.setSourceEventListener(decoder.getSourceEventListener());

            source.setListener(iNativeBuffer -> {
                Iterator<ComplexSamples> it = iNativeBuffer.iterator();
                while(it.hasNext())
                {
                    ComplexSamples samples = it.next();
                    decoder.receive(samples);
                }
            });

            source.open();
            decoder.setSampleRate(source.getSampleRate());
            decoder.start();

            while(true)
            {
                source.next(2048, true);
            }
        }
        catch(IOException ioe)
        {
            if(ioe.getMessage().contains("End of file reached"))
            {
                mLog.info("End of file");
            }
            else
            {
                mLog.error("I/O Error", ioe);
            }
        }
        catch(Exception ioe)
        {
            mLog.error("Error", ioe);
        }
    }
}
