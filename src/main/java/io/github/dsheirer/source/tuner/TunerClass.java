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

import javax.usb.UsbDeviceDescriptor;

public enum TunerClass
{
	AIRSPY( TunerType.AIRSPY_R820T, "1D50", "60A1", "Airspy", "Airspy" ),
	GENERIC_2832( TunerType.RTL2832_VARIOUS, "0BDA", "2832", "RTL2832", "SDR" ),        		
	GENERIC_2838( TunerType.RTL2832_VARIOUS, "0BDA", "2838", "RTL2832", "SDR" ),               
	COMPRO_VIDEOMATE_U620F( TunerType.ELONICS_E4000, "185B", "0620", "Compro", "Videomate U620F" ),   
	COMPRO_VIDEOMATE_U650F( TunerType.ELONICS_E4000, "185B", "0650", "Compro", "Videomate U620F" ),   
	COMPRO_VIDEOMATE_U680F( TunerType.ELONICS_E4000, "185B", "0680", "Compro", "Videomate U620F" ),   
	DEXATEK_LOGILINK_VG002A( TunerType.FCI_FC2580, "1D19", "1101", "Dexatek", "Logilink VG0002A" ),
	DEXATEK_DIGIVOX_MINI_II_REV3( TunerType.FCI_FC2580, "1D19", "1102", "Dexatek", "MSI Digivox Mini II v3.0" ),
	DEXATEK_5217_DVBT( TunerType.FCI_FC2580, "1D19", "1103", "Dexatek", "5217 DVB-T" ),
	ETTUS_USRP_B100( TunerType.ETTUS_VARIOUS, "2500", "0002", "Ettus Research", "USRP B100" ),
	FUNCUBE_DONGLE_PRO( TunerType.FUNCUBE_DONGLE_PRO, "04D8", "FB56", "Hamlincrest", "Funcube Dongle Pro" ),
	FUNCUBE_DONGLE_PRO_PLUS( TunerType.FUNCUBE_DONGLE_PRO_PLUS, "04D8", "FB31", "Hamlincrest", "Funcube Dongle Pro Plus" ),
	GIGABYTE_GTU7300( TunerType.FITIPOWER_FC0012, "1B80", "D393", "Gigabyte", "GT-U7300" ),
	GTEK_T803( TunerType.FITIPOWER_FC0012, "1F4D", "B803", "GTek", "T803" ),
	HACKRF_ONE( TunerType.HACKRF, "1D50", "6089", "Great Scott Gadgets", "HackRF One" ),
	HACKRF_JAWBREAKER( TunerType.HACKRF, "1D50", "604B", "Great Scott Gadgets", "HackRF Jawbreaker" ),
	RAD1O( TunerType.HACKRF, "1D50", "CC15", "Munich hackerspace", "Rad1o" ),
	LIFEVIEW_LV5T_DELUXE( TunerType.FITIPOWER_FC0012, "1F4D", "C803", "Liveview", "LV5T Deluxe" ),
	MYGICA_TD312( TunerType.FITIPOWER_FC0012, "1F4D", "D286", "MyGica", "TD312" ),
	PEAK_102569AGPK( TunerType.FITIPOWER_FC0012, "1B80", "D395", "Peak", "102569AGPK" ),
	PROLECTRIX_DV107669( TunerType.FITIPOWER_FC0012, "1F4D", "D803", "Prolectrix", "DV107669" ),
	SVEON_STV20( TunerType.FITIPOWER_FC0012, "1B80", "D39D", "Sveon", "STV20 DVB-T USB & FM" ),
	TERRATEC_CINERGY_T_REV1( TunerType.FITIPOWER_FC0012, "0CCD", "00A9", "Terratec", "Cinergy T R1" ),
	TERRATEC_CINERGY_T_REV3( TunerType.ELONICS_E4000, "0CCD", "00D3", "Terratec", "Cinergy T R3" ),
	TERRATEC_NOXON_REV1_B3( TunerType.FITIPOWER_FC0013, "0CCD", "00B3", "Terratec", "NOXON R1 (B3)" ),
	TERRATEC_NOXON_REV1_B4( TunerType.FITIPOWER_FC0013, "0CCD", "00B4", "Terratec", "NOXON R1 (B4)" ),
	TERRATEC_NOXON_REV1_B7( TunerType.FITIPOWER_FC0013, "0CCD", "00B7", "Terratec", "NOXON R1 (B7)" ),
	TERRATEC_NOXON_REV1_C6( TunerType.FITIPOWER_FC0013, "0CCD", "00C6", "Terratec", "NOXON R1 (C6)" ),
	TERRATEC_NOXON_REV2( TunerType.ELONICS_E4000, "0CCD", "00E0", "Terratec", "NOXON R2" ),
	TERRATEC_T_STICK_PLUS( TunerType.ELONICS_E4000, "0CCD", "00D7", "Terratec", "T Stick Plus" ),
	TWINTECH_UT40( TunerType.FITIPOWER_FC0013, "1B80", "D3A4", "Twintech", "UT-40" ),
	ZAAPA_ZTMINDVBZP( TunerType.FITIPOWER_FC0012, "1B80", "D398", "Zaapa", "ZT-MINDVBZP" ),
	TEST_TUNER(TunerType.TEST, "0", "0", "ABC Tuners Inc.", "Model XYZ"),
	RECORDING_TUNER(TunerType.RECORDING, "0", "0", "Recording Tuner", "Recording"),
	UNKNOWN( TunerType.UNKNOWN, "0", "0", "Unknown Manufacturer", "Unknown Device" );
	
