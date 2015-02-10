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

import javax.xml.bind.annotation.XmlAttribute;

import decode.DecoderType;
import decode.p25.P25Decoder;
import decode.p25.P25Decoder.Modulation;

public class DecodeConfigP25Phase1 extends DecodeConfiguration
{
	private P25Decoder.Modulation mModulation = Modulation.C4FM;
	
	public DecodeConfigP25Phase1()
    {
	    super( DecoderType.P25_PHASE1 );
	    
	    setAFC( false );
    }
	
    public boolean supportsAFC()
    {
        return false;
    }
    
	@XmlAttribute( name = "modulation" )
	public P25Decoder.Modulation getModulation()
	{
		return mModulation;
	}
	
	public void setModulation( P25Decoder.Modulation modulation )
	{
		mModulation = modulation;
	}
}
