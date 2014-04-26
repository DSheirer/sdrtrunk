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
import java.util.Enumeration;

import sample.complex.ComplexSample;
import source.mixer.MixerChannel;

public class StereoByteBuffer implements Enumeration<ComplexSample>
{
	private ByteBuffer mByteBuffer;
	
	public StereoByteBuffer( byte[] bytes, int frames )
	{
		mByteBuffer = ByteBuffer.wrap( bytes );
		mByteBuffer.order( ByteOrder.LITTLE_ENDIAN );
	}
	
	public byte[] array()
	{
		return mByteBuffer.array();
	}

	public ByteBuffer getChannel( MixerChannel channel )
	{
		return StereoToMonoByteBufferConverter.getChannel( mByteBuffer, channel );
	}
	
	@Override
	public boolean hasMoreElements()
	{
		return mByteBuffer.hasRemaining();
	}

	@Override
    public ComplexSample nextElement()
    {
		return new ComplexSample( (float)mByteBuffer.getShort(),
								  (float)mByteBuffer.getShort() );
    }
}
