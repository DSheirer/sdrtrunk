/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.audio;

import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.channel.metadata.Metadata;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import io.github.dsheirer.sample.real.IFilteredRealBufferListener;
import io.github.dsheirer.sample.real.RealBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides packaging of demodulated audio sample buffers into audio packets for
 * broadcast to registered audio packet listeners.  Includes audio packet
 * metadata in constructed audio packets.
 *
 * Incorporates audio squelch state listener to control if audio packets are
 * broadcast or ignored.
 */
public class AudioModule2 extends Module implements IAudioPacketProvider, IFilteredRealBufferListener,
    ISquelchStateListener, ISourceEventListener, Listener<RealBuffer>
{
    protected static final Logger mLog = LoggerFactory.getLogger(AudioModule2.class);
    private static final double OUTPUT_SAMPLE_RATE = 8000.0;
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;
    private Listener<AudioPacket> mAudioPacketListener;
    private Listener<ReusableBuffer> mResampledBufferListener;
    private RealResampler mAudioResampler;
    private Metadata mMetadata;

    public AudioModule2(Metadata metadata)
    {
        mMetadata = metadata;

        mResampledBufferListener = new Listener<ReusableBuffer>()
        {
            @Override
            public void receive(ReusableBuffer reusableBuffer)
            {
                if(mAudioPacketListener != null)
                {
                    AudioPacket packet = new AudioPacket(reusableBuffer.getSamplesCopy(), mMetadata.copyOf());
                    reusableBuffer.decrementUserCount();
                    mAudioPacketListener.receive(packet);
                }
            }
        };

        setSampleRate(25000.0);
    }

    /**
     * Sets the incoming sample rate
     */
    private void setSampleRate(double sampleRate)
    {
        mAudioResampler = new RealResampler(sampleRate, OUTPUT_SAMPLE_RATE);
        mAudioResampler.setListener(mResampledBufferListener);
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
    public void start()
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
        if(mAudioResampler != null && mSquelchState == SquelchState.UNSQUELCH)
        {
            mAudioResampler.receive(buffer);
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
    public Listener<SourceEvent> getSourceEventListener()
    {
        return new Listener<SourceEvent>()
        {
            @Override
            public void receive(SourceEvent sourceEvent)
            {
                if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
                {
                    setSampleRate(sourceEvent.getValue().doubleValue());
                }
            }
        };
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
