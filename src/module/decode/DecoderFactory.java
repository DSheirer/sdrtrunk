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
import module.decode.config.AuxDecodeConfiguration;
import module.decode.config.DecodeConfiguration;
import module.decode.fleetsync2.Fleetsync2Decoder;
import module.decode.fleetsync2.FleetsyncMessageFilter;
import module.decode.lj1200.LJ1200Decoder;
import module.decode.lj1200.LJ1200MessageFilter;
import module.decode.ltrnet.DecodeConfigLTRNet;
import module.decode.ltrnet.LTRNetDecoder;
import module.decode.ltrnet.LTRNetDecoderPanel;
import module.decode.ltrnet.LTRNetEditor;
import module.decode.ltrnet.LTRNetMessageFilter;
import module.decode.ltrstandard.DecodeConfigLTRStandard;
import module.decode.ltrstandard.LTRStandardDecoder;
import module.decode.ltrstandard.LTRStandardDecoderPanel;
import module.decode.ltrstandard.LTRStandardEditor;
import module.decode.ltrstandard.LTRStandardMessageFilter;
import module.decode.mdc1200.MDCDecoder;
import module.decode.mdc1200.MDCMessageFilter;
import module.decode.mpt1327.DecodeConfigMPT1327;
import module.decode.mpt1327.MPT1327ConfigEditor;
import module.decode.mpt1327.MPT1327Decoder;
import module.decode.mpt1327.MPT1327Decoder.Sync;
import module.decode.mpt1327.MPT1327DecoderPanel;
import module.decode.mpt1327.MPT1327MessageFilter;
import module.decode.nbfm.NBFMDecoder;
import module.decode.nbfm.NBFMDecoderPanel;
import module.decode.nbfm.NBFMEditor;
import module.decode.p25.DecodeConfigP25Phase1;
import module.decode.p25.P25Decoder;
import module.decode.p25.P25Decoder.Modulation;
import module.decode.p25.P25DecoderPanel;
import module.decode.p25.P25Editor;
import module.decode.p25.P25_C4FMDecoder;
import module.decode.p25.P25_LSMDecoder;
import module.decode.p25.audio.P25AudioModule;
import module.decode.p25.message.filter.P25MessageFilterSet;
import module.decode.passport.PassportEditor;
import module.decode.passport.PassportMessageFilter;
import module.decode.state.DecoderPanel;
import module.decode.state.TrafficChannelManager;
import module.decode.tait.Tait1200Decoder;
import module.demodulate.fm.FMDemodulatorModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import properties.SystemProperties;
import settings.SettingsManager;
import util.TimeStamp;
import alias.AliasList;
import audio.AudioModule;
import controller.ResourceManager;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelNode;
import controller.channel.map.ChannelMap;
import filter.AllPassFilter;
import filter.FilterSet;
import filter.IFilter;

public class DecoderFactory
{
	private final static Logger mLog = LoggerFactory.getLogger( DecoderFactory.class );
	
	public static final boolean NO_DC_REMOVAL = false;
	public static final boolean REMOVE_DC = true;
	
	public static final int NO_FREQUENCY_CORRECTION = 0;
	
	/**
	 * Returns a list of one primary decoder and any auxiliary decoders, as
	 * specified in the configurations.
	 * 
	 * @param config - primary decoder configuration
	 * @param auxConfig - auxiliary decoder(s) configuration
	 * 
	 * @return list of configured decoders
	 */
	public static List<Module> getModules( ChannelType channelType,
										   ResourceManager resourceManager,
										   DecodeConfiguration config, 
										   AuxDecodeConfiguration auxConfig, 
										   AliasList aliasList )
	{
		List<Module> modules = getPrimaryModules( channelType, config, 
				aliasList, resourceManager );
		
		modules.addAll( getAuxiliaryDecoders( auxConfig, aliasList ) );
		
		return modules;
	}
	
	public static FMDemodulatorModule getFMDemodulator( DecodeConfiguration config, int pass, int stop, boolean removeDC )
	{
		if( config.isAFCEnabled() )
		{
			return new FMDemodulatorModule( pass, stop, config.getAFCMaximumCorrection(), removeDC );
		}
		else
		{
			return new FMDemodulatorModule( pass, stop, NO_FREQUENCY_CORRECTION, removeDC );
		}
	}

