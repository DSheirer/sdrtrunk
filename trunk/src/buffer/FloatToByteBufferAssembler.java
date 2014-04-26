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

import sample.Broadcaster;
import sample.Listener;

public class FloatToByteBufferAssembler implements Listener<Float>
{
	private Broadcaster<ByteBuffer> mBroadcaster = new Broadcaster<ByteBuffer>();
	
	private int mBufferSize = 0;
	private ByteBuffer mBuffer;
	
	public FloatToByteBufferAssembler( int bufferSize )
	{
		mBufferSize = bufferSize;
		resetBuffer();
	}

    public void addByteBufferListener( Listener<ByteBuffer> listener )
    {
		mBroadcaster.addListener( listener );
    }

    public void removeByteBufferListener( Listener<ByteBuffer> listener )
    {
		mBroadcaster.removeListener( listener );
    }
	
	private void resetBuffer()
	{
		mBuffer = ByteBuffer.allocate( mBufferSize );
		mBuffer.order( ByteOrder.BIG_ENDIAN );
	}

	@Override
    public void receive( Float sample )
    {
		mBuffer.putShort( Float.valueOf( sample ).shortValue() );
		
		if( mBuffer.position() >= mBufferSize )
		{
			//Buffer is full ... send it
			mBroadcaster.receive( mBuffer );
			
			//Reset the buffer
			resetBuffer();
		}
    }
}
