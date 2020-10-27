package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.gain.ComplexFeedForwardGainControl;
import io.github.dsheirer.dsp.psk.DQPSKDecisionDirectedDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.FrequencyCorrectionSyncMonitor;
import io.github.dsheirer.dsp.psk.pll.PLLBandwidth;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableByteBufferProvider;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * DMR decoder module.
 */
public class DMRDecoder extends FeedbackDecoder implements ISourceEventListener, ISourceEventProvider,
        IReusableComplexBufferListener, Listener<ReusableComplexBuffer>, IReusableByteBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDecoder.class);
    protected static final float SAMPLE_COUNTER_GAIN = 0.4f;
    private static final double SYMBOL_RATE = 4800.0;
    private double mSampleRate;
    private Broadcaster<Dibit> mDibitBroadcaster = new Broadcaster<>();
    private DibitToByteBufferAssembler mByteBufferAssembler = new DibitToByteBufferAssembler(300);
    private DMRMessageProcessor mMessageProcessor;
    private Listener<SourceEvent> mSourceEventListener;
    private ComplexFeedForwardGainControl mAGC = new ComplexFeedForwardGainControl(32);
    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    private ComplexFIRFilter2 mBasebandFilter;
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected DQPSKDecisionDirectedDemodulator mQPSKDemodulator;
    protected CostasLoop mCostasLoop;
    protected FrequencyCorrectionSyncMonitor mFrequencyCorrectionSyncMonitor;
    protected DMRMessageFramer mMessageFramer;

    /**
     * Constructs an instance
     */
    public DMRDecoder(DecodeConfigDMR config)
    {
        mMessageProcessor = new DMRMessageProcessor(config);
        mMessageProcessor.setMessageListener(getMessageListener());
        getDibitBroadcaster().addListener(mByteBufferAssembler);
        setSampleRate(25000.0);
    }

    /**
     * DMR decoder type.
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    /**
     * Message processor
     */
    protected DMRMessageProcessor getMessageProcessor()
    {
        return mMessageProcessor;
    }

    /**
     * Sets the sample rate and configures internal decoder components.
     * @param sampleRate
     */
    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= getSymbolRate() * 2)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >9600 (2 * " +
                getSymbolRate() + " symbol rate)");
        }

        mSampleRate = sampleRate;
        mBasebandFilter = new ComplexFIRFilter2(getBasebandFilter());
        mCostasLoop = new CostasLoop(getSampleRate(), getSymbolRate());
        mCostasLoop.setPLLBandwidth(PLLBandwidth.BW_300);
        mFrequencyCorrectionSyncMonitor = new FrequencyCorrectionSyncMonitor(mCostasLoop, this);
        mInterpolatingSampleBuffer = new InterpolatingSampleBuffer(getSamplesPerSymbol(), SAMPLE_COUNTER_GAIN);

        mQPSKDemodulator = new DQPSKDecisionDirectedDemodulator(mCostasLoop, mInterpolatingSampleBuffer);

        if(mMessageFramer != null)
        {
            getDibitBroadcaster().removeListener(mMessageFramer);
        }

        //The Costas Loop receives symbol-inversion correction requests when detected.
        //The PLL gain monitor receives sync detect/loss signals from the message framer
        mMessageFramer = new DMRMessageFramer(mCostasLoop);
        mMessageFramer.setSyncDetectListener(mFrequencyCorrectionSyncMonitor);
        mMessageFramer.setListener(getMessageProcessor());

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
                .sampleRate((int)getSampleRate())
                .passBandCutoff(5100)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandAmplitude(0.0)
                .stopBandStart(6500)
                .stopBandRipple(0.01)
                .build();

            try
            {
                filter = FilterFactory.getTaps(specification);//
            }
            catch(Exception fde) //FilterDesignException
            {
                mLog.error("Couldn't design low pass baseband filter for sample rate: " + getSampleRate());
            }

            if(filter != null)
            {
                mBasebandFilters.put(getSampleRate(), filter);
            }
            else
            {
                throw new IllegalStateException("Couldn't design a DMR baseband filter for sample rate: " + getSampleRate());
            }
        }

        return filter;
    }

    /**
     * Process source events
     */
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
        mFrequencyCorrectionSyncMonitor.reset();
    }

    /**
     * Assembler for packaging Dibit stream into reusable byte buffers.
     */
    protected Broadcaster<Dibit> getDibitBroadcaster()
    {
        return mDibitBroadcaster;
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public void setBufferListener(Listener<ReusableByteBuffer> listener)
    {
        mByteBufferAssembler.setBufferListener(listener);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public void removeBufferListener(Listener<ReusableByteBuffer> listener)
    {
        mByteBufferAssembler.removeBufferListener(listener);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public boolean hasBufferListeners()
    {
        return mByteBufferAssembler.hasBufferListeners();
    }

    protected double getSymbolRate()
    {
        return SYMBOL_RATE;
    }

    /**
     * Current sample rate for this decoder
     */
    protected double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Samples per symbol based on current sample rate and symbol rate.
     */
    public float getSamplesPerSymbol()
    {
        return (float)(getSampleRate() / getSymbolRate());
    }

    /**
     * Sets the source event listener to receive source events from this decoder.
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Removes a registered source event listener from receiving source events from this decoder
     */
    @Override
    public void removeSourceEventListener()
    {
        mSourceEventListener = null;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return new Listener<SourceEvent>()
        {
            @Override
            public void receive(SourceEvent sourceEvent)
            {
                process(sourceEvent);
            }
        };
    }

    /**
     * Broadcasts the source event to an optional registered listener.  This method should primarily be used to
     * issue frequency correction requests to the channel source.
     * @param sourceEvent to broadcast
     */
    public void broadcast(SourceEvent sourceEvent)
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(sourceEvent);
        }
    }

    /**
     * Listener interface to receive reusable complex buffers
     */
    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return DMRDecoder.this;
    }

    /**
     * Starts the decoder
     */
    @Override
    public void start()
    {
        //No-op
    }

    /**
     * Stops the decoder
     */
    @Override
    public void stop()
    {
        //No-op
    }
}
