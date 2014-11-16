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
package decode;

import source.Source.SampleType;
import alias.AliasList;
import controller.channel.ChannelMap;
import controller.channel.ProcessingChain;
import controller.state.ChannelState;
import decode.am.AMChannelState;
import decode.am.AMDecoder;
import decode.config.AuxDecodeConfiguration;
import decode.config.DecodeConfigLTRNet;
import decode.config.DecodeConfigLTRStandard;
import decode.config.DecodeConfigMPT1327;
import decode.config.DecodeConfiguration;
import decode.fleetsync2.Fleetsync2Decoder;
import decode.fleetsync2.FleetsyncChannelState;
import decode.lj1200.LJ1200ChannelState;
import decode.lj1200.LJ1200Decoder;
import decode.ltrnet.LTRNetChannelState;
import decode.ltrnet.LTRNetDecoder;
import decode.ltrstandard.LTRChannelState;
import decode.ltrstandard.LTRStandardDecoder;
import decode.mdc1200.MDCChannelState;
import decode.mdc1200.MDCDecoder;
import decode.mpt1327.MPT1327ChannelState;
import decode.mpt1327.MPT1327Decoder;
import decode.nbfm.NBFMChannelState;
import decode.nbfm.NBFMDecoder;
import decode.p25.P25ChannelState;
import decode.p25.P25Decoder;
import decode.passport.PassportChannelState;
import decode.passport.PassportDecoder;

public class DecoderFactory
{
	/**
	 * Returns a fully configured decoder with embedded auxiliary decoders
	 * @param channelConfig
	 * @return
	 */
	public static Decoder getDecoder( ProcessingChain chain, 
									  SampleType sampleType,
									  AliasList aliasList )
	{
		Decoder retVal = null;

		DecodeConfiguration config = 
				chain.getChannel().getDecodeConfiguration();
		
		
		switch( config.getDecoderType() )
		{
		    case AM:
		        retVal = new AMDecoder( sampleType );
		        break;
			case NBFM:
				retVal = new NBFMDecoder( sampleType );
				break;
			case LTR_STANDARD:
				DecodeConfigLTRStandard ltrStandardConfig = 
				(DecodeConfigLTRStandard)config;

				retVal = new LTRStandardDecoder( sampleType, aliasList, 
						ltrStandardConfig.getMessageDirection() );
				break;
			case LTR_NET:
				DecodeConfigLTRNet ltrNetConfig = (DecodeConfigLTRNet)config;

				retVal = new LTRNetDecoder( sampleType, aliasList, 
						ltrNetConfig.getMessageDirection() );
				break;
			case MPT1327:
				DecodeConfigMPT1327 mptConfig = (DecodeConfigMPT1327)config;

				
				retVal = new MPT1327Decoder( sampleType, aliasList, 
									mptConfig.getSync() );
				break;
			case PASSPORT:
				retVal = new PassportDecoder( sampleType, aliasList );
				break;
			case P25_PHASE1:
				retVal = new P25Decoder( sampleType, aliasList );
				break;
			default:
				throw new IllegalArgumentException( 
					"Unknown decoder type [" + 
							config.getDecoderType().toString() + "]" );
		}

		if( retVal != null )
		{
			AuxDecodeConfiguration auxConfig = 
					chain.getChannel().getAuxDecodeConfiguration();

			for( DecoderType auxDecoder: auxConfig.getAuxDecoders() )
			{
				switch( auxDecoder )
				{
					case FLEETSYNC2:
						retVal.addAuxiliaryDecoder( 
								new Fleetsync2Decoder( aliasList ) );
						break;
					case MDC1200:
						retVal.addAuxiliaryDecoder( 
								new MDCDecoder( aliasList ) );
						break;
					case LJ_1200:
						retVal.addAuxiliaryDecoder( 
								new LJ1200Decoder( aliasList ) );
				}
			}
		}
		
		return retVal;
	}

	public static ChannelState getChannelStateNew( ProcessingChain chain, 
												   AliasList aliasList )
	{
		ChannelState retVal = null;

		DecodeConfiguration decodeConfig = 
				chain.getChannel().getDecodeConfiguration(); 

		switch( decodeConfig.getDecoderType() )
		{
		    case AM:
		        retVal = new AMChannelState( chain, aliasList );
		        break;
			case NBFM:
				retVal = new NBFMChannelState( chain, aliasList );
				break;
			case LTR_STANDARD:
				retVal = new LTRChannelState( chain, aliasList );
				break;
			case LTR_NET:
				retVal = new LTRNetChannelState( chain, aliasList );
				break;
			case MPT1327:
				DecodeConfigMPT1327 mpt = (DecodeConfigMPT1327)decodeConfig;
				
				String channelMapName = mpt.getChannelMapName();
				
				ChannelMap map = chain.getResourceManager().getPlaylistManager()
						.getPlayist().getChannelMapList()
						.getChannelMap( channelMapName );
				
				retVal = new MPT1327ChannelState( chain, aliasList, map );
				break;
			case PASSPORT:
				retVal = new PassportChannelState( chain, aliasList );
				break;
			case P25_PHASE1:
				retVal = new P25ChannelState( chain, aliasList );
				break;
			default:
				break;
		}
		
		if( retVal != null )
		{
			AuxDecodeConfiguration auxConfig = 
					chain.getChannel().getAuxDecodeConfiguration();
			
			for( DecoderType auxDecoder: auxConfig.getAuxDecoders() )
			{
				switch( auxDecoder )
				{
					case FLEETSYNC2:
						retVal.addAuxChannelState( 
								new FleetsyncChannelState( retVal ) );
						break;
					case LJ_1200:
						retVal.addAuxChannelState( 
								new LJ1200ChannelState( retVal ) );
						break;
					case MDC1200:
						retVal.addAuxChannelState( 
								new MDCChannelState( retVal ) );
						break;
						
				}
			}
		}

		return retVal;
	}
}
