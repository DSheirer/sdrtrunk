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
package module.decode.p25;

import javax.xml.bind.annotation.XmlAttribute;

import module.decode.DecoderType;
import module.decode.config.DecodeConfiguration;
import module.decode.p25.P25Decoder.Modulation;

public class DecodeConfigP25Phase1 extends DecodeConfiguration
{
	private P25_LSMDecoder.Modulation mModulation = Modulation.C4FM;
	
	private int mCallTimeout = DEFAULT_CALL_TIMEOUT_SECONDS;
	private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
	
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
	public P25_LSMDecoder.Modulation getModulation()
	{
		return mModulation;
	}
	
	public void setModulation( P25_LSMDecoder.Modulation modulation )
	{
		mModulation = modulation;
	}
	
	@XmlAttribute( name="call_timeout" )
	public int getCallTimeout()
	{
		return mCallTimeout;
	}
	
	/**
	 * Sets the call timeout value in seconds ( 10 - 600 );
	 * @param timeout
	 */
	public void setCallTimeout( int timeout )
	{
		if( CALL_TIMEOUT_MINIMUM <= timeout && timeout <= CALL_TIMEOUT_MAXIMUM )
		{
			mCallTimeout = timeout;
		}
		else
		{
			mCallTimeout = DEFAULT_CALL_TIMEOUT_SECONDS;
		}
	}

	
	@XmlAttribute( name="traffic_channel_pool_size" )
	public int getTrafficChannelPoolSize()
	{
		return mTrafficChannelPoolSize;
	}
	
	/**
	 * Sets the traffic channel pool size which is the maximum number of 
	 * simultaneous traffic channels that can be allocated.
	 * 
	 * This limits the maximum calls so that busy systems won't cause more
	 * traffic channels to be allocated than the decoder/software/host computer
	 * can support.
	 */
	public void setTrafficChannelPoolSize( int size )
	{
		mTrafficChannelPoolSize = size;
	}
}
