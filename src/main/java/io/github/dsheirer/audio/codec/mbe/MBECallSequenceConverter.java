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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for converting MBE call sequences (*.mbe) to PCM wave audio format
 */
public class MBECallSequenceConverter
{
    private final static Logger mLog = LoggerFactory.getLogger(MBECallSequenceConverter.class);

//    public static void convert(Path input, Path output) throws IOException
//    {
//        InputStream inputStream = Files.newInputStream(input);
//        ObjectMapper mapper = new ObjectMapper();
//        MBECallSequence sequence = mapper.readValue(inputStream, MBECallSequence.class);
//        convert(sequence, output);
//    }
//
//    public static void convert(MBECallSequence callSequence, Path outputPath)
//    {
//        if(callSequence == null || callSequence.isEncrypted())
//        {
//            throw new IllegalArgumentException("Cannot decode null or encrypted call sequence");
//        }
//
//        if(callSequence != null && !callSequence.isEncrypted())
//        {
//            ThumbDv.AudioProtocol protocol = ThumbDv.AudioProtocol.P25_PHASE2;
//
//            AudioPacketWaveRecorder recorder = new AudioPacketWaveRecorder(outputPath);
//            recorder.start();
//
//            long delayMillis = 0;
//
//            try(ThumbDv thumbDv = new ThumbDv(protocol, recorder))
//            {
//                thumbDv.start();
//                for(VoiceFrame voiceFrame: callSequence.getVoiceFrames())
//                {
//                    mLog.debug("Frame [" + voiceFrame.getFrame() + "] + Hex [" + AmbeResponse.toHex(voiceFrame.getFrameBytes()) + "]");
//                    thumbDv.decode(voiceFrame.getFrameBytes());
//                    delayMillis += 30;
//                }
//
//                if(delayMillis > 0)
//                {
//                    delayMillis += 1000;
//                    try
//                    {
//                        Thread.sleep(delayMillis);
//                    }
//                    catch(InterruptedException ie)
//                    {
//
//                    }
//                }
//            }
//            catch(IOException ioe)
//            {
//                mLog.error("Error", ioe);
//            }
//
//            recorder.stop(Paths.get(outputPath.toString().replace(".tmp", ".wav")), new WaveMetadata());
//        }
//    }
//
//    public static void main(String[] args)
//    {
//        String mbe = "/home/denny/SDRTrunk/recordings/20190331085324_154250000_3_TS0_65084_6591007.mbe";
//        String mbe = "/home/denny/SDRTrunk/recordings/20190331085324_154250000_2_TS1_65035.mbe";
//
//        Path input = Paths.get(mbe);
//        Path output = Paths.get(mbe.replace(".mbe", ".tmp"));
//
//        mLog.info("Converting: " + mbe);
//
//        try
//        {
//            MBECallSequenceConverter.convert(input, output);
//        }
//        catch(IOException ioe)
//        {
//            mLog.error("Error", ioe);
//        }
//    }
}
