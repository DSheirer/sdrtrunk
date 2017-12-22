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
package io.github.dsheirer.audio.invert;

public enum AudioType
{
	NORMAL( "Normal Audio", "Clear", 0 ),
	MUTE( "Muted Audio", "Muted", 0 ),
	INV2500( "Inverted Audio 2500Hz", "Inverted", 2500 ),
	INV2550( "Inverted Audio 2550Hz", "Inverted", 2550 ),
	INV2600( "Inverted Audio 2600Hz", "Inverted", 2600 ),
	INV2632( "Inverted Audio 2632Hz", "Inverted", 2632 ),
	INV2675( "Inverted Audio 2675Hz", "Inverted", 2675 ),
	INV2718( "Inverted Audio 2718Hz", "Inverted", 2718 ),
	INV2725( "Inverted Audio 2725Hz", "Inverted", 2725 ),
	INV2750( "Inverted Audio 2750Hz", "Inverted", 2750 ),
	INV2760( "Inverted Audio 2760Hz", "Inverted", 2760 ),
	INV2775( "Inverted Audio 2775Hz", "Inverted", 2775 ),
	INV2800( "Inverted Audio 2800Hz", "Inverted", 2800 ),
	INV2825( "Inverted Audio 2825Hz", "Inverted", 2825 ),
	INV2868( "Inverted Audio 2868Hz", "Inverted", 2868 ),
	INV3023( "Inverted Audio 3023Hz", "Inverted", 3023 ),
	INV3107( "Inverted Audio 3107Hz", "Inverted", 3107 ),
	INV3196( "Inverted Audio 3196Hz", "Inverted", 3196 ),
	INV3333( "Inverted Audio 3333Hz", "Inverted", 3333 ),
	INV3339( "Inverted Audio 3339Hz", "Inverted", 3339 ),
	INV3360( "Inverted Audio 3360Hz", "Inverted", 3360 ),
	INV3375( "Inverted Audio 3375Hz", "Inverted", 3375 ),
	INV3400( "Inverted Audio 3400Hz", "Inverted", 3400 ),
	INV3450( "Inverted Audio 3450Hz", "Inverted", 3450 ),
	INV3496( "Inverted Audio 3496Hz", "Inverted", 3496 ),
	INV3729( "Inverted Audio 3729Hz", "Inverted", 3729 ),
	INV4096( "Inverted Audio 4096Hz", "Inverted", 4096 ),
	INV4300( "Inverted Audio 4300Hz", "Inverted", 4300 ),
	INV4500( "Inverted Audio 4500Hz", "Inverted", 4500 ),
	INV4700( "Inverted Audio 4700Hz", "Inverted", 4700 ),
	INV4900( "Inverted Audio 4900Hz", "Inverted", 4900 );
    
    private String mDisplayString;
    private String mShortDisplayString;
    private int mAudioInversionFrequency;
    
    AudioType( String displayString, 
    		   String shortDisplayString, 
    		   int inversionFrequency )
    {
        mDisplayString = displayString;
        mShortDisplayString = shortDisplayString;
        mAudioInversionFrequency = inversionFrequency;
    }
    
    public String getDisplayString()
    {
        return mDisplayString;
    }
    
    public String getShortDisplayString()
    {
    	return mShortDisplayString;
    }
    
    public int getAudioInversionFrequency()
    {
    	return mAudioInversionFrequency;
    }
    
    @Override
    public String toString()
    {
    	return mDisplayString;
    }
}
