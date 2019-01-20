/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.action.AliasActionManager;
import io.github.dsheirer.audio.AudioModule;
import io.github.dsheirer.channel.state.AlwaysUnsquelchedDecoderState;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.filter.AllPassFilter;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.filter.IFilter;
import io.github.dsheirer.gui.editor.EmptyValidatingEditor;
import io.github.dsheirer.gui.editor.ValidatingEditor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.am.AMDecoder;
import io.github.dsheirer.module.decode.am.AMDecoderEditor;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.fleetsync2.Fleetsync2Decoder;
import io.github.dsheirer.module.decode.fleetsync2.Fleetsync2DecoderState;
import io.github.dsheirer.module.decode.fleetsync2.FleetsyncMessageFilter;
import io.github.dsheirer.module.decode.lj1200.LJ1200Decoder;
import io.github.dsheirer.module.decode.lj1200.LJ1200DecoderState;
import io.github.dsheirer.module.decode.lj1200.LJ1200MessageFilter;
import io.github.dsheirer.module.decode.ltrnet.DecodeConfigLTRNet;
import io.github.dsheirer.module.decode.ltrnet.LTRNetDecoder;
import io.github.dsheirer.module.decode.ltrnet.LTRNetDecoderEditor;
import io.github.dsheirer.module.decode.ltrnet.LTRNetDecoderState;
import io.github.dsheirer.module.decode.ltrnet.LTRNetMessageFilter;
import io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard;
import io.github.dsheirer.module.decode.ltrstandard.LTRStandardDecoder;
import io.github.dsheirer.module.decode.ltrstandard.LTRStandardDecoderEditor;
import io.github.dsheirer.module.decode.ltrstandard.LTRStandardDecoderState;
import io.github.dsheirer.module.decode.ltrstandard.LTRStandardMessageFilter;
import io.github.dsheirer.module.decode.mdc1200.MDCDecoder;
import io.github.dsheirer.module.decode.mdc1200.MDCDecoderState;
import io.github.dsheirer.module.decode.mdc1200.MDCMessageFilter;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.mpt1327.MPT1327Decoder;
import io.github.dsheirer.module.decode.mpt1327.MPT1327DecoderEditor;
import io.github.dsheirer.module.decode.mpt1327.MPT1327DecoderState;
import io.github.dsheirer.module.decode.mpt1327.MPT1327MessageFilter;
import io.github.dsheirer.module.decode.mpt1327.MPT1327TrafficChannelManager;
import io.github.dsheirer.module.decode.mpt1327.Sync;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoder;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoderEditor;
import io.github.dsheirer.module.decode.p25.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.P25DecoderC4FM;
import io.github.dsheirer.module.decode.p25.P25DecoderEditor;
import io.github.dsheirer.module.decode.p25.P25DecoderLSM;
import io.github.dsheirer.module.decode.p25.P25DecoderState;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.audio.P25AudioModule;
import io.github.dsheirer.module.decode.p25.message.filter.P25MessageFilterSet;
import io.github.dsheirer.module.decode.passport.DecodeConfigPassport;
import io.github.dsheirer.module.decode.passport.PassportDecoder;
import io.github.dsheirer.module.decode.passport.PassportDecoderEditor;
import io.github.dsheirer.module.decode.passport.PassportDecoderState;
import io.github.dsheirer.module.decode.passport.PassportMessageFilter;
import io.github.dsheirer.module.decode.tait.Tait1200Decoder;
import io.github.dsheirer.module.decode.tait.Tait1200DecoderState;
import io.github.dsheirer.module.demodulate.am.AMDemodulatorModule;
import io.github.dsheirer.module.demodulate.fm.FMDemodulatorModule;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.channel.ChannelRotationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DecoderFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DecoderFactory.class);

    private static final double AM_CHANNEL_BANDWIDTH = 3000.0;
    private static final double FM_CHANNEL_BANDWIDTH = 12500.0;
    private static final double DEMODULATED_AUDIO_SAMPLE_RATE = 8000.0;

    /**
     * Returns a list of one primary decoder and any auxiliary decoders, as
     * specified in the configurations.
     *
     * @return list of configured decoders
     */
    public static List<Module> getModules(ChannelMapModel channelMapModel, Channel channel, AliasModel aliasModel,
                                          UserPreferences userPreferences)
    {
        List<Module> modules = getPrimaryModules(channelMapModel, channel, aliasModel, userPreferences);
        modules.addAll(getAuxiliaryDecoders(channel.getAuxDecodeConfiguration()));
        return modules;
    }

    /**
     * Constructs a primary decoder as specified in the decode configuration
     */
    public static List<Module> getPrimaryModules(ChannelMapModel channelMapModel, Channel channel,
                                                 AliasModel aliasModel, UserPreferences userPreferences)
    {
        List<Module> modules = new ArrayList<Module>();

        //Adds an alias action manager when there are alias actions specified
        if(channel.getAliasListName() != null && !channel.getAliasListName().isEmpty())
        {
            AliasList aliasList = aliasModel.getAliasList(channel.getAliasListName());

            if(aliasList != null && aliasList.hasAliasActions())
            {
                modules.add(new AliasActionManager(aliasList));
            }
        }

        ChannelType channelType = channel.getChannelType();

        /* Baseband low-pass filter pass and stop frequencies */
        DecodeConfiguration decodeConfig = channel.getDecodeConfiguration();

        switch(decodeConfig.getDecoderType())
        {
            case AM:
                modules.add(new AMDecoder(decodeConfig));
                modules.add(new AlwaysUnsquelchedDecoderState(DecoderType.AM, channel.getName()));
                AudioModule audioModuleAM = new AudioModule();

                //Check if the user wants all audio recorded ..
                if(((DecodeConfigAM)decodeConfig).getRecordAudio())
                {
                    audioModuleAM.setRecordAudio(true);
                }
                modules.add(audioModuleAM);
                if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
                {
                    modules.add(new AMDemodulatorModule(AM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
                }
                break;
            case NBFM:
                modules.add(new NBFMDecoder(decodeConfig));
                modules.add(new AlwaysUnsquelchedDecoderState(DecoderType.NBFM, channel.getName()));
                AudioModule audioModuleFM = new AudioModule();

                //Check if the user wants all audio recorded ..
                if(((DecodeConfigNBFM)decodeConfig).getRecordAudio())
                {
                    audioModuleFM.setRecordAudio(true);
                }
                modules.add(audioModuleFM);
                if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
                {
                    modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
                }
                break;
            case LTR_STANDARD:
                MessageDirection direction = ((DecodeConfigLTRStandard)decodeConfig).getMessageDirection();
                modules.add(new LTRStandardDecoder(null, direction));
                modules.add(new LTRStandardDecoderState());
                modules.add(new AudioModule());
                if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
                {
                    modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
                }
                break;
            case LTR_NET:
                modules.add(new LTRNetDecoder((DecodeConfigLTRNet)decodeConfig));
                modules.add(new LTRNetDecoderState());
                modules.add(new AudioModule());
                if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
                {
                    modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
                }
                break;
            case MPT1327:
                DecodeConfigMPT1327 mptConfig = (DecodeConfigMPT1327)decodeConfig;
                ChannelMap channelMap = channelMapModel.getChannelMap(mptConfig.getChannelMapName());
                Sync sync = mptConfig.getSync();
                modules.add(new MPT1327Decoder(sync));
                modules.add(new AudioModule());
                SourceType sourceType = channel.getSourceConfiguration().getSourceType();
                if(sourceType == SourceType.TUNER || sourceType == SourceType.TUNER_MULTIPLE_FREQUENCIES)
                {
                    modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
                }

                long timeout = mptConfig.getCallTimeout() * 1000;

                if(channelType == ChannelType.STANDARD)
                {
                    MPT1327TrafficChannelManager trafficChannelManager = new MPT1327TrafficChannelManager(channel, channelMap);
                    modules.add(trafficChannelManager);
                    modules.add(new MPT1327DecoderState(trafficChannelManager, channelType, timeout));
                }
                else
                {
                    modules.add(new MPT1327DecoderState(channelType, timeout));
                }

                //Add a channel rotation monitor when we have multiple control channel frequencies specified
                if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency &&
                    ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).hasMultipleFrequencies())
                {
                    List<State> activeStates = new ArrayList<>();
                    activeStates.add(State.CONTROL);
                    modules.add(new ChannelRotationMonitor(activeStates, userPreferences));
                }
                break;
            case PASSPORT:
                modules.add(new PassportDecoder(decodeConfig));
                modules.add(new PassportDecoderState());
                modules.add(new AudioModule());
                if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
                {
                    modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
                }
                break;
            case P25_PHASE1:
                DecodeConfigP25Phase1 p25Config = (DecodeConfigP25Phase1)decodeConfig;

                switch(p25Config.getModulation())
                {
                    case C4FM:
                        modules.add(new P25DecoderC4FM());
                        break;
                    case CQPSK:
                        modules.add(new P25DecoderLSM());
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized P25 Phase 1 Modulation [" +
                            p25Config.getModulation() + "]");
                }

                if(channelType == ChannelType.STANDARD)
                {
                    P25TrafficChannelManager trafficChannelManager = new P25TrafficChannelManager(channel);
                    modules.add(trafficChannelManager);
                    modules.add(new P25DecoderState(channel, trafficChannelManager));
                }
                else
                {
                    modules.add(new P25DecoderState(channel));
                }

                modules.add(new P25AudioModule(userPreferences));

                //Add a channel rotation monitor when we have multiple control channel frequencies specified
                if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency &&
                    ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).hasMultipleFrequencies())
                {
                    List<State> activeStates = new ArrayList<>();
                    activeStates.add(State.CONTROL);
                    modules.add(new ChannelRotationMonitor(activeStates, userPreferences));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown decoder type [" + decodeConfig.getDecoderType().toString() + "]");
        }

        return modules;
    }

    /**
     * Constructs a list of auxiliary decoders, as specified in the configuration
     *
     * @param config - auxiliary configuration
     * @return - list of auxiliary decoders
     */
    public static List<Module> getAuxiliaryDecoders(AuxDecodeConfiguration config)
    {
        List<Module> modules = new ArrayList<>();

        if(config != null)
        {
            for(DecoderType auxDecoder : config.getAuxDecoders())
            {
                switch(auxDecoder)
                {
                    case FLEETSYNC2:
                        modules.add(new Fleetsync2Decoder());
                        modules.add(new Fleetsync2DecoderState());
                        break;
                    case MDC1200:
                        modules.add(new MDCDecoder());
                        modules.add(new MDCDecoderState());
                        break;
                    case LJ_1200:
                        modules.add(new LJ1200Decoder());
                        modules.add(new LJ1200DecoderState());
                        break;
                    case TAIT_1200:
                        modules.add(new Tait1200Decoder());
                        modules.add(new Tait1200DecoderState());
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized auxiliary decoder type [" + auxDecoder + "]");
                }
            }
        }

        return modules;
    }

    /**
     * Assembles a filter set containing filters for the primary channel
     * decoder and each of the auxiliary decoders
     */
    public static FilterSet<IMessage> getMessageFilters(List<Module> modules)
    {
        FilterSet<IMessage> filterSet = new FilterSet<>();

        for(Module module : modules)
        {
            if(module instanceof Decoder)
            {
                filterSet.addFilters(getMessageFilter(((Decoder)module).getDecoderType()));
            }
        }

        /* If we don't have any filters, add an ALL-PASS filter */
        if(filterSet.getFilters().isEmpty())
        {
            filterSet.addFilter(new AllPassFilter<>());
        }

        return filterSet;
    }

    /**
     * Returns a set of IMessageFilter objects (FilterSets or Filters) that
     * can process all of the messages produced by the specified decoder type.
     */
    public static List<IFilter<IMessage>> getMessageFilter(DecoderType decoder)
    {
        ArrayList<IFilter<IMessage>> filters = new ArrayList<>();

        switch(decoder)
        {
            case FLEETSYNC2:
                filters.add(new FleetsyncMessageFilter());
                break;
            case LJ_1200:
                filters.add(new LJ1200MessageFilter());
                break;
            case LTR_NET:
                filters.add(new LTRNetMessageFilter());
                break;
            case LTR_STANDARD:
                filters.add(new LTRStandardMessageFilter());
                break;
            case MDC1200:
                filters.add(new MDCMessageFilter());
                break;
            case MPT1327:
                filters.add(new MPT1327MessageFilter());
                break;
            case P25_PHASE1:
                filters.add(new P25MessageFilterSet());
                break;
            case PASSPORT:
                filters.add(new PassportMessageFilter());
                break;
            default:
                break;
        }

        return filters;
    }

    public static DecodeConfiguration getDefaultDecodeConfiguration()
    {
        return getDecodeConfiguration(DecoderType.NBFM);
    }

    public static DecodeConfiguration getDecodeConfiguration(DecoderType decoder)
    {
        DecodeConfiguration retVal;

        switch(decoder)
        {
            case AM:
                return new DecodeConfigAM();
            case LTR_NET:
                return new DecodeConfigLTRNet();
            case LTR_STANDARD:
                return new DecodeConfigLTRStandard();
            case MPT1327:
                return new DecodeConfigMPT1327();
            case NBFM:
                return new DecodeConfigNBFM();
            case PASSPORT:
                return new DecodeConfigPassport();
            case P25_PHASE1:
                return new DecodeConfigP25Phase1();
            default:
                throw new IllegalArgumentException("DecodeConfigFactory - unknown decoder type [" + decoder.toString() + "]");
        }
    }

    public static ValidatingEditor<Channel> getEditor(DecoderType type, ChannelMapModel model)
    {
        switch(type)
        {
            case AM:
                return new AMDecoderEditor();
            case LTR_NET:
                return new LTRNetDecoderEditor();
            case LTR_STANDARD:
                return new LTRStandardDecoderEditor();
            case MPT1327:
                return new MPT1327DecoderEditor(model);
            case NBFM:
                return new NBFMDecoderEditor();
            case P25_PHASE1:
                return new P25DecoderEditor();
            case PASSPORT:
                return new PassportDecoderEditor();
            default:
                break;
        }

        return new EmptyValidatingEditor<Channel>("a decoder");
    }

    /**
     * Creates a copy of the configuration
     */
    public static DecodeConfiguration copy(DecodeConfiguration config)
    {
        if(config != null)
        {
            switch(config.getDecoderType())
            {
                case AM:
                    return new DecodeConfigAM();
                case LTR_NET:
                    DecodeConfigLTRNet originalLTRNet = (DecodeConfigLTRNet)config;
                    DecodeConfigLTRNet copyLTRNet = new DecodeConfigLTRNet();
                    copyLTRNet.setMessageDirection(originalLTRNet.getMessageDirection());
                    return copyLTRNet;
                case LTR_STANDARD:
                    DecodeConfigLTRStandard originalLTRStandard = (DecodeConfigLTRStandard)config;
                    DecodeConfigLTRStandard copyLTRStandard = new DecodeConfigLTRStandard();
                    copyLTRStandard.setMessageDirection(originalLTRStandard.getMessageDirection());
                    return copyLTRStandard;
                case MPT1327:
                    DecodeConfigMPT1327 originalMPT = (DecodeConfigMPT1327)config;
                    DecodeConfigMPT1327 copyMPT = new DecodeConfigMPT1327();
                    copyMPT.setCallTimeout(originalMPT.getCallTimeout());
                    copyMPT.setChannelMapName(originalMPT.getChannelMapName());
                    copyMPT.setSync(originalMPT.getSync());
                    copyMPT.setTrafficChannelPoolSize(originalMPT.getTrafficChannelPoolSize());
                    return copyMPT;
                case NBFM:
                    return new DecodeConfigNBFM();
                case P25_PHASE1:
                    DecodeConfigP25Phase1 originalP25 = (DecodeConfigP25Phase1)config;
                    DecodeConfigP25Phase1 copyP25 = new DecodeConfigP25Phase1();
                    copyP25.setIgnoreDataCalls(originalP25.getIgnoreDataCalls());
                    copyP25.setModulation(originalP25.getModulation());
                    copyP25.setTrafficChannelPoolSize(originalP25.getTrafficChannelPoolSize());
                    return copyP25;
                case PASSPORT:
                    return new DecodeConfigPassport();
                default:
                    throw new IllegalArgumentException("Unrecognized decoder configuration type:" + config.getDecoderType());
            }
        }

        return null;
    }

    /**
     * Decoder(s) that support bitstreams indicating that they produce ReusableByteBuffers of decoded bits.
     *
     * @return set of decoders that support bitstreams.
     */
    public static EnumSet<DecoderType> getBitstreamDecoders()
    {
        return EnumSet.of(DecoderType.P25_PHASE1, DecoderType.MPT1327);
    }
}