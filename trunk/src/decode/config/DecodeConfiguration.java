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
package decode.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import controller.config.Configuration;
import decode.DecoderType;

@XmlSeeAlso( { DecodeConfigAM.class,
               DecodeConfigNBFM.class,
			   DecodeConfigLTRNet.class,
			   DecodeConfigLTRStandard.class,
			   DecodeConfigMPT1327.class,
			   DecodeConfigPassport.class } )
@XmlRootElement( name = "decode_configuration" )
public abstract class DecodeConfiguration extends Configuration
{
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
