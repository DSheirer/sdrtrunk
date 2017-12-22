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
package io.github.dsheirer.dsp.symbol;

import java.util.BitSet;

public class SymbolEvent
{
	private BitSet mBitset;
	private int mSamplesPerSymbol;
	private boolean mDecision;
	private Shift mShift;
	
	public SymbolEvent( BitSet bitset, int samplesPerSymbol, boolean decision, Shift shift )
	{
		mBitset = bitset;
		mSamplesPerSymbol = samplesPerSymbol;
		mDecision = decision;
		mShift = shift;
	}
	
	public BitSet getBitSet()
	{
		return mBitset;
	}
	
	public int getSamplesPerSymbol()
	{
		return mSamplesPerSymbol;
	}
	
	public boolean getDecision()
	{
		return mDecision;
	}
	
	public Shift getShift()
	{
		return mShift;
	}

	public enum Shift
	{ 
		LEFT( "<" ), 
		AGGRESSIVE_LEFT( "<<" ),
		RIGHT( ">" ), 
		AGGRESSIVE_RIGHT( ">>" ),
		NONE( "=" );
		
		private String mLabel;
		
		private Shift( String label )
		{
			mLabel = label;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
	}
}
