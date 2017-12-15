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
package record.mp3;

import audio.AudioPacket;
import audio.convert.MP3AudioConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.AudioRecorder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * MP3 recorder for converting 8 kHz PCM audio packets to MP3 and writing to .mp3 file.
 */
public class MP3Recorder extends AudioRecorder
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3Recorder.class);

    public static final int MP3_BIT_RATE = 16;
    public static final boolean CONSTANT_BIT_RATE = false;

    private MP3AudioConverter mMP3Converter;

    /**
     * MP3 audio recorder module for converting audio packets to 16 kHz constant bit rate MP3 format and
     * recording to a file.
     *
     * @param path to the output file.  File name should include the .mp3 file extension.
     */
    public MP3Recorder(Path path)
    {
        super(path);

        mMP3Converter = new MP3AudioConverter(MP3_BIT_RATE, CONSTANT_BIT_RATE);
    }

    @Override
    protected void record(List<AudioPacket> audioPackets) throws IOException
    {
        OutputStream outputStream = getOutputStream();

        if(outputStream != null)
        {
            processMetadata(audioPackets);

            byte[] mp3Audio = mMP3Converter.convert(audioPackets);

            outputStream.write(mp3Audio);
        }
    }

    @Override
    protected void flush()
    {
        byte[] partialFrame = mMP3Converter.flush();

        if(partialFrame != null && partialFrame.length > 0)
        {
            try
            {
                getOutputStream().write(partialFrame);
            }
            catch(IOException ioe)
            {
                mLog.error("Error writing final audio frame data to file", ioe);
            }
        }
    }

    /**
     * Processes audio metadata contained in the audio packets and converts the metadata to MP3 ID3 metadata tags and
     * writes the ID3 tags to the output stream.
     * @param audioPackets
     */
    private void processMetadata(List<AudioPacket> audioPackets)
    {
        //TODO: detect metadata changes and write out ID3 tags to the MP3 stream
    }
}
