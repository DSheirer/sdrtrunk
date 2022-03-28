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
package io.github.dsheirer.module.decode;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.action.AliasActionManager;
import io.github.dsheirer.audio.AbstractAudioModule;
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
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.am.AMDecoder;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.module.decode.dmr.DMRDecoderState;
import io.github.dsheirer.module.decode.dmr.DMRTrafficChannelManager;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.dmr.audio.DMRAudioModule;
import io.github.dsheirer.module.decode.fleetsync2.Fleetsync2Decoder;
import io.github.dsheirer.module.decode.fleetsync2.Fleetsync2DecoderState;
import io.github.dsheirer.module.decode.fleetsync2.FleetsyncMessageFilter;
import io.github.dsheirer.module.decode.lj1200.LJ1200Decoder;
import io.github.dsheirer.module.decode.lj1200.LJ1200DecoderState;
import io.github.dsheirer.module.decode.lj1200.LJ1200MessageFilter;
import io.github.dsheirer.module.decode.ltrnet.DecodeConfigLTRNet;
import io.github.dsheirer.module.decode.ltrnet.LTRNetDecoder;
import io.github.dsheirer.module.decode.ltrnet.LTRNetDecoderState;
import io.github.dsheirer.module.decode.ltrnet.LTRNetMessageFilter;
import io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard;
import io.github.dsheirer.module.decode.ltrstandard.LTRStandardDecoder;
import io.github.dsheirer.module.decode.ltrstandard.LTRStandardDecoderState;
import io.github.dsheirer.module.decode.ltrstandard.LTRStandardMessageFilter;
import io.github.dsheirer.module.decode.mdc1200.MDCDecoder;
import io.github.dsheirer.module.decode.mdc1200.MDCDecoderState;
import io.github.dsheirer.module.decode.mdc1200.MDCMessageFilter;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.mpt1327.MPT1327Decoder;
import io.github.dsheirer.module.decode.mpt1327.MPT1327DecoderState;
import io.github.dsheirer.module.decode.mpt1327.MPT1327MessageFilter;
import io.github.dsheirer.module.decode.mpt1327.MPT1327TrafficChannelManager;
import io.github.dsheirer.module.decode.mpt1327.Sync;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoder;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoderState;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.audio.P25P1AudioModule;
import io.github.dsheirer.module.decode.p25.audio.P25P2AudioModule;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DecoderC4FM;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DecoderLSM;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DecoderState;
import io.github.dsheirer.module.decode.p25.phase1.message.filter.P25MessageFilterSet;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.P25P2DecoderHDQPSK;
import io.github.dsheirer.module.decode.p25.phase2.P25P2DecoderState;
import io.github.dsheirer.module.decode.passport.DecodeConfigPassport;
import io.github.dsheirer.module.decode.passport.PassportDecoder;
import io.github.dsheirer.module.decode.passport.PassportDecoderState;
import io.github.dsheirer.module.decode.passport.PassportMessageFilter;
import io.github.dsheirer.module.decode.tait.Tait1200Decoder;
import io.github.dsheirer.module.decode.tait.Tait1200DecoderState;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;
import io.github.dsheirer.module.demodulate.am.AMDemodulatorModule;
import io.github.dsheirer.module.demodulate.fm.FMDemodulatorModule;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.channel.rotation.ChannelRotationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
                                          UserPreferences userPreferences, TrafficChannelManager trafficChannelManager)
    {
        List<Module> modules = getPrimaryModules(channelMapModel, channel, aliasModel, userPreferences, trafficChannelManager);
        modules.addAll(getAuxiliaryDecoders(channel.getAuxDecodeConfiguration()));
        return modules;
    }

    /**
     * Constructs a primary decoder as specified in the decode configuration
     *
     * @param channelMapModel for channel map lookups
     * @param channel configuration
     * @param aliasModel for alias lookups
     * @param userPreferences instance
     * @param trafficChannelManager optional traffic channel manager to use
     * @return list of modules to use for a processing chain
     */
    public static List<Module> getPrimaryModules(ChannelMapModel channelMapModel, Channel channel, AliasModel aliasModel,
                                                 UserPreferences userPreferences, TrafficChannelManager trafficChannelManager)
    {
        List<Module> modules = new ArrayList<Module>();

        AliasList aliasList = aliasModel.getAliasList(channel.getAliasListName());
        modules.add(new AliasActionManager(aliasList));

        ChannelType channelType = channel.getChannelType();

        /* Baseband low-pass filter pass and stop frequencies */
        DecodeConfiguration decodeConfig = channel.getDecodeConfiguration();

        switch(decodeConfig.getDecoderType())
        {
            case AM:
                processAM(channel, modules, aliasList, decodeConfig);
                break;
            case DMR:
                processDMR(channel, userPreferences, modules, aliasList, (DecodeConfigDMR)decodeConfig,
                    trafficChannelManager);
                break;
            case NBFM:
                processNBFM(channel, modules, aliasList, decodeConfig);
                break;
            case LTR:
                processLTRStandard(channel, modules, aliasList, (DecodeConfigLTRStandard) decodeConfig);
                break;
            case LTR_NET:
                processLTRNet(channel, modules, aliasList, (DecodeConfigLTRNet) decodeConfig);
                break;
            case MPT1327:
                processMPT1327(channelMapModel, channel, modules, aliasList, channelType, (DecodeConfigMPT1327) decodeConfig);
                break;
            case PASSPORT:
                processPassport(channel, modules, aliasList, decodeConfig);
                break;
            case P25_PHASE1:
                processP25Phase1(channel, userPreferences, modules, aliasList, channelType, (DecodeConfigP25Phase1) decodeConfig);
                break;
            case P25_PHASE2:
                processP25Phase2(channel, userPreferences, modules, aliasList);
                break;
            default:
                throw new IllegalArgumentException("Unknown decoder type [" + decodeConfig.getDecoderType().toString() + "]");
        }

        return modules;
    }

    private static void processP25Phase2(Channel channel, UserPreferences userPreferences, List<Module> modules, AliasList aliasList) {
        modules.add(new P25P2DecoderHDQPSK((DecodeConfigP25Phase2)channel.getDecodeConfiguration()));

        modules.add(new P25P2DecoderState(channel, 0));
        modules.add(new P25P2DecoderState(channel, 1));
        modules.add(new P25P2AudioModule(userPreferences, 0, aliasList));
        modules.add(new P25P2AudioModule(userPreferences, 1, aliasList));
    }

    private static void processP25Phase1(Channel channel, UserPreferences userPreferences, List<Module> modules, AliasList aliasList, ChannelType channelType, DecodeConfigP25Phase1 decodeConfig) {
        DecodeConfigP25Phase1 p25Config = decodeConfig;

        switch(p25Config.getModulation())
        {
            case C4FM:
                modules.add(new P25P1DecoderC4FM());
                break;
            case CQPSK:
                modules.add(new P25P1DecoderLSM());
                break;
            default:
                throw new IllegalArgumentException("Unrecognized P25 Phase 1 Modulation [" +
                    p25Config.getModulation() + "]");
        }

        if(channelType == ChannelType.STANDARD)
        {
            P25TrafficChannelManager trafficChannelManager = new P25TrafficChannelManager(channel);
            modules.add(trafficChannelManager);
            modules.add(new P25P1DecoderState(channel, trafficChannelManager));
        }
        else
        {
            modules.add(new P25P1DecoderState(channel));
        }

        modules.add(new P25P1AudioModule(userPreferences, aliasList));

        //Add a channel rotation monitor when we have multiple control channel frequencies specified
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency &&
            ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).hasMultipleFrequencies())
        {
            List<State> activeStates = new ArrayList<>();
            activeStates.add(State.CONTROL);
            modules.add(new ChannelRotationMonitor(activeStates,
                ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).getFrequencyRotationDelay()));
        }
    }

    private static void processPassport(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfiguration decodeConfig) {
        modules.add(new PassportDecoder(decodeConfig));
        modules.add(new PassportDecoderState());
        modules.add(new AudioModule(aliasList));
        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
        }
    }

    private static void processMPT1327(ChannelMapModel channelMapModel, Channel channel, List<Module> modules, AliasList aliasList, ChannelType channelType, DecodeConfigMPT1327 decodeConfig) {
        DecodeConfigMPT1327 mptConfig = decodeConfig;
        ChannelMap channelMap = channelMapModel.getChannelMap(mptConfig.getChannelMapName());
        Sync sync = mptConfig.getSync();
        modules.add(new MPT1327Decoder(sync));

        final int callTimeoutMilliseconds = mptConfig.getCallTimeoutSeconds() * 1000;

        // Set max segment audio sample length slightly above call timeout to
        // not create a new segment if the processing chain finishes a bit after
        // actual call timeout.
        long maxAudioSegmentLengthMillis = (callTimeoutMilliseconds + 5000);
        modules.add(new AudioModule(aliasList, AbstractAudioModule.DEFAULT_TIMESLOT, maxAudioSegmentLengthMillis));

        SourceType sourceType = channel.getSourceConfiguration().getSourceType();
        if(sourceType == SourceType.TUNER || sourceType == SourceType.TUNER_MULTIPLE_FREQUENCIES)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
        }

        if(channelType == ChannelType.STANDARD)
        {
            MPT1327TrafficChannelManager trafficChannelManager = new MPT1327TrafficChannelManager(channel, channelMap);
            modules.add(trafficChannelManager);
            modules.add(new MPT1327DecoderState(trafficChannelManager, channelType, callTimeoutMilliseconds));
        }
        else
        {
            modules.add(new MPT1327DecoderState(channelType, callTimeoutMilliseconds));
        }

        //Add a channel rotation monitor when we have multiple control channel frequencies specified
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency &&
            ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).hasMultipleFrequencies())
        {
            List<State> activeStates = new ArrayList<>();
            activeStates.add(State.CONTROL);
            modules.add(new ChannelRotationMonitor(activeStates,
                ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).getFrequencyRotationDelay()));
        }
    }

    private static void processLTRNet(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfigLTRNet decodeConfig) {
        modules.add(new LTRNetDecoder(decodeConfig));
        modules.add(new LTRNetDecoderState());
        modules.add(new AudioModule(aliasList));
        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
        }
    }

    private static void processLTRStandard(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfigLTRStandard decodeConfig) {
        MessageDirection direction = decodeConfig.getMessageDirection();
        modules.add(new LTRStandardDecoder(null, direction));
        modules.add(new LTRStandardDecoderState());
        modules.add(new AudioModule(aliasList));
        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
        }
    }

    private static void processNBFM(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfiguration decodeConfig)
    {
        if(!(decodeConfig instanceof DecodeConfigNBFM))
        {
            throw new IllegalArgumentException("Can't create NBFM decoder - unrecognized decode config type: " +
                    (decodeConfig != null ? decodeConfig.getClass() : "null/empty"));
        }

        DecodeConfigNBFM decodeConfigNBFM = (DecodeConfigNBFM)decodeConfig;

        modules.add(new NBFMDecoder(decodeConfigNBFM));
        modules.add(new NBFMDecoderState(channel.getName(), decodeConfigNBFM));
        modules.add(new AudioModule(aliasList));
    }

    private static void processAM(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfiguration decodeConfig) {
        modules.add(new AMDecoder(decodeConfig));
        modules.add(new AlwaysUnsquelchedDecoderState(DecoderType.AM, channel.getName()));

        AudioModule audioModuleAM = new AudioModule(aliasList);
        modules.add(audioModuleAM);

        //Check if the user wants all audio recorded ..
        if(((DecodeConfigAM)decodeConfig).getRecordAudio())
        {
            audioModuleAM.setRecordAudio(true);
        }

        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            modules.add(new AMDemodulatorModule(AM_CHANNEL_BANDWIDTH, DEMODULATED_AUDIO_SAMPLE_RATE));
        }
    }

    /**
     * Creates modules for DMR decoder setup.
     *
     * Note: on some DMR systems (e.g. Capacity+) we convert standard channels to traffic channels (when rest channel
     * changes) and so we reuse the traffic channel manager from the converted channel.
     *
     * @param channel for the DMR decoder
     * @param userPreferences for access to JMBE audio library
     * @param modules list to add to
     * @param aliasList for the audio module
     * @param decodeConfig for the DMR configuration
     * @param trafficChannelManager optional traffic channel manager to re-use
     */
    private static void processDMR(Channel channel, UserPreferences userPreferences, List<Module> modules,
                                   AliasList aliasList, DecodeConfigDMR decodeConfig,
                                   TrafficChannelManager trafficChannelManager)
    {
        modules.add(new DMRDecoder(decodeConfig));

        DMRTrafficChannelManager dmrTrafficChannelManager = null;

        if(channel.isStandardChannel())
        {
            if(trafficChannelManager instanceof DMRTrafficChannelManager)
            {
                dmrTrafficChannelManager = (DMRTrafficChannelManager)trafficChannelManager;
            }
            else
            {
                dmrTrafficChannelManager = new DMRTrafficChannelManager(channel);
            }

            modules.add(dmrTrafficChannelManager);
        }

        modules.add(new DMRDecoderState(channel, 1, dmrTrafficChannelManager));
        modules.add(new DMRDecoderState(channel, 2, dmrTrafficChannelManager));
        modules.add(new DMRAudioModule(userPreferences, aliasList, 1));
        modules.add(new DMRAudioModule(userPreferences, aliasList, 2));

        //Add a channel rotation monitor when we have multiple control channel frequencies specified
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency &&
            ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).hasMultipleFrequencies())
        {
            List<State> activeStates = new ArrayList<>();
            activeStates.add(State.CONTROL);
            modules.add(new ChannelRotationMonitor(activeStates,
                ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).getFrequencyRotationDelay()));
        }
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
            case LTR:
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
            case DMR:
                //filters.add(new DMR) //todo: not finished
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
            case DMR:
                return new DecodeConfigDMR();
            case LTR:
                return new DecodeConfigLTRStandard();
            case LTR_NET:
                return new DecodeConfigLTRNet();
            case MPT1327:
                return new DecodeConfigMPT1327();
            case NBFM:
                return new DecodeConfigNBFM();
            case PASSPORT:
                return new DecodeConfigPassport();
            case P25_PHASE1:
                return new DecodeConfigP25Phase1();
            case P25_PHASE2:
                return new DecodeConfigP25Phase2();
            default:
                throw new IllegalArgumentException("DecodeConfigFactory - unknown decoder type [" + decoder.toString() + "]");
        }
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
                    DecodeConfigAM copyAM = new DecodeConfigAM();
                    DecodeConfigAM origAM = (DecodeConfigAM)config;
                    copyAM.setRecordAudio(origAM.getRecordAudio());
                    return copyAM;
                case DMR:
                    return new DecodeConfigDMR();
                case LTR_NET:
                    DecodeConfigLTRNet originalLTRNet = (DecodeConfigLTRNet)config;
                    DecodeConfigLTRNet copyLTRNet = new DecodeConfigLTRNet();
                    copyLTRNet.setMessageDirection(originalLTRNet.getMessageDirection());
                    return copyLTRNet;
                case LTR:
                    DecodeConfigLTRStandard originalLTRStandard = (DecodeConfigLTRStandard)config;
                    DecodeConfigLTRStandard copyLTRStandard = new DecodeConfigLTRStandard();
                    copyLTRStandard.setMessageDirection(originalLTRStandard.getMessageDirection());
                    return copyLTRStandard;
                case MPT1327:
                    DecodeConfigMPT1327 originalMPT = (DecodeConfigMPT1327)config;
                    DecodeConfigMPT1327 copyMPT = new DecodeConfigMPT1327();
                    copyMPT.setCallTimeoutSeconds(originalMPT.getCallTimeoutSeconds());
                    copyMPT.setChannelMapName(originalMPT.getChannelMapName());
                    copyMPT.setSync(originalMPT.getSync());
                    copyMPT.setTrafficChannelPoolSize(originalMPT.getTrafficChannelPoolSize());
                    return copyMPT;
                case NBFM:
                    DecodeConfigNBFM origNBFM = (DecodeConfigNBFM)config;
                    DecodeConfigNBFM copyNBFM = new DecodeConfigNBFM();
                    copyNBFM.setBandwidth(origNBFM.getBandwidth());
                    return copyNBFM;
                case P25_PHASE1:
                    DecodeConfigP25Phase1 originalP25 = (DecodeConfigP25Phase1)config;
                    DecodeConfigP25Phase1 copyP25 = new DecodeConfigP25Phase1();
                    copyP25.setIgnoreDataCalls(originalP25.getIgnoreDataCalls());
                    copyP25.setModulation(originalP25.getModulation());
                    copyP25.setTrafficChannelPoolSize(originalP25.getTrafficChannelPoolSize());
                    return copyP25;
                case P25_PHASE2:
                    DecodeConfigP25Phase2 originalP25P2 = (DecodeConfigP25Phase2)config;
                    DecodeConfigP25Phase2 copyP25P2 = new DecodeConfigP25Phase2();

                    if(originalP25P2.getScrambleParameters() != null)
                    {
                        copyP25P2.setScrambleParameters(originalP25P2.getScrambleParameters().copy());
                    }
                    return copyP25P2;
                case PASSPORT:
                    return new DecodeConfigPassport();
                default:
                    throw new IllegalArgumentException("Unrecognized decoder configuration type:" + config.getDecoderType());
            }
        }

        return null;
    }
}