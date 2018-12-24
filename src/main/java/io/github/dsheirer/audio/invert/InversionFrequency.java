/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * *****************************************************************************
 */
package io.github.dsheirer.audio.invert;

/**
 * Standard voice inversion foldover frequencies
 * 
 * Reference: http://en.wikipedia.org/wiki/Voice_inversion
 */
public enum InversionFrequency
{
	HZ_2500( 2500 ),
	HZ_2550( 2550 ),
	HZ_2600( 2600 ),
	HZ_2632( 2632 ),
	HZ_2675( 2675 ),
	HZ_2718( 2718 ),
	HZ_2800( 2800 ),
	HZ_2868( 2868 ),
	HZ_2950( 2950 ),
	HZ_3023( 3023 ),
	HZ_3075( 3075 ),
	HZ_3107( 3107 ),
	HZ_3150( 3150 ),
	HZ_3196( 3196 ),
	HZ_3333( 3333 ),
	HZ_3339( 3339 ),
	HZ_3400( 3400 ),
	HZ_3496( 3496 ),
	HZ_3729( 3729 ),
	HZ_4096( 4096 );
	
	private int mFrequency;
	
	private InversionFrequency( int frequency )
	{
		mFrequency = frequency;
	}

	public int getFrequency()
	{
		return mFrequency;
	}
}
