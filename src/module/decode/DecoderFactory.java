/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package module.decode;

import alias.AliasList;
import alias.AliasModel;
import audio.AudioModule;
import controller.channel.Channel;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;
import controller.channel.map.ChannelMap;
import controller.channel.map.ChannelMapModel;
import filter.AllPassFilter;
import filter.FilterSet;
import filter.IFilter;
import gui.editor.EmptyValidatingEditor;
import gui.editor.ValidatingEditor;
import icon.IconManager;
import message.Message;
import message.MessageDirection;
import module.Module;
import module.decode.am.AMDecoder;
import module.decode.am.AMDecoderEditor;
import module.decode.am.AMDecoderPanel;
import module.decode.am.DecodeConfigAM;
import module.decode.config.AuxDecodeConfiguration;
import module.decode.config.DecodeConfiguration;
import module.decode.fleetsync2.Fleetsync2Decoder;
import module.decode.fleetsync2.Fleetsync2DecoderPanel;
import module.decode.fleetsync2.Fleetsync2DecoderState;
import module.decode.fleetsync2.FleetsyncMessageFilter;
import module.decode.lj1200.LJ1200Decoder;
import module.decode.lj1200.LJ1200DecoderPanel;
import module.decode.lj1200.LJ1200DecoderState;
import module.decode.lj1200.LJ1200MessageFilter;
import module.decode.ltrnet.DecodeConfigLTRNet;
import module.decode.ltrnet.LTRNetDecoder;
import module.decode.ltrnet.LTRNetDecoderEditor;
import module.decode.ltrnet.LTRNetDecoderPanel;
import module.decode.ltrnet.LTRNetDecoderState;
import module.decode.ltrnet.LTRNetMessageFilter;
import module.decode.ltrstandard.DecodeConfigLTRStandard;
import module.decode.ltrstandard.LTRStandardDecoder;
import module.decode.ltrstandard.LTRStandardDecoderEditor;
import module.decode.ltrstandard.LTRStandardDecoderPanel;
import module.decode.ltrstandard.LTRStandardDecoderState;
import module.decode.ltrstandard.LTRStandardMessageFilter;
import module.decode.mdc1200.MDCDecoder;
import module.decode.mdc1200.MDCDecoderPanel;
import module.decode.mdc1200.MDCDecoderState;
import module.decode.mdc1200.MDCMessageFilter;
import module.decode.mpt1327.DecodeConfigMPT1327;
import module.decode.mpt1327.MPT1327Decoder;
import module.decode.mpt1327.MPT1327Decoder.Sync;
import module.decode.mpt1327.MPT1327DecoderEditor;
import module.decode.mpt1327.MPT1327DecoderPanel;
import module.decode.mpt1327.MPT1327DecoderState;
import module.decode.mpt1327.MPT1327MessageFilter;
import module.decode.nbfm.DecodeConfigNBFM;
import module.decode.nbfm.NBFMDecoder;
import module.decode.nbfm.NBFMDecoderEditor;
import module.decode.nbfm.NBFMDecoderPanel;
import module.decode.p25.DecodeConfigP25Phase1;
import module.decode.p25.P25Decoder.Modulation;
import module.decode.p25.P25DecoderEditor;
import module.decode.p25.P25DecoderPanel;
import module.decode.p25.P25DecoderState;
import module.decode.p25.P25_C4FMDecoder;
import module.decode.p25.P25_LSMDecoder;
import module.decode.p25.audio.P25AudioModule;
import module.decode.p25.message.filter.P25MessageFilterSet;
import module.decode.passport.DecodeConfigPassport;
import module.decode.passport.PassportDecoder;
import module.decode.passport.PassportDecoderEditor;
import module.decode.passport.PassportDecoderPanel;
import module.decode.passport.PassportDecoderState;
import module.decode.passport.PassportMessageFilter;
import module.decode.state.AlwaysUnsquelchedDecoderState;
import module.decode.state.DecoderPanel;
import module.decode.state.DecoderState;
import module.decode.state.TrafficChannelManager;
import module.decode.tait.Tait1200Decoder;
import module.decode.tait.Tait1200DecoderPanel;
import module.decode.tait.Tait1200DecoderState;
import module.demodulate.am.AMDemodulatorModule;
import module.demodulate.audio.DemodulatedAudioFilterModule;
import module.demodulate.fm.FMDemodulatorModule;
import properties.SystemProperties;
import record.RecorderType;
import record.config.RecordConfiguration;
import settings.SettingsManager;
import util.TimeStamp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DecoderFactory
{
    public static final boolean NO_REMOVE_DC = false;
    public static final boolean REMOVE_DC = true;

    public static final int NO_FREQUENCY_CORRECTION = 0;

    /**
     * Returns a list of one primary decoder and any auxiliary decoders, as
     * specified in the configurations.
     *
     * @return list of configured decoders
     */
    public static List<Module> getModules(ChannelModel channelModel,
                                          ChannelMapModel channelMapModel,
                                          ChannelProcessingManager channelProcessingManager,
                                          AliasModel aliasModel,
                                          Channel channel)
    {

		/* Get the optional alias list for the decode modules to use */
        AliasList aliasList = aliasModel.getAliasList(channel.getAliasListName());

        List<Module> modules = getPrimaryModules(channelModel, channelMapModel,
            channelProcessingManager, aliasList, channel);

        modules.addAll(getAuxiliaryDecoders(channel.getAuxDecodeConfiguration(), aliasList));

        return modules;
    }

    /**
     * Constructs a primary decoder as specified in the decode configuration
     */
    public static List<Module> getPrimaryModules(ChannelModel channelModel,
                                                 ChannelMapModel channelMapModel,
                                                 ChannelProcessingManager channelProcessingManager,
                                                 AliasList aliasList,
                                                 Channel channel)
    {
        List<Module> modules = new ArrayList<Module>();

        ChannelType channelType = channel.getChannelType();

		/* Baseband low-pass filter pass and stop frequencies */
        DecodeConfiguration decodeConfig = channel.getDecodeConfiguration();

        int iqPass = decodeConfig.getDecoderType().getChannelBandwidth() / 2;
        int iqStop = iqPass + 1250;

        RecordConfiguration recordConfig = channel.getRecordConfiguration();

        boolean recordAudio = recordConfig == null ? false :
            recordConfig.getRecorders().contains(RecorderType.AUDIO);

        switch(decodeConfig.getDecoderType())
        {
            case AM:
                modules.add(new AMDecoder(decodeConfig));
                modules.add(new AlwaysUnsquelchedDecoderState(DecoderType.AM, channel.getName()));
                modules.add(new AMDemodulatorModule());
                modules.add(new DemodulatedAudioFilterModule(4000, 6000));
                modules.add(new AudioModule(recordAudio));
                break;
            case NBFM:
                modules.add(new NBFMDecoder(decodeConfig));
                modules.add(new AlwaysUnsquelchedDecoderState(DecoderType.NBFM, channel.getName()));
                modules.add(new FMDemodulatorModule(iqPass, iqStop));
                modules.add(new DemodulatedAudioFilterModule(4000, 6000));
                modules.add(new AudioModule(recordAudio));
                break;
            case LTR_STANDARD:
                MessageDirection direction = ((DecodeConfigLTRStandard) decodeConfig).getMessageDirection();
                modules.add(new LTRStandardDecoder(aliasList, direction));
                modules.add(new LTRStandardDecoderState(aliasList));
                modules.add(new FMDemodulatorModule(iqPass, iqStop));
                modules.add(new DemodulatedAudioFilterModule(4000, 6000));
                modules.add(new AudioModule(recordAudio));
                break;
            case LTR_NET:
                modules.add(new LTRNetDecoder((DecodeConfigLTRNet) decodeConfig, aliasList));
                modules.add(new LTRNetDecoderState(aliasList));
                modules.add(new FMDemodulatorModule(iqPass, iqStop));
                modules.add(new DemodulatedAudioFilterModule(4000, 6000));
                modules.add(new AudioModule(recordAudio));
                break;
            case MPT1327:
                DecodeConfigMPT1327 mptConfig = (DecodeConfigMPT1327) decodeConfig;

                ChannelMap channelMap = channelMapModel
                    .getChannelMap(mptConfig.getChannelMapName());

                Sync sync = mptConfig.getSync();

                modules.add(new MPT1327Decoder(aliasList, sync));


                modules.add(new MPT1327DecoderState(aliasList, channelMap,
                    channelType, mptConfig.getCallTimeout() * 1000));

                if(channelType == ChannelType.STANDARD)
                {
                    long timeout = mptConfig.getCallTimeout() * 1000; //convert to milliseconds

                    modules.add(new TrafficChannelManager(channelModel, channelProcessingManager,
                        decodeConfig, recordConfig, channel.getSystem(),
                        channel.getSite(), (aliasList != null ? aliasList.getName() : null), timeout,
                        mptConfig.getTrafficChannelPoolSize()));
                }

                modules.add(new FMDemodulatorModule(iqPass, iqStop));
                modules.add(new DemodulatedAudioFilterModule(4000, 6000));
                modules.add(new AudioModule(recordAudio));
                break;
            case PASSPORT:
                modules.add(new PassportDecoder(decodeConfig, aliasList));
                modules.add(new PassportDecoderState(aliasList));
                modules.add(new FMDemodulatorModule(iqPass, iqStop));
                modules.add(new DemodulatedAudioFilterModule(4000, 6000));
                modules.add(new AudioModule(recordAudio));
                break;
            case P25_PHASE1:
                DecodeConfigP25Phase1 p25Config = (DecodeConfigP25Phase1) decodeConfig;

                Modulation modulation = p25Config.getModulation();


                switch(modulation)
                {
                    case C4FM:
                        modules.add(new FMDemodulatorModule(6750, 7500));
                        modules.add(new P25_C4FMDecoder(aliasList, decodeConfig.getAFCMaximumCorrection()));
                        modules.add(new P25DecoderState(aliasList, channelType,
                            Modulation.C4FM, p25Config.getIgnoreDataCalls()));
                        break;
                    case CQPSK:
                        modules.add(new P25_LSMDecoder(aliasList));
                        modules.add(new P25DecoderState(aliasList, channelType,
                            Modulation.CQPSK, p25Config.getIgnoreDataCalls()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized P25 "
                            + "Phase 1 Modulation [" + modulation + "]");
                }

                if(channelType == ChannelType.STANDARD)
                {
                    //Set call timeout to 3 seconds for P25 traffic channels
                    long timeout = 3000;

                    modules.add(new TrafficChannelManager(channelModel,
                        channelProcessingManager, decodeConfig,
                        recordConfig, channel.getSystem(), channel.getSite(),
                        (aliasList != null ? aliasList.getName() : null), timeout,
                        p25Config.getTrafficChannelPoolSize()));
                }

                modules.add(new P25AudioModule(recordAudio));
                break;
            default:
                throw new IllegalArgumentException(
                    "Unknown decoder type [" +
                        decodeConfig.getDecoderType().toString() + "]");
        }

        return modules;
    }

    /**
     * Constructs a list of auxiliary decoders, as specified in the configuration
     *
     * @param config - auxiliary configuration
     * @param aliasList - optional alias list
     * @return - list of auxiliary decoders
     */
    public static List<Module> getAuxiliaryDecoders(AuxDecodeConfiguration config,
                                                    AliasList aliasList)
    {
        List<Module> modules = new ArrayList<>();

        if(config != null)
        {
            for(DecoderType auxDecoder : config.getAuxDecoders())
            {
                switch(auxDecoder)
                {
                    case FLEETSYNC2:
                        modules.add(new Fleetsync2Decoder(aliasList));
                        modules.add(new Fleetsync2DecoderState(aliasList));
                        break;
                    case MDC1200:
                        modules.add(new MDCDecoder(aliasList));
                        modules.add(new MDCDecoderState(aliasList));
                        break;
                    case LJ_1200:
                        modules.add(new LJ1200Decoder(aliasList));
                        modules.add(new LJ1200DecoderState(aliasList));
                        break;
                    case TAIT_1200:
                        modules.add(new Tait1200Decoder(aliasList));
                        modules.add(new Tait1200DecoderState(aliasList));
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized auxiliary "
                            + "decoder type [" + auxDecoder + "]");
                }
            }
        }

        return modules;
    }

    public static DecoderPanel getDecoderPanel(IconManager iconManager, SettingsManager settingsManager,
                                               DecoderState decoderState)
    {
        switch(decoderState.getDecoderType())
        {
            case AM:
                return new AMDecoderPanel(iconManager, settingsManager, decoderState);
            case FLEETSYNC2:
                return new Fleetsync2DecoderPanel(iconManager, settingsManager, (Fleetsync2DecoderState) decoderState);
            case LJ_1200:
                return new LJ1200DecoderPanel(iconManager, settingsManager, (LJ1200DecoderState) decoderState);
            case LTR_NET:
                return new LTRNetDecoderPanel(iconManager, settingsManager, (LTRNetDecoderState) decoderState);
            case LTR_STANDARD:
                return new LTRStandardDecoderPanel(iconManager, settingsManager, (LTRStandardDecoderState) decoderState);
            case MDC1200:
                return new MDCDecoderPanel(iconManager, settingsManager, (MDCDecoderState) decoderState);
            case MPT1327:
                return new MPT1327DecoderPanel(iconManager, settingsManager, (MPT1327DecoderState) decoderState);
            case NBFM:
                return new NBFMDecoderPanel(iconManager, settingsManager, decoderState);
            case P25_PHASE1:
                return new P25DecoderPanel(iconManager, settingsManager, (P25DecoderState) decoderState);
            case PASSPORT:
                return new PassportDecoderPanel(iconManager, settingsManager, (PassportDecoderState) decoderState);
            case TAIT_1200:
                return new Tait1200DecoderPanel(iconManager, settingsManager, (Tait1200DecoderState) decoderState);
            default:
                throw new IllegalArgumentException("Unrecognized decoder: " +
                    decoderState.getDecoderType().getDisplayString());
        }
    }

    /**
     * Assembles a filter set containing filters for the primary channel
     * decoder and each of the auxiliary decoders
     */
    public static FilterSet<Message> getMessageFilters(List<Module> modules)
    {
        FilterSet<Message> filterSet = new FilterSet<>();

        for(Module module : modules)
        {
            if(module instanceof Decoder)
            {
                filterSet.addFilters(getMessageFilter(
                    ((Decoder) module).getDecoderType()));
            }
        }

		/* If we don't have any filters, add an ALL-PASS filter */
        if(filterSet.getFilters().isEmpty())
        {
            filterSet.addFilter(new AllPassFilter<Message>());
        }

        return filterSet;
    }

    /**
     * Returns a set of IMessageFilter objects (FilterSets or Filters) that
     * can process all of the messages produced by the specified decoder type.
     */
    public static List<IFilter<Message>> getMessageFilter(DecoderType decoder)
    {
        ArrayList<IFilter<Message>> filters = new ArrayList<IFilter<Message>>();

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
            case P25_PHASE2:
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
                throw new IllegalArgumentException("DecodeConfigFactory - unknown decoder type ["
                    + decoder.toString() + "]");
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
                    DecodeConfigAM originalAM = (DecodeConfigAM) config;
                    DecodeConfigAM copyAM = new DecodeConfigAM();
                    copyAM.setAFC(originalAM.getAFC());
                    copyAM.setAFCMaximumCorrection(originalAM.getAFCMaximumCorrection());
                    return copyAM;
                case LTR_NET:
                    DecodeConfigLTRNet originalLTRNet = (DecodeConfigLTRNet) config;
                    DecodeConfigLTRNet copyLTRNet = new DecodeConfigLTRNet();
                    copyLTRNet.setAFC(originalLTRNet.getAFC());
                    copyLTRNet.setAFCMaximumCorrection(originalLTRNet.getAFCMaximumCorrection());
                    copyLTRNet.setMessageDirection(originalLTRNet.getMessageDirection());
                    return copyLTRNet;
                case LTR_STANDARD:
                    DecodeConfigLTRStandard originalLTRStandard = (DecodeConfigLTRStandard) config;
                    DecodeConfigLTRStandard copyLTRStandard = new DecodeConfigLTRStandard();
                    copyLTRStandard.setAFC(originalLTRStandard.getAFC());
                    copyLTRStandard.setAFCMaximumCorrection(originalLTRStandard.getAFCMaximumCorrection());
                    copyLTRStandard.setMessageDirection(originalLTRStandard.getMessageDirection());
                    return copyLTRStandard;
                case MPT1327:
                    DecodeConfigMPT1327 originalMPT = (DecodeConfigMPT1327) config;
                    DecodeConfigMPT1327 copyMPT = new DecodeConfigMPT1327();
                    copyMPT.setAFC(originalMPT.getAFC());
                    copyMPT.setAFCMaximumCorrection(originalMPT.getAFCMaximumCorrection());
                    copyMPT.setCallTimeout(originalMPT.getCallTimeout());
                    copyMPT.setChannelMapName(originalMPT.getChannelMapName());
                    copyMPT.setSync(originalMPT.getSync());
                    copyMPT.setTrafficChannelPoolSize(originalMPT.getTrafficChannelPoolSize());
                    return copyMPT;
                case NBFM:
                    DecodeConfigNBFM originalNBFM = (DecodeConfigNBFM) config;
                    DecodeConfigNBFM copyNBFM = new DecodeConfigNBFM();
                    copyNBFM.setAFC(originalNBFM.getAFC());
                    copyNBFM.setAFCMaximumCorrection(originalNBFM.getAFCMaximumCorrection());
                    return copyNBFM;
                case P25_PHASE1:
                    DecodeConfigP25Phase1 originalP25 = (DecodeConfigP25Phase1) config;
                    DecodeConfigP25Phase1 copyP25 = new DecodeConfigP25Phase1();
                    copyP25.setAFC(originalP25.getAFC());
                    copyP25.setAFCMaximumCorrection(originalP25.getAFCMaximumCorrection());
                    copyP25.setIgnoreDataCalls(originalP25.getIgnoreDataCalls());
                    copyP25.setModulation(originalP25.getModulation());
                    copyP25.setTrafficChannelPoolSize(originalP25.getTrafficChannelPoolSize());
                    return copyP25;
                case PASSPORT:
                    DecodeConfigPassport originalPass = (DecodeConfigPassport) config;
                    DecodeConfigPassport copyPass = new DecodeConfigPassport();
                    copyPass.setAFC(originalPass.getAFC());
                    copyPass.setAFCMaximumCorrection(originalPass.getAFCMaximumCorrection());
                    return copyPass;
                default:
                    throw new IllegalArgumentException("Unrecognized decoder "
                        + "configuration type:" + config.getDecoderType());
            }
        }

        return null;
    }

    public static String getRecordingFilename(String token)
    {
        //Get the base recording filename
        StringBuilder sb = new StringBuilder();
        sb.append(SystemProperties.getInstance()
            .getApplicationFolder("recordings"));
        sb.append(File.separator);
        sb.append(TimeStamp.getTimeStamp("_"));
        sb.append("_");
        sb.append(token);

        return sb.toString();
    }
}
