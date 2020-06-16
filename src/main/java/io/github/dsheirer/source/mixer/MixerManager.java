/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.mixer;

import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.sample.adapter.RealChannelShortAdapter;
import io.github.dsheirer.sample.adapter.RealShortAdapter;
import io.github.dsheirer.source.config.SourceConfigMixer;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.MixerTunerDataLine;
import io.github.dsheirer.source.tuner.MixerTunerType;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Utility class for accessing input, output and tuner mixers
 */
public class MixerManager
{
    private final static Logger mLog = LoggerFactory.getLogger(MixerManager.class);

    public MixerManager() {}

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
                                AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO);

                            TargetDataLine dataLine;

                            try
                            {
                                dataLine = (TargetDataLine)mixer.getMixer().getLine(info);

                                if(dataLine != null)
                                {
                                    return new RealMixerSource(dataLine, AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO,
                                        new RealShortAdapter("RealMixerSource - Mono"));
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
                                    AudioFormats.PCM_SIGNED_8KHZ_16BITS_STEREO);

                            TargetDataLine dataLine;

                            try
                            {
                                dataLine = (TargetDataLine)mixer.getMixer().getLine(info);

                                if(dataLine != null)
                                {
                                    return new RealMixerSource(dataLine, AudioFormats.PCM_SIGNED_8KHZ_16BITS_STEREO,
                                        new RealChannelShortAdapter(mixerConfig.getChannel(), "RealMixerSource-Stereo"));
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

    public static Collection<MixerTunerDataLine> getMixerTunerDataLines()
    {
        List<MixerTunerDataLine> tuners = new ArrayList<>();

        for(Mixer.Info mixerInfo : AudioSystem.getMixerInfo())
        {
            //Sort between the mixers and the tuner mixers, and load each
            MixerTunerType mixerTunerType = MixerTunerType.getMixerTunerType(mixerInfo);

            if(mixerTunerType != MixerTunerType.UNKNOWN)
            {
                TargetDataLine tdl = getTargetDataLine(mixerInfo, mixerTunerType.getAudioFormat());

                if(tdl != null)
                {
                    switch(mixerTunerType)
                    {
                        case FUNCUBE_DONGLE_PRO:
                        case FUNCUBE_DONGLE_PRO_PLUS:
                            tuners.add(new MixerTunerDataLine(tdl, mixerTunerType));
                            break;
                    }
                }
            }
        }

        return tuners;
    }

    private static TargetDataLine getTargetDataLine(Mixer.Info mixerInfo, AudioFormat format)
    {
        TargetDataLine retVal = null;

        Mixer mixer = AudioSystem.getMixer(mixerInfo);

        if(mixer != null)
        {
            try
            {
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
                AudioFormats.PCM_SIGNED_8KHZ_16BITS_STEREO);

        boolean stereoSupported = mixer.isLineSupported(stereoInfo);

        DataLine.Info monoInfo = new DataLine.Info(TargetDataLine.class,
                AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO);

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
                AudioFormats.PCM_SIGNED_8KHZ_16BITS_STEREO);

        boolean stereoSupported = mixer.isLineSupported(stereoInfo);

        DataLine.Info monoInfo = new DataLine.Info(SourceDataLine.class, AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO);

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
        for(MixerChannelConfiguration config : getOutputMixers())
        {
            mLog.debug(config.toString());
        }
    }
}
