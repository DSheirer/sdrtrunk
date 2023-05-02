/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.source.tuner;

import java.util.EnumSet;

/**
 * Tuner Class enumeration
 */
public enum TunerClass
{
	AIRSPY("Airspy"),
	AIRSPY_HF("Airspy HF+"),
	FUNCUBE_DONGLE_PRO("Funcube Dongle Pro" ),
	FUNCUBE_DONGLE_PRO_PLUS("Funcube Dongle Pro+" ),
	HACKRF("HackRF" ),
	RTL2832("RTL-2832"),
	RSP("RSP"),
	TEST_TUNER("Test"),
	RECORDING_TUNER("Recording"),
	UNKNOWN("Unknown" );
	
	private String mDescription;

	/**
	 * Constructs an entry
	 * @param description of the entry
	 */
	TunerClass(String description)
	{
		mDescription = description;
	}

	@Override
	public String toString()
	{
		return mDescription;
	}

	public static final EnumSet<TunerClass> SUPPORTED_USB_TUNERS = EnumSet.of(AIRSPY, AIRSPY_HF, HACKRF, RTL2832,
			FUNCUBE_DONGLE_PRO, FUNCUBE_DONGLE_PRO_PLUS);

	public static final EnumSet<TunerClass> FUNCUBE_TUNERS = EnumSet.of(FUNCUBE_DONGLE_PRO, FUNCUBE_DONGLE_PRO_PLUS);

	/**
	 * Indicates if this tuner class entry is a supported USB tuner class.
	 */
	public boolean isSupportedUsbTuner()
	{
		return SUPPORTED_USB_TUNERS.contains(this);
	}

	/**
	 * Indicates if the tuner class is a funcube dongle with a matching sound card interface
	 */
	public boolean isFuncubeTuner()
	{
		return FUNCUBE_TUNERS.contains(this);
	}

	/**
	 * Lookup a USB tuner class from the USB vid and pid
	 * @param vendor id
	 * @param product id
	 * @return tuner class or UNKNOWN
	 */
	public static TunerClass lookup(short vendor, short product)
	{
		//Combine vid & pid to make an integer that we can switch on
		int id = ((vendor & 0xFFFF) << 16) | (product & 0xFFFF);

		switch(id)
		{
			case 0x04D8FB31:
				return FUNCUBE_DONGLE_PRO_PLUS;
			case 0x04D8FB56:
				return FUNCUBE_DONGLE_PRO;
			case 0x0BDA2832: //GENERIC RTL-2832
			case 0x0BDA2838: //GENERIC RTL-2832/2838
			case 0x185B0620: //COMPRO VIDEOMATE U620F
			case 0x185B0650: //COMPRO VIDEOMATE U650F
			case 0x185B0680: //COMPRO VIDEOMATE U680F
			case 0x1B80D393: //GIGABYTE GTU7300
			case 0x1B80D395: //PEAK 102569AGPK
			case 0x1B80D398: //ZAAPA ZTMINDVBZP
			case 0x1B80D39D: //SVEON_STV20
			case 0x1B80D3A4: //TWINTECH UT40
			case 0x1D191101: //DEXATEK LIGILINK VG002A
			case 0x1D191102: //DEXATEK DIGIVOX MINI II REV3
			case 0x1D191103: //DEXATEK 5217 DVBT
			case 0x1F4DB803: //GTEK T803
			case 0x1F4DC803: //LIFEVIEW LV5T DELUXE
			case 0x1F4DD286: //MIGICA TD312
			case 0x1F4DD803: //PROLECTRIX DV107669
			case 0x0CCD00A9: //TERRATEC CINERGY T REV1
			case 0x0CCD00B3: //TERRATEC NOXON REV1 B3
			case 0x0CCD00B4: //TERRATEC NOXON REV1 B4
			case 0x0CCD00B7: //TERRATEC NOXON REV1 B7
			case 0x0CCD00C6: //TERRATEC NOXON REV1 C6
			case 0x0CCD00D3: //TERRATEC CINERGY T REV3
			case 0x0CCD00D7: //TERRATEC T STICK PLUS
			case 0x0CCD00E0: //TERRATEC NOXON REV2
				return RTL2832;
			case 0x1D506089: //HACKRF ONE
			case 0x1D50CC15: //HACKRF ONE
			case 0x1D50604B: //HACKRF JAWBREAKER
				return HACKRF;
			case 0x1D5060A1:
				return AIRSPY;
			case 0x03EB800C:
				return AIRSPY_HF;
		}
		
		return TunerClass.UNKNOWN;
	}
}
