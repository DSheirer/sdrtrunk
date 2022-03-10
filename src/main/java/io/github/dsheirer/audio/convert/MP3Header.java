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

/**
 * Utility class for inspecting/parsing MP3 frames
 */
public class MP3Header
{
    private MP3Header() {}

    /**
     * Indicates if the byte array at the offset has a valid MP3 frame sync and is at least 4 bytes long
     * @param frame array of bytes
     * @param offset into the array
     * @return true if the first two offset bytes contain a frame sync and length from offset is at least 4
     */
    public static boolean isValid(byte[] frame, int offset)
    {
        return frame.length >= (offset + 4) && hasSync(frame, offset);
    }

    /**
     * Indicates if the byte array at the offset has a valid MP3 frame sync
     * @param frame array of bytes
     * @param offset into the array
     * @return true if the first two offset bytes contain a frame sync
     */
    public static boolean hasSync(byte[] frame, int offset)
    {
        return (frame[offset] & 0xFF) == 0xFF && (frame[offset + 1] & 0xE0) == 0xE0;
    }

    /**
     * MPEG version for the frame
     */
    public static MPEGVersion getMPEGVersion(byte[] frame, int offset)
    {
        return MPEGVersion.fromValue((frame[offset + 1] & 0x18) >> 3);
    }

    /**
     * MPEG layer for the frame.  Normally layer 3 for audio.
     */
    public static MPEGLayer getMPEGLayer(byte[] frame, int offset)
    {
        return MPEGLayer.fromValue((frame[offset + 1] & 0x6) >> 1);
    }

    /**
     * MP3 bit rate
     */
    public static int getBitRate(byte[] frame, int offset)
    {
        return MP3BitRate.getLayer3BitRate(getMPEGVersion(frame, offset), (frame[offset + 2] & 0xF0) >> 4);
    }

    /**
     * Input audio sample rate.
     */
    public static int getSampleRate(byte[] frame, int offset)
    {
        return MP3SampleRate.getSampleRate(getMPEGVersion(frame, offset), (frame[offset + 2] & 0x0C) >> 2);
    }

    /**
     * Channel mode
     */
    public static ChannelMode getChannelMode(byte[] frame, int offset)
    {
        return ChannelMode.fromValue((frame[offset + 3] & 0xC0) >> 6);
    }

    /**
     * Inspects the frame bytes and provides details about the header.
     */
    public static String inspect(byte[] frame, int offset)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MP3 FRAME");

        if(!isValid(frame, offset))
        {
            sb.append(" INVALID");
            return sb.toString();
        }

        sb.append(" VER:").append(getMPEGVersion(frame, offset));
        sb.append(" MODE:").append(getChannelMode(frame, offset));
        sb.append(" ").append(getMPEGLayer(frame, offset));
        sb.append(" BR:").append(getBitRate(frame, offset));
        sb.append(" SR:").append(getSampleRate(frame, offset));

        //TODO: add MONO/STEREO
        
        return sb.toString();
    }
}
