/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;

public enum MixerTunerType
{

	FUNCUBE_DONGLE_PRO( TunerType.FUNCUBE_DONGLE_PRO,
						"V10",
						"FUNcube Dongle",
						"FUNcube Dongle Pro V1.0",
						new AudioFormat( 96000,  //SampleRate
										16,     //Sample Size
										2,      //Channels
										true,   //Signed
										false ) ), //Little Endian )

	FUNCUBE_DONGLE_PRO_PLUS( TunerType.FUNCUBE_DONGLE_PRO_PLUS,
							 "V20", 
							 "FUNcube Dongle V2.0",
							 "FUNcube Dongle Pro Plus V2.0",
							 new AudioFormat( 192000,  //SampleRate
											16,     //Sample Size
											2,      //Channels
											true,   //Signed
											false ) ), //Little Endian )

	UNKNOWN( TunerType.UNKNOWN,
			 "UNK", 
			 "Unknown",
			 "Unknown Mixer Tuner",
			 new AudioFormat( 48000,  //SampleRate
							16,     //Sample Size
							2,      //Channels
							true,   //Signed
							false ) ); //Little Endian )
	
	private TunerType mTunerType;
	private String mMixerName;
	private String mPartialDescription;
	private String mDisplayString;
	private AudioFormat mAudioFormat;
	
	MixerTunerType( TunerType tunerType,
					String mixerName, 
					String partialDescription,
					String displayString,
					AudioFormat format )
	{
		mTunerType = tunerType;
		mMixerName = mixerName;
		mPartialDescription = partialDescription;
		mDisplayString = displayString;
		mAudioFormat = format;
	}

	public TunerType getTunerType()
	{
		return mTunerType;
	}
	
	public String getMixerName()
	{
		return mMixerName;
	}
	
	public String getPartialDescription()
	{
		return mPartialDescription;
	}
	
	public String getDisplayString()
	{
		return mDisplayString;
	}
	
	public AudioFormat getAudioFormat()
	{
		return mAudioFormat;
	}
	
	public static MixerTunerType getMixerTunerType( Mixer.Info info )
	{
		MixerTunerType retVal = UNKNOWN;

		if( info.getName().contains( FUNCUBE_DONGLE_PRO_PLUS.mPartialDescription ) ||
				info.getDescription().contains( FUNCUBE_DONGLE_PRO_PLUS.mPartialDescription ) )
		{
		    retVal = FUNCUBE_DONGLE_PRO_PLUS;
		}
		else if( info.getName().contains( FUNCUBE_DONGLE_PRO.mPartialDescription ) ||
				info.getDescription().contains( FUNCUBE_DONGLE_PRO.mPartialDescription ) )
		{
		    retVal = FUNCUBE_DONGLE_PRO;
		}
        
		return retVal;
	}
}
