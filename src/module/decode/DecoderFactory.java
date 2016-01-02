/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package module.decode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import message.Message;
import message.MessageDirection;
import module.Module;
import module.decode.am.AMDecoder;
import module.decode.am.AMDecoderPanel;
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
import module.decode.ltrnet.LTRNetDecoderPanel;
import module.decode.ltrnet.LTRNetDecoderState;
import module.decode.ltrnet.LTRNetEditor;
import module.decode.ltrnet.LTRNetMessageFilter;
import module.decode.ltrstandard.DecodeConfigLTRStandard;
import module.decode.ltrstandard.LTRStandardDecoder;
import module.decode.ltrstandard.LTRStandardDecoderPanel;
import module.decode.ltrstandard.LTRStandardDecoderState;
import module.decode.ltrstandard.LTRStandardEditor;
import module.decode.ltrstandard.LTRStandardMessageFilter;
import module.decode.mdc1200.MDCDecoder;
import module.decode.mdc1200.MDCDecoderPanel;
import module.decode.mdc1200.MDCDecoderState;
import module.decode.mdc1200.MDCMessageFilter;
import module.decode.mpt1327.DecodeConfigMPT1327;
import module.decode.mpt1327.MPT1327ConfigEditor;
import module.decode.mpt1327.MPT1327Decoder;
import module.decode.mpt1327.MPT1327Decoder.Sync;
import module.decode.mpt1327.MPT1327DecoderPanel;
import module.decode.mpt1327.MPT1327DecoderState;
import module.decode.mpt1327.MPT1327MessageFilter;
import module.decode.nbfm.NBFMDecoder;
import module.decode.nbfm.NBFMDecoderPanel;
import module.decode.nbfm.NBFMEditor;
import module.decode.p25.DecodeConfigP25Phase1;
import module.decode.p25.P25Decoder.Modulation;
import module.decode.p25.P25DecoderPanel;
import module.decode.p25.P25DecoderState;
import module.decode.p25.P25DecodeEditor;
import module.decode.p25.P25_C4FMDecoder;
import module.decode.p25.P25_LSMDecoder;
import module.decode.p25.audio.P25AudioModule;
import module.decode.p25.message.filter.P25MessageFilterSet;
import module.decode.passport.PassportDecoder;
import module.decode.passport.PassportDecoderPanel;
import module.decode.passport.PassportDecoderState;
import module.decode.passport.PassportEditor;
import module.decode.passport.PassportMessageFilter;
import module.decode.state.AlwaysUnsquelchedDecoderState;
import module.decode.state.DecoderPanel;
import module.decode.state.DecoderState;
import module.decode.state.TrafficChannelManager;
import module.decode.tait.Tait1200Decoder;
import module.decode.tait.Tait1200DecoderPanel;
import module.decode.tait.Tait1200DecoderState;
import module.demodulate.am.AMDemodulatorModule;
import module.demodulate.fm.FMDemodulatorModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.PlaylistManager;
import properties.SystemProperties;
import record.RecorderType;
import record.config.RecordConfiguration;
import settings.SettingsManager;
import util.TimeStamp;
import alias.AliasList;
import audio.AudioModule;
import controller.ResourceManager;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelNode;
import controller.channel.map.ChannelMap;
import controller.site.Site;
import controller.system.System;
import filter.AllPassFilter;
import filter.FilterSet;
import filter.IFilter;

public class DecoderFactory
{
	private final static Logger mLog = LoggerFactory.getLogger( DecoderFactory.class );
	
	public static final boolean NO_REMOVE_DC = false;
	public static final boolean REMOVE_DC = true;
	
	public static final int NO_FREQUENCY_CORRECTION = 0;
	
	/**
	 * Returns a list of one primary decoder and any auxiliary decoders, as
	 * specified in the configurations.
	 * 
	 * @param channelType - standard or traffic channel type
	 * @param resourceManager 
	 * @param decodeConfig - primary decoder configuration
	 * @param recordConfig - recording configuration used by traffic channel manager
	 * @param auxConfig - auxiliary decoder(s) configuration
	 * @param optional alias list - (null is ok)
	 * 
	 * @return list of configured decoders
	 */
	public static List<Module> getModules( ChannelType channelType,
										   ResourceManager resourceManager,
										   DecodeConfiguration decodeConfig,
										   RecordConfiguration recordConfig,
										   AuxDecodeConfiguration auxConfig, 
										   AliasList aliasList,
										   String system,
										   String site,
										   String channelName )
		{
		List<Module> modules = getPrimaryModules( channelType, decodeConfig, 
			recordConfig, aliasList, system, site, channelName, resourceManager );
		
		modules.addAll( getAuxiliaryDecoders( auxConfig, aliasList ) );
		
		return modules;
	}
	
