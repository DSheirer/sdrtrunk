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
package dsp.filter.channelizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class ChannelDistributor
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelDistributor.class);
    private Map<Integer,ComplexSampleAssembler> mListenerMap = new HashMap<>();
    private int mBufferSize;
    private int mChannelCount;

    public ChannelDistributor(int bufferSize, int channelCount)
    {
        mBufferSize = bufferSize;
        mChannelCount = channelCount;

    }

    public void receive(float[] channels)
    {
        for(int channel = 0; channel < channels.length / 2; channel++)
        {
            if(mListenerMap.containsKey(channel))
            {
                ComplexSampleAssembler channelAssembler = mListenerMap.get(channel);

                int index = channel * 2;
                channelAssembler.receive(channels[index], channels[index + 1]);
            }
        }
    }

    public void addListener(int channel, Listener<ComplexBuffer> listener)
    {
        ComplexSampleAssembler channelAssembler = mListenerMap.get(channel);

        if(channelAssembler == null)
        {
            channelAssembler = new ComplexSampleAssembler(mBufferSize);
            mListenerMap.put(channel, channelAssembler);
        }

        channelAssembler.addListener(listener);
    }

    public class ComplexSampleAssembler
    {
        private Broadcaster<ComplexBuffer> mBroadcaster = new Broadcaster<>();
        private float[] mBuffer;
        private int mBufferPointer;
        private int mBufferSize;
        private boolean mEnableLogging;

        public ComplexSampleAssembler(int bufferSize)
        {
            mBufferSize = bufferSize;
            mBuffer = new float[mBufferSize];
        }

        public void receive(float inphase, float quadrature)
        {
            mBuffer[mBufferPointer++] = inphase;
            mBuffer[mBufferPointer++] = quadrature;

            if(mBufferPointer >= mBufferSize)
            {
                float[] bufferCopy = new float[mBuffer.length];
                System.arraycopy(mBuffer,0, bufferCopy, 0, mBuffer.length);

                mBroadcaster.receive(new ComplexBuffer(bufferCopy));

                mBufferPointer = 0;
            }
        }

        public void addListener(Listener<ComplexBuffer> listener)
        {
            mBroadcaster.addListener(listener);
        }
    }
}
