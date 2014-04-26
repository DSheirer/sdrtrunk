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

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;

import controller.config.Configuration;
import decode.DecoderType;

public class AuxDecodeConfiguration extends Configuration
{
	ArrayList<DecoderType> mAuxDecoders = new ArrayList<DecoderType>();
	
	public AuxDecodeConfiguration()
	{
	}

	@XmlElement( name="aux_decoder" )
	public ArrayList<DecoderType> getAuxDecoders()
	{
		return mAuxDecoders;
	}
	
	public void setAuxDecoders( ArrayList<DecoderType> decoders )
	{
		mAuxDecoders = decoders;
	}
	
	public void addAuxDecoder( DecoderType decoder )
	{
		mAuxDecoders.add( decoder );
	}
	
	public void removeAuxDecoder( DecoderType decoder )
	{
		mAuxDecoders.remove( decoder );
	}
	
	public void clearAuxDecoders()
	{
		mAuxDecoders.clear();
	}
}