	private TunerType mTunerType;
	private String mVendorID;
	private String mDeviceID;
	private String mVendorDescription;
	private String mDeviceDescription;
	
	private TunerClass( TunerType tunerType,
					   String vendorID, 
					   String deviceID,
					   String vendorDescription,
					   String deviceDescription )
	{
		mTunerType = tunerType;
		mVendorID = vendorID;
		mDeviceID = deviceID;
		mVendorDescription = vendorDescription;
		mDeviceDescription = deviceDescription;
	}
	
	public String toString()
	{
		return "USB" +
				" Tuner:" + mTunerType.toString() +
				" Vendor:" + mVendorDescription + 
				" Device:" + mDeviceDescription +
				" Address:" + mVendorID + ":" + mDeviceID;
	}
	
	public String getVendorDeviceLabel()
	{
		return mVendorDescription + " " + mDeviceDescription;
	}
	
	public TunerType getTunerType()
	{
		return mTunerType;
	}
	
	public static TunerClass valueOf( UsbDeviceDescriptor descriptor )
	{
		return valueOf( descriptor.idVendor(), descriptor.idProduct() );
	}
	
	public static TunerClass valueOf( short vendor, short product )
	{
		TunerClass retVal = TunerClass.UNKNOWN;

		//Cast the short to integer so that we can switch on unsigned numbers
		int vendorID = vendor & 0xFFFF;
		int productID = product & 0xFFFF;
		
		switch( vendorID )
		{
			case 1240: //04DA
				if( productID == 64305 ) //FB31
				{
					retVal = FUNCUBE_DONGLE_PRO_PLUS;
				}
				else if( productID == 64342 ) //FB56
				{
					retVal = FUNCUBE_DONGLE_PRO;
				}
				break;
			case 3034: //0BDA
				if( productID == 10290 ) //2832
				{
					retVal = GENERIC_2832;
				}
				else if( productID == 10296 ) //2838
				{
					retVal = GENERIC_2838;
				}
				break;
			case 6235: //185B
				if( productID == 1568 ) //0620
				{
					retVal = COMPRO_VIDEOMATE_U620F;
				}
				else if( productID == 1616  ) //0650
				{
					retVal = COMPRO_VIDEOMATE_U650F;
				}
				else if( productID == 1664  ) //0680
				{
					retVal = COMPRO_VIDEOMATE_U680F;
				}
				break;
			case 7040: //1B80
				if( productID == 54163 )  //D393
				{
					retVal = GIGABYTE_GTU7300;
				}
				else if( productID == 54165 ) //D395
				{
					retVal = PEAK_102569AGPK;
				}
				else if( productID == 54168  ) //D398
				{
					retVal = ZAAPA_ZTMINDVBZP;
				}
				else if( productID == 54173  ) //D39D
				{
					retVal = SVEON_STV20;
				}
				else if( productID == 54180 ) //D3A4
				{
					retVal = TWINTECH_UT40;
				}
				break;
			case 7449: //1D19
				if( productID == 4353 ) //1101
				{
					retVal = DEXATEK_LOGILINK_VG002A;
				}
				else if( productID == 4354  ) //1102
				{
					retVal = DEXATEK_DIGIVOX_MINI_II_REV3;
				}
				else if( productID == 4355 ) //1103
				{
					retVal = DEXATEK_5217_DVBT;
				}
				break;
			case 7504: //1D50 
				if( productID == 24713 ) //6089
				{
					retVal = HACKRF_ONE;
				}
				else if( productID == 24737 ) //60A1
				{
					retVal = AIRSPY;
				}
				else if( productID == 52245 ) //CC15
				{
					retVal = HACKRF_ONE;
				}
				else if( productID == 24651 ) //604B
				{
					retVal = HACKRF_JAWBREAKER;
				}
				break;
			case 8013: //1F4D
				if( productID == 47107 ) //B803
				{
					retVal = GTEK_T803;
				}
				else if( productID == 51203 ) //C803
				{
					retVal = LIFEVIEW_LV5T_DELUXE;
				}
				else if( productID == 53894 ) //D286
				{
					retVal = MYGICA_TD312;
				}
				else if( productID == 55299  ) //D803
				{
					retVal = PROLECTRIX_DV107669;
				}
				break;
			case 3277: //0CCD
				if( productID == 169 ) //00A9
				{
					retVal = TERRATEC_CINERGY_T_REV1;
				}
				else if( productID == 179 ) //00B3
				{
					retVal = TERRATEC_NOXON_REV1_B3;
				}
				else if( productID == 180 ) //00B4
				{
					retVal = TERRATEC_NOXON_REV1_B4;
				}
				else if( productID == 181 ) //00B5
				{
					retVal = TERRATEC_NOXON_REV1_B7;
				}
				else if( productID == 198 ) //00C6
				{
					retVal = TERRATEC_NOXON_REV1_C6;
				}
				else if( productID == 211 ) //00D3
				{
					retVal = TERRATEC_CINERGY_T_REV3;
				}
				else if( productID == 215 ) //00D7
				{
					retVal = TERRATEC_T_STICK_PLUS;
				}
				else if( productID == 224) //00E0
				{
					retVal = TERRATEC_NOXON_REV2;
				}
				break;
			case 9472: //2500
				if( productID == 2 ) //0002
				{
					retVal = ETTUS_USRP_B100;
				}
				break;
			default:
		}
		
		return retVal;
	}

	public String getVendorID()
	{
		return mVendorID;
	}
	
	public String getDeviceID()
	{
		return mDeviceID;
	}
	
	public String getVendorDescription()
	{
		return mVendorDescription;
	}
	
	public String getDeviceDescription()
	{
		return mDeviceDescription;
	}
}
