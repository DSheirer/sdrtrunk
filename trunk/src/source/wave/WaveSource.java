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
package source.wave;

import java.io.File;
import java.io.IOException;

import source.Source;

public abstract class WaveSource extends Source
{
	protected File mFile;
	protected long mCurrentPosition = 0;
	protected PositionListener mListener;
	
	public WaveSource( File file, SampleType sampleType )
    {
	    super( sampleType.toString() + " Wave Source", sampleType );
	    mFile = file;
    }
	
	public File getFile()
	{
		return mFile;
	}
	
	public long getCurrentPosition()
	{
		return mCurrentPosition;
	}
	
	public void incrementCurrentLocation( long value, boolean reset )
	{
		mCurrentPosition += value;
		
		if( mListener != null )
		{
			mListener.positionUpdated( mCurrentPosition, reset );
		}
	}
	
	public void addListener( PositionListener listener )
	{
		mListener = listener;
	}
	
	public abstract void open() throws IOException;
	
	public abstract void close() throws IOException;
	
	public abstract boolean next() throws IOException;
	
	public abstract boolean next( int count ) throws IOException;
	
	public abstract void jumpTo( long index ) throws IOException;
	
	public interface PositionListener
	{
		public void positionUpdated( long position, boolean reset );
	}
}
