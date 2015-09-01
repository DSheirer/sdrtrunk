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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import module.decode.DecoderType;
import module.decode.am.DecodeConfigAM;
import module.decode.ltrnet.DecodeConfigLTRNet;
import module.decode.ltrstandard.DecodeConfigLTRStandard;
import module.decode.mpt1327.DecodeConfigMPT1327;
import module.decode.nbfm.DecodeConfigNBFM;
import module.decode.p25.DecodeConfigP25Phase1;
import module.decode.passport.DecodeConfigPassport;
import controller.config.Configuration;

@XmlSeeAlso( { DecodeConfigAM.class,
               DecodeConfigNBFM.class,
			   DecodeConfigLTRNet.class,
			   DecodeConfigLTRStandard.class,
			   DecodeConfigMPT1327.class,
			   DecodeConfigPassport.class,
			   DecodeConfigP25Phase1.class } )
@XmlRootElement( name = "decode_configuration" )
public abstract class DecodeConfiguration extends Configuration
{
	public static final int DEFAULT_CALL_TIMEOUT_SECONDS = 45;
	public static final int CALL_TIMEOUT_MINIMUM = 10;
	public static final int CALL_TIMEOUT_MAXIMUM = 300; //5 minutes
	
	public static final int TRAFFIC_CHANNEL_LIMIT_DEFAULT = 3;
	public static final int TRAFFIC_CHANNEL_LIMIT_MINIMUM = 0;
	public static final int TRAFFIC_CHANNEL_LIMIT_MAXIMUM = 30;
	
	private DecoderType mDecoderType = DecoderType.NBFM;
	private boolean mAFCEnabled = true;
	private int mAFCMaxCorrection = 3000;

	public DecodeConfiguration()
	{
		this( DecoderType.NBFM );
	}
	
	public DecodeConfiguration( DecoderType type )
	{
		mDecoderType = type;
	}

	public DecoderType getDecoderType()
	{
		return mDecoderType;
	}

	@XmlElement( name = "afc" )
	public boolean getAFC()
	{
		return mAFCEnabled;
	}
	
	public boolean isAFCEnabled()
	{
		return mAFCEnabled;
	}
	
	public void setAFC( boolean enabled )
	{
		mAFCEnabled = enabled;
	}
	
	public int getAFCMaximumCorrection()
	{
		return mAFCMaxCorrection;
	}
	
	public void setAFCMaximumCorrection( int max )
	{
		mAFCMaxCorrection = max;
	}
	
	public boolean supportsAFC()
	{
	    return true;
	}
}
