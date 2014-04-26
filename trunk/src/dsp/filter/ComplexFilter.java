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
import sample.complex.ComplexSample;

public abstract class ComplexFilter implements Listener<ComplexSample>
{
	private Listener<ComplexSample> mListener;
	
	public void setListener( Listener<ComplexSample> listener )
	{
		mListener = listener;
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public Listener<ComplexSample> getListener()
	{
		return mListener;
	}
	
	public boolean hasListener()
	{
		return mListener != null;
	}
	
	protected void send( ComplexSample sample )
	{
		if( mListener != null )
		{
			mListener.receive( sample );
		}
	}
}
