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

import java.util.Arrays;

import sample.Listener;


/**
 * Assembles single samples into sample blocks (ie arrays) for broadcast to
 * registered SampleBlockListeners
 */
public class FloatBufferAssembler implements Listener<Float>
{
    private Float[] mFloatBuffer;
    private int mBufferSize;
    private int mBufferPointer = 0;
    private Listener<Float[]> mListener;
    
    public FloatBufferAssembler( int bufferSize )
    {
        mBufferSize = bufferSize;
        mFloatBuffer = new Float[ mBufferSize ];
    }
    
    /**
     * Stores received samples in the sample block and manages the storage
     * pointer.  Sends sample blocks when full.
     */
    @Override
    public void receive( Float sample )
    {
        mFloatBuffer[ mBufferPointer++ ] = sample;
        
        if( mBufferPointer == mBufferSize )
        {
        	if( mListener != null )
        	{
        		mListener.receive( Arrays.copyOf( mFloatBuffer, mBufferSize ) );
        	}

            mBufferPointer = 0;
        }
    }
    
    /**
     * Adds a new sampleblock listener to receive sample blocks as they are 
     * assembled from the sample stream
     */
    public void setListener( Listener<Float[]> listener )
    {
        mListener = listener;
    }
}
