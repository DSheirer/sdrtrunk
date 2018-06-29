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
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter2;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferListener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.sample.buffer.ReusableAudioPacketQueue;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides packaging of demodulated audio sample buffers into audio packets for broadcast to registered audio packet
 * listeners.  Includes audio packet metadata in constructed audio packets.
 *
 * Incorporates audio squelch state listener to control if audio packets are broadcast or ignored.
 *
 * This class is designed to support 8 kHz sample rate demodulated audio.
 */
public class AudioModule extends Module implements IAudioPacketProvider, IReusableBufferListener,
    ISquelchStateListener, Listener<ReusableBuffer>
{
    protected static final Logger mLog = LoggerFactory.getLogger(AudioModule.class);
    private ReusableAudioPacketQueue mAudioPacketQueue = new ReusableAudioPacketQueue("AudioModule");

    private static float[] sHighPassFilterCoefficients;

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
            .sampleRate(8000)
            .stopBandCutoff(200)
            .stopBandAmplitude(0.0)
            .stopBandRipple(0.025)
            .passBandStart(300)
            .passBandAmplitude(1.0)
            .passBandRipple(0.01)
            .build();
        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sHighPassFilterCoefficients = designer.getImpulseResponse();
            }
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }
    }

    private RealFIRFilter2 mHighPassFilter = new RealFIRFilter2(sHighPassFilterCoefficients);
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;
    private Listener<ReusableAudioPacket> mAudioPacketListener;
    private Metadata mMetadata;

    /**
     * Creates an Audio Module.
     *
     * @param metadata to use for audio packets produced by this audio module.
     */
    public AudioModule(Metadata metadata)
    {
        mMetadata = metadata;
    }

    @Override
    public void dispose()
    {
        mAudioPacketListener = null;
        mSquelchStateListener = null;
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
        /* Issue an end-audio packet in case a recorder is still rolling */
        if(mAudioPacketListener != null)
        {
            ReusableAudioPacket endAudioPacket = mAudioPacketQueue.getEndAudioBuffer();
            endAudioPacket.setMetadata(mMetadata.copyOf());
            endAudioPacket.incrementUserCount();
            mAudioPacketListener.receive(endAudioPacket);
        }
    }

    @Override
    public void setAudioPacketListener(Listener<ReusableAudioPacket> listener)
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

    @Override
    public void receive(ReusableBuffer reusableBuffer)
    {
        if(mAudioPacketListener != null && mSquelchState == SquelchState.UNSQUELCH)
        {
            ReusableBuffer highPassFiltered = mHighPassFilter.filter(reusableBuffer);

            ReusableAudioPacket audioPacket = mAudioPacketQueue.getBuffer(highPassFiltered.getSampleCount());
            audioPacket.loadAudioFrom(highPassFiltered);
            audioPacket.setMetadata(mMetadata.copyOf());
            audioPacket.incrementUserCount();

            mAudioPacketListener.receive(audioPacket);

            highPassFiltered.decrementUserCount();
        }
        else
        {
            reusableBuffer.decrementUserCount();
        }
    }

    @Override
    public Listener getReusableBufferListener()
    {
        //Redirect received reusable buffers to the receive(buffer) method
        return this;
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
                ReusableAudioPacket endAudioPacket = mAudioPacketQueue.getEndAudioBuffer();
                endAudioPacket.setMetadata(mMetadata.copyOf());
                mAudioPacketListener.receive(endAudioPacket);
            }

            mSquelchState = state;
        }
    }
}
