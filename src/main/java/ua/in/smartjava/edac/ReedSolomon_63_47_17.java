package ua.in.smartjava.edac;

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

public class ReedSolomon_63_47_17 extends BerlekempMassey_63
{
	/**
	 * Reed-Solomon RS(63,47,17) decoder.  This can also be used for error detection
	 * and correction of the following RS codes:
	 * 
	 * RS(36,20,17) - max 8 errors
	 * RS(24,16,9)  - max 4 errors
	 * RS(24,12,13) - max 6 errors
	 * 
	 * The maximum correctable errors is determined by (n-k)/2, or hamming 
	 * distance divided by 2.
	 */
	public ReedSolomon_63_47_17( int maximumCorrectableErrors )
    {
	    super( maximumCorrectableErrors );
    }
}
