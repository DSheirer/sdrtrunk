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

import channel.metadata.Metadata;
import sample.real.RealBuffer;

public class AudioPacket
{
    private Type mType;
    private RealBuffer mAudioData;
    private Metadata mMetadata;

    public AudioPacket(Type type, Metadata metadata)
    {
        mType = type;
        mMetadata = metadata;
    }

    public AudioPacket(float[] audio, Metadata metadata)
    {
        this(Type.AUDIO, metadata);

        mAudioData = new RealBuffer(audio);
    }

    public boolean hasMetadata()
    {
        return mMetadata != null;
    }

    public Metadata getMetadata()
    {
        return mMetadata;
    }

    public void setMetadata(Metadata metadata)
    {
        mMetadata = metadata;
    }

    public Type getType()
    {
        return mType;
    }

    public RealBuffer getAudioBuffer()
    {
        return mAudioData;
    }

    public boolean hasAudioBuffer()
    {
        return mAudioData != null;
    }

    public enum Type
    {
        AUDIO,
        END;
    }
}
