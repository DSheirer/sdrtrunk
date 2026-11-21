/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.convert;

import io.github.dsheirer.audio.AudioUtils;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.sample.ConversionUtils;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts PCM audio packets to Mono, MP3 encoded audio
 */
public class MP3AudioConverter implements IAudioConverter
{
    private final static Logger mLog = LoggerFactory.getLogger( MP3AudioConverter.class );
    public static final int AUDIO_QUALITY = Lame.QUALITY_LOW;
    private LameEncoder mEncoder;
    private RealResampler mResampler;
    private ByteArrayOutputStream mMP3EncodedFramesStream = new ByteArrayOutputStream();
    private byte[] mOutputFramesBuffer;
    private InputAudioFormat mInputAudioFormat;

    private boolean mNormalizeAudio;

    /**
     * Constructs an instance.
     *
     * @param inputAudioFormat for the desired input sample rate and bit size (resampled from default 8 kHz as needed)
     * @param setting to configure the LAME encoder
     * @param normalizeAudio to normalize the audio gain prior to encoding
     */
    public MP3AudioConverter(InputAudioFormat inputAudioFormat, MP3Setting setting, boolean normalizeAudio)
    {
        mInputAudioFormat = inputAudioFormat;
        mEncoder = LameFactory.getLameEncoder(inputAudioFormat, setting);
        mNormalizeAudio = normalizeAudio;

        //Ensure input audio sample rate is supported for the MP3 setting and update as necessary - should never happen.
        if((mInputAudioFormat.getSampleRate() - mEncoder.getEffectiveSampleRate()) > 1.0)
        {
            mInputAudioFormat = InputAudioFormat.getDefault();
            mLog.warn("MP3 setting [" + setting + "] does not support requested input audio sample rate [" +
                    inputAudioFormat + "] - using default sample rate [" + mInputAudioFormat + "]");
            mEncoder = LameFactory.getLameEncoder(inputAudioFormat, setting);
        }

        //Resampling is only required if desired input sample rate is not system default of 8kHz
        if(mInputAudioFormat != InputAudioFormat.SR_8000 && mInputAudioFormat != InputAudioFormat.SR_32_8000)
        {
            mResampler = LameFactory.getResampler(inputAudioFormat);
        }

        mOutputFramesBuffer = new byte[mEncoder.getPCMBufferSize()];
    }

    /**
     * Converts the list of PCM audio packets to MP3 encoded.
     * @param audioPackets of PCM audio sampled at 8 kHz
     * @return encoded MP3 audio
     */
    public List<byte[]> convert(List<float[]> audioPackets)
    {
        List<byte[]> converted = new ArrayList<>();

        if(mNormalizeAudio)
        {
            audioPackets = AudioUtils.normalize(audioPackets);
        }

        if(mResampler != null)
        {
            audioPackets = mResampler.resample(audioPackets);
        }

        for(float[] audioPacket: audioPackets)
        {
            encodePacket(audioPacket, converted);
        }

        int finalChunkSize = mEncoder.encodeFinish(mOutputFramesBuffer);

        if(finalChunkSize > 0)
        {
            converted.add(Arrays.copyOf(mOutputFramesBuffer, finalChunkSize));
        }

        return converted;
    }

    /**
     * Encodes the PCM audio packet (already resampled) and adds any encoded MP3 frames to the list.
     */
    private void encodePacket(float[] audioPacket, List<byte[]> converted)
    {
        byte[] bytesToEncode;

        if(mInputAudioFormat.getAudioFormat().getSampleSizeInBits() == 16)
        {
            bytesToEncode = ConversionUtils.convertToSigned16BitSamples(audioPacket).array();
        }
        else
        {
            bytesToEncode = ConversionUtils.convertToSigned32BitSamples(audioPacket).array();
        }

        int bytesToEncodePointer = 0;

        int inputChunkSize = FastMath.min(mOutputFramesBuffer.length, bytesToEncode.length);
        int outputChunkSize = 0;

        try
        {
            while(bytesToEncodePointer < bytesToEncode.length)
            {
                outputChunkSize = mEncoder.encodeBuffer(bytesToEncode, bytesToEncodePointer, inputChunkSize, mOutputFramesBuffer);
                bytesToEncodePointer += inputChunkSize;
                inputChunkSize = FastMath.min(mOutputFramesBuffer.length, bytesToEncode.length - bytesToEncodePointer);

                if(outputChunkSize > 0)
                {
                    converted.add(Arrays.copyOf(mOutputFramesBuffer, outputChunkSize));
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("There was an error converting audio to MP3: " + e.getMessage());
        }
    }

    /**
     * Encodes a chunk of a continuing audio stream to MP3.  Unlike {@link #convert(List)}, the encoder and resampler
     * keep their state between invocations, so the chunks of a call are encoded as one continuous stream.  Invoke
     * {@link #finishStream()} after the last chunk.  Note: audio normalization is not applied.
     *
     * @param audioPackets of PCM audio sampled at 8 kHz
     * @return encoded MP3 frames, possibly empty
     */
    public List<byte[]> encodeStream(List<float[]> audioPackets)
    {
        List<byte[]> converted = new ArrayList<>();

        if(mResampler != null)
        {
            List<float[]> resampled = new ArrayList<>();
            mResampler.setListener(resampled::add);

            for(float[] audioPacket: audioPackets)
            {
                mResampler.resample(audioPacket);
            }

            mResampler.setListener(null);
            audioPackets = resampled;
        }

        for(float[] audioPacket: audioPackets)
        {
            encodePacket(audioPacket, converted);
        }

        return converted;
    }

    /**
     * Flushes the resampler and the encoder at the end of a stream encoded with {@link #encodeStream(List)}.
     * @return final encoded MP3 frames
     */
    public List<byte[]> finishStream()
    {
        List<byte[]> converted = new ArrayList<>();

        if(mResampler != null)
        {
            List<float[]> resampled = new ArrayList<>();
            mResampler.setListener(resampled::add);
            mResampler.flush();
            mResampler.setListener(null);

            for(float[] audioPacket: resampled)
            {
                encodePacket(audioPacket, converted);
            }
        }

        int finalChunkSize = mEncoder.encodeFinish(mOutputFramesBuffer);

        if(finalChunkSize > 0)
        {
            converted.add(Arrays.copyOf(mOutputFramesBuffer, finalChunkSize));
        }

        return converted;
    }

    @Override
    public List<byte[]> flush()
    {
        byte[] lastPartialFrame = new byte[mEncoder.getMP3BufferSize()];

        int length = mEncoder.encodeFinish(lastPartialFrame);

        byte[] frame = Arrays.copyOf(lastPartialFrame, length);

        if(frame.length == 0)
        {
            return Collections.emptyList();
        }

        return Collections.singletonList(frame);
    }
}
