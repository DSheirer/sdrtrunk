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
package dsp.filter;

import sample.Listener;
import sample.complex.Complex;

@Deprecated
public abstract class ComplexFilter implements Listener<Complex>
{
	private Listener<Complex> mListener;
	
	public void setListener( Listener<Complex> listener )
	{
		mListener = listener;
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public Listener<Complex> getListener()
	{
		return mListener;
	}
	
	public boolean hasListener()
	{
		return mListener != null;
	}
	
	protected void send( Complex sample )
	{
		if( mListener != null )
		{
			mListener.receive( sample );
		}
	}
}