	/**
	 * Constructs a primary decoder as specified in the decode configuration
	 * @param config - primary decoder configuration
	 * @param aliasList - optional alias list
	 * @return configured decoder or null
	 */
	public static List<Module> getPrimaryModules( ChannelType channelType,
												  DecodeConfiguration config, 
												  AliasList aliasList, 
												  ResourceManager resourceManager )
	{
		List<Module> modules = new ArrayList<Module>();

		/* Baseband low-pass filter pass and stop frequencies */
		int pass = config.getDecoderType().getChannelBandwidth() / 2;
		int stop = pass + 1250;
		
		switch( config.getDecoderType() )
		{
		    case AM:
//		    	decoder = new AMDecoder();
		        break;
			case NBFM:
				modules.add( new AudioModule() );
				modules.add( new NBFMDecoder( config ) );
				modules.add( getFMDemodulator( config, pass, stop, REMOVE_DC ) );
				break;
			case LTR_STANDARD:
				modules.add( new AudioModule( REMOVE_DC ) );
				MessageDirection direction = ((DecodeConfigLTRStandard)config).getMessageDirection();
				modules.add( new LTRStandardDecoder( aliasList, direction ) );
				modules.add( getFMDemodulator( config, pass, stop, NO_DC_REMOVAL ) );
				break;
			case LTR_NET:
				modules.add( new AudioModule( REMOVE_DC ) );
				modules.add( new LTRNetDecoder( (DecodeConfigLTRNet)config, aliasList ) );
//				modules.add( new ComplexBufferRecorderModule( getRecordingFilename( "decimated_baseband" ) ) );
				modules.add( getFMDemodulator( config, pass, stop, NO_DC_REMOVAL ) );
				break;
			case MPT1327:
				DecodeConfigMPT1327 mptConfig = (DecodeConfigMPT1327)config;
				
				modules.add( new AudioModule() );

				ChannelMap channelMap = resourceManager.getPlaylistManager().getPlayist()
					.getChannelMapList().getChannelMap( mptConfig.getChannelMapName() );
				Sync sync = mptConfig.getSync();
				modules.add( new MPT1327Decoder( channelMap, channelType, aliasList, sync ) );

				if( channelType == ChannelType.STANDARD )
				{
					long timeout = mptConfig.getCallTimeout() * 1000; //convert to milliseconds

					modules.add( new TrafficChannelManager( resourceManager, 
							config, timeout, 
							mptConfig.getTrafficChannelPoolSize() ) );
				}
				
//				modules.add( new RealBufferRecorderModule( getRecordingFilename( "demodulated_mpt1327" ) ) );
				modules.add( getFMDemodulator( config, pass, stop, REMOVE_DC ) );
				break;
			case PASSPORT:
//				decoder = new PassportDecoder( config, aliasList );
				break;
			case P25_PHASE1:
				DecodeConfigP25Phase1 p25Config = (DecodeConfigP25Phase1)config;

				Modulation modulation = p25Config.getModulation();
				
				switch( modulation )
				{
					case C4FM:
						modules.add( getFMDemodulator( config, 6750, 7500, REMOVE_DC ) );
						modules.add( new P25_C4FMDecoder( aliasList, channelType ) );
						break;
					case CQPSK:
						modules.add( new P25_LSMDecoder( aliasList, channelType ) );
						break;
					default:
						throw new IllegalArgumentException( "Unrecognized P25 "
							+ "Phase 1 Modulation [" + modulation + "]" );
				}

				if( channelType == ChannelType.STANDARD )
				{
					long timeout = p25Config.getCallTimeout() * 1000; //convert to milliseconds

					modules.add( new TrafficChannelManager( resourceManager, 
							config, timeout, 
							p25Config.getTrafficChannelPoolSize() ) );
				}
				
				modules.add( new P25AudioModule() );
				break;
			default:
				throw new IllegalArgumentException( 
					"Unknown decoder type [" + 
							config.getDecoderType().toString() + "]" );
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
	public static List<Decoder> getAuxiliaryDecoders( AuxDecodeConfiguration config,
			AliasList aliasList )
	{
		List<Decoder> decoders = new ArrayList<Decoder>();

		for( DecoderType auxDecoder: config.getAuxDecoders() )
		{
			switch( auxDecoder )
			{
				case FLEETSYNC2:
					decoders.add( new Fleetsync2Decoder( aliasList ) );
					break;
				case MDC1200:
					decoders.add( new MDCDecoder( aliasList ) );
					break;
				case LJ_1200:
					decoders.add( new LJ1200Decoder( aliasList ) );
					break;
				case TAIT_1200:
					decoders.add( new Tait1200Decoder( aliasList ) );
					break;
			}
		}
		
		return decoders;
	}

	public static DecoderPanel getDecoderPanel( SettingsManager settingsManager,
			Decoder decoder )
	{
		switch( decoder.getDecoderType() )
		{
//			case AM:
//				break;
//			case FLEETSYNC2:
//				break;
//			case LJ_1200:
//				break;
			case LTR_NET:
				return new LTRNetDecoderPanel( settingsManager, (LTRNetDecoder)decoder );
			case LTR_STANDARD:
				return new LTRStandardDecoderPanel( settingsManager, (LTRStandardDecoder)decoder );
//			case MDC1200:
//				break;
			case MPT1327:
				return new MPT1327DecoderPanel( settingsManager, (MPT1327Decoder)decoder );
			case NBFM:
				return new NBFMDecoderPanel( settingsManager, decoder );
			case P25_PHASE1:
				return new P25DecoderPanel( settingsManager, (P25Decoder)decoder );
//			case PASSPORT:
//				break;
			case TAIT_1200:
				break;
			default:
				throw new IllegalArgumentException( "Unrecognized decoder: " + 
						decoder.getDecoderType().getDisplayString() );
		}

		return null;
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
	
	public static DecodeEditor getPanel( DecodeConfiguration config, 
			 ChannelNode channelNode )
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
				configuredPanel = new MPT1327ConfigEditor( config, channelNode );
				break;
			case PASSPORT:
				configuredPanel = new PassportEditor( config );
				break;
			case P25_PHASE1:
				configuredPanel = new P25Editor( config );
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
