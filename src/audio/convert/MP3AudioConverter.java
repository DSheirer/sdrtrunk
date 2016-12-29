/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.convert;

import audio.AudioFormats;
import audio.AudioPacket;
import audio.AudioUtils;
import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import net.sourceforge.lame.mp3.MPEGMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

public class MP3AudioConverter implements IAudioConverter
{
    private final static Logger mLog = LoggerFactory.getLogger( MP3AudioConverter.class );
    public static final int AUDIO_QUALITY = Lame.QUALITY_LOW;
    private LameEncoder mEncoder;
    private ByteArrayOutputStream mMP3Stream = new ByteArrayOutputStream();
    private byte[] mMP3Buffer;

    /**
     * Converts PCM 8kHz 16-bit Little Endian audio packets to Mono, Low Quality MP3 compressed audio.
     *
     * @param bitRate for converted MP3 audio
     * @param variableBitRate (VBR) true or false for constant bit rate (CBR)
     */
    public MP3AudioConverter(int bitRate, boolean variableBitRate)
    {
        mEncoder = new LameEncoder(AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO,
                bitRate, MPEGMode.MONO, AUDIO_QUALITY, variableBitRate);

        mMP3Buffer = new byte[mEncoder.getPCMBufferSize()];
    }

    @Override
    public byte[] convert(List<AudioPacket> audioPackets)
    {
        mMP3Stream.reset();

        byte[] pcmBytes = AudioUtils.convertTo16BitSamples(audioPackets);

        int pcmBufferSize = Math.min(mMP3Buffer.length, pcmBytes.length);

        int mp3BufferSize = 0;

        int pcmBytesPosition = 0;

        try
        {
            while (0 < (mp3BufferSize = mEncoder.encodeBuffer(pcmBytes, pcmBytesPosition, pcmBufferSize, mMP3Buffer)))
            {
                pcmBytesPosition += pcmBufferSize;
                pcmBufferSize = Math.min(mMP3Buffer.length, pcmBytes.length - pcmBytesPosition);
                mMP3Stream.write(mMP3Buffer, 0, mp3BufferSize);
            }

            return mMP3Stream.toByteArray();
        }
        catch(Exception e)
        {
            mLog.error("There was an error converting audio to MP3: " + e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public byte[] flush()
    {
        byte[] lastPartialFrame = new byte[mEncoder.getMP3BufferSize()];

        int length = mEncoder.encodeFinish(lastPartialFrame);

        return Arrays.copyOf(lastPartialFrame, length);
    }
}
