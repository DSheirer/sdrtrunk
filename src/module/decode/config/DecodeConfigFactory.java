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
package module.decode.config;

import module.decode.DecoderType;
import module.decode.am.DecodeConfigAM;
import module.decode.ltrnet.DecodeConfigLTRNet;
import module.decode.ltrstandard.DecodeConfigLTRStandard;
import module.decode.mpt1327.DecodeConfigMPT1327;
import module.decode.nbfm.DecodeConfigNBFM;
import module.decode.p25.DecodeConfigP25Phase1;
import module.decode.passport.DecodeConfigPassport;

public class DecodeConfigFactory
{
	public static DecodeConfiguration getDefaultDecodeConfiguration()
	{
		return getDecodeConfiguration( DecoderType.NBFM );
	}
	
	public static DecodeConfiguration 
						getDecodeConfiguration( DecoderType decoder )
	{
		DecodeConfiguration retVal;

		switch( decoder )
		{
		    case AM:
		        retVal = new DecodeConfigAM();
		        break;
			case LTR_NET:
				retVal = new DecodeConfigLTRNet();
				break;
			case LTR_STANDARD:
				retVal = new DecodeConfigLTRStandard();
				break;
			case MPT1327:
				retVal = new DecodeConfigMPT1327();
				break;
			case NBFM:
				retVal = new DecodeConfigNBFM();
				break;
			case PASSPORT:
				retVal = new DecodeConfigPassport();
				break;
			case P25_PHASE1:
				retVal = new DecodeConfigP25Phase1();
				break;
			default:
				throw new IllegalArgumentException( "DecodeConfigFactory - "
						+ "unknown decoder type [" + decoder.toString() + "]" );
		}
		
		return retVal;
	}

	/**
	 * Creates a copy of the configuration
	 */
	public static DecodeConfiguration copy( DecodeConfiguration config )
	{
		if( config != null )
		{
			switch( config.getDecoderType() )
			{
				case AM:
					DecodeConfigAM originalAM = (DecodeConfigAM)config;
					DecodeConfigAM copyAM = new DecodeConfigAM();
					copyAM.setAFC( originalAM.getAFC() );
					copyAM.setAFCMaximumCorrection( originalAM.getAFCMaximumCorrection() );
					return copyAM;
				case LTR_NET:
					DecodeConfigLTRNet originalLTRNet = (DecodeConfigLTRNet)config;
					DecodeConfigLTRNet copyLTRNet = new DecodeConfigLTRNet();
					copyLTRNet.setAFC( originalLTRNet.getAFC() );
					copyLTRNet.setAFCMaximumCorrection( originalLTRNet.getAFCMaximumCorrection() );
					copyLTRNet.setMessageDirection( originalLTRNet.getMessageDirection() );
					return copyLTRNet;
				case LTR_STANDARD:
					DecodeConfigLTRStandard originalLTRStandard = (DecodeConfigLTRStandard)config;
					DecodeConfigLTRStandard copyLTRStandard = new DecodeConfigLTRStandard();
					copyLTRStandard.setAFC( originalLTRStandard.getAFC() );
					copyLTRStandard.setAFCMaximumCorrection( originalLTRStandard.getAFCMaximumCorrection() );
					copyLTRStandard.setMessageDirection( originalLTRStandard.getMessageDirection() );
					return copyLTRStandard;
				case MPT1327:
					DecodeConfigMPT1327 originalMPT = (DecodeConfigMPT1327)config;
					DecodeConfigMPT1327 copyMPT = new DecodeConfigMPT1327();
					copyMPT.setAFC( originalMPT.getAFC() );
					copyMPT.setAFCMaximumCorrection( originalMPT.getAFCMaximumCorrection() );
					copyMPT.setCallTimeout( originalMPT.getCallTimeout() );
					copyMPT.setChannelMapName( originalMPT.getChannelMapName() );
					copyMPT.setSync( originalMPT.getSync() );
					copyMPT.setTrafficChannelPoolSize( originalMPT.getTrafficChannelPoolSize() );
					return copyMPT;
				case NBFM:
					DecodeConfigNBFM originalNBFM = (DecodeConfigNBFM)config;
					DecodeConfigNBFM copyNBFM = new DecodeConfigNBFM();
					copyNBFM.setAFC( originalNBFM.getAFC() );
					copyNBFM.setAFCMaximumCorrection( originalNBFM.getAFCMaximumCorrection() );
					return copyNBFM;
				case P25_PHASE1:
					DecodeConfigP25Phase1 originalP25 = (DecodeConfigP25Phase1)config;
					DecodeConfigP25Phase1 copyP25 = new DecodeConfigP25Phase1();
					copyP25.setAFC( originalP25.getAFC() );
					copyP25.setAFCMaximumCorrection( originalP25.getAFCMaximumCorrection() );
					copyP25.setIgnoreDataCalls( originalP25.getIgnoreDataCalls() );
					copyP25.setModulation( originalP25.getModulation() );
					copyP25.setTrafficChannelPoolSize( originalP25.getTrafficChannelPoolSize() );
					return copyP25;
				case PASSPORT:
					DecodeConfigPassport originalPass = (DecodeConfigPassport)config;
					DecodeConfigPassport copyPass = new DecodeConfigPassport();
					copyPass.setAFC( originalPass.getAFC() );
					copyPass.setAFCMaximumCorrection( originalPass.getAFCMaximumCorrection() );
					return copyPass;
				default:
					throw new IllegalArgumentException( "Unrecognized decoder "
							+ "configuration type:" + config.getDecoderType() );
			}
		}
		
		return null;
	}
}
