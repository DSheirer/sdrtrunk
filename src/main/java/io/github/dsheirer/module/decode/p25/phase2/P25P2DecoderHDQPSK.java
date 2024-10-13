/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.state.MultiChannelState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.gain.complex.ComplexGainFactory;
import io.github.dsheirer.dsp.gain.complex.IComplexGainControl;
import io.github.dsheirer.dsp.psk.DQPSKGardnerDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.FrequencyCorrectionSyncMonitor;
import io.github.dsheirer.dsp.psk.pll.PLLBandwidth;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.audio.P25P2AudioModule;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected IComplexGainControl mAGC = ComplexGainFactory.getComplexGainControl();
    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    protected IRealFilter mIBasebandFilter;
    protected IRealFilter mQBasebandFilter;
    private DecodeConfigP25Phase2 mDecodeConfigP25Phase2;
    private FrequencyCorrectionSyncMonitor mFrequencyCorrectionSyncMonitor;

    public P25P2DecoderHDQPSK(DecodeConfigP25Phase2 decodeConfigP25Phase2)
    {
        super(6000.0);
        setSampleRate(25000.0);
        mDecodeConfigP25Phase2 = decodeConfigP25Phase2;
    }

    @Override
    public void start()
    {
        super.start();
        mQPSKDemodulator.start();

        //Refresh the scramble parameters each time we start in case they change
        if(mDecodeConfigP25Phase2 != null && mDecodeConfigP25Phase2.getScrambleParameters() != null &&
                !mDecodeConfigP25Phase2.isAutoDetectScrambleParameters())
        {
            mMessageFramer.setScrambleParameters(mDecodeConfigP25Phase2.getScrambleParameters());
        }
    }

    @Override
    public void stop()
    {
        super.stop();
        mQPSKDemodulator.stop();
    }

    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        mIBasebandFilter = FilterFactory.getRealFilter(getBasebandFilter());
        mQBasebandFilter = FilterFactory.getRealFilter(getBasebandFilter());
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
        mMessageFramer = new P25P2MessageFramer(mCostasLoop);

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
     * @param samples containing channelized complex samples
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        float[] i = mIBasebandFilter.filter(samples.i());
        float[] q = mQBasebandFilter.filter(samples.q());

        //Process the buffer for power measurements
        mPowerMonitor.process(i, q);

        ComplexSamples amplified = mAGC.process(i, q, samples.timestamp());
        mQPSKDemodulator.receive(amplified);
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

    public static void main(String[] args)
    {
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.getJmbeLibraryPreference().setPathJmbeLibrary(Path.of("/home/denny/SDRTrunk/jmbe/jmbe-1.0.9.jar"));

        Channel channel = new Channel("Phase 2 Test", Channel.ChannelType.TRAFFIC);
        DecodeConfigP25Phase2 config = new DecodeConfigP25Phase2();
        channel.setDecodeConfiguration(config);

        Path path = Paths.get("/home/denny/temp/20240309_054833_855787500_Duke_Energy_Monroe_Dicks_Creek_Site_201-24_1_baseband.wav");
//        ScrambleParameters scrambleParameters = new ScrambleParameters(0x91F14, 0x201, 0x00A);
//        config.setScrambleParameters(scrambleParameters);

//        Path path = Paths.get("/home/denny/temp/PA-STARNet_ECEN_TRAFFIC_10_baseband_20220610_104744.wav");
//        ScrambleParameters scrambleParameters = new ScrambleParameters(781824, 2370, 2369);
//        config.setScrambleParameters(scrambleParameters);

//        Path path = Paths.get("/home/denny/temp/20240303_203244_859412500_Duke_Energy_P25_Lake_Control_28_baseband.wav");
//        ScrambleParameters scrambleParameters = new ScrambleParameters(0x91F14, 0x2D7, 0x00A);
//        config.setScrambleParameters(scrambleParameters);

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
