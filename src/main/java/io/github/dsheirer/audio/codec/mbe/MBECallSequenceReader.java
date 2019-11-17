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

package io.github.dsheirer.audio.codec.mbe;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import io.github.dsheirer.module.decode.p25.audio.VoiceFrame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reader for MBE call sequence recordings
 */
public class MBECallSequenceReader
{
    public static List<String> getAudioFrames(Path path) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.readValue(path.toFile(), MBECallSequence.class);

        if(obj instanceof MBECallSequence)
        {
            MBECallSequence sequence = (MBECallSequence)obj;

            List<String> audioFrames = new ArrayList<>();

            for(VoiceFrame voiceFrame: sequence.getVoiceFrames())
            {
                audioFrames.add(voiceFrame.getFrame());
            }

            return audioFrames;
        }

        return Collections.emptyList();
    }

    public static void main(String[] args)
    {
        Path path = Path.of("/home/denny/SDRTrunk/recordings/20190706063149_154250000_7_TS1_65084_6591001.mbe");

        try
        {
            List<String> frames = MBECallSequenceReader.getAudioFrames(path);

            Files.writeString(Path.of("/home/denny/SDRTrunk/recordings/mbe_frames.txt"), Joiner.on("\",\n\"").join(frames));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
