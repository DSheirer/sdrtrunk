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
package io.github.dsheirer.module.decode;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.action.AliasActionManager;
import io.github.dsheirer.audio.AbstractAudioModule;
import io.github.dsheirer.audio.AudioModule;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.filter.AllPassFilter;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.filter.IFilter;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.am.AMDecoder;
import io.github.dsheirer.module.decode.am.AMDecoderState;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dcs.DCSDecoder;
import io.github.dsheirer.module.decode.dcs.DCSDecoderState;
import io.github.dsheirer.module.decode.dcs.DCSMessageFilter;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.module.decode.dmr.DMRDecoderState;
import io.github.dsheirer.module.decode.dmr.DMRTrafficChannelManager;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.dmr.audio.DMRAudioModule;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.channel.DMRLsn;
import io.github.dsheirer.module.decode.dmr.channel.DmrRestLsn;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.filter.DmrMessageFilterSet;
import io.github.dsheirer.module.decode.event.DecodeEvent;
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
import io.github.dsheirer.module.decode.p25.phase1.message.filter.P25P1MessageFilterSet;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.P25P2DecoderHDQPSK;
import io.github.dsheirer.module.decode.p25.phase2.P25P2DecoderState;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import io.github.dsheirer.module.decode.p25.phase2.message.filter.P25P2MessageFilterSet;
import io.github.dsheirer.module.decode.passport.DecodeConfigPassport;
import io.github.dsheirer.module.decode.passport.PassportDecoder;
import io.github.dsheirer.module.decode.passport.PassportDecoderState;
import io.github.dsheirer.module.decode.passport.PassportMessageFilter;
import io.github.dsheirer.module.decode.tait.Tait1200Decoder;
import io.github.dsheirer.module.decode.tait.Tait1200DecoderState;
import io.github.dsheirer.module.decode.tait.Tait1200MessageFilter;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;
import io.github.dsheirer.module.demodulate.fm.FMDemodulatorModule;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.channel.rotation.ChannelRotationMonitor;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating decoder modules to use in a processing chain for any supported protocols.
 */
