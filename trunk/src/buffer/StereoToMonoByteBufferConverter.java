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
package buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import source.mixer.MixerChannel;

public class StereoToMonoByteBufferConverter
{
	public static ByteBuffer getChannel( ByteBuffer buffer, MixerChannel channel )
	{
		byte[] channelBuffer = new byte[ buffer.capacity() / 2 ];

		
		int channelBufferPointer = 0;

		if( channel == MixerChannel.RIGHT )
		{
			//throw away the first two bytes
			buffer.get();
			buffer.get();
		}
		
		while( channelBufferPointer < channelBuffer.length )
		{
			if( buffer.hasRemaining() )
			{
				channelBuffer[ channelBufferPointer++ ] = buffer.get();
				channelBuffer[ channelBufferPointer++ ] = buffer.get();
			}
			
			if( buffer.hasRemaining() )
			{
				buffer.get();
				buffer.get();
			}
		}
		
		return ByteBuffer.wrap( channelBuffer ).order( ByteOrder.LITTLE_ENDIAN );
	}
}
