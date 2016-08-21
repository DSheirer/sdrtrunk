package audio;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

import dsp.filter.design.FilterDesignException;
import dsp.filter.fir.FIRFilterSpecification;
import dsp.filter.fir.remez.RemezFIRFilterDesigner;
import dsp.filter.polyphase.PolyphaseFIRDecimatingFilter_RB;
import module.Module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.real.IFilteredRealBufferListener;
import sample.real.RealBuffer;
import audio.metadata.AudioMetadata;
import audio.metadata.IMetadataListener;
import audio.metadata.Metadata;
import audio.squelch.ISquelchStateListener;
import audio.squelch.SquelchState;
import controller.channel.ChannelEvent;
import controller.channel.IChannelEventListener;
import dsp.filter.Filters;
import dsp.filter.fir.real.RealFIRFilter_R_R;

/**
 * Provides packaging of demodulated audio sample buffers into audio packets for
 * broadcast to registered audio packet listeners.  Includes audio packet
 * metadata in constructed audio packets.
 *
 * Incorporates audio squelch state listener to control if audio packets are
 * broadcast or ignored.
 */
public class AudioModule extends Module implements IAudioPacketProvider,
        IChannelEventListener,
        IMetadataListener,
        IFilteredRealBufferListener,
        ISquelchStateListener,
        Listener<RealBuffer>
{
    protected static final Logger mLog = LoggerFactory.getLogger(AudioModule.class);

    private static float[] mDecimationCoefficients;

    static
    {
        //Band-pass filter to both decimate (48kHz to 8kHz) and block 0 - 300 Hz (LTR signalling).
        FIRFilterSpecification specification = FIRFilterSpecification.bandPassBuilder()
                .sampleRate( 48000 )
                .stopFrequency1( 150 )
                .passFrequencyBegin( 300 )
                .passFrequencyEnd( 3500 )
                .stopFrequency2( 4000 )
                .stopRipple( 0.0004 )
                .passRipple( 0.008)
                .build();

        RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

        try
        {
            if (!designer.isValid())
            {
                throw new FilterDesignException("Couldn't design the audio decimation filter");
            }

            mDecimationCoefficients = designer.getImpulseResponse();
        }
        catch (FilterDesignException e)
        {
            mLog.debug("Error designing filter", e);
        }
    }

    /* Provides a unique identifier for this audio module instance to use as a
     * source identifier for all audio packets */
    private static int UNIQUE_ID = 0;
    private int mSourceID;

    private AudioMetadata mAudioMetadata;
    private ChannelEventListener mChannelEventListener = new ChannelEventListener();
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;
    private Listener<AudioPacket> mAudioPacketListener;
    private PolyphaseFIRDecimatingFilter_RB mAudioDecimationFilter;

    public AudioModule(boolean record)
    {
        mSourceID = ++UNIQUE_ID;
        mAudioMetadata = new AudioMetadata(mSourceID, record);

        mAudioDecimationFilter = new PolyphaseFIRDecimatingFilter_RB(mDecimationCoefficients, 6, 2.0f);
        mAudioDecimationFilter.setListener(new Listener<RealBuffer>()
        {
            @Override
            public void receive(RealBuffer realBuffer)
            {
                if (mAudioPacketListener != null)
                {
                    AudioPacket packet = new AudioPacket(realBuffer.getSamples(), mAudioMetadata.copyOf());
                    mAudioPacketListener.receive(packet);
                }
            }
        });
    }

    @Override
    public void dispose()
    {
        mSquelchStateListener = null;
        mAudioPacketListener = null;
    }

    @Override
    public void reset()
    {
        mAudioMetadata.reset();
    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
        /* No start operations provided */
    }

    @Override
    public void stop()
    {
		/* Issue an end audio packet in case a recorder is still rolling */
        if (mAudioPacketListener != null)
        {
            mAudioPacketListener.receive(new AudioPacket(AudioPacket.Type.END,
                    mAudioMetadata.copyOf()));
        }
    }

    /**
     * Processes demodulated audio samples into audio packets with current audio
     * metadata and sends to the registered listener
     */
    @Override
    public void receive(RealBuffer buffer)
    {
        if (mAudioPacketListener != null && mSquelchState == SquelchState.UNSQUELCH)
        {
            mAudioDecimationFilter.receive(buffer);

//			/* We make a copy of the samples, so that we don't affect any other
//			 * processes that might be concurrently processing the same buffer */
//			float[] audio = Arrays.copyOf( buffer.getSamples(), buffer.getSamples().length );
//
//			mAudioFilter.filter( audio );
        }
    }

    @Override
    public Listener<RealBuffer> getFilteredRealBufferListener()
    {
        return this;
    }

    @Override
    public Listener<Metadata> getMetadataListener()
    {
        return mAudioMetadata;
    }

    @Override
    public void setAudioPacketListener(Listener<AudioPacket> listener)
    {
        mAudioPacketListener = listener;
    }

    @Override
    public void removeAudioPacketListener()
    {
        mAudioPacketListener = null;
    }

    @Override
    public Listener<ChannelEvent> getChannelEventListener()
    {
        return mChannelEventListener;
    }

    @Override
    public Listener<SquelchState> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    /**
     * Wrapper for channel event listener.  Responds to channel state reset
     * events to remove/cleanup current audio metadata
     */
    public class ChannelEventListener implements Listener<ChannelEvent>
    {
        @Override
        public void receive(ChannelEvent event)
        {
            switch (event.getEvent())
            {
                case NOTIFICATION_STATE_RESET:
                    mAudioMetadata.reset();
                    break;
                case NOTIFICATION_SELECTION_CHANGE:
                    mAudioMetadata.setSelected(event.getChannel().isSelected());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Wrapper for squelch state listener
     */
    public class SquelchStateListener implements Listener<SquelchState>
    {
        @Override
        public void receive(SquelchState state)
        {
            if (state == SquelchState.SQUELCH && mAudioPacketListener != null)
            {
                mAudioPacketListener.receive(new AudioPacket(AudioPacket.Type.END,
                        mAudioMetadata.copyOf()));
            }

            mSquelchState = state;
        }
    }
}
