/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.source.tuner;

public enum TunerType
{
	AIRSPY_R820T( "Airspy R820T" ),
	ELONICS_E4000( "E4000" ),
	ETTUS_WBX( "WBX" ),
	ETTUS_VARIOUS( "Ettus Tuner" ),
	FCI_FC2580( "FC2580" ),
	FITIPOWER_FC0012( "FC0012" ),
	FITIPOWER_FC0013( "FC0013" ),
	FUNCUBE_DONGLE_PRO( "Funcube Dongle Pro" ),
	FUNCUBE_DONGLE_PRO_PLUS( "Funcube Dongle Pro Plus" ),
	HACKRF( "HackRF" ),
	RAFAELMICRO_R820T( "R820T" ),
	RAFAELMICRO_R828D( "R828D" ),
	RTL2832_VARIOUS( "Generic" ),
	TEST("Test"),
	RECORDING("Recording"),
	UNKNOWN( "Unknown" );
	
	private String mLabel;
	
	private TunerType( String label )
	{
		mLabel = label;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
}
