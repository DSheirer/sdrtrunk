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
package record.wave;

import audio.AudioPacket;
import audio.IAudioPacketListener;
import channel.metadata.Metadata;
import sample.Listener;
import sample.real.RealBuffer;

import java.io.IOException;
import java.nio.file.Path;

public class AudioPacketMonoWaveReader extends MonoWaveReader implements Listener<RealBuffer>
{
    private Metadata mMetadata = new Metadata();

    private IAudioPacketListener mListener;

    /**
     * Reads wav files and converts the contents to AudioPackets.
     *
     * @param path of file to read containing 8kHz, 16-bit signed litte endian PCM audio
     * @param realTime causes the read() method to read and dispatch audio packets in real time.  Note: when using
     * real time playback, the thread calling the read() method will sleep as needed to effect real-time playback,
     * therefore you should invoke read() on a separate thread if this will impact the calling thread.
     * @throws IOException on any file IO errors
     */
    public AudioPacketMonoWaveReader(Path path, boolean realTime) throws IOException
    {
        super(path, realTime);
        setListener(this);
    }

    public void setMetadata(Metadata metadata)
    {
        mMetadata = metadata;
    }

    public void setListener(IAudioPacketListener listener)
    {
        mListener = listener;
    }

    @Override
    public void receive(RealBuffer realBuffer)
    {
        if(mListener != null)
        {
            mListener.getAudioPacketListener().receive(new AudioPacket(realBuffer.getSamples(), mMetadata));
        }
    }
}
