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
package ua.in.smartjava.module.decode.am;

import ua.in.smartjava.module.decode.DecoderType;
import ua.in.smartjava.module.decode.config.DecodeConfiguration;

public class DecodeConfigAM extends DecodeConfiguration
{
	public DecodeConfigAM()
    {
	    super( DecoderType.AM );
	    
	    setAFC( false );
    }
	
    public boolean supportsAFC()
    {
        return false;
    }
}
