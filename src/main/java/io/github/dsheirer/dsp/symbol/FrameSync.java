/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.dsp.symbol;

public enum FrameSync
{
	P25_PHASE1_ERROR_90_CCW( 0xFFEFAFAAEEAAl ),
	P25_PHASE1_NORMAL(       0x5575F5FF77FFl ),  // +33333 -3 +33 -33 +33 -3333 +3 -3 +3 -33333
	P25_PHASE1_ERROR_90_CW(  0x001050551155l ),
	P25_PHASE1_ERROR_180(    0xAA8A0A008800l ),

	P25_PHASE2_NORMAL(      0x575D57F7FFl),
	P25_PHASE2_ERROR_90_CCW(0xFEFBFEAEAAl),
	P25_PHASE2_ERROR_90_CW( 0x0104015155l),
	P25_PHASE2_ERROR_180(   0xA8A2A80800l);
	
	private long mSync;
	
	FrameSync( long sync )
	{
		mSync = sync;
	}
	
	public long getSync()
	{
		return mSync;
	}
}