	public static FMDemodulatorModule getFMDemodulator( 
		DecodeConfiguration config, int pass, int stop, boolean removeDC )
	{
		if( config.isAFCEnabled() )
		{
			return new FMDemodulatorModule( pass, stop, 
					config.getAFCMaximumCorrection(), removeDC );
		}
		else
		{
			return new FMDemodulatorModule( pass, stop, 
					NO_FREQUENCY_CORRECTION, removeDC );
		}
	}

	/**
	 * Constructs a primary decoder as specified in the decode configuration
	 * 
	 * @param channelType - traffic or standard decode channel
	 * @param decodeConfig - primary decoder configuration
	 * @param recordConfig - recording options
	 * @param aliasList - optional alias list
	 * @param resourceManager - shared resource manager
	 * @return configured decoder or null
	 */
	public static List<Module> getPrimaryModules( ChannelType channelType,
												  DecodeConfiguration decodeConfig,
												  RecordConfiguration recordConfig,
												  AliasList aliasList,
												  String system,
												  String site,
												  String channelName,
												  ResourceManager resourceManager )
	{
		List<Module> modules = new ArrayList<Module>();

		/* Baseband low-pass filter pass and stop frequencies */
		int pass = decodeConfig.getDecoderType().getChannelBandwidth() / 2;
		int stop = pass + 1250;
		
		boolean recordAudio = recordConfig == null ? false : 
			recordConfig.getRecorders().contains( RecorderType.AUDIO );
		
		switch( decodeConfig.getDecoderType() )
		{
		    case AM:
		    	modules.add( new AMDemodulatorModule() );
		    	modules.add( new AMDecoder( decodeConfig ) );
		    	modules.add( new AlwaysUnsquelchedDecoderState( DecoderType.AM, channelName ) );
				modules.add( new AudioModule( recordAudio, REMOVE_DC ) );
		        break;
			case NBFM:
				modules.add( new AudioModule( recordAudio, NO_REMOVE_DC ) );
				modules.add( new NBFMDecoder( decodeConfig ) );
		    	modules.add( new AlwaysUnsquelchedDecoderState( DecoderType.NBFM, channelName ) );
				modules.add( getFMDemodulator( decodeConfig, pass, stop, REMOVE_DC ) );
				break;
			case LTR_STANDARD:
				modules.add( new AudioModule( recordAudio, REMOVE_DC ) );
				MessageDirection direction = ((DecodeConfigLTRStandard)decodeConfig).getMessageDirection();
				modules.add( new LTRStandardDecoder( aliasList, direction ) );
				modules.add( new LTRStandardDecoderState( aliasList ) );
				modules.add( getFMDemodulator( decodeConfig, pass, stop, NO_REMOVE_DC ) );
				break;
			case LTR_NET:
				modules.add( new AudioModule( recordAudio, REMOVE_DC ) );
				modules.add( new LTRNetDecoder( (DecodeConfigLTRNet)decodeConfig, aliasList ) );
				modules.add( new LTRNetDecoderState( aliasList ) );
				modules.add( getFMDemodulator( decodeConfig, pass, stop, NO_REMOVE_DC ) );
				break;
			case MPT1327:
				DecodeConfigMPT1327 mptConfig = (DecodeConfigMPT1327)decodeConfig;
				
				modules.add( new AudioModule( recordAudio, NO_REMOVE_DC ) );

				ChannelMap channelMap = resourceManager.getPlaylistManager().getPlayist()
					.getChannelMapList().getChannelMap( mptConfig.getChannelMapName() );
				
				Sync sync = mptConfig.getSync();
				
				modules.add( new MPT1327Decoder( aliasList, sync ) );
				modules.add( new MPT1327DecoderState( aliasList, channelMap, 
						channelType, mptConfig.getCallTimeout() * 1000 ) );

				if( channelType == ChannelType.STANDARD )
				{
					long timeout = mptConfig.getCallTimeout() * 1000; //convert to milliseconds

					modules.add( new TrafficChannelManager( resourceManager, 
							decodeConfig, recordConfig, system, site, 
							( aliasList != null ? aliasList.getName() : null ), timeout, 
							mptConfig.getTrafficChannelPoolSize() ) );
				}
				
				modules.add( getFMDemodulator( decodeConfig, pass, stop, REMOVE_DC ) );
				break;
			case PASSPORT:
				modules.add( new AudioModule( recordAudio, REMOVE_DC ) );
				modules.add( new PassportDecoder( decodeConfig, aliasList ) );
				modules.add( new PassportDecoderState( aliasList ) );
				modules.add( getFMDemodulator( decodeConfig, pass, stop, NO_REMOVE_DC ) );
				break;
			case P25_PHASE1:
				DecodeConfigP25Phase1 p25Config = (DecodeConfigP25Phase1)decodeConfig;

				Modulation modulation = p25Config.getModulation();
				
				switch( modulation )
				{
					case C4FM:
						modules.add( getFMDemodulator( decodeConfig, 6750, 7500, NO_REMOVE_DC ) );
						modules.add( new P25_C4FMDecoder( aliasList ) );
						modules.add( new P25DecoderState( aliasList, channelType, 
								Modulation.C4FM, p25Config.getIgnoreDataCalls() ) );
						break;
					case CQPSK:
						modules.add( new P25_LSMDecoder( aliasList ) );
						modules.add( new P25DecoderState( aliasList, channelType, 
								Modulation.CQPSK, p25Config.getIgnoreDataCalls() ) );
						break;
					default:
						throw new IllegalArgumentException( "Unrecognized P25 "
							+ "Phase 1 Modulation [" + modulation + "]" );
				}

				if( channelType == ChannelType.STANDARD )
				{
					//Set call timeout to 3 seconds for P25 traffic channels
					long timeout = 3000;

					modules.add( new TrafficChannelManager( resourceManager, 
							decodeConfig, recordConfig, system, site, 
							( aliasList != null ? aliasList.getName() : null ), timeout, 
							p25Config.getTrafficChannelPoolSize() ) );
				}
				
				modules.add( new P25AudioModule( recordAudio ) );
				break;
			default:
				throw new IllegalArgumentException( 
					"Unknown decoder type [" + 
							decodeConfig.getDecoderType().toString() + "]" );
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
	public static List<Module> getAuxiliaryDecoders( AuxDecodeConfiguration config,
			AliasList aliasList )
	{
		List<Module> modules = new ArrayList<>();

		if( config != null )
		{
			for( DecoderType auxDecoder: config.getAuxDecoders() )
			{
				switch( auxDecoder )
				{
					case FLEETSYNC2:
						modules.add( new Fleetsync2Decoder( aliasList ) );
						modules.add( new Fleetsync2DecoderState( aliasList ) );
						break;
					case MDC1200:
						modules.add( new MDCDecoder( aliasList ) );
						modules.add( new MDCDecoderState( aliasList ) );
						break;
					case LJ_1200:
						modules.add( new LJ1200Decoder( aliasList ) );
						modules.add( new LJ1200DecoderState( aliasList ) );
						break;
					case TAIT_1200:
						modules.add( new Tait1200Decoder( aliasList ) );
						modules.add( new Tait1200DecoderState( aliasList ) );
						break;
					default:
						throw new IllegalArgumentException( "Unrecognized auxiliary "
								+ "decoder type [" + auxDecoder + "]" );
				}
			}
		}
		
		return modules;
	}

	public static DecoderPanel getDecoderPanel( SettingsManager settingsManager,
			DecoderState decoderState )
	{
		switch( decoderState.getDecoderType() )
		{
			case AM:
				return new AMDecoderPanel( settingsManager, decoderState );
			case FLEETSYNC2:
				return new Fleetsync2DecoderPanel( settingsManager, 
						(Fleetsync2DecoderState)decoderState );
			case LJ_1200:
				return new LJ1200DecoderPanel( settingsManager, 
						(LJ1200DecoderState)decoderState );
			case LTR_NET:
				return new LTRNetDecoderPanel( settingsManager, 
						(LTRNetDecoderState)decoderState );
			case LTR_STANDARD:
				return new LTRStandardDecoderPanel( settingsManager, 
						(LTRStandardDecoderState)decoderState );
			case MDC1200:
				return new MDCDecoderPanel( settingsManager, 
						(MDCDecoderState)decoderState );
			case MPT1327:
				return new MPT1327DecoderPanel( settingsManager, 
						(MPT1327DecoderState)decoderState );
			case NBFM:
				return new NBFMDecoderPanel( settingsManager, decoderState );
			case P25_PHASE1:
				return new P25DecoderPanel( settingsManager, 
						(P25DecoderState)decoderState );
			case PASSPORT:
				return new PassportDecoderPanel( settingsManager, 
						(PassportDecoderState)decoderState );
			case TAIT_1200:
				return new Tait1200DecoderPanel( settingsManager, 
						(Tait1200DecoderState)decoderState );
			default:
				throw new IllegalArgumentException( "Unrecognized decoder: " + 
						decoderState.getDecoderType().getDisplayString() );
		}
	}
	
	/**
	 * Assembles a filter set containing filters for the primary channel 
	 * decoder and each of the auxiliary decoders 
	 */
	public static FilterSet<Message> getMessageFilters( List<Module> modules )
	{
		FilterSet<Message> filterSet = new FilterSet<>();

		for( Module module: modules )
		{
			if( module instanceof Decoder )
			{
				filterSet.addFilters( getMessageFilter( 
						((Decoder)module).getDecoderType() ) );
			}
		}

		/* If we don't have any filters, add an ALL-PASS filter */
		if( filterSet.getFilters().isEmpty() )
		{
			filterSet.addFilter( new AllPassFilter<Message>() );
		}

		return filterSet;
	}
	
	/**
	 * Returns a set of IMessageFilter objects (FilterSets or Filters) that
	 * can process all of the messages produced by the specified decoder type.
	 */
	public static List<IFilter<Message>> getMessageFilter( DecoderType decoder )
	{
		ArrayList<IFilter<Message>> filters = new ArrayList<IFilter<Message>>();

		switch( decoder )
		{
			case FLEETSYNC2:
				filters.add( new FleetsyncMessageFilter() );
				break;
			case LJ_1200:
				filters.add( new LJ1200MessageFilter() );
				break;
			case LTR_NET:
				filters.add( new LTRNetMessageFilter() );
				break;
			case LTR_STANDARD:
				filters.add( new LTRStandardMessageFilter() );
				break;
			case MDC1200:
				filters.add( new MDCMessageFilter() );
				break;
			case MPT1327:
				filters.add( new MPT1327MessageFilter() );
				break;
			case P25_PHASE1:
			case P25_PHASE2:
				filters.add( new P25MessageFilterSet() );
				break;
			case PASSPORT:
				filters.add( new PassportMessageFilter() );
				break;
			default:
				break;
		}
		
		return filters;
	}
	
	public static DecodeEditor getEditorPanel( DecodeConfiguration config, 
			 ChannelNode channelNode, PlaylistManager playlistManager )
	{
		DecodeEditor configuredPanel;
		
		switch( config.getDecoderType() )
		{
			case NBFM:
				configuredPanel = new NBFMEditor( config );
				break;
			case LTR_STANDARD:
				configuredPanel = new LTRStandardEditor( config );
				break;
			case LTR_NET:
				configuredPanel = new LTRNetEditor( config );
				break;
			case MPT1327:
				configuredPanel = new MPT1327ConfigEditor( config, 
						channelNode, playlistManager );
				break;
			case PASSPORT:
				configuredPanel = new PassportEditor( config );
				break;
			case P25_PHASE1:
				configuredPanel = new P25DecodeEditor( config );
				break;
			default:
				configuredPanel = new DecodeEditor( config );
				break;
		}
		
		return configuredPanel;
	}
	
	public static String getRecordingFilename( String token )
	{
    	//Get the base recording filename
        StringBuilder sb = new StringBuilder();
        sb.append( SystemProperties.getInstance()
        					.getApplicationFolder( "recordings" ) );
        sb.append( File.separator );
        sb.append( TimeStamp.getTimeStamp( "_" ) );
        sb.append(  "_" );
        sb.append( token );

        return sb.toString();
	}
}
