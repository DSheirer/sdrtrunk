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
package io.github.dsheirer.module.decode.passport;


import org.apache.commons.math3.util.FastMath;

public enum PassportBand
{
    BAND_800( "800", 800000000 ),
    BAND_900( "900", 900000000 ),
    BAND_400( "400", 400000000 ),
    BAND_420( "420", 420000000 ),
    BAND_440( "440", 440000000 ),
    BAND_450( "450", 450000000 ),
    BAND_460( "460", 460000000 ),
    BAND_470( "470", 470000000 ),
    BAND_480( "480", 480000000 ),
    BAND_490( "490", 490000000 ),
    BAND_CANADA( "CAN", 0 ),
    BAND_NTIA( "NTIA", 0 ),
    BAND_RESERVED12( "RESVD", 0 ),
    BAND_216( "216", 216000000 ),
    BAND_RESERVED14( "RESVD", 0 ),
    BAND_700( "700", 700000000 ),
    BAND_VHF( "VHF", 0 ),
    BAND_UNKNOWN( "UNK", 0 );
    
    public static int CHANNEL_BANDWIDTH = 12500; //Hertz
    
    private String mDescription;
    private int mBase;
    
    private PassportBand( String description, int base )
    {
        mDescription = description;
        mBase = base;
    }
    
    public static PassportBand lookup( int band )
    {
        if( band >= 0 && band < 16 )
        {
            return PassportBand.values()[ band ];
        }
        else
        {
            return PassportBand.BAND_UNKNOWN;
        }
    }
    
    public long getFrequency( int channel )
    {
    	return ( mBase + ( channel * CHANNEL_BANDWIDTH ) );
    }
    
    public int getChannel( long frequency )
    {
    	return FastMath.round( (float)( frequency - mBase ) /
    					   (float)CHANNEL_BANDWIDTH );
    }
    
    public String getDescription()
    {
        return mDescription;
    }
    
    public int getBase()
    {
        return mBase;
    }
}
