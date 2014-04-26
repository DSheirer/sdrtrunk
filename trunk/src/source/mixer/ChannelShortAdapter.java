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
package source.mixer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import source.Source.SampleType;

/**
 * Converts 16-bit little endian byte data into an array of floats 
 */
public class ChannelShortAdapter extends SampleAdapter
{
	private ByteOrder mByteOrder = ByteOrder.LITTLE_ENDIAN;
	private MixerChannel mMixerChannel;
	
	public ChannelShortAdapter( MixerChannel channel )
	{
		/* Only use with LEFT/RIGHT channels */
		assert( channel != MixerChannel.MONO );
		
		mMixerChannel = channel;
	}
			
	@Override
    public Float[] convert( byte[] samples )
    {
		Float[] processed = new Float[ samples.length / 4 ];

		int pointer = 0;

		/* Wrap byte array in a byte buffer so we can process them as shorts */
		ByteBuffer buffer = ByteBuffer.wrap( samples );

		/* Set endian to correct byte ordering */
		buffer.order( mByteOrder );

        while( buffer.hasRemaining() )
        {
        	if( mMixerChannel == MixerChannel.LEFT )
        	{
            	processed[ pointer ] = (float)buffer.getShort();

            	/* Throw away the right channel */
            	buffer.getShort();
        	}
        	else
        	{
            	/* Throw away the left channel */
            	buffer.getShort();

            	processed[ pointer ] = (float)buffer.getShort();
        	}
        	
        	pointer++;
        }
        
        return processed;
    }

	/**
	 * Set byte interpretation to little or big endian.  Defaults to LITTLE
	 * endian
	 */
	public void setByteOrder( ByteOrder order )
	{
		mByteOrder = order;
	}
}