public class DecoderFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DecoderFactory.class);
    private static final double FM_CHANNEL_BANDWIDTH = 12500.0;
    private static final boolean AUDIO_FILTER_ENABLE = true;

    /**
     * Returns a list of one primary decoder and any auxiliary decoders, as
     * specified in the configurations.
     *
     * @return list of configured decoders
     */
    public static List<Module> getModules(ChannelMapModel channelMapModel, Channel channel, AliasModel aliasModel,
                                          UserPreferences userPreferences, TrafficChannelManager trafficChannelManager,
                                          IChannelDescriptor channelDescriptor)
    {
        List<Module> modules = getPrimaryModules(channelMapModel, channel, aliasModel, userPreferences,
                trafficChannelManager, channelDescriptor);
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
     * @param channelDescriptor to preload into the decoder state as the current channel.
     * @return list of modules to use for a processing chain
     */
    public static List<Module> getPrimaryModules(ChannelMapModel channelMapModel, Channel channel, AliasModel aliasModel,
                                                 UserPreferences userPreferences, TrafficChannelManager trafficChannelManager,
                                                 IChannelDescriptor channelDescriptor)
    {
        List<Module> modules = new ArrayList<>();

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
                    trafficChannelManager, channelDescriptor);
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
                processMPT1327(channelMapModel, channel, modules, aliasList, channelType,
                        (DecodeConfigMPT1327) decodeConfig, userPreferences);
                break;
            case PASSPORT:
                processPassport(channel, modules, aliasList, decodeConfig);
                break;
            case P25_PHASE1:
                processP25Phase1(channel, userPreferences, modules, aliasList, trafficChannelManager, channelDescriptor);
                break;
            case P25_PHASE2:
                processP25Phase2(channel, userPreferences, modules, aliasList, trafficChannelManager, channelDescriptor);
                break;
            default:
                throw new IllegalArgumentException("Unknown decoder type [" + decodeConfig.getDecoderType().toString() + "]");
        }

        return modules;
    }

    /**
     * Creates decoder modules for APCO-25 Phase 2 decoder
     * @param channel configuration
     * @param userPreferences reference
     * @param modules collection to add to
     * @param aliasList for the channel
     * @param trafficChannelManager from parent, if this is for a traffic channel, otherwise this can be null and it
     * will be created automatically.
     */
    private static void processP25Phase2(Channel channel, UserPreferences userPreferences, List<Module> modules,
                                         AliasList aliasList, TrafficChannelManager trafficChannelManager,
                                         IChannelDescriptor channelDescriptor)
    {

        modules.add(new P25P2DecoderHDQPSK((DecodeConfigP25Phase2)channel.getDecodeConfiguration()));

        P25TrafficChannelManager p25TrafficChannelManager = null;

        if(channel.getChannelType() == ChannelType.STANDARD)
        {
            p25TrafficChannelManager = new P25TrafficChannelManager(channel);
        }
        else if(trafficChannelManager instanceof P25TrafficChannelManager p25)
        {
            p25TrafficChannelManager = p25;
        }
        else
        {
            p25TrafficChannelManager = new P25TrafficChannelManager(channel);
        }

        //Only add traffic channel manager to the modules if this is the control channel
        if(channel.isStandardChannel())
        {
            modules.add(p25TrafficChannelManager);
        }

        //A single patch group manager is shared across both timeslots
        PatchGroupManager patchGroupManager = new PatchGroupManager();

        P25P2DecoderState decoderState1 = new P25P2DecoderState(channel, P25P2Message.TIMESLOT_1, p25TrafficChannelManager, patchGroupManager);
        decoderState1.setCurrentChannel(channelDescriptor);
        P25P2DecoderState decoderState2 = new P25P2DecoderState(channel, P25P2Message.TIMESLOT_2, p25TrafficChannelManager, patchGroupManager);
        decoderState2.setCurrentChannel(channelDescriptor);
        modules.add(decoderState1);
        modules.add(decoderState2);
        modules.add(new P25P2AudioModule(userPreferences, P25P2Message.TIMESLOT_1, aliasList));
        modules.add(new P25P2AudioModule(userPreferences, P25P2Message.TIMESLOT_2, aliasList));

        //Add a channel rotation monitor when we have multiple control channel frequencies specified
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency sctmf &&
                sctmf.hasMultipleFrequencies())
        {
            List<State> activeStates = new ArrayList<>();
            activeStates.add(State.CONTROL);
            modules.add(new ChannelRotationMonitor(activeStates, sctmf.getFrequencyRotationDelay(), userPreferences));
        }
    }

    /**
     * Creates decoder modules for APCO-25 Phase 1 decoder
     * @param channel configuration
     * @param userPreferences reference
     * @param modules collection to add to
     * @param aliasList for the channel
     */
    private static void processP25Phase1(Channel channel, UserPreferences userPreferences, List<Module> modules,
                                         AliasList aliasList, TrafficChannelManager trafficChannelManager,
                                         IChannelDescriptor channelDescriptor)
    {
        if(channel.getDecodeConfiguration() instanceof DecodeConfigP25Phase1 p1)
        {
            switch(p1.getModulation())
            {
                case C4FM:
                    modules.add(new P25P1DecoderC4FM());
                    break;
                case CQPSK:
                    modules.add(new P25P1DecoderLSM());
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized P25 Phase 1 Modulation [" + p1.getModulation() + "]");
            }
        }

        if(channel.getChannelType() == ChannelType.STANDARD)
        {
            P25TrafficChannelManager primaryTCM = new P25TrafficChannelManager(channel);
            modules.add(primaryTCM);
            modules.add(new P25P1DecoderState(channel, primaryTCM));
        }
        else if(trafficChannelManager instanceof P25TrafficChannelManager parentTCM)
        {
            P25P1DecoderState decoderState = new P25P1DecoderState(channel, parentTCM);
            decoderState.setCurrentChannel(channelDescriptor);
            modules.add(decoderState);
        }
        else
        {
            mLog.warn("Expected non-null traffic channel manager for channel " + channel.getName());
        }

        modules.add(new P25P1AudioModule(userPreferences, aliasList));

        //Add a channel rotation monitor when we have multiple control channel frequencies specified
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency sctmf &&
            sctmf.hasMultipleFrequencies())
        {
            List<State> activeStates = new ArrayList<>();
            activeStates.add(State.CONTROL);
            modules.add(new ChannelRotationMonitor(activeStates, sctmf.getFrequencyRotationDelay(), userPreferences));
        }
    }

    /**
     * Creates decoder modules for Passport decoder
     * @param channel configuration
     * @param modules collection to add to
     * @param aliasList for the channel
     * @param decodeConfig for the channel
     */
    private static void processPassport(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfiguration decodeConfig) {
        modules.add(new PassportDecoder(decodeConfig));
        modules.add(new PassportDecoderState());
        modules.add(new AudioModule(aliasList, AUDIO_FILTER_ENABLE));
        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH));
        }
    }

    /**
     * Creates decoder modules for MPT-1327 decoder
     * @param channelMapModel to use in calculating traffic channel frequencies
     * @param channel configuration
     * @param modules collection to add to
     * @param aliasList for the channel
     * @param channelType for control or traffic
     * @param decodeConfig configuration
     */
    private static void processMPT1327(ChannelMapModel channelMapModel, Channel channel, List<Module> modules,
                                       AliasList aliasList, ChannelType channelType, DecodeConfigMPT1327 decodeConfig,
                                       UserPreferences userPreferences)
    {
        DecodeConfigMPT1327 mptConfig = decodeConfig;
        ChannelMap channelMap = channelMapModel.getChannelMap(mptConfig.getChannelMapName());
        Sync sync = mptConfig.getSync();
        modules.add(new MPT1327Decoder(sync));

        final int callTimeoutMilliseconds = mptConfig.getCallTimeoutSeconds() * 1000;

        // Set max segment audio sample length slightly above call timeout to
        // not create a new segment if the processing chain finishes a bit after
        // actual call timeout.
        long maxAudioSegmentLengthMillis = (callTimeoutMilliseconds + 5000);
        modules.add(new AudioModule(aliasList, AbstractAudioModule.DEFAULT_TIMESLOT, maxAudioSegmentLengthMillis, AUDIO_FILTER_ENABLE));

        SourceType sourceType = channel.getSourceConfiguration().getSourceType();
        if(sourceType == SourceType.TUNER || sourceType == SourceType.TUNER_MULTIPLE_FREQUENCIES)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH));
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
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency sctmf &&
            sctmf.hasMultipleFrequencies())
        {
            List<State> activeStates = new ArrayList<>();
            activeStates.add(State.CONTROL);
            modules.add(new ChannelRotationMonitor(activeStates, sctmf.getFrequencyRotationDelay(), userPreferences));
        }
    }

    /**
     * Creates decoder modules for LTR-Net decoder
     * @param channel configuration
     * @param modules collection to add to
     * @param aliasList for the channel
     * @param decodeConfig for the channel
     */
    private static void processLTRNet(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfigLTRNet decodeConfig) {
        modules.add(new LTRNetDecoder(decodeConfig));
        modules.add(new LTRNetDecoderState());
        modules.add(new AudioModule(aliasList, AUDIO_FILTER_ENABLE));
        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH));
        }
    }

    /**
     * Creates decoder modules for LTR decoder
     * @param channel configuration
     * @param modules collection to add to
     * @param aliasList for the channel
     * @param decodeConfig for the channel
     */
    private static void processLTRStandard(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfigLTRStandard decodeConfig) {
        MessageDirection direction = decodeConfig.getMessageDirection();
        modules.add(new LTRStandardDecoder(direction));
        modules.add(new LTRStandardDecoderState());
        modules.add(new AudioModule(aliasList, AUDIO_FILTER_ENABLE));
        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            modules.add(new FMDemodulatorModule(FM_CHANNEL_BANDWIDTH));
        }
    }

    /**
     * Creates decoder modules for Narrow Band FM decoder
     * @param channel configuration
     * @param modules collection to add to
     * @param aliasList for the channel
     * @param decodeConfig for the channel
     */
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
        modules.add(new AudioModule(aliasList, 0, 60000, decodeConfigNBFM.isAudioFilter()));
    }

    /**
     * Creates decoder modules for AM decoder
     * @param channel configuration
     * @param modules collection to add to
     * @param aliasList for the channel
     * @param decodeConfig for the channel
     */
    private static void processAM(Channel channel, List<Module> modules, AliasList aliasList, DecodeConfiguration decodeConfig)
    {
        if(decodeConfig instanceof DecodeConfigAM configAM)
        {
            modules.add(new AMDecoder(configAM));
            modules.add(new AMDecoderState(channel.getName(), configAM));
            modules.add(new AudioModule(aliasList, 0, 60000, AUDIO_FILTER_ENABLE));
        }
        else
        {
            throw new IllegalArgumentException("Can't create AM decoder - unrecognized decode config type: " +
                    (decodeConfig != null ? decodeConfig.getClass() : "null/empty"));
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
     * @param channelDescriptor for the channel.
     */
    private static void processDMR(Channel channel, UserPreferences userPreferences, List<Module> modules,
                                   AliasList aliasList, DecodeConfigDMR decodeConfig,
                                   TrafficChannelManager trafficChannelManager, IChannelDescriptor channelDescriptor)
    {
        modules.add(new DMRDecoder(decodeConfig, channel.isTrafficChannel()));

        DMRTrafficChannelManager dmrTrafficChannelManager = null;

        if(trafficChannelManager instanceof DMRTrafficChannelManager)
        {
            dmrTrafficChannelManager = (DMRTrafficChannelManager)trafficChannelManager;
        }
        else
        {
            dmrTrafficChannelManager = new DMRTrafficChannelManager(channel);
        }

        //Only register the traffic channel manager as a module if this is the parent control channel.
        if(channel.isStandardChannel())
        {
            modules.add(dmrTrafficChannelManager);
        }

        DMRDecoderState state1 = new DMRDecoderState(channel, DMRMessage.TIMESLOT_1, dmrTrafficChannelManager);
        DMRDecoderState state2 = new DMRDecoderState(channel, DMRMessage.TIMESLOT_2, dmrTrafficChannelManager);

        //Register the states with each other so that they can pass Cap+ site status messaging to resolve current channel
        state1.setSisterDecoderState(state2);
        state2.setSisterDecoderState(state1);

        //If an LSN is provided, apply it to both of the decoder states.
        if(channelDescriptor instanceof DMRLsn lsn)
        {
            //If this is a REST descriptor, change it to a standard LSN descriptor.
            if(channelDescriptor instanceof DmrRestLsn rest)
            {
                lsn = new DMRLsn(rest.getLsn());
                lsn.setTimeslotFrequency(rest.getTimeslotFrequency());
            }

            if(lsn.getTimeslot() == DMRMessage.TIMESLOT_1)
            {
                state1.setCurrentChannel(lsn);
                state2.setCurrentChannel(lsn.getSisterTimeslot());
            }
            else
            {
                state1.setCurrentChannel(lsn.getSisterTimeslot());
                state2.setCurrentChannel(lsn);
            }
        }

        if(decodeConfig.hasChannelGrantEvent())
        {
            DecodeEvent event = decodeConfig.getChannelGrantEvent();

            if(decodeConfig.getChannelGrantEvent().getTimeslot() == DMRMessage.TIMESLOT_1)
            {
                state1.setCurrentCallEvent(event);

                if(event.getChannelDescriptor() instanceof DMRChannel dmrChannel)
                {
                    state2.setCurrentChannel(dmrChannel.getSisterTimeslot());
                }
            }
            else
            {
                state2.setCurrentCallEvent(event);

                if(event.getChannelDescriptor() instanceof DMRChannel dmrChannel)
                {
                    state1.setCurrentChannel(dmrChannel.getSisterTimeslot());
                }
            }
        }

        modules.add(state1);
        modules.add(state2);
        modules.add(new DMRAudioModule(userPreferences, aliasList, DMRMessage.TIMESLOT_1));
        modules.add(new DMRAudioModule(userPreferences, aliasList, DMRMessage.TIMESLOT_2));

        //Add a channel rotation monitor when we have multiple control channel frequencies specified
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency sctmf &&
            sctmf.hasMultipleFrequencies())
        {
            List<State> activeStates = new ArrayList<>();
            activeStates.add(State.CONTROL);
            modules.add(new ChannelRotationMonitor(activeStates, sctmf.getFrequencyRotationDelay(), userPreferences));
        }
    }

    /**
     * Constructs a list of auxiliary decoders, as specified in the channel configuration
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
                    case DCS:
                        modules.add(new DCSDecoder());
                        modules.add(new DCSDecoderState());
                        break;
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
        FilterSet<IMessage> filterSet = new FilterSet<>("Message Filters");

        for(Module module : modules)
        {
            if(module instanceof Decoder)
            {
                filterSet.addFilters(getMessageFilter(((Decoder)module).getDecoderType()));
            }
        }

        //Add an all-others filter as a catch-all for anything that isn't handled by the decoder filters.
        filterSet.addFilter(new AllPassFilter<>("All Other Messages Filter"));

        return filterSet;
    }

    /**
     * Returns a set of IMessageFilter objects (FilterSets or Filters) that
     * can process all messages produced by the specified decoder type.
     */
    public static List<IFilter<IMessage>> getMessageFilter(DecoderType decoder)
    {
        List<IFilter<IMessage>> filters = new ArrayList<>();

        switch(decoder)
        {
            case DCS:
                filters.add(new DCSMessageFilter());
                break;
            case DMR:
                filters.add(new DmrMessageFilterSet());
                break;
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
                filters.add(new P25P1MessageFilterSet());
                break;
            case P25_PHASE2:
                filters.add(new P25P2MessageFilterSet());
                break;
            case PASSPORT:
                filters.add(new PassportMessageFilter());
                break;
            case TAIT_1200:
                filters.add(new Tait1200MessageFilter());
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
                throw new IllegalArgumentException("DecodeConfigFactory - unknown decoder type [" + decoder + "]");
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
                    copyAM.setBandwidth(origAM.getBandwidth());
                    copyAM.setTalkgroup(origAM.getTalkgroup());
                    copyAM.setSquelchThreshold(origAM.getSquelchThreshold());
                    copyAM.setSquelchAutoTrack(origAM.isSquelchAutoTrack());
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
                    copyNBFM.setAudioFilter(origNBFM.isAudioFilter());
                    copyNBFM.setBandwidth(origNBFM.getBandwidth());
                    copyNBFM.setSquelchHysteresisCloseThreshold(origNBFM.getSquelchHysteresisCloseThreshold());
                    copyNBFM.setSquelchHysteresisOpenThreshold(origNBFM.getSquelchHysteresisOpenThreshold());
                    copyNBFM.setSquelchNoiseOpenThreshold(origNBFM.getSquelchNoiseOpenThreshold());
                    copyNBFM.setSquelchNoiseCloseThreshold(origNBFM.getSquelchNoiseCloseThreshold());
                    copyNBFM.setTalkgroup(origNBFM.getTalkgroup());
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