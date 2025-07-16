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
package io.github.dsheirer.source.mixer;

import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.sample.adapter.RealChannelShortAdapter;
import io.github.dsheirer.sample.adapter.RealShortAdapter;
import io.github.dsheirer.source.config.SourceConfigMixer;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.MixerTunerType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Utility class for accessing input, output and tuner mixers
 */
public class MixerManager
{
    private final static Logger mLog = LoggerFactory.getLogger(MixerManager.class);

    public MixerManager()
    {

    }

    public static RealMixerSource getSource(SourceConfiguration config)
    {
        RealMixerSource retVal = null;

        if(config instanceof SourceConfigMixer)
        {
            SourceConfigMixer mixerConfig = (SourceConfigMixer)config;

            String mixerName = mixerConfig.getMixer();

            if(mixerName != null)
            {
                InputMixerConfiguration mixer = getInputMixer(mixerName);

                if(mixer != null)
                {
                    MixerChannel channel = mixerConfig.getChannel();

                    if(mixer.supportsChannel(channel))
                    {

                        if(channel == MixerChannel.MONO)
                        {
                            DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                                AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO);

                            TargetDataLine dataLine;

                            try
                            {
                                dataLine = (TargetDataLine)mixer.getMixer().getLine(info);

                                if(dataLine != null)
                                {
                                    return new RealMixerSource(dataLine, AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO,
                                        new RealShortAdapter());
                                }
                            }
                            catch(LineUnavailableException e)
                            {
                                mLog.error("couldn't get mixer data line for [" + mixerName + "] for channel [" +
                                    channel.name() + "]", e);
                            }

                        }
                        else //STEREO
                        {
                            DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                                    AudioFormats.PCM_SIGNED_8000_HZ_16BITS_STEREO);

                            TargetDataLine dataLine;

                            try
                            {
                                dataLine = (TargetDataLine)mixer.getMixer().getLine(info);

                                if(dataLine != null)
                                {
                                    return new RealMixerSource(dataLine, AudioFormats.PCM_SIGNED_8000_HZ_16BITS_STEREO,
                                        new RealChannelShortAdapter(mixerConfig.getChannel()));
                                }
                            }
                            catch(LineUnavailableException e)
                            {
                                mLog.error("couldn't get mixer data line for [" + mixerName + "] for channel [" +
                                    channel.name() + "]", e);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static List<InputMixerConfiguration> getInputMixers()
    {
        List<InputMixerConfiguration> inputMixers = new ArrayList<>();

        for(Mixer.Info mixerInfo : AudioSystem.getMixerInfo())
        {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            if(mixer != null)
            {
                EnumSet<MixerChannel> inputChannels = getSupportedTargetChannels(mixer);

                if(inputChannels != null)
                {
                    inputMixers.add(new InputMixerConfiguration(mixer, inputChannels));
                }
            }
        }

        return inputMixers;
    }

    public static InputMixerConfiguration getInputMixer(String name)
    {
        for(InputMixerConfiguration mixer : getInputMixers())
        {
            if(mixer.getMixerName().contentEquals(name))
            {
                return mixer;
            }
        }

        return null;
    }

    public static MixerChannelConfiguration getDefaultOutputMixer()
    {
        List<MixerChannelConfiguration> outputMixers = getOutputMixers();

        if(outputMixers.size() >= 1)
        {
            return outputMixers.get(0);
        }

        return null;
    }

    public static List<MixerChannelConfiguration> getOutputMixers()
    {
        List<MixerChannelConfiguration> outputMixers = new ArrayList<>();

        for(Mixer.Info mixerInfo : AudioSystem.getMixerInfo())
        {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            if(mixer != null)
            {
                List<MixerChannel> outputChannels = getSupportedSourceChannels(mixer);

                for(MixerChannel channel : outputChannels)
                {
                    outputMixers.add(new MixerChannelConfiguration(mixer, channel));
                }
            }
        }

        return outputMixers;
    }

    /**
     * Obtains a target data line for the specified mixer tuner type
     * @param mixerTunerType to find
     * @return target data line or null.
     */
    public static TargetDataLine getTunerTargetDataLine(MixerTunerType mixerTunerType)
    {
        for(Mixer.Info mixerInfo : AudioSystem.getMixerInfo())
        {
            MixerTunerType type = MixerTunerType.getMixerTunerType(mixerInfo);

            if(type != null && type == mixerTunerType)
            {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);

                if(mixer != null)
                {
                    for(Line.Info info : mixer.getTargetLineInfo())
                    {
                        if(info instanceof DataLine.Info)
                        {
                            try
                            {
                                Line line = mixer.getLine(info);

                                if(line instanceof TargetDataLine)
                                {
                                    return (TargetDataLine)line;
                                }
                            }
                            catch(LineUnavailableException lue)
                            {
                                mLog.error("Line Unavailable. Unable to get TargetDataLine for Mixer Tuner: " + mixerTunerType);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds the target data line supported format that most closely matches the specified audio format without
     * matching sample rates.
     * @param info for a data line
     * @param audioFormat to find a match
     * @return matching audio format or null.
     */
    public static AudioFormat getMatchingFormat(DataLine.Info info, AudioFormat audioFormat)
    {
        for(AudioFormat audioFormatToTest: info.getFormats())
        {
            if(audioFormat.getSampleSizeInBits() == audioFormatToTest.getSampleSizeInBits() &&
                audioFormat.getChannels() == audioFormatToTest.getChannels() &&
                !(audioFormat.isBigEndian() ^ audioFormatToTest.isBigEndian()))
            {
                return audioFormatToTest;
            }
        }

        return null;
    }

    private static TargetDataLine getTargetDataLine(Mixer.Info mixerInfo, AudioFormat format)
    {
        TargetDataLine retVal = null;

        Mixer mixer = AudioSystem.getMixer(mixerInfo);

        if(mixer != null)
        {
            try
            {
                for(Line line: mixer.getTargetLines())
                {
                    mLog.debug("Line: " + line.getLineInfo().toString());
                }

                for(Line line: mixer.getSourceLines())
                {
                    mLog.debug("Line: " + line.getLineInfo().toString());
                }

                Mixer.Info info = mixer.getMixerInfo();

                mLog.debug(info.toString());

                DataLine.Info datalineInfo = new DataLine.Info(TargetDataLine.class, format);

                retVal = (TargetDataLine)mixer.getLine(datalineInfo);
            }
            catch(Exception e)
            {
                //Do nothing ... we couldn't get the TDL
            }
        }

        return retVal;
    }

    private static EnumSet<MixerChannel> getSupportedTargetChannels(Mixer mixer)
    {
        DataLine.Info stereoInfo = new DataLine.Info(TargetDataLine.class,
                AudioFormats.PCM_SIGNED_8000_HZ_16BITS_STEREO);

        boolean stereoSupported = mixer.isLineSupported(stereoInfo);

        DataLine.Info monoInfo = new DataLine.Info(TargetDataLine.class,
                AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO);

        boolean monoSupported = mixer.isLineSupported(monoInfo);

        if(stereoSupported && monoSupported)
        {
            return EnumSet.of(MixerChannel.LEFT, MixerChannel.RIGHT, MixerChannel.MONO);
        }
        else if(stereoSupported)
        {
            return EnumSet.of(MixerChannel.LEFT, MixerChannel.RIGHT, MixerChannel.MONO);
        }
        else if(monoSupported)
        {
            return EnumSet.of(MixerChannel.MONO);
        }

        return null;
    }

    /**
     * Returns enumset of SourceDataLine (audio output) channels
     * (MONO and/or STEREO) supported by the mixer, or null if the mixer doesn't
     * have any source data lines.
     */
    private static List<MixerChannel> getSupportedSourceChannels(Mixer mixer)
    {
        List<MixerChannel> channels = new ArrayList<>();

        DataLine.Info stereoInfo = new DataLine.Info(SourceDataLine.class,
                AudioFormats.PCM_SIGNED_8000_HZ_16BITS_STEREO);

        boolean stereoSupported = mixer.isLineSupported(stereoInfo);

        DataLine.Info monoInfo = new DataLine.Info(SourceDataLine.class, AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO);

        boolean monoSupported = mixer.isLineSupported(monoInfo);

        if(stereoSupported)
        {
            channels.add(MixerChannel.STEREO);
        }

        if(monoSupported)
        {
            channels.add(MixerChannel.MONO);
        }

        return channels;
    }

    public static String getMixerDevices()
    {
        StringBuilder sb = new StringBuilder();

        for(Mixer.Info mixerInfo : AudioSystem.getMixerInfo())
        {
            sb.append("\n--------------------------------------------------");
            sb.append("\nMIXER name:").append(mixerInfo.getName())
                    .append("\n      desc:").append(mixerInfo.getDescription())
                    .append("\n      vendor:").append(mixerInfo.getVendor())
                    .append("\n      version:").append(mixerInfo.getVersion())
                    .append("\n");

            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info lineInfo1 = mixer.getLineInfo();

            String a = lineInfo1.toString();

            Line.Info[] infos = mixer.getTargetLineInfo();

            Line.Info[] sourceLines = mixer.getSourceLineInfo();

            for(Line.Info lineInfo : sourceLines)
            {
                sb.append("      SOURCE LINE desc:").append(lineInfo)
                        .append("\n               class:").append(lineInfo.getClass())
                        .append("\n               lineclass:").append(lineInfo.getLineClass())
                        .append("\n");
            }

            Line.Info[] targetLines = mixer.getTargetLineInfo();

            for(Line.Info lineInfo : targetLines)
            {
                sb.append("      TARGET LINE desc:").append(lineInfo)
                        .append("\n                class:").append(lineInfo.getClass())
                        .append("\n                lineclass:").append(lineInfo.getLineClass())
                        .append("\n");

                if(lineInfo instanceof DataLine.Info)
                {
                    DataLine.Info dli = (DataLine.Info)lineInfo;

                    for(AudioFormat format: dli.getFormats())
                    {
                        sb.append(" FORMAT:").append(format.toString()).append("\n");

                        if(mixerInfo.getName().startsWith("V"))
                        {
                            sb.append("Iterating formats for " + mixerInfo.getName() + " " + mixerInfo.getDescription()).append("\n");
                            try
                            {
                                Line line = mixer.getLine(lineInfo);
                                sb.append(line.getLineInfo().toString()).append("\n");

                                if(line instanceof TargetDataLine)
                                {
                                    TargetDataLine tdl = (TargetDataLine)line;
                                    tdl.open();
                                    tdl.start();
                                    byte[] bytes = new byte[1024];
                                    int read = tdl.read(bytes, 0, bytes.length);
                                    sb.append("READ:" + read).append("\n");
                                    tdl.close();
                                }
                            }
                            catch(Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            Line.Info portInfo = new Line.Info(Port.class);

            if(mixer.isLineSupported(portInfo))
            {
                sb.append("**PORT LINE IS SUPPORTED BY THIS MIXER***\n");
            }
        }

        return sb.toString();
    }

    public static class InputMixerConfiguration
    {
        private Mixer mMixer;
        private EnumSet<MixerChannel> mChannels;

        public InputMixerConfiguration(Mixer mixer, EnumSet<MixerChannel> channels)
        {
            mMixer = mixer;
            mChannels = channels;
        }

        public Mixer getMixer()
        {
            return mMixer;
        }

        public String getMixerName()
        {
            return mMixer.getMixerInfo().getName();
        }

        public EnumSet<MixerChannel> getChannels()
        {
            return mChannels;
        }

        public boolean supportsChannel(MixerChannel channel)
        {
            return mChannels.contains(channel);
        }

        public String toString()
        {
            return mMixer.getMixerInfo().getName();
        }
    }

    public static void main(String[] args)
    {
        List<MixerChannelConfiguration> configs = MixerManager.getOutputMixers();

        for(MixerChannelConfiguration config : configs)
        {
            System.out.println(config);
        }

        System.out.println(MixerManager.getMixerDevices());
    }
}
