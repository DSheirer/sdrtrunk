/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import java.util.Collections;
import java.util.List;

/**
 * NXDN audio frame data
 */
public class Audio extends NXDNMessage
{
    private final AudioCodec mAudioCodec;
    private final List<byte[]> mAudioFrames;

    /**
     * Constructs an instance
     *
     * @param timestamp for the message
     */
    public Audio(AudioCodec audioCodec, List<byte[]> frames, long timestamp)
    {
        super(new CorrectedBinaryMessage(0), timestamp);
        mAudioCodec = audioCodec;
        mAudioFrames = frames;
    }

    /**
     * Audio frame data
     */
    public List<byte[]> getAudioFrames()
    {
        return mAudioFrames;
    }

    /**
     * AMBE+ audio codec format
     * @return full or half rate
     */
    public AudioCodec getAudioCodec()
    {
        return mAudioCodec;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
