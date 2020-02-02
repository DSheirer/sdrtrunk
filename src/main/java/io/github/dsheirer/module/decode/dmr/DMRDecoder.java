package io.github.dsheirer.module.decode.dmr;

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

public abstract class DMRDecoder extends FeedbackDecoder implements ISourceEventListener, ISourceEventProvider,
        IReusableComplexBufferListener, Listener<ReusableComplexBuffer>, IReusableByteBufferProvider {
    private double mSampleRate;
    private Broadcaster<Dibit> mDibitBroadcaster = new Broadcaster<>();
    private DibitToByteBufferAssembler mByteBufferAssembler = new DibitToByteBufferAssembler(300);
    private DMRMessageProcessor mMessageProcessor;
    private Listener<SourceEvent> mSourceEventListener;
    private double mSymbolRate;

    public DMRDecoder(DecodeConfigDMR config)
    {
        mSymbolRate = 4800.0;
        mMessageProcessor = new DMRMessageProcessor(config);
        mMessageProcessor.setMessageListener(getMessageListener());
        getDibitBroadcaster().addListener(mByteBufferAssembler);
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
        return mSymbolRate;
    }

    /**
     * Current sample rate for this decoder
     */
    protected double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets current sample rate for this decoder
     */
    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= getSymbolRate() * 2)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >9600 (2 * " +
                    getSymbolRate() + " symbol rate)");
        }

        mSampleRate = sampleRate;
    }

    /**
     * Samples per symbol based on current sample rate and symbol rate.
     */
    public float getSamplesPerSymbol()
    {
        return (float)(getSampleRate() / getSymbolRate());
    }

    public void dispose()
    {
        super.dispose();

        mMessageProcessor.dispose();
        mMessageProcessor = null;
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
     * Sub-class processing of received source events
     * @param sourceEvent
     */
    protected abstract void process(SourceEvent sourceEvent);

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

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    protected DMRMessageProcessor getMessageProcessor()
    {
        return mMessageProcessor;
    }

    public abstract DMRDecoder.Modulation getModulation();

    public enum Modulation
    {
        CQPSK("Simulcast (LSM)", "LSM"),
        C4FM("Normal (C4FM)", "C4FM");

        private String mLabel;
        private String mShortLabel;

        private Modulation(String label, String shortLabel)
        {
            mLabel = label;
            mShortLabel = shortLabel;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String getShortLabel()
        {
            return mShortLabel;
        }

        public String toString()
        {
            return getLabel();
        }
    }
}
