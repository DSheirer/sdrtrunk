/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio;

import audio.squelch.ISquelchStateListener;
import audio.squelch.SquelchState;
import channel.metadata.Metadata;
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

import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides packaging of demodulated audio sample buffers into audio packets for
 * broadcast to registered audio packet listeners.  Includes audio packet
 * metadata in constructed audio packets.
 *
 * Incorporates audio squelch state listener to control if audio packets are
 * broadcast or ignored.
 */
public class AudioModule extends Module implements IAudioPacketProvider, IFilteredRealBufferListener,
    ISquelchStateListener, Listener<RealBuffer>
{
    protected static final Logger mLog = LoggerFactory.getLogger(AudioModule.class);

    private static float[] mDecimationCoefficients;

    static
    {
        //Band-pass filter to both decimate (48kHz to 8kHz) and block 0 - 300 Hz (LTR signalling).
        FIRFilterSpecification specification = FIRFilterSpecification.bandPassBuilder()
            .sampleRate(48000)
            .stopFrequency1(150)
            .passFrequencyBegin(300)
            .passFrequencyEnd(3500)
            .stopFrequency2(4000)
            .stopRipple(0.0004)
            .passRipple(0.008)
            .build();

        RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

        try
        {
            if(!designer.isValid())
            {
                throw new FilterDesignException("Couldn't design the audio decimation filter");
            }

            mDecimationCoefficients = designer.getImpulseResponse();
        }
        catch(FilterDesignException e)
        {
            mLog.debug("Error designing filter", e);
        }
    }

    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;
    private Listener<AudioPacket> mAudioPacketListener;
    private PolyphaseFIRDecimatingFilter_RB mAudioDecimationFilter;
    private Metadata mMetadata;

    public AudioModule(Metadata metadata)
    {
        mMetadata = metadata;

        mAudioDecimationFilter = new PolyphaseFIRDecimatingFilter_RB(mDecimationCoefficients, 6, 2.0f);
        mAudioDecimationFilter.setListener(new Listener<RealBuffer>()
        {
            @Override
            public void receive(RealBuffer realBuffer)
            {
                if(mAudioPacketListener != null)
                {
                    AudioPacket packet = new AudioPacket(realBuffer.getSamples(), mMetadata.copyOf());
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
        if(mAudioPacketListener != null)
        {
            mAudioPacketListener.receive(new AudioPacket(AudioPacket.Type.END, mMetadata.copyOf()));
        }
    }

    /**
     * Processes demodulated audio samples into audio packets with current audio
     * metadata and sends to the registered listener
     */
    @Override
    public void receive(RealBuffer buffer)
    {
        if(mAudioPacketListener != null && mSquelchState == SquelchState.UNSQUELCH)
        {
            mAudioDecimationFilter.receive(buffer);
        }
    }

    @Override
    public Listener<RealBuffer> getFilteredRealBufferListener()
    {
        return this;
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
    public Listener<SquelchState> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    /**
     * Wrapper for squelch state listener
     */
    public class SquelchStateListener implements Listener<SquelchState>
    {
        @Override
        public void receive(SquelchState state)
        {
            if(state == SquelchState.SQUELCH && mAudioPacketListener != null)
            {
                mAudioPacketListener.receive(new AudioPacket(AudioPacket.Type.END, mMetadata.copyOf()));
            }

            mSquelchState = state;
        }
    }
}
