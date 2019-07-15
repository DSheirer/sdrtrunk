/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.audio;

import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter2;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferListener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.sample.buffer.ReusableAudioPacketQueue;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
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
public class AudioModule extends AbstractAudioModule implements IReusableBufferListener, Listener<ReusableFloatBuffer>
{

    private static final Logger mLog = LoggerFactory.getLogger(AudioModule.class);
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

    private ReusableAudioPacketQueue mAudioPacketQueue = new ReusableAudioPacketQueue("AudioModule");
    private RealFIRFilter2 mHighPassFilter = new RealFIRFilter2(sHighPassFilterCoefficients);
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;
    private boolean mRecordAudioOverride;

    /**
     * Creates an Audio Module.
     */
    public AudioModule()
    {
    }

    @Override
    public void dispose()
    {
        removeAudioPacketListener();
        mSquelchStateListener = null;
    }

    /**
     * Sets all audio packets as recordable when the argument is true.  Otherwise, defers to the aliased identifiers
     * from the identifier collection to determine whether to record the audio or not.
     * @param recordAudio set to true to mark all audio as recordable.
     */
    public void setRecordAudio(boolean recordAudio)
    {
        mRecordAudioOverride = recordAudio;
    }

    @Override
    public void reset()
    {
        getIdentifierCollection().clear();
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
        if(hasAudioPacketListener())
        {
            ReusableAudioPacket endAudioPacket = mAudioPacketQueue.getEndAudioBuffer();
            endAudioPacket.resetAttributes();
            endAudioPacket.setAudioChannelId(getAudioChannelId());
            endAudioPacket.setIdentifierCollection(getIdentifierCollection().copyOf());

            if(mRecordAudioOverride)
            {
                endAudioPacket.setRecordable(true);
            }
            endAudioPacket.incrementUserCount();
            getAudioPacketListener().receive(endAudioPacket);
        }
    }


    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void receive(ReusableFloatBuffer reusableFloatBuffer)
    {
        if(hasAudioPacketListener() && mSquelchState == SquelchState.UNSQUELCH)
        {
            ReusableFloatBuffer highPassFiltered = mHighPassFilter.filter(reusableFloatBuffer);

            ReusableAudioPacket audioPacket = mAudioPacketQueue.getBuffer(highPassFiltered.getSampleCount());
            audioPacket.resetAttributes();
            audioPacket.setAudioChannelId(getAudioChannelId());
            audioPacket.loadAudioFrom(highPassFiltered);
            if(mRecordAudioOverride)
            {
                audioPacket.setRecordable(true);
            }
            audioPacket.setIdentifierCollection(getIdentifierCollection().copyOf());

            getAudioPacketListener().receive(audioPacket);

            highPassFiltered.decrementUserCount();
        }
        else
        {
            reusableFloatBuffer.decrementUserCount();
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
    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            if(event.getSquelchState() == SquelchState.SQUELCH && hasAudioPacketListener())
            {
                ReusableAudioPacket endAudioPacket = mAudioPacketQueue.getEndAudioBuffer();
                endAudioPacket.resetAttributes();
                endAudioPacket.setAudioChannelId(getAudioChannelId());
                endAudioPacket.setIdentifierCollection(getIdentifierCollection().copyOf());
                getAudioPacketListener().receive(endAudioPacket);
            }

            mSquelchState = event.getSquelchState();
        }
    }
}
