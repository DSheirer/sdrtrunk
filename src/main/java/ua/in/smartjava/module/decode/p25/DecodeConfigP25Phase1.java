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
package ua.in.smartjava.module.decode.p25;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import ua.in.smartjava.module.decode.DecoderType;
import ua.in.smartjava.module.decode.config.DecodeConfiguration;
import ua.in.smartjava.module.decode.p25.P25Decoder.Modulation;

public class DecodeConfigP25Phase1 extends DecodeConfiguration
{
	private P25_LSMDecoder.Modulation mModulation = Modulation.C4FM;
	
	private int mCallTimeout = 1;
	private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
	private boolean mIgnoreDataCalls = true;
	
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
	
	@XmlAttribute( name = "ignore_data_calls" )
	public boolean getIgnoreDataCalls()
	{
		return mIgnoreDataCalls;
	}
	
	public void setIgnoreDataCalls( boolean ignore )
	{
		mIgnoreDataCalls = ignore;
	}

	/**
	 * Note: this field is now deprecated.
	 * @return
	 */
	@XmlTransient
	@Deprecated
	public int getCallTimeout()
	{
		return mCallTimeout;
	}
	
	/**
	 * Sets the call timeout value in seconds ( 10 - 600 );
	 * @param timeout
	 */
	@Deprecated
	public void setCallTimeout( int timeout )
	{
	}

	
	@XmlAttribute( name="traffic_channel_pool_size" )
	public int getTrafficChannelPoolSize()
	{
		return mTrafficChannelPoolSize;
	}
	
	/**
	 * Sets the traffic ua.in.smartjava.channel pool size which is the maximum number of
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
