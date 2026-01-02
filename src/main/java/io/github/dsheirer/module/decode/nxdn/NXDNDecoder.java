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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.dsp.psk.demod.DifferentialDemodulatorFactory;
import io.github.dsheirer.dsp.psk.demod.DifferentialDemodulatorFloat;
import io.github.dsheirer.dsp.squelch.PowerMonitor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IByteBufferProvider;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NXDN Decoder
 */
public class NXDNDecoder extends FeedbackDecoder implements IByteBufferProvider, IComplexSamplesListener,
        ISourceEventListener, ISourceEventProvider, Listener<ComplexSamples>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NXDNDecoder.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final Map<Double,float[]> BASEBAND_FILTERS = new HashMap<>();

    private final NXDNDemodulator mSymbolProcessor;
    private final NXDNMessageFramer mMessageFramer;
    private final NXDNMessageProcessor mMessageProcessor = new NXDNMessageProcessor();
    private final PowerMonitor mPowerMonitor = new PowerMonitor();
    private DifferentialDemodulatorFloat mDemodulator;
    private IRealDecimationFilter mDecimationFilterI;
    private IRealDecimationFilter mDecimationFilterQ;
    private IRealFilter mBasebandFilterI;
    private IRealFilter mBasebandFilterQ;
    private IRealFilter mPulseShapingFilterI;
    private IRealFilter mPulseShapingFilterQ;
    private final int mSymbolRate;
    private DecodeConfigNXDN mConfig;

    private IDemodulator mFMDemodulator;

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
    }

    public NXDNDecoder(DecodeConfigNXDN config)
    {
        mConfig = config;
        mSymbolRate = config.getTransmissionMode().getSymbolRate();
        mMessageProcessor.setMessageListener(getMessageListener());
        mMessageFramer = new NXDNMessageFramer(mMessageProcessor);
        mSymbolProcessor = new NXDNDemodulator(mMessageFramer, this);
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
        if(sampleRate <= mSymbolRate * 2)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >9600 (2 * " + mSymbolRate + " symbol rate)");
        }

        mPowerMonitor.setSampleRate((int)sampleRate);

        int decimation = 1;

        //Identify decimation that gets us as close to 4.0 Samples Per Symbol as possible (19.2 kHz)
        while((sampleRate / decimation) >= 38400)
        {
            decimation *= 2;
        }

        //Decimate a further 2x for 4800 BPS modulation
//        if(mConfig.getTransmissionMode() == TransmissionMode.M4800)
//        {
//            decimation *= 2;
//        }

        mDecimationFilterI = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mDecimationFilterQ = DecimationFilterFactory.getRealDecimationFilter(decimation);

        float decimatedSampleRate = (float)sampleRate / decimation;
        int symbolLength = 26;
        float rrcAlpha = 0.2f;

        float[] rrcTaps = FilterFactory.getRootRaisedCosine(decimatedSampleRate / mSymbolRate,
                symbolLength, rrcAlpha);

        mPulseShapingFilterI = new RealFIRFilter(rrcTaps);
        mPulseShapingFilterQ = new RealFIRFilter(rrcTaps);

        float[] basebandTaps = getBasebandFilter(decimatedSampleRate, mConfig);

        mBasebandFilterI = FilterFactory.getRealFilter(basebandTaps);
        mBasebandFilterQ = FilterFactory.getRealFilter(basebandTaps);
        mDemodulator = DifferentialDemodulatorFactory.getFloatDemodulator(decimatedSampleRate, mSymbolRate);

        mFMDemodulator = FmDemodulatorFactory.getFmDemodulator();
        mSymbolProcessor.setSamplesPerSymbol(mDemodulator.getSamplesPerSymbol());
//        mMessageFramer.setListener(mMessageProcessor);
        mMessageProcessor.setMessageListener(getMessageListener());
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

        //Process buffer for channel power measurements
        mPowerMonitor.process(i, q);

        i = mPulseShapingFilterI.filter(i);
        q = mPulseShapingFilterQ.filter(q);

        i = mBasebandFilterI.filter(i);
        q = mBasebandFilterQ.filter(q);

        // PI/4 DQPSK differential demodulation
//        float[] demodulated = mDemodulator.demodulate(i, q);
        float[] demodulated = mFMDemodulator.demodulate(i, q);

        //Process demodulated samples into symbols and apply message sync detection and framing.
        mSymbolProcessor.process(demodulated);
    }

    /**
     * Constructs a baseband filter for this decoder using the current sample rate
     */
    private float[] getBasebandFilter(double sampleRate, DecodeConfigNXDN config)
    {
        ChannelSpecification channel = config.getChannelSpecification();

        FIRFilterSpecification specification = FIRFilterSpecification
                .lowPassBuilder()
                .sampleRate(sampleRate)
                .passBandCutoff(channel.getPassFrequency())
                .passBandAmplitude(1.0).passBandRipple(0.01)
                .stopBandAmplitude(0.0).stopBandStart(channel.getStopFrequency())
                .stopBandRipple(0.01).build();

        float[] coefficients = null;

        try
        {
            coefficients = FilterFactory.getTaps(specification);
        }
        catch(Exception fde) //FilterDesignException
        {
            System.out.println("Error");
        }

        if(coefficients == null)
        {
            throw new IllegalStateException("Unable to design low pass filter for sample rate [" + sampleRate + "]");
        }

        return coefficients;
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

    /**
     * Sets the source event listener to receive source events from this decoder.
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        super.setSourceEventListener(listener);
        mPowerMonitor.setSourceEventListener(listener);
    }

    @Override
    public void removeSourceEventListener()
    {
        super.removeSourceEventListener();
        mPowerMonitor.setSourceEventListener(null);
    }

    @Override
    public void start()
    {
        super.start();
//        mMessageFramer.start();
    }

    @Override
    public void stop()
    {
        super.stop();
//        mMessageFramer.stop();
    }

    /**
     * Process source events
     */
    private void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                mSymbolProcessor.resetPLL();
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

        DecodeConfigNXDN config = null;
//        String directory = "D:\\Recordings\\NXDN\\"; //Windows
        String directory = "/media/denny/T9/Recordings/NXDN/"; //Linux
//        String file = directory + "20251127_063701_451887500_Bush-NXDN-96_Sentinel-Heights_Control_45_baseband.wav";
//        config = new DecodeConfigNXDN(TransmissionMode.M9600);
        String file = directory + "20251128_052514_150845000_MobileTech-NXDN-48_Fulton_Control_47_baseband.wav";
        config = new DecodeConfigNXDN(TransmissionMode.M4800);

        boolean autoReplay = false;

        NXDNDecoder decoder = new NXDNDecoder(config);
        decoder.start();
        NXDNMessageProcessor processor = new NXDNMessageProcessor();
        decoder.setMessageListener(processor);
        processor.setMessageListener(new Listener<IMessage>()
        {
            @Override
            public void receive(IMessage iMessage)
            {
                System.out.println("L3:" + iMessage);
            }
        });

        UserPreferences userPreferences = new UserPreferences();

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
